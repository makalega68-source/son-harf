package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminReleaseOperationsContractTest {
    @Test
    fun adminFallbackOperationsAreBackedByRealTablesAndAdminGuards() {
        val sql = projectFile("supabase/migrations/20260907100000_admin_release_operations_v1.sql").readText()
        assertTrue(sql.contains("admin_recent_match_analysis_v1"))
        assertTrue(sql.contains("public.match_analysis_snapshots"))
        assertTrue(sql.contains("admin_competition_overview_v1"))
        assertTrue(sql.contains("public.ensure_competitive_season_v1"))
        assertTrue(sql.contains("public.ensure_weekly_tournament_v1"))
        assertTrue(sql.contains("admin_dictionary_release_state_v1"))
        assertTrue(sql.contains("public.dictionary_release_state"))
        assertTrue(sql.contains("auth.uid() is null or not public.is_admin(auth.uid())"))
        assertFalse(sql.contains("grant execute on function public.admin_recent_match_analysis_v1(integer,text) to anon"))
        assertFalse(sql.contains("grant execute on function public.admin_competition_overview_v1() to anon"))
        assertFalse(sql.contains("grant execute on function public.admin_dictionary_release_state_v1() to anon"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
