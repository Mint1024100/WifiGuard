package com.wifiguard.core.security

import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.feature.scanner.domain.model.EncryptionType
import com.wifiguard.feature.scanner.domain.model.WifiInfo
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever

/**
 * Unit tests for SecurityManager class
 * 
 * Тестирует:
 * - Анализ безопасности Wi-Fi сетей
 * - Обнаружение угроз
 * - Рекомендации по безопасности
 * - Обработку различных типов шифрования
 * - Обнаружение аномалий
 */
@RunWith(MockitoJUnitRunner::class)
class SecurityManagerTest {

    @Mock
    private lateinit var mockAesEncryption: AesEncryption

    private lateinit var securityManager: SecurityManager

    @Before
    fun setUp() {
        securityManager = SecurityManager(mockAesEncryption)
    }

    @Test
    fun `analyze network security should detect open network as high risk`() {
        // Given
        val wifiInfo = createWifiInfo(
            ssid = "OpenNetwork",
            encryptionType = EncryptionType.NONE,
            signalStrength = -50
        )

        // When
        val result = securityManager.analyzeNetworkSecurity(wifiInfo)

        // Then
        assertEquals("Open network should be high risk", ThreatLevel.HIGH, result.threatLevel)
        assertTrue("Should contain open network threat", 
            result.threats.contains(ThreatType.OPEN_NETWORK))
        assertTrue("Should have recommendations", result.recommendations.isNotEmpty())
    }

    @Test
    fun `analyze network security should detect WEP as high risk`() {
        // Given
        val wifiInfo = createWifiInfo(
            ssid = "WEPNetwork",
            encryptionType = EncryptionType.WEP,
            signalStrength = -60
        )

        // When
        val result = securityManager.analyzeNetworkSecurity(wifiInfo)

        // Then
        assertEquals("WEP network should be high risk", ThreatLevel.HIGH, result.threatLevel)
        assertTrue("Should contain weak encryption threat", 
            result.threats.contains(ThreatType.WEAK_ENCRYPTION))
    }

    @Test
    fun `analyze network security should detect WPA as medium risk`() {
        // Given
        val wifiInfo = createWifiInfo(
            ssid = "WPANetwork",
            encryptionType = EncryptionType.WPA,
            signalStrength = -55
        )

        // When
        val result = securityManager.analyzeNetworkSecurity(wifiInfo)

        // Then
        assertEquals("WPA network should be medium risk", ThreatLevel.MEDIUM, result.threatLevel)
        assertTrue("Should contain outdated protocol threat", 
            result.threats.contains(ThreatType.OUTDATED_PROTOCOL))
    }

    @Test
    fun `analyze network security should detect WPA2 as low risk`() {
        // Given
        val wifiInfo = createWifiInfo(
            ssid = "WPA2Network",
            encryptionType = EncryptionType.WPA2,
            signalStrength = -45
        )

        // When
        val result = securityManager.analyzeNetworkSecurity(wifiInfo)

        // Then
        assertEquals("WPA2 network should be low risk", ThreatLevel.LOW, result.threatLevel)
        assertFalse("Should not contain major threats", 
            result.threats.contains(ThreatType.OPEN_NETWORK) ||
            result.threats.contains(ThreatType.WEAK_ENCRYPTION) ||
            result.threats.contains(ThreatType.OUTDATED_PROTOCOL))
    }

    @Test
    fun `analyze network security should detect WPA3 as low risk`() {
        // Given
        val wifiInfo = createWifiInfo(
            ssid = "WPA3Network",
            encryptionType = EncryptionType.WPA3,
            signalStrength = -40
        )

        // When
        val result = securityManager.analyzeNetworkSecurity(wifiInfo)

        // Then
        assertEquals("WPA3 network should be low risk", ThreatLevel.LOW, result.threatLevel)
        assertFalse("Should not contain major threats", 
            result.threats.contains(ThreatType.OPEN_NETWORK) ||
            result.threats.contains(ThreatType.WEAK_ENCRYPTION) ||
            result.threats.contains(ThreatType.OUTDATED_PROTOCOL))
    }

    @Test
    fun `analyze network security should detect weak signal`() {
        // Given
        val wifiInfo = createWifiInfo(
            ssid = "WeakSignalNetwork",
            encryptionType = EncryptionType.WPA2,
            signalStrength = -90 // Very weak signal
        )

        // When
        val result = securityManager.analyzeNetworkSecurity(wifiInfo)

        // Then
        assertTrue("Should contain weak signal threat", 
            result.threats.contains(ThreatType.WEAK_SIGNAL))
    }

    @Test
    fun `analyze network security should detect suspicious network name`() {
        // Given
        val wifiInfo = createWifiInfo(
            ssid = "free_wifi",
            encryptionType = EncryptionType.WPA2,
            signalStrength = -50
        )

        // When
        val result = securityManager.analyzeNetworkSecurity(wifiInfo)

        // Then
        assertTrue("Should contain suspicious name threat", 
            result.threats.contains(ThreatType.SUSPICIOUS_SSID))
    }

