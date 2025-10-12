package com.wifiguard.core.data.wifi

import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.domain.model.WifiScanResult
import com.wifiguard.core.domain.model.WifiStandard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Интерфейс для сканирования Wi-Fi сетей
 */
interface WifiScanner {
    /**
     * Запустить сканирование Wi-Fi сетей
     */
    suspend fun startScan(): Result<List<WifiScanResult>>
    
    /**
     * Получить поток результатов сканирования
     */
    fun getScanResultsFlow(): Flow<List<WifiScanResult>>
    
    /**
     * Проверить, включен ли Wi-Fi
     */
    fun isWifiEnabled(): Boolean
    
    /**
     * Получить текущую подключенную сеть
     */
    suspend fun getCurrentNetwork(): WifiScanResult?
    
    /**
     * Запустить непрерывное сканирование
     */
    fun startContinuousScan(intervalMs: Long = 30000): Flow<List<WifiScanResult>>
}

/**
 * Реализация Wi-Fi сканера
 */
@Singleton
class WifiScannerImpl @Inject constructor(
    private val context: Context
) : WifiScanner {
    
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val wifiCapabilitiesAnalyzer = WifiCapabilitiesAnalyzer()
    
    override suspend fun startScan(): Result<List<WifiScanResult>> = withContext(Dispatchers.IO) {
        try {
            if (!wifiManager.isWifiEnabled) {
                return@withContext Result.failure(Exception("Wi-Fi отключен"))
            }
            
            val success = wifiManager.startScan()
            if (!success) {
                return@withContext Result.failure(Exception("Не удалось запустить сканирование"))
            }
            
            // Ждем завершения сканирования
            delay(SCAN_TIMEOUT_MS)
            
            val scanResults = wifiManager.scanResults
            val wifiScanResults = scanResults.map { scanResult ->
                convertToWifiScanResult(scanResult)
            }
            
            Result.success(wifiScanResults)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getScanResultsFlow(): Flow<List<WifiScanResult>> = flow {
        try {
            if (!wifiManager.isWifiEnabled) {
                emit(emptyList())
                return@flow
            }
            
            val success = wifiManager.startScan()
            if (success) {
                delay(SCAN_TIMEOUT_MS)
                val scanResults = wifiManager.scanResults
                val wifiScanResults = scanResults.map { scanResult ->
                    convertToWifiScanResult(scanResult)
                }
                emit(wifiScanResults)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)
    
    override fun isWifiEnabled(): Boolean {
        return wifiManager.isWifiEnabled
    }
    
    override suspend fun getCurrentNetwork(): WifiScanResult? = withContext(Dispatchers.IO) {
        try {
            val wifiInfo = wifiManager.connectionInfo
            if (wifiInfo?.ssid != null && wifiInfo.ssid != "<unknown ssid>") {
                val ssid = wifiInfo.ssid.removeSurrounding("\"")
                val bssid = wifiInfo.bssid
                val rssi = wifiInfo.rssi
                
                // Найти соответствующий результат сканирования
                val scanResults = wifiManager.scanResults
                val matchingResult = scanResults.find { 
                    it.SSID == ssid && it.BSSID == bssid 
                }
                
                if (matchingResult != null) {
                    convertToWifiScanResult(matchingResult, isConnected = true)
                } else {
                    // Создать базовый результат для текущей сети
                    WifiScanResult(
                        ssid = ssid,
                        bssid = bssid ?: "unknown",
                        capabilities = "",
                        frequency = 0,
                        level = rssi,
                        isConnected = true,
                        securityType = SecurityType.UNKNOWN,
                        threatLevel = ThreatLevel.UNKNOWN
                    )
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun startContinuousScan(intervalMs: Long): Flow<List<WifiScanResult>> = flow {
        while (true) {
            try {
                if (wifiManager.isWifiEnabled) {
                    val success = wifiManager.startScan()
                    if (success) {
                        delay(SCAN_TIMEOUT_MS)
                        val scanResults = wifiManager.scanResults
                        val wifiScanResults = scanResults.map { scanResult ->
                            convertToWifiScanResult(scanResult)
                        }
                        emit(wifiScanResults)
                    }
                }
                delay(intervalMs)
            } catch (e: Exception) {
                emit(emptyList())
                delay(intervalMs)
            }
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Конвертировать ScanResult в WifiScanResult
     */
    private fun convertToWifiScanResult(
        scanResult: ScanResult,
        isConnected: Boolean = false
    ): WifiScanResult {
        val ssid = scanResult.SSID ?: "Hidden Network"
        val capabilities = scanResult.capabilities ?: ""
        
        val securityType = SecurityType.fromCapabilities(capabilities)
        val threatLevel = ThreatLevel.fromSecurityType(securityType)
        val wifiStandard = getWifiStandard(scanResult.frequency)
        
        return WifiScanResult(
            ssid = ssid,
            bssid = scanResult.BSSID ?: "unknown",
            capabilities = capabilities,
            frequency = scanResult.frequency,
            level = scanResult.level,
            timestamp = System.currentTimeMillis(),
            securityType = securityType,
            threatLevel = threatLevel,
            isConnected = isConnected,
            isHidden = ssid.isEmpty() || ssid == "Hidden Network",
            vendor = wifiCapabilitiesAnalyzer.getVendorFromBssid(scanResult.BSSID),
            channel = wifiCapabilitiesAnalyzer.getChannelFromFrequency(scanResult.frequency),
            standard = wifiStandard
        )
    }
    
    /**
     * Определить стандарт Wi-Fi по частоте
     */
    private fun getWifiStandard(frequency: Int): WifiStandard {
        return when {
            frequency in 2412..2484 -> WifiStandard.WIFI_2_4_GHZ
            frequency in 5170..5825 -> WifiStandard.WIFI_5_GHZ
            frequency in 5925..7125 -> WifiStandard.WIFI_6E
            else -> WifiStandard.UNKNOWN
        }
    }
    
    companion object {
        private const val SCAN_TIMEOUT_MS = 5000L
    }
}
