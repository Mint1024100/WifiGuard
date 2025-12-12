package com.wifiguard.core.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.wifiguard.core.common.WifiNetworkDomainToEntityMapper
import com.wifiguard.core.common.WifiNetworkEntityToDomainMapper
import com.wifiguard.core.common.WifiScanDomainToEntityMapper
import com.wifiguard.core.common.WifiScanEntityToDomainMapper
import com.wifiguard.core.common.BssidValidator
import com.wifiguard.core.data.local.WifiGuardDatabase
import com.wifiguard.core.data.local.dao.ScanSessionDao
import com.wifiguard.core.data.local.dao.ThreatDao
import com.wifiguard.core.data.local.dao.WifiNetworkDao
import com.wifiguard.core.data.local.dao.WifiScanDao
import com.wifiguard.core.data.local.entity.WifiNetworkEntity
import com.wifiguard.core.domain.model.WifiNetwork
import com.wifiguard.core.domain.model.WifiScanResult
import com.wifiguard.core.domain.repository.WifiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Основной репозиторий для управления Wi-Fi данными.
 * Обеспечивает единую точку доступа к локальным и удалённым источникам данных.
 */
@Singleton
class WifiRepositoryImpl @Inject constructor(
    private val database: WifiGuardDatabase,
    private val wifiNetworkDao: WifiNetworkDao,
    private val wifiScanDao: WifiScanDao,
    private val threatDao: ThreatDao,
    private val scanSessionDao: ScanSessionDao,
    private val wifiNetworkEntityToDomainMapper: WifiNetworkEntityToDomainMapper,
    private val wifiNetworkDomainToEntityMapper: WifiNetworkDomainToEntityMapper,
    private val wifiScanEntityToDomainMapper: WifiScanEntityToDomainMapper,
    private val wifiScanDomainToEntityMapper: WifiScanDomainToEntityMapper
) : WifiRepository {

    /**
     * Получить все сохранённые Wi-Fi сети
     */
    override fun getAllNetworks(): Flow<List<WifiNetwork>> {
        return wifiNetworkDao.getAllNetworks().map { entities ->
            entities.map { wifiNetworkEntityToDomainMapper.map(it) }
        }
    }

    /**
     * Получить сеть по SSID
     */
    override suspend fun getNetworkBySSID(ssid: String): WifiNetwork? {
        val entity = wifiNetworkDao.getNetworkBySSID(ssid)
        return entity?.let { wifiNetworkEntityToDomainMapper.map(it) }
    }

    /**
     * Получить сеть по BSSID (уникальный MAC-адрес точки доступа)
     */
    override suspend fun getNetworkByBssid(bssid: String): WifiNetwork? {
        val entity = wifiNetworkDao.getNetworkByBssid(bssid)
        return entity?.let { wifiNetworkEntityToDomainMapper.map(it) }
    }

    /**
     * Сохранить Wi-Fi сеть
     */
    override suspend fun insertNetwork(network: WifiNetwork) {
        withContext(Dispatchers.IO) {
            try {
                val entity = wifiNetworkDomainToEntityMapper.map(network)
                wifiNetworkDao.insertNetwork(entity)
                Log.d("WifiRepository", "Сеть успешно сохранена: ${network.ssid}")
            } catch (e: Exception) {
                Log.e("WifiRepository", "Ошибка при сохранении сети: ${e.message}", e)
                throw e
            }
        }
    }

    /**
     * Обновить информацию о сети
     */
    override suspend fun updateNetwork(network: WifiNetwork) {
        withContext(Dispatchers.IO) {
            try {
                val entity = wifiNetworkDomainToEntityMapper.map(network)
                wifiNetworkDao.updateNetwork(entity)
                Log.d("WifiRepository", "Сеть успешно обновлена: ${network.ssid}")
            } catch (e: Exception) {
                Log.e("WifiRepository", "Ошибка при обновлении сети: ${e.message}", e)
                throw e
            }
        }
    }

    /**
     * Удалить сеть
     */
    override suspend fun deleteNetwork(network: WifiNetwork) {
        withContext(Dispatchers.IO) {
            val entity = wifiNetworkDomainToEntityMapper.map(network)
            wifiNetworkDao.deleteNetwork(entity)
        }
    }

    /**
     * Получить последние результаты сканирования
     */
    override fun getLatestScans(limit: Int): Flow<List<WifiScanResult>> {
        return wifiScanDao.getLatestScans(limit).map { entities ->
            entities.map { wifiScanEntityToDomainMapper.map(it) }
        }
    }

    /**
     * Сохранить результат сканирования
     */
    override suspend fun insertScanResult(scanResult: WifiScanResult) {
        withContext(Dispatchers.IO) {
            try {
                val entity = wifiScanDomainToEntityMapper.map(scanResult)
                wifiScanDao.insertScanResult(entity)
                Log.d("WifiRepository", "Результат сканирования успешно сохранен: ${scanResult.ssid}")
            } catch (e: Exception) {
                Log.e("WifiRepository", "Ошибка при сохранении результата сканирования: ${e.message}", e)
                throw e
            }
        }
    }

    override suspend fun insertScanResults(scanResults: List<WifiScanResult>) {
        if (scanResults.isEmpty()) return
        withContext(Dispatchers.IO) {
            try {
                val entities = scanResults.map { wifiScanDomainToEntityMapper.map(it) }
                wifiScanDao.insertScans(entities)
                Log.d("WifiRepository", "✅ Батч: сохранено результатов сканирования: ${entities.size}")
            } catch (e: Exception) {
                Log.e("WifiRepository", "❌ Ошибка батч-сохранения сканов: ${e.message}", e)
                throw e
            }
        }
    }

    override suspend fun upsertNetworksFromScanResults(scanResults: List<WifiScanResult>) {
        if (scanResults.isEmpty()) return
        withContext(Dispatchers.IO) {
            val validResults = scanResults
                .asSequence()
                .filter { BssidValidator.isValidForStorage(it.bssid) }
                .toList()

            if (validResults.isEmpty()) return@withContext

            try {
                val bssids = validResults.map { it.bssid }.distinct()
                val (updatedCount, insertedCount) = database.withTransaction {
                    val existing = wifiNetworkDao.getNetworksByBssids(bssids)
                    val existingByBssid = existing.associateBy { it.bssid }

                    val toUpdate = mutableListOf<WifiNetworkEntity>()
                    val toInsert = mutableListOf<WifiNetworkEntity>()

                    validResults.forEach { scan ->
                        val ssid = scan.ssid.takeIf { it.isNotBlank() }
                        val existingEntity = existingByBssid[scan.bssid]

                        if (existingEntity != null) {
                            toUpdate += existingEntity.copy(
                                ssid = ssid ?: existingEntity.ssid,
                                frequency = scan.frequency,
                                signalStrength = scan.level,
                                securityType = scan.securityType,
                                channel = scan.channel,
                                // threatLevel сохраняем как есть (он управляется бизнес-логикой/анализом)
                                isHidden = scan.isHidden,
                                lastSeen = scan.timestamp,
                                detectionCount = existingEntity.detectionCount + 1,
                                vendor = existingEntity.vendor ?: scan.vendor,
                                // notes / isSuspicious сохраняем
                                // firstSeen сохраняем
                            )
                        } else {
                            toInsert += WifiNetworkEntity(
                                id = 0,
                                bssid = scan.bssid,
                                ssid = ssid,
                                frequency = scan.frequency,
                                signalStrength = scan.level,
                                securityType = scan.securityType,
                                channel = scan.channel,
                                threatLevel = com.wifiguard.core.domain.model.ThreatLevel.UNKNOWN,
                                isHidden = scan.isHidden,
                                firstSeen = scan.timestamp,
                                lastSeen = scan.timestamp,
                                detectionCount = 1,
                                isSuspicious = false,
                                vendor = scan.vendor,
                                notes = null
                            )
                        }
                    }

                    if (toUpdate.isNotEmpty()) {
                        wifiNetworkDao.updateNetworks(toUpdate)
                    }
                    if (toInsert.isNotEmpty()) {
                        // Вставляем только новые записи; конфликтов по bssid не ожидается
                        wifiNetworkDao.insertNetworks(toInsert)
                    }
                    Pair(toUpdate.size, toInsert.size)
                }

                Log.d("WifiRepository", "✅ Батч wifi_networks: update=$updatedCount, insert=$insertedCount")
            } catch (e: Exception) {
                Log.e("WifiRepository", "❌ Ошибка батч-upsert сетей: ${e.message}", e)
                throw e
            }
        }
    }

    override suspend fun persistScanResults(scanResults: List<WifiScanResult>) {
        if (scanResults.isEmpty()) return
        withContext(Dispatchers.IO) {
            try {
                database.withTransaction {
                    val entities = scanResults.map { wifiScanDomainToEntityMapper.map(it) }
                    wifiScanDao.insertScans(entities)
                    // Обновляем wifi_networks атомарно вместе со сканами
                    // (без повторной вставки в wifi_scans)
                    // Внутри транзакции: безопасно и быстро
                    // NOTE: upsertNetworksFromScanResults сама использует транзакцию,
                    // поэтому здесь вызываем её "внутреннюю" логику напрямую не получится.
                    // Для простоты выполняем отдельной транзакцией? НЕЛЬЗЯ (уже внутри).
                    // Поэтому используем приватный путь через DAO здесь.
                    val validResults = scanResults
                        .asSequence()
                        .filter { BssidValidator.isValidForStorage(it.bssid) }
                        .toList()

                    if (validResults.isNotEmpty()) {
                        val bssids = validResults.map { it.bssid }.distinct()
                        val existing = wifiNetworkDao.getNetworksByBssids(bssids)
                        val existingByBssid = existing.associateBy { it.bssid }

                        val toUpdate = mutableListOf<WifiNetworkEntity>()
                        val toInsert = mutableListOf<WifiNetworkEntity>()

                        validResults.forEach { scan ->
                            val ssid = scan.ssid.takeIf { it.isNotBlank() }
                            val existingEntity = existingByBssid[scan.bssid]

                            if (existingEntity != null) {
                                toUpdate += existingEntity.copy(
                                    ssid = ssid ?: existingEntity.ssid,
                                    frequency = scan.frequency,
                                    signalStrength = scan.level,
                                    securityType = scan.securityType,
                                    channel = scan.channel,
                                    isHidden = scan.isHidden,
                                    lastSeen = scan.timestamp,
                                    detectionCount = existingEntity.detectionCount + 1,
                                    vendor = existingEntity.vendor ?: scan.vendor
                                )
                            } else {
                                toInsert += WifiNetworkEntity(
                                    id = 0,
                                    bssid = scan.bssid,
                                    ssid = ssid,
                                    frequency = scan.frequency,
                                    signalStrength = scan.level,
                                    securityType = scan.securityType,
                                    channel = scan.channel,
                                    threatLevel = com.wifiguard.core.domain.model.ThreatLevel.UNKNOWN,
                                    isHidden = scan.isHidden,
                                    firstSeen = scan.timestamp,
                                    lastSeen = scan.timestamp,
                                    detectionCount = 1,
                                    isSuspicious = false,
                                    vendor = scan.vendor,
                                    notes = null
                                )
                            }
                        }

                        if (toUpdate.isNotEmpty()) wifiNetworkDao.updateNetworks(toUpdate)
                        if (toInsert.isNotEmpty()) wifiNetworkDao.insertNetworks(toInsert)
                    }
                }
            } catch (e: Exception) {
                Log.e("WifiRepository", "❌ Ошибка persistScanResults: ${e.message}", e)
                throw e
            }
        }
    }

    /**
     * Очистить старые результаты сканирования
     */
    override suspend fun clearOldScans(olderThanMillis: Long) {
        withContext(Dispatchers.IO) {
            wifiScanDao.clearOldScans(olderThanMillis)
        }
    }
    
    /**
     * Удалить сканы старше указанного времени и вернуть количество удаленных записей
     */
    override suspend fun deleteScansOlderThan(timestampMillis: Long): Int {
        return withContext(Dispatchers.IO) {
            try {
                val deletedCount = wifiScanDao.deleteScansOlderThan(timestampMillis)
                Log.d("WifiRepository", "Удалено старых сканов: $deletedCount")
                deletedCount
            } catch (e: Exception) {
                Log.e("WifiRepository", "Ошибка при удалении старых сканов: ${e.message}", e)
                0
            }
        }
    }
    
    /**
     * Получить общее количество сохраненных сканов
     */
    override suspend fun getTotalScansCount(): Int {
        return withContext(Dispatchers.IO) {
            try {
                wifiScanDao.getTotalScansCount()
            } catch (e: Exception) {
                Log.e("WifiRepository", "Ошибка при получении количества сканов: ${e.message}", e)
                0
            }
        }
    }
    
    /**
     * Оптимизировать базу данных (VACUUM)
     * 
     * ИСПРАВЛЕНО: Room не поддерживает VACUUM через @Query,
     * поэтому используем прямой доступ к SupportSQLiteDatabase
     */
    override suspend fun optimizeDatabase() {
        withContext(Dispatchers.IO) {
            try {
                Log.d("WifiRepository", "Начало оптимизации базы данных")
                
                // Получаем прямой доступ к базе данных для выполнения VACUUM
                database.openHelper.writableDatabase.execSQL("VACUUM")
                
                Log.d("WifiRepository", "База данных успешно оптимизирована (VACUUM выполнен)")
            } catch (e: Exception) {
                Log.e("WifiRepository", "Ошибка при оптимизации БД: ${e.message}", e)
            }
        }
    }

    /**
     * Получить статистику по сети
     */
    override fun getNetworkStatistics(ssid: String): Flow<List<WifiScanResult>> {
        return wifiScanDao.getNetworkStatistics(ssid).map { entities ->
            entities.map { wifiScanEntityToDomainMapper.map(it) }
        }
    }

    /**
     * Пометить сеть как подозрительную
     */
    override suspend fun markNetworkAsSuspicious(ssid: String, reason: String) {
        val network = getNetworkBySSID(ssid)
        if (network != null) {
            updateNetwork(
                network.copy(
                    isSuspicious = true,
                    suspiciousReason = reason,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Получить подозрительные сети
     */
    override fun getSuspiciousNetworks(): Flow<List<WifiNetwork>> {
        return wifiNetworkDao.getSuspiciousNetworks().map { entities ->
            entities.map { wifiNetworkEntityToDomainMapper.map(it) }
        }
    }

    /**
     * Очистить все данные, связанные с Wi-Fi сетями, сканированиями и угрозами.
     * Удаляет данные из всех таблиц: threats, wifi_scans, wifi_networks, scan_sessions
     */
    override suspend fun clearAllData() {
        withContext(Dispatchers.IO) {
            try {
                Log.d("WifiRepository", "Начало очистки всех данных из БД")
                
                // Удаляем данные из всех таблиц
                threatDao.deleteAllThreats()
                Log.d("WifiRepository", "Удалены все угрозы")
                
                wifiScanDao.deleteAllScans()
                Log.d("WifiRepository", "Удалены все результаты сканирований")
                
                wifiNetworkDao.deleteAllNetworks()
                Log.d("WifiRepository", "Удалены все сети")
                
                scanSessionDao.deleteAllSessions()
                Log.d("WifiRepository", "Удалены все сессии сканирования")
                
                Log.d("WifiRepository", "Все данные успешно удалены из БД")
            } catch (e: Exception) {
                Log.e("WifiRepository", "Ошибка при удалении данных: ${e.message}", e)
                throw e
            }
        }
    }

    /**
     * Проверить целостность базы данных
     * Выполняет простые запросы к каждой таблице для проверки работоспособности
     */
    override suspend fun validateDatabaseIntegrity(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Проверяем доступность каждой таблицы через простые запросы COUNT
                val threatsCount = threatDao.getTotalThreatsCount()
                val scansCount = wifiScanDao.getTotalScansCount()
                // Для networks используем getAllWifiNetworksSuspend, так как нет метода getTotalNetworksCount
                val networksCount = wifiNetworkDao.getAllWifiNetworksSuspend().size
                val sessionsCount = scanSessionDao.getTotalSessionsCount()
                
                Log.d("WifiRepository", "Проверка целостности БД: threats=$threatsCount, scans=$scansCount, networks=$networksCount, sessions=$sessionsCount")
                
                // Если запросы выполнились без ошибок, БД работает корректно
                true
            } catch (e: Exception) {
                Log.e("WifiRepository", "Ошибка при проверке целостности БД: ${e.message}", e)
                false
            }
        }
    }
}