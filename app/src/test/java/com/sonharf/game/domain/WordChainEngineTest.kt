package com.sonharf.game.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Client-side rules for the Son Harf chain (G4.0 + engine invariants).
 * These are the pre-checks that fail fast before the server call.
 */
class WordChainEngineTest {
    private val engine = WordChainEngine()

    @Test
    fun `rejects word ending with soft g in Turkish`() {
        // G4.0: bir sonraki oyuncuya "Ğ ile başlayan" imkansız bırakır.
        val start = engine.submit(GameState(), "dağ")
        assertEquals("Ğ ile biten kelime oynanamaz", start.message)
        // The chain is not advanced.
        assertTrue(start.chain.isEmpty())
        assertNull(start.winner)
    }

    @Test
    fun `rejects duplicate word`() {
        val a = engine.submit(GameState(), "elma")
        val b = engine.submit(a, "elma")
        assertEquals("Bu kelime daha önce kullanıldı", b.message)
        assertEquals(1, b.chain.size)
    }

    @Test
    fun `enforces last-letter chain`() {
        val a = engine.submit(GameState(), "elma")
        val b = engine.submit(a, "kalem") // 'a' -> should start with 'a'
        assertEquals("Kelime 'A' ile başlamalı", b.message)
        assertEquals(1, b.chain.size)
    }

    @Test
    fun `advances turn on valid word`() {
        val a = engine.submit(GameState(), "elma")
        assertEquals(2, a.currentPlayer)
        assertEquals(1, a.chain.size)
    }

    @Test
    fun `forfeit awards the other player`() {
        val a = engine.submit(GameState(), "kedi")
        val f = engine.forfeit(a)
        assertEquals(1, f.winner)
    }
}
