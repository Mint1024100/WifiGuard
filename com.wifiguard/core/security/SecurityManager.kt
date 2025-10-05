package com.wifiguard.core.security

import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.util.Log
import com.wifiguard.core.common.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Центральный менеджер безопасности Wi-Fi сетей.
 * Анализирует уровень безопасности, выявляет потенциальные угрозы и классифицирует риски.
 * 
 * Основные функции:
 * - Анализ типов шифрования Wi-Fi сетей
 * - Оценка силы сигнала и стабильности соединения
 * - Выявление подозрительных и вредоносных сетей
 * - Расчет общего рейтинга безопасности
 * - Предоставление рекомендаций по безопасности
 */
@Singleton
class SecurityManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val aesEncryption: AesEncryption
) {
    
    companion object {
        private const val TAG = Constants.LogTags.SECURITY_ANALYZER
        
        // Паттерны для обнаружения подозрительных имен сетей
        private val SUSPICIOUS_SSID_PATTERNS = listOf(
            Pattern.compile(".*free.*wifi.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*public.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*guest.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*open.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*hack.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*test.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*temp.*", Pattern.CASE_INSENSITIVE)
        )
        
        // Известные вредоносные SSID
        private val MALICIOUS_SSIDS = setOf(
            "FREE_WIFI_HACKER",
            "VIRUS_NETWORK",
            "HACKER_NETWORK",
            "MALWARE_AP"
        )
    }
    
    /**
     * Результат анализа безопасности Wi-Fi сети
     */
    data class SecurityAnalysis(
        val ssid: String,
        val bssid: String,
        val securityType: String,
        val encryptionLevel: EncryptionLevel,
        val threatLevel: ThreatLevel,
        val securityScore: Int, // 0-100
        val signalStrength: Int,
        val isOpenNetwork: Boolean,
        val isSuspicious: Boolean,
        val isMalicious: Boolean,
        val risks: List<SecurityRisk>,
        val recommendations: List<String>
    )
    
    /**
     * Уровни шифрования
     */
    enum class EncryptionLevel(val displayName: String, val score: Int) {
        NONE("Без шифрования", 0),
        WEP("WEP (устаревший)", 20),
        WPA("WPA (слабый)", 40),
        WPA2("WPA2 (хороший)", 70),
        WPA3("WPA3 (отличный)", 90),
        WPA2_WPA3("WPA2/WPA3 (отличный)", 85),
        UNKNOWN("Неизвестный", 10)
    }
    
    /**
     * Уровни угроз
     */
    enum class ThreatLevel(val displayName: String, val color: Long) {
        NONE("Безопасно", 0xFF4CAF50),
        LOW("Низкий риск", 0xFF8BC34A),
        MEDIUM("Средний риск", 0xFFFFEB3B),
        HIGH("Высокий риск", 0xFFFF9800),
        CRITICAL("Критический риск", 0xFFE53935)
    }
    
    /**
     * Типы рисков безопасности
     */
    enum class SecurityRisk(val description: String, val severity: Int) {
        OPEN_NETWORK("Открытая сеть без пароля", 80),
        WEAK_ENCRYPTION("Слабое шифрование", 60),
        SUSPICIOUS_NAME("Подозрительное имя сети", 70),
        WEAK_SIGNAL("Слабый сигнал", 30),
        KNOWN_MALICIOUS("Известная вредоносная сеть", 100),
        DUPLICATE_SSID("Дублированное имя сети", 50),
        UNUSUAL_FREQUENCY("Необычная частота", 40)
    }
    
    /**
     * Выполняет комплексный анализ безопасности Wi-Fi сети
     */
    suspend fun analyzeNetworkSecurity(scanResult: ScanResult): SecurityAnalysis = withContext(Dispatchers.Default) {
        try {
            Log.d(TAG, "Анализ безопасности сети: ${scanResult.SSID}")
            
            val securityType = determineSecurityType(scanResult.capabilities)
            val encryptionLevel = mapSecurityToEncryption(securityType)
            val isOpen = encryptionLevel == EncryptionLevel.NONE
            val isSuspicious = isSuspiciousNetwork(scanResult.SSID)
            val isMalicious = isMaliciousNetwork(scanResult.SSID)
            val risks = identifyRisks(scanResult, encryptionLevel, isSuspicious, isMalicious)
            val securityScore = calculateSecurityScore(encryptionLevel, scanResult.level, risks)
            val threatLevel = determineThreatLevel(securityScore, risks)
            val recommendations = generateRecommendations(risks, encryptionLevel)
            
            SecurityAnalysis(
                ssid = scanResult.SSID ?: "Скрытая сеть",
                bssid = scanResult.BSSID,
                securityType = securityType,
                encryptionLevel = encryptionLevel,
                threatLevel = threatLevel,
                securityScore = securityScore,
                signalStrength = WifiConfiguration.calculateSignalLevel(scanResult.level, 5),
                isOpenNetwork = isOpen,
                isSuspicious = isSuspicious,
                isMalicious = isMalicious,
                risks = risks,
                recommendations = recommendations
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при анализе безопасности", e)
            createErrorAnalysis(scanResult)
        }
    }
    
    /**
     * Определяет тип безопасности по capabilities строке
     */
    private fun determineSecurityType(capabilities: String): String {
        return when {
            capabilities.contains("WPA3") -> Constants.WifiSecurity.WPA3
            capabilities.contains("WPA2") && capabilities.contains("WPA3") -> Constants.WifiSecurity.WPA2_WPA3
            capabilities.contains("WPA2") -> Constants.WifiSecurity.WPA2
            capabilities.contains("WPA") -> Constants.WifiSecurity.WPA
            capabilities.contains("WEP") -> Constants.WifiSecurity.WEP
            capabilities.contains("[ESS]") && !capabilities.contains("WPA") && !capabilities.contains("WEP") -> Constants.WifiSecurity.NONE
            else -> Constants.WifiSecurity.UNKNOWN
        }
    }
    
    /**
     * Преобразует тип безопасности в уровень шифрования
     */
    private fun mapSecurityToEncryption(securityType: String): EncryptionLevel {
        return when (securityType) {
            Constants.WifiSecurity.NONE -> EncryptionLevel.NONE
            Constants.WifiSecurity.WEP -> EncryptionLevel.WEP
            Constants.WifiSecurity.WPA -> EncryptionLevel.WPA
            Constants.WifiSecurity.WPA2 -> EncryptionLevel.WPA2
            Constants.WifiSecurity.WPA3 -> EncryptionLevel.WPA3
            Constants.WifiSecurity.WPA2_WPA3 -> EncryptionLevel.WPA2_WPA3
            else -> EncryptionLevel.UNKNOWN
        }
    }
    
    /**
     * Проверяет, является ли сеть подозрительной по имени
     */
    private fun isSuspiciousNetwork(ssid: String?): Boolean {
        if (ssid.isNullOrBlank()) return true
        
        return SUSPICIOUS_SSID_PATTERNS.any { pattern ->
            pattern.matcher(ssid).matches()
        }
    }
    
    /**
     * Проверяет, является ли сеть известной вредоносной
     */
    private fun isMaliciousNetwork(ssid: String?): Boolean {
        if (ssid.isNullOrBlank()) return false
        return MALICIOUS_SSIDS.contains(ssid.uppercase())
    }
    
    /**
     * Выявляет все риски безопасности
     */
    private fun identifyRisks(
        scanResult: ScanResult,
        encryptionLevel: EncryptionLevel,
        isSuspicious: Boolean,
        isMalicious: Boolean
    ): List<SecurityRisk> {
        val risks = mutableListOf<SecurityRisk>()
        
        // Проверка открытой сети
        if (encryptionLevel == EncryptionLevel.NONE) {
            risks.add(SecurityRisk.OPEN_NETWORK)
        }
        
        // Проверка слабого шифрования
        if (encryptionLevel == EncryptionLevel.WEP || encryptionLevel == EncryptionLevel.WPA) {
            risks.add(SecurityRisk.WEAK_ENCRYPTION)
        }
        
        // Проверка подозрительного имени
        if (isSuspicious) {
            risks.add(SecurityRisk.SUSPICIOUS_NAME)
        }
        
        // Проверка вредоносной сети
        if (isMalicious) {
            risks.add(SecurityRisk.KNOWN_MALICIOUS)
        }
        
        // Проверка силы сигнала
        if (scanResult.level < -80) {
            risks.add(SecurityRisk.WEAK_SIGNAL)
        }
        
        return risks
    }
    
    /**
     * Рассчитывает общий балл безопасности (0-100)
     */
    private fun calculateSecurityScore(
        encryptionLevel: EncryptionLevel,
        signalLevel: Int,
        risks: List<SecurityRisk>
    ): Int {
        var score = encryptionLevel.score
        
        // Добавляем баллы за силу сигнала
        val signalScore = when {
            signalLevel > -50 -> 20
            signalLevel > -60 -> 15
            signalLevel > -70 -> 10
            signalLevel > -80 -> 5
            else -> 0
        }
        score += signalScore
        
        // Вычитаем баллы за каждый риск
        risks.forEach { risk ->
            score -= (risk.severity * 0.3).toInt()
        }
        
        return score.coerceIn(0, 100)
    }
    
    /**
     * Определяет уровень угрозы на основе балла безопасности
     */
    private fun determineThreatLevel(securityScore: Int, risks: List<SecurityRisk>): ThreatLevel {
        // Критический уровень для известных вредоносных сетей
        if (risks.contains(SecurityRisk.KNOWN_MALICIOUS)) {
            return ThreatLevel.CRITICAL
        }
        
        return when {
            securityScore >= Constants.SECURITY_LEVEL_HIGH_THRESHOLD -> ThreatLevel.NONE
            securityScore >= Constants.SECURITY_LEVEL_MEDIUM_THRESHOLD -> ThreatLevel.LOW
            securityScore >= Constants.SECURITY_LEVEL_LOW_THRESHOLD -> ThreatLevel.MEDIUM
            securityScore >= 20 -> ThreatLevel.HIGH
            else -> ThreatLevel.CRITICAL
        }
    }
    
    /**
     * Генерирует рекомендации по безопасности
     */
    private fun generateRecommendations(
        risks: List<SecurityRisk>,
        encryptionLevel: EncryptionLevel
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        risks.forEach { risk ->
            when (risk) {
                SecurityRisk.OPEN_NETWORK -> {
                    recommendations.add("Избегайте передачи конфиденциальных данных через открытые сети")
                    recommendations.add("Используйте VPN для защиты трафика")
                }
                SecurityRisk.WEAK_ENCRYPTION -> {
                    recommendations.add("Сеть использует устаревшее шифрование")
                    recommendations.add("Рекомендуется найти сеть с WPA2 или WPA3")
                }
                SecurityRisk.SUSPICIOUS_NAME -> {
                    recommendations.add("Будьте осторожны с сетями, имеющими подозрительные названия")
                }
                SecurityRisk.KNOWN_MALICIOUS -> {
                    recommendations.add("⚠️ НЕ ПОДКЛЮЧАЙТЕСЬ к этой сети!")
                    recommendations.add("Сеть помечена как вредоносная")
                }
                SecurityRisk.WEAK_SIGNAL -> {
                    recommendations.add("Слабый сигнал может привести к нестабильному соединению")
                }
                else -> {}
            }
        }
        
        // Общие рекомендации
        if (encryptionLevel == EncryptionLevel.WPA3) {
            recommendations.add("✅ Отличная защита! Сеть использует современное шифрование")
        }
        
        return recommendations.distinct()
    }
    
    /**
     * Создает анализ с ошибкой при неудачном разборе
     */
    private fun createErrorAnalysis(scanResult: ScanResult): SecurityAnalysis {
        return SecurityAnalysis(
            ssid = scanResult.SSID ?: "Неизвестная сеть",
            bssid = scanResult.BSSID,
            securityType = Constants.WifiSecurity.UNKNOWN,
            encryptionLevel = EncryptionLevel.UNKNOWN,
            threatLevel = ThreatLevel.MEDIUM,
            securityScore = 0,
            signalStrength = 0,
            isOpenNetwork = false,
            isSuspicious = true,
            isMalicious = false,
            risks = listOf(SecurityRisk.UNUSUAL_FREQUENCY),
            recommendations = listOf("Не удалось проанализировать безопасность сети")
        )
    }
}