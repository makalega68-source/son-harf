package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KelimeTahtiDifferentiationContractTest {
    @Test
    fun playerFacingBoardUsesStrategicZoneLanguageNotClassicMultiplierLabels() {
        val spec = projectFile("app/src/main/java/com/sonharf/game/WordSiegeBoardSpec.kt").readText()
        assertTrue(spec.contains("\"2H\" -> \"GÖZ\""))
        assertTrue(spec.contains("\"3H\" -> \"KRİT\""))
        assertTrue(spec.contains("\"2K\" -> \"KALE\""))
        assertTrue(spec.contains("\"3K\" -> \"KUŞ\""))
        assertTrue(spec.contains("CenterBonus -> \"TAÇ\""))
        assertTrue(spec.contains("StarBonus -> \"ÖDÜL\""))
    }

    @Test
    fun practiceSurfaceShowsTerritoryBattleIdentity() {
        val screen = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()
        val board = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt").readText()

        assertTrue(screen.contains("HARİTA KONTROLÜ"))
        assertTrue(screen.contains("HAMLEYİ ONAYLA"))
        assertTrue(screen.contains("KUŞATMA +"))
        assertTrue(screen.contains("Kelime $wordPoints • Bölge $territoryPoints"))
        assertTrue(screen.contains("PracticePlayerAccent = Color(0xFF567A64)"))
        assertTrue(screen.contains("PracticeRivalAccent = Color(0xFF5C8299)"))
        assertTrue(board.contains("practiceCellThreatened"))
        assertTrue(board.contains("val regionGap = if (owner != 0) .45.dp else 1.15.dp"))
        assertTrue(board.contains("WordSiegeBoardSpec.displayBonusLabel(activeZone)"))
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
