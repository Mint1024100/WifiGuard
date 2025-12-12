package com.wifiguard.core.monitoring

import android.util.Log
import com.wifiguard.core.common.ConnectionType
import com.wifiguard.core.common.Constants
import com.wifiguard.core.common.NetworkMonitor
import com.wifiguard.core.data.wifi.WifiScanner
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.domain.repository.ThreatRepository
import com.wifiguard.core.notification.INotificationHelper
import com.wifiguard.core.security.SecurityAnalyzer
import com.wifiguard.feature.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observer для отслеживания WiFi подключений и триггера уведомлений об угрозах
 * 
 * Основные функции:
 * - Отслеживает изменения WiFi подключения через NetworkMonitor
 * - Автоматически анализирует безопасность новой сети
 * - Отправляет уведомления при обнаружении угроз
 * - Предотвращает дублирование уведомлений для одной и той же сети
 * - Сохраняет обнаруженные угрозы в базу данных
 * 
 * РЕШАЕМАЯ ПРОБЛЕМА: До этого уведомления нужно было отправлять вручную,
 * теперь они автоматически появляются при подключении к небезопасной сети.
 */
@Singleton
class WifiConnectionObserver @Inject constructor(
    private val networkMonitor: NetworkMonitor,
    private val wifiScanner: WifiScanner,
    private val securityAnalyzer: SecurityAnalyzer,
    private val notificationHelper: INotificationHelper,
    private val settingsRepository: SettingsRepository,
    private val threatRepository: ThreatRepository
) {
    
    companion object {
        private const val TAG = "${Constants.LOG_TAG}_WifiConnectionObserver"
        
        /**
         * Задержка после подключения к сети для стабилизации соединения
         * 2 секунды достаточно, чтобы Android получил полную информацию о сети
         */
        private const val CONNECTION_STABILIZATION_DELAY = 2000L
        
        /**
         * Минимальный интервал между уведомлениями для одной и той же сети
         * 10 минут - защита от спама при переподключениях
         */
        private const val NOTIFICATION_COOLDOWN_MS = 10 * 60 * 1000L
    }
    
    /**
     * Кэш последних уведомленных сетей для предотвращения спама
     * Ключ: BSSID сети
     * Значение: timestamp последнего уведомления
     */
    private val lastNotifiedNetworks = mutableMapOf<String, Long>()
    
    /**
     * Mutex для thread-safe доступа к кэшу
     */
    private val cacheMutex = Mutex()
    
    /**
     * Запустить наблюдение за подключениями WiFi
     * 
     * @param scope CoroutineScope для запуска корутин (обычно applicationScope)
     */
    fun startObserving(scope: CoroutineScope) {
        Log.d(TAG, "🔍 Запуск WifiConnectionObserver")
        
        networkMonitor.observeConnectionType()
            .filter { connectionType ->
                // Реагируем только на подключение к WiFi
                val isWifi = connectionType == ConnectionType.WIFI
                if (isWifi) {
                    Log.d(TAG, "📶 Обнаружено подключение к WiFi")
                }
                isWifi
            }
            .onEach {
                // Даем время на стабилизацию подключения
                Log.d(TAG, "⏳ Ожидание стабилизации подключения (${CONNECTION_STABILIZATION_DELAY}ms)")
                delay(CONNECTION_STABILIZATION_DELAY)
                
                // Проверяем текущую WiFi сеть
                checkCurrentWifiConnection()
            }
            .launchIn(scope)
        
        Log.d(TAG, "✅ WifiConnectionObserver запущен")
    }
    
    /**
     * Проверить безопасность текущей WiFi сети
     */
    private suspend fun checkCurrentWifiConnection() {
        try {
            Log.d(TAG, "🔎 Проверка текущего WiFi подключения")
            
            // Проверяем, включены ли уведомления в настройках
            val notificationsEnabled = settingsRepository.getNotificationsEnabled().first()
            if (!notificationsEnabled) {
                Log.d(TAG, "🔕 Уведомления отключены в настройках приложения")
                return
            }
            
            // Получаем информацию о текущей сети
            val currentNetwork = wifiScanner.getCurrentNetwork()
            if (currentNetwork == null) {
                Log.d(TAG, "❌ Не удалось получить информацию о текущей сети")
                return
            }
            
            Log.d(TAG, "📡 Текущая сеть: SSID='${currentNetwork.ssid}', BSSID='${currentNetwork.bssid}'")
            
            // Проверяем, не отправляли ли мы уже уведомление для этой сети недавно
            if (shouldSkipNotification(currentNetwork.bssid)) {
                Log.d(TAG, "⏭️ Пропускаем уведомление (недавно уже отправляли для этой сети)")
                return
            }
            
            // Анализируем безопасность сети
            Log.d(TAG, "🔬 Анализ безопасности сети...")
            val securityReport = securityAnalyzer.analyzeNetworks(
                scanResults = listOf(currentNetwork),
                metadata = wifiScanner.getLastScanMetadata()
            )
            
            // Ищем угрозы для текущей сети
            val networkThreats = securityReport.networkAnalysis
                .firstOrNull { it.network.bssid == currentNetwork.bssid }
                ?.threats
                ?: emptyList()
            
            val threatLevel = securityReport.networkAnalysis
                .firstOrNull { it.network.bssid == currentNetwork.bssid }
                ?.threatLevel
                ?: ThreatLevel.UNKNOWN
            
            Log.d(TAG, "🎯 Уровень угрозы: $threatLevel, Найдено угроз: ${networkThreats.size}")
            
            // Отправляем уведомление только если угроза HIGH или CRITICAL
            if (threatLevel.isHighOrCritical()) {
                Log.w(TAG, "⚠️ Обнаружена опасная сеть: ${threatLevel.getDescription()}")
                
                // Формируем текст уведомления
                val title = "⚠️ Небезопасная WiFi сеть"
                val content = buildNotificationContent(
                    ssid = currentNetwork.ssid,
                    threatLevel = threatLevel,
                    threatsCount = networkThreats.size
                )
                
                // Отправляем уведомление
                val success = notificationHelper.showThreatNotification(
                    networkBssid = currentNetwork.bssid,
                    threatLevel = threatLevel,
                    title = title,
                    content = content
                )
                
                if (success) {
                    Log.d(TAG, "✅ Уведомление отправлено успешно")
                    
                    // Сохраняем информацию об уведомлении
                    updateNotificationCache(currentNetwork.bssid)
                    
                    // Сохраняем угрозы в базу данных
                    if (networkThreats.isNotEmpty()) {
                        val savedCount = threatRepository.insertThreats(networkThreats)
                        Log.d(TAG, "💾 Сохранено угроз в БД: ${savedCount.size}")
                    }
                } else {
                    Log.w(TAG, "❌ Не удалось отправить уведомление")
                }
            } else {
                Log.d(TAG, "✅ Сеть безопасна или риск приемлемый")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка при проверке WiFi подключения: ${e.message}", e)
        }
    }
    
    /**
     * Проверить, нужно ли пропустить уведомление для данной сети
     * (throttling на основе времени последнего уведомления)
     */
    private suspend fun shouldSkipNotification(bssid: String): Boolean {
        return cacheMutex.withLock {
            val lastNotifiedTime = lastNotifiedNetworks[bssid]
            if (lastNotifiedTime != null) {
                val timeSinceLastNotification = System.currentTimeMillis() - lastNotifiedTime
                timeSinceLastNotification < NOTIFICATION_COOLDOWN_MS
            } else {
                false
            }
        }
    }
    
    /**
     * Обновить кэш уведомленных сетей
     */
    private suspend fun updateNotificationCache(bssid: String) {
        cacheMutex.withLock {
            lastNotifiedNetworks[bssid] = System.currentTimeMillis()
            
            // Очистка старых записей (старше 1 часа)
            val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000L)
            lastNotifiedNetworks.entries.removeIf { it.value < oneHourAgo }
            
            Log.d(TAG, "📝 Кэш обновлен. Всего записей: ${lastNotifiedNetworks.size}")
        }
    }
    
    /**
     * Построить текст уведомления на основе обнаруженных угроз
     */
    private fun buildNotificationContent(
        ssid: String,
        threatLevel: ThreatLevel,
        threatsCount: Int
    ): String {
        return buildString {
            append("Вы подключены к сети \"$ssid\"\n")
            append("Уровень риска: ${threatLevel.getDescription()}\n")
            
            if (threatsCount > 0) {
                append("Обнаружено угроз: $threatsCount\n")
            }
            
            append("\n")
            append(threatLevel.getRecommendation())
        }
    }
    
    /**
     * Очистить кэш уведомлений (для тестирования)
     */
    suspend fun clearNotificationCache() {
        cacheMutex.withLock {
            lastNotifiedNetworks.clear()
            Log.d(TAG, "🗑️ Кэш уведомлений очищен")
        }
    }
}
