package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiege15x15ServerParityContractTest {
    @Test fun parityMigrationPatchesEveryAuthoritativeGeometryLayer() {
        val migration = projectFile(
            "supabase/migrations/20260909101500_word_siege_15x15_server_parity_v6.sql",
        ).readText()

        assertTrue(migration.contains("from generate_series(0, 224) i"))
        assertTrue(migration.contains("jsonb_array_length(board) = 225"))
        assertTrue(migration.contains("p_delta not in (1, 15)"))
        assertTrue(migration.contains("'x not between 0 and 80', 'x not between 0 and 224'"))
        assertTrue(migration.contains("'x / 9 <> v_anchor / 9', 'x / 15 <> v_anchor / 15'"))
        assertTrue(migration.contains("'x % 9 <> v_anchor % 9', 'x % 15 <> v_anchor % 15'"))
        assertTrue(migration.contains("'40 = any(v_indices)', '112 = any(v_indices)'"))
        assertTrue(migration.contains("private.word_siege_prevalidate_move_v2(uuid,jsonb,boolean)"))
        assertTrue(migration.contains("private.word_siege_preview_move_v1(uuid,jsonb,boolean)"))
        assertTrue(migration.contains("'generate_series(0,80)', 'generate_series(0,224)'"))
        assertTrue(migration.contains("word_siege_non_225_games_remaining"))
    }

    @Test fun parityMigrationDoesNotRegressNewerDictionaryOrFinalScoring() {
        val migration = projectFile(
            "supabase/migrations/20260909101500_word_siege_15x15_server_parity_v6.sql",
        ).readText()
        val finalScoreMigration = projectFile(
            "supabase/migrations/20260909090000_word_siege_current_territory_score_v5.sql",
        ).readText()

        assertFalse(migration.contains("create or replace function private.finish_word_siege_game_v1"))
        assertFalse(migration.contains("create or replace function private.word_siege_word_allowed_v1"))
        assertTrue(finalScoreMigration.contains("r.player_one_word_score + (r.player_one_area * 2)"))
        assertTrue(finalScoreMigration.contains("r.player_two_word_score + (r.player_two_area * 2)"))
    }

    @Test fun clientBoardContractMatchesServerParityTarget() {
        assertTrue(WordSiegeBoardSpec.Size == 15)
        assertTrue(WordSiegeBoardSpec.CellCount == 225)
        assertTrue(WordSiegeBoardSpec.CenterIndex == 112)
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
