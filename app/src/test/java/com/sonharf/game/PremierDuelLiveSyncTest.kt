package com.sonharf.game

import com.sonharf.game.data.GameRoomDto
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Locks the Son Harf live-sync fixes: no frozen "01" timer, no stale room overwrites,
// abandoned rooms swept on the server, readable arena overlays and working hints.
class PremierDuelLiveSyncTest {
    private fun room(
        words: Int = 4,
        round: Int = 1,
        current: String? = "host",
        deadline: String? = "2026-09-29T20:26:23.533775+00:00",
        status: String = "playing",
        isBot: Boolean = false,
        botTurn: Boolean = false,
        lastEvent: String? = null,
    ) = GameRoomDto(
        id = "room",
        code = "ABCD",
        hostId = "host",
        guestId = if (isBot) null else "guest",
        status = status,
        validWordCount = words,
        roundNo = round,
        currentPlayerId = current,
        turnDeadline = deadline,
        isBot = isBot,
        botTurn = botTurn,
        lastEvent = lastEvent,
    )

    @Test fun unchangedTimeoutAnswerIsNotProgress() {
        val active = room()
        // Same moment in another offset format is the same deadline.
        assertFalse(premierTurnStateChanged(active, active.copy(turnDeadline = "2026-09-29T20:26:23.533775Z", hostScore = 3)))
        assertTrue(premierTurnStateChanged(active, active.copy(currentPlayerId = "guest")))
        assertTrue(premierTurnStateChanged(active, active.copy(turnDeadline = "2026-09-29T20:26:38.533775+00:00")))
        assertTrue(premierTurnStateChanged(active, active.copy(status = "finished")))
        assertTrue(premierTurnStateChanged(active, active.copy(disconnectedPlayerId = "host")))
    }

    @Test fun olderRoomSnapshotsAreRejected() {
        val current = room(words = 5, current = "guest", deadline = "2026-09-29T20:26:40+00:00")
        // A word behind, a round behind, or a live room after the result: older.
        assertTrue(premierRoomSnapshotIsOlder(room(words = 4), current))
        assertTrue(premierRoomSnapshotIsOlder(room(words = 9, round = 1), current.copy(roundNo = 2)))
        assertTrue(premierRoomSnapshotIsOlder(current, current.copy(status = "finished", winnerId = "host")))
        // Same word count, the turn passed by a timeout: the earlier deadline is older.
        assertTrue(premierRoomSnapshotIsOlder(room(words = 5, current = "host", deadline = "2026-09-29T20:26:25+00:00"), current))
        assertFalse(premierRoomSnapshotIsOlder(current, room(words = 5, current = "host", deadline = "2026-09-29T20:26:25+00:00")))
        // Newer or equal snapshots and other rooms are accepted.
        assertFalse(premierRoomSnapshotIsOlder(room(words = 6), current))
        assertFalse(premierRoomSnapshotIsOlder(current.copy(hostScore = 12), current))
        assertFalse(premierRoomSnapshotIsOlder(room(words = 0).copy(id = "rematch"), current))
    }

    @Test fun missedHumanTurnAgainstTheAiIsNotUndone() {
        val botTurn = room(isBot = true, current = null, deadline = null, botTurn = true, lastEvent = "turn_expired")
        val staleHuman = room(isBot = true, current = "host")
        assertTrue(premierRoomSnapshotIsOlder(staleHuman, botTurn))
        assertFalse(premierRoomSnapshotIsOlder(staleHuman.copy(lastEvent = "bot_missed"), botTurn))
    }

    @Test fun freeHintsFollowOwnershipAndFairPlay() {
        // Ownership that loads mid-match still grants the unused free hints.
        assertEquals(0, MascotHints.freeHintsLeft(realOpponent = false, used = 0, ownsMascot = false))
        assertEquals(3, MascotHints.freeHintsLeft(realOpponent = false, used = 0, ownsMascot = true))
        assertEquals(1, MascotHints.freeHintsLeft(realOpponent = false, used = 2, ownsMascot = true))
        assertEquals(0, MascotHints.freeHintsLeft(realOpponent = false, used = 5, ownsMascot = true))
        // Real opponents: tips only, free for everyone.
        assertEquals(3, MascotHints.freeHintsLeft(realOpponent = true, used = 0, ownsMascot = false))
    }

    @Test fun arenaSourceKeepsTheFixes() {
        val screen = projectFile("app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt").readText()
        val companion = projectFile("app/src/main/java/com/sonharf/game/WordSiegeMascotCompanion.kt").readText()

        // Timer: an unchanged claim answer re-anchors to the server clock instead of freezing.
        assertTrue(screen.contains("if (advanced != null && premierTurnStateChanged(active, advanced))"))
        assertTrue(screen.contains("syncTurnClock()"))
        // Every live room update goes through the monotonic guard.
        assertTrue(screen.contains("fun acceptRoom(next: GameRoomDto): Boolean"))
        assertFalse(screen.contains("room = advanced"))
        assertFalse(screen.contains("room = synced"))
        assertTrue(screen.contains(".collect { acceptWords(it) }"))
        assertTrue(screen.contains("enabled = myTurn && !busy && !preparing && boardSynced"))
        // Streak badge sits in the timer strip, not over the player cards.
        assertFalse(screen.contains("padding(top = 92.dp)"))
        // A rival slip is readable and never hides the last word.
        assertFalse(screen.contains("PremierBoardMessage(slip, PremierBoard.RivalSoft)"))
        // The rival card lights up only on the rival's turn.
        assertTrue(screen.contains("room.currentPlayerId != meId"))
        // Mascot bubble below the perch in the duel; other screens keep the default.
        assertTrue(screen.contains("bubblePlacement = WordSiegeMascotBubblePlacement.PREFER_BELOW"))
        assertTrue(companion.contains("bubblePlacement: WordSiegeMascotBubblePlacement = WordSiegeMascotBubblePlacement.PREFER_ABOVE"))
        // Hints: shown without a mascot, failures explained, banked/bought hints only against the AI.
        assertTrue(screen.contains("val mascotShown = WordSiegeMascotOwnership.hasAny"))
        assertTrue(screen.contains("İpucu şu an alınamadı, tekrar dene."))
        assertTrue(screen.contains("val bankedHints = if (room.isBot) RewardPassState.hints(\"son_harf\") else 0"))
    }

    @Test fun abandonedRoomsAreSweptWithTheLiveTimeoutRules() {
        val sql = projectFile("supabase/migrations/20260930120000_sonharf_stale_room_sweep_v1.sql").readText()
        assertTrue(sql.contains("perform public.claim_turn_timeout(r.id);"))
        assertTrue(sql.contains("set_config('request.jwt.claim.sub', v_actor::text, true)"))
        assertTrue(sql.contains("interval '5 seconds'"))
        assertTrue(sql.contains("and not coalesce(g.is_bot, false)"))
        assertTrue(sql.contains("for update skip locked"))
        assertTrue(sql.contains("revoke all on function private.sweep_sonharf_stale_rooms_v1() from public, anon, authenticated;"))
        assertTrue(sql.contains("'sonharf_stale_room_sweep_v1',\n      '* * * * *',"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
