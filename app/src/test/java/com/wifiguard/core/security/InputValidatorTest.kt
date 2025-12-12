package com.wifiguard.core.security

import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.domain.model.WifiScanResult
import com.wifiguard.core.domain.model.WifiStandard
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Тесты для валидатора входных данных InputValidator
 * 
 * Покрытие:
 * ✅ Валидация BSSID (MAC-адрес)
 * ✅ Валидация SSID
 * ✅ Валидация уровня сигнала
 * ✅ Валидация частоты WiFi
 * ✅ Санитизация поисковых запросов
 * ✅ Обнаружение подозрительных SSID
 * ✅ Валидация полного WifiScanResult
 * 
 * @author WifiGuard Security Team
 */
class InputValidatorTest {
    
    private lateinit var validator: InputValidator
    
    @Before
    fun setUp() {
        validator = InputValidator()
    }
    
    // ==================== ТЕСТЫ BSSID ====================
    
    @Test
    fun `validateBssid - корректный MAC-адрес возвращает Valid`() {
        val result = validator.validateBssid("AA:BB:CC:DD:EE:FF")
        assertTrue("Корректный MAC должен быть валидным", result is InputValidator.ValidationResult.Valid)
    }
    
    @Test
    fun `validateBssid - MAC в нижнем регистре валиден`() {
        val result = validator.validateBssid("aa:bb:cc:dd:ee:ff")
        assertTrue("MAC в нижнем регистре должен быть валидным", result is InputValidator.ValidationResult.Valid)
    }
    
    @Test
    fun `validateBssid - смешанный регистр валиден`() {
        val result = validator.validateBssid("Aa:Bb:Cc:Dd:Ee:Ff")
        assertTrue("MAC в смешанном регистре должен быть валидным", result is InputValidator.ValidationResult.Valid)
    }
    
    @Test
    fun `validateBssid - пустой BSSID возвращает Invalid`() {
        val result = validator.validateBssid("")
        assertTrue("Пустой BSSID должен быть невалидным", result is InputValidator.ValidationResult.Invalid)
    }
    
    @Test
    fun `validateBssid - unknown BSSID возвращает Invalid`() {
        val result = validator.validateBssid("unknown")
        assertTrue("BSSID 'unknown' должен быть невалидным", result is InputValidator.ValidationResult.Invalid)
    }
    
    @Test
    fun `validateBssid - некорректный формат возвращает Invalid`() {
        val invalidMacs = listOf(
            "AA:BB:CC:DD:EE",        // Слишком короткий
            "AA:BB:CC:DD:EE:FF:GG",  // Слишком длинный
            "AA-BB-CC-DD-EE-FF",     // Неправильный разделитель
            "AABBCCDDEEFF",          // Без разделителей
            "GG:HH:II:JJ:KK:LL",     // Невалидные hex-символы
            "AA:BB:CC:DD:EE:FG",     // G - невалидный hex
        )
        
        invalidMacs.forEach { mac ->
            val result = validator.validateBssid(mac)
            assertTrue("MAC '$mac' должен быть невалидным", result is InputValidator.ValidationResult.Invalid)
        }
    }
    
    @Test
    fun `validateBssid - multicast MAC возвращает Invalid`() {
        // Multicast MAC имеет LSB первого байта = 1
        val result = validator.validateBssid("01:BB:CC:DD:EE:FF")
        assertTrue("Multicast MAC должен быть невалидным", result is InputValidator.ValidationResult.Invalid)
    }
    
    // ==================== ТЕСТЫ SSID ====================
    
    @Test
    fun `validateSsid - корректный SSID возвращает Valid`() {
        val result = validator.validateSsid("MyHomeNetwork")
        assertTrue("Корректный SSID должен быть валидным", result is InputValidator.ValidationResult.Valid)
    }
    
    @Test
    fun `validateSsid - пустой SSID валиден (скрытая сеть)`() {
        val result = validator.validateSsid("")
        assertTrue("Пустой SSID должен быть валидным (скрытая сеть)", result is InputValidator.ValidationResult.Valid)
    }
    
    @Test
    fun `validateSsid - unknown ssid валиден`() {
        val result = validator.validateSsid("<unknown ssid>")
        assertTrue("'<unknown ssid>' должен быть валидным", result is InputValidator.ValidationResult.Valid)
    }
    
    @Test
    fun `validateSsid - Hidden Network валиден`() {
        val result = validator.validateSsid("Hidden Network")
        assertTrue("'Hidden Network' должен быть валидным", result is InputValidator.ValidationResult.Valid)
    }
    
    @Test
    fun `validateSsid - слишком длинный SSID возвращает Invalid`() {
        val longSsid = "A".repeat(33) // 33 символа, макс 32
        val result = validator.validateSsid(longSsid)
        assertTrue("SSID > 32 символов должен быть невалидным", result is InputValidator.ValidationResult.Invalid)
    }
    
