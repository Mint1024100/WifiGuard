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

    override fun getBackgroundMonitoring(): Flow<Boolean> {
        return settingsDataSource.getBackgroundMonitoring()
    }

    override suspend fun setBackgroundMonitoring(enabled: Boolean) {
        settingsDataSource.setBackgroundMonitoring(enabled)
    }

    override fun getNotificationsEnabled(): Flow<Boolean> {
        return settingsDataSource.getNotificationsEnabled()
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        settingsDataSource.setNotificationsEnabled(enabled)
    }

    override fun getHighPriorityNotifications(): Flow<Boolean> {
        return settingsDataSource.getHighPriorityNotifications()
    }

    override suspend fun setHighPriorityNotifications(enabled: Boolean) {
        settingsDataSource.setHighPriorityNotifications(enabled)
    }

    override fun getScanInterval(): Flow<Int> {
        return settingsDataSource.getScanInterval()
    }

    override suspend fun setScanInterval(intervalMinutes: Int) {
        settingsDataSource.setScanInterval(intervalMinutes)
    }

    override fun getThreatSensitivity(): Flow<Int> {
        return settingsDataSource.getThreatSensitivity()
    }

    override suspend fun setThreatSensitivity(sensitivity: Int) {
        settingsDataSource.setThreatSensitivity(sensitivity)
    }
}
