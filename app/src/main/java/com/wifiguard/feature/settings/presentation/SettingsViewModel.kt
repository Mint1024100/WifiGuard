package com.wifiguard.feature.settings.presentation

import android.content.Context
// РЕЗЕРВНАЯ КОПИЯ: Удаленный импорт Uri (использовался только для exportData/importData)
// import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import androidx.work.await
// РЕЗЕРВНАЯ КОПИЯ: Удаленный импорт DataTransferManager
// import com.wifiguard.core.data.local.DataTransferManager
import com.wifiguard.core.domain.repository.WifiRepository
import com.wifiguard.feature.settings.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import com.wifiguard.core.common.Constants
import javax.inject.Inject

/**
 * ViewModel для экрана настроек
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    // РЕЗЕРВНАЯ КОПИЯ: Удаленная зависимость dataTransferManager
    // private val dataTransferManager: DataTransferManager,
    private val wifiRepository: WifiRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "SettingsViewModel"
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _scanIntervalDialogVisible = MutableStateFlow(false)
    val scanIntervalDialogVisible: StateFlow<Boolean> = _scanIntervalDialogVisible.asStateFlow()

    private val _clearDataDialogVisible = MutableStateFlow(false)
    val clearDataDialogVisible: StateFlow<Boolean> = _clearDataDialogVisible.asStateFlow()

    private val _clearDataResult = MutableStateFlow<ClearDataResult?>(null)
    val clearDataResult: StateFlow<ClearDataResult?> = _clearDataResult.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                settingsRepository.getAutoScanEnabled().collect { autoScan ->
                    _uiState.value = _uiState.value.copy(autoScanEnabled = autoScan)
                }
            } catch (e: CancellationException) {
                // Пробрасываем CancellationException - это нормальное поведение при очистке ViewModel
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке настройки autoScanEnabled: ${e.message}", e)
                // Используем значения по умолчанию при ошибке
            }
        }

        viewModelScope.launch {
            try {
                settingsRepository.getNotificationsEnabled().collect { notifications ->
                    _uiState.value = _uiState.value.copy(notificationsEnabled = notifications)
                }
            } catch (e: CancellationException) {
                // Пробрасываем CancellationException - это нормальное поведение при очистке ViewModel
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке настройки notificationsEnabled: ${e.message}", e)
                // Используем значения по умолчанию при ошибке
            }
        }

        viewModelScope.launch {
            try {
                settingsRepository.getNotificationSoundEnabled().collect { sound ->
                    _uiState.value = _uiState.value.copy(notificationSoundEnabled = sound)
                }
            } catch (e: CancellationException) {
                // Пробрасываем CancellationException - это нормальное поведение при очистке ViewModel
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке настройки notificationSoundEnabled: ${e.message}", e)
                // Используем значения по умолчанию при ошибке
            }
        }

        viewModelScope.launch {
            try {
                settingsRepository.getNotificationVibrationEnabled().collect { vibration ->
                    _uiState.value = _uiState.value.copy(notificationVibrationEnabled = vibration)
                }
            } catch (e: CancellationException) {
                // Пробрасываем CancellationException - это нормальное поведение при очистке ViewModel
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке настройки notificationVibrationEnabled: ${e.message}", e)
                // Используем значения по умолчанию при ошибке
            }
        }

        viewModelScope.launch {
            try {
                settingsRepository.getScanIntervalMinutes().collect { interval ->
                    _uiState.value = _uiState.value.copy(scanIntervalMinutes = interval)
                }
            } catch (e: CancellationException) {
                // Пробрасываем CancellationException - это нормальное поведение при очистке ViewModel
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке настройки scanIntervalMinutes: ${e.message}", e)
                // Используем значения по умолчанию при ошибке
            }
        }
        
        viewModelScope.launch {
            try {
                settingsRepository.getThemeMode().collect { mode ->
                    _uiState.value = _uiState.value.copy(themeMode = mode)
                }
            } catch (e: CancellationException) {
                // Пробрасываем CancellationException - это нормальное поведение при очистке ViewModel
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке настройки themeMode: ${e.message}", e)
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
            workManager.cancelUniqueWork(Constants.WORK_NAME_WIFI_MONITORING).await()

            // Create new periodic work with updated interval
            val newPeriodicWork = com.wifiguard.core.background.WifiMonitoringWorker.createPeriodicWorkWithInterval(
                _uiState.value.scanIntervalMinutes
            )

            // Schedule new work
            workManager.enqueueUniquePeriodicWork(
                Constants.WORK_NAME_WIFI_MONITORING,
                androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
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

    fun setThemeMode(mode: String) {
        _uiState.value = _uiState.value.copy(themeMode = mode)
        viewModelScope.launch {
            try {
                settingsRepository.setThemeMode(mode)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    // РЕЗЕРВНАЯ КОПИЯ: Удаленный метод exportData()
    // fun exportData(uri: Uri) {
    //     viewModelScope.launch {
    //         try {
    //             dataTransferManager.exportData(uri)
    //         } catch (e: Exception) {
    //             // Обработка ошибки
    //         }
    //     }
    // }

    // РЕЗЕРВНАЯ КОПИЯ: Удаленный метод importData()
    // fun importData(uri: Uri) {
    //     viewModelScope.launch {
    //         try {
    //             dataTransferManager.importData(uri)
    //         } catch (e: Exception) {
    //             // Обработка ошибки
    //         }
    //     }
    // }

    /**
     * Показать диалог подтверждения удаления данных
     */
    fun showClearDataDialog() {
        _clearDataDialogVisible.value = true
    }

    /**
     * Скрыть диалог подтверждения удаления данных
     */
    fun hideClearDataDialog() {
        _clearDataDialogVisible.value = false
    }

    /**
     * Сбросить результат операции очистки данных
     */
    fun resetClearDataResult() {
        _clearDataResult.value = null
    }

    /**
     * Очистить все данные из базы данных
     * Выполняется в корутине с Dispatchers.IO для операций БД
     */
    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("SettingsViewModel", "Начало очистки всех данных")
                
                // Проверяем целостность БД перед удалением
                val isValid = wifiRepository.validateDatabaseIntegrity()
                if (!isValid) {
                    Log.w("SettingsViewModel", "Обнаружены проблемы с целостностью БД перед удалением")
                }
                
                // Удаляем данные из всех таблиц
                wifiRepository.clearAllData()
                
                Log.d("SettingsViewModel", "Все данные успешно удалены")
                
                // Показываем успешное сообщение на главном потоке
                withContext(Dispatchers.Main) {
                    _clearDataResult.value = ClearDataResult.Success
                    _clearDataDialogVisible.value = false
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Ошибка при удалении данных: ${e.message}", e)
                
                // Показываем сообщение об ошибке на главном потоке
                withContext(Dispatchers.Main) {
                    _clearDataResult.value = ClearDataResult.Error(e.message ?: "Неизвестная ошибка")
                    _clearDataDialogVisible.value = false
                }
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
    val scanIntervalMinutes: Int = 15,
    val themeMode: String = "system"
)

/**
 * Результат операции очистки данных
 */
sealed class ClearDataResult {
    object Success : ClearDataResult()
    data class Error(val message: String) : ClearDataResult()
}