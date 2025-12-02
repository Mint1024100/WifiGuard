package com.wifiguard.core.data.repository

import com.wifiguard.core.common.WifiNetworkDomainToEntityMapper
import com.wifiguard.core.common.WifiNetworkEntityToDomainMapper
import com.wifiguard.core.common.WifiScanDomainToEntityMapper
import com.wifiguard.core.common.WifiScanEntityToDomainMapper
import com.wifiguard.core.data.local.dao.ScanSessionDao
import com.wifiguard.core.data.local.dao.ThreatDao
import com.wifiguard.core.data.local.dao.WifiNetworkDao
import com.wifiguard.core.data.local.dao.WifiScanDao
import com.wifiguard.core.domain.model.WifiNetwork
import com.wifiguard.core.domain.model.WifiScanResult
import com.wifiguard.core.domain.repository.WifiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Основной репозиторий для управления Wi-Fi данными.
 * Обеспечивает единую точку доступа к локальным и удалённым источникам данных.
 */
@Singleton
class WifiRepositoryImpl @Inject constructor(
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
     * Сохранить Wi-Fi сеть
     */
    override suspend fun insertNetwork(network: WifiNetwork) {
        val entity = wifiNetworkDomainToEntityMapper.map(network)
        wifiNetworkDao.insertNetwork(entity)
    }

    /**
     * Обновить информацию о сети
     */
    override suspend fun updateNetwork(network: WifiNetwork) {
        val entity = wifiNetworkDomainToEntityMapper.map(network)
        wifiNetworkDao.updateNetwork(entity)
    }

    /**
     * Удалить сеть
     */
    override suspend fun deleteNetwork(network: WifiNetwork) {
        val entity = wifiNetworkDomainToEntityMapper.map(network)
        wifiNetworkDao.deleteNetwork(entity)
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
        val entity = wifiScanDomainToEntityMapper.map(scanResult)
        wifiScanDao.insertScanResult(entity)
    }

    /**
     * Очистить старые результаты сканирования
     */
    override suspend fun clearOldScans(olderThanMillis: Long) {
        wifiScanDao.clearOldScans(olderThanMillis)
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
     */
    override suspend fun clearAllData() {
        threatDao.deleteAllThreats()
        wifiScanDao.deleteAllScans()
        wifiNetworkDao.deleteAllNetworks()
        scanSessionDao.deleteAllSessions()
    }
}