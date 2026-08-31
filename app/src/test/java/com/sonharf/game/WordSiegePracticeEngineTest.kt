package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegePracticeEngineTest {
    @Test
    fun firstPracticeMoveCoversCenterAndUsesBonus() {
        val state = WordSiegePracticeEngine.newGame().copy(playerRack = "KALEMTR")

        val (next, move) = WordSiegePracticeEngine.applyMove(
            state = state,
            owner = 1,
            placements = linkedMapOf(38 to 0, 39 to 1, 40 to 2, 41 to 3, 42 to 4),
            horizontal = true,
        )

        assertEquals("KALEM", move.primaryWord)
        assertEquals(12, move.wordScore)
        assertEquals(5, next.playerArea)
        assertEquals(2, next.currentOwner)
        assertTrue(next.board[40].bonusUsed)
    }

    @Test
    fun botCanFindMoveAndCaptureAnExistingTile() {
        val initial = WordSiegePracticeEngine.newGame().copy(
            playerRack = "KALEMTR",
            botRack = "MASASİN",
        )
        val opened = WordSiegePracticeEngine.applyMove(
            initial,
            1,
            linkedMapOf(38 to 0, 39 to 1, 40 to 2, 41 to 3, 42 to 4),
            true,
        ).first
        val planned = WordSiegePracticeEngine.bestBotMove(opened)

        assertNotNull(planned)
        val botPlan = requireNotNull(planned)
        val (afterBot, botMove) = WordSiegePracticeEngine.applyMove(
            opened,
            2,
            botPlan.placements,
            botPlan.horizontal,
        )

        assertTrue(botMove.formedWords.isNotEmpty())
        assertTrue(afterBot.botArea > 0)
        assertTrue(afterBot.board.any { it.owner == 2 })
        assertEquals(1, afterBot.currentOwner)
    }

    @Test
    fun botValidatorRejectsInvalidCrossWord() {
        val base = WordSiegePracticeEngine.newGame()
        val board = base.board.toMutableList().also {
            it[29] = it[29].copy(letter = "B", owner = 1, bonusUsed = true)
        }
        val state = base.copy(
            board = board,
            currentOwner = 2,
            botRack = "KALEMZZ",
        )

        val error = runCatching {
            WordSiegePracticeEngine.validateMove(
                state = state,
                owner = 2,
                placements = linkedMapOf(38 to 0, 39 to 1, 40 to 2, 41 to 3, 42 to 4),
                horizontal = true,
            )
        }.exceptionOrNull()

        assertTrue(error is WordSiegePracticeError)
        assertEquals("word_siege_invalid_word:BK", error?.message)
    }

    @Test
    fun botValidatorRejectsInvalidExtension() {
        val base = WordSiegePracticeEngine.newGame()
        val board = base.board.toMutableList()
        "MAKALE".forEachIndexed { offset, letter ->
            val index = 36 + offset
            board[index] = board[index].copy(letter = letter.toString(), owner = 1, bonusUsed = true)
        }
        val state = base.copy(
            board = board,
            currentOwner = 2,
            botRack = "BZZZZZZ",
        )

        val error = runCatching {
            WordSiegePracticeEngine.validateMove(
                state = state,
                owner = 2,
                placements = mapOf(42 to 0),
                horizontal = true,
            )
        }.exceptionOrNull()

        assertTrue(error is WordSiegePracticeError)
        assertEquals("word_siege_invalid_word:MAKALEB", error?.message)
    }

    @Test
    fun consecutivePassesFinishPractice() {
        val first = WordSiegePracticeEngine.pass(WordSiegePracticeEngine.newGame(), 1)
        val finished = WordSiegePracticeEngine.pass(first, 2)

        assertEquals("finished", finished.status)
        assertEquals("consecutive_passes", finished.lastAction)
    }

    @Test
    fun exchangeKeepsTileCountsAndChangesTurn() {
        val state = WordSiegePracticeEngine.newGame()
        val next = WordSiegePracticeEngine.exchange(state, 1, setOf(0, 2))

        assertEquals(7, next.playerRack.length)
        assertEquals(state.bag.length, next.bag.length)
        assertEquals(2, next.currentOwner)
    }
}
