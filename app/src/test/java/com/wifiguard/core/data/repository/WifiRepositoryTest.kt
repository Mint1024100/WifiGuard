package com.wifiguard.core.data.repository

import com.wifiguard.core.common.WifiNetworkDomainToEntityMapper
import com.wifiguard.core.common.WifiNetworkEntityToDomainMapper
import com.wifiguard.core.common.WifiScanDomainToEntityMapper
import com.wifiguard.core.common.WifiScanEntityToDomainMapper
import com.wifiguard.core.data.local.dao.ScanSessionDao
import com.wifiguard.core.data.local.dao.ThreatDao
import com.wifiguard.core.data.local.dao.WifiNetworkDao
import com.wifiguard.core.data.local.dao.WifiScanDao
import com.wifiguard.core.data.local.entity.WifiNetworkEntity
import com.wifiguard.core.data.local.entity.WifiScanEntity
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
    
    private lateinit var database: com.wifiguard.core.data.local.WifiGuardDatabase
    private lateinit var wifiNetworkDao: WifiNetworkDao
    private lateinit var wifiScanDao: WifiScanDao
    private lateinit var threatDao: ThreatDao
    private lateinit var scanSessionDao: ScanSessionDao
    private lateinit var wifiNetworkEntityToDomainMapper: WifiNetworkEntityToDomainMapper
    private lateinit var wifiNetworkDomainToEntityMapper: WifiNetworkDomainToEntityMapper
    private lateinit var wifiScanEntityToDomainMapper: WifiScanEntityToDomainMapper
    private lateinit var wifiScanDomainToEntityMapper: WifiScanDomainToEntityMapper
    private lateinit var wifiRepository: WifiRepositoryImpl
    
    @Before
    fun setup() {
        database = mockk()
        wifiNetworkDao = mockk()
        wifiScanDao = mockk()
        threatDao = mockk()
        scanSessionDao = mockk()
        wifiNetworkEntityToDomainMapper = mockk()
        wifiNetworkDomainToEntityMapper = mockk()
        wifiScanEntityToDomainMapper = mockk()
        wifiScanDomainToEntityMapper = mockk()
        
        wifiRepository = WifiRepositoryImpl(
            database = database,
            wifiNetworkDao = wifiNetworkDao,
            wifiScanDao = wifiScanDao,
            threatDao = threatDao,
            scanSessionDao = scanSessionDao,
            wifiNetworkEntityToDomainMapper = wifiNetworkEntityToDomainMapper,
            wifiNetworkDomainToEntityMapper = wifiNetworkDomainToEntityMapper,
            wifiScanEntityToDomainMapper = wifiScanEntityToDomainMapper,
            wifiScanDomainToEntityMapper = wifiScanDomainToEntityMapper
        )
    }
    
    @Test
    fun `getAllNetworks returns flow from dao with mapper`() = runTest {
        // Given
        val entity = mockk<WifiNetworkEntity>()
        val expectedNetwork = WifiNetwork(
            ssid = "TestNetwork",
            bssid = "00:11:22:33:44:55",
            securityType = com.wifiguard.core.domain.model.SecurityType.WPA2,
            signalStrength = -50,
            frequency = 2400,
            channel = 6,
            firstSeen = System.currentTimeMillis(),
            lastSeen = System.currentTimeMillis()
        )
        every { wifiNetworkDao.getAllNetworks() } returns flowOf(listOf(entity))
        every { wifiNetworkEntityToDomainMapper.map(entity) } returns expectedNetwork
        
        // When
        val result = wifiRepository.getAllNetworks().toList()
        
        // Then
        assertEquals(1, result.size)
        assertEquals(listOf(expectedNetwork), result[0])
        verify { wifiNetworkDao.getAllNetworks() }
        verify { wifiNetworkEntityToDomainMapper.map(entity) }
    }
    
    @Test
    fun `getNetworkBySSID returns network from dao with mapper`() = runTest {
        // Given
        val ssid = "TestNetwork"
        val entity = mockk<WifiNetworkEntity>()
        val expectedNetwork = WifiNetwork(
            ssid = ssid,
            bssid = "00:11:22:33:44:55",
            securityType = com.wifiguard.core.domain.model.SecurityType.WPA2,
            signalStrength = -50,
            frequency = 2400,
            channel = 6,
            firstSeen = System.currentTimeMillis(),
            lastSeen = System.currentTimeMillis()
        )
        coEvery { wifiNetworkDao.getNetworkBySSID(ssid) } returns entity
        every { wifiNetworkEntityToDomainMapper.map(entity) } returns expectedNetwork
        
        // When
        val result = wifiRepository.getNetworkBySSID(ssid)
        
        // Then
        assertEquals(expectedNetwork, result)
        coVerify { wifiNetworkDao.getNetworkBySSID(ssid) }
        verify { wifiNetworkEntityToDomainMapper.map(entity) }
    }
    
    @Test
    fun `insertNetwork calls dao insert with entity mapper`() = runTest {
        // Given
        val network = WifiNetwork(
            ssid = "TestNetwork",
            bssid = "00:11:22:33:44:55",
            securityType = com.wifiguard.core.domain.model.SecurityType.WPA2,
            signalStrength = -50,
            frequency = 2400,
            channel = 6,
            firstSeen = System.currentTimeMillis(),
            lastSeen = System.currentTimeMillis()
        )
        val entity = mockk<WifiNetworkEntity>()
        every { wifiNetworkDomainToEntityMapper.map(network) } returns entity
        coEvery { wifiNetworkDao.insertNetwork(entity) } returns 1L
        
        // When
        wifiRepository.insertNetwork(network)
        
        // Then
        verify { wifiNetworkDomainToEntityMapper.map(network) }
        coVerify { wifiNetworkDao.insertNetwork(entity) }
    }
    
    @Test
    fun `insertNetwork handles errors correctly`() = runTest {
        // Given
        val network = WifiNetwork(
            ssid = "TestNetwork",
            bssid = "00:11:22:33:44:55",
            securityType = com.wifiguard.core.domain.model.SecurityType.WPA2,
            signalStrength = -50,
            frequency = 2400,
            channel = 6,
            firstSeen = System.currentTimeMillis(),
            lastSeen = System.currentTimeMillis()
        )
        val entity = mockk<WifiNetworkEntity>()
        every { wifiNetworkDomainToEntityMapper.map(network) } returns entity
        coEvery { wifiNetworkDao.insertNetwork(entity) } throws Exception("Database error")
        
        // When & Then
        try {
            wifiRepository.insertNetwork(network)
            fail("Expected exception was not thrown")
        } catch (e: Exception) {
            assertEquals("Database error", e.message)
        }
    }
    
    @Test
    fun `updateNetwork calls dao update with entity mapper`() = runTest {
        // Given
        val network = WifiNetwork(
            ssid = "TestNetwork",
            bssid = "00:11:22:33:44:55",
            securityType = com.wifiguard.core.domain.model.SecurityType.WPA2,
            signalStrength = -50,
            frequency = 2400,
            channel = 6,
            firstSeen = System.currentTimeMillis(),
            lastSeen = System.currentTimeMillis()
        )
        val entity = mockk<WifiNetworkEntity>()
        every { wifiNetworkDomainToEntityMapper.map(network) } returns entity
        coEvery { wifiNetworkDao.updateNetwork(entity) } returns Unit
        
        // When
        wifiRepository.updateNetwork(network)
        
        // Then
        verify { wifiNetworkDomainToEntityMapper.map(network) }
        coVerify { wifiNetworkDao.updateNetwork(entity) }
    }
    
    @Test
    fun `deleteNetwork calls dao delete with entity mapper`() = runTest {
        // Given
        val network = WifiNetwork(
            ssid = "TestNetwork",
            bssid = "00:11:22:33:44:55",
            securityType = com.wifiguard.core.domain.model.SecurityType.WPA2,
            signalStrength = -50,
            frequency = 2400,
            channel = 6,
            firstSeen = System.currentTimeMillis(),
            lastSeen = System.currentTimeMillis()
        )
        val entity = mockk<WifiNetworkEntity>()
        every { wifiNetworkDomainToEntityMapper.map(network) } returns entity
        coEvery { wifiNetworkDao.deleteNetwork(entity) } returns Unit
        
        // When
        wifiRepository.deleteNetwork(network)
        
        // Then
        verify { wifiNetworkDomainToEntityMapper.map(network) }
        coVerify { wifiNetworkDao.deleteNetwork(entity) }
    }
    
    @Test
    fun `getLatestScans returns flow from dao with mapper`() = runTest {
        // Given
        val limit = 10
        val entity = mockk<WifiScanEntity>()
        val expectedScan = WifiScanResult(
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
            standard = com.wifiguard.core.domain.model.WifiStandard.WIFI_5_GHZ
        )
        every { wifiScanDao.getLatestScans(limit) } returns flowOf(listOf(entity))
        every { wifiScanEntityToDomainMapper.map(entity) } returns expectedScan
        
        // When
        val result = wifiRepository.getLatestScans(limit).toList()
        
        // Then
        assertEquals(1, result.size)
        assertEquals(listOf(expectedScan), result[0])
        verify { wifiScanDao.getLatestScans(limit) }
        verify { wifiScanEntityToDomainMapper.map(entity) }
    }
    
    @Test
    fun `insertScanResult calls dao insert with entity mapper`() = runTest {
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
            standard = com.wifiguard.core.domain.model.WifiStandard.WIFI_5_GHZ
        )
        val entity = mockk<WifiScanEntity>()
        every { wifiScanDomainToEntityMapper.map(scanResult) } returns entity
        coEvery { wifiScanDao.insertScanResult(entity) } returns 1L
        
        // When
        wifiRepository.insertScanResult(scanResult)
        
        // Then
        verify { wifiScanDomainToEntityMapper.map(scanResult) }
        coVerify { wifiScanDao.insertScanResult(entity) }
    }
    
    @Test
    fun `insertScanResult handles errors correctly`() = runTest {
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
            standard = com.wifiguard.core.domain.model.WifiStandard.WIFI_5_GHZ
        )
        val entity = mockk<WifiScanEntity>()
        every { wifiScanDomainToEntityMapper.map(scanResult) } returns entity
        coEvery { wifiScanDao.insertScanResult(entity) } throws Exception("Database error")
        
        // When & Then
        try {
            wifiRepository.insertScanResult(scanResult)
            fail("Expected exception was not thrown")
        } catch (e: Exception) {
            assertEquals("Database error", e.message)
        }
    }
    
    @Test
    fun `clearOldScans calls dao clear`() = runTest {
        // Given
        val olderThanMillis = System.currentTimeMillis() - 86400000L // 1 day ago
        coEvery { wifiScanDao.clearOldScans(olderThanMillis) } returns Unit
        
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
        val entity = mockk<WifiNetworkEntity>()
        val originalNetwork = WifiNetwork(
            ssid = ssid,
            bssid = "00:11:22:33:44:55",
            securityType = com.wifiguard.core.domain.model.SecurityType.WPA2,
            signalStrength = -50,
            frequency = 2400,
            channel = 6,
            firstSeen = System.currentTimeMillis(),
            lastSeen = System.currentTimeMillis(),
            isSuspicious = false,
            suspiciousReason = null
        )
        
        coEvery { wifiNetworkDao.getNetworkBySSID(ssid) } returns entity
        every { wifiNetworkEntityToDomainMapper.map(entity) } returns originalNetwork
        every { wifiNetworkDomainToEntityMapper.map(any()) } returns entity
        coEvery { wifiNetworkDao.updateNetwork(entity) } returns Unit
        
        // When
        wifiRepository.markNetworkAsSuspicious(ssid, reason)
        
        // Then
        coVerify { wifiNetworkDao.getNetworkBySSID(ssid) }
        verify { wifiNetworkEntityToDomainMapper.map(entity) }
        verify { 
            wifiNetworkDomainToEntityMapper.map(match { network ->
                network.isSuspicious == true && 
                network.suspiciousReason == reason &&
                network.lastUpdated > 0
            })
        }
        coVerify { wifiNetworkDao.updateNetwork(entity) }
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
    
    @Test
    fun `clearAllData deletes all data from all tables`() = runTest {
        // Given
        coEvery { threatDao.deleteAllThreats() } returns Unit
        coEvery { wifiScanDao.deleteAllScans() } returns Unit
        coEvery { wifiNetworkDao.deleteAllNetworks() } returns Unit
        coEvery { scanSessionDao.deleteAllSessions() } returns Unit
        
        // When
        wifiRepository.clearAllData()
        
        // Then
        coVerify(exactly = 1) { threatDao.deleteAllThreats() }
        coVerify(exactly = 1) { wifiScanDao.deleteAllScans() }
        coVerify(exactly = 1) { wifiNetworkDao.deleteAllNetworks() }
        coVerify(exactly = 1) { scanSessionDao.deleteAllSessions() }
    }
    
    @Test
    fun `clearAllData handles errors correctly`() = runTest {
        // Given
        coEvery { threatDao.deleteAllThreats() } returns Unit
        coEvery { wifiScanDao.deleteAllScans() } throws Exception("Delete error")
        
        // When & Then
        try {
            wifiRepository.clearAllData()
            fail("Expected exception was not thrown")
        } catch (e: Exception) {
            assertEquals("Delete error", e.message)
        }
        
        coVerify(exactly = 1) { threatDao.deleteAllThreats() }
        coVerify(exactly = 1) { wifiScanDao.deleteAllScans() }
        coVerify(exactly = 0) { wifiNetworkDao.deleteAllNetworks() }
        coVerify(exactly = 0) { scanSessionDao.deleteAllSessions() }
    }
    
    @Test
    fun `validateDatabaseIntegrity returns true when all tables are accessible`() = runTest {
        // Given
        coEvery { threatDao.getTotalThreatsCount() } returns 10
        coEvery { wifiScanDao.getTotalScansCount() } returns 20
        coEvery { wifiNetworkDao.getAllWifiNetworksSuspend() } returns emptyList()
        coEvery { scanSessionDao.getTotalSessionsCount() } returns 5
        
        // When
        val result = wifiRepository.validateDatabaseIntegrity()
        
        // Then
        assertTrue(result)
        coVerify(exactly = 1) { threatDao.getTotalThreatsCount() }
        coVerify(exactly = 1) { wifiScanDao.getTotalScansCount() }
        coVerify(exactly = 1) { wifiNetworkDao.getAllWifiNetworksSuspend() }
        coVerify(exactly = 1) { scanSessionDao.getTotalSessionsCount() }
    }
    
    @Test
    fun `validateDatabaseIntegrity returns false when database error occurs`() = runTest {
        // Given
        coEvery { threatDao.getTotalThreatsCount() } throws Exception("Database error")
        
        // When
        val result = wifiRepository.validateDatabaseIntegrity()
        
        // Then
        assertFalse(result)
        coVerify(exactly = 1) { threatDao.getTotalThreatsCount() }
        coVerify(exactly = 0) { wifiScanDao.getTotalScansCount() }
    }
}
