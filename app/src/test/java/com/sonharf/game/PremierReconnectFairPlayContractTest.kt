package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PremierReconnectFairPlayContractTest {
    @Test fun reconnectGraceBeatsNormalTurnTimeout() {
        val migration = projectFile("supabase/migrations/20260920161000_premier_reconnect_fairness_v1.sql").readText()
        assertTrue(migration.contains("r.disconnected_player_id=r.current_player_id"))
        assertTrue(migration.contains("r.reconnect_deadline <= clock_timestamp()"))
        assertTrue(migration.contains("return public.sonharf_finish_room(r.id,reconnect_winner,false,'reconnect_timeout')"))
        assertTrue(migration.indexOf("r.disconnected_player_id=r.current_player_id") < migration.indexOf("r.turn_deadline is null"))
    }

    @Test fun reconnectCountdownUsesParticipantOnlyServerClock() {
        val migration = projectFile("supabase/migrations/20260920161000_premier_reconnect_fairness_v1.sql").readText()
        val backend = projectFile("app/src/main/java/com/sonharf/game/data/OnlineGameBackend.kt").readText()
        val screen = projectFile("app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt").readText()

        assertTrue(migration.contains("get_premier_reconnect_clock_v1"))
        assertTrue(migration.contains("grant execute on function public.get_premier_reconnect_clock_v1(uuid) to authenticated"))
        assertTrue(backend.contains("getPremierReconnectClock"))
        assertTrue(screen.contains("reconnectClock.remainingMs"))
        assertTrue(screen.contains("PREMIER_RECONNECT_SECONDS * 1000L"))
    }

    @Test fun clientShowsReconnectGraceInsteadOfClaimingTurnTimeout() {
        val screen = projectFile("app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt").readText()
        assertTrue(screen.contains("room?.disconnectedPlayerId"))
        assertTrue(screen.contains("room?.reconnectDeadline"))
        assertTrue(screen.contains("backend.heartbeatRoom(active.id)"))
        assertTrue(screen.contains("PremierReconnectBanner"))
        assertFalse(screen.contains("reconnect_deadline_client_authority"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
