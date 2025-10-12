package com.wifiguard.feature.scanner

import com.wifiguard.feature.scanner.domain.model.EncryptionType
import com.wifiguard.feature.scanner.domain.model.WifiInfo
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

/**
 * Unit tests for Wi-Fi scanning functionality
 * 
 * Тестирует:
 * - Парсинг типов шифрования
 * - Валидацию Wi-Fi данных
 * - Обработку различных форматов capabilities
 * - Качество сигнала
 */
@RunWith(MockitoJUnitRunner::class)
class WifiScannerTest {

    @Test
    fun `parse encryption type should correctly identify WPA3`() {
        // Given
        val capabilities = "[WPA3-SAE-CCMP][ESS]"

        // When
        val encryptionType = WifiInfo.parseEncryptionType(capabilities)

        // Then
        assertEquals("Should identify WPA3", EncryptionType.WPA3, encryptionType)
    }

    @Test
    fun `parse encryption type should correctly identify WPA2`() {
        // Given
        val capabilities = "[WPA2-PSK-CCMP][ESS]"

        // When
        val encryptionType = WifiInfo.parseEncryptionType(capabilities)

        // Then
        assertEquals("Should identify WPA2", EncryptionType.WPA2, encryptionType)
    }

    @Test
    fun `parse encryption type should correctly identify WPA`() {
        // Given
        val capabilities = "[WPA-PSK-TKIP][ESS]"

        // When
        val encryptionType = WifiInfo.parseEncryptionType(capabilities)

        // Then
        assertEquals("Should identify WPA", EncryptionType.WPA, encryptionType)
    }

    @Test
    fun `parse encryption type should correctly identify WEP`() {
        // Given
        val capabilities = "[WEP][ESS]"

        // When
        val encryptionType = WifiInfo.parseEncryptionType(capabilities)

        // Then
        assertEquals("Should identify WEP", EncryptionType.WEP, encryptionType)
    }

    @Test
    fun `parse encryption type should correctly identify open network`() {
        // Given
        val capabilities = "[ESS]"

        // When
        val encryptionType = WifiInfo.parseEncryptionType(capabilities)

        // Then
        assertEquals("Should identify open network", EncryptionType.NONE, encryptionType)
    }

    @Test
    fun `parse encryption type should handle empty capabilities`() {
        // Given
        val capabilities = ""

        // When
        val encryptionType = WifiInfo.parseEncryptionType(capabilities)

        // Then
        assertEquals("Should identify open network for empty capabilities", EncryptionType.NONE, encryptionType)
    }

    @Test
    fun `parse encryption type should handle unknown capabilities`() {
        // Given
        val capabilities = "[UNKNOWN-SECURITY]"

        // When
        val encryptionType = WifiInfo.parseEncryptionType(capabilities)

        // Then
        assertEquals("Should identify unknown encryption", EncryptionType.UNKNOWN, encryptionType)
    }

    @Test
    fun `wifi info should calculate correct signal quality`() {
        // Test excellent signal
        val excellentWifi = createWifiInfo(signalStrength = -30)
        assertEquals("Should be excellent quality", 
            com.wifiguard.feature.scanner.domain.model.SignalQuality.EXCELLENT, 
            excellentWifi.signalQuality)

        // Test good signal
        val goodWifi = createWifiInfo(signalStrength = -55)
        assertEquals("Should be good quality", 
            com.wifiguard.feature.scanner.domain.model.SignalQuality.GOOD, 
            goodWifi.signalQuality)

        // Test fair signal
        val fairWifi = createWifiInfo(signalStrength = -65)
        assertEquals("Should be fair quality", 
            com.wifiguard.feature.scanner.domain.model.SignalQuality.FAIR, 
            fairWifi.signalQuality)

        // Test poor signal
        val poorWifi = createWifiInfo(signalStrength = -80)
        assertEquals("Should be poor quality", 
            com.wifiguard.feature.scanner.domain.model.SignalQuality.POOR, 
            poorWifi.signalQuality)
    }

