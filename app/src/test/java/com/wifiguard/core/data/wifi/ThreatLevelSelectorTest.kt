package com.wifiguard.core.data.wifi

import com.wifiguard.core.data.local.entity.ThreatEntity
import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.security.ThreatType
import org.junit.Assert.assertEquals
import org.junit.Test

class ThreatLevelSelectorTest {
    @Test
    fun `calculateMaxThreatLevel - выбирает максимальную severity из угроз`() {
        val threats = listOf(
            createThreat(severity = ThreatLevel.LOW),
            createThreat(severity = ThreatLevel.CRITICAL),
            createThreat(severity = ThreatLevel.MEDIUM),
        )

        val result = ThreatLevelSelector.calculateMaxThreatLevel(
            unresolvedThreats = threats,
            securityType = SecurityType.WPA2
        )

        assertEquals(ThreatLevel.CRITICAL, result)
    }

    @Test
    fun `calculateMaxThreatLevel - fallback по securityType если угроз нет`() {
        val result = ThreatLevelSelector.calculateMaxThreatLevel(
            unresolvedThreats = emptyList(),
            securityType = SecurityType.WPA2
        )

        assertEquals(ThreatLevel.LOW, result)
    }

    @Test
    fun `calculateMaxThreatLevel - fallback HIGH если securityType UNKNOWN`() {
        val result = ThreatLevelSelector.calculateMaxThreatLevel(
            unresolvedThreats = emptyList(),
            securityType = SecurityType.UNKNOWN
        )

        assertEquals(ThreatLevel.HIGH, result)
    }

    private fun createThreat(severity: ThreatLevel): ThreatEntity {
        return ThreatEntity(
            id = 1L,
            scanId = 1L,
            threatType = ThreatType.UNKNOWN_THREAT,
            severity = severity,
            description = "Тестовая угроза",
            networkSsid = "TestNetwork",
            networkBssid = "AA:BB:CC:DD:EE:FF",
            additionalInfo = null,
            timestamp = 0L,
            isResolved = false,
            resolutionTimestamp = null,
            resolutionNote = null,
            isNotified = false
        )
    }
}












