package com.wifiguard.feature.scanner.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wifiguard.core.domain.model.WifiScanResult
import com.wifiguard.core.domain.model.ScanType
import com.wifiguard.core.domain.repository.WifiRepository
import com.wifiguard.core.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * ViewModel для управления историей сканирования Wi-Fi сетей.
 * Обеспечивает фильтрацию, поиск и анализ данных о сканированиях.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val wifiRepository: WifiRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    
    // Состояние UI
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    
    // Поисковый запрос
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Исходные данные сканирования
    private val rawScanResults: StateFlow<List<WifiScanResult>> = wifiRepository.getLatestScans(1000)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Отфильтрованные результаты сканирования
    val filteredScanResults: StateFlow<List<WifiScanResult>> = combine(
        rawScanResults,
        _searchQuery,
        _uiState
    ) { scans, query, state ->
        filterScanResults(scans, query, state)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    // Статистика сканирования
    private val _scanStatistics = MutableStateFlow<ScanStatistics?>(null)
    val scanStatistics: StateFlow<ScanStatistics?> = _scanStatistics.asStateFlow()
    
    init {
        // Автоматически обновляем статистику при изменении данных
        viewModelScope.launch {
            rawScanResults.collect { scans ->
                updateScanStatistics(scans)
            }
        }
    }
    
    /**
     * Обновить поисковый запрос
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * Очистить поиск
     */
    fun clearSearch() {
        _searchQuery.value = ""
    }
    
    /**
     * Обновить фильтр по времени
     */
    fun updateTimeFilter(filter: TimeFilter) {
        _uiState.update { it.copy(timeFilter = filter) }
    }
    
    /**
     * Обновить фильтр по типу сканирования
     */
    fun updateScanTypeFilter(filter: ScanTypeFilter) {
        _uiState.update { it.copy(scanTypeFilter = filter) }
    }
    
    /**
     * Обновить сортировку
     */
    fun updateSortOption(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
    }
    
    /**
     * Очистить старые записи сканирования
     */
    fun clearOldScans(olderThanDays: Int) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(olderThanDays.toLong())
                wifiRepository.clearOldScans(cutoffTime)
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        message = "Успешно очищены записи старше $olderThanDays дней"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Ошибка очистки данных: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Очистить сообщения
     */
    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, message = null) }
    }
    
    /**
     * Отфильтровать результаты сканирования
     */
    private fun filterScanResults(
        scans: List<WifiScanResult>, 
        query: String, 
        state: HistoryUiState
    ): List<WifiScanResult> {
        var filtered = scans
        
        // Фильтр по времени
        if (state.timeFilter != TimeFilter.ALL_TIME) {
            val cutoffTime = System.currentTimeMillis() - state.timeFilter.milliseconds
            filtered = filtered.filter { it.timestamp >= cutoffTime }
        }
        
        // Фильтр по типу сканирования
        if (state.scanTypeFilter != ScanTypeFilter.ALL) {
            filtered = filtered.filter { 
                it.scanType == when (state.scanTypeFilter) {
                    ScanTypeFilter.MANUAL -> ScanType.MANUAL
                    ScanTypeFilter.AUTOMATIC -> ScanType.AUTOMATIC
                    ScanTypeFilter.BACKGROUND -> ScanType.BACKGROUND
                    ScanTypeFilter.ALL -> it.scanType // This case won't be reached due to the condition above
                }
            }
        }
        
        // Поиск по SSID
        if (query.isNotBlank()) {
            filtered = filtered.filter { 
                it.ssid.contains(query, ignoreCase = true)
            }
        }
        
        // Сортировка
        return when (state.sortOption) {
            SortOption.TIME_DESC -> filtered.sortedByDescending { it.timestamp }
            SortOption.TIME_ASC -> filtered.sortedBy { it.timestamp }
            SortOption.SIGNAL_DESC -> filtered.sortedByDescending { it.level }
            SortOption.SIGNAL_ASC -> filtered.sortedBy { it.level }
            SortOption.SSID_ASC -> filtered.sortedBy { it.ssid }
            SortOption.FREQUENCY_DESC -> filtered.sortedByDescending { it.frequency }
        }
    }
    
    /**
     * Обновить статистику сканирования
     */
    private fun updateScanStatistics(scans: List<WifiScanResult>) {
        if (scans.isEmpty()) {
            _scanStatistics.value = null
            return
        }
        
        val now = System.currentTimeMillis()
        val last24Hours = now - TimeUnit.DAYS.toMillis(1)
        val lastWeek = now - TimeUnit.DAYS.toMillis(7)
        
        val scansLast24h = scans.count { it.timestamp >= last24Hours }
        val scansLastWeek = scans.count { it.timestamp >= lastWeek }
        
        val uniqueNetworks = scans.distinctBy { it.ssid }.size
        val averageSignal = scans.map { it.level }.average().takeIf { !it.isNaN() } ?: 0.0
        
        val scansByType = scans.groupBy { it.scanType }
            .mapValues { it.value.size }
        
        _scanStatistics.value = ScanStatistics(
            totalScans = scans.size,
            scansLast24Hours = scansLast24h,
            scansLastWeek = scansLastWeek,
            uniqueNetworks = uniqueNetworks,
            averageSignalStrength = averageSignal.toInt(),
            scansByType = scansByType
        )
    }
}

