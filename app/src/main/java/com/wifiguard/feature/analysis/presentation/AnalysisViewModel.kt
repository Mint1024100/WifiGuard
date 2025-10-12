package com.wifiguard.feature.analysis.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wifiguard.core.data.wifi.WifiScanner
import com.wifiguard.core.security.SecurityAnalyzer
import com.wifiguard.core.security.SecurityReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для экрана анализа безопасности
 */
@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val wifiScanner: WifiScanner,
    private val securityAnalyzer: SecurityAnalyzer
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()
    
    fun analyzeNetworks() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    error = null
                )
                
                // Получаем результаты сканирования
                val scanResult = wifiScanner.startScan()
                if (scanResult.isSuccess) {
                    val networks = scanResult.getOrNull() ?: emptyList()
                    
                    // Анализируем безопасность
                    val securityReport = securityAnalyzer.analyzeNetworks(networks)
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        securityReport = securityReport,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = scanResult.exceptionOrNull()?.message ?: "Ошибка сканирования"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Неизвестная ошибка"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * Состояние UI экрана анализа
 */
data class AnalysisUiState(
    val isLoading: Boolean = false,
    val securityReport: SecurityReport? = null,
    val error: String? = null
)
