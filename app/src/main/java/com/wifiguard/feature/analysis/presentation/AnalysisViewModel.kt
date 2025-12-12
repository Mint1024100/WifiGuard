package com.wifiguard.feature.analysis.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wifiguard.core.domain.repository.WifiRepository
import com.wifiguard.core.security.SecurityAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * ViewModel для экрана анализа
 * 
 * ОПТИМИЗИРОВАНО: Переведено на реактивный подход с автообновлением через Flow
 * вместо однократной загрузки с .first()
 */
@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val wifiRepository: WifiRepository,
    private val securityAnalyzer: SecurityAnalyzer
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    init {
        observeAnalysisData()
    }

    /**
     * Наблюдать за изменениями данных и автоматически обновлять анализ
     * 
     * ИСПРАВЛЕНО: Использует .onEach().launchIn() вместо .first()
     * Теперь UI обновляется автоматически при изменении данных в БД
     */
    private fun observeAnalysisData() {
        // Устанавливаем начальное состояние загрузки
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        wifiRepository.getLatestScans(limit = 100)
            .onEach { recentScans ->
                try {
                    // Запускаем анализ безопасности
                    val securityReport = securityAnalyzer.analyzeNetworks(recentScans)
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        securityReport = securityReport,
                        lastUpdateTime = System.currentTimeMillis(),
                        error = null
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Ошибка анализа данных",
                        lastUpdateTime = System.currentTimeMillis()
                    )
                }
            }
            .catch { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Ошибка загрузки данных",
                    lastUpdateTime = System.currentTimeMillis()
                )
            }
            .launchIn(viewModelScope)
    }

    /**
     * Обновить анализ вручную (уже не требуется, т.к. автообновление работает)
     * Оставлено для совместимости с UI
     */
    fun refreshAnalysis() {
        // Теперь обновление происходит автоматически через Flow
        // Просто сбрасываем ошибку и обновляем timestamp
        _uiState.value = _uiState.value.copy(
            error = null,
            lastUpdateTime = System.currentTimeMillis()
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * Состояние UI экрана анализа
 * 
 * ОБНОВЛЕНО: Добавлено поле lastUpdateTime для отображения времени обновления
 */
data class AnalysisUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val securityReport: com.wifiguard.core.security.SecurityReport? = null,
    val lastUpdateTime: Long = 0L
)