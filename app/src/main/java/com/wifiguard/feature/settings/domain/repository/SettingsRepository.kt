package com.wifiguard.feature.settings.domain.repository

import com.wifiguard.core.data.preferences.AppSettings
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing app settings
 */
interface SettingsRepository {
    
    fun getAutoScanEnabled(): Flow<Boolean>
    suspend fun setAutoScanEnabled(enabled: Boolean)
    
    fun getScanIntervalMinutes(): Flow<Int>
    suspend fun setScanIntervalMinutes(minutes: Int)
    
    fun getNotificationsEnabled(): Flow<Boolean>
    suspend fun setNotificationsEnabled(enabled: Boolean)
    
    fun getNotificationSoundEnabled(): Flow<Boolean>
    suspend fun setNotificationSoundEnabled(enabled: Boolean)
    
    fun getNotificationVibrationEnabled(): Flow<Boolean>
    suspend fun setNotificationVibrationEnabled(enabled: Boolean)
    
    fun getDataRetentionDays(): Flow<Int>
    suspend fun setDataRetentionDays(days: Int)

    /**
     * Разрешение пользователя: авто-отключение Wi‑Fi при критической угрозе.
     *
     * ВАЖНО: по умолчанию выключено (false).
     */
    fun getAutoDisableWifiOnCritical(): Flow<Boolean>
    suspend fun setAutoDisableWifiOnCritical(enabled: Boolean)
    
    fun getThemeMode(): Flow<String>
    suspend fun setThemeMode(mode: String)
    
    fun getAllSettings(): Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings)
    suspend fun clearAllSettings()
    
    /**
     * Сбросить все настройки к значениям по умолчанию
     */
    suspend fun resetToDefaults()
}