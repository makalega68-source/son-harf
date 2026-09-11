package com.sonharf.game

import com.sonharf.game.data.SharedDictionaryService
import com.sonharf.game.data.WordSiegeCellDto
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class WordSiegePracticeBotAvailabilityRegressionTest {
    @Before
    fun setUp() {
        SharedDictionaryService.clearForTests()
        SharedDictionaryService.installSnapshotForTests(
            language = "tr",
            words = listOf("at"),
            botWords = emptyList(),
        )
    }

    @After
    fun tearDown() {
        SharedDictionaryService.clearForTests()
    }

    @Test
    fun botCanPlayCanonicalWordWhenVerifiedBotSnapshotIsUnavailable() {
        assertFalse(SharedDictionaryService.hasBotSnapshot("tr"))
        val state = WordSiegePracticeState(
            board = List(WordSiegeBoardSpec.CellCount) { WordSiegeCellDto() },
            bag = "EEEEEEE",
            playerRack = "AAAAAAA",
            botRack = "ATBBBBB",
            currentOwner = 2,
        )

        val (next, move) = WordSiegePracticeEngine.applyMove(
            state = state,
            owner = 2,
            placements = linkedMapOf(
                WordSiegeBoardSpec.CenterIndex to 0,
                WordSiegeBoardSpec.CenterIndex + 1 to 1,
            ),
        )

        assertEquals("AT", move.primaryWord)
        assertEquals(1, next.currentOwner)
        assertEquals(move.wordScore, next.botWordScore)
    }

    @Test
    fun resilientPlannerUsesBoardAnchorEvenWithoutVerifiedBotSnapshot() = runBlocking {
        val board = MutableList(WordSiegeBoardSpec.CellCount) { WordSiegeCellDto() }
        board[WordSiegeBoardSpec.CenterIndex] = WordSiegeCellDto(letter = "A", owner = 1, bonusUsed = true)
        val state = WordSiegePracticeState(
            board = board,
            bag = "EEEEEEE",
            playerRack = "AAAAAAA",
            botRack = "TBBBBBB",
            currentOwner = 2,
            playerWordScore = 1,
            playerArea = 1,
            playerAreaScore = WordSiegeFinalRules.CUBE_TRANSFER_POINTS,
            moveCount = 1,
        )

        val move = resilientPracticeBotMove(
            state = state,
            playerRating = 1000,
            playerWins = 0,
            playerLosses = 0,
        )

        assertNotNull(move)
        assertEquals("AT", move?.primaryWord)
    }
}
