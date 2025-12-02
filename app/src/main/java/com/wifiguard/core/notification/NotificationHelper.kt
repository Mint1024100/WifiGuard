package com.wifiguard.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.wifiguard.MainActivity
import com.wifiguard.R
import com.wifiguard.core.common.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Вспомогательный класс для работы с уведомлениями
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val notificationManager: NotificationManagerCompat
        get() = NotificationManagerCompat.from(context)

    init {
        createNotificationChannel()
    }

    /**
     * Создать канал уведомлений
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                "Уведомления об угрозах",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о критических и потенциальных угрозах безопасности Wi-Fi"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Отправить уведомление об угрозе
     */
    fun showThreatNotification(
        title: String,
        content: String,
        vibrationEnabled: Boolean = true,
        soundEnabled: Boolean = true
    ) {
        val pendingIntent = createPendingIntent()
        
        val notificationBuilder = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications) // Используем вашу иконку уведомлений
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(0) // Убираем стандартные настройки, чтобы контролировать их вручную

        // Установить вибрацию если включена
        if (vibrationEnabled) {
            notificationBuilder.setVibrate(longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400))
        }

        // Установить звук если включен
        if (soundEnabled) {
            notificationBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        }

        notificationManager.notify(Constants.NOTIFICATION_ID, notificationBuilder.build())
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
    fun cancelNotification() {
        notificationManager.cancel(Constants.NOTIFICATION_ID)
    }
}