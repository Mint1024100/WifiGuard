package com.wifiguard.testing

import com.wifiguard.core.data.wifi.WifiScanner
import com.wifiguard.core.domain.model.Freshness
import com.wifiguard.core.domain.model.ScanMetadata
import com.wifiguard.core.domain.model.ScanSource
import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.domain.model.WifiScanResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Фейковая реализация [WifiScanner] для стабильных UI тестов.
 *
 * Цель: убрать зависимость от реального Wi‑Fi/геолокации/окружения устройства.
 */
@Singleton
class FakeWifiScanner @Inject constructor() : WifiScanner {

    private val scanResultsFlow = MutableStateFlow(defaultNetworks())
    private val wifiEnabledFlow = MutableStateFlow(true)

    private var nextScanResult: Result<List<WifiScanResult>> = Result.success(scanResultsFlow.value)
    private var currentNetwork: WifiScanResult? = null
    private var lastScanMetadata: ScanMetadata? = ScanMetadata.create(
        source = ScanSource.ACTIVE_SCAN,
        freshness = Freshness.FRESH
    )

    fun setWifiEnabled(enabled: Boolean) {
        wifiEnabledFlow.value = enabled
    }

    fun setCurrentNetwork(network: WifiScanResult?) {
        currentNetwork = network
    }

    fun setNextScanResult(result: Result<List<WifiScanResult>>) {
        nextScanResult = result
    }

    fun setScanResults(results: List<WifiScanResult>) {
        scanResultsFlow.value = results
        nextScanResult = Result.success(results)
    }

    fun resetToDefaults() {
        setWifiEnabled(true)
        setCurrentNetwork(null)
        setScanResults(defaultNetworks())
        lastScanMetadata = ScanMetadata.create(ScanSource.ACTIVE_SCAN, Freshness.FRESH)
    }

    override suspend fun startScan(): Result<List<WifiScanResult>> {
        val result = nextScanResult
        if (result.isSuccess) {
            val list = result.getOrNull().orEmpty()
            scanResultsFlow.value = list
            lastScanMetadata = ScanMetadata.create(ScanSource.ACTIVE_SCAN, Freshness.FRESH)
        } else {
            // Скан не удался — метаданные всё равно обновляем, чтобы UI мог показать «попытку».
            lastScanMetadata = ScanMetadata.create(ScanSource.ACTIVE_SCAN, Freshness.STALE)
        }
        return result
    }

    override fun getScanResultsFlow(): Flow<List<WifiScanResult>> = scanResultsFlow.asStateFlow()

    override fun isWifiEnabled(): Boolean = wifiEnabledFlow.value

    override fun observeWifiEnabled(): Flow<Boolean> = wifiEnabledFlow.asStateFlow()

    override suspend fun getCurrentNetwork(): WifiScanResult? = currentNetwork

    override fun startContinuousScan(intervalMs: Long): Flow<List<WifiScanResult>> = scanResultsFlow.asStateFlow()

    override fun getLastScanMetadata(): ScanMetadata? = lastScanMetadata

    private fun defaultNetworks(): List<WifiScanResult> {
        val now = System.currentTimeMillis()
        return listOf(
            WifiScanResult(
                ssid = "Test WiFi 1",
                bssid = "02:00:00:00:00:01",
                capabilities = "[WPA2-PSK-CCMP][ESS]",
                frequency = 2412,
                level = -45,
                timestamp = now,
                securityType = SecurityType.WPA2,
                threatLevel = ThreatLevel.SAFE,
                isConnected = false,
                channel = 1
            ),
            WifiScanResult(
                ssid = "Test WiFi 2",
                bssid = "02:00:00:00:00:02",
                capabilities = "[ESS]",
                frequency = 5180,
                level = -62,
                timestamp = now - 1_000,
                securityType = SecurityType.OPEN,
                threatLevel = ThreatLevel.LOW,
                isConnected = false,
                channel = 36
            )
        )
    }
}






