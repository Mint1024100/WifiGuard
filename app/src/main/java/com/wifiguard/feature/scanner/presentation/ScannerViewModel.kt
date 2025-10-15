package com.wifiguard.feature.scanner.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wifiguard.core.common.AppException
import com.wifiguard.core.common.PermissionHandler
import com.wifiguard.core.common.Result
import com.wifiguard.core.common.asResult
import com.wifiguard.core.common.toUserMessage
import com.wifiguard.core.data.wifi.WifiScanner
import com.wifiguard.core.domain.model.WifiScanResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для экрана сканирования
 */
@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val wifiScanner: WifiScanner,
    private val permissionHandler: PermissionHandler,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    companion object {
        private const val KEY_PERMISSION_STATE = "permission_state"
        private const val KEY_SCAN_RESULT = "scan_result"
        private const val KEY_UI_STATE = "ui_state"
        private const val KEY_LAST_SCAN_TIME = "last_scan_time"
    }
    
    private val _permissionState = savedStateHandle.getStateFlow<PermissionState>(
        KEY_PERMISSION_STATE, 
        PermissionState.NotChecked
    )
    private val _scanResult = savedStateHandle.getStateFlow<Result<List<WifiScanResult>>>(
        KEY_SCAN_RESULT,
        Result.Loading
    )
    private val _uiState = savedStateHandle.getStateFlow<ScannerUiState>(
        KEY_UI_STATE,
        ScannerUiState()
    )
    
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()
    val scanResult: StateFlow<Result<List<WifiScanResult>>> = _scanResult.asStateFlow()
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()
    
    init {
        checkPermissions()
        checkWifiStatus()
        restoreLastScanTime()
    }
    
    fun checkPermissions() {
        val newState = if (permissionHandler.hasWifiScanPermissions()) {
            PermissionState.Granted
        } else {
            PermissionState.NotGranted
        }
        savedStateHandle[KEY_PERMISSION_STATE] = newState
    }
    
    fun onPermissionResult(granted: Boolean, shouldShowRationale: Boolean) {
        val newState = when {
            granted -> PermissionState.Granted
            shouldShowRationale -> PermissionState.ShouldShowRationale
            else -> PermissionState.PermanentlyDenied
        }
        
        savedStateHandle[KEY_PERMISSION_STATE] = newState
        
        if (granted) {
            startScan()
        }
    }
    
    fun startScan() {
        if (!permissionHandler.hasWifiScanPermissions()) {
            savedStateHandle[KEY_PERMISSION_STATE] = PermissionState.NotGranted
            savedStateHandle[KEY_SCAN_RESULT] = Result.Error(
                AppException.NoPermissionException(),
                "Нет разрешения на сканирование Wi-Fi"
            )
            return
        }
        
        viewModelScope.launch {
            savedStateHandle[KEY_SCAN_RESULT] = Result.Loading
            
            try {
                val result = wifiScanner.startScan()
                val newResult = if (result.isSuccess) {
                    val networks = result.getOrNull() ?: emptyList()
                    // Сохраняем время последнего успешного сканирования
                    savedStateHandle[KEY_LAST_SCAN_TIME] = System.currentTimeMillis()
                    Result.Success(networks)
                } else {
                    Result.Error(
                        result.exceptionOrNull() ?: Exception("Ошибка сканирования"),
                        result.exceptionOrNull()?.message ?: "Ошибка сканирования"
                    )
                }
                savedStateHandle[KEY_SCAN_RESULT] = newResult
            } catch (e: SecurityException) {
                savedStateHandle[KEY_SCAN_RESULT] = Result.Error(e, "Нет разрешения на сканирование Wi-Fi")
                savedStateHandle[KEY_PERMISSION_STATE] = PermissionState.NotGranted
            } catch (e: Exception) {
                savedStateHandle[KEY_SCAN_RESULT] = Result.Error(e, e.toUserMessage())
            }
        }
    }
    
    fun checkWifiStatus() {
        val currentState = savedStateHandle.get<ScannerUiState>(KEY_UI_STATE) ?: ScannerUiState()
        val newState = currentState.copy(
            isWifiEnabled = wifiScanner.isWifiEnabled()
        )
        savedStateHandle[KEY_UI_STATE] = newState
    }
    
    fun clearError() {
        if (savedStateHandle.get<Result<List<WifiScanResult>>>(KEY_SCAN_RESULT) is Result.Error) {
            savedStateHandle[KEY_SCAN_RESULT] = Result.Loading
        }
    }
    
    fun retry() {
        startScan()
    }
    
    private fun restoreLastScanTime() {
        val lastScanTime = savedStateHandle.get<Long>(KEY_LAST_SCAN_TIME)
        if (lastScanTime != null) {
            val currentState = savedStateHandle.get<ScannerUiState>(KEY_UI_STATE) ?: ScannerUiState()
            val newState = currentState.copy(lastScanTime = lastScanTime)
            savedStateHandle[KEY_UI_STATE] = newState
        }
    }
    
    sealed class PermissionState {
        object NotChecked : PermissionState()
        object NotGranted : PermissionState()
        object ShouldShowRationale : PermissionState()
        object PermanentlyDenied : PermissionState()
        object Granted : PermissionState()
    }
    
    // Expose the permission handler for use in UI
    fun hasWifiPermissions(): Boolean = permissionHandler.hasWifiScanPermissions()
}

/**
 * Состояние UI экрана сканирования
 */
data class ScannerUiState(
    val isScanning: Boolean = false,
    val isWifiEnabled: Boolean = true,
    val networks: List<WifiScanResult> = emptyList(),
    val lastScanTime: Long? = null,
    val error: String? = null
)
