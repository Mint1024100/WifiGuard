package com.wifiguard.feature.analysis.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wifiguard.core.data.wifi.ScanStatusBus
import com.wifiguard.core.data.wifi.ScanStatusState
import com.wifiguard.core.domain.repository.WifiRepository
import com.wifiguard.core.security.SecurityAnalyzer
import com.wifiguard.core.service.WifiForegroundScanService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * ViewModel для экрана анализа
 * 
 * ОПТИМИЗИРОВАНО: Переведено на реактивный подход с автообновлением через Flow
 * вместо однократной загрузки с .first()
 */
@HiltViewModel
class AnalysisViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val wifiRepository: WifiRepository,
    private val securityAnalyzer: SecurityAnalyzer,
    private val scanStatusBus: ScanStatusBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    init {
        observeScanStatus()
        observeAnalysisData()
    }

    /**
     * Запросить автоскан (UI может вызывать при входе на экран).
     * Реальный скан выполняет ForegroundService, а мы отображаем состояние через [ScanStatusBus].
     */
    fun requestAutoScan() {
        scanStatusBus.update(ScanStatusState.Starting())
    }

    private fun observeScanStatus() {
        scanStatusBus.state
            .onEach { status ->
                // ИСПРАВЛЕНО: Используем update() для атомарного обновления состояния
                _uiState.update { it.copy(scanStatus = status) }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Наблюдать за изменениями данных и автоматически обновлять анализ
     * 
     * ИСПРАВЛЕНО: Использует .onEach().launchIn() вместо .first()
     * Теперь UI обновляется автоматически при изменении данных в БД
     */
    @OptIn(FlowPreview::class) // debounce() помечен как preview API в kotlinx.coroutines
    private fun observeAnalysisData() {
        // ИСПРАВЛЕНО: Используем update() для атомарного обновления состояния
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        wifiRepository.getLatestScans(limit = 100)
            // Room может эмитить пачку обновлений при массовой вставке сканов.
            // Делаем debounce + mapLatest, чтобы пересчитывать отчёт один раз и отменять устаревшие расчёты.
            // ИСПРАВЛЕНО: Увеличено значение debounce для более стабильной работы на медленных устройствах
            .debounce(500)
            .distinctUntilChangedBy { scans ->
                // Достаточно дешёвого «сигнатурного» ключа: последняя запись + размер.
                val headTimestamp = scans.firstOrNull()?.timestamp ?: -1L
                headTimestamp to scans.size
            }
            .mapLatest { recentScans ->
                if (recentScans.isEmpty()) null else securityAnalyzer.analyzeNetworks(recentScans)
            }
            .onEach { securityReport ->
                // ИСПРАВЛЕНО: Используем update() для атомарного обновления состояния
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        securityReport = securityReport,
                        lastUpdateTime = System.currentTimeMillis(),
                        error = if (securityReport == null) "Нет данных для анализа" else null
                    )
                }
            }
            .catch { e ->
                // ИСПРАВЛЕНО: Используем update() для атомарного обновления состояния
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = e.message ?: "Ошибка загрузки данных",
                        lastUpdateTime = System.currentTimeMillis()
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Единая функция для перезагрузки/обновления данных
     * Запускает новое сканирование через ForegroundService
     */
    fun refreshData(context: Context) {
        // ИСПРАВЛЕНО: НЕ обновляем lastUpdateTime здесь, чтобы избежать перезапуска LaunchedEffect
        // lastUpdateTime будет обновлен автоматически при получении новых данных через observeAnalysisData
        _uiState.update { currentState ->
            currentState.copy(
                error = null
                // УБРАНО: lastUpdateTime = System.currentTimeMillis() - это вызывало цикл перезапуска LaunchedEffect
            )
        }
        // Запускаем новое сканирование
        WifiForegroundScanService.start(context)
    }

    /**
     * Обновить анализ вручную (уже не требуется, т.к. автообновление работает)
     * Оставлено для совместимости с UI
     * @deprecated Используйте refreshData(context) вместо этого
     */
    @Deprecated("Используйте refreshData(context) вместо этого")
    fun refreshAnalysis() {
        // Теперь обновление происходит автоматически через Flow
        // ИСПРАВЛЕНО: Используем update() для атомарного обновления состояния
        _uiState.update { currentState ->
            currentState.copy(
                error = null,
                lastUpdateTime = System.currentTimeMillis()
            )
        }
    }

    fun clearError() {
        // ИСПРАВЛЕНО: Используем update() для атомарного обновления состояния
        _uiState.update { it.copy(error = null) }
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
    val lastUpdateTime: Long = 0L,
    val scanStatus: ScanStatusState = ScanStatusState.Idle
)