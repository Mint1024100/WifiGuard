package com.wifiguard.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для экрана настроек
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    // TODO: Inject DataStore or other settings repository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    fun setAutoScanEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(autoScanEnabled = enabled)
            // TODO: Save to DataStore
        }
    }
    
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
            // TODO: Save to DataStore
        }
    }
    
    fun setNotificationSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(notificationSoundEnabled = enabled)
            // TODO: Save to DataStore
        }
    }
    
    fun setNotificationVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(notificationVibrationEnabled = enabled)
            // TODO: Save to DataStore
        }
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
    val dataRetentionDays: Int = 30
)
