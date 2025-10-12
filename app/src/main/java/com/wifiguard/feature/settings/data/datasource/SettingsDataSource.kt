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
    
    fun getHighPriorityNotifications(): Flow<Boolean>
    suspend fun setHighPriorityNotifications(enabled: Boolean)
    
    fun getScanInterval(): Flow<Int>
    suspend fun setScanInterval(intervalMinutes: Int)
    
    fun getThreatSensitivity(): Flow<Int>
    suspend fun setThreatSensitivity(sensitivity: Int)
}
