package com.sonharf.game

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class WordSiegeEntryModesContractTest {
    @Test
    fun entryOffersModesWhileMyGamesOwnsTheLibrary() {
        val entry = projectFile("app/src/main/java/com/sonharf/game/WordSiegeEntryScreen.kt").readText()

        val library = projectFile("app/src/main/java/com/sonharf/game/MyGamesScreen.kt").readText()
        assertFalse(entry.contains("\"OYUNLARIM\""))
        assertTrue(library.contains("GameInvitesScreen"))
        assertTrue(library.contains("CompactMatchRow"))
        assertFalse(library.contains("findOrCreateWordSiegeGame"))
        assertTrue(entry.contains("\"12 SAAT\""))
        assertTrue(entry.contains("\"24 SAAT\""))
        assertTrue(entry.contains("\"HIZLI DÜELLO\""))
        assertTrue(entry.contains("\"AI İLE OYNA\""))
        assertTrue(entry.contains("WordSiegeEntryMode.SERIES"))
        assertTrue(entry.contains("WordSiegeEntryMode.AI"))
        assertTrue(entry.contains("WordSiegePracticeScreen"))
        assertTrue(entry.contains("startClassic(24)"))
        assertTrue(entry.contains("WordSiegeSeriesScreen(verifiedAccess = true, directEntry = true)"))
    }

    @Test
    fun classicDurationIsServerSelectedAnd24HoursIsSchemaValid() {
        val backend = projectFile("app/src/main/java/com/sonharf/game/data/WordSiegeBackend.kt").readText()
        val migration = projectFile("supabase/migrations/20260923013000_word_siege_24_hour_mode_v1.sql").readText()

        assertTrue(backend.contains("find_or_create_word_siege_game_v2"))
        assertTrue(backend.contains("p_turn_duration_hours"))
        assertTrue(backend.contains("turnDurationHours == 12 || turnDurationHours == 24"))
        assertTrue(migration.contains("turn_duration_hours in (12,24,72)"))
        assertTrue(migration.contains("p_turn_duration_hours not in (12,24,72)"))
    }

    @Test
    fun onlineBoardKeepsOwnershipRulesAndAddsPremiumFrame() {
        val board = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()

        assertTrue(board.contains("PanSiegeMine get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.mine"))
        assertTrue(board.contains("PanSiegeRival get() = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.rival"))
        assertTrue(board.contains("color = if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.frame"))
        assertTrue(board.contains("shadowElevation = if (WordSiegeWalnutIvory.enabled) 7.dp else 14.dp"))
        assertTrue(board.contains("border = BorderStroke(if (boardSkin != null) 0.dp else 2.dp, if (WordSiegeWalnutIvory.enabled) WordSiegeWalnutIvory.frameEdge"))
        assertTrue(board.contains("WordSiegeWalnutIvory.tile"))
        assertFalse(board.contains("WordSiegePremiumPanel("))
    }

    private fun projectFile(path: String): File =
        listOf(File(path), File("../$path")).firstOrNull(File::exists)
            ?: error("Project path missing: $path")
}
