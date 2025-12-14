package com.wifiguard.core.data.wifi

import android.Manifest
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.wifiguard.core.common.Constants
import com.wifiguard.core.common.DeviceDebugLogger
import com.wifiguard.core.domain.model.Freshness
import com.wifiguard.core.domain.model.ScanMetadata
import com.wifiguard.core.domain.model.ScanSource
import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.WifiScanResult
import com.wifiguard.core.domain.model.WifiScanStatus
import com.wifiguard.feature.scanner.domain.model.WifiInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicLong

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
        
        // Константы для throttling
        private const val FOREGROUND_THROTTLE_INTERVAL = 30_000L // 30 секунд
        private const val BACKGROUND_THROTTLE_INTERVAL = 1_800_000L // 30 минут
        
        // Константы для свежести данных
        private const val FRESH_DATA_THRESHOLD = 300_000L // 5 минут
        private const val STALE_DATA_THRESHOLD = 1_800_000L // 30 минут
    }
    
    // Время последнего запроса на активное сканирование (startScan)
    private val lastScanRequestTime = AtomicLong(0L)

    // Время последнего получения обновлённых результатов (EXTRA_RESULTS_UPDATED = true)
    private val lastResultsUpdateTime = AtomicLong(0L)

    // Сериализация startScan() для предотвращения гонок при одновременных вызовах (Worker/Service/UI)
    private val scanMutex = Mutex()
    
    /**
     * Запускает сканирование Wi-Fi сетей с учетом ограничений Android 10+
     * @return статус сканирования (Success, Throttled, Restricted, Failed)
     */
    suspend fun startScan(): WifiScanStatus {
        Log.d("WifiGuardDebug", "WifiScannerService: Starting scan")
        val runId = DeviceDebugLogger.currentRunId()
        return scanMutex.withLock {
            try {
            if (!wifiManager.isWifiEnabled) {
                Log.w(TAG, "WiFi отключен")
                Log.d("WifiGuardDebug", "WifiScannerService: WiFi is not enabled")
                DeviceDebugLogger.log(
                    context = context,
                    runId = runId,
                    hypothesisId = "A",
                    location = "WifiScannerService.kt:startScan",
                    message = "Старт скана: WiFi выключен",
                    data = org.json.JSONObject().apply {
                        put("sdkInt", Build.VERSION.SDK_INT)
                        put("locationEnabled", DeviceDebugLogger.isLocationEnabled(context))
                    }
                )
                return WifiScanStatus.Failed("WiFi is not enabled")
            }

            // На части устройств (OEM) startScan/scanResults ограничены при выключенной геолокации.
            if (!DeviceDebugLogger.isLocationEnabled(context)) {
                DeviceDebugLogger.log(
                    context = context,
                    runId = runId,
                    hypothesisId = "A",
                    location = "WifiScannerService.kt:startScan",
                    message = "Старт скана: геолокация выключена (restricted)",
                    data = org.json.JSONObject().apply {
                        put("sdkInt", Build.VERSION.SDK_INT)
                        put("locationEnabled", false)
                    }
                )
                return WifiScanStatus.Restricted("Геолокация выключена")
            }

            val currentTime = System.currentTimeMillis()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+: проверяем ограничения throttling
                val lastRequest = lastScanRequestTime.get()
                val timeSinceLastScan = if (lastRequest > 0) currentTime - lastRequest else Long.MAX_VALUE
                val isInForeground = isAppInForeground()
                val throttleInterval = if (isInForeground) {
                    FOREGROUND_THROTTLE_INTERVAL // 30 секунд для foreground (4 раза за 2 минуты)
                } else {
                    BACKGROUND_THROTTLE_INTERVAL // 30 минут для background
                }
                
                Log.d(TAG, "Android 10+: timeSinceLastScan=$timeSinceLastScan, throttleInterval=$throttleInterval, isInForeground=$isInForeground")
                
                if (timeSinceLastScan < throttleInterval && lastRequest > 0) {
                    val nextAvailableTime = lastRequest + throttleInterval
                    Log.w(TAG, "Scan throttled. Next available in: ${throttleInterval - timeSinceLastScan}ms")
                    Log.d("WifiGuardDebug", "WifiScannerService: Scan throttled, next available at $nextAvailableTime")
                    return WifiScanStatus.Throttled(nextAvailableTime)
                }
                
                // Попытка запустить сканирование
                @Suppress("DEPRECATION")
                val scanStarted = wifiManager.startScan()
                
                if (scanStarted) {
                    lastScanRequestTime.set(currentTime)
                    // Оптимистично считаем результаты "свежими" сразу после успешного запуска.
                    // Реальное обновление также фиксируется в BroadcastReceiver (EXTRA_RESULTS_UPDATED = true).
                    lastResultsUpdateTime.set(currentTime)
                    Log.d(TAG, "Сканирование WiFi запущено успешно (Android 10+)")
                    Log.d("WifiGuardDebug", "WifiScannerService: WiFi scan started successfully")
                    DeviceDebugLogger.log(
                        context = context,
                        runId = runId,
                        hypothesisId = "A",
                        location = "WifiScannerService.kt:startScan",
                        message = "Старт скана: startScan()=true",
                        data = org.json.JSONObject().apply {
                            put("sdkInt", Build.VERSION.SDK_INT)
                            put("isInForeground", isInForeground)
                            put("throttleIntervalMs", throttleInterval)
                            put("timeSinceLastScanMs", timeSinceLastScan)
                            put("locationEnabled", DeviceDebugLogger.isLocationEnabled(context))
                        }
                    )
                    return WifiScanStatus.Success(currentTime)
                } else {
                    Log.w(TAG, "Android 10+ background scan restricted")
                    Log.d("WifiGuardDebug", "WifiScannerService: Scan restricted on Android 10+")
                    DeviceDebugLogger.log(
                        context = context,
                        runId = runId,
                        hypothesisId = "A",
                        location = "WifiScannerService.kt:startScan",
                        message = "Старт скана: startScan()=false (restricted)",
                        data = org.json.JSONObject().apply {
                            put("sdkInt", Build.VERSION.SDK_INT)
                            put("isInForeground", isInForeground)
                            put("locationEnabled", DeviceDebugLogger.isLocationEnabled(context))
                        }
                    )
                    return WifiScanStatus.Restricted(
                        "Android 10+ background scan restricted. Use foreground service or wait for throttle interval."
                    )
                }
            } else {
                // Android 9 и ниже: стандартное сканирование
                Log.d("WifiGuardDebug", "WifiScannerService: Android 9 or lower, attempting active scan")
                @Suppress("DEPRECATION")
                val scanStarted = wifiManager.startScan()
                
                if (scanStarted) {
                    lastScanRequestTime.set(currentTime)
                    lastResultsUpdateTime.set(currentTime)
                    Log.d(TAG, "Сканирование WiFi запущено успешно")
                    Log.d("WifiGuardDebug", "WifiScannerService: WiFi scan started successfully")
                    DeviceDebugLogger.log(
                        context = context,
                        runId = runId,
                        hypothesisId = "A",
                        location = "WifiScannerService.kt:startScan",
                        message = "Старт скана (до Android 10): startScan()=true",
                        data = org.json.JSONObject().apply {
                            put("sdkInt", Build.VERSION.SDK_INT)
                            put("locationEnabled", DeviceDebugLogger.isLocationEnabled(context))
                        }
                    )
                    return WifiScanStatus.Success(currentTime)
                } else {
                    Log.w(TAG, "Не удалось запустить сканирование WiFi")
                    Log.d("WifiGuardDebug", "WifiScannerService: Failed to start WiFi scan")
                    DeviceDebugLogger.log(
                        context = context,
                        runId = runId,
                        hypothesisId = "A",
                        location = "WifiScannerService.kt:startScan",
                        message = "Старт скана (до Android 10): startScan()=false",
                        data = org.json.JSONObject().apply {
                            put("sdkInt", Build.VERSION.SDK_INT)
                            put("locationEnabled", DeviceDebugLogger.isLocationEnabled(context))
                        }
                    )
                    return WifiScanStatus.Failed("WifiManager.startScan() returned false")
                }
            }
            } catch (e: SecurityException) {
            Log.e(TAG, "Нет разрешений для сканирования WiFi", e)
            Log.e("WifiGuardDebug", "WifiScannerService: Security exception during scan: ${e.message}", e)
            DeviceDebugLogger.log(
                context = context,
                runId = runId,
                hypothesisId = "C",
                location = "WifiScannerService.kt:startScan",
                message = "SecurityException при startScan()",
                data = org.json.JSONObject().apply {
                    put("sdkInt", Build.VERSION.SDK_INT)
                    put("error", e.message ?: "unknown")
                    put("locationEnabled", DeviceDebugLogger.isLocationEnabled(context))
                }
            )
            WifiScanStatus.Failed("Security exception: ${e.message}")
            } catch (e: Exception) {
            Log.e(TAG, "Ошибка при запуске сканирования WiFi", e)
            Log.e("WifiGuardDebug", "WifiScannerService: Error during scan: ${e.message}", e)
            DeviceDebugLogger.log(
                context = context,
                runId = runId,
                hypothesisId = "D",
                location = "WifiScannerService.kt:startScan",
                message = "Exception при startScan()",
                data = org.json.JSONObject().apply {
                    put("sdkInt", Build.VERSION.SDK_INT)
                    put("errorType", e.javaClass.simpleName)
                    put("error", e.message ?: "unknown")
                    put("locationEnabled", DeviceDebugLogger.isLocationEnabled(context))
                }
            )
            WifiScanStatus.Failed("Exception: ${e.message}")
            }
        }
    }
    
    /**
     * Проверяет, находится ли приложение в foreground
     */
    private fun isAppInForeground(): Boolean {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val appProcesses = activityManager.runningAppProcesses ?: return false
            appProcesses.any { 
                it.processName == context.packageName && 
                (
                    it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND ||
                    it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE ||
                    it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при проверке foreground статуса", e)
            false
        }
    }
    
    /**
     * Получает результаты сканирования с метаданными о свежести данных
     * @return пара: список сетей и метаданные сканирования
     */
    fun getScanResultsWithMetadata(): Pair<List<WifiScanResult>, ScanMetadata> {
        Log.d("WifiGuardDebug", "WifiScannerService: Getting scan results with metadata")
        
        val scanResults = getScanResultsAsCoreModels()
        val currentTime = System.currentTimeMillis()
        val lastUpdated = lastResultsUpdateTime.get()
        val lastRequested = lastScanRequestTime.get()
        val baseTime = when {
            lastUpdated > 0 -> lastUpdated
            lastRequested > 0 -> lastRequested
            else -> 0L
        }
        val age = if (baseTime > 0) currentTime - baseTime else Long.MAX_VALUE
        
        val freshness = when {
            age < FRESH_DATA_THRESHOLD -> Freshness.FRESH      // < 5 минут
            age < STALE_DATA_THRESHOLD -> Freshness.STALE     // 5-30 минут
            else -> Freshness.EXPIRED                          // > 30 минут
        }
        
        val source = if (lastUpdated > 0 && (currentTime - lastUpdated) < 60_000L) {
            ScanSource.ACTIVE_SCAN
        } else {
            ScanSource.SYSTEM_CACHE
        }
        
        val metadata = ScanMetadata(
            timestamp = if (baseTime > 0) baseTime else currentTime,
            source = source,
            freshness = freshness
        )
        
        Log.d(TAG, "Scan metadata: age=${age}ms, freshness=$freshness, source=$source")
        Log.d("WifiGuardDebug", "WifiScannerService: Metadata - age=${age}ms, freshness=$freshness, source=$source")
        
        return Pair(scanResults, metadata)
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
     * ИСПРАВЛЕНО: Добавлена проверка разрешений и улучшена обработка ошибок
     */
    fun getScanResultsAsCoreModels(): List<WifiScanResult> {
        Log.d("WifiGuardDebug", "WifiScannerService: Getting scan results as core models")
        val runId = DeviceDebugLogger.currentRunId()
        
        // #region agent log
        try {
            val logJson = org.json.JSONObject().apply {
                put("sessionId", "debug-session")
                put("runId", runId)
                put("hypothesisId", "B")
                put("location", "WifiScannerService.kt:245")
                put("message", "Начало получения результатов сканирования как core models")
                put("data", org.json.JSONObject().apply {
                    put("wifiEnabled", wifiManager.isWifiEnabled)
                    put("sdkVersion", Build.VERSION.SDK_INT)
                })
                put("timestamp", System.currentTimeMillis())
            }
            java.io.File("/Users/mint1024/Desktop/андроид/.cursor/debug.log").appendText("${logJson}\n")
        } catch (e: Exception) {}
        // #endregion
        
        return try {
            if (!wifiManager.isWifiEnabled) {
                Log.w(TAG, "WiFi отключен")
                Log.d("WifiGuardDebug", "WifiScannerService: WiFi is not enabled, returning empty list")
                DeviceDebugLogger.log(
                    context = context,
                    runId = runId,
                    hypothesisId = "B",
                    location = "WifiScannerService.kt:getScanResultsAsCoreModels",
                    message = "scanResults: WiFi выключен -> пусто",
                    data = org.json.JSONObject().apply {
                        put("sdkInt", Build.VERSION.SDK_INT)
                        put("locationEnabled", DeviceDebugLogger.isLocationEnabled(context))
                    }
                )
                // #region agent log
                try {
                    val logJson = org.json.JSONObject().apply {
                        put("sessionId", "debug-session")
                        put("runId", runId)
                        put("hypothesisId", "B")
                        put("location", "WifiScannerService.kt:264")
                        put("message", "WiFi отключен, возвращаем пустой список")
                        put("data", org.json.JSONObject())
                        put("timestamp", System.currentTimeMillis())
                    }
                    java.io.File("/Users/mint1024/Desktop/андроид/.cursor/debug.log").appendText("${logJson}\n")
                } catch (e: Exception) {}
                // #endregion
                return emptyList()
            }

            // ИСПРАВЛЕНО: Проверяем разрешения перед доступом к scanResults
            val hasPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PackageManager.PERMISSION_GRANTED == 
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) &&
                PackageManager.PERMISSION_GRANTED == 
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.NEARBY_WIFI_DEVICES
                    )
            } else {
                PackageManager.PERMISSION_GRANTED == 
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
            }
            
            if (!hasPermissions) {
                Log.w(TAG, "Нет разрешений для получения результатов сканирования")
                // #region agent log
                try {
                    val logJson = org.json.JSONObject().apply {
                        put("sessionId", "debug-session")
                        put("runId", runId)
                        put("hypothesisId", "C")
                        put("location", "WifiScannerService.kt:295")
                        put("message", "Нет разрешений для получения результатов сканирования")
                        put("data", org.json.JSONObject().apply {
                            put("sdkVersion", Build.VERSION.SDK_INT)
                        })
                        put("timestamp", System.currentTimeMillis())
                    }
                    java.io.File("/Users/mint1024/Desktop/андроид/.cursor/debug.log").appendText("${logJson}\n")
                } catch (e: Exception) {}
                // #endregion
                return emptyList()
            }

            val scanResults = wifiManager.scanResults
            Log.d(TAG, "Получено ${scanResults.size} результатов сканирования")
            Log.d("WifiGuardDebug", "WifiScannerService: Got ${scanResults.size} scan results")
            DeviceDebugLogger.log(
                context = context,
                runId = runId,
                hypothesisId = "B",
                location = "WifiScannerService.kt:getScanResultsAsCoreModels",
                message = "scanResults получены",
                data = org.json.JSONObject().apply {
                    put("sdkInt", Build.VERSION.SDK_INT)
                    put("rawResultsCount", scanResults.size)
                    put("locationEnabled", DeviceDebugLogger.isLocationEnabled(context))
                }
            )
            
            // #region agent log
            try {
                val logJson = org.json.JSONObject().apply {
                    put("sessionId", "debug-session")
                    put("runId", runId)
                    put("hypothesisId", "B")
                    put("location", "WifiScannerService.kt:315")
                    put("message", "Получены raw результаты сканирования из WifiManager")
                    put("data", org.json.JSONObject().apply {
                        put("rawResultsCount", scanResults.size)
                    })
                    put("timestamp", System.currentTimeMillis())
                }
                java.io.File("/Users/mint1024/Desktop/андроид/.cursor/debug.log").appendText("${logJson}\n")
            } catch (e: Exception) {}
            // #endregion

            if (scanResults.isEmpty()) {
                Log.d("WifiGuardDebug", "WifiScannerService: Scan results are empty - this may be due to Android background restrictions")
            }

            val mappedResults = scanResults.mapNotNull { scanResult ->
                try {
                    // Используем безопасный способ получения SSID
                    val ssidForLog = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        scanResult.wifiSsid?.toString() ?: "unknown"
                    } else {
                        @Suppress("DEPRECATION")
                        scanResult.SSID ?: "unknown"
                    }
                    Log.d("WifiGuardDebug", "WifiScannerService: Processing scan result for $ssidForLog")
                    scanResultToWifiScanResult(scanResult)
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка преобразования результата сканирования: ${e.message}")
                    Log.e("WifiGuardDebug", "WifiScannerService: Error converting scan result: ${e.message}", e)
                    // #region agent log
                    try {
                        val logJson = org.json.JSONObject().apply {
                            put("sessionId", "debug-session")
                            put("runId", runId)
                            put("hypothesisId", "D")
                            put("location", "WifiScannerService.kt:343")
                            put("message", "Ошибка преобразования результата сканирования")
                            put("data", org.json.JSONObject().apply {
                                put("error", e.message ?: "unknown")
                                put("errorType", e.javaClass.simpleName)
                            })
                            put("timestamp", System.currentTimeMillis())
                        }
                        java.io.File("/Users/mint1024/Desktop/андроид/.cursor/debug.log").appendText("${logJson}\n")
                    } catch (logEx: Exception) {}
                    // #endregion
                    null
                }
            }

            Log.d("WifiGuardDebug", "WifiScannerService: Successfully converted ${mappedResults.size} scan results to core models")
            
            // #region agent log
            try {
                val logJson = org.json.JSONObject().apply {
                    put("sessionId", "debug-session")
                    put("runId", runId)
                    put("hypothesisId", "B")
                    put("location", "WifiScannerService.kt:362")
                    put("message", "Успешно преобразованы результаты сканирования")
                    put("data", org.json.JSONObject().apply {
                        put("mappedCount", mappedResults.size)
                    })
                    put("timestamp", System.currentTimeMillis())
                }
                java.io.File("/Users/mint1024/Desktop/андроид/.cursor/debug.log").appendText("${logJson}\n")
            } catch (e: Exception) {}
            // #endregion
            
            mappedResults
        } catch (e: SecurityException) {
            Log.e(TAG, "Нет разрешений для получения результатов сканирования", e)
            Log.e("WifiGuardDebug", "WifiScannerService: Security exception getting scan results: ${e.message}", e)
            // #region agent log
            try {
                val logJson = org.json.JSONObject().apply {
                    put("sessionId", "debug-session")
                    put("runId", runId)
                    put("hypothesisId", "C")
                    put("location", "WifiScannerService.kt:380")
                    put("message", "SecurityException при получении результатов сканирования")
                    put("data", org.json.JSONObject().apply {
                        put("error", e.message ?: "unknown")
                        put("sdkVersion", Build.VERSION.SDK_INT)
                    })
                    put("timestamp", System.currentTimeMillis())
                }
                java.io.File("/Users/mint1024/Desktop/андроид/.cursor/debug.log").appendText("${logJson}\n")
            } catch (logEx: Exception) {}
            // #endregion
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении результатов сканирования", e)
            Log.e("WifiGuardDebug", "WifiScannerService: Error getting scan results: ${e.message}", e)
            // #region agent log
            try {
                val logJson = org.json.JSONObject().apply {
                    put("sessionId", "debug-session")
                    put("runId", runId)
                    put("hypothesisId", "D")
                    put("location", "WifiScannerService.kt:397")
                    put("message", "Общая ошибка при получении результатов сканирования")
                    put("data", org.json.JSONObject().apply {
                        put("error", e.message ?: "unknown")
                        put("errorType", e.javaClass.simpleName)
                        put("sdkVersion", Build.VERSION.SDK_INT)
                    })
                    put("timestamp", System.currentTimeMillis())
                }
                java.io.File("/Users/mint1024/Desktop/андроид/.cursor/debug.log").appendText("${logJson}\n")
            } catch (logEx: Exception) {}
            // #endregion
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
                        lastResultsUpdateTime.set(System.currentTimeMillis())
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
                // Android 10+ - пытаемся получить максимально точную информацию, сохраняя совместимость.
                val connectivityManager =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                val activeNetwork = connectivityManager.activeNetwork
                val caps = connectivityManager.getNetworkCapabilities(activeNetwork)

                if (caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) != true) {
                    return null
                }

                // На Android 12+ (S) доступен transportInfo (android.net.wifi.WifiInfo).
                // На Android 10-11 используем устаревший wifiManager.connectionInfo как fallback.
                val platformWifiInfo: android.net.wifi.WifiInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    caps.transportInfo as? android.net.wifi.WifiInfo
                } else {
                    @Suppress("DEPRECATION")
                    wifiManager.connectionInfo
                }

                val rawSsid = platformWifiInfo?.ssid
                val ssid = rawSsid
                    ?.removeSurrounding("\"")
                    ?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
                    ?: ""

                val bssid = platformWifiInfo?.bssid
                    ?.takeIf { it.isNotBlank() && it != "02:00:00:00:00:00" }
                    ?: ""

                // Пытаемся найти сеть в результатах сканирования для получения capabilities/частоты/канала.
                val scanResults = wifiManager.scanResults
                val matchingScan = when {
                    bssid.isNotBlank() -> scanResults.firstOrNull { scan ->
                        @Suppress("DEPRECATION")
                        val scanBssid = scan.BSSID
                        scanBssid?.equals(bssid, ignoreCase = true) == true
                    }
                    ssid.isNotBlank() -> scanResults.firstOrNull {
                        val scanSsid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            it.wifiSsid?.toString() ?: ""
                        } else {
                            @Suppress("DEPRECATION")
                            it.SSID ?: ""
                        }
                        scanSsid.removeSurrounding("\"").equals(ssid, ignoreCase = true)
                    }
                    else -> null
                }

                return if (matchingScan != null) {
                    scanResultToWifiInfo(matchingScan).copy(isConnected = true)
                } else {
                    // Если нет совпадения в scanResults, возвращаем минимум данных, которые удалось получить.
                    val frequency = platformWifiInfo?.frequency ?: 2400
                    WifiInfo(
                        ssid = if (ssid.isNotBlank()) ssid else "Текущая сеть",
                        bssid = if (bssid.isNotBlank()) bssid else "unknown",
                        capabilities = "",
                        level = platformWifiInfo?.rssi ?: -1,
                        frequency = frequency,
                        timestamp = System.currentTimeMillis(),
                        encryptionType = com.wifiguard.feature.scanner.domain.model.EncryptionType.UNKNOWN,
                        signalStrength = platformWifiInfo?.rssi ?: -1,
                        channel = getChannelFromFrequency(frequency),
                        bandwidth = null,
                        isHidden = ssid.isBlank(),
                        isConnected = true
                    )
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
     * Наблюдает за изменениями состояния WiFi в реальном времени
     * @return Flow с состоянием WiFi (true = включен, false = выключен)
     */
    fun observeWifiEnabled(): Flow<Boolean> = callbackFlow {
        Log.d("WifiGuardDebug", "WifiScannerService: Starting to observe WiFi state changes")
        
        val wifiStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                        val wifiState = intent.getIntExtra(
                            WifiManager.EXTRA_WIFI_STATE,
                            WifiManager.WIFI_STATE_UNKNOWN
                        )
                        val isEnabled = wifiState == WifiManager.WIFI_STATE_ENABLED
                        Log.d(TAG, "WiFi state changed: enabled=$isEnabled, state=$wifiState")
                        Log.d("WifiGuardDebug", "WifiScannerService: WiFi state changed: enabled=$isEnabled")
                        trySend(isEnabled)
                    }
                }
            }
        }
        
        // Регистрируем receiver для отслеживания изменений состояния WiFi
        val intentFilter = IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION)
        Log.d("WifiGuardDebug", "WifiScannerService: Registering WiFi state receiver")
        context.registerReceiver(wifiStateReceiver, intentFilter)
        
        // Отправляем текущее состояние
        val currentState = isWifiEnabled()
        Log.d(TAG, "Initial WiFi state: enabled=$currentState")
        Log.d("WifiGuardDebug", "WifiScannerService: Sending initial WiFi state: enabled=$currentState")
        trySend(currentState)
        
        awaitClose {
            try {
                Log.d("WifiGuardDebug", "WifiScannerService: Unregistering WiFi state receiver")
                context.unregisterReceiver(wifiStateReceiver)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "WiFi state receiver already unregistered")
                Log.d("WifiGuardDebug", "WifiScannerService: WiFi state receiver already unregistered")
            }
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
            // ВАЖНО: ScanResult.timestamp — это время в микросекундах с момента загрузки устройства,
            // а не unix-epoch. Для статистики/сортировки/очистки в БД используем epoch-время.
            timestamp = System.currentTimeMillis(),
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
            // ВАЖНО: ScanResult.timestamp — uptime, не unix-epoch.
            // Статистика и очистка используют epoch-время.
            timestamp = System.currentTimeMillis(),
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