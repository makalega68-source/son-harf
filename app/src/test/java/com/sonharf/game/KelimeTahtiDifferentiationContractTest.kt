package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KelimeTahtiDifferentiationContractTest {
    @Test
    fun bonusLabelsExplainTheActualEffectInBothLanguages() {
        org.junit.Assert.assertEquals("Harf\n×2", WordSiegeBoardSpec.displayBonusLabel("2H"))
        org.junit.Assert.assertEquals("Kelime\n×3", WordSiegeBoardSpec.displayBonusLabel("3K"))
        org.junit.Assert.assertEquals("Word\n×4", WordSiegeBoardSpec.displayBonusLabel("4K", false))
        org.junit.Assert.assertEquals("+25", WordSiegeBoardSpec.displayBonusLabel("3Y"))
    }

    @Test
    fun practiceSurfaceShowsSimplifiedWordBoardIdentity() {
        val screen = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()
        val board = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt").readText()

        assertFalse(screen.contains("HARİTA KONTROLÜ"))
        assertTrue(screen.contains("HAMLEYİ ONAYLA"))
        assertFalse(screen.contains("KUŞATMA +"))
        val scoreCard = projectFile("app/src/main/java/com/sonharf/game/WordSiegeGameUi.kt").readText()
        assertTrue(scoreCard.contains("Kelime Puanı"))
        assertTrue(scoreCard.contains("Bölge Puanı"))
        assertTrue(scoreCard.contains("1 küp = 2 puan"))
        assertTrue(screen.contains("PracticePlayerAccent = Color(0xFF567A64)"))
        assertTrue(screen.contains("PracticeRivalAccent = Color(0xFF9B4D4A)"))
        assertTrue(board.contains("PracticeSiegeMine = Color(0xFFA8D5B5)"))
        assertTrue(board.contains("PracticeSiegeRival = Color(0xFFE4AEAA)"))
        assertTrue(board.contains("practiceCellThreatened"))
        assertTrue(board.contains("val regionGap = 1.25.dp"))
        assertTrue(board.contains("WordSiegeBoardSpec.displayBonusLabel(activeZone, !SonHarfUiState.isEnglish)"))
        assertFalse(screen.contains("altın 4K karesi"))
    }

    @Test
    fun staleMatchmakingCannotTrapAPlayerForever() {
        val migration = projectFile("supabase/migrations/20260911190000_word_siege_matchmaking_expiry_v1.sql").readText()
        assertTrue(migration.contains("matchmaking_expired"))
        assertTrue(migration.contains("interval '30 minutes'"))
        assertTrue(migration.contains("status='cancelled'"))
        assertTrue(migration.contains("create or replace function private.find_or_create_word_siege_game_v2"))
    }

    private fun projectFile(path: String): File =
        listOf(File(path), File("../$path")).firstOrNull(File::exists)
            ?: error("Project path missing: $path")
}