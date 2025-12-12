package com.wifiguard.feature.securityreport.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wifiguard.core.domain.repository.WifiRepository
import com.wifiguard.core.security.SecurityAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityReportViewModel @Inject constructor(
    private val wifiRepository: WifiRepository,
    private val securityAnalyzer: SecurityAnalyzer
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecurityReportUiState())
    val uiState: StateFlow<SecurityReportUiState> = _uiState.asStateFlow()

    init {
        loadSecurityReport()
    }

    fun loadSecurityReport() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // Загружаем данные для отчета безопасности
                val recentScans = wifiRepository.getLatestScans(limit = 100).first()
                
                // Создаем отчет безопасности
                val securityReport = securityAnalyzer.analyzeNetworks(recentScans)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    securityReport = securityReport
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Ошибка загрузки данных отчета"
                )
            }
        }
    }

    fun refreshReport() {
        loadSecurityReport()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class SecurityReportUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val securityReport: com.wifiguard.core.security.SecurityReport? = null
)