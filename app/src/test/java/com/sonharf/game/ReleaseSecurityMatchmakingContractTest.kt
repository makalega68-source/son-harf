package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseSecurityMatchmakingContractTest {
    @Test fun timeoutRpcRejectsAnonymousAndUsesNullSafeParticipantCheck() {
        val sql = hardeningMigration().readText()
        assertTrue(sql.contains("if v_uid is null then raise exception 'not_authenticated'; end if;"))
        assertTrue(sql.contains("v_uid is distinct from r.host_id and v_uid is distinct from r.guest_id"))
        assertTrue(sql.contains("revoke all on function public.claim_turn_timeout_v2(uuid,uuid,timestamptz) from public,anon;"))
        assertTrue(sql.contains("grant execute on function public.claim_turn_timeout_v2(uuid,uuid,timestamptz) to authenticated,service_role;"))
    }

    @Test fun matchmakingWaitsFullFifteenSecondsAndProtectsSingleQueueResolution() {
        val sql = hardeningMigration().readText()
        assertTrue(sql.contains("q.queued_at<=now()-interval '15 seconds'"))
        assertFalse(sql.contains("q.queued_at<=now()-interval '10 seconds'"))
        assertTrue(sql.contains("where user_id=v_uid and status='waiting'"))
        assertTrue(sql.contains("if not found then"))
        assertTrue(sql.contains("delete from public.game_rooms where id=r.id and host_id=v_uid and is_bot=true;"))
        assertTrue(sql.contains("if v_uid is null then raise exception 'not_authenticated'; end if;"))
    }

    private fun hardeningMigration(): File = projectFile(
        "supabase/migrations/20260907020500_release_security_matchmaking_hardening.sql",
    )

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
