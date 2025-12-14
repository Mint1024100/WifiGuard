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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import com.wifiguard.core.common.Constants
import com.wifiguard.core.background.WorkManagerSafe
import com.wifiguard.core.common.loge
import com.wifiguard.core.common.logw
import com.wifiguard.feature.settings.domain.model.ThemeMode
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

    private val _dataRetentionDialogVisible = MutableStateFlow(false)
    val dataRetentionDialogVisible: StateFlow<Boolean> = _dataRetentionDialogVisible.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadSettings()
    }

    /**
     * ИСПРАВЛЕНО: Загрузка настроек через вложенные combine() для устранения race conditions
     * Все Flow объединены в один поток, что гарантирует атомарное обновление состояния
     * Используются вложенные combine(), так как combine() поддерживает максимум 5 параметров
     */
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                // ИСПРАВЛЕНО: Используем вложенные combine() для 7 Flow
                // Сначала объединяем первые 5 Flow в промежуточный Flow
                val firstFiveFlow = combine(
                    settingsRepository.getAutoScanEnabled(),
                    settingsRepository.getNotificationsEnabled(),
                    settingsRepository.getNotificationSoundEnabled(),
                    settingsRepository.getNotificationVibrationEnabled(),
                    settingsRepository.getScanIntervalMinutes()
                ) { autoScan, notifications, sound, vibration, interval ->
                    // Возвращаем data class для типобезопасности
                    SettingsPartialState(
                        autoScanEnabled = autoScan,
                        notificationsEnabled = notifications,
                        notificationSoundEnabled = sound,
                        notificationVibrationEnabled = vibration,
                        scanIntervalMinutes = interval
                    )
                }
                
                // Затем объединяем результат с оставшимися 2 Flow
                combine(
                    firstFiveFlow,
                    settingsRepository.getThemeMode(),
                    settingsRepository.getDataRetentionDays()
                ) { partialState, theme, retention ->
                    SettingsUiState(
                        autoScanEnabled = partialState.autoScanEnabled,
                        notificationsEnabled = partialState.notificationsEnabled,
                        notificationSoundEnabled = partialState.notificationSoundEnabled,
                        notificationVibrationEnabled = partialState.notificationVibrationEnabled,
                        scanIntervalMinutes = partialState.scanIntervalMinutes,
                        themeMode = theme,
                        dataRetentionDays = retention
                    )
                }
                    .catch { exception ->
                        if (exception is CancellationException) throw exception
                        loge("Ошибка при загрузке настроек: ${exception.message}", exception)
                        _errorMessage.value = "Не удалось загрузить настройки"
                        // Используем значения по умолчанию при ошибке
                        emit(SettingsUiState())
                    }
                    .collect { newState ->
                        _uiState.value = newState
                    }
            } catch (e: CancellationException) {
                // Пробрасываем CancellationException - это нормальное поведение при очистке ViewModel
                throw e
            } catch (e: Exception) {
                loge("Критическая ошибка при загрузке настроек: ${e.message}", e)
                _errorMessage.value = "Критическая ошибка при загрузке настроек"
                // Используем значения по умолчанию при критической ошибке
                _uiState.value = SettingsUiState()
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
        // Валидация: WorkManager требует минимум 15 минут для периодических задач
        val validMinutes = maxOf(Constants.MIN_SCAN_INTERVAL_MINUTES, 
            minOf(minutes, Constants.MAX_SCAN_INTERVAL_MINUTES))
        
        if (validMinutes != minutes) {
            logw("Интервал сканирования скорректирован: $minutes -> $validMinutes минут")
        }
        
        val oldInterval = _uiState.value.scanIntervalMinutes
        _uiState.value = _uiState.value.copy(scanIntervalMinutes = validMinutes)
        viewModelScope.launch {
            try {
                settingsRepository.setScanIntervalMinutes(validMinutes)
                // Trigger rescheduling of the background work
                rescheduleBackgroundWork()
            } catch (e: Exception) {
                loge("Ошибка при сохранении интервала сканирования: ${e.message}", e)
                _errorMessage.value = "Не удалось сохранить интервал сканирования"
                // Откатываем изменение при ошибке
                _uiState.value = _uiState.value.copy(scanIntervalMinutes = oldInterval)
            }
        }
    }

    /**
     * ИСПРАВЛЕНО: Безопасная перенастройка фоновой работы
     * Добавлена проверка на null для WorkManager (может быть null на некоторых устройствах)
     */
    private suspend fun rescheduleBackgroundWork() {
        try {
            val workManager = WorkManagerSafe.getInstanceOrNull(context) ?: return

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
            loge("Ошибка при перенастройке фоновой работы: ${e.message}", e)
            throw e
        }
    }

    fun setAutoScanEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoScanEnabled = enabled)
        viewModelScope.launch {
            try {
                settingsRepository.setAutoScanEnabled(enabled)
            } catch (e: Exception) {
                loge("Ошибка при сохранении настройки автоматического сканирования: ${e.message}", e)
                _errorMessage.value = "Не удалось сохранить настройку"
                // Откатываем изменение при ошибке
                _uiState.value = _uiState.value.copy(autoScanEnabled = !enabled)
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
        viewModelScope.launch {
            try {
                settingsRepository.setNotificationsEnabled(enabled)
            } catch (e: Exception) {
                loge("Ошибка при сохранении настройки уведомлений: ${e.message}", e)
                _errorMessage.value = "Не удалось сохранить настройку уведомлений"
                // Откатываем изменение при ошибке
                _uiState.value = _uiState.value.copy(notificationsEnabled = !enabled)
            }
        }
    }

    fun setNotificationSoundEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(notificationSoundEnabled = enabled)
        viewModelScope.launch {
            try {
                settingsRepository.setNotificationSoundEnabled(enabled)
            } catch (e: Exception) {
                loge("Ошибка при сохранении настройки звука уведомлений: ${e.message}", e)
                _errorMessage.value = "Не удалось сохранить настройку звука"
                // Откатываем изменение при ошибке
                _uiState.value = _uiState.value.copy(notificationSoundEnabled = !enabled)
            }
        }
    }

    fun setNotificationVibrationEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(notificationVibrationEnabled = enabled)
        viewModelScope.launch {
            try {
                settingsRepository.setNotificationVibrationEnabled(enabled)
            } catch (e: Exception) {
                loge("Ошибка при сохранении настройки вибрации: ${e.message}", e)
                _errorMessage.value = "Не удалось сохранить настройку вибрации"
                // Откатываем изменение при ошибке
                _uiState.value = _uiState.value.copy(notificationVibrationEnabled = !enabled)
            }
        }
    }

    fun setThemeMode(mode: String) {
        // Валидация: проверяем, что режим темы валиден
        if (!ThemeMode.isValid(mode)) {
            logw("Недопустимый режим темы: $mode, используется системная тема")
            val validMode = ThemeMode.System.value
            _uiState.value = _uiState.value.copy(themeMode = validMode)
            viewModelScope.launch {
                try {
                    settingsRepository.setThemeMode(validMode)
                } catch (e: Exception) {
                    loge("Ошибка при сохранении режима темы: ${e.message}", e)
                    _errorMessage.value = "Не удалось сохранить режим темы"
                }
            }
            return
        }
        
        val oldTheme = _uiState.value.themeMode
        _uiState.value = _uiState.value.copy(themeMode = mode)
        viewModelScope.launch {
            try {
                settingsRepository.setThemeMode(mode)
            } catch (e: Exception) {
                loge("Ошибка при сохранении режима темы: ${e.message}", e)
                _errorMessage.value = "Не удалось сохранить режим темы"
                // Откатываем изменение при ошибке
                _uiState.value = _uiState.value.copy(themeMode = oldTheme)
            }
        }
    }

    fun showDataRetentionDialog() {
        _dataRetentionDialogVisible.value = true
    }

    fun hideDataRetentionDialog() {
        _dataRetentionDialogVisible.value = false
    }

    fun setDataRetentionDays(days: Int) {
        // Валидация: проверяем, что значение входит в список допустимых
        if (days !in Constants.VALID_DATA_RETENTION_DAYS) {
            logw("Недопустимое значение периода хранения данных: $days, используется значение по умолчанию")
            val validDays = Constants.DEFAULT_DATA_RETENTION_DAYS
            _uiState.value = _uiState.value.copy(dataRetentionDays = validDays)
            viewModelScope.launch {
                try {
                    settingsRepository.setDataRetentionDays(validDays)
                } catch (e: Exception) {
                    loge("Ошибка при сохранении периода хранения данных: ${e.message}", e)
                    _errorMessage.value = "Не удалось сохранить период хранения данных"
                }
            }
            return
        }
        
        val oldDays = _uiState.value.dataRetentionDays
        _uiState.value = _uiState.value.copy(dataRetentionDays = days)
        viewModelScope.launch {
            try {
                settingsRepository.setDataRetentionDays(days)
            } catch (e: Exception) {
                loge("Ошибка при сохранении периода хранения данных: ${e.message}", e)
                _errorMessage.value = "Не удалось сохранить период хранения данных"
                // Откатываем изменение при ошибке
                _uiState.value = _uiState.value.copy(dataRetentionDays = oldDays)
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
     * Сбросить сообщение об ошибке
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
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
 * Промежуточное состояние для объединения первых 5 Flow
 */
private data class SettingsPartialState(
    val autoScanEnabled: Boolean,
    val notificationsEnabled: Boolean,
    val notificationSoundEnabled: Boolean,
    val notificationVibrationEnabled: Boolean,
    val scanIntervalMinutes: Int
)

/**
 * Состояние UI экрана настроек
 */
data class SettingsUiState(
    val autoScanEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val notificationSoundEnabled: Boolean = true,
    val notificationVibrationEnabled: Boolean = true,
    val scanIntervalMinutes: Int = 15,
    val themeMode: String = "system",
    val dataRetentionDays: Int = 30
)

/**
 * Результат операции очистки данных
 */
sealed class ClearDataResult {
    object Success : ClearDataResult()
    data class Error(val message: String) : ClearDataResult()
}