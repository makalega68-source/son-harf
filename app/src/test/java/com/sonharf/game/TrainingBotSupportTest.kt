package com.sonharf.game

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingBotSupportTest {
    @Test
    fun ownershipIsAlwaysRelativeToViewer() {
        assertEquals(WordSiegeOwnershipRelation.SELF, TrainingBotSupport.ownershipRelation(1, 1))
        assertEquals(WordSiegeOwnershipRelation.OPPONENT, TrainingBotSupport.ownershipRelation(2, 1))
        assertEquals(WordSiegeOwnershipRelation.OPPONENT, TrainingBotSupport.ownershipRelation(1, 2))
        assertEquals(WordSiegeOwnershipRelation.SELF, TrainingBotSupport.ownershipRelation(2, 2))
        assertEquals(WordSiegeOwnershipRelation.NEUTRAL, TrainingBotSupport.ownershipRelation(0, 2))
    }

    @Test
    fun ownershipFillsKeepBlackLettersHighlyReadable() {
        assertTrue(TrainingBotSupport.blackContrastRatio(TrainingBotSupport.OWN_FILL_ARGB) >= 7.0)
        assertTrue(TrainingBotSupport.blackContrastRatio(TrainingBotSupport.OPPONENT_FILL_ARGB) >= 7.0)
    }

    @Test
    fun botNamesAreNaturalMixedAndAvoidImmediateRepeat() {
        assertTrue(TrainingBotSupport.turkishBotNames.containsAll(listOf("Elif", "Zeynep", "Emir", "Mert")))
        var previous = "Elif"
        repeat(200) { seed ->
            val next = TrainingBotSupport.chooseBotName(previous, Random(seed))
            assertNotEquals(previous, next)
            previous = next
        }
    }

    @Test
    fun reactionTimingReflectsDifficultyWithoutBeingInstant() {
        val easy = TrainingBotSupport.reactionDelayMs(TrainingBotDifficulty.EASY, 3, Random(7))
        val medium = TrainingBotSupport.reactionDelayMs(TrainingBotDifficulty.MEDIUM, 3, Random(7))
        val hard = TrainingBotSupport.reactionDelayMs(TrainingBotDifficulty.HARD, 3, Random(7))
        assertTrue(easy > medium)
        assertTrue(medium > hard)
        assertTrue(hard >= 800)
    }
}