    @Test
    fun `validateSsid - SSID с русскими символами валиден`() {
        val result = validator.validateSsid("Моя домашняя сеть")
        assertTrue("SSID с кириллицей должен быть валидным", result is InputValidator.ValidationResult.Valid)
    }
    
    @Test
    fun `validateSsid - SSID с эмодзи валиден`() {
        val result = validator.validateSsid("WiFi 📶")
        assertTrue("SSID с эмодзи должен быть валидным", result is InputValidator.ValidationResult.Valid)
    }
    
    // ==================== ТЕСТЫ УРОВНЯ СИГНАЛА ====================
    
    @Test
    fun `validateSignalStrength - типичный уровень валиден`() {
        val validLevels = listOf(-30, -50, -70, -90, -100)
        validLevels.forEach { level ->
            val result = validator.validateSignalStrength(level)
            assertTrue("Уровень $level dBm должен быть валидным", result is InputValidator.ValidationResult.Valid)
        }
    }
    
    @Test
    fun `validateSignalStrength - граничные значения валидны`() {
        assertTrue("0 dBm должен быть валидным", 
            validator.validateSignalStrength(0) is InputValidator.ValidationResult.Valid)
        assertTrue("-127 dBm должен быть валидным", 
            validator.validateSignalStrength(-127) is InputValidator.ValidationResult.Valid)
    }
    
    @Test
    fun `validateSignalStrength - значение вне диапазона невалидно`() {
        assertTrue("1 dBm должен быть невалидным", 
            validator.validateSignalStrength(1) is InputValidator.ValidationResult.Invalid)
        assertTrue("-128 dBm должен быть невалидным", 
            validator.validateSignalStrength(-128) is InputValidator.ValidationResult.Invalid)
    }
    
    // ==================== ТЕСТЫ ЧАСТОТЫ ====================
    
    @Test
    fun `validateFrequency - частота 2_4 GHz валидна`() {
        val valid2_4GHz = listOf(2412, 2437, 2462, 2484)
        valid2_4GHz.forEach { freq ->
            val result = validator.validateFrequency(freq)
            assertTrue("Частота $freq MHz должна быть валидной", result is InputValidator.ValidationResult.Valid)
        }
    }
    
    @Test
    fun `validateFrequency - частота 5 GHz валидна`() {
        val valid5GHz = listOf(5180, 5240, 5500, 5745)
        valid5GHz.forEach { freq ->
            val result = validator.validateFrequency(freq)
            assertTrue("Частота $freq MHz должна быть валидной", result is InputValidator.ValidationResult.Valid)
        }
    }
    
    @Test
    fun `validateFrequency - частота 6 GHz (WiFi 6E) валидна`() {
        val valid6GHz = listOf(5935, 6115, 6875, 7115)
        valid6GHz.forEach { freq ->
            val result = validator.validateFrequency(freq)
            assertTrue("Частота $freq MHz должна быть валидной", result is InputValidator.ValidationResult.Valid)
        }
    }
    
    @Test
    fun `validateFrequency - нулевая частота валидна (неизвестно)`() {
        val result = validator.validateFrequency(0)
        assertTrue("Частота 0 должна быть валидной", result is InputValidator.ValidationResult.Valid)
    }
    
    @Test
    fun `validateFrequency - некорректная частота невалидна`() {
        val invalidFreqs = listOf(1000, 3000, 10000)
        invalidFreqs.forEach { freq ->
            val result = validator.validateFrequency(freq)
            assertTrue("Частота $freq MHz должна быть невалидной", result is InputValidator.ValidationResult.Invalid)
        }
    }
    
    // ==================== ТЕСТЫ САНИТИЗАЦИИ ПОИСКА ====================
    
    @Test
    fun `sanitizeSearchQuery - нормальный запрос остаётся без изменений`() {
        val query = "MyNetwork"
        val sanitized = validator.sanitizeSearchQuery(query)
        assertEquals("Нормальный запрос не должен меняться", query, sanitized)
    }
    
    @Test
    fun `sanitizeSearchQuery - пробелы сохраняются`() {
        val query = "My Home Network"
        val sanitized = validator.sanitizeSearchQuery(query)
        assertEquals("Пробелы должны сохраняться", query, sanitized)
    }
    
    @Test
    fun `sanitizeSearchQuery - кириллица сохраняется`() {
        val query = "Моя сеть"
        val sanitized = validator.sanitizeSearchQuery(query)
        assertEquals("Кириллица должна сохраняться", query, sanitized)
    }
    
    @Test
    fun `sanitizeSearchQuery - пустой запрос возвращает null`() {
        assertNull("Пустой запрос должен возвращать null", validator.sanitizeSearchQuery(""))
        assertNull("Пробелы должны возвращать null", validator.sanitizeSearchQuery("   "))
    }
    
