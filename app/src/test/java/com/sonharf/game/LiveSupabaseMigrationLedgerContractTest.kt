package com.sonharf.game

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveSupabaseMigrationLedgerContractTest {
    @Test
    fun `live migration provenance stays documented without replaying superseded migrations`() {
        val ledger = repoFile("docs/LIVE_SUPABASE_MIGRATION_LEDGER.md").readText()
        listOf(
            "20260831161225",
            "20260831161544",
            "20260831161852",
            "20260904083102",
            "20260904083134",
            "20260904083238",
            "20260904083258",
            "20260904083323",
            "20260904083343",
            "20260904083438",
            "20260904083620",
            "20260904083637",
            "20260906160322",
            "20260906160407",
            "20260907095809",
            "20260907104346",
            "20260907200345",
            "20260908102724",
            "20260908102820",
            "20260910071515",
            "20260910113149",
            "20260911150315",
            "20260911151543",
            "20260912090721",
        ).forEach { version -> assertTrue("Missing live migration version $version", ledger.contains(version)) }

        assertFalse(repoFileExists("supabase/migrations/20260906190500_core_duel_server_authoritative_bot_rpc.sql"))

        listOf(
            "20260831163500_admin_security_hardening_v3.sql",
            "20260831165000_owner_son_coin_purchase_fix.sql",
            "20260831171500_admin_rpc_execute_hardening_v1.sql",
        ).forEach { staleName ->
            assertFalse("Stale #191 migration must stay non-executable: $staleName", repoFileExists("supabase/migrations/$staleName"))
        }

        listOf(
            "20260904090000_store_vip_production_hardening.sql",
            "20260904090500_play_entitlement_reconciliation.sql",
            "20260904091000_reward_center_v8.sql",
            "20260904091500_style_trial_direct.sql",
            "20260904092000_word_siege_area_score_authority.sql",
            "20260904092500_vip_social_authority.sql",
            "20260904093000_vip_match_analysis.sql",
            "20260904093500_season_store_tracks.sql",
            "20260904094000_season_style_equip_hardening.sql",
        ).forEach { staleName ->
            assertFalse("Stale #243 migration must stay non-executable: $staleName", repoFileExists("supabase/migrations/$staleName"))
        }
        assertTrue(repoFileExists("supabase/migrations/20260904083134_play_entitlement_reconciliation.sql"))
        assertTrue(repoFileExists("supabase/migrations/20260912103000_shop_sale_window_purchase_enforcement.sql"))

        val releasedSubmit = repoFile("supabase/migrations/20260907103000_remove_bilbakalim_and_release_bot_submit.sql").readText()
        assertTrue(releasedSubmit.contains("language sql"))
        assertTrue(releasedSubmit.contains("select public.submit_word_v3_core_v1(p_room_id, p_word)"))
        assertFalse(releasedSubmit.contains("r := public.bot_take_turn(r.id)"))

        val timeout = repoFile("supabase/migrations/20260910060000_fix_premier_bot_timeout.sql").readText()
        assertTrue(timeout.contains("('playing','final','sudden_death')"))
        assertTrue(timeout.contains("final_moves_remaining"))
        assertTrue(timeout.contains("bot_turn = (r.is_bot and timed_out_player = r.host_id)"))

        val turnClock = repoFile("supabase/migrations/20260910142500_premier_turn_15_seconds.sql").readText()
        assertTrue(turnClock.contains("interval '15 seconds'"))

        val topology = repoFile("supabase/migrations/20260911170000_word_siege_custom_topology_v8.sql").readText()
        assertTrue(topology.contains("revoke all on function private.word_siege_new_board_v1() from public, anon, authenticated"))

        val invites = repoFile("supabase/migrations/20260911173000_word_siege_friend_invites_v9.sql").readText()
        assertTrue(invites.contains("if inv.expires_at < now() then"))
        assertTrue(invites.contains("set status = 'expired', responded_at = now()"))
        assertTrue(invites.contains("return null;"))

        listOf(
            "docs/live-supabase-history/20260906114237_restore_single_verified_theme_catalog.sql",
            "docs/live-supabase-history/20260906160322_core_duel_bot_server_authority_v5.sql",
            "docs/live-supabase-history/20260906160407_core_duel_existing_rpc_bot_autocontinue.sql",
            "docs/live-supabase-history/20260907200345_fix_midmatch_quiz_longword_steal.sql",
        ).forEach { path -> assertAuditOnlySnapshot(path) }

        val exactLiveSnapshots = mapOf(
            "docs/live-supabase-history/20260831161225_admin_security_hardening_v3.sql" to "4e4936c026dc0b44fa50c8c95543a4a9",
            "docs/live-supabase-history/20260831161544_owner_son_coin_purchase_fix.sql" to "de99c49d4495bce975af9d58f6eff0cc",
            "docs/live-supabase-history/20260831161852_admin_rpc_execute_hardening_v1.sql" to "cb0a0dc7bba55554e7b9284bed760aad",
            "docs/live-supabase-history/20260904083102_store_vip_production_hardening.sql" to "90a87a9481c9313b9cc3760b71fba46f",
            "docs/live-supabase-history/20260904083238_reward_center_v8.sql" to "435312e09b3ef3c193f11453aec886e4",
            "docs/live-supabase-history/20260904083258_style_trial_direct.sql" to "85a400fd59d26323ffacc7f31c212395",
            "docs/live-supabase-history/20260904083323_word_siege_area_score_authority.sql" to "840ddd901b45c51eda90302f28d70f76",
            "docs/live-supabase-history/20260904083343_vip_social_authority.sql" to "ce8ad8375a3fd0c955d831a7059b509f",
            "docs/live-supabase-history/20260904083438_vip_match_analysis.sql" to "3d3c70b73a8b9feb3bd4fdc0cdefac12",
            "docs/live-supabase-history/20260904083620_season_store_tracks.sql" to "7b32bf574b9f835001bfcbf8a9c812fa",
            "docs/live-supabase-history/20260904083637_season_style_equip_hardening.sql" to "ecc32b097486e24a34b46ce927fd872c",
        )
        exactLiveSnapshots.forEach { (path, expectedMd5) ->
            val snapshot = assertAuditOnlySnapshot(path)
            assertTrue("Snapshot must declare its live MD5: $path", snapshot.contains("Live statement MD5: $expectedMd5"))
            val sqlBody = snapshot.substringAfter("\n\n")
            assertEquals("Live statement body drifted: $path", expectedMd5, md5(sqlBody))
        }

        val adminHardening = repoFile("docs/live-supabase-history/20260831161225_admin_security_hardening_v3.sql").readText()
        assertTrue(adminHardening.contains("revoke all on function public.is_admin() from public, anon"))
        assertTrue(adminHardening.contains("revoke all on function public.admin_access_v1() from public, anon"))
        assertTrue(adminHardening.contains("revoke all on function public.enforce_chat_admin_control_v1() from public, anon, authenticated"))
        assertTrue(adminHardening.contains("p_turn_duration_hours integer default 12"))
        assertTrue(ledger.contains("güncel Kelime Tahtı matchmaking/lifecycle source-of-truth değildir"))

        val ownerPurchaseFix = repoFile("docs/live-supabase-history/20260831161544_owner_son_coin_purchase_fix.sql").readText()
        assertTrue(ownerPurchaseFix.contains("unlimited_son_coin"))
        assertTrue(ownerPurchaseFix.contains("revoke all on function public.buy_mascot_fruit_v1(text,integer) from public, anon"))

        val adminExecuteHardening = repoFile("docs/live-supabase-history/20260831161852_admin_rpc_execute_hardening_v1.sql").readText()
        assertTrue(adminExecuteHardening.contains("revoke all on function public.admin_dashboard_v1() from public, anon"))
        assertTrue(adminExecuteHardening.contains("revoke all on function public.is_admin() from public, anon"))
        assertTrue(adminExecuteHardening.contains("revoke all on function public.enforce_new_game_admin_controls_v1() from public, anon, authenticated"))
        assertTrue(adminExecuteHardening.contains("revoke all on function public.enforce_chat_admin_control_v1() from public, anon, authenticated"))
        assertTrue(adminExecuteHardening.contains("grant execute on function public.admin_dashboard_v1() to authenticated, service_role"))
    }

    private fun assertAuditOnlySnapshot(path: String): String {
        val snapshot = repoFile(path).readText()
        assertTrue(snapshot.contains("AUDIT-ONLY LIVE SNAPSHOT"))
        assertTrue(snapshot.contains("DO NOT EXECUTE"))
        return snapshot
    }

    private fun md5(value: String): String = MessageDigest.getInstance("MD5")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun repoFile(path: String): File = sequenceOf(File(path), File("../$path"))
        .firstOrNull { it.exists() }
        ?: error("Missing repository file: $path")

    private fun repoFileExists(path: String): Boolean = sequenceOf(File(path), File("../$path")).any { it.exists() }
}
