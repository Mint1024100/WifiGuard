package com.wifiguard.core.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.wifiguard.core.data.wifi.WifiScannerService
import com.wifiguard.core.domain.repository.ThreatRepository
import com.wifiguard.core.domain.repository.WifiRepository
import com.wifiguard.core.security.SecurityAnalyzer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Worker для мониторинга Wi-Fi сетей в фоновом режиме
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

    override suspend fun doWork(): Result {
        android.util.Log.d("WifiGuardDebug", "WifiMonitoringWorker: Starting work")
        return try {
            // Проверяем, включена ли Wi-Fi
            if (!wifiScannerService.isWifiEnabled()) {
                android.util.Log.d("WifiGuardDebug", "WifiMonitoringWorker: WiFi is not enabled, returning success")
                return Result.success()
            }

            // Запускаем сканирование
            android.util.Log.d("WifiGuardDebug", "WifiMonitoringWorker: Requesting WiFi scan")
            val scanStarted = wifiScannerService.startScan()
            if (!scanStarted) {
                android.util.Log.d("WifiGuardDebug", "WifiMonitoringWorker: Failed to start scan")
                return Result.success()
            }

            // Получаем результаты сканирования как доменные модели для анализа безопасности
            android.util.Log.d("WifiGuardDebug", "WifiMonitoringWorker: Getting scan results")
            val scanResults = wifiScannerService.getScanResultsAsCoreModels()
            android.util.Log.d("WifiGuardDebug", "WifiMonitoringWorker: Got ${scanResults.size} scan results")

            if (scanResults.isNotEmpty()) {
                // Сохраняем все результаты сканирования в базу данных (независимо от уровня угрозы)
                scanResults.forEach { scanResult ->
                    wifiRepository.insertScanResult(scanResult)
                }
                android.util.Log.d("WifiGuardDebug", "Saved ${scanResults.size} scan results to DB")

                // Анализируем безопасность сетей
                android.util.Log.d("WifiGuardDebug", "WifiMonitoringWorker: Analyzing network security")
                val securityReport = securityAnalyzer.analyzeNetworks(scanResults)
                android.util.Log.d("WifiGuardDebug", "WifiMonitoringWorker: Security analysis complete, found ${securityReport.threats.size} threats")

                // Сохраняем угрозы
                val threats = securityReport.threats
                if (threats.isNotEmpty()) {
                    android.util.Log.d("WifiGuardDebug", "WifiMonitoringWorker: Inserting ${threats.size} threats to repository")
                    threatRepository.insertThreats(threats)
                } else {
                    android.util.Log.d("WifiGuardDebug", "WifiMonitoringWorker: No threats detected in security analysis")
                }
            } else {
                android.util.Log.d("WifiGuardDebug", "WifiMonitoringWorker: No scan results received - possibly due to Android background restrictions or no networks available")
            }

            android.util.Log.d("WifiGuardDebug", "WifiMonitoringWorker: Work completed successfully")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("WifiGuardDebug", "WifiMonitoringWorker: Error during work: ${e.message}", e)
            Result.failure()
        }
    }

    companion object {
        fun createPeriodicWork(): PeriodicWorkRequest {
            return PeriodicWorkRequestBuilder<WifiMonitoringWorker>(
                30, TimeUnit.MINUTES  // Increased interval to reduce battery usage (default)
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(true)
                        .setRequiresDeviceIdle(true)  // Added device idle constraint for better battery optimization
                        .setRequiresCharging(false)   // Don't require charging to allow scanning when device is in use
                        .build()
                )
                .addTag("wifi_monitoring")
                .build()
        }

        fun createPeriodicWorkWithInterval(intervalMinutes: Int): PeriodicWorkRequest {
            return PeriodicWorkRequestBuilder<WifiMonitoringWorker>(
                intervalMinutes.toLong(), TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(true)
                        .setRequiresDeviceIdle(true)  // Added device idle constraint for better battery optimization
                        .setRequiresCharging(false)   // Don't require charging to allow scanning when device is in use
                        .build()
                )
                .addTag("wifi_monitoring")
                .build()
        }
    }
}