package com.sonharf.game

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class PremierTurnClockTest {
    private val now = Instant.parse("2026-09-09T10:00:00Z")

    @Test
    fun countdownUsesCeilingSoLiveTurnNeverShowsZeroEarly() {
        assertEquals(15, premierRemainingTurnSeconds(now.plusMillis(20_000), now))
        assertEquals(15, premierRemainingTurnSeconds(now.plusMillis(15_000), now))
        assertEquals(15, premierRemainingTurnSeconds(now.plusMillis(14_999), now))
        assertEquals(11, premierRemainingTurnSeconds(now.plusMillis(10_001), now))
        assertEquals(10, premierRemainingTurnSeconds(now.plusMillis(10_000), now))
        assertEquals(2, premierRemainingTurnSeconds(now.plusMillis(1_001), now))
        assertEquals(1, premierRemainingTurnSeconds(now.plusMillis(999), now))
        assertEquals(1, premierRemainingTurnSeconds(now.plusMillis(1), now))
    }


    @Test
    fun roundCapsAreExactlyFifteenThirteenEleven() {
        assertEquals(15, premierTurnSecondsForRound(1))
        assertEquals(13, premierTurnSecondsForRound(2))
        assertEquals(11, premierTurnSecondsForRound(3))
        assertEquals(11, premierTurnSecondsForRound(4))
        assertEquals(13, premierRemainingTurnSecondsFromMillis(20_000, 13))
        assertEquals(11, premierRemainingTurnSecondsFromMillis(20_000, 11))
        assertEquals(13, premierRemainingTurnSeconds(now.plusMillis(20_000), now, 13))
        assertEquals(11, premierRemainingTurnSeconds(now.plusMillis(20_000), now, 11))
    }

    @Test
    fun expiredDeadlineReturnsZeroForAuthoritativeTimeoutPath() {
        assertEquals(0, premierRemainingTurnSeconds(now, now))
        assertEquals(0, premierRemainingTurnSeconds(now.minusMillis(1), now))
    }
}