    @Test
    fun `wifi info should calculate correct signal strength percentage`() {
        // Test various signal strengths
        val wifi1 = createWifiInfo(signalStrength = -30)
        assertEquals("Should be 100% for -30 dBm", 100, wifi1.getSignalStrengthPercent())

        val wifi2 = createWifiInfo(signalStrength = -50)
        assertEquals("Should be 80% for -50 dBm", 80, wifi2.getSignalStrengthPercent())

        val wifi3 = createWifiInfo(signalStrength = -60)
        assertEquals("Should be 60% for -60 dBm", 60, wifi3.getSignalStrengthPercent())

        val wifi4 = createWifiInfo(signalStrength = -70)
        assertEquals("Should be 40% for -70 dBm", 40, wifi4.getSignalStrengthPercent())

        val wifi5 = createWifiInfo(signalStrength = -80)
        assertEquals("Should be 20% for -80 dBm", 20, wifi5.getSignalStrengthPercent())

        val wifi6 = createWifiInfo(signalStrength = -90)
        assertEquals("Should be 0% for -90 dBm", 0, wifi6.getSignalStrengthPercent())
    }

    @Test
    fun `wifi info should identify correct frequency band`() {
        // Test 2.4 GHz band
        val wifi24 = createWifiInfo(frequency = 2412)
        assertEquals("Should identify 2.4 GHz", "2.4 ГГц", wifi24.getFrequencyBand())

        // Test 5 GHz band
        val wifi5 = createWifiInfo(frequency = 5180)
        assertEquals("Should identify 5 GHz", "5 ГГц", wifi5.getFrequencyBand())

        // Test 6 GHz band
        val wifi6 = createWifiInfo(frequency = 6000)
        assertEquals("Should identify 6 GHz", "6 ГГц", wifi6.getFrequencyBand())

        // Test unknown frequency
        val wifiUnknown = createWifiInfo(frequency = 1000)
        assertEquals("Should identify unknown frequency", "Неизвестно", wifiUnknown.getFrequencyBand())
    }

    @Test
    fun `wifi info should correctly identify secure networks`() {
        // Test secure networks
        val secureWifi1 = createWifiInfo(encryptionType = EncryptionType.WPA2)
        assertTrue("WPA2 should be secure", secureWifi1.isSecure)

        val secureWifi2 = createWifiInfo(encryptionType = EncryptionType.WPA3)
        assertTrue("WPA3 should be secure", secureWifi2.isSecure)

        // Test insecure networks
        val insecureWifi1 = createWifiInfo(encryptionType = EncryptionType.NONE)
        assertFalse("Open network should not be secure", insecureWifi1.isSecure)

        val insecureWifi2 = createWifiInfo(encryptionType = EncryptionType.WEP)
        assertFalse("WEP should not be considered secure", insecureWifi2.isSecure)
    }

    @Test
    fun `encryption type should correctly identify open networks`() {
        // Test open network
        assertTrue("NONE should be open", EncryptionType.NONE.isOpen())
        
        // Test secured networks
        assertFalse("WPA2 should not be open", EncryptionType.WPA2.isOpen())
        assertFalse("WPA3 should not be open", EncryptionType.WPA3.isOpen())
        assertFalse("WEP should not be open", EncryptionType.WEP.isOpen())
    }

    @Test
    fun `encryption type should have correct security levels`() {
        // Test security levels
        assertEquals("NONE should have no security", 
            com.wifiguard.feature.scanner.domain.model.SecurityLevel.NONE, 
            EncryptionType.NONE.securityLevel)
        
        assertEquals("WEP should have low security", 
            com.wifiguard.feature.scanner.domain.model.SecurityLevel.LOW, 
            EncryptionType.WEP.securityLevel)
        
        assertEquals("WPA should have medium security", 
            com.wifiguard.feature.scanner.domain.model.SecurityLevel.MEDIUM, 
            EncryptionType.WPA.securityLevel)
        
        assertEquals("WPA2 should have high security", 
            com.wifiguard.feature.scanner.domain.model.SecurityLevel.HIGH, 
            EncryptionType.WPA2.securityLevel)
        
        assertEquals("WPA3 should have maximum security", 
            com.wifiguard.feature.scanner.domain.model.SecurityLevel.MAXIMUM, 
            EncryptionType.WPA3.securityLevel)
    }

    private fun createWifiInfo(
        ssid: String = "TestNetwork",
        bssid: String = "AA:BB:CC:DD:EE:FF",
        encryptionType: EncryptionType = EncryptionType.WPA2,
        signalStrength: Int = -50,
        frequency: Int = 2412,
        capabilities: String = "[WPA2-PSK-CCMP][ESS]"
    ): WifiInfo {
        return WifiInfo(
            ssid = ssid,
            bssid = bssid,
            capabilities = capabilities,
            level = signalStrength,
            frequency = frequency,
            timestamp = System.currentTimeMillis(),
            encryptionType = encryptionType,
            signalStrength = signalStrength,
            channel = 1,
            bandwidth = "20MHz",
            isHidden = false,
            isConnected = false,
            isSaved = false
        )
    }
}

