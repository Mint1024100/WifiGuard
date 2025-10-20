package com.wifiguard.feature.scanner.data.repository

import com.wifiguard.core.data.wifi.WifiScannerService
import com.wifiguard.feature.scanner.domain.model.WifiInfo
import com.wifiguard.feature.scanner.domain.repository.WifiScannerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация репозитория для работы с WiFi сканированием
 * Использует WifiScannerService для получения данных о WiFi сетях
 */
@Singleton
class WifiScannerRepositoryImpl @Inject constructor(
    private val wifiScannerService: WifiScannerService
) : WifiScannerRepository {

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

    override suspend fun startScan(): Boolean {
        return wifiScannerService.startScan()
    }

    override suspend fun stopObserving() {
        // Currently, the WifiScannerService doesn't have a stop method
        // The observation is handled automatically through BroadcastReceiver
        // which is unregistered when the Flow is closed
    }
}
