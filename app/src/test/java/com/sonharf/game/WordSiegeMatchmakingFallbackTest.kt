package com.sonharf.game

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeMatchmakingFallbackTest {
    @Test fun botFallbackStartsAfterFifteenSecondsWithoutCancellingRealQueue() {
        assertEquals(15_000L, WORD_SIEGE_BOT_FALLBACK_DELAY_MS)

        val pan = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()
        val practice = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()
        val experience = projectFile("app/src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()

        assertTrue(pan.contains("delay(WORD_SIEGE_BOT_FALLBACK_DELAY_MS)"))
        assertTrue(pan.contains("fallbackPracticeActive"))
        assertTrue(pan.contains("game.status == \"waiting\""))
        assertTrue(pan.contains("matchmakingFallback = true"))
        assertTrue(pan.contains("temporary bot match starts immediately"))
        assertTrue(!pan.contains("cancelWordSiegeWaiting"))

        assertTrue(practice.contains("matchmakingFallback: Boolean = false"))
        assertTrue(practice.contains("BOT MAÇI • GERÇEK RAKİP ARANIYOR"))
        assertTrue(practice.contains("BOT MATCH • FINDING REAL RIVAL"))
        assertTrue(practice.contains("Gerçek rakip araması arka planda sürüyor"))
        assertTrue(practice.contains("Real matchmaking continues in the background"))
        assertTrue(practice.contains("ALIŞTIRMA"))
        assertTrue(practice.contains("PRACTICE"))
        assertTrue(!practice.contains("ANA SÖZLÜK"))
        assertTrue(!practice.contains("MAIN DICTIONARY"))

        // Manual practice stays explicitly practice-mode by relying on the default false parameter.
        assertTrue(experience.contains("WordSiegePracticeScreen(onExit = { practiceActive = false })"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}