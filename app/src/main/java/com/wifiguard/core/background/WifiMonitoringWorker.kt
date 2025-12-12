package com.wifiguard.core.background

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.wifiguard.core.data.wifi.WifiScannerService
import com.wifiguard.core.domain.model.Freshness
import com.wifiguard.core.domain.model.WifiScanStatus
import com.wifiguard.core.domain.repository.ThreatRepository
import com.wifiguard.core.domain.repository.WifiRepository
import com.wifiguard.core.security.SecurityAnalyzer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Worker для мониторинга Wi-Fi сетей в фоновом режиме
 * 
 * КРИТИЧЕСКИЕ ИСПРАВЛЕНИЯ:
 * ✅ УДАЛЕНО setRequiresDeviceIdle(true) - предотвращало сканирование на активных устройствах
 * ✅ УДАЛЕНО setRequiresBatteryNotLow(true) - слишком ограничительное для безопасности
 * ✅ Добавлено адаптивное планирование на основе уровня заряда
 * ✅ Добавлен экспоненциальный backoff для неудачных попыток
 * ✅ Умная логика retry с Result.retry()
 * ✅ Обработка CancellationException для корректной отмены
 * 
 * @author WifiGuard Security Team
 */
@HiltWorker
class WifiMonitoringWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val wifiScannerService: WifiScannerService,
    private val securityAnalyzer: SecurityAnalyzer,
    private val threatRepository: ThreatRepository,
    private val wifiRepository: WifiRepository
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val TAG = "WifiMonitoringWorker"
        
        // Интервалы сканирования
        private const val DEFAULT_INTERVAL_MINUTES = 15L
        private const val LOW_BATTERY_INTERVAL_MINUTES = 30L
        private const val CRITICAL_BATTERY_INTERVAL_MINUTES = 60L
        
        // Пороги батареи
        private const val LOW_BATTERY_THRESHOLD = 20
        private const val CRITICAL_BATTERY_THRESHOLD = 10
        
        // Максимальное количество повторных попыток
        private const val MAX_RETRY_COUNT = 3
        
        /**
         * Создаёт периодическую работу с оптимизированными ограничениями
         * 
         * ИСПРАВЛЕНО: Удалены слишком ограничительные constraints
         */
        fun createPeriodicWork(): PeriodicWorkRequest {
            return PeriodicWorkRequestBuilder<WifiMonitoringWorker>(
                DEFAULT_INTERVAL_MINUTES, TimeUnit.MINUTES
            )
                .setConstraints(createOptimizedConstraints())
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag("wifi_monitoring")
                .build()
        }
        
        /**
         * Создаёт периодическую работу с пользовательским интервалом
         */
        fun createPeriodicWorkWithInterval(intervalMinutes: Int): PeriodicWorkRequest {
            // Минимальный интервал - 15 минут (ограничение Android)
            val validInterval = maxOf(15, intervalMinutes).toLong()
            
            return PeriodicWorkRequestBuilder<WifiMonitoringWorker>(
                validInterval, TimeUnit.MINUTES
            )
                .setConstraints(createOptimizedConstraints())
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag("wifi_monitoring")
                .build()
        }
        
        /**
         * Создаёт адаптивную периодическую работу на основе уровня батареи
         */
        fun createAdaptivePeriodicWork(batteryLevel: Int): PeriodicWorkRequest {
            val intervalMinutes = when {
                batteryLevel <= CRITICAL_BATTERY_THRESHOLD -> CRITICAL_BATTERY_INTERVAL_MINUTES
                batteryLevel <= LOW_BATTERY_THRESHOLD -> LOW_BATTERY_INTERVAL_MINUTES
                else -> DEFAULT_INTERVAL_MINUTES
            }
            
            Log.d(TAG, "📊 Адаптивный интервал: ${intervalMinutes}мин (батарея: ${batteryLevel}%)")
            
            return PeriodicWorkRequestBuilder<WifiMonitoringWorker>(
                intervalMinutes, TimeUnit.MINUTES
            )
                .setConstraints(createOptimizedConstraints())
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag("wifi_monitoring")
                .addTag("adaptive")
                .build()
        }
        
        /**
         * Создаёт оптимизированные ограничения
         * 
         * КРИТИЧЕСКОЕ ИСПРАВЛЕНИЕ:
         * - НЕ требует device idle (позволяет сканирование при активном использовании)
         * - НЕ требует высокий заряд батареи (безопасность важнее)
         */
        private fun createOptimizedConstraints(): Constraints {
            return Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                // ИСПРАВЛЕНО: Удалено setRequiresBatteryNotLow - слишком ограничительно
                // ИСПРАВЛЕНО: Удалено setRequiresDeviceIdle - предотвращало сканирование
                .setRequiresCharging(false)
                .build()
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "🔍 WifiMonitoringWorker: Starting work (attempt ${runAttemptCount + 1})")
        
        return try {
            // Проверяем количество попыток для умного retry
            if (runAttemptCount > MAX_RETRY_COUNT) {
                Log.w(TAG, "⚠️ Превышено максимальное количество попыток ($MAX_RETRY_COUNT)")
                return Result.failure()
            }
            
            // Проверяем, включена ли Wi-Fi
            if (!wifiScannerService.isWifiEnabled()) {
                Log.d(TAG, "📴 WiFi выключен, пропускаем сканирование")
                return Result.success()
            }
            
            // Проверяем уровень батареи для логирования
            val batteryLevel = getBatteryLevel()
            Log.d(TAG, "🔋 Уровень батареи: $batteryLevel%")
            
            // Запускаем сканирование
            Log.d(TAG, "📡 Запуск WiFi сканирования")
            val scanStatus = wifiScannerService.startScan()
            
            // Обрабатываем различные статусы сканирования
            when (scanStatus) {
                is WifiScanStatus.Success -> {
                    Log.d(TAG, "✅ Сканирование успешно (timestamp=${scanStatus.timestamp})")
                    
                    val (networks, metadata) = wifiScannerService.getScanResultsWithMetadata()
                    Log.d(TAG, "📊 Найдено ${networks.size} сетей (freshness=${metadata.freshness})")
                    
                    if (networks.isNotEmpty()) {
                        // Сохраняем результаты сканирования
                        networks.forEach { scanResult ->
                            wifiRepository.insertScanResult(scanResult)
                        }
                        Log.d(TAG, "💾 Сохранено ${networks.size} результатов в БД")
                        
                        // Анализируем безопасность
                        val securityReport = securityAnalyzer.analyzeNetworks(networks, metadata)
                        Log.d(TAG, "🛡️ Анализ завершён: ${securityReport.threats.size} угроз")
                        
                        // Сохраняем угрозы
                        if (securityReport.threats.isNotEmpty()) {
                            threatRepository.insertThreats(securityReport.threats)
                            Log.d(TAG, "⚠️ Сохранено ${securityReport.threats.size} угроз")
                        }
                    }
                    Result.success()
                }
                
                is WifiScanStatus.Throttled -> {
                    val minutesUntilNext = (scanStatus.nextAvailableTime - System.currentTimeMillis()) / 60000
                    Log.w(TAG, "⏳ Сканирование ограничено системой (следующее через ${minutesUntilNext}мин)")
                    
                    // Используем кэшированные результаты
                    val (networks, metadata) = wifiScannerService.getScanResultsWithMetadata()
                    
                    if (metadata.freshness != Freshness.EXPIRED && networks.isNotEmpty()) {
                        val securityReport = securityAnalyzer.analyzeNetworks(networks, metadata)
                        if (securityReport.threats.isNotEmpty()) {
                            threatRepository.insertThreats(securityReport.threats)
                        }
                        Result.success()
                    } else {
                        // Кэш устарел - успешно завершаем, но без данных
                        Log.w(TAG, "📭 Кэш устарел или пуст")
                        Result.success()
                    }
                }
                
                is WifiScanStatus.Restricted -> {
                    Log.e(TAG, "🚫 Сканирование ограничено: ${scanStatus.reason}")
                    
                    // Попробуем использовать кэш
                    val (networks, metadata) = wifiScannerService.getScanResultsWithMetadata()
                    
                    if (networks.isNotEmpty() && metadata.freshness != Freshness.EXPIRED) {
                        val securityReport = securityAnalyzer.analyzeNetworks(networks, metadata)
                        if (securityReport.threats.isNotEmpty()) {
                            threatRepository.insertThreats(securityReport.threats)
                        }
                    }
                    
                    // Повторная попытка с экспоненциальным backoff
                    Result.retry()
                }
                
                is WifiScanStatus.Failed -> {
                    Log.e(TAG, "❌ Сканирование не удалось: ${scanStatus.error}")
                    
                    // Повторная попытка вместо полного провала
                    if (runAttemptCount < MAX_RETRY_COUNT) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.d(TAG, "🛑 Работа отменена")
            throw e // Пробрасываем для корректной отмены
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка: ${e.message}", e)
            
            // Умная логика retry
            if (runAttemptCount < MAX_RETRY_COUNT) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
    
    /**
     * Получает текущий уровень заряда батареи
     */
    private fun getBatteryLevel(): Int {
        val batteryIntent: Intent? = applicationContext.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        
        return batteryIntent?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                (level * 100 / scale)
            } else {
                100 // По умолчанию полный заряд
            }
        } ?: 100
    }
}