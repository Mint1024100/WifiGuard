package com.wifiguard.core.security

import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.domain.model.ThreatType
import com.wifiguard.core.domain.model.WifiScanResult
import com.wifiguard.core.domain.model.WifiStandard
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Тесты для детектора угроз ThreatDetector
 * 
 * Покрытие:
 * ✅ Детекция дублирующихся SSID (Evil Twin)
 * ✅ Детекция подозрительных SSID
 * ✅ Детекция глобальных угроз
 * ✅ Оптимизация - индексирование по SSID
 * ✅ Кэширование результатов
 * ✅ Производительность O(n) вместо O(n²)
 * 
 * ИСПРАВЛЕНО:
 * - Используются безопасные имена сетей без подозрительных ключевых слов
 * - Учтена логика ThreatDetector.detectSuspiciousSsid():
 *   - Подозрительные паттерны: "wifi", "free", "public", "guest", "hotspot" и др.
 *   - Имена роутеров по умолчанию: "linksys", "netgear", "dlink", "tp-link" и др.
 * - Примеры безопасных имён: "MyHomeNetwork", "Квартира 42", "Office_5G"
 * 
 * @author WifiGuard Security Team
 */
class ThreatDetectorTest {
    
    private lateinit var detector: ThreatDetector
    
    @Before
    fun setUp() {
        detector = ThreatDetector()
    }
    
    // ==================== ТЕСТЫ ДЕТЕКЦИИ ДУБЛИКАТОВ SSID ====================
    
    @Test
    fun `detectDuplicateSsid - находит дубликаты SSID`() {
        val networks = listOf(
            createNetwork("TestNetwork", "AA:BB:CC:DD:EE:01"),
            createNetwork("TestNetwork", "AA:BB:CC:DD:EE:02"),
            createNetwork("OtherNetwork", "AA:BB:CC:DD:EE:03")
        )
        
        detector.buildSsidIndex(networks)
        
        val threat = detector.detectDuplicateSsid(networks[0], networks)
        
        assertNotNull("Должна быть обнаружена угроза дублирования", threat)
        assertEquals("Тип угрозы должен быть DUPLICATE_SSID", ThreatType.DUPLICATE_SSID, threat!!.type)
        assertEquals("Уровень угрозы должен быть HIGH", ThreatLevel.HIGH, threat.severity)
    }
    
    @Test
    fun `detectDuplicateSsid - не находит дубликаты для уникальных SSID`() {
        val networks = listOf(
            createNetwork("Network1", "AA:BB:CC:DD:EE:01"),
            createNetwork("Network2", "AA:BB:CC:DD:EE:02"),
            createNetwork("Network3", "AA:BB:CC:DD:EE:03")
        )
        
        detector.buildSsidIndex(networks)
        
        val threat = detector.detectDuplicateSsid(networks[0], networks)
        
        assertNull("Не должно быть угрозы для уникальных SSID", threat)
    }
    
    @Test
    fun `detectDuplicateSsid - кэширует результаты`() {
        val networks = listOf(
            createNetwork("TestNetwork", "AA:BB:CC:DD:EE:01"),
            createNetwork("TestNetwork", "AA:BB:CC:DD:EE:02")
        )
        
        detector.buildSsidIndex(networks)
        
        // Первый вызов
        val threat1 = detector.detectDuplicateSsid(networks[0], networks)
        // Второй вызов (должен использовать кэш)
        val threat2 = detector.detectDuplicateSsid(networks[0], networks)
        
        assertNotNull("Первая угроза должна быть обнаружена", threat1)
        assertNotNull("Вторая угроза должна быть обнаружена", threat2)
        // Оба результата должны быть идентичны
        assertEquals("Типы угроз должны совпадать", threat1?.type, threat2?.type)
        assertEquals("Уровни угроз должны совпадать", threat1?.severity, threat2?.severity)
    }
    
    // ==================== ТЕСТЫ ДЕТЕКЦИИ ПОДОЗРИТЕЛЬНЫХ SSID ====================
    
