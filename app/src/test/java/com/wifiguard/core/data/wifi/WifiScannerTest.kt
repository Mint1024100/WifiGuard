package com.wifiguard.core.data.wifi

import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.domain.model.WifiStandard
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Тесты для WifiScanner
 */
@RunWith(MockitoJUnitRunner::class)
class WifiScannerTest {
    
    private lateinit var context: Context
    private lateinit var wifiManager: WifiManager
    private lateinit var wifiScanner: WifiScanner
    
    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        wifiManager = mockk(relaxed = true)
        
        every { context.getSystemService(Context.WIFI_SERVICE) } returns wifiManager
        every { wifiManager.isWifiEnabled } returns true
        
        wifiScanner = WifiScannerImpl(context)
    }
    
    @Test
    fun `isWifiEnabled should return true when wifi is enabled`() {
        // Given
        every { wifiManager.isWifiEnabled } returns true
        
        // When
        val result = wifiScanner.isWifiEnabled()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `isWifiEnabled should return false when wifi is disabled`() {
        // Given
        every { wifiManager.isWifiEnabled } returns false
        
        // When
        val result = wifiScanner.isWifiEnabled()
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `startScan should return success when scan is successful`() = runTest {
        // Given
        val scanResults = createMockScanResults()
        every { wifiManager.startScan() } returns true
        every { wifiManager.scanResults } returns scanResults
        
        // When
        val result = wifiScanner.startScan()
        
        // Then
        assertTrue(result.isSuccess)
        val scanResultsList = result.getOrNull()
        assertNotNull(scanResultsList)
        assertEquals(2, scanResultsList.size)
        
        verify { wifiManager.startScan() }
    }
    
    @Test
    fun `startScan should return failure when wifi is disabled`() = runTest {
        // Given
        every { wifiManager.isWifiEnabled } returns false
        
        // When
        val result = wifiScanner.startScan()
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Wi-Fi отключен") == true)
    }
    
    @Test
    fun `startScan should return failure when scan fails`() = runTest {
        // Given
        every { wifiManager.startScan() } returns false
        
        // When
        val result = wifiScanner.startScan()
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Не удалось запустить сканирование") == true)
    }
    
    @Test
    fun `getScanResultsFlow should emit results when scan is successful`() = runTest {
        // Given
        val scanResults = createMockScanResults()
        every { wifiManager.startScan() } returns true
        every { wifiManager.scanResults } returns scanResults
        
        // When
        val flow = wifiScanner.getScanResultsFlow()
        val results = flow.first()
        
        // Then
        assertNotNull(results)
        assertEquals(2, results.size)
        
        val firstResult = results.first()
        assertEquals("TestNetwork", firstResult.ssid)
        assertEquals("00:11:22:33:44:55", firstResult.bssid)
        assertEquals(SecurityType.WPA2, firstResult.securityType)
        assertEquals(ThreatLevel.SAFE, firstResult.threatLevel)
    }
    
    @Test
    fun `getCurrentNetwork should return null when not connected`() = runTest {
        // Given
        val wifiInfo = mockk<android.net.wifi.WifiInfo>(relaxed = true)
        every { wifiManager.connectionInfo } returns wifiInfo
        every { wifiInfo.ssid } returns null
        
        // When
        val result = wifiScanner.getCurrentNetwork()
        
        // Then
        assertEquals(null, result)
    }
    
    @Test
    fun `getCurrentNetwork should return connected network when connected`() = runTest {
        // Given
        val wifiInfo = mockk<android.net.wifi.WifiInfo>(relaxed = true)
        val scanResults = createMockScanResults()
        
        every { wifiManager.connectionInfo } returns wifiInfo
        every { wifiInfo.ssid } returns "\"TestNetwork\""
        every { wifiInfo.bssid } returns "00:11:22:33:44:55"
        every { wifiInfo.rssi } returns -50
        every { wifiManager.scanResults } returns scanResults
        
        // When
        val result = wifiScanner.getCurrentNetwork()
        
        // Then
        assertNotNull(result)
        assertEquals("TestNetwork", result.ssid)
        assertEquals("00:11:22:33:44:55", result.bssid)
        assertTrue(result.isConnected)
    }
    
    @Test
    fun `startContinuousScan should emit results periodically`() = runTest {
        // Given
        val scanResults = createMockScanResults()
        every { wifiManager.startScan() } returns true
        every { wifiManager.scanResults } returns scanResults
        
        // When
        val flow = wifiScanner.startContinuousScan(100) // 100ms interval for testing
        val results = flow.take(2).toList()
        
        // Then
        assertEquals(2, results.size)
        results.forEach { result ->
            assertEquals(2, result.size)
        }
    }
    
    private fun createMockScanResults(): List<ScanResult> {
        val scanResult1 = mockk<ScanResult>(relaxed = true)
        every { scanResult1.SSID } returns "TestNetwork"
        every { scanResult1.BSSID } returns "00:11:22:33:44:55"
        every { scanResult1.capabilities } returns "WPA2-PSK-CCMP"
        every { scanResult1.frequency } returns 2412
        every { scanResult1.level } returns -50
        
        val scanResult2 = mockk<ScanResult>(relaxed = true)
        every { scanResult2.SSID } returns "OpenNetwork"
        every { scanResult2.BSSID } returns "00:11:22:33:44:56"
        every { scanResult2.capabilities } returns ""
        every { scanResult2.frequency } returns 2437
        every { scanResult2.level } returns -60
        
        return listOf(scanResult1, scanResult2)
    }
}