/**
 * Состояние UI для экрана истории
 */
data class HistoryUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null,
    val timeFilter: TimeFilter = TimeFilter.ALL_TIME,
    val scanTypeFilter: ScanTypeFilter = ScanTypeFilter.ALL,
    val sortOption: SortOption = SortOption.TIME_DESC
)

/**
 * Фильтры по времени
 */
enum class TimeFilter(val displayName: String, val milliseconds: Long) {
    LAST_HOUR("Последний час", TimeUnit.HOURS.toMillis(1)),
    LAST_24_HOURS("Последние 24 часа", TimeUnit.DAYS.toMillis(1)),
    LAST_WEEK("Последняя неделя", TimeUnit.DAYS.toMillis(7)),
    LAST_MONTH("Последний месяц", TimeUnit.DAYS.toMillis(30)),
    ALL_TIME("Всё время", 0)
}

/**
 * Фильтры по типу сканирования
 */
enum class ScanTypeFilter(val displayName: String) {
    ALL("Все типы"),
    MANUAL("Ручное"),
    AUTOMATIC("Автоматическое"),
    BACKGROUND("Фоновое")
}

/**
 * Опции сортировки
 */
enum class SortOption(val displayName: String) {
    TIME_DESC("По времени (сначала новые)"),
    TIME_ASC("По времени (сначала старые)"),
    SIGNAL_DESC("По силе сигнала (сильные)"),
    SIGNAL_ASC("По силе сигнала (слабые)"),
    SSID_ASC("По алфавиту (SSID)"),
    FREQUENCY_DESC("По частоте (высокие)")
}

/**
 * Преобразовать ScanTypeFilter в ScanType
 */
fun ScanTypeFilter.toScanType(): ScanType {
    return when (this) {
        ScanTypeFilter.ALL -> ScanType.MANUAL // для случая ALL возвращаем любое значение, т.к. фильтр не применяется
        ScanTypeFilter.MANUAL -> ScanType.MANUAL
        ScanTypeFilter.AUTOMATIC -> ScanType.AUTOMATIC
        ScanTypeFilter.BACKGROUND -> ScanType.BACKGROUND
    }
}

/**
 * Статистика сканирования
 */
data class ScanStatistics(
    val totalScans: Int,
    val scansLast24Hours: Int,
    val scansLastWeek: Int,
    val uniqueNetworks: Int,
    val averageSignalStrength: Int,
    val scansByType: Map<ScanType, Int>
)