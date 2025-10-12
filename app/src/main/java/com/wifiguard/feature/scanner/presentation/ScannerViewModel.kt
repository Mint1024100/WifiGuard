package com.wifiguard.feature.scanner.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wifiguard.core.data.wifi.WifiScanner
import com.wifiguard.core.domain.model.WifiScanResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для экрана сканирования
 */
@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val wifiScanner: WifiScanner
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()
    
    init {
        checkWifiStatus()
    }
    
    fun startScan() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isScanning = true,
                    error = null
                )
                
                val result = wifiScanner.startScan()
                if (result.isSuccess) {
                    val networks = result.getOrNull() ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        isScanning = false,
                        networks = networks,
                        lastScanTime = System.currentTimeMillis(),
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isScanning = false,
                        error = result.exceptionOrNull()?.message ?: "Ошибка сканирования"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    error = e.message ?: "Неизвестная ошибка"
                )
            }
        }
    }
    
    fun checkWifiStatus() {
        _uiState.value = _uiState.value.copy(
            isWifiEnabled = wifiScanner.isWifiEnabled()
        )
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
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