    @Test
    fun `analyze network security should detect suspicious MAC address`() {
        // Given
        val wifiInfo = createWifiInfo(
            ssid = "NormalNetwork",
            bssid = "00:00:00:00:00:00", // Suspicious MAC
            encryptionType = EncryptionType.WPA2,
            signalStrength = -50
        )

        // When
        val result = securityManager.analyzeNetworkSecurity(wifiInfo)

        // Then
        assertTrue("Should contain suspicious MAC threat", 
            result.threats.contains(ThreatType.SUSPICIOUS_BSSID))
    }

    @Test
    fun `analyze network security should detect unknown security as medium risk`() {
        // Given
        val wifiInfo = createWifiInfo(
            ssid = "UnknownNetwork",
            encryptionType = EncryptionType.UNKNOWN,
            signalStrength = -60
        )

        // When
        val result = securityManager.analyzeNetworkSecurity(wifiInfo)

        // Then
        assertEquals("Unknown security should be medium risk", ThreatLevel.MEDIUM, result.threatLevel)
        assertTrue("Should contain unknown security threat", 
            result.threats.contains(ThreatType.UNKNOWN_ENCRYPTION))
    }

    @Test
    fun `analyze network security should provide appropriate recommendations`() {
        // Given
        val wifiInfo = createWifiInfo(
            ssid = "OpenNetwork",
            encryptionType = EncryptionType.NONE,
            signalStrength = -50
        )

        // When
        val result = securityManager.analyzeNetworkSecurity(wifiInfo)

        // Then
        assertTrue("Should have recommendations", result.recommendations.isNotEmpty())
        assertTrue("Should recommend avoiding open networks", 
            result.recommendations.any { it.contains("открытых сетей") || it.contains("open") })
    }

    @Test
    fun `analyze network security should handle multiple threats`() {
        // Given
        val wifiInfo = createWifiInfo(
            ssid = "free_wifi",
            bssid = "00:00:00:00:00:00",
            encryptionType = EncryptionType.WEP,
            signalStrength = -90
        )

        // When
        val result = securityManager.analyzeNetworkSecurity(wifiInfo)

        // Then
        assertEquals("Should be high risk with multiple threats", ThreatLevel.HIGH, result.threatLevel)
        assertTrue("Should have multiple threats", result.threats.size > 1)
        assertTrue("Should contain weak encryption", 
            result.threats.contains(ThreatType.WEAK_ENCRYPTION))
        assertTrue("Should contain suspicious name", 
            result.threats.contains(ThreatType.SUSPICIOUS_SSID))
        assertTrue("Should contain suspicious MAC", 
            result.threats.contains(ThreatType.SUSPICIOUS_BSSID))
        assertTrue("Should contain weak signal", 
            result.threats.contains(ThreatType.WEAK_SIGNAL))
    }

    @Test
    fun `analyze network security should set correct timestamp`() {
        // Given
        val wifiInfo = createWifiInfo(
            ssid = "TestNetwork",
            encryptionType = EncryptionType.WPA2,
            signalStrength = -50
        )
        val beforeAnalysis = System.currentTimeMillis()

        // When
        val result = securityManager.analyzeNetworkSecurity(wifiInfo)

        // Then
        val afterAnalysis = System.currentTimeMillis()
        assertTrue("Timestamp should be within analysis time range", 
            result.analysisTimestamp in beforeAnalysis..afterAnalysis)
    }

    @Test
    fun `encrypt sensitive data should delegate to AesEncryption`() {
        // Given
        val testData = "sensitive data"
        val mockEncryptedData = EncryptedData(
            encryptedData = "encrypted".toByteArray(),
            iv = ByteArray(12),
            hmac = ByteArray(32)
        )
        whenever(mockAesEncryption.encrypt(testData)).thenReturn(mockEncryptedData)

        // When
        val result = securityManager.encryptSensitiveData(testData)

        // Then
        assertEquals("Should return encrypted data from AesEncryption", mockEncryptedData, result)
    }

    @Test
    fun `decrypt sensitive data should delegate to AesEncryption`() {
        // Given
        val mockEncryptedData = EncryptedData(
            encryptedData = "encrypted".toByteArray(),
            iv = ByteArray(12),
            hmac = ByteArray(32)
        )
        val expectedDecrypted = "decrypted data"
        whenever(mockAesEncryption.decrypt(mockEncryptedData)).thenReturn(expectedDecrypted)

        // When
        val result = securityManager.decryptSensitiveData(mockEncryptedData)

        // Then
        assertEquals("Should return decrypted data from AesEncryption", expectedDecrypted, result)
    }

    private fun createWifiInfo(
        ssid: String = "TestNetwork",
        bssid: String = "AA:BB:CC:DD:EE:FF",
        encryptionType: EncryptionType = EncryptionType.WPA2,
        signalStrength: Int = -50,
        capabilities: String = "[WPA2-PSK-CCMP][ESS]",
        frequency: Int = 2412,
        channel: Int = 1
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
            channel = channel,
            bandwidth = "20MHz",
            isHidden = false,
            isConnected = false,
            isSaved = false
        )
    }
}

