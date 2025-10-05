package com.wifiguard.core.data.repository

import com.wifiguard.core.data.local.dao.WifiNetworkDao
import com.wifiguard.core.data.local.dao.WifiScanDao
import com.wifiguard.core.domain.model.WifiNetwork
import com.wifiguard.core.domain.model.WifiScanResult
import com.wifiguard.core.domain.repository.WifiRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Основной репозиторий для управления Wi-Fi данными.
 * Обеспечивает единую точку доступа к локальным и удалённым источникам данных.
 */
@Singleton
class WifiRepositoryImpl @Inject constructor(
    private val wifiNetworkDao: WifiNetworkDao,
    private val wifiScanDao: WifiScanDao
) : WifiRepository {

    /**
     * Получить все сохранённые Wi-Fi сети
     */
    override fun getAllNetworks(): Flow<List<WifiNetwork>> {
        return wifiNetworkDao.getAllNetworks()
    }

    /**
     * Получить сеть по SSID
     */
    override suspend fun getNetworkBySSID(ssid: String): WifiNetwork? {
        return wifiNetworkDao.getNetworkBySSID(ssid)
    }

    /**
     * Сохранить Wi-Fi сеть
     */
    override suspend fun insertNetwork(network: WifiNetwork) {
        wifiNetworkDao.insertNetwork(network)
    }

    /**
     * Обновить информацию о сети
     */
    override suspend fun updateNetwork(network: WifiNetwork) {
        wifiNetworkDao.updateNetwork(network)
    }

    /**
     * Удалить сеть
     */
    override suspend fun deleteNetwork(network: WifiNetwork) {
        wifiNetworkDao.deleteNetwork(network)
    }

    /**
     * Получить последние результаты сканирования
     */
    override fun getLatestScans(limit: Int): Flow<List<WifiScanResult>> {
        return wifiScanDao.getLatestScans(limit)
    }

    /**
     * Сохранить результат сканирования
     */
    override suspend fun insertScanResult(scanResult: WifiScanResult) {
        wifiScanDao.insertScanResult(scanResult)
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
    override suspend fun getNetworkStatistics(ssid: String): Flow<List<WifiScanResult>> {
        return wifiScanDao.getNetworkStatistics(ssid)
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
        return wifiNetworkDao.getSuspiciousNetworks()
    }
}