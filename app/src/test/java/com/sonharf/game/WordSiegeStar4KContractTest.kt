package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeStar4KContractTest {
    private val migrationPath = "supabase/migrations/20260909145500_word_siege_star_4k_board_v7.sql"

    @Test fun serverCreatesGold4KCenterAndOneRandomThreeStarCellForNewGames() {
        val sql = projectFile(migrationPath).readText()
        assertTrue(sql.contains("when i = 112 then '4K'"))
        assertTrue(sql.contains("when i = v_star_index then '3Y'"))
        assertTrue(sql.contains("order by random()"))
        assertTrue(sql.contains("language plpgsql\nvolatile"))
        assertFalse(sql.contains("update public.word_siege_games set board"))
    }

    @Test fun authoritativeSubmitAndPreviewAwardStarOnceAndSupport4K() {
        val sql = projectFile(migrationPath).readText()
        assertTrue(sql.contains("if v_bonus = '4K' then v_word_multiplier := v_word_multiplier * 4"))
        assertTrue(sql.contains("count(*)::integer * 25"))
        assertTrue(sql.contains("coalesce(v_board -> index_value ->> 'bonus', '') = '3Y'"))
        assertTrue(sql.contains("create or replace function private.submit_word_siege_move_v1"))
        assertTrue(sql.contains("create or replace function private.word_siege_preview_move_v1"))
        assertTrue(sql.contains("player_one_word_score = player_one_word_score + case when v_owner = 1 then v_score else 0 end"))
        assertTrue(sql.contains("player_two_word_score = player_two_word_score + case when v_owner = 2 then v_score else 0 end"))
    }

    @Test fun practiceHeaderDropsMainDictionaryBrandingAndBoardKeepsLatestWordVisible() {
        val screen = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()
        val board = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt").readText()
        assertFalse(screen.contains("ANA SÖZLÜK"))
        assertFalse(screen.contains("MAIN DICTIONARY"))
        assertTrue(screen.contains("padding(horizontal = 4.dp, vertical = 2.dp)"))
        assertTrue(screen.contains("Ortadaki altın 4K karesinden geç"))
        assertTrue(board.contains("highlightAlpha.animateTo(0.42f"))
        assertTrue(board.contains("PracticeBonus4K"))
        assertTrue(board.contains("PracticeBonusStar"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
