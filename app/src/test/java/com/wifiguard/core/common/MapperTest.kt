package com.wifiguard.core.common

import com.wifiguard.core.data.local.entity.WifiNetworkEntity
import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.domain.model.TrustLevel
import com.wifiguard.core.domain.model.WifiNetwork
import org.junit.Assert.assertEquals
import org.junit.Test

class MapperTest {

    private val wifiNetworkEntityToDomainMapper = WifiNetworkEntityToDomainMapper()
    private val wifiNetworkDomainToEntityMapper = WifiNetworkDomainToEntityMapper()

    @Test
    fun `WifiNetworkEntityToDomainMapper maps correctly`() {
        // Given
        val entity = WifiNetworkEntity(
            bssid = "00:11:22:33:44:55",
            ssid = "TestNetwork",
            frequency = 2412,
            signalStrength = -50,
            securityType = SecurityType.WPA2,
            channel = 1,
            threatLevel = ThreatLevel.LOW,
            isHidden = false,
            firstSeen = 1000L,
            lastSeen = 2000L,
            detectionCount = 5,
            isSuspicious = false,
            vendor = "TestVendor",
            notes = "None"
        )

        // When
        val domain = wifiNetworkEntityToDomainMapper.map(entity)

        // Then
        assertEquals(entity.ssid, domain.ssid)
        assertEquals(entity.bssid, domain.bssid)
        assertEquals(entity.securityType, domain.securityType)
        assertEquals(entity.signalStrength, domain.signalStrength)
        assertEquals(entity.frequency, domain.frequency)
        assertEquals(entity.channel, domain.channel)
        assertEquals(entity.vendor, domain.vendor)
        assertEquals(entity.firstSeen, domain.firstSeen)
        assertEquals(entity.lastSeen, domain.lastSeen)
    }

    @Test
    fun `WifiNetworkDomainToEntityMapper maps correctly`() {
        // Given
        val domain = WifiNetwork(
            ssid = "TestNetwork",
            bssid = "00:11:22:33:44:55",
            securityType = SecurityType.WPA2,
            signalStrength = -50,
            frequency = 2412,
            channel = 1,
            vendor = "TestVendor",
            firstSeen = 1000L,
            lastSeen = 2000L,
            lastUpdated = 2000L,
            isSuspicious = false,
            suspiciousReason = "None",
            connectionCount = 5,
            isKnown = true,
            trustLevel = TrustLevel.KNOWN
        )

        // When
        val entity = wifiNetworkDomainToEntityMapper.map(domain)

        // Then
        assertEquals(domain.ssid, entity.ssid)
        assertEquals(domain.bssid, entity.bssid)
        assertEquals(domain.securityType, entity.securityType)
        assertEquals(domain.signalStrength, entity.signalStrength)
        assertEquals(domain.frequency, entity.frequency)
        assertEquals(domain.channel, entity.channel)
    }
}














