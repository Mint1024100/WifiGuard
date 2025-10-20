package com.wifiguard.feature.settings.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация источника данных для настроек
 */
@Singleton
class SettingsDataSourceImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsDataSource {

    companion object {
        private val AUTO_SCAN_ENABLED = booleanPreferencesKey("auto_scan_enabled")
        private val BACKGROUND_MONITORING = booleanPreferencesKey("background_monitoring")
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val NOTIFICATION_SOUND_ENABLED = booleanPreferencesKey("notification_sound_enabled")
        private val NOTIFICATION_VIBRATION_ENABLED = booleanPreferencesKey("notification_vibration_enabled")
        private val HIGH_PRIORITY_NOTIFICATIONS = booleanPreferencesKey("high_priority_notifications")
        private val SCAN_INTERVAL = intPreferencesKey("scan_interval")
        private val THREAT_SENSITIVITY = intPreferencesKey("threat_sensitivity")
        private val DATA_RETENTION_DAYS = intPreferencesKey("data_retention_days")
    }

    override fun getAutoScanEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[AUTO_SCAN_ENABLED] ?: true
        }
    }

    override suspend fun setAutoScanEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_SCAN_ENABLED] = enabled
        }
    }

    override fun getBackgroundMonitoring(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[BACKGROUND_MONITORING] ?: true
        }
    }

    override suspend fun setBackgroundMonitoring(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[BACKGROUND_MONITORING] = enabled
        }
    }

    override fun getNotificationsEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[NOTIFICATIONS_ENABLED] ?: true
        }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    override fun getHighPriorityNotifications(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[HIGH_PRIORITY_NOTIFICATIONS] ?: false
        }
    }

    override suspend fun setHighPriorityNotifications(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[HIGH_PRIORITY_NOTIFICATIONS] = enabled
        }
    }

    override fun getScanInterval(): Flow<Int> {
        return dataStore.data.map { preferences ->
            preferences[SCAN_INTERVAL] ?: 15
        }
    }

    override suspend fun setScanInterval(intervalMinutes: Int) {
        dataStore.edit { preferences ->
            preferences[SCAN_INTERVAL] = intervalMinutes
        }
    }

    override fun getThreatSensitivity(): Flow<Int> {
        return dataStore.data.map { preferences ->
            preferences[THREAT_SENSITIVITY] ?: 1
        }
    }

    override suspend fun setThreatSensitivity(sensitivity: Int) {
        dataStore.edit { preferences ->
            preferences[THREAT_SENSITIVITY] = sensitivity
        }
    }

    override fun getNotificationSoundEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[NOTIFICATION_SOUND_ENABLED] ?: true
        }
    }

    override suspend fun setNotificationSoundEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATION_SOUND_ENABLED] = enabled
        }
    }

    override fun getNotificationVibrationEnabled(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[NOTIFICATION_VIBRATION_ENABLED] ?: true
        }
    }

    override suspend fun setNotificationVibrationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATION_VIBRATION_ENABLED] = enabled
        }
    }

    override fun getDataRetentionDays(): Flow<Int> {
        return dataStore.data.map { preferences ->
            preferences[DATA_RETENTION_DAYS] ?: 30
        }
    }

    override suspend fun setDataRetentionDays(days: Int) {
        dataStore.edit { preferences ->
            preferences[DATA_RETENTION_DAYS] = days
        }
    }

    override fun getAllSettings(): Flow<com.wifiguard.core.data.preferences.AppSettings> {
        return dataStore.data.map { preferences ->
            com.wifiguard.core.data.preferences.AppSettings(
                autoScanEnabled = preferences[AUTO_SCAN_ENABLED] ?: true,
                scanIntervalMinutes = preferences[SCAN_INTERVAL] ?: 15,
                notificationsEnabled = preferences[NOTIFICATIONS_ENABLED] ?: true,
                notificationSoundEnabled = preferences[NOTIFICATION_SOUND_ENABLED] ?: true,
                notificationVibrationEnabled = preferences[NOTIFICATION_VIBRATION_ENABLED] ?: true,
                dataRetentionDays = preferences[DATA_RETENTION_DAYS] ?: 30,
                threatAlertEnabled = true, // Default value as it's not in SettingsDataSource
                criticalThreatNotifications = preferences[HIGH_PRIORITY_NOTIFICATIONS] ?: false,
                themeMode = "system", // Default value as it's not in SettingsDataSource
                language = "ru", // Default value as it's not in SettingsDataSource
                firstLaunch = true, // Default value as it's not in SettingsDataSource
                lastScanTimestamp = 0L, // Default value as it's not in SettingsDataSource
                totalScansCount = 0, // Default value as it's not in SettingsDataSource
                analyticsEnabled = false, // Default value as it's not in SettingsDataSource
                crashReportingEnabled = false // Default value as it's not in SettingsDataSource
            )
        }
    }

    override suspend fun updateSettings(settings: com.wifiguard.core.data.preferences.AppSettings) {
        dataStore.edit { preferences ->
            preferences[AUTO_SCAN_ENABLED] = settings.autoScanEnabled
            preferences[SCAN_INTERVAL] = settings.scanIntervalMinutes
            preferences[NOTIFICATIONS_ENABLED] = settings.notificationsEnabled
            preferences[NOTIFICATION_SOUND_ENABLED] = settings.notificationSoundEnabled
            preferences[NOTIFICATION_VIBRATION_ENABLED] = settings.notificationVibrationEnabled
            preferences[DATA_RETENTION_DAYS] = settings.dataRetentionDays
            preferences[HIGH_PRIORITY_NOTIFICATIONS] = settings.criticalThreatNotifications
        }
    }

    override suspend fun clearAllSettings() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
