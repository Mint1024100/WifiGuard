package com.wifiguard.core.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wifiguard.core.common.Logger
import com.wifiguard.core.data.preferences.PreferencesDataSource
import com.wifiguard.core.domain.repository.ThreatRepository
import com.wifiguard.core.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Worker для отправки уведомлений об угрозах
 */
@HiltWorker
class ThreatNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val threatRepository: ThreatRepository,
    private val preferencesDataSource: PreferencesDataSource,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Logger.d("ThreatNotificationWorker: Starting work")

        return try {
            // Check if notifications are enabled
            val notificationsEnabled = preferencesDataSource.getNotificationsEnabled().first()
            Logger.d("ThreatNotificationWorker: Notifications enabled = $notificationsEnabled")

            if (!notificationsEnabled) {
                Logger.d("ThreatNotificationWorker: Notifications disabled, returning success")
                return Result.success()
            }

            // Get critical unnotified threats
            val criticalThreats = threatRepository.getCriticalUnnotifiedThreats()
            Logger.d("ThreatNotificationWorker: Found ${criticalThreats.size} critical unnotified threats")

            if (criticalThreats.isNotEmpty()) {
                // Check sound and vibration settings
                val soundEnabled = preferencesDataSource.getNotificationSoundEnabled().first()
                val vibrationEnabled = preferencesDataSource.getNotificationVibrationEnabled().first()
                Logger.d("ThreatNotificationWorker: Sound=$soundEnabled, Vibration=$vibrationEnabled")

                // Send notification for each critical threat
                for (threat in criticalThreats) {
                    // NOTE: Do not log SSID/BSSID in production for privacy
                    Logger.d("ThreatNotificationWorker: Posting notification for threat ID: ${threat.id}")

                    notificationHelper.showThreatNotification(
                        title = "Обнаружена критическая угроза!",
                        content = "Сеть: ${threat.networkSsid} - ${threat.description}",
                        vibrationEnabled = vibrationEnabled,
                        soundEnabled = soundEnabled
                    )

                    // Mark threat as notified
                    threatRepository.markThreatAsNotified(threat.id)
                    Logger.d("ThreatNotificationWorker: Marked threat ${threat.id} as notified")
                }

                Logger.d("ThreatNotificationWorker: Posted notifications for ${criticalThreats.size} threats")
            } else {
                Logger.d("ThreatNotificationWorker: No critical threats to notify")
            }

            Logger.d("ThreatNotificationWorker: Work completed successfully")
            Result.success()
        } catch (e: Exception) {
            Logger.e("ThreatNotificationWorker: Error during work: ${e.message}", e)
            Result.failure()
        }
    }
    
    companion object {
        fun createPeriodicWork(): androidx.work.PeriodicWorkRequest {
            return androidx.work.PeriodicWorkRequestBuilder<ThreatNotificationWorker>(
                60, java.util.concurrent.TimeUnit.MINUTES  // Check for new threats every hour
            )
                .addTag("threat_notifications")
                .build()
        }
    }
}