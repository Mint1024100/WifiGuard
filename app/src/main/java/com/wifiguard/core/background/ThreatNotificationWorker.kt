package com.wifiguard.core.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.wifiguard.MainActivity
import com.wifiguard.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Воркер для отправки уведомлений об угрозах безопасности
 */
@HiltWorker
class ThreatNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    override suspend fun doWork(): Result {
        try {
            val threatCount = inputData.getInt("threat_count", 0)
            val threatTypes = inputData.getString("threat_types") ?: ""
            
            if (threatCount > 0) {
                sendThreatNotification(threatCount, threatTypes)
            }
            
            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }
    
    private fun sendThreatNotification(threatCount: Int, threatTypes: String) {
        createNotificationChannel()
        
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("Обнаружены угрозы Wi-Fi")
            .setContentText("Найдено $threatCount потенциальных угроз безопасности")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Обнаружены угрозы безопасности в Wi-Fi сетях. Рекомендуется проверить анализ безопасности.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SECURITY)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                setShowBadge(true)
            }
            
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    companion object {
        private const val CHANNEL_ID = "threat_notifications"
        private const val CHANNEL_NAME = "Уведомления об угрозах"
        private const val CHANNEL_DESCRIPTION = "Уведомления о обнаруженных угрозах безопасности Wi-Fi"
        private const val NOTIFICATION_ID = 1001
    }
}
