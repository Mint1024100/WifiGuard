package com.wifiguard.core.security

import com.wifiguard.feature.scanner.domain.model.WifiInfo
import com.wifiguard.feature.scanner.domain.model.SecurityType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityManager @Inject constructor(
    private val aesEncryption: AesEncryption
) {
    
    fun analyzeNetworkSecurity(wifiInfo: WifiInfo): SecurityAnalysisResult {
        val threats = mutableListOf<SecurityThreat>()
        var riskLevel = RiskLevel.LOW
        
        // Analyze encryption type
        when (wifiInfo.securityType) {
            SecurityType.OPEN -> {
                threats.add(SecurityThreat.OPEN_NETWORK)
                riskLevel = RiskLevel.HIGH
            }
            SecurityType.WEP -> {
                threats.add(SecurityThreat.WEAK_ENCRYPTION)
                riskLevel = RiskLevel.HIGH
            }
            SecurityType.WPA -> {
                threats.add(SecurityThreat.OUTDATED_SECURITY)
                riskLevel = RiskLevel.MEDIUM
            }
            SecurityType.WPA2 -> {
                // WPA2 is acceptable but check for other issues
                if (wifiInfo.signalStrength < -80) {
                    threats.add(SecurityThreat.WEAK_SIGNAL)
                }
            }
            SecurityType.WPA3 -> {
                // WPA3 is secure, check signal only
                if (wifiInfo.signalStrength < -80) {
                    threats.add(SecurityThreat.WEAK_SIGNAL)
                }
            }
            SecurityType.UNKNOWN -> {
                threats.add(SecurityThreat.UNKNOWN_SECURITY)
                riskLevel = RiskLevel.MEDIUM
            }
        }
        
        // Check for suspicious network names
        if (isSuspiciousNetworkName(wifiInfo.ssid)) {
            threats.add(SecurityThreat.SUSPICIOUS_NAME)
            if (riskLevel == RiskLevel.LOW) riskLevel = RiskLevel.MEDIUM
        }
        
        // Check signal strength for potential evil twin attacks
        if (wifiInfo.signalStrength > -30) {
            threats.add(SecurityThreat.UNUSUALLY_STRONG_SIGNAL)
            if (riskLevel == RiskLevel.LOW) riskLevel = RiskLevel.MEDIUM
        }
        
        return SecurityAnalysisResult(
            networkId = wifiInfo.bssid,
            riskLevel = riskLevel,
            threats = threats,
            recommendations = generateRecommendations(threats)
        )
    }
    
    private fun isSuspiciousNetworkName(ssid: String): Boolean {
        val suspiciousPatterns = listOf(
            "free", "wifi", "internet", "network", "connection",
            "hotspot", "guest", "public", "open", "test"
        )
        
        val lowerSsid = ssid.lowercase()
        return suspiciousPatterns.any { lowerSsid.contains(it) } ||
                ssid.length < 3 ||
                ssid.all { it.isDigit() }
    }
    
    private fun generateRecommendations(threats: List<SecurityThreat>): List<String> {
        val recommendations = mutableListOf<String>()
        
        threats.forEach { threat ->
            when (threat) {
                SecurityThreat.OPEN_NETWORK -> {
                    recommendations.add("Избегайте подключения к открытым сетям без защиты")
                    recommendations.add("Используйте VPN при подключении к открытым сетям")
                }
                SecurityThreat.WEAK_ENCRYPTION -> {
                    recommendations.add("Не подключайтесь к сетям с устаревшим шифрованием WEP")
                }
                SecurityThreat.OUTDATED_SECURITY -> {
                    recommendations.add("По возможности используйте сети с WPA2 или WPA3")
                }
                SecurityThreat.WEAK_SIGNAL -> {
                    recommendations.add("Слабый сигнал может указывать на удаленность или проблемы")
                }
                SecurityThreat.SUSPICIOUS_NAME -> {
                    recommendations.add("Будьте осторожны с сетями с подозрительными именами")
                }
                SecurityThreat.UNUSUALLY_STRONG_SIGNAL -> {
                    recommendations.add("Необычно сильный сигнал может указывать на поддельную точку доступа")
                }
                SecurityThreat.UNKNOWN_SECURITY -> {
                    recommendations.add("Проверьте тип безопасности сети перед подключением")
                }
            }
        }
        
        return recommendations.distinct()
    }
    
    fun encryptSensitiveData(data: String): EncryptedData {
        return aesEncryption.encrypt(data)
    }
    
    fun decryptSensitiveData(encryptedData: EncryptedData): String {
        return aesEncryption.decrypt(encryptedData)
    }
}

data class SecurityAnalysisResult(
    val networkId: String,
    val riskLevel: RiskLevel,
    val threats: List<SecurityThreat>,
    val recommendations: List<String>
)

enum class RiskLevel {
    LOW, MEDIUM, HIGH
}

enum class SecurityThreat {
    OPEN_NETWORK,
    WEAK_ENCRYPTION,
    OUTDATED_SECURITY,
    WEAK_SIGNAL,
    SUSPICIOUS_NAME,
    UNUSUALLY_STRONG_SIGNAL,
    UNKNOWN_SECURITY
}