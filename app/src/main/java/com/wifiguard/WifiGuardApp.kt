package com.wifiguard

import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.wifiguard.core.background.WifiMonitoringWorker
import com.wifiguard.feature.settings.domain.repository.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.hilt.work.HiltWorkerFactory

/**
 * Главный класс приложения WifiGuard
 */
@HiltAndroidApp
class WifiGuardApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val applicationScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // Инициализация приложения
        initializeApp()
    }

    private fun initializeApp() {
        applicationScope.launch {
            settingsRepository.getAutoScanEnabled().collect { isEnabled ->
                if (isEnabled) {
                    val workManager = WorkManager.getInstance(this@WifiGuardApp)
                    workManager.enqueueUniquePeriodicWork(
                        "wifi_monitoring_work",
                        ExistingPeriodicWorkPolicy.UPDATE,
                        WifiMonitoringWorker.createPeriodicWork()
                    )
                } else {
                    val workManager = WorkManager.getInstance(this@WifiGuardApp)
                    workManager.cancelUniqueWork("wifi_monitoring_work")
                }
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}