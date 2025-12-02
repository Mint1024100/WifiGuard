package com.wifiguard.core.security

import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.domain.model.WifiScanResult
import com.wifiguard.core.domain.model.SecurityThreat
import com.wifiguard.core.domain.model.ThreatType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Детектор угроз безопасности Wi-Fi сетей
 */
@Singleton
class ThreatDetector @Inject constructor() {
    
    /**
     * Детектировать дублирующиеся SSID (возможная атака Evil Twin)
     */
    fun detectDuplicateSsid(
        network: WifiScanResult,
        allNetworks: List<WifiScanResult>
    ): SecurityThreat? {
        val duplicateNetworks = allNetworks.filter { 
            it.ssid == network.ssid && it.bssid != network.bssid 
        }
        
        if (duplicateNetworks.isNotEmpty()) {
            return SecurityThreat(
                id = 0, // ID будет установлен при сохранении в БД
                type = ThreatType.DUPLICATE_SSID,
                severity = ThreatLevel.HIGH,
                description = "Обнаружен дублирующийся SSID '${network.ssid}' с разными BSSID. Возможна атака Evil Twin.",
                networkSsid = network.ssid,
                networkBssid = network.bssid,
                additionalInfo = "Найдено ${duplicateNetworks.size + 1} сетей с одинаковым именем"
            )
        }
        
        return null
    }
    
    /**
     * Детектировать подозрительные SSID
     */
    fun detectSuspiciousSsid(network: WifiScanResult): SecurityThreat? {
        val ssid = network.ssid.lowercase()
        
        // Список подозрительных паттернов
        val suspiciousPatterns = listOf(
            "free wifi",
            "free-wifi",
            "free_wifi",
            "public wifi",
            "guest",
            "hotspot",
            "internet",
            "wifi",
            "wireless",
            "open",
            "no password",
            "no-password",
            "no_password",
            "admin",
            "root",
            "test",
            "default",
            "linksys",
            "netgear",
            "dlink",
            "tp-link",
            "asus",
            "belkin",
            "router",
            "modem"
        )
        
        // Проверяем на подозрительные паттерны
        val suspiciousPattern = suspiciousPatterns.find { pattern ->
            ssid.contains(pattern) || ssid == pattern
        }
        
        if (suspiciousPattern != null) {
            return SecurityThreat(
                id = 0, // ID будет установлен при сохранении в БД
                type = ThreatType.SUSPICIOUS_SSID,
                severity = ThreatLevel.MEDIUM,
                description = "Подозрительное имя сети: '${network.ssid}' (содержит '$suspiciousPattern')",
                networkSsid = network.ssid,
                networkBssid = network.bssid,
                additionalInfo = "Избегайте сетей с общими именами"
            )
        }
        
        // Проверяем на слишком длинные или короткие имена
        if (ssid.length > 32) {
            return SecurityThreat(
                id = 0, // ID будет установлен при сохранении в БД
                type = ThreatType.SUSPICIOUS_SSID,
                severity = ThreatLevel.LOW,
                description = "Подозрительно длинное имя сети: '${network.ssid}' (${ssid.length} символов)",
                networkSsid = network.ssid,
                networkBssid = network.bssid
            )
        }
        
        if (ssid.length < 3 && !network.isHidden) {
            return SecurityThreat(
                id = 0, // ID будет установлен при сохранении в БД
                type = ThreatType.SUSPICIOUS_SSID,
                severity = ThreatLevel.LOW,
                description = "Подозрительно короткое имя сети: '${network.ssid}'",
                networkSsid = network.ssid,
                networkBssid = network.bssid
            )
        }
        
        return null
    }
    
    /**
     * Детектировать открытые сети
     */
    fun detectOpenNetwork(network: WifiScanResult): SecurityThreat? {
        if (network.securityType == SecurityType.OPEN) {
            return SecurityThreat(
                id = 0, // ID будет установлен при сохранении в БД
                type = ThreatType.OPEN_NETWORK,
                severity = ThreatLevel.CRITICAL,
                description = "Открытая сеть без шифрования: '${network.ssid}'",
                networkSsid = network.ssid,
                networkBssid = network.bssid,
                additionalInfo = "Все данные передаются в открытом виде"
            )
        }
        
        return null
    }
    
