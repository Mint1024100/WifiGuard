package com.wifiguard.core.background

import android.content.Context
import android.net.wifi.WifiManager
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.wifiguard.core.domain.model.WifiScanResult
import com.wifiguard.core.domain.repository.WifiRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * WorkManager Worker для фонового мониторинга Wi-Fi сетей.
 * Выполняет периодическое сканирование и обнаружение новых сетей.
 */
@HiltWorker
class WifiMonitoringWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val wifiRepository: WifiRepository,
    private val wifiManager: WifiManager
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        const val WORK_NAME = "wifi_monitoring_work"
        const val TAG = "WifiMonitoringWorker"
        
        private const val MONITORING_INTERVAL_MINUTES = 15L
        private const val FLEX_INTERVAL_MINUTES = 5L
        
        /**
         * Запланировать периодический мониторинг
         */
        fun schedulePeriodicMonitoring(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // Не требует сети
                .setRequiresBatteryNotLow(true) // Не работает при низком заряде
                .setRequiresDeviceIdle(false) // Может работать при активном устройстве
                .build()
            
            val periodicWorkRequest = PeriodicWorkRequestBuilder<WifiMonitoringWorker>(
                repeatInterval = MONITORING_INTERVAL_MINUTES,
                repeatIntervalTimeUnit = TimeUnit.MINUTES,
                flexTimeInterval = FLEX_INTERVAL_MINUTES,
                flexTimeIntervalUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .addTag(TAG)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP, // Сохраняем существующую задачу
                    periodicWorkRequest
                )
        }
        
        /**
         * Отменить мониторинг
         */
        fun cancelMonitoring(context: Context) {
            WorkManager.getInstance(context)
                .cancelUniqueWork(WORK_NAME)
        }
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Проверка состояния Wi-Fi
            if (!wifiManager.isWifiEnabled) {
                return@withContext Result.success()
            }
            
            // Фоновое сканирование
            performBackgroundScan()
            
            // Очистка старых данных (старше 30 дней)
            val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
            wifiRepository.clearOldScans(thirtyDaysAgo)
            
            Result.success()
            
        } catch (e: SecurityException) {
            // Не хватает разрешений - пропускаем это выполнение
            Result.success()
        } catch (e: Exception) {
            // При ошибке - повторяем попытку позже
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
    
    /**
     * Выполнить фоновое сканирование
     */
    private suspend fun performBackgroundScan() {
        try {
            // Запуск сканирования
            val scanStarted = wifiManager.startScan()
            
            if (scanStarted) {
                // Мок результат фонового сканирования
                // TODO: Заменить на реальную обработку результатов
                val backgroundScanResult = WifiScanResult(
                    ssid = "BackgroundNetwork",
                    bssid = "AA:BB:CC:DD:EE:FF",
                    signalStrength = -60,
                    frequency = 5200,
                    channel = 40,
                    timestamp = System.currentTimeMillis(),
                    scanType = WifiScanResult.ScanType.BACKGROUND
                )
                
                // Сохраняем результат
                wifiRepository.insertScanResult(backgroundScanResult)
                
                // Анализируем на подозрительность
                analyzeNetworkSecurity(backgroundScanResult)
            }
            
        } catch (e: SecurityException) {
            // Не хватает разрешений
            throw e
        }
    }
    
    /**
     * Простой анализ безопасности сети
     * TODO: Расширить логику анализа
     */
    private suspend fun analyzeNetworkSecurity(scanResult: WifiScanResult) {
        // Пример простого анализа
        val isSuspicious = when {
            scanResult.ssid.isBlank() -> true // Скрытое имя
            scanResult.ssid.contains("free", ignoreCase = true) -> true // Подозрительное имя
            scanResult.ssid.equals("AndroidAP", ignoreCase = true) -> true // Точка доступа Android
            scanResult.signalStrength > -20 -> true // Очень сильный сигнал (возможно, рядом)
            else -> false
        }
        
        if (isSuspicious) {
            val reason = when {
                scanResult.ssid.isBlank() -> "Скрытое имя сети"
                scanResult.ssid.contains("free", ignoreCase = true) -> "Подозрительное имя сети"
                scanResult.ssid.equals("AndroidAP", ignoreCase = true) -> "Возможная точка доступа"
                scanResult.signalStrength > -20 -> "Подозрительно сильный сигнал"
                else -> "Неизвестная причина"
            }
            
            wifiRepository.markNetworkAsSuspicious(scanResult.ssid, reason)
        }
    }
}