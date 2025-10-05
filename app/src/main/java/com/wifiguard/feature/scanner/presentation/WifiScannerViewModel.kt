package com.wifiguard.feature.scanner.presentation

import android.net.wifi.WifiManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wifiguard.core.domain.model.WifiNetwork
import com.wifiguard.core.domain.model.WifiScanResult
import com.wifiguard.core.domain.repository.WifiRepository
import com.wifiguard.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для управления сканированием Wi-Fi сетей.
 * Обрабатывает бизнес-логику сканирования и предоставляет данные для UI.
 */
@HiltViewModel
class WifiScannerViewModel @Inject constructor(
    private val wifiRepository: WifiRepository,
    private val wifiManager: WifiManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    
    // Состояние UI
    private val _uiState = MutableStateFlow(WifiScannerUiState())
    val uiState: StateFlow<WifiScannerUiState> = _uiState.asStateFlow()
    
    // Список обнаруженных сетей
    private val _discoveredNetworks = MutableStateFlow<List<WifiNetwork>>(emptyList())
    val discoveredNetworks: StateFlow<List<WifiNetwork>> = _discoveredNetworks.asStateFlow()
    
    // Подозрительные сети
    val suspiciousNetworks: StateFlow<List<WifiNetwork>> = wifiRepository.getSuspiciousNetworks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Последние результаты сканирования
    val latestScans: StateFlow<List<WifiScanResult>> = wifiRepository.getLatestScans(50)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    init {
        // Загрузка сохранённых сетей при создании
        loadSavedNetworks()
    }
    
    /**
     * Запустить сканирование Wi-Fi сетей
     */
    fun startWifiScan() {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isScanning = true, errorMessage = null) }
            
            try {
                // Проверка включено ли Wi-Fi
                if (!wifiManager.isWifiEnabled) {
                    _uiState.update { 
                        it.copy(
                            isScanning = false, 
                            errorMessage = "ошибка: Wi-Fi отключён. Пожалуйста, включите Wi-Fi."
                        )
                    }
                    return@launch
                }
                
                // Запуск сканирования
                val scanStarted = wifiManager.startScan()
                
                if (scanStarted) {
                    // Мок данные для тестирования (в реальном приложении заменить на реальные результаты)
                    processScanResults()
                    
                    _uiState.update { 
                        it.copy(
                            isScanning = false, 
                            lastScanTime = System.currentTimeMillis(),
                            scanCount = it.scanCount + 1
                        )
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isScanning = false, 
                            errorMessage = "Не удалось запустить сканирование"
                        )
                    }
                }
                
            } catch (e: SecurityException) {
                _uiState.update { 
                    it.copy(
                        isScanning = false, 
                        errorMessage = "Не хватает разрешений для сканирования Wi-Fi"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isScanning = false, 
                        errorMessage = "Ошибка сканирования: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Остановить сканирование
     */
    fun stopWifiScan() {
        _uiState.update { it.copy(isScanning = false) }
    }
    
    /**
     * Очистить ошибку
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * Пометить сеть как подозрительную
     */
    fun markNetworkAsSuspicious(ssid: String, reason: String) {
        viewModelScope.launch(ioDispatcher) {
            try {
                wifiRepository.markNetworkAsSuspicious(ssid, reason)
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = "Ошибка при отметке сети: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Обновить фильтр сетей
     */
    fun updateNetworkFilter(filter: NetworkFilter) {
        _uiState.update { it.copy(networkFilter = filter) }
        loadSavedNetworks() // Перезагрузить с новым фильтром
    }
    
    /**
     * Загрузить сохранённые сети
     */
    private fun loadSavedNetworks() {
        viewModelScope.launch(ioDispatcher) {
            wifiRepository.getAllNetworks().collect { networks ->
                val filteredNetworks = when (_uiState.value.networkFilter) {
                    NetworkFilter.ALL -> networks
                    NetworkFilter.SUSPICIOUS -> networks.filter { it.isSuspicious }
                    NetworkFilter.KNOWN -> networks.filter { it.isKnown }
                    NetworkFilter.UNKNOWN -> networks.filter { !it.isKnown }
                }
                _discoveredNetworks.value = filteredNetworks
            }
        }
    }
    
    /**
     * Обработать результаты сканирования
     * TODO: Заменить на реальную обработку результатов сканирования
     */
    private suspend fun processScanResults() {
        // Мок результаты для тестирования
        val mockScanResult = WifiScanResult(
            ssid = "TestNetwork",
            bssid = "00:11:22:33:44:55",
            signalStrength = -45,
            frequency = 2450,
            channel = 6,
            timestamp = System.currentTimeMillis(),
            scanType = WifiScanResult.ScanType.MANUAL
        )
        
        // Сохранить результат сканирования
        wifiRepository.insertScanResult(mockScanResult)
    }
}

/**
 * Состояние UI для экрана сканера
 */
data class WifiScannerUiState(
    val isScanning: Boolean = false,
    val lastScanTime: Long? = null,
    val scanCount: Int = 0,
    val errorMessage: String? = null,
    val networkFilter: NetworkFilter = NetworkFilter.ALL
)

/**
 * Фильтр для отображения сетей
 */
enum class NetworkFilter {
    ALL,        // Все сети
    SUSPICIOUS, // Подозрительные
    KNOWN,      // Известные
    UNKNOWN     // Неизвестные
}