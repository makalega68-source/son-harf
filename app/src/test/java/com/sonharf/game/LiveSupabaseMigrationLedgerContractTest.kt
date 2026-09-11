package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveSupabaseMigrationLedgerContractTest {
    @Test
    fun `live migration provenance stays documented without replaying superseded bot rpc`() {
        val ledger = repoFile("docs/LIVE_SUPABASE_MIGRATION_LEDGER.md").readText()
        listOf(
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
        ).forEach { version -> assertTrue("Missing live migration version $version", ledger.contains(version)) }

        assertFalse(repoFileExists("supabase/migrations/20260906190500_core_duel_server_authoritative_bot_rpc.sql"))

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
        ).forEach { path ->
            val snapshot = repoFile(path).readText()
            assertTrue(snapshot.contains("AUDIT-ONLY LIVE SNAPSHOT"))
            assertTrue(snapshot.contains("DO NOT EXECUTE"))
        }
    }

    private fun repoFile(path: String): File = sequenceOf(File(path), File("../$path"))
        .firstOrNull { it.exists() }
        ?: error("Missing repository file: $path")

    private fun repoFileExists(path: String): Boolean = sequenceOf(File(path), File("../$path")).any { it.exists() }
}
