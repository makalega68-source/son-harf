package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SiegeMetaContractTest {
    @Test
    fun `siege meta functions are new, authenticated and server-authoritative`() {
        val sql = file("supabase/migrations/20260924190000_siege_meta_v1.sql")
        listOf(
            "get_siege_match_history_v1", "get_siege_rivals_v1", "get_siege_missions_v1",
            "claim_siege_mission_v1", "get_daily_reward_cycle_v1", "claim_daily_reward_cycle_v1", "get_public_cosmetics_v1",
        ).forEach { name ->
            assertTrue("$name missing", sql.contains("create or replace function public.$name("))
            assertTrue("$name must not be anon-callable", sql.contains("revoke all on function public.$name("))
        }
        assertTrue(sql.contains("security definer set search_path = ''"))
        assertTrue(sql.contains("on conflict (user_id, mission_id, period_start) do nothing"))
        assertTrue(sql.contains("on conflict (user_id, checkin_date) do nothing"))
        assertTrue(sql.contains("array[30, 40, 50, 60, 80, 100, 150]"))
        // Existing functions are not redefined here.
        assertFalse(sql.contains("function public.claim_daily_checkin_v1"))
        assertFalse(sql.contains("function public.get_unified_missions_v1"))
    }

    @Test
    fun `the app reads missions, the reward cycle, rivals and cosmetics from the server`() {
        val retention = file("app/src/main/java/com/sonharf/game/ProfessionalRetentionScreen.kt")
        val social = file("app/src/main/java/com/sonharf/game/ProfessionalSocialScreen.kt")
        val profile = file("app/src/main/java/com/sonharf/game/PlayerProfileSheet.kt")
        assertTrue(retention.contains("backend.getSiegeMissions()"))
        assertTrue(retention.contains("backend.claimSiegeMission(mission.missionId)"))
        assertTrue(retention.contains("backend.getDailyRewardCycle()"))
        assertTrue(retention.contains("backend.claimDailyRewardCycle()"))
        assertTrue(social.contains("backend.getSiegeRivals(20)"))
        assertTrue(profile.contains("backend.getPublicCosmetics(playerId)"))
    }

    private fun file(path: String): String =
        sequenceOf(File(path), File("../$path")).firstOrNull { it.exists() }?.readText() ?: error("Missing $path")
}
