package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LetterLadderEngineTest {
    private val chain = listOf("kalın", "yalın", "yalan", "yalak", "yamak", "yumak")
    private val puzzle = LetterLadderPuzzle(
        id = "test",
        start = chain.first(),
        target = chain.last(),
        solution = chain,
    )
    private val dictionary = chain.toSet() + setOf("salın")

    @Test
    fun knownFiveMoveChainChangesEveryPositionExactlyOnce() {
        val used = mutableSetOf<Int>()
        chain.zipWithNext().forEach { (from, to) ->
            val changed = LetterLadderEngine.changedIndex(from, to)
            assertNotNull(changed)
            assertTrue("position $changed changed more than once", used.add(changed!!))
        }
        assertEquals(setOf(0, 1, 2, 3, 4), used)
        assertEquals("yumak", chain.last())
    }

    @Test
    fun changingALockedPositionIsRejected() {
        val result = LetterLadderEngine.validateMove(
            puzzle = puzzle,
            current = "yalın",
            candidate = "kalın",
            usedPositions = setOf(0),
            dictionary = dictionary,
        )
        assertFalse(result.accepted)
        assertEquals(LetterLadderReject.POSITION_ALREADY_USED, result.reject)
    }

    @Test
    fun newPositionMustImmediatelyTakeItsFinalTargetLetter() {
        val result = LetterLadderEngine.validateMove(
            puzzle = puzzle,
            current = "kalın",
            candidate = "salın",
            usedPositions = emptySet(),
            dictionary = dictionary,
        )
        assertFalse(result.accepted)
        assertEquals(LetterLadderReject.WRONG_TARGET_LETTER, result.reject)
    }

    @Test
    fun validSequenceReachesTargetInExactlyFiveMoves() {
        var current = puzzle.start
        val used = mutableSetOf<Int>()
        chain.drop(1).forEach { next ->
            val result = LetterLadderEngine.validateMove(
                puzzle = puzzle,
                current = current,
                candidate = next,
                usedPositions = used,
                dictionary = dictionary,
            )
            assertTrue("$current -> $next must be accepted", result.accepted)
            assertNotNull(result.changedIndex)
            used += result.changedIndex!!
            current = next
        }
        assertEquals(5, used.size)
        assertEquals(puzzle.target, current)
    }

    @Test
    fun completionPathFindsAFullRouteForHints() {
        val route = LetterLadderEngine.completionPath(
            puzzle = puzzle,
            current = puzzle.start,
            usedPositions = emptySet(),
            dictionary = dictionary,
        )

        assertNotNull(route)
        assertEquals(chain, route)
    }

    @Test
    fun locallyValidMoveCanBeRecognizedAsADeadEnd() {
        val deadEndDictionary = chain.toSet() + setOf("kalık")
        val localMove = LetterLadderEngine.validateMove(
            puzzle = puzzle,
            current = "kalın",
            candidate = "kalık",
            usedPositions = emptySet(),
            dictionary = deadEndDictionary,
        )

        assertTrue(localMove.accepted)
        assertEquals(4, localMove.changedIndex)
        assertNull(
            LetterLadderEngine.completionPath(
                puzzle = puzzle,
                current = "kalık",
                usedPositions = setOf(4),
                dictionary = deadEndDictionary,
            ),
        )
    }

    @Test
    fun generatorReturnsALegalFiveMovePuzzleFromCanonicalCandidates() {
        val generated = LetterLadderEngine.generate(
            sourceWords = chain.toSet(),
            language = "tr",
            seed = 42L,
            preferCurated = true,
        )
        assertNotNull(generated)
        generated!!
        assertEquals(6, generated.solution.size)
        assertEquals(generated.start, generated.solution.first())
        assertEquals(generated.target, generated.solution.last())
        assertTrue((0 until 5).all { generated.start[it] != generated.target[it] })
    }
}
