package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Locks the product rule that every game has exactly one playable entry surface.
class GameEntryArenaRegressionTest {
    @Test
    fun everyGameRoutesDirectlyToItsOnlyPlayableEntrySurface() {
        val shell = projectFile("app/src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()
        val siege = projectFile("app/src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()
        val duel = projectFile("app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt").readText()
        val ladder = projectFile("app/src/main/java/com/sonharf/game/LetterLadderGame.kt").readText()

        assertFalse(shell.contains("SIEGE_ENTRY"))
        assertFalse(shell.contains("LAST_LETTER_ENTRY"))
        assertFalse(shell.contains("LETTER_PATH_ENTRY"))
        assertFalse(shell.contains("PremiumSiegeEntryScreen"))
        assertFalse(shell.contains("PremiumLastLetterEntryScreen"))
        assertFalse(shell.contains("PremiumLetterPathEntryScreen"))
        assertTrue(shell.contains("onPrimary = { openGame(PremiumDestination.SIEGE) }"))
        assertTrue(shell.contains("onLastLetter = { openGame(PremiumDestination.LAST_LETTER) }"))
        assertTrue(shell.contains("onLetterPath = { openGame(PremiumDestination.LETTER_PATH) }"))

        assertTrue(siege.contains("onLanguageChange = { SonHarfUiState.language = it }"))
        assertTrue(siege.contains("Kelime kur • alan ele geçir"))
        assertTrue(duel.contains("PremierLanguageSwitch(language, onLanguage)"))
        assertTrue(duel.contains("Son harften yeni kelime üret"))
        assertTrue(ladder.contains("SonHarfUiState.language = code"))
        assertTrue(ladder.contains("Bir harfi değiştir • hedefe ulaş"))
    }

    private fun projectFile(path: String): File =
        listOf(File(path), File("../$path")).firstOrNull(File::exists)
            ?: error("Project path missing: $path")
}
