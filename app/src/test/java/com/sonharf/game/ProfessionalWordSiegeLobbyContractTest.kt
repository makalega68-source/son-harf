package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfessionalWordSiegeLobbyContractTest {
    @Test
    fun `standard siege routes through professional lobby and preserves core match actions`() {
        val entry = repoFile("app/src/main/java/com/sonharf/game/WordSiegeEntryScreen.kt").readText()
        val lobby = repoFile("app/src/main/java/com/sonharf/game/ProfessionalWordSiegeExperience.kt").readText()

        assertTrue(entry.contains("ProfessionalWordSiegeExperienceScreen"))
        assertTrue(lobby.contains("GameTopBar("))
        assertTrue(lobby.contains("GamePrimaryButton("))
        assertTrue(lobby.contains("RAKİP BUL"))
        assertTrue(lobby.contains("ALIŞTIRMA"))
        assertTrue(lobby.contains("WordSiegePanMatch("))
        assertTrue(lobby.contains("backend.submitWordSiegeMove("))
        assertTrue(lobby.contains("backend.passWordSiegeTurn("))
        assertTrue(lobby.contains("backend.exchangeWordSiegeTiles("))
        assertTrue(lobby.contains("backend.sendWordSiegeMessage("))
        assertTrue(lobby.contains("backend.forfeitWordSiegeGame("))
        assertTrue(lobby.contains("WordSiegeFinalRules.currentTerritoryScore"))
        assertFalse(lobby.contains("MainUi."))
        assertFalse(lobby.contains("SonHarfTheme."))
    }

    private fun repoFile(path: String): File = sequenceOf(File(path), File("../$path"))
        .firstOrNull { it.exists() }
        ?: error("Missing repository file: $path")
}
