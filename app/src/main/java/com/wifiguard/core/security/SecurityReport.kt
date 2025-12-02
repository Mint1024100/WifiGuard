package com.wifiguard.core.security

import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.domain.model.ThreatType
import com.wifiguard.core.domain.model.WifiScanResult
import com.wifiguard.core.domain.model.SecurityThreat
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

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