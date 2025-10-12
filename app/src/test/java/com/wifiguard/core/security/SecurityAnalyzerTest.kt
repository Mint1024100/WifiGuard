package com.wifiguard.core.security

import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.domain.model.WifiScanResult
import com.wifiguard.core.domain.model.WifiStandard
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Тесты для SecurityAnalyzer
 */
@RunWith(MockitoJUnitRunner::class)
class SecurityAnalyzerTest {
    
    @Mock
    private lateinit var threatDetector: ThreatDetector
    
    @Mock
    private lateinit var encryptionAnalyzer: EncryptionAnalyzer
    
    private lateinit var securityAnalyzer: SecurityAnalyzer
    
    @Before
    fun setUp() {
        securityAnalyzer = SecurityAnalyzer(threatDetector, encryptionAnalyzer)
    }
    
    @Test
    fun `analyzeNetworks should return security report with correct statistics`() = runBlocking {
        // Given
        val scanResults = createTestScanResults()
        val expectedThreats = listOf(
            SecurityThreat(
                type = ThreatType.OPEN_NETWORK,
                severity = ThreatLevel.CRITICAL,
                description = "Open network",
                networkSsid = "OpenNetwork",
                networkBssid = "00:11:22:33:44:55"
            )
        )
        
        whenever(threatDetector.detectGlobalThreats(scanResults)).thenReturn(expectedThreats)
        whenever(encryptionAnalyzer.analyzeEncryption(any())).thenReturn(
            EncryptionAnalysis(
                securityType = SecurityType.OPEN,
                threats = expectedThreats,
                vulnerabilities = listOf("No encryption"),
                recommendations = listOf("Avoid open networks"),
                encryptionStrength = EncryptionStrength.NONE,
                isSecure = false
            )
        )
        
        // When
        val result = securityAnalyzer.analyzeNetworks(scanResults)
        
        // Then
        assertNotNull(result)
        assertEquals(3, result.totalNetworks)
        assertEquals(1, result.criticalRiskNetworks)
        assertEquals(1, result.threats.size)
        assertEquals(ThreatLevel.CRITICAL, result.overallRiskLevel)
    }
    
    @Test
    fun `analyzeNetworks should handle empty scan results`() = runBlocking {
        // Given
        val emptyScanResults = emptyList<WifiScanResult>()
        
        whenever(threatDetector.detectGlobalThreats(emptyScanResults)).thenReturn(emptyList())
        
        // When
        val result = securityAnalyzer.analyzeNetworks(emptyScanResults)
        
        // Then
        assertNotNull(result)
        assertEquals(0, result.totalNetworks)
        assertEquals(0, result.threats.size)
        assertEquals(ThreatLevel.SAFE, result.overallRiskLevel)
    }
    
    @Test
    fun `analyzeNetworks should detect duplicate SSID threats`() = runBlocking {
        // Given
        val scanResults = createDuplicateSsidScanResults()
        val duplicateThreat = SecurityThreat(
            type = ThreatType.DUPLICATE_SSID,
            severity = ThreatLevel.HIGH,
            description = "Duplicate SSID detected",
            networkSsid = "TestNetwork",
            networkBssid = "00:11:22:33:44:55"
        )
        
        whenever(threatDetector.detectGlobalThreats(scanResults)).thenReturn(listOf(duplicateThreat))
        whenever(encryptionAnalyzer.analyzeEncryption(any())).thenReturn(
            EncryptionAnalysis(
                securityType = SecurityType.WPA2,
                threats = emptyList(),
                vulnerabilities = emptyList(),
                recommendations = emptyList(),
                encryptionStrength = EncryptionStrength.STRONG,
                isSecure = true
            )
        )
        
        // When
        val result = securityAnalyzer.analyzeNetworks(scanResults)
        
        // Then
        assertNotNull(result)
        assertEquals(2, result.totalNetworks)
        assertEquals(1, result.threats.size)
        assertEquals(ThreatType.DUPLICATE_SSID, result.threats.first().type)
        assertEquals(ThreatLevel.HIGH, result.overallRiskLevel)
    }
    
