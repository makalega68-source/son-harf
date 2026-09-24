package com.sonharf.game

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeEntryModesContractTest {
    @Test
    fun entryExposesTurnLengthAndSeriesModes() {
        val entry = projectFile("app/src/main/java/com/sonharf/game/WordSiegeEntryScreen.kt").readText()

        // "My games" and practice live in the Match Center and PLAY tab; the entry keeps the
        // 12/24-hour turn choice for new standard games and the Series format.
        assertTrue(entry.contains("\"12 SAAT\""))
        assertTrue(entry.contains("\"24 SAAT\""))
        assertTrue(entry.contains("WordSiegeLaunchConfig.classicTurnHours = 24"))
        assertTrue(entry.contains("WordSiegeEntryMode.SERIES"))
        assertTrue(entry.contains("WordSiegeSeriesScreen(verifiedAccess = true)"))
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

        assertTrue(board.contains("PanSiegeMine = Color(0xFF8BD8AA)"))
        assertTrue(board.contains("PanSiegeRival = Color(0xFFEDA09B)"))
        assertTrue(board.contains("color = Color(0xFF2F1D13)"))
        assertTrue(board.contains("shadowElevation = 14.dp"))
        assertTrue(board.contains("border = BorderStroke(2.dp, Color(0xFFC6A56B))"))
        assertTrue(board.contains("Color(0xFFF2DFC0)"))
        assertTrue(board.contains("WordSiegePremiumPanel("))
    }

    private fun projectFile(path: String): File =
        listOf(File(path), File("../$path")).firstOrNull(File::exists)
            ?: error("Project path missing: $path")
}
