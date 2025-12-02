package com.wifiguard.feature.settings.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wifiguard.core.data.local.DataTransferManager
import com.wifiguard.core.domain.repository.WifiRepository
import com.wifiguard.feature.settings.domain.repository.SettingsRepository
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
    private val settingsRepository: SettingsRepository,
    private val dataTransferManager: DataTransferManager,
    private val wifiRepository: WifiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                settingsRepository.getAutoScanEnabled().collect { autoScan ->
                    _uiState.value = _uiState.value.copy(autoScanEnabled = autoScan)
                }
            } catch (e: Exception) {
                // Используем значения по умолчанию при ошибке
            }
        }
        
        viewModelScope.launch {
            try {
                settingsRepository.getNotificationsEnabled().collect { notifications ->
                    _uiState.value = _uiState.value.copy(notificationsEnabled = notifications)
                }
            } catch (e: Exception) {
                // Используем значения по умолчанию при ошибке
            }
        }
        
        viewModelScope.launch {
            try {
                settingsRepository.getNotificationSoundEnabled().collect { sound ->
                    _uiState.value = _uiState.value.copy(notificationSoundEnabled = sound)
                }
            } catch (e: Exception) {
                // Используем значения по умолчанию при ошибке
            }
        }
        
        viewModelScope.launch {
            try {
                settingsRepository.getNotificationVibrationEnabled().collect { vibration ->
                    _uiState.value = _uiState.value.copy(notificationVibrationEnabled = vibration)
                }
            } catch (e: Exception) {
                // Используем значения по умолчанию при ошибке
            }
        }
    }

    fun setAutoScanEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoScanEnabled = enabled)
        viewModelScope.launch {
            try {
                settingsRepository.setAutoScanEnabled(enabled)
            } catch (e: Exception) {
                // Обработка ошибки
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
        viewModelScope.launch {
            try {
                settingsRepository.setNotificationsEnabled(enabled)
            } catch (e: Exception) {
                // Обработка ошибки
            }
        }
    }

    fun setNotificationSoundEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(notificationSoundEnabled = enabled)
        viewModelScope.launch {
            try {
                settingsRepository.setNotificationSoundEnabled(enabled)
            } catch (e: Exception) {
                // Обработка ошибки
            }
        }
    }

    fun setNotificationVibrationEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(notificationVibrationEnabled = enabled)
        viewModelScope.launch {
            try {
                settingsRepository.setNotificationVibrationEnabled(enabled)
            } catch (e: Exception) {
                // Обработка ошибки
            }
        }
    }

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            try {
                dataTransferManager.exportData(uri)
            } catch (e: Exception) {
                // Обработка ошибки
            }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            try {
                dataTransferManager.importData(uri)
            } catch (e: Exception) {
                // Обработка ошибки
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            try {
                wifiRepository.clearAllData()
            } catch (e: Exception) {
                // Обработка ошибки
            }
        }
    }
}

/**
 * Состояние UI экрана настроек
 */
data class SettingsUiState(
    val autoScanEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val notificationSoundEnabled: Boolean = true,
    val notificationVibrationEnabled: Boolean = true
)