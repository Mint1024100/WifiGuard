package com.wifiguard.core.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BssidValidatorTest {
    @Test
    fun `isValidForStorage - отклоняет пустые и плейсхолдеры`() {
        assertFalse(BssidValidator.isValidForStorage(""))
        assertFalse(BssidValidator.isValidForStorage("   "))
        assertFalse(BssidValidator.isValidForStorage("unknown"))
        assertFalse(BssidValidator.isValidForStorage("UNKNOWN"))
        assertFalse(BssidValidator.isValidForStorage("02:00:00:00:00:00"))
    }

    @Test
    fun `isValidForStorage - принимает нормальный MAC`() {
        assertTrue(BssidValidator.isValidForStorage("AA:BB:CC:DD:EE:FF"))
    }
}












