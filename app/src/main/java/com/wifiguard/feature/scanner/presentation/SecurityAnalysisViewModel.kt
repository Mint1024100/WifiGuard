package com.wifiguard.feature.scanner.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.ThreatSeverity
import com.wifiguard.core.domain.model.ThreatType
import com.wifiguard.core.domain.model.WifiNetwork
import com.wifiguard.core.domain.model.WifiScanResult
import com.wifiguard.core.security.RiskLevel
import com.wifiguard.core.domain.repository.WifiRepository
import com.wifiguard.core.di.IoDispatcher
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
    private val securityManager: com.wifiguard.core.security.SecurityManager,
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
     * Проанализировать угрозы безопасности с использованием SecurityManager
     */
    private fun analyzeSecurityThreats(networks: List<WifiNetwork>): List<SecurityThreat> {
        val threats = mutableListOf<SecurityThreat>()
        
        networks.forEach { network ->
            try {
                // Преобразуем WifiNetwork в WifiInfo для SecurityManager
                val wifiInfo = com.wifiguard.feature.scanner.domain.model.WifiInfo(
                    ssid = network.ssid,
                    bssid = network.bssid,
                    capabilities = "",
                    level = network.signalStrength,
                    frequency = network.frequency,
                    timestamp = network.lastSeen,
                    encryptionType = mapSecurityTypeToEncryptionType(network.securityType),
                    signalStrength = network.signalStrength,
                    channel = network.channel,
                    bandwidth = null,
                    isHidden = network.ssid.isEmpty()
                )
                
                // Используем SecurityManager для анализа
                val analysisResult = securityManager.analyzeNetworkSecurity(wifiInfo)
                
                // Преобразуем результаты SecurityManager в наши угрозы
                analysisResult.threats.forEach { threatType ->
                    val threatSeverity = when (analysisResult.riskLevel) {
                        com.wifiguard.core.security.RiskLevel.LOW -> ThreatSeverity.LOW
                        com.wifiguard.core.security.RiskLevel.MEDIUM -> ThreatSeverity.MEDIUM
                        com.wifiguard.core.security.RiskLevel.HIGH -> ThreatSeverity.HIGH
                    }
                    
                    val description = getThreatDescription(threatType)
                    val recommendation = getThreatRecommendation(threatType)
                    
                    threats.add(
                        SecurityThreat(
                            networkSsid = network.ssid,
                            type = threatType,
                            severity = threatSeverity,
                            description = description,
                            recommendation = recommendation,
                            detectedAt = analysisResult.analysisTimestamp
                        )
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("SecurityAnalysisViewModel", "Error analyzing network ${network.ssid}", e)
            }
        }
        
        return threats
    }
    
    /** 
     * Map SecurityType to EncryptionType for compatibility
     */
    private fun mapSecurityTypeToEncryptionType(securityType: SecurityType): com.wifiguard.feature.scanner.domain.model.EncryptionType {
        return when (securityType) {
            SecurityType.OPEN -> com.wifiguard.feature.scanner.domain.model.EncryptionType.NONE
            SecurityType.WEP -> com.wifiguard.feature.scanner.domain.model.EncryptionType.WEP
            SecurityType.WPA -> com.wifiguard.feature.scanner.domain.model.EncryptionType.WPA
            SecurityType.WPA2 -> com.wifiguard.feature.scanner.domain.model.EncryptionType.WPA2
            SecurityType.WPA3 -> com.wifiguard.feature.scanner.domain.model.EncryptionType.WPA3
            SecurityType.WPA2_WPA3 -> com.wifiguard.feature.scanner.domain.model.EncryptionType.WPA2
            SecurityType.EAP -> com.wifiguard.feature.scanner.domain.model.EncryptionType.UNKNOWN
            SecurityType.UNKNOWN -> com.wifiguard.feature.scanner.domain.model.EncryptionType.UNKNOWN
        }
    }


    
    /** 
     * Вспомогательный класс для возврата четырех значений
     */
    private data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )

    /**
     * Получить описание угрозы
     */
    private fun getThreatDescription(threatType: ThreatType): String {
        return when (threatType) {
            ThreatType.OPEN_NETWORK -> "Открытая сеть без шифрования"
            ThreatType.WEAK_ENCRYPTION -> "Устаревшее или слабое шифрование"
            ThreatType.EVIL_TWIN -> "Обнаружена поддельная точка доступа (Evil Twin)"
            ThreatType.SUSPICIOUS_SSID -> "Подозрительное имя сети"
            ThreatType.SUSPICIOUS_BSSID -> "Подозрительный MAC-адрес"
            ThreatType.SUSPICIOUS_ACTIVITY -> "Обнаружена подозрительная активность"
            ThreatType.DUPLICATE_SSID -> "Обнаружено дублирование SSID"
            ThreatType.MULTIPLE_DUPLICATES -> "Обнаружено несколько дубликатов SSID"
            ThreatType.WEAK_SIGNAL -> "Слабый сигнал"
            ThreatType.UNKNOWN_ENCRYPTION -> "Неизвестный тип шифрования"
            else -> "Обнаружена потенциальная угроза"
        }
    }
    
    /**
     * Получить рекомендации для угрозы
     */
    private fun getThreatRecommendation(threatType: ThreatType): String {
        return when (threatType) {
            ThreatType.OPEN_NETWORK -> "Избегайте передачи конфиденциальных данных"
            ThreatType.WEAK_ENCRYPTION -> "Не подключайтесь к этой сети"
            ThreatType.EVIL_TWIN -> "Немедленно отключитесь от этой сети"
            ThreatType.SUSPICIOUS_SSID -> "Проверьте подлинность сети"
            ThreatType.SUSPICIOUS_BSSID -> "Будьте осторожны при подключении"
            ThreatType.SUSPICIOUS_ACTIVITY -> "Возможна атака на сеть"
            ThreatType.DUPLICATE_SSID -> "Возможна атака evil twin"
            ThreatType.MULTIPLE_DUPLICATES -> "Высокая вероятность атаки evil twin"
            ThreatType.WEAK_SIGNAL -> "Слабый сигнал может указывать на проблемы"
            ThreatType.UNKNOWN_ENCRYPTION -> "Проверьте тип безопасности сети"
            else -> "Будьте внимательны"
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

// ThreatType определен в SecurityReport.kt

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