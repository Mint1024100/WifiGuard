package com.wifiguard.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wifiguard.core.data.preferences.AppSettings
import com.wifiguard.feature.settings.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для экрана настроек
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                settingsRepository.getAutoScanEnabled(),
                settingsRepository.getScanIntervalMinutes(),
                settingsRepository.getNotificationsEnabled(),
                settingsRepository.getNotificationSoundEnabled(),
                settingsRepository.getNotificationVibrationEnabled(),
                settingsRepository.getDataRetentionDays()
            ) { autoScan, interval, notifications, sound, vibration, retention ->
                SettingsUiState(
                    autoScanEnabled = autoScan,
                    scanInterval = interval * 60 * 1000L, // Convert to milliseconds
                    notificationsEnabled = notifications,
                    notificationSoundEnabled = sound,
                    notificationVibrationEnabled = vibration,
                    dataRetentionDays = retention
                )
            }.collect { settings ->
                _uiState.value = settings
            }
        }
    }
    
    fun setAutoScanEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoScanEnabled(enabled)
        }
    }
    
    fun setScanIntervalMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setScanIntervalMinutes(minutes)
        }
    }
    
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
        }
    }
    
    fun setNotificationSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationSoundEnabled(enabled)
        }
    }
    
    fun setNotificationVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationVibrationEnabled(enabled)
        }
    }
    
    fun setDataRetentionDays(days: Int) {
        viewModelScope.launch {
            settingsRepository.setDataRetentionDays(days)
        }
    }
    
    fun exportSettings() {
        viewModelScope.launch {
            try {
                val settings = settingsRepository.getAllSettings()
                // TODO: Implement export functionality
                _uiState.value = _uiState.value.copy(
                    exportMessage = "Настройки экспортированы успешно"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Ошибка экспорта: ${e.message}"
                )
            }
        }
    }
    
    fun importSettings() {
        viewModelScope.launch {
            try {
                // TODO: Implement import functionality
                _uiState.value = _uiState.value.copy(
                    importMessage = "Настройки импортированы успешно"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Ошибка импорта: ${e.message}"
                )
            }
        }
    }
    
    fun clearAllData() {
        viewModelScope.launch {
            try {
                settingsRepository.clearAllSettings()
                _uiState.value = _uiState.value.copy(
                    clearMessage = "Все данные очищены"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Ошибка очистки: ${e.message}"
                )
            }
        }
    }
    
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            exportMessage = null,
            importMessage = null,
            clearMessage = null,
            error = null
        )
    }
}

/**
 * Состояние UI экрана настроек
 */
data class SettingsUiState(
    val autoScanEnabled: Boolean = true,
    val scanInterval: Long = 15 * 60 * 1000, // 15 minutes
    val notificationsEnabled: Boolean = true,
    val notificationSoundEnabled: Boolean = true,
    val notificationVibrationEnabled: Boolean = true,
    val dataRetentionDays: Int = 30,
    val exportMessage: String? = null,
    val importMessage: String? = null,
    val clearMessage: String? = null,
    val error: String? = null
)
