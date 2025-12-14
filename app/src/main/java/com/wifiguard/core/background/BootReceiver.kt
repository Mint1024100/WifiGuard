package com.wifiguard.core.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.ExistingPeriodicWorkPolicy
import com.wifiguard.core.common.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Receiver для обработки событий загрузки системы
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "${'$'}{Constants.LOG_TAG}_BootReceiver"
        
        // DataStore extension for getting scan interval
        private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d(TAG, "System boot completed, starting background monitoring")
                // ВАЖНО: BroadcastReceiver имеет жёсткие таймауты. Нельзя блокировать поток через runBlocking().
                val pendingResult = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        startBackgroundMonitoring(context)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private suspend fun startBackgroundMonitoring(context: Context) {
        try {
            // Проверяем наличие необходимых разрешений перед запуском фоновой задачи
            if (!hasRequiredPermissions(context)) {
                Log.w(TAG, "Required permissions not granted, skipping background monitoring")
                return
            }

            val workManager = WorkManagerSafe.getInstanceOrNull(context)
            if (workManager == null) {
                Log.w(TAG, "WorkManager недоступен. Фоновый мониторинг после загрузки пропущен.")
                return
            }
            
            // Get the scan interval from DataStore
            val scanIntervalMinutes = getScanIntervalFromDataStore(context)
            
            // Create periodic work with the user's preferred interval
            val wifiPeriodicWork = WifiMonitoringWorker.createPeriodicWorkWithInterval(scanIntervalMinutes)
            workManager.enqueueUniquePeriodicWork(
                Constants.WORK_NAME_WIFI_MONITORING,
                ExistingPeriodicWorkPolicy.UPDATE,
                wifiPeriodicWork
            )

            // Запускаем периодический мониторинг уведомлений об угрозах
            val notificationPeriodicWork = ThreatNotificationWorker.createPeriodicWork()
            workManager.enqueueUniquePeriodicWork(
                Constants.WORK_NAME_THREAT_NOTIFICATION,
                ExistingPeriodicWorkPolicy.UPDATE,
                notificationPeriodicWork
            )

            Log.d(TAG, "Background monitoring and threat notifications started successfully with interval: ${'$'}{scanIntervalMinutes} minutes")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start background monitoring: ${'$'}{e.message}", e)
        }
    }
    
    private suspend fun getScanIntervalFromDataStore(context: Context): Int {
        val scanIntervalKey = intPreferencesKey("scan_interval")
        return try {
            val preferences = context.settingsDataStore.data.first()
            preferences[scanIntervalKey] ?: 15
        } catch (e: Exception) {
            Log.e(TAG, "Error reading scan interval from DataStore: ${'$'}{e.message}")
            15
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