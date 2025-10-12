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
        private val HIGH_PRIORITY_NOTIFICATIONS = booleanPreferencesKey("high_priority_notifications")
        private val SCAN_INTERVAL = intPreferencesKey("scan_interval")
        private val THREAT_SENSITIVITY = intPreferencesKey("threat_sensitivity")
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
}