    /**
     * Детектировать слабое шифрование
     */
    fun detectWeakEncryption(network: WifiScanResult): SecurityThreat? {
        when (network.securityType) {
            SecurityType.WEP -> {
                return SecurityThreat(
                    id = 0, // ID будет установлен при сохранении в БД
                    type = ThreatType.WEAK_ENCRYPTION,
                    severity = ThreatLevel.HIGH,
                    description = "Слабое шифрование WEP: '${network.ssid}'",
                    networkSsid = network.ssid,
                    networkBssid = network.bssid,
                    additionalInfo = "WEP легко взламывается за несколько минут"
                )
            }
            SecurityType.WPA -> {
                return SecurityThreat(
                    id = 0, // ID будет установлен при сохранении в БД
                    type = ThreatType.WEAK_ENCRYPTION,
                    severity = ThreatLevel.MEDIUM,
                    description = "Устаревшее шифрование WPA: '${network.ssid}'",
                    networkSsid = network.ssid,
                    networkBssid = network.bssid,
                    additionalInfo = "WPA устарел и имеет известные уязвимости"
                )
            }
            else -> return null
        }
    }
    
    /**
     * Детектировать глобальные угрозы
     */
    fun detectGlobalThreats(networks: List<WifiScanResult>): List<SecurityThreat> {
        val threats = mutableListOf<SecurityThreat>()
        
        // Группируем сети по SSID
        val networksBySsid = networks.groupBy { it.ssid }
        
        // Проверяем на множественные дублирующиеся SSID
        networksBySsid.forEach { (ssid, networkList) ->
            if (networkList.size > 2) {
                threats.add(
                    SecurityThreat(
                        id = 0, // ID будет установлен при сохранении в БД
                        type = ThreatType.MULTIPLE_DUPLICATES,
                        severity = ThreatLevel.HIGH,
                        description = "Обнаружено ${networkList.size} сетей с одинаковым SSID: '$ssid'",
                        networkSsid = ssid,
                        networkBssid = "multiple",
                        additionalInfo = "Высокая вероятность атаки Evil Twin"
                    )
                )
            }
        }
        
        // Проверяем на подозрительную активность (много открытых сетей)
        val openNetworks = networks.count { it.securityType == SecurityType.OPEN }
        if (openNetworks > 3) {
            threats.add(
                SecurityThreat(
                    id = 0, // ID будет установлен при сохранении в БД
                    type = ThreatType.SUSPICIOUS_ACTIVITY,
                    severity = ThreatLevel.MEDIUM,
                    description = "Обнаружено $openNetworks открытых сетей в зоне",
                    networkSsid = "multiple",
                    networkBssid = "multiple",
                    additionalInfo = "Возможна попытка создания поддельных точек доступа"
                )
            )
        }
        
        // Проверяем на подозрительные BSSID (MAC-адреса)
        val suspiciousBssids = detectSuspiciousBssids(networks)
        threats.addAll(suspiciousBssids)
        
        return threats
    }
    
    /**
     * Детектировать подозрительные BSSID
     */
    private fun detectSuspiciousBssids(networks: List<WifiScanResult>): List<SecurityThreat> {
        val threats = mutableListOf<SecurityThreat>()
        
        // Проверяем на последовательные MAC-адреса
        val bssids = networks.map { it.bssid }.filter { it != "unknown" }
        val sortedBssids = bssids.sorted()
        
        for (i in 0 until sortedBssids.size - 1) {
            val current = sortedBssids[i]
            val next = sortedBssids[i + 1]
            
            if (isSequentialMac(current, next)) {
                threats.add(
                    SecurityThreat(
                        id = 0, // ID будет установлен при сохранении в БД
                        type = ThreatType.SUSPICIOUS_BSSID,
                        severity = ThreatLevel.MEDIUM,
                        description = "Обнаружены последовательные MAC-адреса: $current и $next",
                        networkSsid = "multiple",
                        networkBssid = "multiple",
                        additionalInfo = "Возможна подделка MAC-адресов"
                    )
                )
            }
        }
        
        return threats
    }
    
    /**
     * Проверить, являются ли MAC-адреса последовательными
     */
    private fun isSequentialMac(mac1: String, mac2: String): Boolean {
        try {
            val mac1Bytes = mac1.split(":").map { it.toInt(16) }
            val mac2Bytes = mac2.split(":").map { it.toInt(16) }
            
            if (mac1Bytes.size != 6 || mac2Bytes.size != 6) return false
            
            // Проверяем, отличаются ли только последние 2 байта
            val first4Bytes1 = mac1Bytes.take(4)
            val first4Bytes2 = mac2Bytes.take(4)
            
            if (first4Bytes1 != first4Bytes2) return false
            
            val last2Bytes1 = mac1Bytes.drop(4)
            val last2Bytes2 = mac2Bytes.drop(4)
            
            val diff = (last2Bytes2[0] * 256 + last2Bytes2[1]) - (last2Bytes1[0] * 256 + last2Bytes1[1])
            
            return diff in 1..10 // Разница не более 10
        } catch (e: Exception) {
            return false
        }
    }
}