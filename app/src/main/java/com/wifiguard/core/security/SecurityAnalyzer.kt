package com.wifiguard.core.security

import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.domain.model.WifiScanResult
import com.wifiguard.core.domain.model.SecurityThreat
import com.wifiguard.core.domain.model.ThreatType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Анализатор безопасности Wi-Fi сетей
 */
@Singleton
class SecurityAnalyzer @Inject constructor(
    private val threatDetector: ThreatDetector,
    private val encryptionAnalyzer: EncryptionAnalyzer
) {
    
    /**
     * Анализировать безопасность списка сетей
     */
    suspend fun analyzeNetworks(scanResults: List<WifiScanResult>): SecurityReport {
        val threats = mutableListOf<SecurityThreat>()
        val networkAnalysis = mutableListOf<NetworkSecurityAnalysis>()
        
        // Анализируем каждую сеть
        scanResults.forEach { network ->
            val analysis = analyzeNetwork(network, scanResults)
            networkAnalysis.add(analysis)
            
            // Добавляем угрозы
            if (analysis.threatLevel.isHighOrCritical()) {
                threats.addAll(analysis.threats)
            }
        }
        
        // Детектируем глобальные угрозы
        val globalThreats = threatDetector.detectGlobalThreats(scanResults)
        threats.addAll(globalThreats)
        
        return SecurityReport(
            timestamp = System.currentTimeMillis(),
            totalNetworks = scanResults.size,
            safeNetworks = networkAnalysis.count { it.threatLevel.isSafe() },
            lowRiskNetworks = networkAnalysis.count { it.threatLevel == ThreatLevel.LOW },
            mediumRiskNetworks = networkAnalysis.count { it.threatLevel == ThreatLevel.MEDIUM },
            highRiskNetworks = networkAnalysis.count { it.threatLevel == ThreatLevel.HIGH },
            criticalRiskNetworks = networkAnalysis.count { it.threatLevel == ThreatLevel.CRITICAL },
            threats = threats,
            networkAnalysis = networkAnalysis,
            overallRiskLevel = calculateOverallRiskLevel(threats),
            recommendations = generateRecommendations(threats, networkAnalysis)
        )
    }
    
    /**
     * Анализировать отдельную сеть
     */
    private fun analyzeNetwork(
        network: WifiScanResult,
        allNetworks: List<WifiScanResult>
    ): NetworkSecurityAnalysis {
        val threats = mutableListOf<SecurityThreat>()
        
        // Анализ типа шифрования
        val encryptionAnalysis = encryptionAnalyzer.analyzeEncryption(network)
        threats.addAll(encryptionAnalysis.threats)
        
        // Проверка на дублирующиеся SSID
        val duplicateSsidThreat = threatDetector.detectDuplicateSsid(network, allNetworks)
        if (duplicateSsidThreat != null) {
            threats.add(duplicateSsidThreat)
        }
        
        // Проверка на подозрительные SSID
        val suspiciousSsidThreat = threatDetector.detectSuspiciousSsid(network)
        if (suspiciousSsidThreat != null) {
            threats.add(suspiciousSsidThreat)
        }
        
        // Проверка на слабый сигнал
        if (network.level < -80) {
            threats.add(
                SecurityThreat(
                    type = ThreatType.WEAK_SIGNAL,
                    severity = ThreatLevel.MEDIUM,
                    description = "Очень слабый сигнал (${network.level} dBm)",
                    networkSsid = network.ssid,
                    networkBssid = network.bssid
                )
            )
        }
        
        // Определяем общий уровень угрозы
        val threatLevel = determineThreatLevel(threats, network.securityType)
        
        return NetworkSecurityAnalysis(
            network = network,
            threatLevel = threatLevel,
            threats = threats,
            encryptionAnalysis = encryptionAnalysis,
            securityScore = calculateSecurityScore(network, threats)
        )
    }
    
    /**
     * Определить уровень угрозы
     */
    private fun determineThreatLevel(
        threats: List<SecurityThreat>,
        securityType: SecurityType
    ): ThreatLevel {
        if (threats.isEmpty() && securityType.isRecommended()) {
            return ThreatLevel.SAFE
        }
        
        val maxThreatLevel = threats.maxOfOrNull { it.severity } ?: ThreatLevel.SAFE
        
        return when {
            maxThreatLevel == ThreatLevel.CRITICAL -> ThreatLevel.CRITICAL
            maxThreatLevel == ThreatLevel.HIGH -> ThreatLevel.HIGH
            maxThreatLevel == ThreatLevel.MEDIUM -> ThreatLevel.MEDIUM
            securityType.isInsecure() -> ThreatLevel.HIGH
            securityType.isDeprecated() -> ThreatLevel.MEDIUM
            else -> ThreatLevel.LOW
        }
    }
    
    /**
     * Рассчитать общий уровень риска
     */
    private fun calculateOverallRiskLevel(threats: List<SecurityThreat>): ThreatLevel {
        if (threats.isEmpty()) return ThreatLevel.SAFE
        
        val criticalCount = threats.count { it.severity == ThreatLevel.CRITICAL }
        val highCount = threats.count { it.severity == ThreatLevel.HIGH }
        val mediumCount = threats.count { it.severity == ThreatLevel.MEDIUM }
        
        return when {
            criticalCount > 0 -> ThreatLevel.CRITICAL
            highCount > 2 -> ThreatLevel.HIGH
            highCount > 0 || mediumCount > 3 -> ThreatLevel.MEDIUM
            mediumCount > 0 -> ThreatLevel.LOW
            else -> ThreatLevel.SAFE
        }
    }
    
    /**
     * Рассчитать оценку безопасности
     */
    private fun calculateSecurityScore(
        network: WifiScanResult,
        threats: List<SecurityThreat>
    ): Int {
        var score = 100
        
        // Штрафы за тип безопасности
        score -= when (network.securityType) {
            SecurityType.OPEN -> 80
            SecurityType.WEP -> 60
            SecurityType.WPA -> 40
            SecurityType.WPA2 -> 10
            SecurityType.WPA3 -> 0
            SecurityType.WPA2_WPA3 -> 5
            SecurityType.EAP -> 15
            SecurityType.UNKNOWN -> 50
        }
        
        // Штрафы за угрозы
        threats.forEach { threat ->
            score -= when (threat.severity) {
                ThreatLevel.CRITICAL -> 30
                ThreatLevel.HIGH -> 20
                ThreatLevel.MEDIUM -> 10
                ThreatLevel.LOW -> 5
                else -> 0
            }
        }
        
        // Штраф за слабый сигнал
        if (network.level < -80) {
            score -= 15
        }
        
        return maxOf(0, score)
    }
    
    /**
     * Генерировать рекомендации
     */
    private fun generateRecommendations(
        threats: List<SecurityThreat>,
        networkAnalysis: List<NetworkSecurityAnalysis>
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Рекомендации по типам угроз
        if (threats.any { it.type == ThreatType.OPEN_NETWORK }) {
            recommendations.add("Избегайте подключения к открытым Wi-Fi сетям")
        }
        
        if (threats.any { it.type == ThreatType.WEAK_ENCRYPTION }) {
            recommendations.add("Не используйте сети с WEP или WPA шифрованием")
        }
        
        if (threats.any { it.type == ThreatType.DUPLICATE_SSID }) {
            recommendations.add("Остерегайтесь дублирующихся SSID - возможна атака Evil Twin")
        }
        
        if (threats.any { it.type == ThreatType.SUSPICIOUS_SSID }) {
            recommendations.add("Не подключайтесь к сетям с подозрительными именами")
        }
        
        // Общие рекомендации
        val criticalNetworks = networkAnalysis.count { it.threatLevel == ThreatLevel.CRITICAL }
        if (criticalNetworks > 0) {
            recommendations.add("Обнаружено $criticalNetworks критически небезопасных сетей")
        }
        
        val safeNetworks = networkAnalysis.count { it.threatLevel.isSafe() }
        if (safeNetworks > 0) {
            recommendations.add("Рекомендуется использовать только WPA2/WPA3 сети ($safeNetworks найдено)")
        }
        
        return recommendations
    }
}