    @Test
    fun `sanitizeSearchQuery - опасные символы удаляются`() {
        val query = "Network<script>alert('xss')</script>"
        val sanitized = validator.sanitizeSearchQuery(query)
        assertNotNull("Санитизированный запрос не должен быть null", sanitized)
        assertFalse("Опасные символы должны быть удалены", sanitized!!.contains("<"))
    }
    
    @Test
    fun `sanitizeSearchQuery - длина ограничивается`() {
        val longQuery = "A".repeat(200)
        val sanitized = validator.sanitizeSearchQuery(longQuery)
        assertNotNull(sanitized)
        assertTrue("Длина должна быть ограничена 100 символами", sanitized!!.length <= 100)
    }
    
    // ==================== ТЕСТЫ ОБНАРУЖЕНИЯ ПОДОЗРИТЕЛЬНЫХ SSID ====================
    
    @Test
    fun `isSuspiciousSsid - Free WiFi подозрителен`() {
        val suspiciousSsids = listOf(
            "Free WiFi",
            "FREE-WIFI",
            "free_wifi",
            "Public WiFi",
            "Guest Network",
            "Free Internet Hotspot"
        )
        
        suspiciousSsids.forEach { ssid ->
            assertTrue("SSID '$ssid' должен быть подозрительным", validator.isSuspiciousSsid(ssid))
        }
    }
    
    @Test
    fun `isSuspiciousSsid - роутер по умолчанию подозрителен`() {
        val defaultSsids = listOf("linksys", "NETGEAR", "dlink", "TP-Link", "ASUS")
        
        defaultSsids.forEach { ssid ->
            assertTrue("SSID '$ssid' должен быть подозрительным", validator.isSuspiciousSsid(ssid))
        }
    }
    
    @Test
    fun `isSuspiciousSsid - обычные SSID не подозрительны`() {
        val normalSsids = listOf(
            "MyHomeWiFi",
            "Квартира 42",
            "Office_5G",
            "Smith Family Network"
        )
        
        normalSsids.forEach { ssid ->
            assertFalse("SSID '$ssid' не должен быть подозрительным", validator.isSuspiciousSsid(ssid))
        }
    }
    
    // ==================== ТЕСТЫ ПОЛНОЙ ВАЛИДАЦИИ ====================
    
    @Test
    fun `validateWifiScanResult - корректный результат валиден`() {
        val scanResult = createValidScanResult()
        val result = validator.validateWifiScanResult(scanResult)
        assertTrue("Корректный результат должен быть валидным", result is InputValidator.ValidationResult.Valid)
    }
    
    @Test
    fun `validateWifiScanResult - невалидный BSSID делает результат невалидным`() {
        val scanResult = createValidScanResult().copy(bssid = "invalid-mac")
        val result = validator.validateWifiScanResult(scanResult)
        assertTrue("Результат с невалидным BSSID должен быть невалидным", result is InputValidator.ValidationResult.Invalid)
    }
    
    @Test
    fun `validateWifiScanResult - невалидный уровень сигнала делает результат невалидным`() {
        val scanResult = createValidScanResult().copy(level = 100)
        val result = validator.validateWifiScanResult(scanResult)
        assertTrue("Результат с невалидным уровнем сигнала должен быть невалидным", result is InputValidator.ValidationResult.Invalid)
    }
    
    @Test
    fun `filterValidResults - фильтрует невалидные результаты`() {
        // ИСПРАВЛЕНО: Третий результат использует BSSID "BA:CC:DD:EE:FF:00" вместо "BB:CC:DD:EE:FF:00"
        // Причина: BB в binary = 10111011 (LSB=1), что делает его multicast MAC-адресом (невалидным)
        // BA в binary = 10111010 (LSB=0), что делает его unicast MAC-адресом (валидным)
        val results = listOf(
            createValidScanResult(),                                     // VALID (корректный)
            createValidScanResult().copy(bssid = "invalid"),             // INVALID (невалидный формат BSSID)
            createValidScanResult().copy(bssid = "BA:CC:DD:EE:FF:00"),  // VALID (корректный unicast MAC)
            createValidScanResult().copy(level = 999)                    // INVALID (невалидный уровень сигнала)
        )
        
        val filtered = validator.filterValidResults(results)
        
        assertEquals("Должно остаться 2 валидных результата", 2, filtered.size)
    }
    
    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================
    
    private fun createValidScanResult(): WifiScanResult {
        return WifiScanResult(
            ssid = "TestNetwork",
            bssid = "AA:BB:CC:DD:EE:FF",
            capabilities = "[WPA2-PSK-CCMP][ESS]",
            frequency = 2437,
            level = -65,
            timestamp = System.currentTimeMillis(),
            securityType = SecurityType.WPA2,
            threatLevel = ThreatLevel.SAFE,
            isConnected = false,
            isHidden = false,
            vendor = "Test Vendor",
            channel = 6,
            standard = WifiStandard.WIFI_2_4_GHZ
        )
    }
}
