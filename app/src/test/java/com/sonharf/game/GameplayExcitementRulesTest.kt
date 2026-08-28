package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Test

class GameplayExcitementRulesTest {
    @Test fun coreSonHarfRulesStayLocked() {
        assertEquals(45, GameplayExcitementRules.TURN_SECONDS)
        assertEquals(3, GameplayExcitementRules.CORRECT_WORD_POINTS)
        assertEquals(-1, GameplayExcitementRules.INVALID_WORD_POINTS)
        assertEquals(5, GameplayExcitementRules.STREAK_TARGET)
        assertEquals(3, GameplayExcitementRules.STREAK_BONUS_POINTS)
    }

    @Test fun triviaMilestoneCountdownIsStable() {
        assertEquals(5, GameplayExcitementRules.wordsToNextTrivia(0))
        assertEquals(1, GameplayExcitementRules.wordsToNextTrivia(4))
        assertEquals(5, GameplayExcitementRules.wordsToNextTrivia(5))
    }
}
