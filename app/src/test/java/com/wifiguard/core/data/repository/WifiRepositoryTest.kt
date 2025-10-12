package com.wifiguard.core.data.repository

import com.wifiguard.core.data.local.dao.WifiNetworkDao
import com.wifiguard.core.data.local.dao.WifiScanDao
import com.wifiguard.core.domain.model.WifiNetwork
import com.wifiguard.core.domain.model.WifiScanResult
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for WifiRepositoryImpl
 */
class WifiRepositoryTest {
    
    private lateinit var wifiNetworkDao: WifiNetworkDao
    private lateinit var wifiScanDao: WifiScanDao
    private lateinit var wifiRepository: WifiRepositoryImpl
    
    @Before
    fun setup() {
        wifiNetworkDao = mockk()
        wifiScanDao = mockk()
        wifiRepository = WifiRepositoryImpl(wifiNetworkDao, wifiScanDao)
    }
    
    @Test
    fun `getAllNetworks returns flow from dao`() = runTest {
        // Given
        val expectedNetworks = listOf(
            WifiNetwork(
                ssid = "TestNetwork",
                bssid = "00:11:22:33:44:55",
                securityType = com.wifiguard.core.domain.model.SecurityType.WPA2,
                signalLevel = -50,
                frequency = 2400,
                isConnected = false,
                isHidden = false,
                lastSeen = System.currentTimeMillis()
            )
        )
        every { wifiNetworkDao.getAllNetworks() } returns flowOf(expectedNetworks)
        
        // When
        val result = wifiRepository.getAllNetworks().toList()
        
        // Then
        assertEquals(1, result.size)
        assertEquals(expectedNetworks, result[0])
        verify { wifiNetworkDao.getAllNetworks() }
    }
    
    @Test
    fun `getNetworkBySSID returns network from dao`() = runTest {
        // Given
        val ssid = "TestNetwork"
        val expectedNetwork = WifiNetwork(
            ssid = ssid,
            bssid = "00:11:22:33:44:55",
            securityType = com.wifiguard.core.domain.model.SecurityType.WPA2,
            signalLevel = -50,
            frequency = 2400,
            isConnected = false,
            isHidden = false,
            lastSeen = System.currentTimeMillis()
        )
        coEvery { wifiNetworkDao.getNetworkBySSID(ssid) } returns expectedNetwork
        
        // When
        val result = wifiRepository.getNetworkBySSID(ssid)
        
        // Then
        assertEquals(expectedNetwork, result)
        coVerify { wifiNetworkDao.getNetworkBySSID(ssid) }
    }
    
    @Test
    fun `insertNetwork calls dao insert`() = runTest {
        // Given
        val network = WifiNetwork(
            ssid = "TestNetwork",
            bssid = "00:11:22:33:44:55",
            securityType = com.wifiguard.core.domain.model.SecurityType.WPA2,
            signalLevel = -50,
            frequency = 2400,
            isConnected = false,
            isHidden = false,
            lastSeen = System.currentTimeMillis()
        )
        coEvery { wifiNetworkDao.insertNetwork(network) } just Runs
        
        // When
        wifiRepository.insertNetwork(network)
        
        // Then
        coVerify { wifiNetworkDao.insertNetwork(network) }
    }
    
    @Test
    fun `updateNetwork calls dao update`() = runTest {
        // Given
        val network = WifiNetwork(
            ssid = "TestNetwork",
            bssid = "00:11:22:33:44:55",
            securityType = com.wifiguard.core.domain.model.SecurityType.WPA2,
            signalLevel = -50,
            frequency = 2400,
            isConnected = false,
            isHidden = false,
            lastSeen = System.currentTimeMillis()
        )
        coEvery { wifiNetworkDao.updateNetwork(network) } just Runs
        
        // When
        wifiRepository.updateNetwork(network)
        
        // Then
        coVerify { wifiNetworkDao.updateNetwork(network) }
    }
    
    @Test
    fun `deleteNetwork calls dao delete`() = runTest {
        // Given
        val network = WifiNetwork(
            ssid = "TestNetwork",
            bssid = "00:11:22:33:44:55",
            securityType = com.wifiguard.core.domain.model.SecurityType.WPA2,
            signalLevel = -50,
            frequency = 2400,
            isConnected = false,
            isHidden = false,
            lastSeen = System.currentTimeMillis()
        )
        coEvery { wifiNetworkDao.deleteNetwork(network) } just Runs
        
        // When
        wifiRepository.deleteNetwork(network)
        
        // Then
        coVerify { wifiNetworkDao.deleteNetwork(network) }
    }
    
