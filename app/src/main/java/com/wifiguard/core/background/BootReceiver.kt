package com.wifiguard.core.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Получатель системных событий для запуска фонового мониторинга
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var workManager: WorkManager
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d(TAG, "System event received: ${intent.action}")
                startBackgroundMonitoring(context)
            }
        }
    }
    
    private fun startBackgroundMonitoring(context: Context) {
        try {
            val wifiMonitoringRequest = OneTimeWorkRequestBuilder<WifiMonitoringWorker>()
                .addTag(WORK_TAG_WIFI_MONITORING)
                .build()
            
            workManager.enqueueUniqueWork(
                WORK_NAME_WIFI_MONITORING,
                ExistingWorkPolicy.REPLACE,
                wifiMonitoringRequest
            )
            
            Log.d(TAG, "Background monitoring started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start background monitoring", e)
        }
    }
    
    companion object {
        private const val TAG = "BootReceiver"
        private const val WORK_NAME_WIFI_MONITORING = "wifi_monitoring_work"
        private const val WORK_TAG_WIFI_MONITORING = "wifi_monitoring"
    }
}