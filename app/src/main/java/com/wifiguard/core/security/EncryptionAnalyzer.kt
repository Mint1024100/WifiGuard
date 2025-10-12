package com.wifiguard.core.security

import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.domain.model.WifiScanResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Анализатор шифрования Wi-Fi сетей
 */
@Singleton
class EncryptionAnalyzer @Inject constructor() {
    
    /**
     * Анализировать шифрование сети
     */
    fun analyzeEncryption(network: WifiScanResult): EncryptionAnalysis {
        val threats = mutableListOf<SecurityThreat>()
        val vulnerabilities = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        
        // Анализируем тип безопасности
        when (network.securityType) {
            SecurityType.OPEN -> {
                threats.add(
                    SecurityThreat(
                        type = ThreatType.OPEN_NETWORK,
                        severity = ThreatLevel.CRITICAL,
                        description = "Сеть без шифрования",
                        networkSsid = network.ssid,
                        networkBssid = network.bssid
                    )
                )
                vulnerabilities.add("Отсутствие шифрования")
                recommendations.add("Не подключайтесь к открытым сетям")
            }
            
            SecurityType.WEP -> {
                threats.add(
                    SecurityThreat(
                        type = ThreatType.WEAK_ENCRYPTION,
                        severity = ThreatLevel.HIGH,
                        description = "Устаревшее шифрование WEP",
                        networkSsid = network.ssid,
                        networkBssid = network.bssid
                    )
                )
                vulnerabilities.add("WEP легко взламывается")
                vulnerabilities.add("Статический ключ")
                vulnerabilities.add("Слабый алгоритм RC4")
                recommendations.add("Обновите роутер до WPA2/WPA3")
            }
            
            SecurityType.WPA -> {
                threats.add(
                    SecurityThreat(
                        type = ThreatType.WEAK_ENCRYPTION,
                        severity = ThreatLevel.MEDIUM,
                        description = "Устаревшее шифрование WPA",
                        networkSsid = network.ssid,
                        networkBssid = network.bssid
                    )
                )
                vulnerabilities.add("WPA имеет известные уязвимости")
                vulnerabilities.add("Слабый алгоритм TKIP")
                recommendations.add("Обновите до WPA2 или WPA3")
            }
            
            SecurityType.WPA2 -> {
                // WPA2 относительно безопасен, но проверяем на дополнительные уязвимости
                if (network.capabilities.contains("TKIP")) {
                    threats.add(
                        SecurityThreat(
                            type = ThreatType.WEAK_ENCRYPTION,
                            severity = ThreatLevel.LOW,
                            description = "WPA2 с устаревшим TKIP",
                            networkSsid = network.ssid,
                            networkBssid = network.bssid
                        )
                    )
                    vulnerabilities.add("TKIP устарел и имеет уязвимости")
                    recommendations.add("Используйте CCMP вместо TKIP")
                }
                
                if (network.capabilities.contains("WPS")) {
                    threats.add(
                        SecurityThreat(
                            type = ThreatType.WPS_VULNERABILITY,
                            severity = ThreatLevel.MEDIUM,
                            description = "Включен WPS (уязвим к атакам)",
                            networkSsid = network.ssid,
                            networkBssid = network.bssid
                        )
                    )
                    vulnerabilities.add("WPS может быть взломан")
                    recommendations.add("Отключите WPS в настройках роутера")
                }
            }
            
            SecurityType.WPA3 -> {
                // WPA3 самый безопасный, но проверяем на дополнительные настройки
                if (network.capabilities.contains("SAE")) {
                    // SAE (Simultaneous Authentication of Equals) - хорошо
                }
            }
            
            SecurityType.WPA2_WPA3 -> {
                // Переходный режим - относительно безопасен
                recommendations.add("Рекомендуется полный переход на WPA3")
            }
            
            SecurityType.EAP -> {
                // Корпоративные сети - обычно безопасны
                recommendations.add("Убедитесь в подлинности сертификатов")
            }
            
            SecurityType.UNKNOWN -> {
                threats.add(
                    SecurityThreat(
                        type = ThreatType.UNKNOWN_ENCRYPTION,
                        severity = ThreatLevel.MEDIUM,
                        description = "Неизвестный тип шифрования",
                        networkSsid = network.ssid,
                        networkBssid = network.bssid
                    )
                )
                recommendations.add("Не подключайтесь к сетям с неизвестным шифрованием")
            }
        }
        
        // Анализируем capabilities строку на дополнительные уязвимости
        analyzeCapabilitiesString(network.capabilities, network, threats, vulnerabilities, recommendations)
        
        return EncryptionAnalysis(
            securityType = network.securityType,
            threats = threats,
            vulnerabilities = vulnerabilities,
            recommendations = recommendations,
            encryptionStrength = calculateEncryptionStrength(network.securityType, network.capabilities),
            isSecure = threats.none { it.severity.isHighOrCritical() }
        )
    }
    