    @Test
    fun `analyzeNetworks should generate appropriate recommendations`() = runBlocking {
        // Given
        val scanResults = createTestScanResults()
        val threats = listOf(
            SecurityThreat(
                type = ThreatType.OPEN_NETWORK,
                severity = ThreatLevel.CRITICAL,
                description = "Open network",
                networkSsid = "OpenNetwork",
                networkBssid = "00:11:22:33:44:55"
            ),
            SecurityThreat(
                type = ThreatType.WEAK_ENCRYPTION,
                severity = ThreatLevel.HIGH,
                description = "Weak encryption",
                networkSsid = "WeakNetwork",
                networkBssid = "00:11:22:33:44:56"
            )
        )
        
        whenever(threatDetector.detectGlobalThreats(scanResults)).thenReturn(threats)
        whenever(encryptionAnalyzer.analyzeEncryption(any())).thenReturn(
            EncryptionAnalysis(
                securityType = SecurityType.OPEN,
                threats = threats,
                vulnerabilities = listOf("No encryption"),
                recommendations = listOf("Avoid open networks"),
                encryptionStrength = EncryptionStrength.NONE,
                isSecure = false
            )
        )
        
        // When
        val result = securityAnalyzer.analyzeNetworks(scanResults)
        
        // Then
        assertNotNull(result)
        assertTrue(result.recommendations.isNotEmpty())
        assertTrue(result.recommendations.any { it.contains("открытых") })
        assertTrue(result.recommendations.any { it.contains("WEP") || it.contains("WPA") })
    }
    
    private fun createTestScanResults(): List<WifiScanResult> {
        return listOf(
            WifiScanResult(
                ssid = "OpenNetwork",
                bssid = "00:11:22:33:44:55",
                capabilities = "",
                frequency = 2412,
                level = -50,
                securityType = SecurityType.OPEN,
                threatLevel = ThreatLevel.CRITICAL,
                isConnected = false,
                isHidden = false,
                vendor = "TestVendor",
                channel = 1,
                standard = WifiStandard.WIFI_2_4_GHZ
            ),
            WifiScanResult(
                ssid = "SecureNetwork",
                bssid = "00:11:22:33:44:56",
                capabilities = "WPA2-PSK-CCMP",
                frequency = 5180,
                level = -60,
                securityType = SecurityType.WPA2,
                threatLevel = ThreatLevel.SAFE,
                isConnected = false,
                isHidden = false,
                vendor = "TestVendor",
                channel = 36,
                standard = WifiStandard.WIFI_5_GHZ
            ),
            WifiScanResult(
                ssid = "WeakNetwork",
                bssid = "00:11:22:33:44:57",
                capabilities = "WEP",
                frequency = 2437,
                level = -70,
                securityType = SecurityType.WEP,
                threatLevel = ThreatLevel.HIGH,
                isConnected = false,
                isHidden = false,
                vendor = "TestVendor",
                channel = 6,
                standard = WifiStandard.WIFI_2_4_GHZ
            )
        )
    }
    
    private fun createDuplicateSsidScanResults(): List<WifiScanResult> {
        return listOf(
            WifiScanResult(
                ssid = "TestNetwork",
                bssid = "00:11:22:33:44:55",
                capabilities = "WPA2-PSK-CCMP",
                frequency = 2412,
                level = -50,
                securityType = SecurityType.WPA2,
                threatLevel = ThreatLevel.SAFE,
                isConnected = false,
                isHidden = false,
                vendor = "TestVendor",
                channel = 1,
                standard = WifiStandard.WIFI_2_4_GHZ
            ),
            WifiScanResult(
                ssid = "TestNetwork",
                bssid = "00:11:22:33:44:56",
                capabilities = "WPA2-PSK-CCMP",
                frequency = 2412,
                level = -60,
                securityType = SecurityType.WPA2,
                threatLevel = ThreatLevel.SAFE,
                isConnected = false,
                isHidden = false,
                vendor = "TestVendor",
                channel = 1,
                standard = WifiStandard.WIFI_2_4_GHZ
            )
        )
    }
}
