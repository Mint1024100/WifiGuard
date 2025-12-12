package com.wifiguard.feature.scanner.data.datasource

import com.wifiguard.core.domain.model.WifiScanStatus
import com.wifiguard.feature.scanner.domain.model.WifiInfo
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс источника данных для WiFi сканирования
 */
interface WifiDataSource {
    suspend fun scanWifiNetworks(): List<WifiInfo>
    fun observeWifiNetworks(): Flow<List<WifiInfo>>
    suspend fun getCurrentWifiInfo(): WifiInfo?
    fun observeCurrentWifiInfo(): Flow<WifiInfo?>
    suspend fun isWifiEnabled(): Boolean
    fun observeWifiState(): Flow<Boolean>
    suspend fun startWifiScan(): WifiScanStatus
}
