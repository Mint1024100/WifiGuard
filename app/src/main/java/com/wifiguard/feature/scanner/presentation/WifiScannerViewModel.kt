package com.wifiguard.feature.scanner.presentation

import android.net.wifi.WifiManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wifiguard.core.domain.model.WifiNetwork
import com.wifiguard.core.domain.model.WifiScanResult
import com.wifiguard.core.domain.repository.WifiRepository
import com.wifiguard.core.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

/**
 * ViewModel для управления сканированием Wi-Fi сетей.
 * Обрабатывает бизнес-логику сканирования и предоставляет данные для UI.
 */
@HiltViewModel
class WifiScannerViewModel @Inject constructor(
    private val wifiRepository: WifiRepository,
    private val wifiManager: WifiManager,
    private val wifiScannerService: com.wifiguard.core.data.wifi.WifiScannerService,
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
                
                // ЕДИНЫЙ ПУТЬ: запускаем сканирование через WifiScannerService (он учитывает throttling/restrictions).
                when (val status = wifiScannerService.startScan()) {
                    is com.wifiguard.core.domain.model.WifiScanStatus.Success -> {
                        // Даём системе немного времени обновить scanResults
                        delay(1200)
                        processScanResults()
                        _uiState.update {
                            it.copy(
                                isScanning = false,
                                lastScanTime = System.currentTimeMillis(),
                                scanCount = it.scanCount + 1
                            )
                        }
                    }
                    is com.wifiguard.core.domain.model.WifiScanStatus.Throttled -> {
                        // Используем кэшированные результаты, обновляем wifi_networks без добавления шума в wifi_scans
                        val cached = wifiScannerService.getScanResultsAsCoreModels()
                        wifiRepository.upsertNetworksFromScanResults(cached)
                        _uiState.update {
                            it.copy(
                                isScanning = false,
                                errorMessage = "Сканирование ограничено системой. Показаны кэшированные данные."
                            )
                        }
                    }
                    is com.wifiguard.core.domain.model.WifiScanStatus.Restricted -> {
                        _uiState.update {
                            it.copy(
                                isScanning = false,
                                errorMessage = "Сканирование ограничено системой. Попробуйте позже."
                            )
                        }
                    }
                    is com.wifiguard.core.domain.model.WifiScanStatus.Failed -> {
                        _uiState.update {
                            it.copy(
                                isScanning = false,
                                errorMessage = "Ошибка сканирования: ${status.error}"
                            )
                        }
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
     * ИСПРАВЛЕНО: Используем безопасный метод из WifiScannerService вместо прямого доступа к wifiManager.scanResults
     */
    private suspend fun processScanResults() {
        try {
            // ИСПРАВЛЕНО: Используем безопасный метод из WifiScannerService, который обрабатывает разрешения и ошибки
            val (scanResults, metadata) = wifiScannerService.getScanResultsWithMetadata()
            
            if (scanResults.isEmpty()) {
                Log.d("WifiScannerViewModel", "Нет результатов сканирования")
                return
            }
            
            Log.d("WifiScannerViewModel", "Обработка ${scanResults.size} результатов сканирования")
            
            // Результаты уже преобразованы в WifiScanResult в WifiScannerService
            val results = scanResults.map { result ->
                result.copy(scanType = com.wifiguard.core.domain.model.ScanType.MANUAL)
            }

            // ОПТИМИЗАЦИЯ: атомарная батч-запись (wifi_scans + wifi_networks)
            wifiRepository.persistScanResults(results)
            Log.d("WifiScannerViewModel", "Результаты сканирования успешно обработаны")
        } catch (e: SecurityException) {
            Log.e("WifiScannerViewModel", "Нет разрешений для получения результатов", e)
            _uiState.update { 
                it.copy(errorMessage = "Нет разрешений для получения результатов сканирования")
            }
        } catch (e: Exception) {
            Log.e("WifiScannerViewModel", "Ошибка обработки результатов", e)
            _uiState.update { 
                it.copy(errorMessage = "Ошибка обработки результатов: ${e.message}")
            }
        }
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