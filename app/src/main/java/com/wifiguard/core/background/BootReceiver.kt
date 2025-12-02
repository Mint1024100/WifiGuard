package com.wifiguard.core.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
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
            // Проверяем наличие необходимых разрешений перед запуском фоновой задачи
            if (!hasRequiredPermissions(context)) {
                Log.w(TAG, "Required permissions not granted, skipping background monitoring")
                return
            }
            
            val workManager = WorkManager.getInstance(context)
            
            // Запускаем периодический мониторинг Wi-Fi
            val wifiPeriodicWork = WifiMonitoringWorker.createPeriodicWork()
            workManager.enqueueUniquePeriodicWork(
                "wifi_monitoring_periodic",
                ExistingPeriodicWorkPolicy.KEEP,
                wifiPeriodicWork
            )
            
            // Запускаем периодический мониторинг уведомлений об угрозах
            val notificationPeriodicWork = ThreatNotificationWorker.createPeriodicWork()
            workManager.enqueueUniquePeriodicWork(
                "threat_notification_periodic",
                ExistingPeriodicWorkPolicy.KEEP,
                notificationPeriodicWork
            )
            
            Log.d(TAG, "Background monitoring and threat notifications started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start background monitoring: ${e.message}", e)
        }
    }
    
    private fun hasRequiredPermissions(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requires NEARBY_WIFI_DEVICES
            val locationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val nearbyWifiPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.NEARBY_WIFI_DEVICES
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            locationPermission && nearbyWifiPermission
        } else {
            // Android 6-12 requires ACCESS_FINE_LOCATION
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
}

/**
 * Extension для создания периодической работы для уведомлений
 */
fun ThreatNotificationWorker.Companion.createPeriodicWork(): PeriodicWorkRequest {
    return PeriodicWorkRequestBuilder<ThreatNotificationWorker>(
        5, java.util.concurrent.TimeUnit.MINUTES
    )
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .build()
        )
        .addTag("threat_notification")
        .build()
}

/**
 * Extension для создания периодической работы для мониторинга Wi-Fi
 */
fun WifiMonitoringWorker.Companion.createPeriodicWork(): PeriodicWorkRequest {
    return PeriodicWorkRequestBuilder<WifiMonitoringWorker>(
        15, java.util.concurrent.TimeUnit.MINUTES
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