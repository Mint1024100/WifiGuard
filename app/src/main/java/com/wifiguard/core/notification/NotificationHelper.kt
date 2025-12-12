package com.wifiguard.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.wifiguard.MainActivity
import com.wifiguard.R
import com.wifiguard.core.common.Constants
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.feature.settings.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Вспомогательный класс для работы с уведомлениями
 * Поддерживает Android API 26+ (Oreo и выше)
 * 
 * ОБНОВЛЕНО: Добавлен throttling для предотвращения спама уведомлений
 * и интеграция с SettingsRepository для автоматического применения настроек.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : INotificationHelper {

    companion object {
        private const val TAG = "${Constants.LOG_TAG}_NotificationHelper"
        
        /**
         * Интервал throttling - минимальное время между уведомлениями для одной и той же угрозы
         * 5 минут = 300 000 миллисекунд
         */
        private const val THROTTLE_INTERVAL_MS = 5 * 60 * 1000L
    }
    
    /**
     * Throttling для предотвращения спама уведомлений.
     */
    private val throttle = NotificationThrottle(THROTTLE_INTERVAL_MS)

    private val notificationManager: NotificationManagerCompat
        get() = NotificationManagerCompat.from(context)

    init {
        createNotificationChannel()
        Log.d(TAG, "NotificationHelper инициализирован")
    }

    /**
     * Создать канал уведомлений с полными настройками для критических угроз
     */
    override fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                "Уведомления об угрозах безопасности",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Критические уведомления о небезопасных Wi-Fi сетях и обнаруженных угрозах"
                
                // Включаем LED индикатор (красный цвет для угроз)
                enableLights(true)
                lightColor = Color.RED
                
                // Включаем вибрацию
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                
                // Устанавливаем звук
                setSound(soundUri, audioAttributes)
                
                // Показывать на экране блокировки
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                
                // Может прерывать режим "Не беспокоить" (для критических угроз)
                setBypassDnd(false)
                
                // Показывать значок на ярлыке приложения
                setShowBadge(true)
            }

            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            systemNotificationManager.createNotificationChannel(channel)
            
            Log.d(TAG, "✅ Канал уведомлений создан: ${channel.id} (Importance: ${channel.importance})")
        } else {
            Log.d(TAG, "Android < O, каналы уведомлений не требуются")
        }
    }

    /**
     * Отправить уведомление об угрозе с throttling и автоматическими настройками
     * 
     * НОВАЯ ВЕРСИЯ: Интегрирована с SettingsRepository и добавлен throttling
     * 
     * @param networkBssid BSSID сети для идентификации
     * @param threatLevel уровень угрозы для определения приоритета
     * @param title заголовок уведомления
     * @param content текст уведомления
     * @return true если уведомление отправлено успешно
     */
    override suspend fun showThreatNotification(
        networkBssid: String,
        threatLevel: ThreatLevel,
        title: String,
        content: String,
        notificationId: Int?
    ): Boolean {
        Log.d(TAG, "📢 Попытка отправить уведомление: BSSID='$networkBssid', ThreatLevel=$threatLevel")
        
        // Проверяем throttling
        if (!shouldShowNotification(networkBssid, threatLevel)) {
            Log.d(TAG, "⏭️ Уведомление пропущено из-за throttling")
            return false
        }
        
        // Проверяем разрешение на уведомления
        if (!checkNotificationPermission()) {
            Log.w(TAG, "⚠️ Нет разрешения POST_NOTIFICATIONS (Android 13+)")
            return false
        }
        
        // Проверяем, включены ли уведомления в системе
        if (!areNotificationsEnabled()) {
            Log.w(TAG, "⚠️ Уведомления отключены пользователем в настройках системы")
            return false
        }
        
        try {
            // Получаем настройки звука и вибрации из репозитория
            val vibrationEnabled = settingsRepository.getNotificationVibrationEnabled().first()
            val soundEnabled = settingsRepository.getNotificationSoundEnabled().first()
            
            Log.d(TAG, "🔧 Настройки: вибрация=$vibrationEnabled, звук=$soundEnabled")
            
            val pendingIntent = createPendingIntent()
            
            // Определяем приоритет и категорию на основе уровня угрозы
            val (priority, category) = getPriorityAndCategory(threatLevel)
            
            val notificationBuilder = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(priority)
                .setCategory(category)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(0) // Убираем стандартные настройки для ручного контроля
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            // Установить вибрацию если включена
            if (vibrationEnabled) {
                notificationBuilder.setVibrate(longArrayOf(0, 250, 250, 250))
                Log.d(TAG, "✅ Вибрация включена")
            }

            // Установить звук если включен
            if (soundEnabled) {
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                notificationBuilder.setSound(soundUri)
                Log.d(TAG, "✅ Звук включен: $soundUri")
            }

            val notification = notificationBuilder.build()
            val safeNotificationId = notificationId ?: buildNotificationId(networkBssid, threatLevel)
            notificationManager.notify(safeNotificationId, notification)
            
            // Обновляем кэш throttling
            updateNotificationCache(networkBssid, threatLevel)
            
            Log.d(TAG, "✅ Уведомление успешно отправлено (ID: $safeNotificationId)")
            return true
            
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ SecurityException при отправке уведомления: ${e.message}", e)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка при отправке уведомления: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Проверить, нужно ли показывать уведомление (throttling)
     * 
     * @param bssid BSSID сети
     * @param threatLevel уровень угрозы
     * @return true если уведомление можно показать
     */
    private fun shouldShowNotification(bssid: String, threatLevel: ThreatLevel): Boolean {
        val key = "$bssid:$threatLevel"
        return throttle.shouldShow(key)
    }
    
    /**
     * Обновить кэш уведомлений после отправки
     * 
     * @param bssid BSSID сети
     * @param threatLevel уровень угрозы
     */
    private fun updateNotificationCache(bssid: String, threatLevel: ThreatLevel) {
        val key = "$bssid:$threatLevel"
        throttle.markShown(key)
        Log.d(TAG, "📝 Throttling: отметили уведомление как показанное")
    }
    
    /**
     * Получить приоритет и категорию уведомления на основе уровня угрозы
     * 
     * @param threatLevel уровень угрозы
     * @return пара (приоритет, категория)
     */
    private fun getPriorityAndCategory(threatLevel: ThreatLevel): Pair<Int, String> {
        return when (threatLevel) {
            ThreatLevel.CRITICAL -> Pair(
                NotificationCompat.PRIORITY_MAX,
                NotificationCompat.CATEGORY_ALARM
            )
            ThreatLevel.HIGH -> Pair(
                NotificationCompat.PRIORITY_HIGH,
                NotificationCompat.CATEGORY_ERROR
            )
            ThreatLevel.MEDIUM -> Pair(
                NotificationCompat.PRIORITY_DEFAULT,
                NotificationCompat.CATEGORY_ERROR  // WARNING не существует, используем ERROR
            )
            ThreatLevel.LOW -> Pair(
                NotificationCompat.PRIORITY_LOW,
                NotificationCompat.CATEGORY_STATUS
            )
            else -> Pair(
                NotificationCompat.PRIORITY_DEFAULT,
                NotificationCompat.CATEGORY_STATUS
            )
        }
    }
    
    /**
     * УСТАРЕВШИЙ МЕТОД: Оставлен для обратной совместимости
     * Рекомендуется использовать новую версию с параметрами networkBssid и threatLevel
     */
    @Deprecated(
        message = "Используйте версию с параметрами networkBssid и threatLevel",
        replaceWith = ReplaceWith("showThreatNotification(networkBssid, threatLevel, title, content)")
    )
    override fun showThreatNotification(
        title: String,
        content: String,
        vibrationEnabled: Boolean,
        soundEnabled: Boolean
    ): Boolean {
        Log.d(TAG, "⚠️ Использование устаревшего метода showThreatNotification")
        Log.d(TAG, "📢 Попытка отправить уведомление: title='$title'")
        
        // Проверяем разрешение на уведомления
        if (!checkNotificationPermission()) {
            Log.w(TAG, "⚠️ Нет разрешения POST_NOTIFICATIONS (Android 13+)")
            return false
        }
        
        // Проверяем, включены ли уведомления в системе
        if (!areNotificationsEnabled()) {
            Log.w(TAG, "⚠️ Уведомления отключены пользователем в настройках системы")
            return false
        }
        
        try {
            val pendingIntent = createPendingIntent()
            
            val notificationBuilder = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(0)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            // Установить вибрацию если включена
            if (vibrationEnabled) {
                notificationBuilder.setVibrate(longArrayOf(0, 250, 250, 250))
                Log.d(TAG, "✅ Вибрация включена")
            }

            // Установить звук если включен
            if (soundEnabled) {
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                notificationBuilder.setSound(soundUri)
                Log.d(TAG, "✅ Звук включен: $soundUri")
            }

            val notification = notificationBuilder.build()
            notificationManager.notify(Constants.NOTIFICATION_ID_THREAT_FALLBACK, notification)
            
            Log.d(TAG, "✅ Уведомление успешно отправлено (ID: ${Constants.NOTIFICATION_ID_THREAT_FALLBACK})")
            return true
            
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ SecurityException при отправке уведомления: ${e.message}", e)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка при отправке уведомления: ${e.message}", e)
            return false
        }
    }

    /**
     * Создать PendingIntent для перехода в приложение при нажатии на уведомление
     */
    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Отменить уведомление
     */
    override fun cancelNotification() {
        notificationManager.cancel(Constants.NOTIFICATION_ID_THREAT_FALLBACK)
        Log.d(TAG, "🔕 Уведомление отменено (ID: ${Constants.NOTIFICATION_ID_THREAT_FALLBACK})")
    }

    /**
     * Проверить разрешение POST_NOTIFICATIONS для Android 13+
     * 
     * @return true если разрешение предоставлено или не требуется (Android < 13)
     */
    override fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            Log.d(TAG, "Проверка разрешения POST_NOTIFICATIONS (Android 13+): $hasPermission")
            hasPermission
        } else {
            // На Android < 13 разрешение не требуется
            Log.d(TAG, "Android < 13, разрешение POST_NOTIFICATIONS не требуется")
            true
        }
    }

    /**
     * Проверить, включены ли уведомления в системе
     * 
     * @return true если уведомления включены
     */
    override fun areNotificationsEnabled(): Boolean {
        val enabled = notificationManager.areNotificationsEnabled()
        Log.d(TAG, "Уведомления ${if (enabled) "включены" else "отключены"} в системе")
        
        // Дополнительная проверка канала для Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && enabled) {
            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = systemNotificationManager.getNotificationChannel(Constants.NOTIFICATION_CHANNEL_ID)
            
            if (channel != null) {
                val channelEnabled = channel.importance != NotificationManager.IMPORTANCE_NONE
                Log.d(TAG, "Канал уведомлений ${Constants.NOTIFICATION_CHANNEL_ID}: " +
                        "${if (channelEnabled) "включен" else "отключен"} (importance: ${channel.importance})")
                return channelEnabled
            } else {
                Log.w(TAG, "⚠️ Канал уведомлений ${Constants.NOTIFICATION_CHANNEL_ID} не найден")
                return false
            }
        }
        
        return enabled
    }

    /**
     * Отправить тестовое уведомление для проверки работоспособности
     * Полезно для тестирования и отладки системы уведомлений
     * 
     * @return true если уведомление отправлено успешно
     */
    override fun testNotification(): Boolean {
        Log.d(TAG, "🧪 Отправка тестового уведомления")
        
        return try {
            // ВАЖНО: не используем runBlocking, чтобы не блокировать поток (в т.ч. UI).
            // Для тестового уведомления достаточно синхронного метода (без обращения к Flow-настройкам).
            @Suppress("DEPRECATION")
            showThreatNotification(
                title = "🧪 Тестовое уведомление",
                content = "Если вы видите это уведомление, система работает корректно! ✅",
                vibrationEnabled = true,
                soundEnabled = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при отправке тестового уведомления: ${e.message}", e)
            false
        }
    }

    /**
     * Получить статус уведомлений для диагностики
     * 
     * @return строка с информацией о состоянии системы уведомлений
     */
    override fun getNotificationStatus(): String {
        val permission = checkNotificationPermission()
        val enabled = areNotificationsEnabled()
        val androidVersion = Build.VERSION.SDK_INT
        
        return buildString {
            appendLine("=== Статус уведомлений ===")
            appendLine("Android API: $androidVersion")
            appendLine("POST_NOTIFICATIONS разрешение: ${if (permission) "✅ Да" else "❌ Нет"}")
            appendLine("Уведомления включены: ${if (enabled) "✅ Да" else "❌ Нет"}")
            appendLine("ID канала: ${Constants.NOTIFICATION_CHANNEL_ID}")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = systemNotificationManager.getNotificationChannel(Constants.NOTIFICATION_CHANNEL_ID)
                if (channel != null) {
                    appendLine("Важность канала: ${channel.importance}")
                    appendLine("Звук: ${if (channel.sound != null) "✅" else "❌"}")
                    appendLine("Вибрация: ${if (channel.shouldVibrate()) "✅" else "❌"}")
                    appendLine("LED: ${if (channel.shouldShowLights()) "✅" else "❌"}")
                } else {
                    appendLine("⚠️ Канал не найден")
                }
            }
        }
    }

    /**
     * Собрать стабильный ID уведомления, чтобы разные сети/уровни не перетирали друг друга.
     */
    private fun buildNotificationId(networkBssid: String, threatLevel: ThreatLevel): Int {
        val raw = "$networkBssid:${threatLevel.name}"
        val hash = raw.hashCode()
        val positive = if (hash == Int.MIN_VALUE) 0 else kotlin.math.abs(hash)
        return Constants.NOTIFICATION_ID_THREAT_BASE + (positive % 9_000_000)
    }
}