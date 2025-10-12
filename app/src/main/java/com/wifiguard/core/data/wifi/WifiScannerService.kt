package com.wifiguard.core.data.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
        return try {
            if (!wifiManager.isWifiEnabled) {
                Log.w(TAG, "WiFi отключен")
                return false
            }
            
            val success = wifiManager.startScan()
            
            if (success) {
                Log.d(TAG, "Сканирование WiFi запущено успешно")
            } else {
                Log.w(TAG, "Не удалось запустить сканирование WiFi")
            }
            
            success
        } catch (e: SecurityException) {
            Log.e(TAG, "Нет разрешений для сканирования WiFi", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при запуске сканирования WiFi", e)
            false
        }
    }
    
    /**
     * Получает результаты последнего сканирования
     * @return список результатов сканирования
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
     * Наблюдает за результатами сканирования в реальном времени
     * @return Flow с результатами сканирования
     */
    fun observeScanResults(): Flow<List<WifiInfo>> = callbackFlow {
        val scanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                    val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                    
                    if (success) {
                        Log.d(TAG, "Результаты сканирования обновлены")
                        val results = getScanResults()
                        trySend(results)
                    } else {
                        Log.w(TAG, "Сканирование не удалось")
                        trySend(emptyList())
                    }
                }
            }
        }
        
        // Регистрируем receiver
        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(scanReceiver, intentFilter)
        
        // Отправляем текущие результаты
        val currentResults = getScanResults()
        trySend(currentResults)
        
        awaitClose {
            try {
                context.unregisterReceiver(scanReceiver)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при отмене регистрации receiver", e)
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
        
        return WifiInfo(
            ssid = scanResult.SSID ?: "",
            bssid = scanResult.BSSID ?: "",
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
            isHidden = scanResult.SSID.isNullOrEmpty()
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
        
        return WifiScanResult(
            ssid = scanResult.SSID ?: "",
            bssid = scanResult.BSSID ?: "",
            signalStrength = scanResult.level,
            frequency = frequency,
            channel = channel,
            timestamp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                scanResult.timestamp / 1000
            } else {
                System.currentTimeMillis()
            },
            scanType = scanType,
            securityType = encryptionType
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
}


