package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * G4.5 kısmı — LetterLadder çekirdek mantığının birim testleri.
 *
 * Test sözlüğü minimal ama gerçekçi (5 harfli TR-ish kelimeler).
 * Amaç: hamle doğrulama, konum kilitleme, hedef harf zorlaması,
 * tamamlanma yolu ve çıkmaz sezimi.
 */
class LetterLadderEngineTest {

    // Puzzle: KABLO -> KABIN (tek harf değişir: 'l' -> 'i', 4 -> 3)
    // Not: sözlük altındaki tüm kelimeler engine'e "sözlükte" görünsün diye
    // set olarak veriliyor.
    private val dict = setOf(
        "kablo", "kabuk", "kabin", "kabus",
        "sabun", "sabit", "sabır",
        "elma", "elmas",
    )

    private val puzzle = LetterLadderPuzzle(
        id = "test:1",
        start = "kabuk",
        target = "kabus",
        solution = listOf("kabuk", "kabus"),
    )

    @Test
    fun `changedIndex returns the single differing position`() {
        assertEquals(4, LetterLadderEngine.changedIndex("kabuk", "kabus"))
        assertEquals(0, LetterLadderEngine.changedIndex("kabuk", "sabuk"))
    }

    @Test
    fun `changedIndex returns null when zero or many positions differ`() {
        assertNull(LetterLadderEngine.changedIndex("kabuk", "kabuk"))
        assertNull(LetterLadderEngine.changedIndex("kabuk", "sabin"))
    }

    @Test
    fun `validateMove rejects non-dictionary word`() {
        val check = LetterLadderEngine.validateMove(
            puzzle = puzzle,
            current = "kabuk",
            candidate = "kabuz", // not in dict
            usedPositions = emptySet(),
            dictionary = dict,
        )
        assertFalse(check.accepted)
        assertEquals(LetterLadderReject.NOT_DICTIONARY, check.reject)
    }

    @Test
    fun `validateMove rejects wrong length`() {
        val check = LetterLadderEngine.validateMove(
            puzzle = puzzle,
            current = "kabuk",
            candidate = "kabl", // 4 harf
            usedPositions = emptySet(),
            dictionary = dict + "kabl",
        )
        assertFalse(check.accepted)
        assertEquals(LetterLadderReject.LENGTH, check.reject)
    }

    @Test
    fun `validateMove rejects when nothing or multiple letters change`() {
        val same = LetterLadderEngine.validateMove(
            puzzle, "kabuk", "kabuk", emptySet(), dict,
        )
        assertFalse(same.accepted)
        assertEquals(LetterLadderReject.NOT_ONE_CHANGE, same.reject)

        val two = LetterLadderEngine.validateMove(
            puzzle, "kabuk", "sabin", emptySet(), dict + "sabin",
        )
        assertFalse(two.accepted)
        assertEquals(LetterLadderReject.NOT_ONE_CHANGE, two.reject)
    }

    @Test
    fun `validateMove rejects reusing a locked position`() {
        // Change position 4 first, then try to change it again.
        val check = LetterLadderEngine.validateMove(
            puzzle = puzzle,
            current = "kabuk",
            candidate = "kabus",           // pos 4 change
            usedPositions = setOf(4),      // already used
            dictionary = dict,
        )
        assertFalse(check.accepted)
        assertEquals(LetterLadderReject.POSITION_ALREADY_USED, check.reject)
    }

    @Test
    fun `validateMove rejects when changed letter differs from target letter`() {
        // Puzzle target position 4 must equal 's'. Playing "kabin"
        // changes position 4 from 'k' to 'n' -- that's fine as a
        // one-letter change but 'n' != target[4]='s', so it's rejected
        // with WRONG_TARGET_LETTER.
        val check = LetterLadderEngine.validateMove(
            puzzle = puzzle,
            current = "kabuk",
            candidate = "kabin",   // pos 3 'u'->'i' AND pos 4 'k'->'n'
            usedPositions = emptySet(),
            dictionary = dict,
        )
        // Two letters changed -> NOT_ONE_CHANGE first.
        assertFalse(check.accepted)
        assertEquals(LetterLadderReject.NOT_ONE_CHANGE, check.reject)
    }

    @Test
    fun `validateMove accepts valid single-letter move toward target`() {
        val check = LetterLadderEngine.validateMove(
            puzzle = puzzle,
            current = "kabuk",
            candidate = "kabus",  // pos 4 'k'->'s' matches target[4]
            usedPositions = emptySet(),
            dictionary = dict,
        )
        assertTrue(check.accepted)
        assertEquals(4, check.changedIndex)
    }

    @Test
    fun `completionPath returns route when reachable`() {
        // Already-target case returns just [target].
        val same = LetterLadderEngine.completionPath(
            puzzle, "kabus", emptySet(), dict,
        )
        assertEquals(listOf("kabus"), same)

        // One move away.
        val path = LetterLadderEngine.completionPath(
            puzzle, "kabuk", emptySet(), dict,
        )
        assertNotNull(path)
        assertEquals("kabuk", path!!.first())
        assertEquals("kabus", path.last())
    }

    @Test
    fun `completionPath returns null when target is unreachable`() {
        // All positions used but not at target: unreachable.
        val path = LetterLadderEngine.completionPath(
            puzzle,
            current = "kabin",  // not target, all positions used
            usedPositions = (0 until LetterLadderEngine.WORD_LENGTH).toSet(),
            dictionary = dict,
        )
        assertNull(path)
    }
}
