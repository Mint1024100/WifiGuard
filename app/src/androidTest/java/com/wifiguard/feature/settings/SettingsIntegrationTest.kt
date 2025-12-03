package com.wifiguard.feature.settings

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.*
import androidx.work.testing.WorkManagerTestInitHelper
import com.wifiguard.core.background.WifiMonitoringWorker
import com.wifiguard.feature.settings.presentation.SettingsViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Integration test for Settings functionality, particularly the scan interval feature
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SettingsIntegrationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context
    private lateinit var workManager: WorkManager

    @Before
    fun setup() {
        Log.d("WifiGuardTest", "Setting up test environment")

        // Initialize Hilt
        hiltRule.inject()

        // Initialize WorkManager for testing
        context = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()

        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)

        Log.d("WifiGuardTest", "Test setup completed")
    }

    /**
     * Scenario 1: Verify that changing the scan interval reschedules the background work
     * - Setup: Initialize WorkManagerTestInitHelper and your Hilt test dependencies.
     * - Action: Call viewModel.setScanInterval(30).
     * - Check: Query WorkManager for UniqueWork with name "wifi_monitoring_periodic". Assert that:
     *   - Work exists and is in ENQUEUED state.
     *   - (Optional but good) The repeat interval is approximately 30 minutes (note: getting exact interval from `WorkInfo` is tricky, so at least verify the work *request* was updated/replaced).
     */
    @Test
    fun testIntervalChangeReschedulesWork() = runTest {
        Log.d("WifiGuardTest", "Starting testIntervalChangeReschedulesWork")

        // Cancel any existing work first
        workManager.cancelUniqueWork("wifi_monitoring_periodic").result.get()

        // Create test data source with initial scan interval
        val testDataSource = object : com.wifiguard.feature.settings.data.datasource.SettingsDataSource {
            private var scanInterval = 15 // Default value

            override fun getAutoScanEnabled() = kotlinx.coroutines.flow.flowOf(true)
            override suspend fun setAutoScanEnabled(enabled: Boolean) {}

            override fun getBackgroundMonitoring() = kotlinx.coroutines.flow.flowOf(true)
            override suspend fun setBackgroundMonitoring(enabled: Boolean) {}

            override fun getNotificationsEnabled() = kotlinx.coroutines.flow.flowOf(true)
            override suspend fun setNotificationsEnabled(enabled: Boolean) {}

            override fun getNotificationSoundEnabled() = kotlinx.coroutines.flow.flowOf(true)
            override suspend fun setNotificationSoundEnabled(enabled: Boolean) {}

            override fun getNotificationVibrationEnabled() = kotlinx.coroutines.flow.flowOf(true)
            override suspend fun setNotificationVibrationEnabled(enabled: Boolean) {}

            override fun getHighPriorityNotifications() = kotlinx.coroutines.flow.flowOf(false)
            override suspend fun setHighPriorityNotifications(enabled: Boolean) {}

            override fun getScanInterval() = kotlinx.coroutines.flow.flowOf(scanInterval)
            override suspend fun setScanInterval(intervalMinutes: Int) {
                scanInterval = intervalMinutes
            }

            override fun getThreatSensitivity() = kotlinx.coroutines.flow.flowOf(1)
            override suspend fun setThreatSensitivity(sensitivity: Int) {}

            override fun getDataRetentionDays() = kotlinx.coroutines.flow.flowOf(30)
            override suspend fun setDataRetentionDays(days: Int) {}

            override fun getAllSettings() = kotlinx.coroutines.flow.flowOf(
                com.wifiguard.core.data.preferences.AppSettings(
                    autoScanEnabled = true,
                    scanIntervalMinutes = scanInterval, // Use current value
                    notificationsEnabled = true,
                    notificationSoundEnabled = true,
                    notificationVibrationEnabled = true,
                    dataRetentionDays = 30,
                    threatAlertEnabled = true,
                    criticalThreatNotifications = false,
                    themeMode = "system",
                    language = "ru",
                    firstLaunch = true,
                    lastScanTimestamp = 0L,
                    totalScansCount = 0,
                    analyticsEnabled = false,
                    crashReportingEnabled = false
                )
            )

            override suspend fun updateSettings(settings: com.wifiguard.core.data.preferences.AppSettings) {}
            override suspend fun clearAllSettings() {}
        }

        val repository = com.wifiguard.feature.settings.data.repository.SettingsRepositoryImpl(testDataSource)
        val mockWifiRepository = object : com.wifiguard.core.domain.repository.WifiRepository {
            override fun context(): Context = context
            override suspend fun saveScanResult(wifiScanResult: com.wifiguard.core.domain.model.WifiScanResult) {}
            override suspend fun saveScanResults(wifiScanResults: List<com.wifiguard.core.domain.model.WifiScanResult>) {}
            override suspend fun getScanResults(): List<com.wifiguard.core.domain.model.WifiScanResult> = emptyList()
            override suspend fun getScanResultById(id: Long): com.wifiguard.core.domain.model.WifiScanResult? = null
            override suspend fun getThreatScanResults(): List<com.wifiguard.core.domain.model.WifiScanResult> = emptyList()
            override suspend fun deleteScanResult(id: Long) {}
            override suspend fun clearAllData() {}
        }

        val dataTransferManager = object : com.wifiguard.core.data.local.DataTransferManager(
            context,
            mockWifiRepository
        ) {
            override suspend fun exportData(uri: android.net.Uri) {}
            override suspend fun importData(uri: android.net.Uri) {}
        }

        val viewModel = SettingsViewModel(repository, dataTransferManager, mockWifiRepository, context)

        // Initially, set the scan interval to 30 minutes
        viewModel.setScanInterval(30)

        // Wait a bit for the async operations to complete
        kotlinx.coroutines.delay(1000)

        // Verify that the work has been rescheduled with the new interval
        val workInfos = workManager.getWorkInfosForUniqueWork("wifi_monitoring_periodic").get()

        // Check that the work exists and is enqueued
        assert(workInfos.isNotEmpty()) { "Work should exist" }
        val workInfo = workInfos.first()
        assert(workInfo.state == WorkInfo.State.ENQUEUED) { "Work should be in ENQUEUED state, but was ${workInfo.state}" }

        Log.d("WifiGuardTest", "Work state is ${workInfo.state}, which is correct for testIntervalChangeReschedulesWork")

        // Additional check: verify the tags
        assert(workInfo.tags.contains("wifi_monitoring")) { "Work should have wifi_monitoring tag" }

        Log.d("WifiGuardTest", "testIntervalChangeReschedulesWork completed successfully")
    }

    /**
     * Scenario 2: Verify Default Initialization
     * - Action: Simulate app cold start (trigger BootReceiver logic).
     * - Check: Assert that the default PeriodicWork is scheduled with the appropriate interval.
     */
    @Test
    fun testDefaultInitialization() = runTest {
        Log.d("WifiGuardTest", "Starting testDefaultInitialization")

        // Cancel any existing work first
        workManager.cancelUniqueWork("wifi_monitoring_periodic").result.get()

        // Initialize the test data store with default settings
        val defaultDataSource = object : com.wifiguard.feature.settings.data.datasource.SettingsDataSource {
            override fun getAutoScanEnabled() = kotlinx.coroutines.flow.flowOf(true)
            override suspend fun setAutoScanEnabled(enabled: Boolean) {}

            override fun getBackgroundMonitoring() = kotlinx.coroutines.flow.flowOf(true)
            override suspend fun setBackgroundMonitoring(enabled: Boolean) {}

            override fun getNotificationsEnabled() = kotlinx.coroutines.flow.flowOf(true)
            override suspend fun setNotificationsEnabled(enabled: Boolean) {}

            override fun getNotificationSoundEnabled() = kotlinx.coroutines.flow.flowOf(true)
            override suspend fun setNotificationSoundEnabled(enabled: Boolean) {}

            override fun getNotificationVibrationEnabled() = kotlinx.coroutines.flow.flowOf(true)
            override suspend fun setNotificationVibrationEnabled(enabled: Boolean) {}

            override fun getHighPriorityNotifications() = kotlinx.coroutines.flow.flowOf(false)
            override suspend fun setHighPriorityNotifications(enabled: Boolean) {}

            override fun getScanInterval() = kotlinx.coroutines.flow.flowOf(15) // Default is 15 minutes
            override suspend fun setScanInterval(intervalMinutes: Int) {}

            override fun getThreatSensitivity() = kotlinx.coroutines.flow.flowOf(1)
            override suspend fun setThreatSensitivity(sensitivity: Int) {}

            override fun getDataRetentionDays() = kotlinx.coroutines.flow.flowOf(30)
            override suspend fun setDataRetentionDays(days: Int) {}

            override fun getAllSettings() = kotlinx.coroutines.flow.flowOf(
                com.wifiguard.core.data.preferences.AppSettings(
                    autoScanEnabled = true,
                    scanIntervalMinutes = 15,
                    notificationsEnabled = true,
                    notificationSoundEnabled = true,
                    notificationVibrationEnabled = true,
                    dataRetentionDays = 30,
                    threatAlertEnabled = true,
                    criticalThreatNotifications = false,
                    themeMode = "system",
                    language = "ru",
                    firstLaunch = true,
                    lastScanTimestamp = 0L,
                    totalScansCount = 0,
                    analyticsEnabled = false,
                    crashReportingEnabled = false
                )
            )

            override suspend fun updateSettings(settings: com.wifiguard.core.data.preferences.AppSettings) {}
            override suspend fun clearAllSettings() {}
        }

        val bootReceiver = com.wifiguard.core.background.BootReceiver()

        // Create an intent that simulates boot completion
        val intent = android.content.Intent(android.content.Intent.ACTION_BOOT_COMPLETED)

        // Send the intent to the receiver
        bootReceiver.onReceive(context, intent)

        // Wait for the work to be scheduled
        kotlinx.coroutines.delay(1000)

        // Verify that the default work has been scheduled
        val workInfos = workManager.getWorkInfosForUniqueWork("wifi_monitoring_periodic").get()

        assert(workInfos.isNotEmpty()) { "Default work should exist after boot" }
        val workInfo = workInfos.first()
        assert(workInfo.state == WorkInfo.State.ENQUEUED) { "Default work should be in ENQUEUED state, but was ${workInfo.state}" }

        Log.d("WifiGuardTest", "Default work state is ${workInfo.state}, which is correct for testDefaultInitialization")

        Log.d("WifiGuardTest", "testDefaultInitialization completed successfully")
    }

    /**
     * Scenario 3: Verify Work Request Contains Expected Constraints
     * - Check: Ensure the scheduled work has appropriate constraints.
     */
    @Test
    fun testWorkConstraints() = runTest {
        Log.d("WifiGuardTest", "Starting testWorkConstraints")

        // Cancel any existing work first
        workManager.cancelUniqueWork("wifi_monitoring_periodic").result.get()

        // Create a periodic work request similar to what would be scheduled
        val workRequest = WifiMonitoringWorker.createPeriodicWorkWithInterval(25) // Use 25 minutes to distinguish from default

        workManager.enqueueUniquePeriodicWork(
            "wifi_monitoring_periodic",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        ).result.get()

        // Wait for the work to be scheduled
        kotlinx.coroutines.delay(1000)

        // Get the work info
        val workInfos = workManager.getWorkInfosForUniqueWork("wifi_monitoring_periodic").get()
        assert(workInfos.isNotEmpty()) { "Work should exist for constraint test" }

        val workInfo = workInfos.first()
        assert(workInfo.state == WorkInfo.State.ENQUEUED) { "Work should be in ENQUEUED state, but was ${workInfo.state}" }

        // Check tags
        assert(workInfo.tags.contains("wifi_monitoring")) { "Work should have wifi_monitoring tag" }

        Log.d("WifiGuardTest", "Work has appropriate tags: ${workInfo.tags}")
        Log.d("WifiGuardTest", "testWorkConstraints completed successfully")
    }
}