package com.wifiguard.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore wrapper for managing app preferences
 */
@Singleton
class PreferencesDataSource @Inject constructor(
    private val context: Context
) {
    
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wifiguard_preferences")
    
    /**
     * Get auto-scan enabled setting
     */
    fun getAutoScanEnabled(): Flow<Boolean> {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.AUTO_SCAN_ENABLED] ?: true
            }
    }
    
    /**
     * Set auto-scan enabled setting
     */
    suspend fun setAutoScanEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_SCAN_ENABLED] = enabled
        }
    }
    
    /**
     * Get scan interval in minutes
     */
    fun getScanIntervalMinutes(): Flow<Int> {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.SCAN_INTERVAL_MINUTES] ?: 15
            }
    }
    
    /**
     * Set scan interval in minutes
     */
    suspend fun setScanIntervalMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SCAN_INTERVAL_MINUTES] = minutes
        }
    }
    
    /**
     * Get notifications enabled setting
     */
    fun getNotificationsEnabled(): Flow<Boolean> {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true
            }
    }
    
    /**
     * Set notifications enabled setting
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }
    
    /**
     * Get notification sound enabled setting
     */
    fun getNotificationSoundEnabled(): Flow<Boolean> {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.NOTIFICATION_SOUND_ENABLED] ?: true
            }
    }
    
    /**
     * Set notification sound enabled setting
     */
    suspend fun setNotificationSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_SOUND_ENABLED] = enabled
        }
    }
    
    /**
     * Get notification vibration enabled setting
     */
    fun getNotificationVibrationEnabled(): Flow<Boolean> {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.NOTIFICATION_VIBRATION_ENABLED] ?: true
            }
    }
    
    /**
     * Set notification vibration enabled setting
     */
    suspend fun setNotificationVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_VIBRATION_ENABLED] = enabled
        }
    }
    
    /**
     * Get data retention days
     */
    fun getDataRetentionDays(): Flow<Int> {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.DATA_RETENTION_DAYS] ?: 30
            }
    }
    
    /**
     * Set data retention days
     */
    suspend fun setDataRetentionDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DATA_RETENTION_DAYS] = days
        }
    }
    
    /**
     * Get all settings as a flow
     */
    fun getAllSettings(): Flow<AppSettings> {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                AppSettings(
                    autoScanEnabled = preferences[PreferencesKeys.AUTO_SCAN_ENABLED] ?: true,
                    scanIntervalMinutes = preferences[PreferencesKeys.SCAN_INTERVAL_MINUTES] ?: 15,
                    notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
                    notificationSoundEnabled = preferences[PreferencesKeys.NOTIFICATION_SOUND_ENABLED] ?: true,
                    notificationVibrationEnabled = preferences[PreferencesKeys.NOTIFICATION_VIBRATION_ENABLED] ?: true,
                    dataRetentionDays = preferences[PreferencesKeys.DATA_RETENTION_DAYS] ?: 30,
                    threatAlertEnabled = preferences[PreferencesKeys.THREAT_ALERT_ENABLED] ?: true,
                    criticalThreatNotifications = preferences[PreferencesKeys.CRITICAL_THREAT_NOTIFICATIONS] ?: true,
                    themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "system",
                    language = preferences[PreferencesKeys.LANGUAGE] ?: "ru",
                    firstLaunch = preferences[PreferencesKeys.FIRST_LAUNCH] ?: true,
                    lastScanTimestamp = preferences[PreferencesKeys.LAST_SCAN_TIMESTAMP] ?: 0L,
                    totalScansCount = preferences[PreferencesKeys.TOTAL_SCANS_COUNT] ?: 0,
                    analyticsEnabled = preferences[PreferencesKeys.ANALYTICS_ENABLED] ?: false,
                    crashReportingEnabled = preferences[PreferencesKeys.CRASH_REPORTING_ENABLED] ?: false
                )
            }
    }
    
    /**
     * Update multiple settings at once
     */
    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_SCAN_ENABLED] = settings.autoScanEnabled
            preferences[PreferencesKeys.SCAN_INTERVAL_MINUTES] = settings.scanIntervalMinutes
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = settings.notificationsEnabled
            preferences[PreferencesKeys.NOTIFICATION_SOUND_ENABLED] = settings.notificationSoundEnabled
            preferences[PreferencesKeys.NOTIFICATION_VIBRATION_ENABLED] = settings.notificationVibrationEnabled
            preferences[PreferencesKeys.DATA_RETENTION_DAYS] = settings.dataRetentionDays
            preferences[PreferencesKeys.THREAT_ALERT_ENABLED] = settings.threatAlertEnabled
            preferences[PreferencesKeys.CRITICAL_THREAT_NOTIFICATIONS] = settings.criticalThreatNotifications
            preferences[PreferencesKeys.THEME_MODE] = settings.themeMode
            preferences[PreferencesKeys.LANGUAGE] = settings.language
            preferences[PreferencesKeys.FIRST_LAUNCH] = settings.firstLaunch
            preferences[PreferencesKeys.LAST_SCAN_TIMESTAMP] = settings.lastScanTimestamp
            preferences[PreferencesKeys.TOTAL_SCANS_COUNT] = settings.totalScansCount
            preferences[PreferencesKeys.ANALYTICS_ENABLED] = settings.analyticsEnabled
            preferences[PreferencesKeys.CRASH_REPORTING_ENABLED] = settings.crashReportingEnabled
        }
    }
    
    /**
     * Clear all settings (reset to defaults)
     */
    suspend fun clearAllSettings() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

/**
 * Data class representing all app settings
 */
data class AppSettings(
    val autoScanEnabled: Boolean = true,
    val scanIntervalMinutes: Int = 15,
    val notificationsEnabled: Boolean = true,
    val notificationSoundEnabled: Boolean = true,
    val notificationVibrationEnabled: Boolean = true,
    val dataRetentionDays: Int = 30,
    val threatAlertEnabled: Boolean = true,
    val criticalThreatNotifications: Boolean = true,
    val themeMode: String = "system",
    val language: String = "ru",
    val firstLaunch: Boolean = true,
    val lastScanTimestamp: Long = 0L,
    val totalScansCount: Int = 0,
    val analyticsEnabled: Boolean = false,
    val crashReportingEnabled: Boolean = false
)
