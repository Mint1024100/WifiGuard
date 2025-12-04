package com.wifiguard.core.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.wifiguard.core.common.Logger
import com.wifiguard.core.data.wifi.WifiScannerService
import com.wifiguard.core.domain.repository.ThreatRepository
import com.wifiguard.core.domain.repository.WifiRepository
import com.wifiguard.core.security.SecurityAnalyzer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
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
        Logger.d("WifiMonitoringWorker: Starting work")
        return try {
            // Check if Wi-Fi is enabled
            if (!wifiScannerService.isWifiEnabled()) {
                Logger.d("WifiMonitoringWorker: WiFi is not enabled, returning success")
                return Result.success()
            }

            // Start scan
            Logger.d("WifiMonitoringWorker: Requesting WiFi scan")
            val scanStarted = wifiScannerService.startScan()
            if (!scanStarted) {
                Logger.d("WifiMonitoringWorker: Failed to start scan")
                return Result.success()
            }

            // Get scan results as domain models for security analysis
            Logger.d("WifiMonitoringWorker: Getting scan results")
            val scanResults = wifiScannerService.getScanResultsAsCoreModels()
            Logger.d("WifiMonitoringWorker: Got ${scanResults.size} scan results")

            if (scanResults.isNotEmpty()) {
                // Save all scan results to database (regardless of threat level)
                scanResults.forEach { scanResult ->
                    wifiRepository.insertScanResult(scanResult)
                }
                Logger.d("Saved ${scanResults.size} scan results to DB")

                // Analyze network security
                Logger.d("WifiMonitoringWorker: Analyzing network security")
                val securityReport = securityAnalyzer.analyzeNetworks(scanResults)
                Logger.d("WifiMonitoringWorker: Security analysis complete, found ${securityReport.threats.size} threats")

                // Save threats
                val threats = securityReport.threats
                if (threats.isNotEmpty()) {
                    Logger.d("WifiMonitoringWorker: Inserting ${threats.size} threats to repository")
                    threatRepository.insertThreats(threats)
                } else {
                    Logger.d("WifiMonitoringWorker: No threats detected in security analysis")
                }
            } else {
                Logger.d("WifiMonitoringWorker: No scan results received - possibly due to Android background restrictions or no networks available")
            }

            Logger.d("WifiMonitoringWorker: Work completed successfully")
            Result.success()
        } catch (e: Exception) {
            Logger.e("WifiMonitoringWorker: Error during work: ${e.message}", e)
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