    /**
     * Анализировать capabilities строку
     */
    private fun analyzeCapabilitiesString(
        capabilities: String,
        network: WifiScanResult,
        threats: MutableList<SecurityThreat>,
        vulnerabilities: MutableList<String>,
        recommendations: MutableList<String>
    ) {
        // Проверяем на WPS
        if (capabilities.contains("WPS")) {
            threats.add(
                SecurityThreat(
                    type = ThreatType.WPS_VULNERABILITY,
                    severity = ThreatLevel.MEDIUM,
                    description = "WPS включен (уязвим к атакам)",
                    networkSsid = network.ssid,
                    networkBssid = network.bssid
                )
            )
            vulnerabilities.add("WPS может быть взломан за несколько часов")
            recommendations.add("Отключите WPS в настройках роутера")
        }
        
        // Проверяем на TKIP
        if (capabilities.contains("TKIP")) {
            threats.add(
                SecurityThreat(
                    type = ThreatType.WEAK_ENCRYPTION,
                    severity = ThreatLevel.LOW,
                    description = "Используется устаревший TKIP",
                    networkSsid = network.ssid,
                    networkBssid = network.bssid
                )
            )
            vulnerabilities.add("TKIP имеет известные уязвимости")
            recommendations.add("Используйте CCMP (AES) вместо TKIP")
        }
        
        // Проверяем на CCMP (хорошо)
        if (capabilities.contains("CCMP")) {
            // CCMP (AES) - хорошо, не добавляем угроз
        }
        
        // Проверяем на Enterprise аутентификацию
        if (capabilities.contains("EAP")) {
            recommendations.add("Убедитесь в подлинности сертификатов сервера")
        }
        
        // Проверяем на 802.11n/ac/ax
        if (capabilities.contains("RSN")) {
            // RSN (Robust Security Network) - хорошо
        }
    }
    
    /**
     * Рассчитать силу шифрования
     */
    private fun calculateEncryptionStrength(securityType: SecurityType, capabilities: String): EncryptionStrength {
        return when (securityType) {
            SecurityType.OPEN -> EncryptionStrength.NONE
            SecurityType.WEP -> EncryptionStrength.WEAK
            SecurityType.WPA -> {
                if (capabilities.contains("TKIP")) EncryptionStrength.WEAK
                else EncryptionStrength.MEDIUM
            }
            SecurityType.WPA2 -> {
                when {
                    capabilities.contains("CCMP") -> EncryptionStrength.STRONG
                    capabilities.contains("TKIP") -> EncryptionStrength.MEDIUM
                    else -> EncryptionStrength.MEDIUM
                }
            }
            SecurityType.WPA3 -> EncryptionStrength.VERY_STRONG
            SecurityType.WPA2_WPA3 -> EncryptionStrength.STRONG
            SecurityType.EAP -> EncryptionStrength.STRONG
            SecurityType.UNKNOWN -> EncryptionStrength.UNKNOWN
        }
    }
}

/**
 * Результат анализа шифрования
 */
data class EncryptionAnalysis(
    val securityType: SecurityType,
    val threats: List<SecurityThreat>,
    val vulnerabilities: List<String>,
    val recommendations: List<String>,
    val encryptionStrength: EncryptionStrength,
    val isSecure: Boolean
)

/**
 * Сила шифрования
 */
enum class EncryptionStrength {
    NONE,           // Без шифрования
    WEAK,           // Слабое (WEP, WPA с TKIP)
    MEDIUM,         // Среднее (WPA2 с TKIP)
    STRONG,         // Сильное (WPA2 с CCMP)
    VERY_STRONG,    // Очень сильное (WPA3)
    UNKNOWN         // Неизвестно
}
