package com.sonharf.game

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class PremierTurnClockTest {
    private val now = Instant.parse("2026-09-09T10:00:00Z")

    @Test
    fun countdownUsesCeilingSoLiveTurnNeverShowsZeroEarly() {
        assertEquals(20, premierRemainingTurnSeconds(now.plusMillis(20_000), now))
        assertEquals(20, premierRemainingTurnSeconds(now.plusMillis(19_001), now))
        assertEquals(2, premierRemainingTurnSeconds(now.plusMillis(1_001), now))
        assertEquals(1, premierRemainingTurnSeconds(now.plusMillis(999), now))
        assertEquals(1, premierRemainingTurnSeconds(now.plusMillis(1), now))
    }

    @Test
    fun expiredDeadlineReturnsZeroForAuthoritativeTimeoutPath() {
        assertEquals(0, premierRemainingTurnSeconds(now, now))
        assertEquals(0, premierRemainingTurnSeconds(now.minusMillis(1), now))
    }
}