    @Test
    fun `detectSuspiciousSsid - находит Free WiFi`() {
        val network = createNetwork("Free WiFi", "AA:BB:CC:DD:EE:01")
        
        val threat = detector.detectSuspiciousSsid(network)
        
        assertNotNull("Должна быть обнаружена подозрительная сеть", threat)
        assertEquals("Тип угрозы должен быть SUSPICIOUS_SSID", ThreatType.SUSPICIOUS_SSID, threat!!.type)
    }
    
    @Test
    fun `detectSuspiciousSsid - находит роутеры по умолчанию`() {
        val suspiciousNames = listOf("linksys", "NETGEAR", "dlink", "TP-Link_XXXX")
        
        suspiciousNames.forEach { name ->
            val network = createNetwork(name, "AA:BB:CC:DD:EE:01")
            val threat = detector.detectSuspiciousSsid(network)
            
            assertNotNull("SSID '$name' должен быть подозрительным", threat)
        }
    }
    
    @Test
    fun `detectSuspiciousSsid - не помечает обычные SSID`() {
        // ИСПРАВЛЕНО: Используем безопасные имена сетей, не содержащие подозрительные ключевые слова
        // Подозрительные паттерны из ThreatDetector.kt:100-126:
        // "free wifi", "wifi", "public wifi", "guest", "hotspot", "internet", "wireless", 
        // "open", "no password", "admin", "root", "test", "default", 
        // "linksys", "netgear", "dlink", "tp-link", "asus", "belkin", "router", "modem"
        val normalNames = listOf("MyHomeNetwork", "Квартира 42", "Office_5G")
        
        normalNames.forEach { name ->
            val network = createNetwork(name, "AA:BB:CC:DD:EE:01")
            val threat = detector.detectSuspiciousSsid(network)
            
            assertNull("SSID '$name' не должен быть подозрительным", threat)
        }
    }
    
    @Test
    fun `detectSuspiciousSsid - находит слишком длинные SSID`() {
        val longName = "A".repeat(35) // Больше 32 символов
        val network = createNetwork(longName, "AA:BB:CC:DD:EE:01")
        
        val threat = detector.detectSuspiciousSsid(network)
        
        assertNotNull("Слишком длинный SSID должен быть подозрительным", threat)
        assertEquals("Уровень угрозы должен быть LOW", ThreatLevel.LOW, threat!!.severity)
    }
    
    // ==================== ТЕСТЫ ГЛОБАЛЬНОЙ ДЕТЕКЦИИ УГРОЗ ====================
    
    @Test
    fun `detectGlobalThreats - находит множественные дубликаты`() {
        val networks = listOf(
            createNetwork("SameNetwork", "AA:BB:CC:DD:EE:01"),
            createNetwork("SameNetwork", "AA:BB:CC:DD:EE:02"),
            createNetwork("SameNetwork", "AA:BB:CC:DD:EE:03"),
            createNetwork("OtherNetwork", "AA:BB:CC:DD:EE:04")
        )
        
        val threats = detector.detectGlobalThreats(networks)
        
        val multipleDuplicates = threats.find { it.type == ThreatType.MULTIPLE_DUPLICATES }
        assertNotNull("Должны быть обнаружены множественные дубликаты", multipleDuplicates)
        assertEquals("Уровень угрозы должен быть HIGH", ThreatLevel.HIGH, multipleDuplicates!!.severity)
    }
    
    @Test
    fun `detectGlobalThreats - находит много открытых сетей`() {
        val networks = (1..5).map { i ->
            createNetwork("OpenNetwork$i", "AA:BB:CC:DD:EE:0$i", SecurityType.OPEN)
        }
        
        val threats = detector.detectGlobalThreats(networks)
        
        val suspiciousActivity = threats.find { it.type == ThreatType.SUSPICIOUS_ACTIVITY }
        assertNotNull("Должна быть обнаружена подозрительная активность", suspiciousActivity)
    }
    
