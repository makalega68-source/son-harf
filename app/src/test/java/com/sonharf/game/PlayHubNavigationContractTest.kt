package com.sonharf.game

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayHubNavigationContractTest {
    @Test
    fun `bottom navigation follows the product order with PLAY in the middle`() {
        assertEquals(
            listOf("HOME", "SOCIAL", "PLAY", "LEAGUE", "SHOP"),
            GameMainTab.entries.map { it.name },
        )
        val shell = source("ProfessionalUnifiedApp.kt")
        assertTrue(shell.contains("GameMainTab.PLAY -> ProfessionalDestination.PLAY"))
        assertTrue(shell.contains("GameMainTab.LEAGUE -> ProfessionalDestination.LEADERBOARD"))
        assertTrue(shell.contains("GameMainTab.SHOP -> ProfessionalDestination.SHOP"))
        assertTrue(shell.contains("ProfessionalDestination.PLAY -> PlayHubScreen("))
        // Profile is reached from the home avatar, not from a tab.
        assertFalse(shell.contains("GameMainTab.PROFILE"))
    }

    @Test
    fun `PLAY puts Kelime Kusatmasi first and reuses the existing siege flows`() {
        val hub = source("PlayHubScreen.kt")
        val shell = source("ProfessionalUnifiedApp.kt")
        val experience = source("ProfessionalWordSiegeExperience.kt")

        assertTrue(hub.indexOf("KELİME KUŞATMASI") < hub.indexOf("DİĞER OYUN MODLARI"))
        assertTrue(hub.contains("HIZLI EŞLEŞME"))
        assertTrue(hub.contains("Arkadaşla Oyna"))
        assertTrue(hub.contains("Özel Oda"))
        assertTrue(hub.contains("Rövanş"))
        assertTrue(hub.contains("Antrenman"))
        assertTrue(hub.contains("backend.inviteFriendToWordSiege("))

        assertTrue(shell.contains("onQuickMatch = { openSiege(WordSiegeEntryAction.QUICK_MATCH) }"))
        assertTrue(shell.contains("onPractice = { openSiege(WordSiegeEntryAction.PRACTICE) }"))
        assertTrue(shell.contains("initialAction = siegeAction"))

        // Quick match is the existing server matchmaking; the 15 s bot fallback stays in the match screen.
        assertTrue(experience.contains("fun startQuickMatch()"))
        assertTrue(experience.contains("backend.findOrCreateWordSiegeGame("))
        assertEquals(15_000L, WORD_SIEGE_BOT_FALLBACK_DELAY_MS)
    }

    @Test
    fun `no level or seviye appears on the professional shell screens`() {
        listOf(
            "ProfessionalProfileScreen.kt",
            "ProfessionalRetentionScreen.kt",
            "ProfessionalHomeScreen.kt",
            "PlayHubScreen.kt",
        ).forEach { name ->
            val screen = source(name)
            assertFalse("$name shows a level", screen.contains("Seviye"))
            assertFalse("$name shows XP progress", screen.contains("XPProgress("))
        }
    }

    private fun source(name: String): String =
        sequenceOf(File("src/main/java/com/sonharf/game/$name"), File("app/src/main/java/com/sonharf/game/$name"))
            .firstOrNull { it.exists() }
            ?.readText()
            ?: error("Missing source file: $name")
}
