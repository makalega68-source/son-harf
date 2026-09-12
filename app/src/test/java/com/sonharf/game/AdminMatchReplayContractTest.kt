package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminMatchReplayContractTest {
    @Test
    fun `admin replay stays read only paginated and uses current board territory scoring`() {
        val migration = repoFile("supabase/staging-migrations/20260912_admin_match_replay_v1.sql")
            .readText()
        val compact = migration.replace(Regex("\\s+"), "").lowercase()

        assertTrue(compact.contains("securitydefinersetsearch_path=''"))
        assertTrue(compact.contains("auth.uid()"))
        assertTrue(compact.contains("notpublic.is_admin()"))
        assertTrue(compact.contains("raiseexception'admin_required'"))

        assertTrue(compact.contains("least(greatest(coalesce(p_limit,50),1),100)"))
        assertTrue(compact.contains("'has_more'"))
        assertTrue(compact.contains("'mode','classic'"))
        assertTrue(compact.contains("'mode','siege'"))

        assertTrue(compact.contains("jsonb_array_elements(v_siege.board)"))
        assertTrue(compact.contains("'cube_points',2"))
        assertTrue(compact.contains("'word_points_permanent',true"))
        assertTrue(compact.contains("'final_territory_source','final_board_owner_snapshot'"))
        assertTrue(compact.contains("v_one_owned*2"))
        assertTrue(compact.contains("v_two_owned*2"))
        assertTrue(compact.contains("ownership_counts_reconstructed_from_capture_deltas"))
        assertTrue(compact.contains("'exact_historical_capture_cell_indices',false"))

        assertFalse(compact.contains("player_one_area_score"))
        assertFalse(compact.contains("player_two_area_score"))

        assertTrue(compact.contains("revokeallonfunctionpublic.admin_match_replay_v1(uuid,text,integer,integer)frompublic,anon,service_role"))
        assertTrue(compact.contains("grantexecuteonfunctionpublic.admin_match_replay_v1(uuid,text,integer,integer)toauthenticated"))
        assertFalse(compact.contains("toanon"))
    }

    private fun repoFile(path: String): File = sequenceOf(File(path), File("../$path"))
        .firstOrNull { it.exists() }
        ?: error("Missing repository file: $path")
}
