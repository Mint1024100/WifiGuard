package com.wifiguard.core.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.wifiguard.core.data.wifi.WifiScannerService
import com.wifiguard.core.domain.repository.ThreatRepository
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
    private val threatRepository: ThreatRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Проверяем, включена ли Wi-Fi
            if (!wifiScannerService.isWifiEnabled()) {
                return Result.success()
            }

            // Запускаем сканирование
            wifiScannerService.startScan()
            
            // Получаем результаты сканирования как доменные модели для анализа безопасности
            val scanResults = wifiScannerService.getScanResultsAsCoreModels()
            
            if (scanResults.isNotEmpty()) {
                // Анализируем безопасность сетей
                val securityReport = securityAnalyzer.analyzeNetworks(scanResults)
                
                // Сохраняем угрозы
                val threats = securityReport.threats
                if (threats.isNotEmpty()) {
                    threatRepository.insertThreats(threats)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        fun createPeriodicWork(): PeriodicWorkRequest {
            return PeriodicWorkRequestBuilder<WifiMonitoringWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .addTag("wifi_monitoring")
                .build()
        }
    }
}