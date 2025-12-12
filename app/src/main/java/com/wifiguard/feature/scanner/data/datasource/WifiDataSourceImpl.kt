package com.wifiguard.feature.scanner.data.datasource

import com.wifiguard.core.data.wifi.WifiScannerService
import com.wifiguard.feature.scanner.domain.model.WifiInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация источника данных для WiFi сканирования
 */
@Singleton
class WifiDataSourceImpl @Inject constructor(
    private val wifiScannerService: WifiScannerService
) : WifiDataSource {

    override suspend fun scanWifiNetworks(): List<WifiInfo> {
        return wifiScannerService.getScanResults()
    }

    override fun observeWifiNetworks(): Flow<List<WifiInfo>> {
        return wifiScannerService.observeScanResults()
    }

    override suspend fun getCurrentWifiInfo(): WifiInfo? {
        return wifiScannerService.getCurrentNetwork()
    }

    override fun observeCurrentWifiInfo(): Flow<WifiInfo?> {
        return flow {
            emit(wifiScannerService.getCurrentNetwork())
        }
    }

    override suspend fun isWifiEnabled(): Boolean {
        return wifiScannerService.isWifiEnabled()
    }

    override fun observeWifiState(): Flow<Boolean> {
        return flow {
            emit(wifiScannerService.isWifiEnabled())
        }
    }

    override suspend fun startWifiScan(): com.wifiguard.core.domain.model.WifiScanStatus {
        return wifiScannerService.startScan()
    }
}
