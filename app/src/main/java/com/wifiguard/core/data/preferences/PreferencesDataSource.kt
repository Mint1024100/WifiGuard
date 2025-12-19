package com.wifiguard.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ИСПРАВЛЕНО: DataStore wrapper с правильной обработкой ошибок
 * 
 * ИСПРАВЛЕНИЯ:
 * ✅ Использует инжектированный DataStore вместо создания нового экземпляра
 * ✅ Правильная обработка ошибок с graceful fallback
 * ✅ Cancellation safety - не перехватывает CancellationException
 * ✅ Thread-safe операции чтения/записи
 */
@Singleton
open class PreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    
    /**
     * ИСПРАВЛЕНО: Get auto-scan enabled setting с правильной обработкой ошибок
     */
    fun getAutoScanEnabled(): Flow<Boolean> {
        return dataStore.data
            .catch { exception ->
                // ИСПРАВЛЕНО: Не перехватываем CancellationException
                if (exception is CancellationException) {
                    throw exception
                }
                // ИСПРАВЛЕНО: Graceful fallback для IOException
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    // Логируем другие ошибки и возвращаем дефолтное значение
                    android.util.Log.e("PreferencesDataSource", "Error reading auto-scan setting", exception)
                    emit(emptyPreferences())
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.AUTO_SCAN_ENABLED] ?: true
            }
    }
    
    /**
     * ИСПРАВЛЕНО: Set auto-scan enabled setting с обработкой ошибок
     */
    suspend fun setAutoScanEnabled(enabled: Boolean) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.AUTO_SCAN_ENABLED] = enabled
            }
        } catch (e: CancellationException) {
            // ИСПРАВЛЕНО: Пробрасываем CancellationException дальше
            throw e
        } catch (e: Exception) {
            // ИСПРАВЛЕНО: Логируем ошибки записи
            android.util.Log.e("PreferencesDataSource", "Error writing auto-scan setting", e)
            throw e
        }
    }
    
    /**
     * ИСПРАВЛЕНО: Get scan interval с правильной обработкой ошибок
     */
    fun getScanIntervalMinutes(): Flow<Int> {
        return dataStore.data
            .catch { exception ->
                if (exception is CancellationException) throw exception
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    android.util.Log.e("PreferencesDataSource", "Error reading scan interval", exception)
                    emit(emptyPreferences())
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.SCAN_INTERVAL_MINUTES] ?: 15
            }
    }
    
    /**
     * ИСПРАВЛЕНО: Set scan interval с обработкой ошибок
     */
    suspend fun setScanIntervalMinutes(minutes: Int) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.SCAN_INTERVAL_MINUTES] = minutes
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("PreferencesDataSource", "Error writing scan interval", e)
            throw e
        }
    }
    
    /**
     * ИСПРАВЛЕНО: Get notifications enabled с правильной обработкой ошибок
     */
    fun getNotificationsEnabled(): Flow<Boolean> {
        return dataStore.data
            .catch { exception ->
                if (exception is CancellationException) throw exception
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    android.util.Log.e("PreferencesDataSource", "Error reading notifications setting", exception)
                    emit(emptyPreferences())
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true
            }
    }
    
    /**
     * ИСПРАВЛЕНО: Set notifications enabled с обработкой ошибок
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("PreferencesDataSource", "Error writing notifications setting", e)
            throw e
        }
    }
    
    /**
     * ИСПРАВЛЕНО: Get notification sound enabled с правильной обработкой ошибок
     */
    fun getNotificationSoundEnabled(): Flow<Boolean> {
        return dataStore.data
            .catch { exception ->
                if (exception is CancellationException) throw exception
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    android.util.Log.e("PreferencesDataSource", "Error reading notification sound setting", exception)
                    emit(emptyPreferences())
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.NOTIFICATION_SOUND_ENABLED] ?: true
            }
    }
    
    /**
     * ИСПРАВЛЕНО: Set notification sound enabled с обработкой ошибок
     */
    suspend fun setNotificationSoundEnabled(enabled: Boolean) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.NOTIFICATION_SOUND_ENABLED] = enabled
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("PreferencesDataSource", "Error writing notification sound setting", e)
            throw e
        }
    }
    
    /**
     * ИСПРАВЛЕНО: Get notification vibration enabled с правильной обработкой ошибок
     */
    fun getNotificationVibrationEnabled(): Flow<Boolean> {
        return dataStore.data
            .catch { exception ->
                if (exception is CancellationException) throw exception
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    android.util.Log.e("PreferencesDataSource", "Error reading notification vibration setting", exception)
                    emit(emptyPreferences())
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.NOTIFICATION_VIBRATION_ENABLED] ?: true
            }
    }

    /**
     * Получить флаг авто-отключения Wi‑Fi при критической угрозе.
     *
     * ВАЖНО: по умолчанию false (требуется явное согласие пользователя).
     */
    fun getAutoDisableWifiOnCritical(): Flow<Boolean> {
        return dataStore.data
            .catch { exception ->
                if (exception is CancellationException) throw exception
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    android.util.Log.e(
                        "PreferencesDataSource",
                        "Error reading auto-disable wifi on critical",
                        exception
                    )
                    emit(emptyPreferences())
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.AUTO_DISABLE_WIFI_ON_CRITICAL] ?: false
            }
    }

    /**
     * Установить флаг авто-отключения Wi‑Fi при критической угрозе.
     */
    suspend fun setAutoDisableWifiOnCritical(enabled: Boolean) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.AUTO_DISABLE_WIFI_ON_CRITICAL] = enabled
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("PreferencesDataSource", "Error writing auto-disable wifi on critical", e)
            throw e
        }
    }
    
    /**
     * ИСПРАВЛЕНО: Set notification vibration enabled с обработкой ошибок
     */
    suspend fun setNotificationVibrationEnabled(enabled: Boolean) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.NOTIFICATION_VIBRATION_ENABLED] = enabled
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("PreferencesDataSource", "Error writing notification vibration setting", e)
            throw e
        }
    }
    
    /**
     * ИСПРАВЛЕНО: Get data retention days с правильной обработкой ошибок
     */
    fun getDataRetentionDays(): Flow<Int> {
        return dataStore.data
            .catch { exception ->
                if (exception is CancellationException) throw exception
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    android.util.Log.e("PreferencesDataSource", "Error reading data retention days", exception)
                    emit(emptyPreferences())
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.DATA_RETENTION_DAYS] ?: 30
            }
    }
    
    /**
     * ИСПРАВЛЕНО: Set data retention days с обработкой ошибок
     */
    suspend fun setDataRetentionDays(days: Int) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.DATA_RETENTION_DAYS] = days
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("PreferencesDataSource", "Error writing data retention days", e)
            throw e
        }
    }
    
    /**
     * ИСПРАВЛЕНО: Get theme mode с правильной обработкой ошибок
     */
    fun getThemeMode(): Flow<String> {
        return dataStore.data
            .catch { exception ->
                if (exception is CancellationException) throw exception
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    android.util.Log.e("PreferencesDataSource", "Error reading theme mode", exception)
                    emit(emptyPreferences())
                }
            }
            .map { preferences ->
                preferences[PreferencesKeys.THEME_MODE] ?: "system"
            }
    }
    
    /**
     * ИСПРАВЛЕНО: Set theme mode с обработкой ошибок
     */
    suspend fun setThemeMode(mode: String) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.THEME_MODE] = mode
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("PreferencesDataSource", "Error writing theme mode", e)
            throw e
        }
    }

    /**
     * ИСПРАВЛЕНО: Get all settings с правильной обработкой ошибок
     */
    fun getAllSettings(): Flow<AppSettings> {
        return dataStore.data
            .catch { exception ->
                if (exception is CancellationException) throw exception
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    android.util.Log.e("PreferencesDataSource", "Error reading all settings", exception)
                    emit(emptyPreferences())
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
                    autoDisableWifiOnCritical = preferences[PreferencesKeys.AUTO_DISABLE_WIFI_ON_CRITICAL] ?: false,
                    themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "system",
                    language = preferences[PreferencesKeys.LANGUAGE] ?: "ru",
                    firstLaunch = preferences[PreferencesKeys.FIRST_LAUNCH] ?: true,
                    lastScanTimestamp = preferences[PreferencesKeys.LAST_SCAN_TIMESTAMP] ?: 0L,
                    totalScansCount = preferences[PreferencesKeys.TOTAL_SCANS_COUNT] ?: 0,
                    crashReportingEnabled = preferences[PreferencesKeys.CRASH_REPORTING_ENABLED] ?: false
                )
            }
    }
    
    /**
     * ИСПРАВЛЕНО: Update multiple settings с обработкой ошибок
     */
    suspend fun updateSettings(settings: AppSettings) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.AUTO_SCAN_ENABLED] = settings.autoScanEnabled
                preferences[PreferencesKeys.SCAN_INTERVAL_MINUTES] = settings.scanIntervalMinutes
                preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = settings.notificationsEnabled
                preferences[PreferencesKeys.NOTIFICATION_SOUND_ENABLED] = settings.notificationSoundEnabled
                preferences[PreferencesKeys.NOTIFICATION_VIBRATION_ENABLED] = settings.notificationVibrationEnabled
                preferences[PreferencesKeys.DATA_RETENTION_DAYS] = settings.dataRetentionDays
                preferences[PreferencesKeys.THREAT_ALERT_ENABLED] = settings.threatAlertEnabled
                preferences[PreferencesKeys.CRITICAL_THREAT_NOTIFICATIONS] = settings.criticalThreatNotifications
                preferences[PreferencesKeys.AUTO_DISABLE_WIFI_ON_CRITICAL] = settings.autoDisableWifiOnCritical
                preferences[PreferencesKeys.THEME_MODE] = settings.themeMode
                preferences[PreferencesKeys.LANGUAGE] = settings.language
                preferences[PreferencesKeys.FIRST_LAUNCH] = settings.firstLaunch
                preferences[PreferencesKeys.LAST_SCAN_TIMESTAMP] = settings.lastScanTimestamp
                preferences[PreferencesKeys.TOTAL_SCANS_COUNT] = settings.totalScansCount
                preferences[PreferencesKeys.CRASH_REPORTING_ENABLED] = settings.crashReportingEnabled
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("PreferencesDataSource", "Error updating settings", e)
            throw e
        }
    }
    
    /**
     * ИСПРАВЛЕНО: Clear all settings с обработкой ошибок
     */
    suspend fun clearAllSettings() {
        try {
            dataStore.edit { preferences ->
                preferences.clear()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("PreferencesDataSource", "Error clearing settings", e)
            throw e
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
    val autoDisableWifiOnCritical: Boolean = false,
    val themeMode: String = "system",
    val language: String = "ru",
    val firstLaunch: Boolean = true,
    val lastScanTimestamp: Long = 0L,
    val totalScansCount: Int = 0,
    val crashReportingEnabled: Boolean = false
)
