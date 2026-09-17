package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeMatchIntroContractTest {
    @Test
    fun introRunsOnlyForFreshPlayingMatchAndDoesNotOwnNetworking() {
        val match = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()
        val intro = projectFile("app/src/main/java/com/sonharf/game/WordSiegeMatchIntro.kt").readText()

        assertTrue(match.contains("game.status == \"playing\" && game.moveCount == 0"))
        assertTrue(match.contains("WordSiegeMatchIntro("))
        assertTrue(match.contains("onComplete = { showMatchIntro = false }"))
        assertTrue(match.contains("WORD_SIEGE_BOT_FALLBACK_DELAY_MS = 15_000L"))

        assertTrue(intro.contains("WORD_SIEGE_MATCH_INTRO_TOTAL_MS = 2_400L"))
        assertTrue(intro.contains("WordSiegeIntroVersus"))
        assertTrue(intro.contains("WordSiegeIntroMapPreview"))
        assertTrue(intro.contains("WordSiegeIntroLaunch"))
        assertTrue(intro.contains("ProfilePhotoAvatarWithGender"))
        assertTrue(intro.contains("WordSiegeBoardSpec.bonusAt"))

        val lowered = intro.lowercase()
        assertFalse(lowered.contains("onlinegamebackend"))
        assertFalse(lowered.contains("supabase"))
        assertFalse(lowered.contains("getwordsiegegame"))
        assertFalse(lowered.contains("findorcreatewordsiegegame"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
