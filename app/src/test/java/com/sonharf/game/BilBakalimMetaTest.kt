package com.sonharf.game

import org.junit.Assert.*
import org.junit.Test

class BilBakalimMetaTest {
    @Test fun masteryIsPercentBased() { assertEquals(75, BilBakalimCompetitionEngine.categoryMastery(3,4)) }
    @Test fun dailyChallengeAlwaysFitsDeck() {
        val i = BilBakalimCompetitionEngine.dailyChallenge(235)
        assertTrue(i in bilBakalimQuestions.indices)
    }
    @Test fun jokerIsHintOnly() {
        val range = BilBakalimCompetitionEngine.jokerHint(100.0)
        assertEquals(80.0, range.first, 0.001)
        assertEquals(120.0, range.second, 0.001)
    }
}
