package com.sonharf.game

import java.io.File
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegePracticeWinnerTest {
    @Test
    fun normalFinishUsesTerritoryControlEvenWhenTotalPointsAreHigher() {
        var state = WordSiegePracticeEngine.newGame(random = Random(1)).copy(
            bag = "A".repeat(19),
            playerWordScore = 400,
            playerAreaScore = 35,
            botWordScore = 250,
            botAreaScore = 48,
            // The player leads 435-298 in total points, while the bot controls more territory.
            // Normal Kelime Tahtı victory is deliberately decided by current territory control.
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
        assertEquals(2, state.winnerOwner)
    }

    @Test
    fun forcedForfeitWinnerStillOverridesScoreAndTerritory() {
        val state = WordSiegePracticeEngine.newGame(random = Random(2)).copy(
            playerWordScore = 500,
            playerAreaScore = 50,
            botWordScore = 10,
            botAreaScore = 0,
            playerArea = 30,
            botArea = 2,
            currentOwner = 1,
        )

        val finished = WordSiegePracticeEngine.forfeit(state, 1)

        assertEquals("finished", finished.status)
        assertEquals(2, finished.winnerOwner)
        assertEquals("forfeit", finished.lastAction)
    }

    @Test
    fun resultScreenExplainsWhenPointOrderDiffersFromMatchResult() {
        val source = File("src/main/java/com/sonharf/game/MatchResultScreen.kt").readText()

        assertTrue(source.contains("scoreOrderDiffersFromResult"))
        assertTrue(source.contains("sh(\"TOPLAM PUAN\", \"TOTAL SCORE\")"))
        assertTrue(source.contains("Bunlar toplam puanlardır"))
        assertTrue(source.contains("alan hâkimiyeti veya pes etme"))
        assertTrue(source.contains("ResultScoreColumn(mine, if (won) accent else Color.White, Modifier.weight(1f))"))
    }
}
