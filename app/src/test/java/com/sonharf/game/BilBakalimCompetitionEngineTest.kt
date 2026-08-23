package com.sonharf.game

import org.junit.Assert.*
import org.junit.Test

class BilBakalimCompetitionEngineTest {
    @Test fun finalRiskStacksWithoutPayToWinPower() {
        val result = BilBakalimCompetitionEngine.resolve(100.0, 130.0, 100.0, BilRoundRules(10, 20, riskEnabled = true))
        assertTrue(result.won)
        assertEquals(60, result.points)
        assertEquals("Final Ustası", result.title)
    }

    @Test fun bossAddsFiveBeforeMultiplier() {
        val result = BilBakalimCompetitionEngine.resolve(10.0, 20.0, 10.0, BilRoundRules(5, 0))
        assertEquals(15, result.points)
        assertEquals("Boss Avcısı", result.title)
    }

    @Test fun lossNeverAwardsPoints() {
        val result = BilBakalimCompetitionEngine.resolve(null, 10.0, 10.0, BilRoundRules(1, 20, true))
        assertFalse(result.won)
        assertEquals(0, result.points)
    }

    @Test fun leagueBoundariesAreStable() {
        assertEquals("BRONZ", BilBakalimCompetitionEngine.league(999))
        assertEquals("GÜMÜŞ", BilBakalimCompetitionEngine.league(1000))
        assertEquals("ALTIN", BilBakalimCompetitionEngine.league(1200))
        assertEquals("ELMAS", BilBakalimCompetitionEngine.league(1400))
        assertEquals("BİLGE", BilBakalimCompetitionEngine.league(1600))
    }
}
