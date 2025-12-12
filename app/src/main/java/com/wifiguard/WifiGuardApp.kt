package com.wifiguard

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import com.wifiguard.core.background.DataCleanupWorker
import com.wifiguard.core.background.ThreatNotificationWorker
import com.wifiguard.core.background.WifiMonitoringWorker
import com.wifiguard.core.common.Constants
import com.wifiguard.core.common.DeviceDebugLogger
import com.wifiguard.core.monitoring.WifiConnectionObserver
import com.wifiguard.feature.settings.domain.repository.SettingsRepository
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.hilt.work.HiltWorkerFactory

/**
 * Главный класс приложения WifiGuard
 * 
 * ОБНОВЛЕНО: Добавлен WifiConnectionObserver для автоматических уведомлений
 */
@HiltAndroidApp
class WifiGuardApp : Application(), Configuration.Provider {

    companion object {
        private const val TAG = "${Constants.LOG_TAG}_App"
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var settingsRepository: Lazy<SettingsRepository>
    
    @Inject
    lateinit var wifiConnectionObserver: Lazy<WifiConnectionObserver>

    // Используем SupervisorJob для изоляции ошибок
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "🚀 Запуск приложения WifiGuard")

        // ВАЖНО: для диагностики падений/пустых сканов пишем NDJSON-лог на устройстве.
        // runId фиксированный, чтобы логи из одного запуска группировались.
        val runId = "run1"
        DeviceDebugLogger.logAppStart(this, runId)
        installCrashLogger(runId)

        // Инициализация приложения
        initializeApp()
        
        // НОВОЕ: Запуск наблюдателя WiFi подключений
        startWifiConnectionObserver()
    }

    private fun installCrashLogger(runId: String) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            DeviceDebugLogger.log(
                context = this,
                runId = runId,
                hypothesisId = "CRASH",
                location = "WifiGuardApp.kt:installCrashLogger",
                message = "Необработанное исключение (краш)",
                data = org.json.JSONObject().apply {
                    put("thread", t.name ?: "unknown")
                    put("errorType", e.javaClass.simpleName)
                    put("error", e.message ?: "unknown")
                    put("stack", e.stackTraceToString().take(4000))
                }
            )
            defaultHandler?.uncaughtException(t, e)
        }
    }

    private fun initializeApp() {
        applicationScope.launch {
            val workManager = WorkManager.getInstance(this@WifiGuardApp)

            // Убираем дубли, созданные старыми версиями приложения (разные имена unique-work).
            // ВАЖНО: отмена безопасна - новые имена будут поставлены заново ниже по настройкам.
            workManager.cancelUniqueWork("wifi_monitoring_work")
            workManager.cancelUniqueWork("wifi_monitoring_periodic")
            workManager.cancelUniqueWork("threat_notification_work")
            workManager.cancelUniqueWork("threat_notification_periodic")

            // Периодическая очистка БД (раз в сутки) - независимо от UI.
            workManager.enqueueUniquePeriodicWork(
                Constants.WORK_NAME_DATA_CLEANUP,
                ExistingPeriodicWorkPolicy.KEEP,
                DataCleanupWorker.createPeriodicWork()
            )

            // Периодическая отправка уведомлений по критическим угрозам.
            workManager.enqueueUniquePeriodicWork(
                Constants.WORK_NAME_THREAT_NOTIFICATION,
                ExistingPeriodicWorkPolicy.UPDATE,
                ThreatNotificationWorker.createPeriodicWork()
            )

            settingsRepository.get()
                .getAutoScanEnabled()
                .distinctUntilChanged()
                .collect { isEnabled ->
                if (isEnabled) {
                    workManager.enqueueUniquePeriodicWork(
                        Constants.WORK_NAME_WIFI_MONITORING,
                        ExistingPeriodicWorkPolicy.UPDATE,
                        WifiMonitoringWorker.createPeriodicWork()
                    )
                    Log.d(TAG, "✅ Автоматическое сканирование включено")
                } else {
                    workManager.cancelUniqueWork(Constants.WORK_NAME_WIFI_MONITORING)
                    Log.d(TAG, "🔕 Автоматическое сканирование отключено")
                }
            }
        }
    }
    
    /**
     * Запустить наблюдатель WiFi подключений для автоматических уведомлений
     * 
     * РЕШЕНИЕ ПРОБЛЕМЫ 1.1: Теперь уведомления об угрозах приходят автоматически
     * при подключении к небезопасной сети, а не только при ручном сканировании.
     */
    private fun startWifiConnectionObserver() {
        applicationScope.launch {
            try {
                wifiConnectionObserver.get().startObserving(applicationScope)
                Log.d(TAG, "✅ WifiConnectionObserver запущен - уведомления об угрозах активны")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Ошибка запуска WifiConnectionObserver: ${e.message}", e)
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}