package com.wifiguard.core.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationThrottleTest {
    @Test
    fun `shouldShow - разрешает первое уведомление и блокирует до истечения интервала`() {
        var now = 0L
        val throttle = NotificationThrottle(
            intervalMs = 1000L,
            nowMs = { now },
            cleanupWindowMs = 60 * 60 * 1000L
        )

        val key = "AA:BB:CC:DD:EE:FF:HIGH"

        assertTrue(throttle.shouldShow(key))

        throttle.markShown(key)
        assertFalse(throttle.shouldShow(key))

        now = 999L
        assertFalse(throttle.shouldShow(key))

        now = 1000L
        assertTrue(throttle.shouldShow(key))
    }
}