    @Test
    fun `getLatestScans returns flow from dao`() = runTest {
        // Given
        val limit = 10
        val expectedScans = listOf(
            WifiScanResult(
                ssid = "TestNetwork",
                bssid = "00:11:22:33:44:55",
                capabilities = "[WPA2-PSK-CCMP]",
                frequency = 2400,
                level = -50,
                timestamp = System.currentTimeMillis(),
                securityType = com.wifiguard.core.domain.model.SecurityType.WPA2,
                threatLevel = com.wifiguard.core.domain.model.ThreatLevel.SAFE,
                isConnected = false,
                isHidden = false,
                vendor = "TestVendor",
                channel = 6,
                standard = com.wifiguard.core.domain.model.WifiStandard.WIFI_5
            )
        )
        every { wifiScanDao.getLatestScans(limit) } returns flowOf(expectedScans)
        
        // When
        val result = wifiRepository.getLatestScans(limit).toList()
        
        // Then
        assertEquals(1, result.size)
        assertEquals(expectedScans, result[0])
        verify { wifiScanDao.getLatestScans(limit) }
    }
    
    @Test
    fun `insertScanResult calls dao insert`() = runTest {
        // Given
        val scanResult = WifiScanResult(
            ssid = "TestNetwork",
            bssid = "00:11:22:33:44:55",
            capabilities = "[WPA2-PSK-CCMP]",
            frequency = 2400,
            level = -50,
            timestamp = System.currentTimeMillis(),
            securityType = com.wifiguard.core.domain.model.SecurityType.WPA2,
            threatLevel = com.wifiguard.core.domain.model.ThreatLevel.SAFE,
            isConnected = false,
            isHidden = false,
            vendor = "TestVendor",
            channel = 6,
            standard = com.wifiguard.core.domain.model.WifiStandard.WIFI_5
        )
        coEvery { wifiScanDao.insertScanResult(scanResult) } just Runs
        
        // When
        wifiRepository.insertScanResult(scanResult)
        
        // Then
        coVerify { wifiScanDao.insertScanResult(scanResult) }
    }
    
    @Test
    fun `clearOldScans calls dao clear`() = runTest {
        // Given
        val olderThanMillis = System.currentTimeMillis() - 86400000L // 1 day ago
        coEvery { wifiScanDao.clearOldScans(olderThanMillis) } just Runs
        
        // When
        wifiRepository.clearOldScans(olderThanMillis)
        
        // Then
        coVerify { wifiScanDao.clearOldScans(olderThanMillis) }
    }
    
    @Test
    fun `markNetworkAsSuspicious updates network with suspicious flag`() = runTest {
        // Given
        val ssid = "TestNetwork"
        val reason = "Suspicious activity detected"
        val originalNetwork = WifiNetwork(
            ssid = ssid,
            bssid = "00:11:22:33:44:55",
            securityType = com.wifiguard.core.domain.model.SecurityType.WPA2,
            signalLevel = -50,
            frequency = 2400,
            isConnected = false,
            isHidden = false,
            lastSeen = System.currentTimeMillis(),
            isSuspicious = false,
            suspiciousReason = null
        )
        val expectedUpdatedNetwork = originalNetwork.copy(
            isSuspicious = true,
            suspiciousReason = reason,
            lastUpdated = any()
        )
        
        coEvery { wifiNetworkDao.getNetworkBySSID(ssid) } returns originalNetwork
        coEvery { wifiNetworkDao.updateNetwork(any()) } just Runs
        
        // When
        wifiRepository.markNetworkAsSuspicious(ssid, reason)
        
        // Then
        coVerify { wifiNetworkDao.getNetworkBySSID(ssid) }
        coVerify { 
            wifiNetworkDao.updateNetwork(match { network ->
                network.isSuspicious == true && 
                network.suspiciousReason == reason &&
                network.lastUpdated > 0
            })
        }
    }
    
    @Test
    fun `markNetworkAsSuspicious does nothing when network not found`() = runTest {
        // Given
        val ssid = "NonExistentNetwork"
        val reason = "Suspicious activity detected"
        
        coEvery { wifiNetworkDao.getNetworkBySSID(ssid) } returns null
        
        // When
        wifiRepository.markNetworkAsSuspicious(ssid, reason)
        
        // Then
        coVerify { wifiNetworkDao.getNetworkBySSID(ssid) }
        coVerify(exactly = 0) { wifiNetworkDao.updateNetwork(any()) }
    }
}
