package com.wifiguard.feature.scanner.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wifiguard.core.domain.model.WifiNetwork
import com.wifiguard.core.domain.model.WifiScanResult
import com.wifiguard.core.domain.repository.WifiRepository
import com.wifiguard.core.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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

    // Текущий BSSID, для которого загружены детали (важно для списка с переворотом карточек)
    private val _loadedBssid = MutableStateFlow<String?>(null)
    val loadedBssid: StateFlow<String?> = _loadedBssid.asStateFlow()
    
    // Статистика сканирования по сети
    private val _networkStatistics = MutableStateFlow<List<WifiScanResult>>(emptyList())
    val networkStatistics: StateFlow<List<WifiScanResult>> = _networkStatistics.asStateFlow()
    
    // Аналитика сигнала
    private val _signalAnalytics = MutableStateFlow<SignalAnalytics?>(null)
    val signalAnalytics: StateFlow<SignalAnalytics?> = _signalAnalytics.asStateFlow()

    private var statisticsJob: Job? = null
    
    /**
     * Загрузить детали сети по BSSID (уникальный MAC точки доступа).
     */
    fun loadNetworkDetails(bssid: String) {
        viewModelScope.launch(ioDispatcher) {
            _loadedBssid.value = bssid
            _uiState.update { it.copy(isLoading = true, errorMessage = null, bssid = bssid) }
            
            // #region agent log
            try {
                val logJson = org.json.JSONObject().apply {
                    put("sessionId", "debug-session")
                    put("runId", "run1")
                    put("hypothesisId", "D")
                    put("location", "NetworkDetailsViewModel.kt:loadNetworkDetails")
                    put("message", "Начало загрузки деталей сети")
                    put("data", org.json.JSONObject().apply {
                        put("bssid", bssid)
                    })
                    put("timestamp", System.currentTimeMillis())
                }
                java.io.File("/Users/mint1024/Desktop/андроид/.cursor/debug.log").appendText("${logJson}\n")
            } catch (e: Exception) {}
            // #endregion
            
            try {
                // Получаем сеть
                val network = wifiRepository.getNetworkByBssid(bssid)
                // #region agent log
                try {
                    val logJson = org.json.JSONObject().apply {
                        put("sessionId", "debug-session")
                        put("runId", "run1")
                        put("hypothesisId", "D")
                        put("location", "NetworkDetailsViewModel.kt:loadNetworkDetails:afterGetNetwork")
                        put("message", "Результат getNetworkByBssid")
                        put("data", org.json.JSONObject().apply {
                            put("bssid", bssid)
                            put("networkFound", network != null)
                            if (network != null) {
                                put("ssid", network.ssid)
                                put("firstSeen", network.firstSeen)
                            }
                        })
                        put("timestamp", System.currentTimeMillis())
                    }
                    java.io.File("/Users/mint1024/Desktop/андроид/.cursor/debug.log").appendText("${logJson}\n")
                } catch (e: Exception) {}
                // #endregion
                
                // Сеть может отсутствовать в БД - это нормально для новых сетей
                _currentNetwork.value = network
                
                // Загружаем статистику (даже если сети нет в БД, статистика может быть)
                loadNetworkStatisticsByBssid(bssid)
                
                _uiState.update { it.copy(isLoading = false, errorMessage = null) }
            } catch (e: Exception) {
                // #region agent log
                try {
                    val logJson = org.json.JSONObject().apply {
                        put("sessionId", "debug-session")
                        put("runId", "run1")
                        put("hypothesisId", "D")
                        put("location", "NetworkDetailsViewModel.kt:loadNetworkDetails:exception")
                        put("message", "Исключение при загрузке сети")
                        put("data", org.json.JSONObject().apply {
                            put("bssid", bssid)
                            put("error", e.message ?: "unknown")
                            put("errorType", e.javaClass.simpleName)
                        })
                        put("timestamp", System.currentTimeMillis())
                    }
                    java.io.File("/Users/mint1024/Desktop/андроид/.cursor/debug.log").appendText("${logJson}\n")
                } catch (logEx: Exception) {}
                // #endregion
                
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
     * Загрузить статистику сканирования сети по BSSID.
     */
    private fun loadNetworkStatisticsByBssid(bssid: String) {
        statisticsJob?.cancel()
        statisticsJob = viewModelScope.launch(ioDispatcher) {
            try {
                wifiRepository.getNetworkStatisticsByBssid(bssid).collect { scans ->
                    _networkStatistics.value = scans
                    if (scans.isNotEmpty()) {
                        calculateSignalAnalytics(scans)
                    } else {
                        _signalAnalytics.value = null
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
                val ssid = network.ssid
                if (ssid.isBlank()) {
                    _uiState.update { it.copy(errorMessage = "Нельзя пометить сеть без SSID") }
                    return@launch
                }

                wifiRepository.markNetworkAsSuspicious(ssid, reason)
                
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
        val bssid = _loadedBssid.value
        if (!bssid.isNullOrBlank()) {
            loadNetworkDetails(bssid)
        }
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
    val statisticsPeriod: StatisticsPeriod = StatisticsPeriod.LAST_24_HOURS,
    val bssid: String? = null
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