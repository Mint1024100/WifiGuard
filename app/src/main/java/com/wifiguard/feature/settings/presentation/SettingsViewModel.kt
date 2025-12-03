package com.wifiguard.feature.settings.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import androidx.work.await
import com.wifiguard.core.data.local.DataTransferManager
import com.wifiguard.core.domain.repository.WifiRepository
import com.wifiguard.feature.settings.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val wifiRepository: WifiRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _scanIntervalDialogVisible = MutableStateFlow(false)
    val scanIntervalDialogVisible: StateFlow<Boolean> = _scanIntervalDialogVisible.asStateFlow()

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

        viewModelScope.launch {
            try {
                settingsRepository.getScanIntervalMinutes().collect { interval ->
                    _uiState.value = _uiState.value.copy(scanIntervalMinutes = interval)
                }
            } catch (e: Exception) {
                // Используем значения по умолчанию при ошибке
            }
        }
    }

    fun showScanIntervalDialog() {
        _scanIntervalDialogVisible.value = true
    }

    fun hideScanIntervalDialog() {
        _scanIntervalDialogVisible.value = false
    }

    fun setScanInterval(minutes: Int) {
        _uiState.value = _uiState.value.copy(scanIntervalMinutes = minutes)
        viewModelScope.launch {
            try {
                settingsRepository.setScanIntervalMinutes(minutes)
                // Trigger rescheduling of the background work
                rescheduleBackgroundWork()
            } catch (e: Exception) {
                // Обработка ошибки
            }
        }
    }

    private suspend fun rescheduleBackgroundWork() {
        // We'll call into a use case or directly into a service that handles work scheduling
        // This will be implemented to cancel the existing work and schedule new work with the new interval
        // For now, we'll schedule the work using WorkManager
        try {
            val workManager = WorkManager.getInstance(context)

            // Cancel existing periodic work
            workManager.cancelUniqueWork("wifi_monitoring_periodic").await()

            // Create new periodic work with updated interval
            val newPeriodicWork = com.wifiguard.core.background.WifiMonitoringWorker.createPeriodicWorkWithInterval(
                _uiState.value.scanIntervalMinutes
            )

            // Schedule new work
            workManager.enqueueUniquePeriodicWork(
                "wifi_monitoring_periodic",
                androidx.work.ExistingPeriodicWorkPolicy.REPLACE,
                newPeriodicWork
            )
        } catch (e: Exception) {
            // Handle error
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
    val notificationVibrationEnabled: Boolean = true,
    val scanIntervalMinutes: Int = 15
)