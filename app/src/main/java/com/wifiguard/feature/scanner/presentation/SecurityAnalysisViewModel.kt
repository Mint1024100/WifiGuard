package com.wifiguard.feature.scanner.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wifiguard.core.domain.model.SecurityType
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
 * ViewModel для анализа безопасности Wi-Fi сетей.
 * Обнаруживает подозрительные сети, анализирует угрозы и предоставляет рекомендации.
 */
@HiltViewModel
class SecurityAnalysisViewModel @Inject constructor(
    private val wifiRepository: WifiRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    
    // Состояние UI
    private val _uiState = MutableStateFlow(SecurityAnalysisUiState())
    val uiState: StateFlow<SecurityAnalysisUiState> = _uiState.asStateFlow()
    
    // Подозрительные сети
    val suspiciousNetworks: StateFlow<List<WifiNetwork>> = wifiRepository.getSuspiciousNetworks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Результаты анализа безопасности
    private val _securityAnalysis = MutableStateFlow<List<SecurityThreat>>(emptyList())
    val securityAnalysis: StateFlow<List<SecurityThreat>> = _securityAnalysis.asStateFlow()
    
    // Статистика безопасности
    private val _securityStatistics = MutableStateFlow<SecurityStatistics?>(null)
    val securityStatistics: StateFlow<SecurityStatistics?> = _securityStatistics.asStateFlow()
    
    init {
        // Автоматически запускаем анализ при создании
        startSecurityAnalysis()
    }
    
    /**
     * Запустить анализ безопасности
     */
    fun startSecurityAnalysis() {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isAnalyzing = true, errorMessage = null) }
            
            try {
                // Получаем все сети
                wifiRepository.getAllNetworks().collect { networks ->
                    if (networks.isNotEmpty()) {
                        // Анализируем угрозы
                        val threats = analyzeSecurityThreats(networks)
                        _securityAnalysis.value = threats
                        
                        // Обновляем статистику
                        updateSecurityStatistics(networks, threats)
                        
                        _uiState.update { 
                            it.copy(
                                isAnalyzing = false,
                                lastAnalysisTime = System.currentTimeMillis()
                            )
                        }
                    } else {
                        _uiState.update { 
                            it.copy(
                                isAnalyzing = false,
                                errorMessage = "Нет данных для анализа"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isAnalyzing = false,
                        errorMessage = "Ошибка анализа: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Обновить фильтр угроз
     */
    fun updateThreatFilter(filter: ThreatFilter) {
        _uiState.update { it.copy(threatFilter = filter) }
    }
    
    /**
     * Очистить ошибку
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * Проанализировать угрозы безопасности
     */
    private fun analyzeSecurityThreats(networks: List<WifiNetwork>): List<SecurityThreat> {
        val threats = mutableListOf<SecurityThreat>()
        
        networks.forEach { network ->
            // 1. Открытые сети
            if (network.securityType == SecurityType.OPEN) {
                threats.add(
                    SecurityThreat(
                        networkSsid = network.ssid,
                        type = ThreatType.OPEN_NETWORK,
                        severity = ThreatSeverity.MEDIUM,
                        description = "Открытая сеть без шифрования",
                        recommendation = "Избегайте передачи конфиденциальных данных",
                        detectedAt = System.currentTimeMillis()
                    )
                )
            }
            
            // 2. Слабое шифрование WEP
            if (network.securityType == SecurityType.WEP) {
                threats.add(
                    SecurityThreat(
                        networkSsid = network.ssid,
                        type = ThreatType.WEAK_ENCRYPTION,
                        severity = ThreatSeverity.HIGH,
                        description = "Устаревшее шифрование WEP",
                        recommendation = "Не подключайтесь к этой сети",
                        detectedAt = System.currentTimeMillis()
                    )
                )
            }
            
            // 3. Подозрительные сети (искусственная точка доступа)
            if (network.isSuspicious) {
                threats.add(
                    SecurityThreat(
                        networkSsid = network.ssid,
                        type = ThreatType.ROGUE_ACCESS_POINT,
                        severity = ThreatSeverity.HIGH,
                        description = network.suspiciousReason ?: "Подозрительная точка доступа",
                        recommendation = "Не подключайтесь к этой сети",
                        detectedAt = System.currentTimeMillis()
                    )
                )
            }
            
            // 4. Подозрительные имена сетей
            if (isSuspiciousNetworkName(network.ssid)) {
                threats.add(
                    SecurityThreat(
                        networkSsid = network.ssid,
                        type = ThreatType.SUSPICIOUS_NAME,
                        severity = ThreatSeverity.LOW,
                        description = "Подозрительное имя сети",
                        recommendation = "Проверьте подлинность сети",
                        detectedAt = System.currentTimeMillis()
                    )
                )
            }
        }
        
        return threats
    }
    
    /**
     * Проверить, является ли имя сети подозрительным
     */
    private fun isSuspiciousNetworkName(ssid: String): Boolean {
        val suspiciousPatterns = listOf(
            "free", "wifi", "internet", "бесплатно",
            "hack", "test", "default", "linksys",
            "netgear", "dlink", "admin", "password"
        )
        
        return suspiciousPatterns.any { pattern ->
            ssid.contains(pattern, ignoreCase = true)
        }
    }
    
    /**
     * Обновить статистику безопасности
     */
    private fun updateSecurityStatistics(
        networks: List<WifiNetwork>,
        threats: List<SecurityThreat>
    ) {
        val totalNetworks = networks.size
        val openNetworks = networks.count { it.securityType == SecurityType.OPEN }
        val wepNetworks = networks.count { it.securityType == SecurityType.WEP }
        val wpaNetworks = networks.count { 
            it.securityType == SecurityType.WPA || it.securityType == SecurityType.WPA2
        }
        val wpa3Networks = networks.count { it.securityType == SecurityType.WPA3 }
        val suspiciousNetworks = networks.count { it.isSuspicious }
        
        val threatsBySeverity = threats.groupBy { it.severity }
            .mapValues { it.value.size }
        
        _securityStatistics.value = SecurityStatistics(
            totalNetworks = totalNetworks,
            secureNetworks = wpaNetworks + wpa3Networks,
            openNetworks = openNetworks,
            wepNetworks = wepNetworks,
            suspiciousNetworks = suspiciousNetworks,
            totalThreats = threats.size,
            highSeverityThreats = threatsBySeverity[ThreatSeverity.HIGH] ?: 0,
            mediumSeverityThreats = threatsBySeverity[ThreatSeverity.MEDIUM] ?: 0,
            lowSeverityThreats = threatsBySeverity[ThreatSeverity.LOW] ?: 0
        )
    }
}

/**
 * Состояние UI для анализа безопасности
 */
data class SecurityAnalysisUiState(
    val isAnalyzing: Boolean = false,
    val errorMessage: String? = null,
    val lastAnalysisTime: Long? = null,
    val threatFilter: ThreatFilter = ThreatFilter.ALL
)

/**
 * Угроза безопасности
 */
data class SecurityThreat(
    val networkSsid: String,
    val type: ThreatType,
    val severity: ThreatSeverity,
    val description: String,
    val recommendation: String,
    val detectedAt: Long
)

/**
 * Типы угроз
 */
enum class ThreatType(val displayName: String) {
    OPEN_NETWORK("Открытая сеть"),
    WEAK_ENCRYPTION("Слабое шифрование"),
    ROGUE_ACCESS_POINT("Мошенническая точка доступа"),
    SUSPICIOUS_NAME("Подозрительное имя")
}

/**
 * Уровень серьёзности угрозы
 */
enum class ThreatSeverity(val displayName: String, val color: String) {
    HIGH("Высокий", "#FF5722"),
    MEDIUM("Средний", "#FF9800"),
    LOW("Низкий", "#FFC107")
}

/**
 * Фильтры для угроз
 */
enum class ThreatFilter(val displayName: String) {
    ALL("Все угрозы"),
    HIGH("Высокие"),
    MEDIUM("Средние"),
    LOW("Низкие")
}

/**
 * Статистика безопасности
 */
data class SecurityStatistics(
    val totalNetworks: Int,
    val secureNetworks: Int,
    val openNetworks: Int,
    val wepNetworks: Int,
    val suspiciousNetworks: Int,
    val totalThreats: Int,
    val highSeverityThreats: Int,
    val mediumSeverityThreats: Int,
    val lowSeverityThreats: Int
) {
    val securityScore: Int
        get() = if (totalNetworks == 0) 100 else {
            val secureRatio = secureNetworks.toFloat() / totalNetworks
            val threatPenalty = (totalThreats * 10).coerceAtMost(50)
            ((secureRatio * 100) - threatPenalty).toInt().coerceIn(0, 100)
        }
}