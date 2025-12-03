package com.wifiguard.core.data.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.wifiguard.core.common.Constants
import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.WifiScanResult
import com.wifiguard.feature.scanner.domain.model.WifiInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Сервис для сканирования Wi-Fi сетей с использованием Android WiFi API.
 * Обрабатывает реальные результаты сканирования и преобразует их в доменные модели.
 */
@Singleton
class WifiScannerService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wifiManager: WifiManager
) {
    companion object {
        private const val TAG = "${Constants.LOG_TAG}_WifiScanner"
    }
    
    /**
     * Запускает сканирование Wi-Fi сетей
     * @return true если сканирование успешно запущено
     */
    suspend fun startScan(): Boolean {
        Log.d("WifiGuardDebug", "WifiScannerService: Starting scan")
        return try {
            if (!wifiManager.isWifiEnabled) {
                Log.w(TAG, "WiFi отключен")
                Log.d("WifiGuardDebug", "WifiScannerService: WiFi is not enabled")
                return false
            }

            val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - результаты кэшируются системой, не запускаем активное сканирование
                Log.d(TAG, "Android 10+, активное сканирование ограничено, используем кеш")
                Log.d("WifiGuardDebug", "WifiScannerService: Android 10+, using system cache for scan")
                true  // Считаем, что сканирование "успешно" в контексте новых ограничений
            } else {
                // Android 9 и ниже - можем попытаться запустить сканирование
                Log.d("WifiGuardDebug", "WifiScannerService: Android 9 or lower, attempting active scan")
                @Suppress("DEPRECATION")
                wifiManager.startScan()
            }

            if (success) {
                Log.d(TAG, "Сканирование WiFi запущено успешно")
                Log.d("WifiGuardDebug", "WifiScannerService: WiFi scan started successfully")
            } else {
                Log.w(TAG, "Не удалось запустить сканирование WiFi")
                Log.d("WifiGuardDebug", "WifiScannerService: Failed to start WiFi scan")
            }

            success
        } catch (e: SecurityException) {
            Log.e(TAG, "Нет разрешений для сканирования WiFi", e)
            Log.e("WifiGuardDebug", "WifiScannerService: Security exception during scan: ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при запуске сканирования WiFi", e)
            Log.e("WifiGuardDebug", "WifiScannerService: Error during scan: ${e.message}", e)
            false
        }
    }
    
    /**
     * Получает результаты последнего сканирования как feature доменные модели
     * @return список результатов сканирования как WifiInfo
     */
    fun getScanResults(): List<WifiInfo> {
        return try {
            if (!wifiManager.isWifiEnabled) {
                Log.w(TAG, "WiFi отключен")
                return emptyList()
            }
            
            val scanResults = wifiManager.scanResults
            Log.d(TAG, "Получено ${scanResults.size} результатов сканирования")
            
            scanResults.mapNotNull { scanResult ->
                try {
                    scanResultToWifiInfo(scanResult)
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка преобразования результата сканирования: ${e.message}")
                    null
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Нет разрешений для получения результатов сканирования", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении результатов сканирования", e)
            emptyList()
        }
    }
    
    /**
     * Получает результаты последнего сканирования как core доменные модели
     * @return список результатов сканирования как WifiScanResult
     */
    fun getScanResultsAsCoreModels(): List<WifiScanResult> {
        Log.d("WifiGuardDebug", "WifiScannerService: Getting scan results as core models")
        return try {
            if (!wifiManager.isWifiEnabled) {
                Log.w(TAG, "WiFi отключен")
                Log.d("WifiGuardDebug", "WifiScannerService: WiFi is not enabled, returning empty list")
                return emptyList()
            }

            val scanResults = wifiManager.scanResults
            Log.d(TAG, "Получено ${scanResults.size} результатов сканирования")
            Log.d("WifiGuardDebug", "WifiScannerService: Got ${scanResults.size} scan results")

            if (scanResults.isEmpty()) {
                Log.d("WifiGuardDebug", "WifiScannerService: Scan results are empty - this may be due to Android background restrictions")
            }

            val mappedResults = scanResults.mapNotNull { scanResult ->
                try {
                    Log.d("WifiGuardDebug", "WifiScannerService: Processing scan result for ${scanResult.SSID}")
                    scanResultToWifiScanResult(scanResult)
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка преобразования результата сканирования: ${e.message}")
                    Log.e("WifiGuardDebug", "WifiScannerService: Error converting scan result: ${e.message}", e)
                    null
                }
            }

            Log.d("WifiGuardDebug", "WifiScannerService: Successfully converted ${mappedResults.size} scan results to core models")
            mappedResults
        } catch (e: SecurityException) {
            Log.e(TAG, "Нет разрешений для получения результатов сканирования", e)
            Log.e("WifiGuardDebug", "WifiScannerService: Security exception getting scan results: ${e.message}", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении результатов сканирования", e)
            Log.e("WifiGuardDebug", "WifiScannerService: Error getting scan results: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Наблюдает за результатами сканирования в реальном времени
     * @return Flow с результатами сканирования
     */
    fun observeScanResults(): Flow<List<WifiInfo>> = callbackFlow {
        Log.d("WifiGuardDebug", "WifiScannerService: Starting to observe scan results")
        val scanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                    val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                    Log.d("WifiGuardDebug", "WifiScannerService: Scan results available, success = $success")

                    if (success) {
                        Log.d(TAG, "Результаты сканирования обновлены")
                        Log.d("WifiGuardDebug", "WifiScannerService: Sending updated scan results")
                        val results = getScanResults()
                        Log.d("WifiGuardDebug", "WifiScannerService: Sending ${results.size} scan results to flow")
                        trySend(results)
                    } else {
                        Log.w(TAG, "Сканирование не удалось")
                        Log.d("WifiGuardDebug", "WifiScannerService: Scan failed, sending empty list")
                        trySend(emptyList())
                    }
                }
            }
        }

        // Регистрируем receiver
        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        Log.d("WifiGuardDebug", "WifiScannerService: Registering scan results receiver")
        context.registerReceiver(scanReceiver, intentFilter)

        // Отправляем текущие результаты
        val currentResults = getScanResults()
        Log.d("WifiGuardDebug", "WifiScannerService: Sending initial ${currentResults.size} scan results")
        trySend(currentResults)

        awaitClose {
            try {
                Log.d("WifiGuardDebug", "WifiScannerService: Unregistering scan results receiver")
                context.unregisterReceiver(scanReceiver)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при отмене регистрации receiver", e)
                Log.e("WifiGuardDebug", "WifiScannerService: Error unregistering receiver: ${e.message}", e)
            }
        }
    }
    
    /**
     * Получает информацию о текущей подключенной сети
     * @return информация о подключенной сети или null
     */
    fun getCurrentNetwork(): WifiInfo? {
        return try {
            if (!wifiManager.isWifiEnabled) {
                return null
            }
            
            // В Android 29+ connectionInfo устарел из-за соображений конфиденциальности
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - получаем только минимально необходимую информацию без устаревшего API
                // Получаем информацию через ConnectivityManager
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                val activeNetwork = connectivityManager.activeNetwork
                val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
                
                if (caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true) {
                    // На Android 10+ мы не можем получить полную информацию о сети напрямую
                    // Возвращаем минимально возможную информацию
                    WifiInfo(
                        ssid = "Current Network", // Не можем получить точный SSID
                        bssid = "unknown", // Не можем получить точный BSSID
                        capabilities = "",
                        level = -1, // Не можем получить точный уровень сигнала
                        frequency = 2400, // Значение по умолчанию
                        timestamp = System.currentTimeMillis(),
                        encryptionType = com.wifiguard.feature.scanner.domain.model.EncryptionType.UNKNOWN,
                        signalStrength = -1,
                        channel = 0,
                        bandwidth = null,
                        isHidden = false,
                        isConnected = true
                    )
                } else {
                    null
                }
            } else {
                // Android 9 и ниже - можем использовать устаревший, но работающий API
                @Suppress("DEPRECATION")
                val connectionInfo = wifiManager.connectionInfo
                if (connectionInfo.networkId == -1) {
                    return null
                }
                
                val ssid = connectionInfo.ssid.removeSurrounding("\"")
                val bssid = connectionInfo.bssid ?: return null
                val rssi = connectionInfo.rssi
                val frequency = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    connectionInfo.frequency
                } else {
                    2400 // Default to 2.4GHz for older devices
                }
                
                WifiInfo(
                    ssid = ssid,
                    bssid = bssid,
                    capabilities = "",
                    level = rssi,
                    frequency = frequency,
                    timestamp = System.currentTimeMillis(),
                    encryptionType = com.wifiguard.feature.scanner.domain.model.EncryptionType.UNKNOWN,
                    signalStrength = rssi,
                    channel = getChannelFromFrequency(frequency),
                    bandwidth = null,
                    isHidden = false,
                    isConnected = true
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Нет разрешений для получения информации о сети", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении информации о текущей сети", e)
            null
        }
    }
    
    /**
     * Проверяет, включен ли WiFi
     * @return true если WiFi включен
     */
    fun isWifiEnabled(): Boolean {
        return try {
            wifiManager.isWifiEnabled
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при проверке состояния WiFi", e)
            false
        }
    }
    
    /**
     * Преобразует Android ScanResult в WifiInfo
     */
    private fun scanResultToWifiInfo(scanResult: ScanResult): WifiInfo {
        val encryptionType = parseEncryptionType(scanResult.capabilities)
        val frequency = scanResult.frequency
        val channel = getChannelFromFrequency(frequency)
        val bandwidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when (scanResult.channelWidth) {
                ScanResult.CHANNEL_WIDTH_20MHZ -> "20 MHz"
                ScanResult.CHANNEL_WIDTH_40MHZ -> "40 MHz"
                ScanResult.CHANNEL_WIDTH_80MHZ -> "80 MHz"
                ScanResult.CHANNEL_WIDTH_160MHZ -> "160 MHz"
                ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> "80+80 MHz"
                else -> null
            }
        } else {
            null
        }
        
        // В Android 13+ SSID и BSSID устарели, используем безопасные методы в зависимости от версии
        val ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                // Android 13+ - используем новые безопасные методы (если доступны)
                scanResult.wifiSsid?.toString() ?: ""
            } catch (e: Exception) {
                ""
            }
        } else {
            // Android 12 и ниже - можем использовать устаревший, но работающий API
            @Suppress("DEPRECATION")
            scanResult.SSID ?: ""
        }
        
        // Используем BSSID с учетом версии Android
        val bssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            @Suppress("DEPRECATION")
            scanResult.BSSID ?: ""
        } else {
            @Suppress("DEPRECATION")
            scanResult.BSSID ?: ""
        }
        
        return WifiInfo(
            ssid = ssid,
            bssid = bssid,
            capabilities = scanResult.capabilities ?: "",
            level = scanResult.level,
            frequency = frequency,
            timestamp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                scanResult.timestamp / 1000 // Convert microseconds to milliseconds
            } else {
                System.currentTimeMillis()
            },
            encryptionType = encryptionType,
            signalStrength = scanResult.level,
            channel = channel,
            bandwidth = bandwidth,
            isHidden = ssid.isNullOrEmpty()
        )
    }
    
    /**
     * Преобразует Android ScanResult в WifiScanResult (для сохранения в БД)
     */
    fun scanResultToWifiScanResult(
        scanResult: ScanResult,
        scanType: com.wifiguard.core.domain.model.ScanType = com.wifiguard.core.domain.model.ScanType.MANUAL
    ): WifiScanResult {
        val encryptionType = parseEncryptionType(scanResult.capabilities)
        val frequency = scanResult.frequency
        val channel = getChannelFromFrequency(frequency)
        
        // В Android 13+ SSID и BSSID устарели, используем безопасные методы в зависимости от версии
        val ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                // Android 13+ - используем новые безопасные методы (если доступны)
                scanResult.wifiSsid?.toString() ?: ""
            } catch (e: Exception) {
                ""
            }
        } else {
            // Android 12 и ниже - можем использовать устаревший, но работающий API
            @Suppress("DEPRECATION")
            scanResult.SSID ?: ""
        }
        
        val securityType = mapEncryptionTypeToSecurityType(encryptionType)
        
        // Используем BSSID с учетом версии Android
        val bssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            @Suppress("DEPRECATION")
            scanResult.BSSID ?: ""
        } else {
            @Suppress("DEPRECATION")
            scanResult.BSSID ?: ""
        }
        
        return WifiScanResult(
            ssid = ssid,
            bssid = bssid,
            capabilities = scanResult.capabilities ?: "",
            frequency = frequency,
            level = scanResult.level,
            timestamp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                scanResult.timestamp / 1000
            } else {
                System.currentTimeMillis()
            },
            scanType = scanType,
            securityType = securityType,
            channel = channel
        )
    }
    
    /**
     * Определяет тип шифрования по строке capabilities
     */
    private fun parseEncryptionType(capabilities: String): com.wifiguard.feature.scanner.domain.model.EncryptionType {
        return when {
            capabilities.contains("WPA3", ignoreCase = true) -> 
                com.wifiguard.feature.scanner.domain.model.EncryptionType.WPA3
            capabilities.contains("WPA2", ignoreCase = true) -> 
                com.wifiguard.feature.scanner.domain.model.EncryptionType.WPA2
            capabilities.contains("WPA", ignoreCase = true) && !capabilities.contains("WPA2", ignoreCase = true) -> 
                com.wifiguard.feature.scanner.domain.model.EncryptionType.WPA
            capabilities.contains("WEP", ignoreCase = true) -> 
                com.wifiguard.feature.scanner.domain.model.EncryptionType.WEP
            capabilities.contains("WPS", ignoreCase = true) -> 
                com.wifiguard.feature.scanner.domain.model.EncryptionType.WPS
            capabilities.isEmpty() || capabilities.contains("[ESS]") -> 
                com.wifiguard.feature.scanner.domain.model.EncryptionType.NONE
            else -> 
                com.wifiguard.feature.scanner.domain.model.EncryptionType.UNKNOWN
        }
    }
    
    /**
     * Вычисляет номер канала по частоте
     */
    private fun getChannelFromFrequency(frequency: Int): Int {
        return when {
            // 2.4 GHz band
            frequency in 2412..2484 -> {
                when (frequency) {
                    2412 -> 1
                    2417 -> 2
                    2422 -> 3
                    2427 -> 4
                    2432 -> 5
                    2437 -> 6
                    2442 -> 7
                    2447 -> 8
                    2452 -> 9
                    2457 -> 10
                    2462 -> 11
                    2467 -> 12
                    2472 -> 13
                    2484 -> 14
                    else -> ((frequency - 2412) / 5) + 1
                }
            }
            // 5 GHz band
            frequency in 5000..5900 -> {
                (frequency - 5000) / 5
            }
            // 6 GHz band (WiFi 6E)
            frequency in 5955..7115 -> {
                (frequency - 5955) / 5
            }
            else -> 0
        }
    }
    
    /** 
     * Отображает тип шифрования из feature модуля в core тип безопасности
     */
    private fun mapEncryptionTypeToSecurityType(encryptionType: com.wifiguard.feature.scanner.domain.model.EncryptionType): com.wifiguard.core.domain.model.SecurityType {
        return when (encryptionType) {
            com.wifiguard.feature.scanner.domain.model.EncryptionType.NONE -> com.wifiguard.core.domain.model.SecurityType.OPEN
            com.wifiguard.feature.scanner.domain.model.EncryptionType.WEP -> com.wifiguard.core.domain.model.SecurityType.WEP
            com.wifiguard.feature.scanner.domain.model.EncryptionType.WPA -> com.wifiguard.core.domain.model.SecurityType.WPA
            com.wifiguard.feature.scanner.domain.model.EncryptionType.WPA2 -> com.wifiguard.core.domain.model.SecurityType.WPA2
            com.wifiguard.feature.scanner.domain.model.EncryptionType.WPA3 -> com.wifiguard.core.domain.model.SecurityType.WPA3
            com.wifiguard.feature.scanner.domain.model.EncryptionType.WPS -> com.wifiguard.core.domain.model.SecurityType.UNKNOWN
            com.wifiguard.feature.scanner.domain.model.EncryptionType.UNKNOWN -> com.wifiguard.core.domain.model.SecurityType.UNKNOWN
        }
    }
}