package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LetterLadderEngineTest {
    private val chain = listOf("kalın", "yalın", "yalan", "yalak", "yamak")
    private val puzzle = LetterLadderPuzzle(
        id = "test",
        start = chain.first(),
        target = chain.last(),
        solution = chain,
    )
    private val dictionary = chain.toSet() + setOf("salın")

    @Test
    fun knownFourMoveChainChangesFourPositionsExactlyOnce() {
        val used = mutableSetOf<Int>()
        chain.zipWithNext().forEach { (from, to) ->
            val changed = LetterLadderEngine.changedIndex(from, to)
            assertNotNull(changed)
            assertTrue("position $changed changed more than once", used.add(changed!!))
        }
        assertEquals(setOf(0, 2, 3, 4), used)
        assertEquals("yamak", chain.last())
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
    fun validSequenceReachesTargetInExactlyFourMoves() {
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
        assertEquals(4, used.size)
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
    fun safeHintCountsChoicesWithoutExposingAWord() {
        assertEquals(
            1,
            LetterLadderEngine.viableNextMoveCount(
                puzzle = puzzle,
                current = puzzle.start,
                usedPositions = emptySet(),
                dictionary = dictionary,
            ),
        )
    }

    @Test
    fun safeHintReturnsOnlyThePositionThatCanContinue() {
        assertEquals(
            listOf(0),
            LetterLadderEngine.viableNextMoveIndices(
                puzzle = puzzle,
                current = puzzle.start,
                usedPositions = emptySet(),
                dictionary = dictionary,
            ),
        )
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
    fun generatorReturnsALegalFourMovePuzzleFromCanonicalCandidates() {
        val generated = LetterLadderEngine.generate(
            sourceWords = chain.toSet(),
            language = "tr",
            seed = 42L,
        )
        assertNotNull(generated)
        generated!!
        assertEquals(5, generated.solution.size)
        assertEquals(generated.start, generated.solution.first())
        assertEquals(generated.target, generated.solution.last())
        assertEquals(1, (0 until 5).count { generated.start[it] == generated.target[it] })
    }

    @Test
    fun generatorExcludesRecentlyPlayedRouteInEitherDirection() {
        val firstRoute = listOf("abcde", "fbcde", "fgcde", "fghde", "fghie")
        val secondRoute = listOf("klmno", "plmno", "pqmno", "pqrno", "pqrso")
        val source = (firstRoute + secondRoute).toSet()
        val first = LetterLadderEngine.generate(source, "en", seed = 7L)
        assertNotNull(first)

        val next = LetterLadderEngine.generate(
            sourceWords = source,
            language = "en",
            seed = 7L,
            excludedPuzzleIds = setOf(first!!.id),
        )

        assertNotNull(next)
        assertTrue("Recent puzzle must not repeat", next!!.id != first.id)
        assertTrue(next.solution.toSet().intersect(first.solution.toSet()).isEmpty())
    }
}
