package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SonHarfPlayedWordsVisibilityTest {
    @Test
    fun playedWordsAreVisibleToEveryPlayerOnEveryScreenSize() {
        val screen = File("src/main/java/com/sonharf/game/PremierWordDuelScreen.kt").readText()

        assertTrue(screen.contains("PremierHistoryDrawer(words, language)"))
        assertTrue(screen.contains("OYNANAN KELİMELER"))
        assertTrue(screen.contains("PLAYED WORDS"))
        assertFalse(screen.contains("PremierHistoryDrawer(words, language, isPro)"))
        assertFalse(screen.contains("PRO • Tüm oynanan kelimeler"))
        assertFalse(screen.contains("PRO ÖZELLİĞİ"))
    }
}
