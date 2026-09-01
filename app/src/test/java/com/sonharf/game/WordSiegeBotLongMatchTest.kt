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
            botRack = "AAAAAAA",
            playerRack = "AAAAAAA",
            bag = "A".repeat(180),
        )
        // "AA" forces progressive one-cell anchor extensions after the opening move.
        // This isolates the long-board candidate/rack-renewal invariant from lexicon variety.
        val lexicon = listOf("AA")

        repeat(24) { turn ->
            val beforeBag = state.bag.length
            val beforeRack = state.botRack
            val plan = WordSiegeBotPlanner.planFromLexicon(
                state = state,
                lexiconInput = lexicon,
                difficulty = TrainingBotDifficulty.HARD,
                random = Random(1000 + turn),
            ) { words -> words.toSet() }

            println(
                "turn_number=${turn + 1} rack=$beforeRack bag_remaining=$beforeBag " +
                    "anchor_count=${plan.anchorCount} candidate_count_before_validation=${plan.structuralCandidateCount} " +
                    "candidate_count_after_validation=${plan.validCandidateCount} " +
                    "selected_move=${plan.move?.primaryWord ?: "none"} pass_reason=${plan.passReason ?: "none"}",
            )
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
        assertTrue(state.board.count { it.letter != null } >= 25)
    }
}
