package com.wifiguard.feature.scanner.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wifiguard.core.common.*
import com.wifiguard.core.common.Result
import com.wifiguard.core.data.wifi.WifiScanner
import com.wifiguard.core.common.Logger
import com.wifiguard.core.domain.model.WifiScanResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import javax.inject.Inject

/**
 * ViewModel для экрана сканирования
 */
@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val wifiScanner: WifiScanner,
    val permissionHandler: PermissionHandler,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    companion object {
        private const val KEY_PERMISSION_STATE = "permission_state"
        private const val KEY_SCAN_RESULT = "scan_result"
        private const val KEY_UI_STATE = "ui_state"
        private const val KEY_LAST_SCAN_TIME = "last_scan_time"
    }
    
    private val _permissionState = MutableStateFlow(
        stringToPermissionState(savedStateHandle.get<String>(KEY_PERMISSION_STATE) ?: permissionStateToString(PermissionState.NotChecked))
    )
    
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()
    
    private val _scanResult = savedStateHandle.getStateFlow<String>(KEY_SCAN_RESULT, "")
        .map { json ->
            if (json.isNotEmpty()) {
                try {
                    val serializableResult = Json.decodeFromString<SerializableResultWrapper>(json)
                    serializableResult.toResultForWifiList()
                } catch (e: Exception) {
                    Result.Loading
                }
            } else {
                Result.Loading
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Result.Loading
        )
    
    // Состояние текущей подключенной сети
    private val _currentNetwork = MutableStateFlow<WifiScanResult?>(null)
    val currentNetwork: StateFlow<WifiScanResult?> = _currentNetwork.asStateFlow()
    
    private val _uiState = MutableStateFlow(
        stringToScannerUiState(savedStateHandle.get<String>(KEY_UI_STATE))
    )
    
    val scanResult: StateFlow<Result<List<WifiScanResult>>> = _scanResult
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()
    
    init {
        // Начинаем слушать изменения в SavedStateHandle для синхронизации с MutableStateFlow
        viewModelScope.launch {
            savedStateHandle.getStateFlow<String>(
                KEY_PERMISSION_STATE,
                permissionStateToString(PermissionState.NotChecked)
            ).collect { stringState ->
                _permissionState.value = stringToPermissionState(stringState)
            }
        }
        
        // Также слушаем изменения для состояния UI
        viewModelScope.launch {
            savedStateHandle.getStateFlow<String>(
                KEY_UI_STATE,
                scannerUiStateToString(ScannerUiState())
            ).collect { stringState ->
                val currentState = _uiState.value
                _uiState.value = stringToScannerUiState(stringState).copy(
                    networks = currentState.networks // Сохраняем список сетей, который не сохраняется в состоянии
                )
            }
        }
        
        checkPermissions()
        checkWifiStatus()
        restoreLastScanTime()
        
        // Загружаем информацию о текущей подключенной сети
        loadCurrentNetwork()

        // Автоматически запускаем сканирование только если есть разрешения
        if (permissionHandler.hasWifiScanPermissions()) {
            startScan()
        }
    }
    
    private fun updatePermissionState(newState: PermissionState) {
        _permissionState.value = newState
        savedStateHandle[KEY_PERMISSION_STATE] = permissionStateToString(newState)
    }
    
    fun checkPermissions() {
        val newState = if (permissionHandler.hasWifiScanPermissions()) {
            PermissionState.Granted
        } else {
            PermissionState.NotGranted
        }
        updatePermissionState(newState)
    }
    
    fun onPermissionResult(granted: Boolean, shouldShowRationale: Boolean) {
        val newState = when {
            granted -> PermissionState.Granted
            shouldShowRationale -> PermissionState.ShouldShowRationale
            else -> PermissionState.PermanentlyDenied
        }
        
        updatePermissionState(newState)
        
        // Обновляем состояние UI при изменении разрешений
        _uiState.value = _uiState.value.copy(
            error = if (!granted) "Нет разрешения на сканирование" else null
        )
        savedStateHandle[KEY_UI_STATE] = scannerUiStateToString(_uiState.value)
        
        if (granted) {
            startScan()
        }
    }
    
    fun startScan() {
        if (!permissionHandler.hasWifiScanPermissions()) {
            updatePermissionState(PermissionState.NotGranted)
            _uiState.value = _uiState.value.copy(error = "Нет разрешения на сканирование Wi-Fi")
            savedStateHandle[KEY_UI_STATE] = scannerUiStateToString(_uiState.value)
            savedStateHandle[KEY_SCAN_RESULT] = Json.encodeToString(
                Result.Error(
                    AppException.NoPermissionException(),
                    "Нет разрешения на сканирование Wi-Fi"
                ).toSerializableForWifiList()
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true)
            savedStateHandle[KEY_UI_STATE] = scannerUiStateToString(_uiState.value)
            savedStateHandle[KEY_SCAN_RESULT] = Json.encodeToString(SerializableResultWrapper("Loading"))
            
            try {
                val result = wifiScanner.startScan()
                val newResult = if (result.isSuccess) {
                    val networks = result.getOrNull() ?: emptyList()
                    // Обновляем состояние UI с сетями
                    _uiState.value = _uiState.value.copy(
                        isScanning = false,
                        networks = networks,
                        error = null
                    )
                    // Сохраняем время последного успешного сканирования
                    savedStateHandle[KEY_LAST_SCAN_TIME] = System.currentTimeMillis()
                    Result.Success(networks)
                } else {
                    _uiState.value = _uiState.value.copy(isScanning = false)
                    Result.Error(
                        result.exceptionOrNull() ?: Exception("Ошибка сканирования"),
                        result.exceptionOrNull()?.message ?: "Ошибка сканирования"
                    )
                }
                savedStateHandle[KEY_UI_STATE] = scannerUiStateToString(_uiState.value)
                savedStateHandle[KEY_SCAN_RESULT] = Json.encodeToString(newResult.toSerializableForWifiList())
            } catch (e: SecurityException) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    error = "Нет разрешения на сканирование Wi-Fi"
                )
                savedStateHandle[KEY_UI_STATE] = scannerUiStateToString(_uiState.value)
                savedStateHandle[KEY_SCAN_RESULT] = Json.encodeToString(Result.Error(e, "Нет разрешения на сканирование Wi-Fi").toSerializableForWifiList())
                updatePermissionState(PermissionState.NotGranted)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    error = e.toUserMessage()
                )
                // ВАЖНО: Обновляем scanResult на Error, чтобы выйти из состояния Loading
                savedStateHandle[KEY_UI_STATE] = scannerUiStateToString(_uiState.value)
                savedStateHandle[KEY_SCAN_RESULT] = Json.encodeToString(Result.Error(e, e.toUserMessage()).toSerializableForWifiList())
            }
        }
    }
    
    fun checkWifiStatus() {
        val currentState = _uiState.value
        val newState = currentState.copy(
            isWifiEnabled = wifiScanner.isWifiEnabled()
        )
        _uiState.value = newState
        savedStateHandle[KEY_UI_STATE] = scannerUiStateToString(newState)
    }
    
    fun clearError() {
        val currentResultJson = savedStateHandle.get<String>(KEY_SCAN_RESULT)
        if (!currentResultJson.isNullOrEmpty()) {
            try {
                val serializableResult = Json.decodeFromString<SerializableResultWrapper>(currentResultJson)
                if (serializableResult.type == "Error") {
                    savedStateHandle[KEY_SCAN_RESULT] = Json.encodeToString(SerializableResultWrapper("Loading"))
                    _uiState.value = _uiState.value.copy(error = null)
                    savedStateHandle[KEY_UI_STATE] = scannerUiStateToString(_uiState.value)
                }
            } catch (e: Exception) {
                // В случае ошибки десериализации, просто продолжаем
            }
        }
    }
    
    fun retry() {
        startScan()
    }
    
    private fun restoreLastScanTime() {
        val lastScanTime = savedStateHandle.get<Long>(KEY_LAST_SCAN_TIME)
        if (lastScanTime != null) {
            val currentState = _uiState.value
            val newState = currentState.copy(lastScanTime = lastScanTime)
            _uiState.value = newState
            savedStateHandle[KEY_UI_STATE] = scannerUiStateToString(newState)
        }
    }
    
    sealed class PermissionState {
        object NotChecked : PermissionState()
        object NotGranted : PermissionState()
        object ShouldShowRationale : PermissionState()
        object PermanentlyDenied : PermissionState()
        object Granted : PermissionState()
        
        companion object {
            @JvmStatic
            fun fromString(value: String?): PermissionState {
                return when (value) {
                    "NOT_CHECKED" -> NotChecked
                    "NOT_GRANTED" -> NotGranted
                    "SHOULD_SHOW_RATIONALE" -> ShouldShowRationale
                    "PERMANENTLY_DENIED" -> PermanentlyDenied
                    "GRANTED" -> Granted
                    else -> NotChecked
                }
            }
        }
    }
    
    private fun permissionStateToString(permissionState: PermissionState): String {
        return when (permissionState) {
            is PermissionState.NotChecked -> "NOT_CHECKED"
            is PermissionState.NotGranted -> "NOT_GRANTED"
            is PermissionState.ShouldShowRationale -> "SHOULD_SHOW_RATIONALE"
            is PermissionState.PermanentlyDenied -> "PERMANENTLY_DENIED"
            is PermissionState.Granted -> "GRANTED"
        }
    }
    
    private fun stringToPermissionState(value: String?): PermissionState {
        return try {
            PermissionState.fromString(value)
        } catch (e: Exception) {
            PermissionState.NotChecked // возвращаем безопасное значение по умолчанию
        }
    }
    
    // Загрузить информацию о текущей подключенной сети
    private fun loadCurrentNetwork() {
        viewModelScope.launch {
            try {
                val currentNetworkResult = wifiScanner.getCurrentNetwork()
                _currentNetwork.value = currentNetworkResult
            } catch (e: Exception) {
                // Логируем ошибку, но не показываем пользователю, так как это не критично
                // для основного функционала сканирования
                Logger.e("Ошибка получения текущей сети", e)
            }
        }
    }
    
    // Ручное обновление информации о текущей подключенной сети
    fun refreshCurrentNetwork() {
        loadCurrentNetwork()
    }
    
    // Expose the permission handler for use in UI
    fun hasWifiPermissions(): Boolean = permissionHandler.hasWifiScanPermissions()
    
    private fun scannerUiStateToString(uiState: ScannerUiState): String {
        return try {
            val serializableState = SerializableScannerUiState(
                isScanning = uiState.isScanning,
                isWifiEnabled = uiState.isWifiEnabled,
                lastScanTime = uiState.lastScanTime,
                error = uiState.error
            )
            Json.encodeToString(SerializableScannerUiState.serializer(), serializableState)
        } catch (e: Exception) {
            // Возвращаем безопасное значение по умолчанию в случае ошибки сериализации
            val defaultState = SerializableScannerUiState()
            Json.encodeToString(SerializableScannerUiState.serializer(), defaultState)
        }
    }
    
    private fun stringToScannerUiState(jsonString: String?): ScannerUiState {
        return try {
            if (jsonString.isNullOrEmpty()) {
                ScannerUiState()
            } else {
                val serializableState = Json.decodeFromString(SerializableScannerUiState.serializer(), jsonString)
                // Преобразуем обратно в полное состояние, но без сетей (они не сохраняются)
                ScannerUiState(
                    isScanning = serializableState.isScanning,
                    isWifiEnabled = serializableState.isWifiEnabled,
                    networks = emptyList(), // Сети не сохраняются в состоянии, будут загружены отдельно
                    lastScanTime = serializableState.lastScanTime,
                    error = serializableState.error
                )
            }
        } catch (e: Exception) {
            ScannerUiState() // возвращаем безопасное значение по умолчанию
        }
    }
}

/**
 * Состояние UI экрана сканирования для использования в ViewModel
 */
data class ScannerUiState(
    val isScanning: Boolean = false,
    val isWifiEnabled: Boolean = true,
    val networks: List<WifiScanResult> = emptyList(),
    val lastScanTime: Long? = null,
    val error: String? = null
)

@Serializable
data class SerializableScannerUiState(
    val isScanning: Boolean = false,
    val isWifiEnabled: Boolean = true,
    val lastScanTime: Long? = null,
    val error: String? = null
)
