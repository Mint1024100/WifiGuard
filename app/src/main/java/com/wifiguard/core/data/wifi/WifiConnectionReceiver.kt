package com.wifiguard.core.data.wifi

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.wifiguard.core.common.Constants
import com.wifiguard.core.data.local.WifiGuardDatabase
import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.notification.INotificationHelper
import com.wifiguard.core.security.ThreatType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * BroadcastReceiver для мониторинга подключений к WiFi сетям в реальном времени.
 * Отслеживает события подключения и анализирует безопасность сети.
 */
@AndroidEntryPoint
class WifiConnectionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var wifiScannerService: WifiScannerService

    @Inject
    lateinit var notificationHelper: INotificationHelper

    @Inject
    lateinit var database: WifiGuardDatabase

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "WifiConnectionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Получено событие: ${intent.action}")

        // Проверяем разрешения
        if (!checkPermissions(context)) {
            Log.w(TAG, "Недостаточно разрешений для мониторинга WiFi подключений")
            return
        }

        when (intent.action) {
            // CONNECTIVITY_ACTION deprecated в Android 8.0+, используем только NETWORK_STATE_CHANGED_ACTION
            // Для Android 8.0+ рекомендуется использовать NetworkCallback вместо BroadcastReceiver
            @Suppress("DEPRECATION")
            ConnectivityManager.CONNECTIVITY_ACTION,
            WifiManager.NETWORK_STATE_CHANGED_ACTION -> {
                handleWifiConnectionChange(context)
            }
        }
    }

    /**
     * Обрабатывает изменение состояния WiFi подключения
     */
    private fun handleWifiConnectionChange(context: Context) {
        Log.d(TAG, "Обработка изменения WiFi подключения")

        // Используем goAsync() для асинхронной обработки в BroadcastReceiver
        val pendingResult = goAsync()

        receiverScope.launch {
            try {
                // Проверяем, подключены ли мы к WiFi
                if (!isConnectedToWifi(context)) {
                    Log.d(TAG, "Не подключены к WiFi сети")
                    return@launch
                }

                Log.d(TAG, "Обнаружено подключение к WiFi сети")

                // Получаем информацию о текущей подключенной сети
                val currentNetwork = getCurrentWifiInfo(context)

                if (currentNetwork == null) {
                    Log.w(TAG, "Не удалось получить информацию о текущей сети")
                    return@launch
                }

                Log.d(TAG, "Подключены к сети: SSID=${currentNetwork.ssid}, BSSID=${currentNetwork.bssid}")

                // Анализируем безопасность сети
                analyzeNetworkSecurity(context, currentNetwork)

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при обработке WiFi подключения: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Анализирует безопасность подключенной сети
     */
    private suspend fun analyzeNetworkSecurity(context: Context, networkInfo: NetworkInfo) {
        Log.d(TAG, "Начало анализа безопасности сети ${networkInfo.ssid}")

        try {
            // Шаг 1: Запускаем сканирование для получения актуальной информации
            Log.d(TAG, "Запуск сканирования WiFi сетей")
            val scanStatus = wifiScannerService.startScan()
            Log.d(TAG, "Статус запуска сканирования: $scanStatus")

            // Даем время на завершение сканирования
            kotlinx.coroutines.delay(2000)

            // Шаг 2: Получаем результаты сканирования и ищем текущую сеть
            val scanResults = wifiScannerService.getScanResultsAsCoreModels()
            Log.d(TAG, "Получено ${scanResults.size} результатов сканирования")

            // ВАЖНО: BSSID является более надежным идентификатором сети, чем SSID.
            // Сначала пытаемся найти по BSSID, и только если он недоступен/пустой — по SSID.
            val currentNetworkScanResult = if (networkInfo.bssid.isNotBlank()) {
                scanResults.firstOrNull { it.bssid.equals(networkInfo.bssid, ignoreCase = true) }
            } else {
                scanResults.firstOrNull { it.ssid.equals(networkInfo.ssid, ignoreCase = true) }
            }

            // Определяем тип безопасности
            val securityType = if (currentNetworkScanResult != null) {
                currentNetworkScanResult.securityType
            } else {
                SecurityType.fromCapabilities(networkInfo.capabilities)
            }

            Log.d(TAG, "Тип безопасности сети: $securityType")

            // Шаг 3: Проверяем БД на наличие известных угроз для этой сети
            val threats = withContext(Dispatchers.IO) {
                database.threatDao().getThreatsByNetworkBssid(networkInfo.bssid).first()
            }
            val unresolvedThreats = threats.filter { !it.isResolved }

            if (unresolvedThreats.isNotEmpty()) {
                Log.d(TAG, "Найдено ${unresolvedThreats.size} нерешенных угроз для этой сети")

                // Определяем максимальный уровень угрозы из найденных (по severity),
                // с безопасным fallback по типу безопасности сети.
                val maxThreatLevel = ThreatLevelSelector.calculateMaxThreatLevel(
                    unresolvedThreats = unresolvedThreats,
                    securityType = securityType
                )

                // Отправляем уведомление о известных угрозах
                notificationHelper.showThreatNotification(
                    networkBssid = networkInfo.bssid,
                    threatLevel = maxThreatLevel,
                    title = "⚠️ Известная опасная сеть!",
                    content = "Сеть \"${networkInfo.ssid}\" имеет ${unresolvedThreats.size} известных угроз"
                )

                // Если для сети уже есть известные угрозы, дальнейший анализ не требуется.
                return
            }

            // Шаг 4: Проверяем, является ли сеть небезопасной по типу шифрования
            if (securityType.isInsecure()) {
                Log.d(TAG, "Сеть небезопасна! Тип безопасности: $securityType")

                // Отправляем уведомление о небезопасной сети
                val notificationTitle = when (securityType) {
                    SecurityType.OPEN -> "🚨 Открытая сеть!"
                    SecurityType.WEP -> "⚠️ Устаревшее шифрование WEP"
                    else -> "⚠️ Небезопасная сеть"
                }

                val notificationContent = when (securityType) {
                    SecurityType.OPEN -> "Сеть \"${networkInfo.ssid}\" не защищена. Ваши данные могут быть перехвачены!"
                    SecurityType.WEP -> "Сеть \"${networkInfo.ssid}\" использует устаревшее шифрование WEP. Рекомендуется отключиться!"
                    else -> "Сеть \"${networkInfo.ssid}\" может быть небезопасной"
                }

                // Определяем уровень угрозы на основе типа безопасности
                val threatLevel = ThreatLevel.fromSecurityType(securityType)

                notificationHelper.showThreatNotification(
                    networkBssid = networkInfo.bssid,
                    threatLevel = threatLevel,
                    title = notificationTitle,
                    content = notificationContent
                )

                Log.d(TAG, "Уведомление о небезопасной сети отправлено")
            } else {
                Log.d(TAG, "Сеть безопасна, угроз не обнаружено")
            }

        } catch (e: kotlinx.coroutines.CancellationException) {
            // ВАЖНО: CancellationException нельзя подавлять — пробрасываем дальше
            Log.d(TAG, "Анализ отменён")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при анализе безопасности сети: ${e.message}", e)
        }
    }

    /**
     * Проверяет, подключены ли мы к WiFi сети
     */
    private fun isConnectedToWifi(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                @Suppress("DEPRECATION")
                networkInfo?.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при проверке WiFi подключения: ${e.message}", e)
            false
        }
    }

    /**
     * Получает информацию о текущей подключенной WiFi сети
     */
    private fun getCurrentWifiInfo(context: Context): NetworkInfo? {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            if (!wifiManager.isWifiEnabled) {
                Log.d(TAG, "WiFi отключен")
                return null
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ - получаем информацию через новый API
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)

                if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                    val wifiInfo = capabilities.transportInfo as? WifiInfo

                    if (wifiInfo != null) {
                        val ssid = wifiInfo.ssid?.removeSurrounding("\"") ?: ""
                        val bssid = wifiInfo.bssid ?: ""

                        // Пытаемся найти сеть в результатах сканирования для получения capabilities
                        val scanResults = wifiManager.scanResults
                        val matchingScan = scanResults.firstOrNull { it.BSSID == bssid }
                        val capabilities = matchingScan?.capabilities ?: ""

                        return NetworkInfo(
                            ssid = ssid,
                            bssid = bssid,
                            capabilities = capabilities,
                            rssi = wifiInfo.rssi
                        )
                    }
                }
            } else {
                // Android 11 и ниже - используем устаревший API
                @Suppress("DEPRECATION")
                val connectionInfo = wifiManager.connectionInfo

                if (connectionInfo.networkId != -1) {
                    val ssid = connectionInfo.ssid?.removeSurrounding("\"") ?: ""
                    val bssid = connectionInfo.bssid ?: ""

                    // Пытаемся найти сеть в результатах сканирования для получения capabilities
                    val scanResults = wifiManager.scanResults
                    val matchingScan = scanResults.firstOrNull { 
                        it.BSSID?.equals(bssid, ignoreCase = true) == true 
                    }
                    val capabilities = matchingScan?.capabilities ?: ""

                    return NetworkInfo(
                        ssid = ssid,
                        bssid = bssid,
                        capabilities = capabilities,
                        rssi = connectionInfo.rssi
                    )
                }
            }

            null
        } catch (e: SecurityException) {
            Log.e(TAG, "Нет разрешений для получения информации о WiFi: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении информации о WiFi: ${e.message}", e)
            null
        }
    }

    /**
     * Проверяет наличие необходимых разрешений
     */
    private fun checkPermissions(context: Context): Boolean {
        val requiredPermissions = mutableListOf(
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE
        )

        // Android 13+ требует разрешение на уведомления
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            Log.w(TAG, "Отсутствуют разрешения: ${missingPermissions.joinToString(", ")}")
            return false
        }

        return true
    }

    /**
     * Модель информации о сети
     */
    private data class NetworkInfo(
        val ssid: String,
        val bssid: String,
        val capabilities: String,
        val rssi: Int
    )
}
