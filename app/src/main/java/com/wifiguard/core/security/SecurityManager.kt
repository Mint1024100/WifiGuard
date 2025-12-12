package com.wifiguard.core.security

import android.util.Log
import com.wifiguard.core.common.Constants
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.domain.model.ThreatType
import com.wifiguard.feature.scanner.domain.model.WifiInfo
import com.wifiguard.feature.scanner.domain.model.EncryptionType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityManager @Inject constructor(
    private val aesEncryption: AesEncryption
) {
    
    companion object {
        private const val TAG = "SecurityManager"
        private const val SUSPICIOUS_MAC_THRESHOLD = 3 // Количество подозрительных MAC адресов
        private const val SIGNAL_ANOMALY_THRESHOLD = 20 // dBm разница для аномалии
        private const val TIMING_ATTACK_THRESHOLD_MS = 1000L // Максимальное время анализа
        private const val MAX_NETWORK_HISTORY = 100 // Максимальное количество записей истории
    }
    
    // Кэш для отслеживания сетей и их характеристик
    private val networkHistory = ConcurrentHashMap<String, NetworkHistory>()
    private val suspiciousMacAddresses = ConcurrentHashMap<String, AtomicLong>()
    private val analysisStartTime = AtomicLong(0L)
    
    fun analyzeNetworkSecurity(wifiInfo: WifiInfo): SecurityAnalysisResult {
        // Защита от timing attacks
        val startTime = System.currentTimeMillis()
        analysisStartTime.set(startTime)
        
        return try {
            val threats = mutableListOf<ThreatType>()
            var threatLevel = ThreatLevel.LOW
            
            // 1. Анализ типа шифрования
            analyzeEncryptionType(wifiInfo, threats, threatLevel).let { newThreatLevel ->
                threatLevel = newThreatLevel
            }
            
            // 2. Проверка MAC адреса на подозрительность
            if (isSuspiciousMacAddress(wifiInfo.bssid)) {
                threats.add(ThreatType.SUSPICIOUS_BSSID)
                if (threatLevel == ThreatLevel.LOW) threatLevel = ThreatLevel.MEDIUM
            }
            
            // 3. Анализ аномалий сигнала
            if (detectSignalAnomaly(wifiInfo)) {
                threats.add(ThreatType.MULTIPLE_DUPLICATES) // Using similar type for signal anomaly
                if (threatLevel == ThreatLevel.LOW) threatLevel = ThreatLevel.MEDIUM
            }
            
            // 4. Проверка подозрительных имен сетей
            if (isSuspiciousNetworkName(wifiInfo.ssid)) {
                threats.add(ThreatType.SUSPICIOUS_SSID)
                if (threatLevel == ThreatLevel.LOW) threatLevel = ThreatLevel.MEDIUM
            }
            
            // 5. Проверка на Evil Twin атаки
            if (detectEvilTwinAttack(wifiInfo)) {
                threats.add(ThreatType.DUPLICATE_SSID) // Using similar type for evil twin
                threatLevel = ThreatLevel.HIGH
            }
            
            // 6. Обновляем историю сети
            updateNetworkHistory(wifiInfo)
            
            val result = SecurityAnalysisResult(
                networkId = wifiInfo.bssid,
                threatLevel = threatLevel,
                threats = threats,
                recommendations = generateRecommendations(threats),
                analysisTimestamp = System.currentTimeMillis()
            )
            
            Log.d(TAG, "Security analysis completed for ${wifiInfo.ssid} in ${System.currentTimeMillis() - startTime}ms")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при анализе безопасности: ${e.message}", e)
            SecurityAnalysisResult(
                networkId = wifiInfo.bssid,
                threatLevel = ThreatLevel.HIGH,
                threats = listOf(ThreatType.UNKNOWN_THREAT),
                recommendations = listOf("Анализ безопасности не удался - считать высокий риск"),
                analysisTimestamp = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Анализирует тип шифрования и определяет уровень риска
     */
    private fun analyzeEncryptionType(wifiInfo: WifiInfo, threats: MutableList<ThreatType>, currentRisk: ThreatLevel): ThreatLevel {
        var threatLevel = currentRisk
        
        when (wifiInfo.encryptionType) {
            EncryptionType.NONE -> {
                threats.add(ThreatType.OPEN_NETWORK)
                threatLevel = ThreatLevel.HIGH
            }
            EncryptionType.WEP -> {
                threats.add(ThreatType.WEAK_ENCRYPTION)
                threatLevel = ThreatLevel.HIGH
            }
            EncryptionType.WPA -> {
                threats.add(ThreatType.OUTDATED_PROTOCOL)
                threatLevel = ThreatLevel.MEDIUM
            }
            EncryptionType.WPA2 -> {
                // WPA2 приемлем, но проверяем другие проблемы
                if (wifiInfo.signalStrength < Constants.WEAK_SIGNAL_THRESHOLD) {
                    threats.add(ThreatType.WEAK_SIGNAL)
                }
            }
            EncryptionType.WPA3 -> {
                // WPA3 безопасен, проверяем только сигнал
                if (wifiInfo.signalStrength < Constants.WEAK_SIGNAL_THRESHOLD) {
                    threats.add(ThreatType.WEAK_SIGNAL)
                }
            }
            EncryptionType.UNKNOWN -> {
                threats.add(ThreatType.UNKNOWN_ENCRYPTION)
                threatLevel = ThreatLevel.MEDIUM
            }
            EncryptionType.WPS -> {
                threats.add(ThreatType.OUTDATED_PROTOCOL)
                threatLevel = ThreatLevel.MEDIUM
            }
        }
        
        return threatLevel
    }
    
    /**
     * Проверяет MAC адрес на подозрительность
     */
    private fun isSuspiciousMacAddress(bssid: String): Boolean {
        // Проверяем известные подозрительные MAC адреса
        val suspiciousMacs = listOf(
            "00:00:00:00:00:00", // Нулевой MAC
            "FF:FF:FF:FF:FF:FF", // Broadcast MAC
            "02:00:00:00:00:00"  // Локально администрируемый
        )
        
        if (suspiciousMacs.contains(bssid.uppercase())) {
            return true
        }
        
        // Проверяем локально администрируемые MAC адреса (второй бит = 1)
        val macBytes = bssid.split(":").map { it.toInt(16) }
        if (macBytes.isNotEmpty() && (macBytes[0] and 0x02) != 0) {
            suspiciousMacAddresses.computeIfAbsent(bssid) { AtomicLong(0) }.incrementAndGet()
            return true
        }
        
        // Проверяем количество подозрительных MAC адресов
        return suspiciousMacAddresses.size > SUSPICIOUS_MAC_THRESHOLD
    }
    
    /**
     * Обнаруживает аномалии в силе сигнала
     */
    private fun detectSignalAnomaly(wifiInfo: WifiInfo): Boolean {
        val history = networkHistory[wifiInfo.bssid]
        
        if (history == null) {
            // Первое обнаружение сети
            return false
        }
        
        val avgSignal = history.getAverageSignalStrength()
        val signalDifference = kotlin.math.abs(wifiInfo.signalStrength - avgSignal)
        
        // Аномалия если разница больше порога
        return signalDifference > SIGNAL_ANOMALY_THRESHOLD
    }
    
    /**
     * Обнаруживает Evil Twin атаки
     */
    private fun detectEvilTwinAttack(wifiInfo: WifiInfo): Boolean {
        val history = networkHistory[wifiInfo.bssid]
        
        if (history == null) {
            return false
        }
        
        // Проверяем на необычно сильный сигнал (возможный Evil Twin)
        if (wifiInfo.signalStrength > -30) {
            return true
        }
        
        // Проверяем на резкие изменения в силе сигнала
        val avgSignal = history.getAverageSignalStrength()
        val signalDifference = wifiInfo.signalStrength - avgSignal
        
        // Если сигнал стал значительно сильнее, это может быть Evil Twin
        return signalDifference > 15 // 15 dBm разница
    }
    
    /**
     * Обновляет историю сети
     */
    private fun updateNetworkHistory(wifiInfo: WifiInfo) {
        val history = networkHistory.computeIfAbsent(wifiInfo.bssid) { NetworkHistory() }
        history.addSignalReading(wifiInfo.signalStrength)
        
        // Ограничиваем размер истории
        if (networkHistory.size > MAX_NETWORK_HISTORY) {
            val oldestKey = networkHistory.keys.first()
            networkHistory.remove(oldestKey)
        }
    }
    
    private fun isSuspiciousNetworkName(ssid: String): Boolean {
        val suspiciousPatterns = listOf(
            "free", "wifi", "internet", "network", "connection",
            "hotspot", "guest", "public", "open", "test",
            "admin", "setup", "config", "router", "default"
        )
        
        val lowerSsid = ssid.lowercase()
        return suspiciousPatterns.any { lowerSsid.contains(it) } ||
                ssid.length < 3 ||
                ssid.all { it.isDigit() } ||
                ssid.matches(Regex(".*\\d{4,}.*")) // Содержит 4+ цифр подряд
    }
    
    private fun generateRecommendations(threats: List<ThreatType>): List<String> {
        val recommendations = mutableListOf<String>()
        
        threats.forEach { threat ->
            when (threat) {
                ThreatType.OPEN_NETWORK -> {
                    recommendations.add("Избегайте подключения к открытым сетям без защиты")
                    recommendations.add("Используйте VPN при подключении к открытым сетям")
                }
                ThreatType.WEAK_ENCRYPTION -> {
                    recommendations.add("Не подключайтесь к сетям с устаревшим шифрованием WEP")
                }
                ThreatType.OUTDATED_PROTOCOL -> {
                    recommendations.add("По возможности используйте сети с WPA2 или WPA3")
                }
                ThreatType.WEAK_SIGNAL -> {
                    recommendations.add("Слабый сигнал может указывать на удаленность или проблемы")
                }
                ThreatType.SUSPICIOUS_SSID -> {
                    recommendations.add("Будьте осторожны с сетями с подозрительными именами")
                }
                ThreatType.MULTIPLE_DUPLICATES -> {
                    recommendations.add("Необычно сильный сигнал может указывать на поддельную точку доступа")
                }
                ThreatType.UNKNOWN_ENCRYPTION -> {
                    recommendations.add("Проверьте тип безопасности сети перед подключением")
                }
                ThreatType.SUSPICIOUS_BSSID -> {
                    recommendations.add("Подозрительный MAC адрес - избегайте подключения")
                }
                ThreatType.DUPLICATE_SSID -> {
                    recommendations.add("Аномалия в силе сигнала - возможна поддельная точка доступа")
                }
                else -> {
                    recommendations.add("Обнаружена угроза безопасности")
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
    val threatLevel: ThreatLevel,
    val threats: List<ThreatType>,
    val recommendations: List<String>,
    val analysisTimestamp: Long
)