package com.wifiguard.core.security

import android.util.Log
import com.wifiguard.core.common.BssidValidator
import com.wifiguard.core.domain.model.Freshness
import com.wifiguard.core.domain.model.ScanMetadata
import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.domain.model.WifiScanResult
import com.wifiguard.core.domain.model.SecurityThreat
import com.wifiguard.core.domain.model.ThreatType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    
    companion object {
        private const val TAG = "SecurityAnalyzer"
    }

    /**
     * Анализировать безопасность списка сетей с учетом метаданных сканирования
     */
    suspend fun analyzeNetworks(
        scanResults: List<WifiScanResult>,
        metadata: ScanMetadata? = null
    ): SecurityReport = withContext(Dispatchers.Default) {
        // ВАЖНО ДЛЯ БЛОКА "СТАТИСТИКА" (AnalysisScreen):
        // `scanResults` часто приходит из истории (wifi_scans) и может содержать много записей на одну и ту же сеть.
        // Для корректной статистики нужен "срез" по сетям: 1 запись на BSSID (или SSID, если BSSID неизвестен).
        val snapshotNetworks = buildSnapshotNetworks(scanResults)

        val threats = mutableListOf<SecurityThreat>()
        val networkAnalysis = mutableListOf<NetworkSecurityAnalysis>()

        // Строим индекс для O(1) детекции дубликатов SSID по актуальному срезу
        threatDetector.buildSsidIndex(snapshotNetworks)

        // Анализируем каждую сеть
        snapshotNetworks.forEach { network ->
            val analysis = analyzeNetwork(network, snapshotNetworks, metadata)
            networkAnalysis.add(analysis)

            // Добавляем угрозы
            if (analysis.threatLevel.isHighOrCritical()) {
                threats.addAll(analysis.threats)
            }
        }

        // Детектируем глобальные угрозы
        val globalThreats = threatDetector.detectGlobalThreats(snapshotNetworks)
        threats.addAll(globalThreats)

        return@withContext SecurityReport(
            timestamp = System.currentTimeMillis(),
            totalNetworks = snapshotNetworks.size,
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
     * Построить актуальный "срез" сетей: один элемент на сеть.
     *
     * Почему это важно:
     * - В БД wifi_scans хранится история, и один BSSID может встречаться много раз.
     * - Блок "Статистика" должен показывать количество СЕТЕЙ, а не количество ЗАПИСЕЙ истории.
     *
     * Стратегия:
     * - Если BSSID валидный — группируем по BSSID (нижний регистр).
     * - Иначе группируем по SSID (нижний регистр), чтобы скрытые/неизвестные BSSID не раздували статистику.
     * - Сохраняем первую встреченную запись (ожидается, что вход уже отсортирован по timestamp DESC).
     */
    private fun buildSnapshotNetworks(scanResults: List<WifiScanResult>): List<WifiScanResult> {
        if (scanResults.isEmpty()) return emptyList()

        val unique = LinkedHashMap<String, WifiScanResult>(scanResults.size)

        scanResults.forEachIndexed { index, scan ->
            val key = when {
                // Учитываем пару (BSSID, SSID): в норме BSSID уникален, но на некоторых устройствах/прошивках
                // возможны некорректные/общие BSSID для разных SSID. Для статистики корректнее разделять по SSID.
                BssidValidator.isValidForStorage(scan.bssid) -> "bssid:${scan.bssid.lowercase()}|ssid:${scan.ssid.lowercase()}"
                scan.ssid.isNotBlank() -> "ssid:${scan.ssid.lowercase()}"
                else -> "unknown:$index"
            }
            unique.putIfAbsent(key, scan)
        }

        return unique.values.toList()
    }
    
    /**
     * Анализировать отдельную сеть с учетом метаданных сканирования
     */
    private fun analyzeNetwork(
        network: WifiScanResult,
        allNetworks: List<WifiScanResult>,
        metadata: ScanMetadata? = null
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
        
        // Определяем общий уровень угрозы с учетом метаданных
        val threatLevel = determineThreatLevel(threats, network.securityType, metadata)
        
        return NetworkSecurityAnalysis(
            network = network,
            threatLevel = threatLevel,
            threats = threats,
            encryptionAnalysis = encryptionAnalysis,
            securityScore = calculateSecurityScore(network, threats)
        )
    }
    
    /**
     * Определить уровень угрозы с учетом метаданных сканирования (FAIL-SAFE подход)
     * 
     * КРИТИЧЕСКИ ВАЖНО: Используем fail-safe подход - при неопределенности считаем небезопасным
     */
    private fun determineThreatLevel(
        threats: List<SecurityThreat>,
        securityType: SecurityType,
        metadata: ScanMetadata? = null
    ): ThreatLevel {
        // FAIL-SAFE: Если данные истекли - считаем небезопасным
        if (metadata != null && metadata.freshness == Freshness.EXPIRED) {
            Log.w(TAG, "Scan data expired (age=${metadata.getAgeInMinutes()}min). Marking as UNKNOWN (fail-safe).")
            return ThreatLevel.UNKNOWN
        }
        
        // FAIL-SAFE: Если нет свежих данных и тип шифрования слабый - потенциально небезопасно
        if (metadata != null && metadata.freshness == Freshness.STALE && !securityType.isRecommended()) {
            Log.w(TAG, "Stale data (age=${metadata.getAgeInMinutes()}min) with weak encryption. Marking as POTENTIALLY_UNSAFE.")
            return ThreatLevel.HIGH // Повышаем уровень угрозы для устаревших данных с слабым шифрованием
        }
        
        // Если обнаружены угрозы - классифицируем по серьезности
        if (threats.isNotEmpty()) {
            val hasCritical = threats.any { it.severity == ThreatLevel.CRITICAL }
            val hasHigh = threats.any { it.severity == ThreatLevel.HIGH }
            val hasMedium = threats.any { it.severity == ThreatLevel.MEDIUM }
            
            return when {
                hasCritical -> ThreatLevel.CRITICAL
                hasHigh -> ThreatLevel.HIGH
                hasMedium -> ThreatLevel.MEDIUM
                else -> ThreatLevel.LOW
            }
        }
        
        // Проверяем тип безопасности
        when {
            securityType.isInsecure() -> {
                Log.d(TAG, "Insecure security type: $securityType")
                return ThreatLevel.HIGH
            }
            securityType.isDeprecated() -> {
                Log.d(TAG, "Deprecated security type: $securityType")
                return ThreatLevel.MEDIUM
            }
        }
        
        // FAIL-SAFE: Безопасно ТОЛЬКО если:
        // 1. Свежие данные (или нет информации о метаданных для обратной совместимости)
        // 2. Рекомендованный тип шифрования
        // 3. Нет обнаруженных угроз
        if ((metadata == null || metadata.freshness == Freshness.FRESH) && securityType.isRecommended()) {
            Log.d(TAG, "Network is safe: fresh data, recommended encryption, no threats")
            return ThreatLevel.SAFE
        }
        
        // FAIL-SAFE: В остальных случаях - неизвестно (безопаснее, чем считать безопасным)
        Log.d(TAG, "Cannot determine safety level with certainty. Marking as UNKNOWN (fail-safe).")
        return ThreatLevel.UNKNOWN
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