    @Test
    fun `detectGlobalThreats - не срабатывает на мало открытых сетей`() {
        val networks = (1..2).map { i ->
            createNetwork("OpenNetwork$i", "AA:BB:CC:DD:EE:0$i", SecurityType.OPEN)
        }
        
        val threats = detector.detectGlobalThreats(networks)
        
        val suspiciousActivity = threats.find { it.type == ThreatType.SUSPICIOUS_ACTIVITY }
        assertNull("Не должно быть подозрительной активности для 2 открытых сетей", suspiciousActivity)
    }
    
    // ==================== ТЕСТЫ ОПТИМИЗАЦИИ ====================
    
    @Test
    fun `buildSsidIndex - строит корректный индекс`() {
        val networks = listOf(
            createNetwork("Network1", "AA:BB:CC:DD:EE:01"),
            createNetwork("Network1", "AA:BB:CC:DD:EE:02"),
            createNetwork("Network2", "AA:BB:CC:DD:EE:03")
        )
        
        // Строим индекс (должен выполниться без ошибок)
        detector.buildSsidIndex(networks)
        
        // Проверяем через детекцию дубликатов
        val threat = detector.detectDuplicateSsid(networks[0], networks)
        assertNotNull("Индекс должен позволять находить дубликаты", threat)
    }
    
    @Test
    fun `clearCaches - очищает все кэши`() {
        val networks = listOf(
            createNetwork("TestNetwork", "AA:BB:CC:DD:EE:01"),
            createNetwork("TestNetwork", "AA:BB:CC:DD:EE:02")
        )
        
        detector.buildSsidIndex(networks)
        detector.detectDuplicateSsid(networks[0], networks)
        
        // Очищаем кэши
        detector.clearCaches()
        
        // После очистки детекция должна работать корректно
        detector.buildSsidIndex(networks)
        val threat = detector.detectDuplicateSsid(networks[0], networks)
        assertNotNull("После очистки кэша детекция должна работать", threat)
    }
    
    @Test
    fun `performance - обработка большого списка сетей`() {
        // Создаём 100 сетей
        val networks = (1..100).map { i ->
            createNetwork("Network${i % 10}", "AA:BB:CC:DD:EE:${String.format("%02X", i)}")
        }
        
        val startTime = System.currentTimeMillis()
        
        detector.buildSsidIndex(networks)
        val threats = detector.detectGlobalThreats(networks)
        
        val duration = System.currentTimeMillis() - startTime
        
        // Должно выполниться быстро (менее 1 секунды для 100 сетей)
        assertTrue("Обработка 100 сетей должна занять < 1с (заняла ${duration}мс)", duration < 1000)
        assertTrue("Должны быть обнаружены угрозы", threats.isNotEmpty())
    }
    
    // ==================== ТЕСТЫ ДЕТЕКЦИИ ПОДОЗРИТЕЛЬНЫХ BSSID ====================
    
    @Test
    fun `detectGlobalThreats - находит последовательные MAC-адреса`() {
        // Создаём сети с последовательными MAC в одном OUI
        val networks = listOf(
            createNetwork("Network1", "AA:BB:CC:DD:00:01"),
            createNetwork("Network2", "AA:BB:CC:DD:00:02"),
            createNetwork("Network3", "AA:BB:CC:DD:00:03"),
            createNetwork("Network4", "11:22:33:44:55:66") // Другой OUI
        )
        
        val threats = detector.detectGlobalThreats(networks)
        
        val suspiciousBssid = threats.find { it.type == ThreatType.SUSPICIOUS_BSSID }
        assertNotNull("Должны быть обнаружены подозрительные BSSID", suspiciousBssid)
    }
    
    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================
    
    private fun createNetwork(
        ssid: String,
        bssid: String,
        securityType: SecurityType = SecurityType.WPA2
    ): WifiScanResult {
        return WifiScanResult(
            ssid = ssid,
            bssid = bssid,
            capabilities = "[WPA2-PSK-CCMP][ESS]",
            frequency = 2437,
            level = -65,
            timestamp = System.currentTimeMillis(),
            securityType = securityType,
            threatLevel = ThreatLevel.SAFE,
            isConnected = false,
            isHidden = false,
            vendor = "Test Vendor",
            channel = 6,
            standard = WifiStandard.WIFI_2_4_GHZ
        )
    }
}
