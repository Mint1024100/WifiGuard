package com.wifiguard

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
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
import org.json.JSONObject
import javax.inject.Inject
import androidx.hilt.work.HiltWorkerFactory
import com.wifiguard.BuildConfig

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
        // runId уникальный на запуск приложения, чтобы проще сравнивать разные прогоны.
        val runId = DeviceDebugLogger.startNewRun()
        DeviceDebugLogger.logAppStart(this, runId)
        installCrashLogger(runId)
        
        // #region agent log
        // Логирование информации о версии приложения для диагностики проблемы обновления
        try {
            val packageInfo: PackageInfo = getPackageInfoCompat(flags = 0L)
            val logData = JSONObject().apply {
                put("versionCode", PackageInfoCompat.getLongVersionCode(packageInfo))
                put("versionName", packageInfo.versionName ?: "unknown")
                put("packageName", packageName)
                put("applicationId", BuildConfig.APPLICATION_ID)
                put("buildType", if (BuildConfig.DEBUG) "debug" else "release")
            }
            DeviceDebugLogger.log(
                context = this,
                runId = runId,
                hypothesisId = "A",
                location = "WifiGuardApp.kt:onCreate",
                message = "Информация о версии приложения (гипотеза A: versionCode)",
                data = logData
            )
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка логирования версии приложения: ${e.message}", e)
            DeviceDebugLogger.log(
                context = this,
                runId = runId,
                hypothesisId = "A",
                location = "WifiGuardApp.kt:onCreate",
                message = "Ошибка получения информации о версии",
                data = JSONObject().apply {
                    put("error", e.message ?: "unknown")
                }
            )
        }
        // #endregion
        
        // #region agent log
        // Логирование информации о подписи APK (гипотеза B: несоответствие подписи)
        try {
            val signatureHashes = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                // API 28+: используем SigningInfo вместо deprecated signatures/GET_SIGNATURES
                @Suppress("NewApi")
                val packageInfo = getPackageInfoCompat(flags = PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                @Suppress("NewApi")
                val signers = packageInfo.signingInfo?.apkContentsSigners ?: emptyArray()
                signers.map { it.toByteArray().contentHashCode().toString() }
            } else {
                // API < 28: альтернативы нет, используем legacy signatures.
                @Suppress("DEPRECATION")
                val packageInfo = getPackageInfoCompat(flags = PackageManager.GET_SIGNATURES.toLong())
                @Suppress("DEPRECATION")
                val signers = packageInfo.signatures ?: emptyArray()
                signers.map { it.toByteArray().contentHashCode().toString() }
            }
            DeviceDebugLogger.log(
                context = this,
                runId = runId,
                hypothesisId = "B",
                location = "WifiGuardApp.kt:onCreate",
                message = "Информация о подписи APK (гипотеза B: несоответствие подписи)",
                data = JSONObject().apply {
                    put("signatureHashes", signatureHashes.joinToString(","))
                    put("signatureCount", signatureHashes.size)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка логирования подписи APK: ${e.message}", e)
            DeviceDebugLogger.log(
                context = this,
                runId = runId,
                hypothesisId = "B",
                location = "WifiGuardApp.kt:onCreate",
                message = "Ошибка получения информации о подписи",
                data = JSONObject().apply {
                    put("error", e.message ?: "unknown")
                }
            )
        }
        // #endregion

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
                data = JSONObject().apply {
                    put("thread", t.name ?: "unknown")
                    put("errorType", e.javaClass.simpleName)
                    put("error", e.message ?: "unknown")
                    put("stack", e.stackTraceToString().take(4000))
                }
            )
            defaultHandler?.uncaughtException(t, e)
        }
    }

    private fun getPackageInfoCompat(flags: Long): PackageInfo {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, flags.toInt())
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