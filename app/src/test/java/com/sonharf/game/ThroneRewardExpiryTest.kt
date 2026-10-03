package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThroneRewardExpiryTest {
    @Test fun expiryUsesServerRemainingTimeDespiteClientClockSkew() {
        assertEquals(8_000L, throneRewardDeadline("2026-10-04T20:59:55Z", "2026-10-04T21:00:00Z", 3_000L))
    }
    @Test fun permanentFrameHasNoDeadline() {
        assertNull(throneRewardDeadline("2026-10-04T20:59:55Z", null, 3_000L))
    }
    @Test fun expiredOrMalformedRewardNeverGetsExtraTime() {
        assertEquals(2_000L, throneRewardDeadline("2026-10-04T21:00:01Z", "2026-10-04T21:00:00Z", 3_000L))
        assertEquals(3_000L, throneRewardDeadline(null, "invalid", 3_000L))
    }
}
