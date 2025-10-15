package com.wifiguard.core.data.wifi

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.wifiguard.core.common.Logger
import com.wifiguard.core.common.logd
import com.wifiguard.core.common.loge
import com.wifiguard.core.common.logw
import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.domain.model.WifiScanResult
import com.wifiguard.core.domain.model.WifiStandard
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiScannerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : WifiScanner {
    
    private val wifiManager: WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    /**
     * Проверяет включен ли Wi-Fi
     */
    override fun isWifiEnabled(): Boolean {
        logd("Checking WiFi enabled status")
        return wifiManager.isWifiEnabled
    }
    
    /**
     * Запрашивает включение Wi-Fi
     * На Android 10+ открывает системную панель настроек
     */
    fun requestEnableWifi(context: Context) {
        logd("Requesting WiFi enable")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ - показать панель настроек
            val panelIntent = Intent(Settings.Panel.ACTION_WIFI)
            panelIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(panelIntent)
        } else {
            // Android 9 и ниже - программное включение
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = true
        }
    }
    
    override suspend fun startScan(): Result<List<WifiScanResult>> = withContext(Dispatchers.IO) {
        try {
            logd("Starting WiFi scan")
            
            // Проверка разрешений
            if (!hasLocationPermission()) {
                loge("Location permission not granted for WiFi scan")
                return@withContext Result.failure(SecurityException("Требуется разрешение ACCESS_FINE_LOCATION"))
            }
            
            if (!isWifiEnabled()) {
                loge("WiFi is disabled, cannot start scan")
                return@withContext Result.failure(IllegalStateException("Wi-Fi выключен. Включите Wi-Fi для сканирования."))
            }
            
            val success = wifiManager.startScan()
            if (!success) {
                logw("Failed to start WiFi scan, using cached results")
                // Если не удалось запустить сканирование, возвращаем кешированные результаты
                val cachedResults = getScanResults()
                return@withContext Result.success(cachedResults)
            }
            
            // Ждем завершения сканирования (на Android 9+ результаты могут быть кешированными)
            logd("Waiting for scan results...")
            delay(SCAN_TIMEOUT_MS)
            
            val scanResults = getScanResults()
            logd("Scan completed, found ${scanResults.size} networks")
            Result.success(scanResults)
        } catch (e: SecurityException) {
            loge("Security exception during WiFi scan", e)
            Result.failure(e)
        } catch (e: Exception) {
            loge("Exception during WiFi scan", e)
            Result.failure(e)
        }
    }
    
    override fun getScanResultsFlow(): Flow<List<WifiScanResult>> = callbackFlow {
        logd("Creating scan results flow")
        
        // Проверка разрешений
        if (!hasLocationPermission()) {
            loge("Location permission not granted for scan flow")
            close(SecurityException("Требуется разрешение ACCESS_FINE_LOCATION"))
            return@callbackFlow
        }
        
        if (!isWifiEnabled()) {
            loge("WiFi is disabled for scan flow")
            close(IllegalStateException("Wi-Fi выключен. Включите Wi-Fi для сканирования."))
            return@callbackFlow
        }
        
        val scanResultsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    WifiManager.SCAN_RESULTS_AVAILABLE_ACTION -> {
                        val success = intent.getBooleanExtra(
                            WifiManager.EXTRA_RESULTS_UPDATED,
                            false
                        )
                        
                        logd("Received scan results, success=$success")
                        
                        if (success) {
                            val results = getScanResults()
                            trySend(results)
                        } else {
                            // Сканирование не удалось, используем кешированные результаты
                            val cachedResults = getScanResults()
                            trySend(cachedResults)
                        }
                    }
                }
            }
        }
        
        // Регистрируем receiver
        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(scanResultsReceiver, intentFilter)
        
        // Запускаем сканирование
        val scanStarted = wifiManager.startScan()
        
        if (!scanStarted) {
            // Если не удалось запустить сканирование, отправляем кешированные результаты
            val cachedResults = getScanResults()
            trySend(cachedResults)
        }
        
        awaitClose {
            try {
                context.unregisterReceiver(scanResultsReceiver)
                logd("Unregistered scan results receiver")
            } catch (e: IllegalArgumentException) {
                // Receiver уже был удален
                logw("Scan results receiver already unregistered")
            }
        }
    }
    
    /**
     * Получает результаты последнего сканирования
     * ВАЖНО: На Android 9+ результаты могут быть кешированными (до 2 минут)
     */
    @Suppress("DEPRECATION")
    private fun getScanResults(): List<WifiScanResult> {
        if (!hasLocationPermission()) {
            logw("No location permission to get scan results")
            return emptyList()
        }
        
        return try {
            logd("Getting scan results from WiFi manager")
            wifiManager.scanResults.map { result ->
                convertToWifiScanResult(result)
            }.filter { network ->
                // Фильтруем пустые SSID и скрытые сети
                network.ssid.isNotBlank() && network.ssid != "<unknown ssid>"
            }
        } catch (e: SecurityException) {
            // Нет разрешения
            loge("Security exception getting scan results", e)
            emptyList()
        } catch (e: Exception) {
            // Другая ошибка
            loge("Exception getting scan results", e)
            emptyList()
        }
    }
    
    override fun startContinuousScan(intervalMs: Long): Flow<List<WifiScanResult>> = flow {
        logd("Starting continuous scan with interval $intervalMs ms")
        while (true) {
            try {
                if (isWifiEnabled() && hasLocationPermission()) {
                    val result = startScan()
                    if (result.isSuccess) {
                        emit(result.getOrNull() ?: emptyList())
                    } else {
                        emit(emptyList())
                    }
                }
                delay(intervalMs)
            } catch (e: Exception) {
                loge("Exception in continuous scan", e)
                emit(emptyList())
                delay(intervalMs)
            }
        }
    }.flowOn(Dispatchers.IO)
    
    override suspend fun getCurrentNetwork(): WifiScanResult? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) {
            logw("No location permission to get current network")
            return@withContext null
        }
        
        try {
            logd("Getting current network info")
            val wifiInfo = wifiManager.connectionInfo
            if (wifiInfo.networkId == -1) {
                logd("No network currently connected")
                null
            } else {
                val ssid = wifiInfo.ssid.removeSurrounding("\"")
                val bssid = wifiInfo.bssid
                val rssi = wifiInfo.rssi
                val frequency = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    wifiInfo.frequency
                } else {
                    0
                }
                
                logd("Connected to network: $ssid ($bssid)")
                
                // Find the corresponding scan result to get more details
                val scanResults = getScanResults()
                val matchingResult = scanResults.find { 
                    it.ssid == ssid && it.bssid == bssid 
                }
                
                matchingResult ?: WifiScanResult(
                    ssid = ssid,
                    bssid = bssid ?: "unknown",
                    capabilities = "",
                    frequency = frequency,
                    level = rssi,
                    timestamp = System.currentTimeMillis(),
                    securityType = SecurityType.UNKNOWN,
                    threatLevel = ThreatLevel.UNKNOWN,
                    isConnected = true,
                    isHidden = ssid.isEmpty() || ssid == "<unknown ssid>",
                    vendor = null,
                    channel = 0,
                    standard = WifiStandard.UNKNOWN
                )
            }
        } catch (e: Exception) {
            loge("Exception getting current network", e)
            null
        }
    }
    
    /**
     * Определяет тип безопасности сети
     */
    private fun determineSecurityType(capabilities: String): SecurityType {
        return when {
            capabilities.contains("WPA3", ignoreCase = true) -> SecurityType.WPA3
            capabilities.contains("WPA2", ignoreCase = true) -> SecurityType.WPA2
            capabilities.contains("WPA", ignoreCase = true) -> SecurityType.WPA
            capabilities.contains("WEP", ignoreCase = true) -> SecurityType.WEP
            capabilities.contains("EAP", ignoreCase = true) -> SecurityType.EAP
            else -> SecurityType.OPEN
        }
    }
    
    /**
     * Проверяет наличие разрешения на местоположение
     */
    private fun hasLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 6-12
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Конвертировать ScanResult в WifiScanResult
     */
    private fun convertToWifiScanResult(
        scanResult: android.net.wifi.ScanResult,
        isConnected: Boolean = false
    ): WifiScanResult {
        val ssid = scanResult.SSID ?: "Hidden Network"
        val capabilities = scanResult.capabilities ?: ""
        
        val securityType = determineSecurityType(capabilities)
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
            vendor = WifiCapabilitiesAnalyzer().getVendorFromBssid(scanResult.BSSID),
            channel = WifiCapabilitiesAnalyzer().getChannelFromFrequency(scanResult.frequency),
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