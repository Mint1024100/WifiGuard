package com.wifiguard.feature.scanner.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wifiguard.core.domain.model.WifiNetwork
import com.wifiguard.core.domain.model.WifiScanResult
import com.wifiguard.core.domain.repository.WifiRepository
import com.wifiguard.core.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для управления деталями конкретной Wi-Fi сети.
 * Предоставляет детальную информацию, статистику и аналитику по сети.
 */
@HiltViewModel
class NetworkDetailsViewModel @Inject constructor(
    private val wifiRepository: WifiRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    
    // Состояние UI
    private val _uiState = MutableStateFlow(NetworkDetailsUiState())
    val uiState: StateFlow<NetworkDetailsUiState> = _uiState.asStateFlow()
    
    // Информация о текущей сети
    private val _currentNetwork = MutableStateFlow<WifiNetwork?>(null)
    val currentNetwork: StateFlow<WifiNetwork?> = _currentNetwork.asStateFlow()
    
    // Статистика сканирования по сети
    private val _networkStatistics = MutableStateFlow<List<WifiScanResult>>(emptyList())
    val networkStatistics: StateFlow<List<WifiScanResult>> = _networkStatistics.asStateFlow()
    
    // Аналитика сигнала
    private val _signalAnalytics = MutableStateFlow<SignalAnalytics?>(null)
    val signalAnalytics: StateFlow<SignalAnalytics?> = _signalAnalytics.asStateFlow()
    
    /**
     * Загрузить детали сети по SSID
     */
    fun loadNetworkDetails(ssid: String) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                // Получаем сеть
                val network = wifiRepository.getNetworkBySSID(ssid)
                if (network != null) {
                    _currentNetwork.value = network
                    
                    // Загружаем статистику
                    loadNetworkStatistics(ssid)
                    
                    _uiState.update { it.copy(isLoading = false) }
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "Сеть не найдена: $ssid"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Ошибка загрузки сети: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Загрузить статистику сканирования сети
     */
    private fun loadNetworkStatistics(ssid: String) {
        viewModelScope.launch(ioDispatcher) {
            try {
                wifiRepository.getNetworkStatistics(ssid).collect { scans ->
                    _networkStatistics.value = scans
                    
                    // Вычисляем аналитику сигнала
                    if (scans.isNotEmpty()) {
                        calculateSignalAnalytics(scans)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = "Ошибка загрузки статистики: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Пометить сеть как подозрительную
     */
    fun markAsSuspicious(reason: String) {
        val network = _currentNetwork.value ?: return
        
        viewModelScope.launch(ioDispatcher) {
            try {
                wifiRepository.markNetworkAsSuspicious(network.ssid, reason)
                
                // Обновляем локальное состояние
                _currentNetwork.value = network.copy(
                    isSuspicious = true,
                    suspiciousReason = reason,
                    lastUpdated = System.currentTimeMillis()
                )
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = "Ошибка при отметке сети: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Очистить ошибку
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * Обновить период статистики
     */
    fun updateStatisticsPeriod(period: StatisticsPeriod) {
        _uiState.update { it.copy(statisticsPeriod = period) }
        
        // Перезагружаем статистику с новым периодом
        _currentNetwork.value?.let { loadNetworkDetails(it.ssid) }
    }
    
    /**
     * Вычислить аналитику сигнала
     */
    private fun calculateSignalAnalytics(scans: List<WifiScanResult>) {
        if (scans.isEmpty()) return
        
        val signalLevels = scans.map { it.level }
        
        val analytics = SignalAnalytics(
            averageSignal = signalLevels.average().toInt(),
            minSignal = signalLevels.minOrNull() ?: 0,
            maxSignal = signalLevels.maxOrNull() ?: 0,
            scansCount = scans.size,
            lastScanTime = scans.maxByOrNull { it.timestamp }?.timestamp ?: 0,
            signalVariation = calculateSignalVariation(signalLevels)
        )
        
        _signalAnalytics.value = analytics
    }
    
    /**
     * Вычислить вариацию сигнала (стандартное отклонение)
     */
    private fun calculateSignalVariation(signalStrengths: List<Int>): Double {
        if (signalStrengths.size < 2) return 0.0
        
        val mean = signalStrengths.average()
        val variance = signalStrengths.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
}

/**
 * Состояние UI для экрана деталей сети
 */
data class NetworkDetailsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val statisticsPeriod: StatisticsPeriod = StatisticsPeriod.LAST_24_HOURS
)

/**
 * Аналитика сигнала сети
 */
data class SignalAnalytics(
    val averageSignal: Int,
    val minSignal: Int,
    val maxSignal: Int,
    val scansCount: Int,
    val lastScanTime: Long,
    val signalVariation: Double
)

/**
 * Периоды для отображения статистики
 */
enum class StatisticsPeriod(val displayName: String, val milliseconds: Long) {
    LAST_HOUR("Последний час", 60 * 60 * 1000),
    LAST_24_HOURS("Последние 24 часа", 24 * 60 * 60 * 1000),
    LAST_WEEK("Последняя неделя", 7 * 24 * 60 * 60 * 1000),
    ALL_TIME("Всё время", Long.MAX_VALUE)
}