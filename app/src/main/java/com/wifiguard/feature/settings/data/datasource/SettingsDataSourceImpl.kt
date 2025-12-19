package com.wifiguard.feature.settings.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ИСПРАВЛЕНО: Реализация источника данных для настроек с правильной обработкой ошибок
 * 
 * ИСПРАВЛЕНИЯ:
 * ✅ Правильная обработка ошибок чтения/записи
 * ✅ Cancellation safety - не перехватывает CancellationException
 * ✅ Graceful fallback для IOException
 * ✅ Thread-safe операции
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
        private val THEME_MODE = androidx.datastore.preferences.core.stringPreferencesKey("theme_mode")
        private val AUTO_DISABLE_WIFI_ON_CRITICAL = booleanPreferencesKey("auto_disable_wifi_on_critical")
    }

    /**
     * ИСПРАВЛЕНО: Get auto-scan enabled с правильной обработкой ошибок
     */
    override fun getAutoScanEnabled(): Flow<Boolean> {
        return dataStore.data
            .catch { exception ->
                if (exception is CancellationException) throw exception
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    android.util.Log.e("SettingsDataSource", "Error reading auto-scan enabled", exception)
                    emit(emptyPreferences())
                }
            }
            .map { preferences ->
                preferences[AUTO_SCAN_ENABLED] ?: true
            }
    }

    /**
     * ИСПРАВЛЕНО: Set auto-scan enabled с обработкой ошибок
     */
    override suspend fun setAutoScanEnabled(enabled: Boolean) {
        try {
            dataStore.edit { preferences ->
                preferences[AUTO_SCAN_ENABLED] = enabled
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("SettingsDataSource", "Error writing auto-scan enabled", e)
            throw e
        }
    }

    /**
     * ИСПРАВЛЕНО: Все методы чтения с правильной обработкой ошибок
     */
    override fun getBackgroundMonitoring(): Flow<Boolean> {
        return dataStore.data
            .catch { exception ->
                if (exception is CancellationException) throw exception
                if (exception is IOException) emit(emptyPreferences())
                else {
                    android.util.Log.e("SettingsDataSource", "Error reading background monitoring", exception)
                    emit(emptyPreferences())
                }
            }
            .map { preferences ->
                preferences[BACKGROUND_MONITORING] ?: true
            }
    }

    override suspend fun setBackgroundMonitoring(enabled: Boolean) {
        try {
            dataStore.edit { preferences ->
                preferences[BACKGROUND_MONITORING] = enabled
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("SettingsDataSource", "Error writing background monitoring", e)
            throw e
        }
    }

    override fun getNotificationsEnabled(): Flow<Boolean> {
        return dataStore.data
            .catch { exception ->
                if (exception is CancellationException) throw exception
                if (exception is IOException) emit(emptyPreferences())
                else {
                    android.util.Log.e("SettingsDataSource", "Error reading notifications enabled", exception)
                    emit(emptyPreferences())
                }
            }
            .map { preferences ->
                preferences[NOTIFICATIONS_ENABLED] ?: true
            }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        try {
            dataStore.edit { preferences ->
                preferences[NOTIFICATIONS_ENABLED] = enabled
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("SettingsDataSource", "Error writing notifications enabled", e)
            throw e
        }
    }

    override fun getHighPriorityNotifications(): Flow<Boolean> {
        return dataStore.data
            .catch { exception ->
                if (exception is CancellationException) throw exception
                if (exception is IOException) emit(emptyPreferences())
                else {
                    android.util.Log.e("SettingsDataSource", "Error reading high priority notifications", exception)
                    emit(emptyPreferences())
                }
            }
            .map { preferences ->
                preferences[HIGH_PRIORITY_NOTIFICATIONS] ?: false
            }
    }

    override suspend fun setHighPriorityNotifications(enabled: Boolean) {
        try {
            dataStore.edit { preferences ->
                preferences[HIGH_PRIORITY_NOTIFICATIONS] = enabled
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("SettingsDataSource", "Error writing high priority notifications", e)
            throw e
        }
    }

    override fun getScanInterval(): Flow<Int> {
        return dataStore.data
            .catch { exception ->
                if (exception is CancellationException) throw exception
                if (exception is IOException) emit(emptyPreferences())
                else {
                    android.util.Log.e("SettingsDataSource", "Error reading scan interval", exception)
                    emit(emptyPreferences())
                }
            }
            .map { preferences ->
                preferences[SCAN_INTERVAL] ?: 15
            }
    }

    override suspend fun setScanInterval(intervalMinutes: Int) {
        try {
            dataStore.edit { preferences ->
                preferences[SCAN_INTERVAL] = intervalMinutes
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("SettingsDataSource", "Error writing scan interval", e)
            throw e
        }
    }

    override fun getThreatSensitivity(): Flow<Int> {
        return dataStore.data
            .catch { exception ->
                if (exception is CancellationException) throw exception
                if (exception is IOException) emit(emptyPreferences())
                else {
                    android.util.Log.e("SettingsDataSource", "Error reading threat sensitivity", exception)
                    emit(emptyPreferences())
                }
            }
            .map { preferences ->
                preferences[THREAT_SENSITIVITY] ?: 1
            }
    }

    override suspend fun setThreatSensitivity(sensitivity: Int) {
        try {
            dataStore.edit { preferences ->
                preferences[THREAT_SENSITIVITY] = sensitivity
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("SettingsDataSource", "Error writing threat sensitivity", e)
            throw e
        }
    }

    override fun getNotificationSoundEnabled(): Flow<Boolean> {
        return dataStore.data
            .catch { exception ->
                if (exception is CancellationException) throw exception
                if (exception is IOException) emit(emptyPreferences())
                else {
                    android.util.Log.e("SettingsDataSource", "Error reading notification sound", exception)
                    emit(emptyPreferences())
                }
            }
            .map { preferences ->
                preferences[NOTIFICATION_SOUND_ENABLED] ?: true
            }
    }

    override suspend fun setNotificationSoundEnabled(enabled: Boolean) {
        try {
            dataStore.edit { preferences ->
                preferences[NOTIFICATION_SOUND_ENABLED] = enabled
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("SettingsDataSource", "Error writing notification sound", e)
            throw e
        }
    }

    override fun getNotificationVibrationEnabled(): Flow<Boolean> {
        return dataStore.data
            .catch { exception ->
                if (exception is CancellationException) throw exception
                if (exception is IOException) emit(emptyPreferences())
                else {
                    android.util.Log.e("SettingsDataSource", "Error reading notification vibration", exception)
                    emit(emptyPreferences())
                }
            }
            .map { preferences ->
                preferences[NOTIFICATION_VIBRATION_ENABLED] ?: true
            }
    }

    override suspend fun setNotificationVibrationEnabled(enabled: Boolean) {
        try {
            dataStore.edit { preferences ->
                preferences[NOTIFICATION_VIBRATION_ENABLED] = enabled
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("SettingsDataSource", "Error writing notification vibration", e)
            throw e
        }
    }

    override fun getDataRetentionDays(): Flow<Int> {
        return dataStore.data
            .catch { exception ->
                if (exception is CancellationException) throw exception
                if (exception is IOException) emit(emptyPreferences())
                else {
                    android.util.Log.e("SettingsDataSource", "Error reading data retention days", exception)
                    emit(emptyPreferences())
                }
            }
            .map { preferences ->
                preferences[DATA_RETENTION_DAYS] ?: 30
            }
    }

    override suspend fun setDataRetentionDays(days: Int) {
        try {
            dataStore.edit { preferences ->
                preferences[DATA_RETENTION_DAYS] = days
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("SettingsDataSource", "Error writing data retention days", e)
            throw e
        }
    }

    override fun getThemeMode(): Flow<String> {
        return dataStore.data
            .catch { exception ->
                if (exception is CancellationException) throw exception
                if (exception is IOException) emit(emptyPreferences())
                else {
                    android.util.Log.e("SettingsDataSource", "Error reading theme mode", exception)
                    emit(emptyPreferences())
                }
            }
            .map { preferences ->
                preferences[THEME_MODE] ?: "system"
            }
    }

    override suspend fun setThemeMode(mode: String) {
        try {
            dataStore.edit { preferences ->
                preferences[THEME_MODE] = mode
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("SettingsDataSource", "Error writing theme mode", e)
            throw e
        }
    }

    override fun getAutoDisableWifiOnCritical(): Flow<Boolean> {
        return dataStore.data
            .catch { exception ->
                if (exception is CancellationException) throw exception
                if (exception is IOException) emit(emptyPreferences())
                else {
                    android.util.Log.e(
                        "SettingsDataSource",
                        "Error reading auto-disable wifi on critical",
                        exception
                    )
                    emit(emptyPreferences())
                }
            }
            .map { preferences ->
                preferences[AUTO_DISABLE_WIFI_ON_CRITICAL] ?: false
            }
    }

    override suspend fun setAutoDisableWifiOnCritical(enabled: Boolean) {
        try {
            dataStore.edit { preferences ->
                preferences[AUTO_DISABLE_WIFI_ON_CRITICAL] = enabled
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("SettingsDataSource", "Error writing auto-disable wifi on critical", e)
            throw e
        }
    }

    /**
     * ИСПРАВЛЕНО: Get all settings с правильной обработкой ошибок
     */
    override fun getAllSettings(): Flow<com.wifiguard.core.data.preferences.AppSettings> {
        return dataStore.data
            .catch { exception ->
                if (exception is CancellationException) throw exception
                if (exception is IOException) emit(emptyPreferences())
                else {
                    android.util.Log.e("SettingsDataSource", "Error reading all settings", exception)
                    emit(emptyPreferences())
                }
            }
            .map { preferences ->
                com.wifiguard.core.data.preferences.AppSettings(
                    autoScanEnabled = preferences[AUTO_SCAN_ENABLED] ?: true,
                    scanIntervalMinutes = preferences[SCAN_INTERVAL] ?: 15,
                    notificationsEnabled = preferences[NOTIFICATIONS_ENABLED] ?: true,
                    notificationSoundEnabled = preferences[NOTIFICATION_SOUND_ENABLED] ?: true,
                    notificationVibrationEnabled = preferences[NOTIFICATION_VIBRATION_ENABLED] ?: true,
                    dataRetentionDays = preferences[DATA_RETENTION_DAYS] ?: 30,
                    threatAlertEnabled = true, // Default value as it's not in SettingsDataSource
                    criticalThreatNotifications = preferences[HIGH_PRIORITY_NOTIFICATIONS] ?: false,
                    autoDisableWifiOnCritical = preferences[AUTO_DISABLE_WIFI_ON_CRITICAL] ?: false,
                    themeMode = preferences[THEME_MODE] ?: "system",
                    language = "ru", // Default value as it's not in SettingsDataSource
                    firstLaunch = true, // Default value as it's not in SettingsDataSource
                    lastScanTimestamp = 0L, // Default value as it's not in SettingsDataSource
                    totalScansCount = 0, // Default value as it's not in SettingsDataSource
                    crashReportingEnabled = false // Default value as it's not in SettingsDataSource
                )
            }
    }

    /**
     * ИСПРАВЛЕНО: Update settings с обработкой ошибок
     */
    override suspend fun updateSettings(settings: com.wifiguard.core.data.preferences.AppSettings) {
        try {
            dataStore.edit { preferences ->
                preferences[AUTO_SCAN_ENABLED] = settings.autoScanEnabled
                preferences[SCAN_INTERVAL] = settings.scanIntervalMinutes
                preferences[NOTIFICATIONS_ENABLED] = settings.notificationsEnabled
                preferences[NOTIFICATION_SOUND_ENABLED] = settings.notificationSoundEnabled
                preferences[NOTIFICATION_VIBRATION_ENABLED] = settings.notificationVibrationEnabled
                preferences[DATA_RETENTION_DAYS] = settings.dataRetentionDays
                preferences[HIGH_PRIORITY_NOTIFICATIONS] = settings.criticalThreatNotifications
                preferences[AUTO_DISABLE_WIFI_ON_CRITICAL] = settings.autoDisableWifiOnCritical
                preferences[THEME_MODE] = settings.themeMode
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("SettingsDataSource", "Error updating settings", e)
            throw e
        }
    }

    /**
     * ИСПРАВЛЕНО: Clear all settings с обработкой ошибок
     */
    override suspend fun clearAllSettings() {
        try {
            dataStore.edit { preferences ->
                preferences.clear()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("SettingsDataSource", "Error clearing settings", e)
            throw e
        }
    }

}
