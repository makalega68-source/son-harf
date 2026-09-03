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
        assertTrue(pan.contains("delay(WORD_SIEGE_BOT_FALLBACK_DELAY_MS)"))
        assertTrue(pan.contains("fallbackPracticeActive"))
        assertTrue(pan.contains("game.status == \"waiting\""))
        assertTrue(pan.contains("WordSiegePracticeScreen"))
        assertTrue(!pan.contains("cancelWordSiegeWaiting"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
