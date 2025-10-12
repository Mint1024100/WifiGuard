package com.wifiguard.core.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.wifiguard.core.common.Constants
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * BroadcastReceiver для обработки событий загрузки системы.
 * Автоматически запускает фоновый мониторинг Wi-Fi после перезагрузки устройства.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var workManager: WorkManager
    
    companion object {
        private const val TAG = "${Constants.LOG_TAG}_BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "🚀 Система загружена - запуск фонового мониторинга")
                startBackgroundMonitoring(context)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d(TAG, "📦 Приложение обновлено - перезапуск мониторинга")
                startBackgroundMonitoring(context)
            }
            else -> {
                Log.d(TAG, "❓ Неизвестное действие: ${intent.action}")
            }
        }
    }
    
    /**
     * Запускает фоновый мониторинг Wi-Fi сетей
     */
    private fun startBackgroundMonitoring(context: Context) {
        try {
            // Создаем уникальную работу для мониторинга
            val monitoringWork = OneTimeWorkRequestBuilder<WifiMonitoringWorker>()
                .setInitialDelay(30, TimeUnit.SECONDS) // Задержка для стабилизации системы
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .addTag(Constants.WorkManagerTags.BACKGROUND_MONITORING)
                .build()
            
            // Запускаем работу
            workManager.enqueueUniqueWork(
                Constants.WorkManagerTags.BACKGROUND_MONITORING,
                ExistingWorkPolicy.REPLACE,
                monitoringWork
            )
            
            Log.d(TAG, "✅ Фоновый мониторинг запущен")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка запуска фонового мониторинга: ${e.message}", e)
        }
    }
}
