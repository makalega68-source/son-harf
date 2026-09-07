package com.sonharf.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LetterLadderEngineTest {
    private val chain = listOf("kalın", "yalın", "yalan", "yalak", "yamak", "yumak")
    private val puzzle = LetterLadderPuzzle(
        id = "test",
        start = chain.first(),
        target = chain.last(),
        solution = chain,
    )
    private val dictionary = chain.toSet() + setOf("yalan", "yelin")

    @Test
    fun `known five move chain changes every position exactly once`() {
        val used = mutableSetOf<Int>()
        chain.zipWithNext().forEach { (from, to) ->
            val changed = LetterLadderEngine.changedIndex(from, to)
            assertNotNull(changed)
            assertTrue(used.add(changed), "position $changed changed more than once")
        }
        assertEquals(setOf(0, 1, 2, 3, 4), used)
        assertEquals("yumak", chain.last())
    }

    @Test
    fun `changing a locked position is rejected`() {
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
    fun `new position must immediately take its final target letter`() {
        val result = LetterLadderEngine.validateMove(
            puzzle = puzzle,
            current = "kalın",
            candidate = "yelin",
            usedPositions = emptySet(),
            dictionary = dictionary,
        )
        assertFalse(result.accepted)
        assertEquals(LetterLadderReject.WRONG_TARGET_LETTER, result.reject)
    }

    @Test
    fun `valid sequence reaches target in exactly five moves`() {
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
            assertTrue(result.accepted, "$current -> $next must be accepted")
            used += assertNotNull(result.changedIndex)
            current = next
        }
        assertEquals(5, used.size)
        assertEquals(puzzle.target, current)
    }

    @Test
    fun `generator returns a legal five move puzzle from canonical candidates`() {
        val generated = LetterLadderEngine.generate(
            sourceWords = chain.toSet(),
            language = "tr",
            seed = 42L,
            preferCurated = true,
        )
        assertNotNull(generated)
        assertEquals(6, generated.solution.size)
        assertEquals(generated.start, generated.solution.first())
        assertEquals(generated.target, generated.solution.last())
        assertTrue((0 until 5).all { generated.start[it] != generated.target[it] })
    }
}
