package com.wifiguard.feature.settings.data.datasource

import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс источника данных для настроек
 */
interface SettingsDataSource {
    fun getAutoScanEnabled(): Flow<Boolean>
    suspend fun setAutoScanEnabled(enabled: Boolean)
    
    fun getBackgroundMonitoring(): Flow<Boolean>
    suspend fun setBackgroundMonitoring(enabled: Boolean)
    
    fun getNotificationsEnabled(): Flow<Boolean>
    suspend fun setNotificationsEnabled(enabled: Boolean)
    
    fun getNotificationSoundEnabled(): Flow<Boolean>
    suspend fun setNotificationSoundEnabled(enabled: Boolean)
    
    fun getNotificationVibrationEnabled(): Flow<Boolean>
    suspend fun setNotificationVibrationEnabled(enabled: Boolean)
    
    fun getHighPriorityNotifications(): Flow<Boolean>
    suspend fun setHighPriorityNotifications(enabled: Boolean)
    
    fun getScanInterval(): Flow<Int>
    suspend fun setScanInterval(intervalMinutes: Int)
    
    fun getThreatSensitivity(): Flow<Int>
    suspend fun setThreatSensitivity(sensitivity: Int)

    /**
     * Разрешение пользователя: авто-отключение Wi‑Fi при критической угрозе.
     *
     * ВАЖНО: по умолчанию выключено (false).
     */
    fun getAutoDisableWifiOnCritical(): Flow<Boolean>
    suspend fun setAutoDisableWifiOnCritical(enabled: Boolean)
    
    fun getDataRetentionDays(): Flow<Int>
    suspend fun setDataRetentionDays(days: Int)

    fun getThemeMode(): Flow<String>
    suspend fun setThemeMode(mode: String)
    
    fun getAllSettings(): Flow<com.wifiguard.core.data.preferences.AppSettings>
    suspend fun updateSettings(settings: com.wifiguard.core.data.preferences.AppSettings)
    suspend fun clearAllSettings()
}
