package com.wifiguard.core.data.wifi

import com.wifiguard.core.domain.model.ThreatLevel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CriticalWifiProtectionTest {

    @Test
    fun `shouldAttemptAutoDisable - critical + consent true - returns true`() {
        assertTrue(
            CriticalWifiProtection.shouldAttemptAutoDisable(
                threatLevel = ThreatLevel.CRITICAL,
                userConsentEnabled = true
            )
        )
    }

    @Test
    fun `shouldAttemptAutoDisable - critical + consent false - returns false`() {
        assertFalse(
            CriticalWifiProtection.shouldAttemptAutoDisable(
                threatLevel = ThreatLevel.CRITICAL,
                userConsentEnabled = false
            )
        )
    }

    @Test
    fun `shouldAttemptAutoDisable - non critical + consent true - returns false`() {
        assertFalse(
            CriticalWifiProtection.shouldAttemptAutoDisable(
                threatLevel = ThreatLevel.HIGH,
                userConsentEnabled = true
            )
        )
    }

    @Test
    fun `canDisableWifiBySdk - api 28 - true`() {
        assertTrue(CriticalWifiProtection.canDisableWifiBySdk(28))
    }

    @Test
    fun `canDisableWifiBySdk - api 29 - false`() {
        assertFalse(CriticalWifiProtection.canDisableWifiBySdk(29))
    }
}


