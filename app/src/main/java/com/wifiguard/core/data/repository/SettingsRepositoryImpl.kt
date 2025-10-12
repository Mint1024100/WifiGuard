package com.wifiguard.core.data.repository

import com.wifiguard.core.data.preferences.AppSettings
import com.wifiguard.core.data.preferences.PreferencesDataSource
import com.wifiguard.feature.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SettingsRepository using DataStore
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource
) : SettingsRepository {
    
    override fun getAutoScanEnabled(): Flow<Boolean> {
        return preferencesDataSource.getAutoScanEnabled()
    }
    
    override suspend fun setAutoScanEnabled(enabled: Boolean) {
        preferencesDataSource.setAutoScanEnabled(enabled)
    }
    
    override fun getScanIntervalMinutes(): Flow<Int> {
        return preferencesDataSource.getScanIntervalMinutes()
    }
    
    override suspend fun setScanIntervalMinutes(minutes: Int) {
        preferencesDataSource.setScanIntervalMinutes(minutes)
    }
    
    override fun getNotificationsEnabled(): Flow<Boolean> {
        return preferencesDataSource.getNotificationsEnabled()
    }
    
    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        preferencesDataSource.setNotificationsEnabled(enabled)
    }
    
    override fun getNotificationSoundEnabled(): Flow<Boolean> {
        return preferencesDataSource.getNotificationSoundEnabled()
    }
    
    override suspend fun setNotificationSoundEnabled(enabled: Boolean) {
        preferencesDataSource.setNotificationSoundEnabled(enabled)
    }
    
    override fun getNotificationVibrationEnabled(): Flow<Boolean> {
        return preferencesDataSource.getNotificationVibrationEnabled()
    }
    
    override suspend fun setNotificationVibrationEnabled(enabled: Boolean) {
        preferencesDataSource.setNotificationVibrationEnabled(enabled)
    }
    
    override fun getDataRetentionDays(): Flow<Int> {
        return preferencesDataSource.getDataRetentionDays()
    }
    
    override suspend fun setDataRetentionDays(days: Int) {
        preferencesDataSource.setDataRetentionDays(days)
    }
    
    override fun getAllSettings(): Flow<AppSettings> {
        return preferencesDataSource.getAllSettings()
    }
    
    override suspend fun updateSettings(settings: AppSettings) {
        preferencesDataSource.updateSettings(settings)
    }
    
    override suspend fun clearAllSettings() {
        preferencesDataSource.clearAllSettings()
    }
}
