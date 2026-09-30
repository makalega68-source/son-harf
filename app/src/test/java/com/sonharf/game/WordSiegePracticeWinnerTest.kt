package com.sonharf.game

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Test

class WordSiegePracticeWinnerTest {
    @Test
    fun normalFinishUsesDisplayedTotalScoreInsteadOfRawTerritoryCount() {
        var state = WordSiegePracticeEngine.newGame(random = Random(1)).copy(
            bag = "A".repeat(19),
            playerWordScore = 400,
            playerAreaScore = 35,
            botWordScore = 250,
            botAreaScore = 48,
            // Reproduce the bad-result shape: bot owns more cells, but the player has the higher
            // displayed/final score (435 vs 298).
            playerArea = 10,
            botArea = 20,
            currentOwner = 1,
            consecutivePasses = 0,
        )

        state = WordSiegePracticeEngine.pass(state, 1)
        state = WordSiegePracticeEngine.pass(state, 2)
        state = WordSiegePracticeEngine.pass(state, 1)
        state = WordSiegePracticeEngine.pass(state, 2)

        assertEquals("finished", state.status)
        assertEquals(435, WordSiegePracticeEngine.totalScore(state, 1))
        assertEquals(298, WordSiegePracticeEngine.totalScore(state, 2))
        assertEquals(1, state.winnerOwner)
    }

    @Test
    fun forcedForfeitWinnerStillOverridesHigherScore() {
        val state = WordSiegePracticeEngine.newGame(random = Random(2)).copy(
            playerWordScore = 500,
            playerAreaScore = 50,
            botWordScore = 10,
            botAreaScore = 0,
            currentOwner = 1,
        )

        val finished = WordSiegePracticeEngine.forfeit(state, 1)

        assertEquals("finished", finished.status)
        assertEquals(2, finished.winnerOwner)
        assertEquals("forfeit", finished.lastAction)
    }
}
