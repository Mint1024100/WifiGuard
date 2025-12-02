package com.wifiguard.core.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wifiguard.core.domain.repository.ThreatRepository
import com.wifiguard.core.data.preferences.PreferencesDataSource
import com.wifiguard.core.notification.NotificationHelper
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
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Проверяем, включены ли уведомления
            val notificationsEnabled = preferencesDataSource.getNotificationsEnabled().first()
            if (!notificationsEnabled) {
                return Result.success()
            }

            // Получаем критические угрозы, которые еще не были уведомлены
            val criticalThreats = threatRepository.getCriticalUnnotifiedThreats()
            
            if (criticalThreats.isNotEmpty()) {
                // Проверяем настройки звука и вибрации
                val soundEnabled = preferencesDataSource.getNotificationSoundEnabled().first()
                val vibrationEnabled = preferencesDataSource.getNotificationVibrationEnabled().first()

                // Отправляем уведомление для каждой критической угрозы
                for (threat in criticalThreats) {
                    notificationHelper.showThreatNotification(
                        title = "Обнаружена критическая угроза!",
                        content = "Сеть: ${threat.networkSsid} - ${threat.description}",
                        vibrationEnabled = vibrationEnabled,
                        soundEnabled = soundEnabled
                    )
                    
                    // Отмечаем угрозу как уведомленную
                    threatRepository.markThreatAsNotified(threat.id)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
    
    companion object
}