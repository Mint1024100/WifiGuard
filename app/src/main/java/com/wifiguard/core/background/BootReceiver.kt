package com.wifiguard.core.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.wifiguard.core.common.Constants

/**
 * Receiver для обработки событий загрузки системы
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "${Constants.LOG_TAG}_BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d(TAG, "System boot completed, starting background monitoring")
                startBackgroundMonitoring(context)
            }
        }
    }

    private fun startBackgroundMonitoring(context: Context) {
        try {
            val workManager = WorkManager.getInstance(context)
            
            // Запускаем периодический мониторинг Wi-Fi
            val periodicWork = WifiMonitoringWorker.createPeriodicWork()
            
            workManager.enqueueUniquePeriodicWork(
                "wifi_monitoring_periodic",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWork
            )
            
            Log.d(TAG, "Background monitoring started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start background monitoring: ${e.message}", e)
        }
    }
}