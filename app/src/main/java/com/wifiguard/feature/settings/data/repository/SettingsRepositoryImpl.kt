package com.wifiguard.feature.settings.data.repository

import com.wifiguard.feature.settings.data.datasource.SettingsDataSource
import com.wifiguard.feature.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация репозитория для настроек
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataSource: SettingsDataSource
) : SettingsRepository {

    override fun getAutoScanEnabled(): Flow<Boolean> {
        return settingsDataSource.getAutoScanEnabled()
    }

    override suspend fun setAutoScanEnabled(enabled: Boolean) {
        settingsDataSource.setAutoScanEnabled(enabled)
    }

    override fun getNotificationsEnabled(): Flow<Boolean> {
        return settingsDataSource.getNotificationsEnabled()
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        settingsDataSource.setNotificationsEnabled(enabled)
    }

    override fun getNotificationSoundEnabled(): Flow<Boolean> {
        return settingsDataSource.getNotificationSoundEnabled()
    }

    override suspend fun setNotificationSoundEnabled(enabled: Boolean) {
        settingsDataSource.setNotificationSoundEnabled(enabled)
    }

    override fun getNotificationVibrationEnabled(): Flow<Boolean> {
        return settingsDataSource.getNotificationVibrationEnabled()
    }

    override suspend fun setNotificationVibrationEnabled(enabled: Boolean) {
        settingsDataSource.setNotificationVibrationEnabled(enabled)
    }

    override fun getScanIntervalMinutes(): Flow<Int> {
        return settingsDataSource.getScanInterval()
    }

    override suspend fun setScanIntervalMinutes(minutes: Int) {
        settingsDataSource.setScanInterval(minutes)
    }

    override fun getDataRetentionDays(): Flow<Int> {
        return settingsDataSource.getDataRetentionDays()
    }

    override suspend fun setDataRetentionDays(days: Int) {
        settingsDataSource.setDataRetentionDays(days)
    }

    override fun getThemeMode(): Flow<String> {
        return settingsDataSource.getThemeMode()
    }

    override suspend fun setThemeMode(mode: String) {
        settingsDataSource.setThemeMode(mode)
    }

    override fun getAllSettings(): Flow<com.wifiguard.core.data.preferences.AppSettings> {
        return settingsDataSource.getAllSettings()
    }

    override suspend fun updateSettings(settings: com.wifiguard.core.data.preferences.AppSettings) {
        settingsDataSource.updateSettings(settings)
    }

    override suspend fun clearAllSettings() {
        settingsDataSource.clearAllSettings()
    }
    
    override suspend fun resetToDefaults() {
        // Сброс к дефолтным значениям
        settingsDataSource.setAutoScanEnabled(false)
        settingsDataSource.setScanInterval(15) // 15 минут
        settingsDataSource.setNotificationsEnabled(true)
        settingsDataSource.setNotificationSoundEnabled(true)
        settingsDataSource.setNotificationVibrationEnabled(true)
        settingsDataSource.setDataRetentionDays(30) // 30 дней
        settingsDataSource.setThemeMode("system") // Системная тема
    }
}
