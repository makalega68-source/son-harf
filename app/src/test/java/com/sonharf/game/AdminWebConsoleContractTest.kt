package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminWebConsoleContractTest {
    @Test
    fun webConsoleCoversReleaseOperationsWithoutClientSideAuthority() {
        val html = projectFile("admin/index.html").readText()
        assertTrue(html.contains("admin_system_health"))
        assertTrue(html.contains("admin_dashboard_v1"))
        assertTrue(html.contains("admin_recent_match_analysis_v1"))
        assertTrue(html.contains("admin_match_replay_v1"))
        assertTrue(html.contains("admin_competition_overview_v1"))
        assertTrue(html.contains("admin_dictionary_release_state_v1"))
        assertTrue(html.contains("admin_publish_dictionary_release_v1"))
        assertTrue(html.contains("admin_rollback_dictionary_release_v1"))
        assertTrue(html.contains("admin_monthly_revenue_v1"))
        assertTrue(html.contains("admin_top_products_v1"))
        assertTrue(html.contains("admin_top_store_items_v1"))
        assertTrue(html.contains("admin_close_room"))
        assertTrue(html.contains("matchmaking_enabled"))
        assertTrue(html.contains("15 saniye"))
    }

    @Test
    fun replayRpcIsReadOnlyAndAdminOnly() {
        val sql = projectFile("supabase/migrations/20260907103000_admin_match_replay_v1.sql").readText()
        assertTrue(sql.contains("admin_match_replay_v1"))
        assertTrue(sql.contains("public.game_words"))
        assertTrue(sql.contains("public.word_siege_moves"))
        assertTrue(sql.contains("auth.uid() is null or not public.is_admin(auth.uid())"))
        assertTrue(sql.contains("revoke all on function public.admin_match_replay_v1(uuid) from public,anon"))
        assertTrue(sql.contains("grant execute on function public.admin_match_replay_v1(uuid) to authenticated,service_role"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
