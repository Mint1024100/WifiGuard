package com.wifiguard.core.background

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wifiguard.core.common.Constants
import com.wifiguard.core.domain.repository.ThreatRepository
import com.wifiguard.core.data.preferences.PreferencesDataSource
import com.wifiguard.core.notification.INotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Worker для отправки уведомлений об угрозах
 */
@HiltWorker
class ThreatNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val threatRepository: ThreatRepository,
    private val preferencesDataSource: PreferencesDataSource,
    private val notificationHelper: INotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("WifiGuardDebug", "ThreatNotificationWorker: Starting work")

        return try {
            // Проверяем, включены ли уведомления
            val notificationsEnabled = preferencesDataSource.getNotificationsEnabled().first()
            Log.d("WifiGuardDebug", "ThreatNotificationWorker: Notifications enabled = $notificationsEnabled")

            if (!notificationsEnabled) {
                Log.d("WifiGuardDebug", "ThreatNotificationWorker: Notifications disabled, returning success")
                return Result.success()
            }

            // Получаем критические угрозы, которые еще не были уведомлены
            val criticalThreats = threatRepository.getCriticalUnnotifiedThreats()
            Log.d("WifiGuardDebug", "ThreatNotificationWorker: Found ${criticalThreats.size} critical unnotified threats")

            if (criticalThreats.isNotEmpty()) {
                // Отправляем уведомление для каждой критической угрозы
                for (threat in criticalThreats) {
                    Log.d("WifiGuardDebug", "ThreatNotificationWorker: Posting notification for threat: ${threat.networkSsid}")

                    notificationHelper.showThreatNotification(
                        networkBssid = threat.networkBssid,
                        threatLevel = threat.severity,
                        title = "Обнаружена критическая угроза!",
                        content = "Сеть: ${threat.networkSsid} - ${threat.description}",
                        notificationId = buildThreatNotificationId(threat.id)
                    )

                    // Отмечаем угрозу как уведомленную
                    threatRepository.markThreatAsNotified(threat.id)
                    Log.d("WifiGuardDebug", "ThreatNotificationWorker: Marked threat ${threat.id} as notified")
                }

                Log.d("WifiGuardDebug", "ThreatNotificationWorker: Posted notifications for ${criticalThreats.size} threats")
            } else {
                Log.d("WifiGuardDebug", "ThreatNotificationWorker: No critical threats to notify")
            }

            Log.d("WifiGuardDebug", "ThreatNotificationWorker: Work completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e("WifiGuardDebug", "ThreatNotificationWorker: Error during work: ${e.message}", e)
            Result.failure()
        }
    }
    
    companion object {
        fun createPeriodicWork(): androidx.work.PeriodicWorkRequest {
            return androidx.work.PeriodicWorkRequestBuilder<ThreatNotificationWorker>(
                60, java.util.concurrent.TimeUnit.MINUTES  // Check for new threats every hour
            )
                .addTag(Constants.WORK_TAG_THREAT_NOTIFICATION)
                .build()
        }

        private fun buildThreatNotificationId(threatId: Long): Int {
            val safe = (threatId % 9_000_000L).toInt()
            return Constants.NOTIFICATION_ID_THREAT_BASE + safe
        }
    }
}