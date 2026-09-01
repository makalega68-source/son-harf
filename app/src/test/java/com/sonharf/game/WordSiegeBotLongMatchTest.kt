package com.sonharf.game

import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeBotLongMatchTest {
    @Test
    fun botPlannerKeepsProducingCandidatesAcross24ProgressiveTurns() = runBlocking {
        var state = WordSiegePracticeEngine.newGame().copy(
            currentOwner = 2,
            botRack = "ATATATA",
            playerRack = "ATATATA",
            bag = "AT".repeat(90),
        )
        val lexicon = buildList {
            for (length in 2..9) {
                add(buildString { repeat(length) { append(if (it % 2 == 0) 'A' else 'T') } })
                add(buildString { repeat(length) { append(if (it % 2 == 0) 'T' else 'A') } })
            }
        }

        repeat(24) { turn ->
            val beforeBag = state.bag.length
            val beforeRack = state.botRack
            val plan = WordSiegeBotPlanner.planFromLexicon(
                state = state,
                lexiconInput = lexicon,
                difficulty = TrainingBotDifficulty.HARD,
                random = Random(1000 + turn),
            ) { words -> words.toSet() }

            assertTrue("turn=$turn passReason=${plan.passReason}", plan.validCandidateCount > 0)
            val move = plan.move
            assertNotNull("turn=$turn", move)
            val played = requireNotNull(move)
            val usedTiles = played.placements.size
            val (next, _) = WordSiegePracticeEngine.applyMove(
                state, 2, played.placements, played.horizontal,
            ) { true }

            assertTrue("turn=$turn rack did not consume a tile", usedTiles in 1..beforeRack.length)
            assertEquals("turn=$turn bag refill mismatch", beforeBag - usedTiles, next.bag.length)
            assertEquals("turn=$turn rack did not refill", 7, next.botRack.length)
            assertTrue("turn=$turn anchor count", plan.anchorCount >= 1)

            state = next.copy(currentOwner = 2, consecutivePasses = 0, status = "playing")
        }

        assertTrue(state.moveCount >= 24)
        assertTrue(state.board.count { it.letter != null } >= 20)
    }
}
