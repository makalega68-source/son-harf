package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityDefinerExecuteSurfaceContractTest {
    @Test
    fun triggerOnlyFunctionsAreNotApiExecutable() {
        val sql = projectFile("supabase/migrations/20260907111500_security_definer_execute_surface_v1.sql").readText()
        listOf(
            "apply_bomb_duel_deadline_v1()",
            "grant_default_mascot_inventory()",
            "grant_legend_profile_frame_v1()",
            "reject_terminal_soft_g_game_word()",
            "v7_profiles_xp_bonus_trigger()",
        ).forEach { fn ->
            assertTrue(sql.contains("revoke all on function public.$fn from public,anon,authenticated"))
            assertTrue(sql.contains("grant execute on function public.$fn to service_role"))
        }
    }

    @Test
    fun authenticatedPlayerMutationsRejectAnonAtPrivilegeLayer() {
        val sql = projectFile("supabase/migrations/20260907111500_security_definer_execute_surface_v1.sql").readText()
        assertTrue(sql.contains("revoke all on function public.claim_daily_goal_v10(text) from public,anon"))
        assertTrue(sql.contains("grant execute on function public.claim_daily_goal_v10(text) to authenticated,service_role"))
        assertTrue(sql.contains("revoke all on function public.set_display_name(text) from public,anon"))
        assertTrue(sql.contains("grant execute on function public.set_display_name(text) to authenticated,service_role"))
        assertFalse(sql.contains("grant execute on function public.claim_daily_goal_v10(text) to anon"))
        assertFalse(sql.contains("grant execute on function public.set_display_name(text) to anon"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
