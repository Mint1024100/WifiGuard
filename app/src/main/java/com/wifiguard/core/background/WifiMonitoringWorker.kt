package com.wifiguard.core.background

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.wifiguard.core.data.wifi.WifiScanner
import com.wifiguard.core.security.SecurityAnalyzer
import com.wifiguard.core.data.local.WifiGuardDatabase
import com.wifiguard.core.data.local.dao.WifiScanDao
import com.wifiguard.core.data.local.dao.ThreatDao
import com.wifiguard.core.data.local.dao.ScanSessionDao
import com.wifiguard.core.data.local.entity.WifiScanEntity
import com.wifiguard.core.data.local.entity.ThreatEntity
import com.wifiguard.core.data.local.entity.ScanSessionEntity
import com.wifiguard.core.domain.model.WifiScanResult
import com.wifiguard.core.security.SecurityThreat
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Фоновый воркер для мониторинга Wi-Fi сетей
 */
@HiltWorker
class WifiMonitoringWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val wifiScanner: WifiScanner,
    private val securityAnalyzer: SecurityAnalyzer,
    private val database: WifiGuardDatabase
) : CoroutineWorker(context, workerParams) {
    
    private val wifiScanDao: WifiScanDao = database.wifiScanDao()
    private val threatDao: ThreatDao = database.threatDao()
    private val scanSessionDao: ScanSessionDao = database.scanSessionDao()
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting Wi-Fi monitoring work")
            
            // Проверяем, включен ли Wi-Fi
            if (!wifiScanner.isWifiEnabled()) {
                Log.w(TAG, "Wi-Fi is disabled, skipping scan")
                return@withContext Result.success()
            }
            
            // Создаем сессию сканирования
            val sessionId = UUID.randomUUID().toString()
            val session = ScanSessionEntity(
                sessionId = sessionId,
                startTimestamp = System.currentTimeMillis(),
                isBackgroundScan = true
            )
            scanSessionDao.insertSession(session)
            
            // Выполняем сканирование
            val scanResult = wifiScanner.startScan()
            if (scanResult.isFailure) {
                Log.e(TAG, "Wi-Fi scan failed", scanResult.exceptionOrNull())
                return@withContext Result.failure()
            }
            
            val scanResults = scanResult.getOrNull() ?: emptyList()
            Log.d(TAG, "Found ${scanResults.size} networks")
            
            // Сохраняем результаты сканирования
            val scanEntities = scanResults.map { scanResult ->
                convertToEntity(scanResult, sessionId)
            }
            wifiScanDao.insertScans(scanEntities)
            
            // Анализируем безопасность
            val securityReport = securityAnalyzer.analyzeNetworks(scanResults)
            
            // Сохраняем угрозы
            val threatEntities = securityReport.threats.map { threat ->
                ThreatEntity(
                    scanId = 0, // Будет обновлено после получения ID
                    threatType = threat.type,
                    severity = threat.severity,
                    description = threat.description,
                    networkSsid = threat.networkSsid,
                    networkBssid = threat.networkBssid,
                    additionalInfo = threat.additionalInfo,
                    timestamp = threat.timestamp
                )
            }
            threatDao.insertThreats(threatEntities)
            
            // Обновляем статистику сессии
            scanSessionDao.updateSessionStatistics(
                sessionId = sessionId,
                totalNetworks = securityReport.totalNetworks,
                safeNetworks = securityReport.safeNetworks,
                lowRiskNetworks = securityReport.lowRiskNetworks,
                mediumRiskNetworks = securityReport.mediumRiskNetworks,
                highRiskNetworks = securityReport.highRiskNetworks,
                criticalRiskNetworks = securityReport.criticalRiskNetworks,
                overallRiskLevel = securityReport.overallRiskLevel,
                totalThreats = securityReport.threats.size
            )
            
            // Завершаем сессию
            scanSessionDao.endSession(sessionId, System.currentTimeMillis())
            
            // Запускаем уведомления о критических угрозах
            if (securityReport.hasCriticalThreats()) {
                scheduleThreatNotification(securityReport.threats.filter { it.severity.isCritical() })
            }
            
            Log.d(TAG, "Wi-Fi monitoring work completed successfully")
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Wi-Fi monitoring work failed", e)
            Result.failure()
        }
    }
    
    private fun convertToEntity(scanResult: WifiScanResult, sessionId: String): WifiScanEntity {
        return WifiScanEntity(
            ssid = scanResult.ssid,
            bssid = scanResult.bssid,
            capabilities = scanResult.capabilities,
            frequency = scanResult.frequency,
            level = scanResult.level,
            timestamp = scanResult.timestamp,
            securityType = scanResult.securityType,
            threatLevel = scanResult.threatLevel,
            isConnected = scanResult.isConnected,
            isHidden = scanResult.isHidden,
            vendor = scanResult.vendor,
            channel = scanResult.channel,
            standard = scanResult.standard,
            scanSessionId = sessionId
        )
    }
    
    private fun scheduleThreatNotification(threats: List<SecurityThreat>) {
        try {
            val notificationRequest = OneTimeWorkRequestBuilder<ThreatNotificationWorker>()
                .addTag(WORK_TAG_THREAT_NOTIFICATION)
                .setInputData(
                    Data.Builder()
                        .putInt("threat_count", threats.size)
                        .putString("threat_types", threats.map { it.type.name }.joinToString(","))
                        .build()
                )
                .build()
            
            WorkManager.getInstance(applicationContext)
                .enqueueUniqueWork(
                    WORK_NAME_THREAT_NOTIFICATION,
                    ExistingWorkPolicy.REPLACE,
                    notificationRequest
                )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule threat notification", e)
        }
    }
    
    companion object {
        private const val TAG = "WifiMonitoringWorker"
        private const val WORK_NAME_THREAT_NOTIFICATION = "threat_notification_work"
        private const val WORK_TAG_THREAT_NOTIFICATION = "threat_notification"
        
        /**
         * Создать периодическую работу для мониторинга
         */
        fun createPeriodicWork(): PeriodicWorkRequest {
            return PeriodicWorkRequestBuilder<WifiMonitoringWorker>(
                15, java.util.concurrent.TimeUnit.MINUTES
            )
            .addTag(WORK_TAG_WIFI_MONITORING)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()
        }
        
        private const val WORK_TAG_WIFI_MONITORING = "wifi_monitoring"
    }
}