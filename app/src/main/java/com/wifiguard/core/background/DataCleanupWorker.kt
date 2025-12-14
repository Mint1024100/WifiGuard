package com.wifiguard.core.background

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.wifiguard.core.common.Constants
import com.wifiguard.core.domain.repository.WifiRepository
import com.wifiguard.feature.settings.domain.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Worker для автоматической очистки старых данных сканирования
 * 
 * РЕШЕНИЕ ПРОБЛЕМЫ 3.2: Периодическая очистка устаревших данных на основе
 * настроек пользователя для освобождения места и поддержания производительности БД
 */
@HiltWorker
class DataCleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val wifiRepository: WifiRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "${Constants.LOG_TAG}_DataCleanupWorker"
        
        /**
         * Имя уникальной периодической работы
         */
        const val WORK_NAME = "data_cleanup_periodic"
        
        /**
         * Интервал выполнения очистки (раз в сутки)
         */
        private const val REPEAT_INTERVAL_HOURS = 24L
        
        /**
         * Создать запрос на периодическую работу очистки данных
         */
        fun createPeriodicWork(): PeriodicWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)  // Запускать только если батарея не разряжена
                .setRequiresStorageNotLow(true)  // Запускать только если есть место
                .build()
            
            return PeriodicWorkRequestBuilder<DataCleanupWorker>(
                REPEAT_INTERVAL_HOURS,
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.HOURS)  // Первый запуск через 1 час после установки
                .addTag(Constants.WORK_TAG_DATA_CLEANUP)
                .build()
        }
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "🧹 Запуск автоочистки старых данных")
            
            // Получаем настройку периода хранения данных
            val dataRetentionDays = settingsRepository.getDataRetentionDays().first()
            
            // Если установлено "Навсегда" (-1), пропускаем очистку
            if (dataRetentionDays == -1) {
                Log.d(TAG, "📅 Период хранения данных: Навсегда - очистка пропущена")
                return Result.success()
            }
            
            Log.d(TAG, "📅 Период хранения данных: $dataRetentionDays дней")
            
            // Вычисляем timestamp для удаления данных старше указанного периода
            val currentTime = System.currentTimeMillis()
            val cutoffTime = currentTime - (dataRetentionDays * 24 * 60 * 60 * 1000L)
            
            Log.d(TAG, "🗑️ Удаляем данные старше ${formatDate(cutoffTime)}")
            
            // Удаляем старые сканы
            val deletedScansCount = wifiRepository.deleteScansOlderThan(cutoffTime)
            Log.d(TAG, "✅ Удалено сканов: $deletedScansCount")
            
            // Удаляем старые сети (опционально - зависит от реализации)
            // Обычно сети не удаляются, только обновляются
            
            // Удаляем старые угрозы
            // Это зависит от наличия метода в ThreatRepository
            // threatRepository.deleteThreatsOlderThan(cutoffTime)
            
            // Оптимизируем базу данных после удаления (VACUUM)
            wifiRepository.optimizeDatabase()
            Log.d(TAG, "🔧 База данных оптимизирована")
            
            // Выводим статистику после очистки
            val totalScans = wifiRepository.getTotalScansCount()
            Log.d(TAG, "📊 Осталось сканов: $totalScans")
            
            Log.d(TAG, "✅ Автоочистка завершена успешно")
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка при автоочистке данных: ${e.message}", e)
            
            // Повторим попытку при следующем запуске
            Result.retry()
        }
    }
    
    /**
     * Форматировать timestamp в читаемый вид для логов
     */
    private fun formatDate(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
        return format.format(date)
    }
}











