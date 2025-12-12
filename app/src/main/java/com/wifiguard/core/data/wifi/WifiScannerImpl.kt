package com.wifiguard.core.data.wifi

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.wifiguard.core.common.Constants
import com.wifiguard.core.common.DeviceDebugLogger
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация WifiScanner с поддержкой многопоточности
 * 
 * КРИТИЧЕСКИЕ ИСПРАВЛЕНИЯ БЕЗОПАСНОСТИ:
 * ✅ УДАЛЁН GlobalScope - использует caller's scope через suspend functions
 * ✅ Добавлен Mutex для сериализации операций сканирования
 * ✅ Thread-safe state management через StateFlow
 * ✅ AtomicBoolean для предотвращения race conditions
 * ✅ Proper error handling with structured concurrency
 * ✅ WifiCapabilitiesAnalyzer теперь Singleton через DI (оптимизация памяти)
 * 
 * @author WifiGuard Security Team
 */
@Singleton
class WifiScannerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wifiCapabilitiesAnalyzer: WifiCapabilitiesAnalyzer
) : WifiScanner {
    
    private val wifiManager: WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    
    // ИСПРАВЛЕНО: Mutex для сериализации операций сканирования
    private val scanMutex = Mutex()
    
    // ИСПРАВЛЕНО: AtomicBoolean для thread-safe проверки состояния
    private val isScanInProgress = AtomicBoolean(false)
    
    // ИСПРАВЛЕНО: StateFlow для thread-safe state management
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()
    
    /**
     * Состояния сканирования
     */
    sealed class ScanState {
        object Idle : ScanState()
        object Scanning : ScanState()
        data class Completed(val results: List<WifiScanResult>) : ScanState()
        data class Error(val message: String) : ScanState()
    }

    /**
     * Проверяет включен ли Wi-Fi
     */
    override fun isWifiEnabled(): Boolean {
        logd("Checking WiFi enabled status")
        return wifiManager.isWifiEnabled
    }
    
    /**
     * Наблюдает за изменениями состояния WiFi в реальном времени
     */
    override fun observeWifiEnabled(): Flow<Boolean> = callbackFlow {
        logd("Starting to observe WiFi state changes")
        
        val wifiStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                        val wifiState = intent.getIntExtra(
                            WifiManager.EXTRA_WIFI_STATE,
                            WifiManager.WIFI_STATE_UNKNOWN
                        )
                        val isEnabled = wifiState == WifiManager.WIFI_STATE_ENABLED
                        logd("WiFi state changed: enabled=$isEnabled, state=$wifiState")
                        trySend(isEnabled)
                    }
                }
            }
        }
        
        // Регистрируем receiver для отслеживания изменений состояния WiFi
        val intentFilter = IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION)
        context.registerReceiver(wifiStateReceiver, intentFilter)
        
        // Отправляем текущее состояние
        val currentState = isWifiEnabled()
        logd("Sending initial WiFi state: enabled=$currentState")
        trySend(currentState)
        
        awaitClose {
            try {
                context.unregisterReceiver(wifiStateReceiver)
                logd("Unregistered WiFi state receiver")
            } catch (e: IllegalArgumentException) {
                logw("WiFi state receiver already unregistered")
            }
        }
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
    
    /**
     * Запускает сканирование Wi-Fi сетей
     * 
     * ИСПРАВЛЕНО: Использует Mutex для сериализации операций сканирования
     * и предотвращения race conditions при множественных вызовах
     */
    override suspend fun startScan(): Result<List<WifiScanResult>> = scanMutex.withLock {
        withContext(Dispatchers.IO) {
            // #region agent log
            try {
                val logJson = JSONObject().apply {
                    put("sessionId", "debug-session")
                    put("runId", "run1")
                    put("hypothesisId", "A")
                    put("location", "WifiScannerImpl.kt:157")
                    put("message", "Начало сканирования WiFi")
                    put("data", JSONObject().apply {
                        put("isScanInProgress", isScanInProgress.get())
                    })
                    put("timestamp", System.currentTimeMillis())
                }
                File("/Users/mint1024/Desktop/андроид/.cursor/debug.log").appendText("${logJson}\n")
            } catch (e: Exception) {}
            // #endregion
            
            // Проверяем, не выполняется ли уже сканирование
            if (!isScanInProgress.compareAndSet(false, true)) {
                logw("Сканирование уже выполняется, пропускаем")
                // #region agent log
                try {
                    val logJson = JSONObject().apply {
                        put("sessionId", "debug-session")
                        put("runId", "run1")
                        put("hypothesisId", "A")
                        put("location", "WifiScannerImpl.kt:172")
                        put("message", "Сканирование уже выполняется, пропускаем")
                        put("data", JSONObject())
                        put("timestamp", System.currentTimeMillis())
                    }
                    File("/Users/mint1024/Desktop/андроид/.cursor/debug.log").appendText("${logJson}\n")
                } catch (e: Exception) {}
                // #endregion
                return@withContext Result.failure(IllegalStateException("Сканирование уже выполняется"))
            }
            
            try {
                _scanState.value = ScanState.Scanning
                logd("🔍 Starting WiFi scan")

                // Проверка разрешений
                if (!hasLocationPermission()) {
                    loge("❌ Location permission not granted for WiFi scan")
                    _scanState.value = ScanState.Error("Нет разрешения на местоположение")
                    // #region agent log
                    try {
                        val logJson = JSONObject().apply {
                            put("sessionId", "debug-session")
                            put("runId", "run1")
                            put("hypothesisId", "C")
                            put("location", "WifiScannerImpl.kt:195")
                            put("message", "Нет разрешений для сканирования WiFi")
                            put("data", JSONObject())
                            put("timestamp", System.currentTimeMillis())
                        }
                        File("/Users/mint1024/Desktop/андроид/.cursor/debug.log").appendText("${logJson}\n")
                    } catch (e: Exception) {}
                    // #endregion
                    return@withContext Result.failure(SecurityException("Требуется разрешение ACCESS_FINE_LOCATION"))
                }

                // Важно: на части устройств (OEM) scanResults и/или активное сканирование недоступны,
                // если системная геолокация выключена.
                if (!isSystemLocationEnabled()) {
                    val message = "Включите геолокацию для поиска Wi‑Fi сетей"
                    logw(message)
                    _scanState.value = ScanState.Error(message)
                    return@withContext Result.failure(IllegalStateException(message))
                }

                if (!isWifiEnabled()) {
                    loge("❌ WiFi is disabled, cannot start scan")
                    _scanState.value = ScanState.Error("Wi-Fi отключен")
                    // #region agent log
                    try {
                        val logJson = JSONObject().apply {
                            put("sessionId", "debug-session")
                            put("runId", "run1")
                            put("hypothesisId", "A")
                            put("location", "WifiScannerImpl.kt:211")
                            put("message", "WiFi отключен, сканирование невозможно")
                            put("data", JSONObject())
                            put("timestamp", System.currentTimeMillis())
                        }
                        File("/Users/mint1024/Desktop/андроид/.cursor/debug.log").appendText("${logJson}\n")
                    } catch (e: Exception) {}
                    // #endregion
                    return@withContext Result.failure(IllegalStateException("Wi-Fi выключен. Включите Wi-Fi для сканирования."))
                }

                // В Android 9+ результаты сканирования кэшируются системой
                // Прямое сканирование ограничено из-за политики конфиденциальности
                val scanResults = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ - используем кешированные результаты
                    logd("📱 Android 10+, using cached scan results")
                    getScanResults()
                } else {
                    // Android 9 и ниже - можно использовать startScan() с подавлением предупреждения
                    @Suppress("DEPRECATION")
                    val success = wifiManager.startScan()
                    if (!success) {
                        logw("⚠️ Failed to start WiFi scan, using cached results")
                        getScanResults()
                    } else {
                        // Ждем завершения сканирования
                        logd("⏳ Waiting for scan results...")
                        delay(SCAN_TIMEOUT_MS)
                        getScanResults()
                    }
                }

                logd("✅ Got scan results, found ${scanResults.size} networks")
                _scanState.value = ScanState.Completed(scanResults)
                Result.success(scanResults)
            } catch (e: SecurityException) {
                loge("❌ Security exception during WiFi scan", e)
                _scanState.value = ScanState.Error(e.message ?: "Security exception")
                Result.failure(e)
            } catch (e: Exception) {
                loge("❌ Exception during WiFi scan", e)
                _scanState.value = ScanState.Error(e.message ?: "Unknown error")
                Result.failure(e)
            } finally {
                isScanInProgress.set(false)
            }
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
                            // Launch a coroutine in the current scope to handle the suspend function
                            launch(Dispatchers.IO) {
                                val results = getScanResults()
                                trySend(results)
                            }
                        } else {
                            // Сканирование не удалось, используем кешированные результаты
                            // Launch a coroutine in the current scope to handle the suspend function
                            launch(Dispatchers.IO) {
                                val cachedResults = getScanResults()
                                trySend(cachedResults)
                            }
                        }
                    }
                }
            }
        }
        
        // Регистрируем receiver
        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(scanResultsReceiver, intentFilter)
        
        // Запускаем сканирование в зависимости от версии Android
        val scanStarted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ - не запускаем активное сканирование, используем кешированные данные
            true // Считаем, что сканирование "успешно" запущено
        } else {
            // Android 9 и ниже - можем попытаться запустить сканирование
            @Suppress("DEPRECATION")
            wifiManager.startScan()
        }
        
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
    private suspend fun getScanResults(): List<WifiScanResult> = withContext(Dispatchers.IO) {
        // #region agent log
        try {
            val logJson = JSONObject().apply {
                put("sessionId", "debug-session")
                put("runId", "run1")
                put("hypothesisId", "B")
                put("location", "WifiScannerImpl.kt:302")
                put("message", "Начало получения результатов сканирования")
                put("data", JSONObject().apply {
                    put("hasPermission", hasLocationPermission())
                    put("wifiEnabled", isWifiEnabled())
                })
                put("timestamp", System.currentTimeMillis())
            }
            File("/Users/mint1024/Desktop/андроид/.cursor/debug.log").appendText("${logJson}\n")
        } catch (e: Exception) {}
        // #endregion
        
        if (!hasLocationPermission()) {
            logw("No location permission to get scan results")
            // #region agent log
            try {
                val logJson = JSONObject().apply {
                    put("sessionId", "debug-session")
                    put("runId", "run1")
                    put("hypothesisId", "C")
                    put("location", "WifiScannerImpl.kt:318")
                    put("message", "Нет разрешений для получения результатов сканирования")
                    put("data", JSONObject())
                    put("timestamp", System.currentTimeMillis())
                }
                File("/Users/mint1024/Desktop/андроид/.cursor/debug.log").appendText("${logJson}\n")
            } catch (e: Exception) {}
            // #endregion
            return@withContext emptyList()
        }

        if (!isSystemLocationEnabled()) {
            logw("System location is disabled, scanResults may be restricted on this device")
            return@withContext emptyList()
        }

        return@withContext try {
            logd("Getting scan results from WiFi manager")
            // #region agent log
            try {
                val logJson = JSONObject().apply {
                    put("sessionId", "debug-session")
                    put("runId", "run1")
                    put("hypothesisId", "B")
                    put("location", "WifiScannerImpl.kt:333")
                    put("message", "Попытка получить scanResults из WifiManager")
                    put("data", JSONObject().apply {
                        put("sdkVersion", Build.VERSION.SDK_INT)
                    })
                    put("timestamp", System.currentTimeMillis())
                }
                File("/Users/mint1024/Desktop/андроид/.cursor/debug.log").appendText("${logJson}\n")
            } catch (e: Exception) {}
            // #endregion
            
            val rawResults = wifiManager.scanResults
            
            // #region agent log
            try {
                val logJson = JSONObject().apply {
                    put("sessionId", "debug-session")
                    put("runId", "run1")
                    put("hypothesisId", "B")
                    put("location", "WifiScannerImpl.kt:348")
                    put("message", "Получены raw результаты сканирования")
                    put("data", JSONObject().apply {
                        put("rawResultsCount", rawResults.size)
                    })
                    put("timestamp", System.currentTimeMillis())
                }
                File("/Users/mint1024/Desktop/андроид/.cursor/debug.log").appendText("${logJson}\n")
            } catch (e: Exception) {}
            // #endregion
            
            val convertedResults = rawResults.map { result ->
                convertToWifiScanResult(result)
            }.map { network ->
                // ИСПРАВЛЕНО: Не фильтруем скрытые сети, а маркируем их как подозрительные
                // Скрытые сети могут использоваться для атак Evil Twin
                if (network.ssid.isBlank() || network.ssid == Constants.UNKNOWN_SSID) {
                    network.copy(
                        ssid = Constants.HIDDEN_NETWORK_LABEL,
                        isHidden = true,
                        threatLevel = ThreatLevel.MEDIUM
                    )
                } else {
                    network
                }
            }
            
            // #region agent log
            try {
                val logJson = JSONObject().apply {
                    put("sessionId", "debug-session")
                    put("runId", "run1")
                    put("hypothesisId", "B")
                    put("location", "WifiScannerImpl.kt:375")
                    put("message", "Успешно преобразованы результаты сканирования")
                    put("data", JSONObject().apply {
                        put("convertedCount", convertedResults.size)
                    })
                    put("timestamp", System.currentTimeMillis())
                }
                File("/Users/mint1024/Desktop/андроид/.cursor/debug.log").appendText("${logJson}\n")
            } catch (e: Exception) {}
            // #endregion
            
            convertedResults
        } catch (e: SecurityException) {
            // Нет разрешения
            loge("Security exception getting scan results", e)
            // #region agent log
            try {
                val logJson = JSONObject().apply {
                    put("sessionId", "debug-session")
                    put("runId", "run1")
                    put("hypothesisId", "C")
                    put("location", "WifiScannerImpl.kt:392")
                    put("message", "SecurityException при получении результатов сканирования")
                    put("data", JSONObject().apply {
                        put("error", e.message ?: "unknown")
                        put("sdkVersion", Build.VERSION.SDK_INT)
                    })
                    put("timestamp", System.currentTimeMillis())
                }
                File("/Users/mint1024/Desktop/андроид/.cursor/debug.log").appendText("${logJson}\n")
            } catch (logEx: Exception) {}
            // #endregion
            emptyList()
        } catch (e: Exception) {
            // Другая ошибка
            loge("Exception getting scan results", e)
            // #region agent log
            try {
                val logJson = JSONObject().apply {
                    put("sessionId", "debug-session")
                    put("runId", "run1")
                    put("hypothesisId", "D")
                    put("location", "WifiScannerImpl.kt:410")
                    put("message", "Общая ошибка при получении результатов сканирования")
                    put("data", JSONObject().apply {
                        put("error", e.message ?: "unknown")
                        put("errorType", e.javaClass.simpleName)
                        put("sdkVersion", Build.VERSION.SDK_INT)
                    })
                    put("timestamp", System.currentTimeMillis())
                }
                File("/Users/mint1024/Desktop/андроид/.cursor/debug.log").appendText("${logJson}\n")
            } catch (logEx: Exception) {}
            // #endregion
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

            // В Android 29+ connectionInfo устарел из-за соображений конфиденциальности
            // Используем разные подходы в зависимости от версии Android
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - получаем только минимально необходимую информацию без устаревшего API
                // Получаем информацию через ConnectivityManager и используем устаревший, но разрешенный способ
                // для получения информации о подключенной сети (только SSID)
                try {
                    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                    val activeNetwork = connectivityManager.activeNetwork
                    val caps = connectivityManager.getNetworkCapabilities(activeNetwork)

                    if (caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true) {
                        // На Android 10+ мы все еще можем получить ограниченную информацию о подключенной сети
                        // через активную сеть, если у нас есть разрешения
                        @Suppress("DEPRECATION")
                        val wifiInfo = wifiManager.connectionInfo
                        val connectedBssid = wifiInfo.bssid
                        val connectedSsid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            // Android 13+ - используем устаревший, но более безопасный способ получения SSID
                            // Так как wifiSsid требует специальных разрешений, используем ssid с подавлением предупреждения
                            try {
                                @Suppress("DEPRECATION")
                                wifiInfo.ssid.removeSurrounding("\"").takeIf { it != "<unknown ssid>" }
                            } catch (e: Exception) {
                                // #region agent log
                                try {
                                    val logJson = JSONObject().apply {
                                        put("sessionId", "debug-session")
                                        put("runId", "run1")
                                        put("hypothesisId", "D")
                                        put("location", "WifiScannerImpl.kt:387")
                                        put("message", "Ошибка получения SSID на Android 13+")
                                        put("data", JSONObject().apply {
                                            put("sdkVersion", Build.VERSION.SDK_INT)
                                            put("error", e.message ?: "unknown")
                                        })
                                        put("timestamp", System.currentTimeMillis())
                                    }
                                    File("/Users/mint1024/Desktop/андроид/.cursor/debug.log").appendText("${logJson}\n")
                                } catch (logEx: Exception) {}
                                // #endregion
                                null // При проблемах доступа возвращаем null
                            }
                        } else {
                            // Android 10-12 - можем использовать устаревший, но работающий API
                            @Suppress("DEPRECATION")
                            wifiInfo.ssid.removeSurrounding("\"").takeIf { it != "<unknown ssid>" }
                        }
                        
                        // #region agent log
                        try {
                            val logJson = JSONObject().apply {
                                put("sessionId", "debug-session")
                                put("runId", "run1")
                                put("hypothesisId", "D")
                                put("location", "WifiScannerImpl.kt:397")
                                put("message", "WifiScannerImpl получение информации о подключенной сети")
                                put("data", JSONObject().apply {
                                    put("sdkVersion", Build.VERSION.SDK_INT)
                                    put("connectedBssid", connectedBssid ?: "null")
                                    put("connectedSsid", connectedSsid ?: "null")
                                    put("ssidIsBlank", connectedSsid.isNullOrBlank())
                                    put("bssidIsNull", connectedBssid == null)
                                })
                                put("timestamp", System.currentTimeMillis())
                            }
                            File("/Users/mint1024/Desktop/андроид/.cursor/debug.log").appendText("${logJson}\n")
                        } catch (e: Exception) {}
                        // #endregion

                        // Проверяем, что мы действительно подключены к Wi-Fi
                        if (!connectedSsid.isNullOrBlank() && connectedBssid != null) {
                            // Теперь найдем соответствующую сеть в результатах сканирования
                            val latestScans = getScanResults()
                            val matchingScanResult = latestScans.find {
                                (it.ssid == connectedSsid || it.ssid.removeSurrounding("\"") == connectedSsid) &&
                                it.bssid == connectedBssid
                            }

                            if (matchingScanResult != null) {
                                // Нашли совпадение, возвращаем как подключенную
                                matchingScanResult.copy(isConnected = true)
                            } else {
                                // Не нашли в сканировании, но знаем, что подключены к этой сети
                                // Создаем базовую информацию о подключенной сети
                                // ОПТИМИЗИРОВАНО: используем инжектированный Singleton
                                WifiScanResult(
                                    ssid = connectedSsid,
                                    bssid = connectedBssid,
                                    capabilities = "",
                                    frequency = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        wifiInfo.frequency
                                    } else {
                                        0
                                    },
                                    level = wifiInfo.rssi,
                                    timestamp = System.currentTimeMillis(),
                                    securityType = SecurityType.UNKNOWN,
                                    threatLevel = ThreatLevel.UNKNOWN,
                                    isConnected = true,
                                    isHidden = false, // Точная информация о статусе скрытой сети недоступна
                                    vendor = wifiCapabilitiesAnalyzer.getVendorFromBssid(connectedBssid),
                                    channel = wifiCapabilitiesAnalyzer.getChannelFromFrequency(
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            wifiInfo.frequency
                                        } else {
                                            0
                                        }
                                    ),
                                    standard = getWifiStandard(
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            wifiInfo.frequency
                                        } else {
                                            0
                                        }
                                    )
                                )
                            }
                        } else {
                            // Поскольку мы не можем получить точную информацию на Android 10+,
                            // попробуем использовать результаты сканирования
                            val latestScans = getScanResults()
                            if (latestScans.isNotEmpty()) {
                                // Возвращаем сеть с наилучшим сигналом как потенциально подключенную
                                latestScans.maxByOrNull { it.level }?.copy(isConnected = true)
                            } else {
                                null
                            }
                        }
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    loge("Exception getting network info on Android 10+", e)
                    null
                }
            } else {
                // Android 9 и ниже - можем использовать устаревший, но работающий API
                @Suppress("DEPRECATION")
                val connectionInfo = wifiManager.connectionInfo
                if (connectionInfo.networkId == -1) {
                    null
                } else {
                    // Создаем WifiScanResult из connectionInfo
                    val ssid = connectionInfo.ssid.removeSurrounding("\"")
                    val bssid = connectionInfo.bssid
                    val rssi = connectionInfo.rssi
                    val frequency = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        connectionInfo.frequency
                    } else {
                        0
                    }

                    // Найдем соответствующую сеть в результатах сканирования для получения дополнительной информации
                    val scanResults = getScanResults()
                    val matchingResult = scanResults.find {
                        it.ssid == ssid && it.bssid == bssid
                    }

                    matchingResult?.copy(isConnected = true) ?: WifiScanResult(
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
     * ИСПРАВЛЕНО: Добавлено логирование для диагностики проблем с разрешениями
     */
    private fun hasLocationPermission(): Boolean {
        // #region agent log
        try {
            val logJson = JSONObject().apply {
                put("sessionId", "debug-session")
                put("runId", "run1")
                put("hypothesisId", "C")
                put("location", "WifiScannerImpl.kt:565")
                put("message", "Проверка разрешений для WiFi сканирования")
                put("data", JSONObject().apply {
                    put("sdkVersion", Build.VERSION.SDK_INT)
                    val fineLocation = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    put("ACCESS_FINE_LOCATION", fineLocation)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val nearbyWifi = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.NEARBY_WIFI_DEVICES
                        ) == PackageManager.PERMISSION_GRANTED
                        put("NEARBY_WIFI_DEVICES", nearbyWifi)
                    }
                })
                put("timestamp", System.currentTimeMillis())
            }
            File("/Users/mint1024/Desktop/андроид/.cursor/debug.log").appendText("${logJson}\n")
        } catch (e: Exception) {}
        // #endregion
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            val fineLocation = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val nearbyWifi = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
            
            val hasAll = fineLocation && nearbyWifi
            
            // #region agent log
            try {
                val logJson = JSONObject().apply {
                    put("sessionId", "debug-session")
                    put("runId", "run1")
                    put("hypothesisId", "C")
                    put("location", "WifiScannerImpl.kt:595")
                    put("message", "Результат проверки разрешений Android 13+")
                    put("data", JSONObject().apply {
                        put("hasAllPermissions", hasAll)
                        put("fineLocation", fineLocation)
                        put("nearbyWifi", nearbyWifi)
                    })
                    put("timestamp", System.currentTimeMillis())
                }
                File("/Users/mint1024/Desktop/андроид/.cursor/debug.log").appendText("${logJson}\n")
            } catch (e: Exception) {}
            // #endregion
            
            hasAll
        } else {
            // Android 6-12
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Проверка системной геолокации (тумблер в настройках).
     * На Android 9+ используем isLocationEnabled, иначе проверяем провайдеры.
     */
    private fun isSystemLocationEnabled(): Boolean {
        return runCatching {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lm.isLocationEnabled
            } else {
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            }
        }.getOrDefault(DeviceDebugLogger.isLocationEnabled(context))
    }
    
    /**
     * Конвертировать ScanResult в WifiScanResult
     */
    private suspend fun convertToWifiScanResult(
        scanResult: android.net.wifi.ScanResult,
        isConnected: Boolean = false
    ): WifiScanResult = withContext(Dispatchers.Default) {
        // В Android 13+ SSID и BSSID устарели, используем безопасные методы в зависимости от версии
        val ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - используем безопасные методы
            // На Android 13+ приложение может получать только ограниченную информацию о SSID
            // без разрешения NETWORK_SETTINGs или системного уровня
            try {
                // Используем новый безопасный способ получения SSID (когда доступен)
                scanResult.wifiSsid?.toString() ?: "Hidden Network"
            } catch (e: Exception) {
                "Hidden Network" // Значение по умолчанию при проблемах доступа
            }
        } else {
            // Android 12 и ниже - можем использовать устаревший, но работающий API
            @Suppress("DEPRECATION")
            scanResult.SSID ?: "Hidden Network"
        }

        val capabilities = scanResult.capabilities ?: ""

        val securityType = determineSecurityType(capabilities)
        val threatLevel = ThreatLevel.fromSecurityType(securityType)
        val wifiStandard = getWifiStandard(scanResult.frequency)

        // Используем BSSID с учетом версии Android
        val bssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - ограничения на получение полной информации
            // Пока используем BSSID, так как альтернативы нет, подавим предупреждение
            @Suppress("DEPRECATION")
            scanResult.BSSID ?: "unknown"
        } else {
            @Suppress("DEPRECATION")
            scanResult.BSSID ?: "unknown"
        }

        // ОПТИМИЗИРОВАНО: используем инжектированный Singleton вместо создания нового экземпляра
        return@withContext WifiScanResult(
            ssid = ssid,
            bssid = bssid,
            capabilities = capabilities,
            frequency = scanResult.frequency,
            level = scanResult.level,
            timestamp = System.currentTimeMillis(),
            securityType = securityType,
            threatLevel = threatLevel,
            isConnected = isConnected,
            isHidden = ssid.isEmpty() || ssid == "Hidden Network",
            vendor = wifiCapabilitiesAnalyzer.getVendorFromBssid(bssid),
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
    
    /**
     * Получить метаданные последнего сканирования
     * На Android 10+ всегда возвращает SYSTEM_CACHE, так как прямое сканирование ограничено
     */
    override fun getLastScanMetadata(): com.wifiguard.core.domain.model.ScanMetadata? {
        // WifiScannerImpl использует кэшированные результаты системы
        // Возвращаем метаданные с предположением, что данные из системного кэша
        val currentTime = System.currentTimeMillis()
        
        return com.wifiguard.core.domain.model.ScanMetadata(
            timestamp = currentTime,
            source = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                com.wifiguard.core.domain.model.ScanSource.SYSTEM_CACHE
            } else {
                com.wifiguard.core.domain.model.ScanSource.ACTIVE_SCAN
            },
            freshness = com.wifiguard.core.domain.model.Freshness.UNKNOWN // Не знаем точный возраст системного кэша
        )
    }
    
    companion object {
        private const val SCAN_TIMEOUT_MS = 5000L
    }
}