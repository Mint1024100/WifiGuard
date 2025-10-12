package com.wifiguard.core.security

import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.domain.model.WifiScanResult
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Отчет о безопасности Wi-Fi сетей
 */
@Parcelize
data class SecurityReport(
    val timestamp: Long,
    val totalNetworks: Int,
    val safeNetworks: Int,
    val lowRiskNetworks: Int,
    val mediumRiskNetworks: Int,
    val highRiskNetworks: Int,
    val criticalRiskNetworks: Int,
    val threats: List<SecurityThreat>,
    val networkAnalysis: List<NetworkSecurityAnalysis>,
    val overallRiskLevel: ThreatLevel,
    val recommendations: List<String>
) : Parcelable {
    
    /**
     * Получить процент безопасных сетей
     */
    fun getSafeNetworksPercentage(): Int {
        return if (totalNetworks > 0) {
            (safeNetworks * 100) / totalNetworks
        } else {
            0
        }
    }
    
    /**
     * Получить процент небезопасных сетей
     */
    fun getUnsafeNetworksPercentage(): Int {
        return if (totalNetworks > 0) {
            ((highRiskNetworks + criticalRiskNetworks) * 100) / totalNetworks
        } else {
            0
        }
    }
    
    /**
     * Получить общую оценку безопасности
     */
    fun getOverallSecurityScore(): Int {
        return when (overallRiskLevel) {
            ThreatLevel.SAFE -> 100
            ThreatLevel.LOW -> 80
            ThreatLevel.MEDIUM -> 60
            ThreatLevel.HIGH -> 40
            ThreatLevel.CRITICAL -> 20
            ThreatLevel.UNKNOWN -> 50
        }
    }
    
    /**
     * Проверить, есть ли критические угрозы
     */
    fun hasCriticalThreats(): Boolean {
        return criticalRiskNetworks > 0 || threats.any { it.severity == ThreatLevel.CRITICAL }
    }
    
    /**
     * Получить количество угроз по типам
     */
    fun getThreatsByType(): Map<ThreatType, Int> {
        return threats.groupingBy { it.type }.eachCount()
    }
    
    /**
     * Получить наиболее опасные сети
     */
    fun getMostDangerousNetworks(): List<NetworkSecurityAnalysis> {
        return networkAnalysis
            .filter { it.threatLevel.isHighOrCritical() }
            .sortedByDescending { it.threatLevel.getNumericValue() }
    }
}

/**
 * Анализ безопасности отдельной сети
 */
@Parcelize
data class NetworkSecurityAnalysis(
    val network: WifiScanResult,
    val threatLevel: ThreatLevel,
    val threats: List<SecurityThreat>,
    val encryptionAnalysis: EncryptionAnalysis,
    val securityScore: Int
) : Parcelable {
    
    /**
     * Проверить, является ли сеть безопасной
     */
    fun isSafe(): Boolean {
        return threatLevel.isSafe() && threats.isEmpty()
    }
    
    /**
     * Получить описание уровня безопасности
     */
    fun getSecurityDescription(): String {
        return when (threatLevel) {
            ThreatLevel.SAFE -> "Безопасная сеть"
            ThreatLevel.LOW -> "Низкий риск"
            ThreatLevel.MEDIUM -> "Средний риск"
            ThreatLevel.HIGH -> "Высокий риск"
            ThreatLevel.CRITICAL -> "Критический риск"
            ThreatLevel.UNKNOWN -> "Неизвестный уровень"
        }
    }
}

/**
 * Угроза безопасности
 */
@Parcelize
data class SecurityThreat(
    val type: ThreatType,
    val severity: ThreatLevel,
    val description: String,
    val networkSsid: String,
    val networkBssid: String,
    val additionalInfo: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {
    
    /**
     * Получить краткое описание угрозы
     */
    fun getShortDescription(): String {
        return when (type) {
            ThreatType.OPEN_NETWORK -> "Открытая сеть"
            ThreatType.WEAK_ENCRYPTION -> "Слабое шифрование"
            ThreatType.DUPLICATE_SSID -> "Дублирующийся SSID"
            ThreatType.SUSPICIOUS_SSID -> "Подозрительное имя"
            ThreatType.WPS_VULNERABILITY -> "Уязвимость WPS"
            ThreatType.WEAK_SIGNAL -> "Слабый сигнал"
            ThreatType.UNKNOWN_ENCRYPTION -> "Неизвестное шифрование"
            ThreatType.MULTIPLE_DUPLICATES -> "Множественные дубликаты"
            ThreatType.SUSPICIOUS_ACTIVITY -> "Подозрительная активность"
            ThreatType.SUSPICIOUS_BSSID -> "Подозрительный MAC-адрес"
        }
    }
}

/**
 * Типы угроз безопасности
 */
@Parcelize
enum class ThreatType : Parcelable {
    OPEN_NETWORK,           // Открытая сеть
    WEAK_ENCRYPTION,        // Слабое шифрование
    DUPLICATE_SSID,         // Дублирующийся SSID
    SUSPICIOUS_SSID,        // Подозрительное имя сети
    WPS_VULNERABILITY,      // Уязвимость WPS
    WEAK_SIGNAL,            // Слабый сигнал
    UNKNOWN_ENCRYPTION,     // Неизвестное шифрование
    MULTIPLE_DUPLICATES,    // Множественные дубликаты
    SUSPICIOUS_ACTIVITY,    // Подозрительная активность
    SUSPICIOUS_BSSID        // Подозрительный MAC-адрес
}
