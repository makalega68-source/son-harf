package com.sonharf.game

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Locks the real-device fixes requested for the rebuilt Premier 1v1 arena.
class PremierDuelUxRegressionTest {
    @Test fun premierArenaKeepsProfilesVisibleAndServerAuthoritativeRecovery() {
        val screen = File("src/main/java/com/sonharf/game/PremierWordDuelScreen.kt").readText()
        val backend = File("src/main/java/com/sonharf/game/data/PremierDuelBackend.kt").readText()
        val onlineBackend = File("src/main/java/com/sonharf/game/data/OnlineGameBackend.kt").readText()
        val turnClock = File("src/main/java/com/sonharf/game/data/PremierTurnClock.kt").readText()

        // Human profile photos use the rectangular runtime so purchased rectangular frames align.
        assertTrue(screen.contains("ProfilePhotoAvatarRectWithGender("))
        assertTrue(screen.contains("width = 70.dp"))
        assertTrue(screen.contains("height = 54.dp"))
        assertTrue(screen.contains("width = 84.dp"))
        assertTrue(screen.contains("height = 64.dp"))
        assertTrue(screen.contains("PremierBotAvatar(size = 58.dp"))
        assertTrue(screen.contains("PremierBotAvatar(size = 70.dp"))
        assertFalse(screen.contains("MageCatCompanion("))
        assertFalse(screen.contains("SyntheticBotPortrait("))

        // Chat remains typed/realtime and now has an unread red indicator.
        assertTrue(screen.contains("Text(pt(language, \"SOHBET\", \"CHAT\")"))
        assertFalse(screen.contains("enabled = !room.isBot"))
        assertTrue(screen.contains("var hasUnreadChat by remember { mutableStateOf(false) }"))
        assertTrue(screen.contains("if (latest != null && latest.id != previousId && latest.senderId != backend.currentUserId())"))
        assertTrue(screen.contains("hasUnreadChat = !showQuickChat"))
        assertTrue(screen.contains("unreadChat = hasUnreadChat"))
        assertTrue(screen.contains("Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-3).dp).size(10.dp).clip(CircleShape).background(PremierUi.Red)"))
        assertTrue(screen.contains("hasUnreadChat = false"))

        assertTrue(screen.contains("Alignment.CenterStart"))
        assertTrue(screen.contains("Alignment.CenterEnd"))
        assertTrue(screen.contains("turnSeconds = 1"))
        assertTrue(screen.contains("backend.claimTurnTimeout(active.id)"))
        assertTrue(screen.contains("backend.botTakeTurn(active.id)"))
        assertTrue(screen.contains("backend.submitPremierWord(active.id, candidate)"))
        assertFalse(screen.contains("backend.validateCoreWordDetailed(candidate"))
        assertTrue(screen.contains("modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)"))
        assertFalse(screen.contains("timeoutClaimKey"))
        assertTrue(backend.contains("submitWord(roomId, word)"))
        assertTrue(backend.contains("getRoom(roomId)"))
        assertTrue(backend.contains("botTakeTurn(roomId)"))
        assertFalse(backend.contains("submit_word_v4"))

        // Returning to a live bot room must refresh the server deadline instead of charging offline time.
        assertTrue(screen.contains("found?.isBot == true && found.isPremierLive()"))
        assertTrue(screen.contains("backend.resumePremierBotMatch(found.id)"))
        assertTrue(onlineBackend.contains("suspend fun resumePremierBotMatch(roomId: String): GameRoomDto"))
        assertTrue(onlineBackend.contains("\"resume_premier_bot_match_v1\""))
        assertTrue(onlineBackend.contains("put(\"p_room_id\", roomId)"))

        // The 15-second visible timer stays server-clock anchored and monotonic on-device.
        assertTrue(screen.contains("private const val PREMIER_TURN_SECONDS = 15"))
        assertTrue(screen.contains("fetchPremierTurnClock(active.id)"))
        assertTrue(screen.contains("SystemClock.elapsedRealtime()"))
        assertTrue(screen.contains("premierRemainingTurnSecondsFromMillis(initialRemainingMs - elapsedMs)"))
        assertTrue(screen.contains("\"15 sn tur\""))
        assertTrue(turnClock.contains("data class PremierTurnClockDto"))
        assertTrue(turnClock.contains("\"get_premier_turn_clock_v1\""))
        assertTrue(turnClock.contains("put(\"p_room_id\", roomId)"))

        // The old instruction is removed; the latest played word is the central context line.
        assertTrue(screen.contains("val latestPlayedWord"))
        assertTrue(screen.contains("latestPlayedWord.ifBlank"))
        assertFalse(screen.contains("“\$required” ile başlayan bir kelime yaz"))
        assertFalse(screen.contains("Enter a word starting with “\$required”"))
        assertTrue(screen.contains("fontSize = if (veryCompact) 14.sp else 16.sp"))

        // History chips center as a group instead of hugging the left edge.
        assertTrue(screen.contains("Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally)"))

        // Purchased action VFX is used cosmetically on turn arrival and accepted moves.
        assertTrue(screen.contains("PurchasedVictoryVfx("))
        assertTrue(screen.contains("eventKey = \"turn:"))
        assertTrue(screen.contains("eventKey = \"accepted:"))

        // Target card and central letter remain compact on real devices.
        assertTrue(screen.contains("if (veryCompact) 78.dp"))
        assertTrue(screen.contains("if (compact) 88.dp"))
        assertTrue(screen.contains("if (tall) 118.dp"))
        assertTrue(screen.contains("else 104.dp"))
        assertTrue(screen.contains("if (required.length > 1) .32f else .42f"))
        assertFalse(screen.contains("if (tall) 164.dp"))

        // Send consumes the visible attempt immediately, then the authoritative server result arrives.
        val candidateIndex = screen.indexOf("val candidate = input")
        val clearIndex = screen.indexOf("input = \"\"", candidateIndex)
        val submitIndex = screen.indexOf("backend.submitPremierWord(active.id, candidate)", candidateIndex)
        assertTrue(candidateIndex >= 0)
        assertTrue(clearIndex > candidateIndex)
        assertTrue(submitIndex > clearIndex)
        assertTrue(screen.contains("val accepted = next.validWordCount > active.validWordCount"))
        assertTrue(screen.contains("pt(language, \"DOĞRU\", \"CORRECT\")"))
        assertTrue(screen.contains("pt(language, \"YANLIŞ\", \"WRONG\")"))
        assertTrue(screen.contains("PremierMoveFeedback("))

        // The input bar no longer carries the redundant server badge.
        assertFalse(screen.contains("PremierStatPill(pt(language, \"SUNUCU\", \"SERVER\")"))

        // Chat is a typed transcript for human and bot matches; canned quick-message UI is gone.
        assertTrue(screen.contains("private fun PremierChatSheet("))
        assertTrue(screen.contains("messages = if (room?.isBot == true) botChat else chat"))
        assertTrue(screen.contains("OutlinedTextField("))
        assertTrue(screen.contains("\"Mesaj yaz…\""))
        assertTrue(screen.contains("backend.sendChat(active.id, message)"))
        assertTrue(screen.contains("backend.getChat(active.id)"))
        assertTrue(screen.contains("premierBotChatReply(language, message)"))
        assertTrue(screen.contains("Bot ile serbestçe yazış."))
        assertFalse(screen.contains("quickMessages"))
        assertFalse(screen.contains("Hızlı reaksiyonlar"))
        assertFalse(screen.contains("Bot maçında gerçek mesajlaşma kapalıdır."))
    }

    @Test fun serverAnchoredCountdownRoundsUpWithoutSkippingSeconds() {
        assertEquals(15, premierRemainingTurnSecondsFromMillis(20_000))
        assertEquals(15, premierRemainingTurnSecondsFromMillis(15_000))
        assertEquals(15, premierRemainingTurnSecondsFromMillis(14_999))
        assertEquals(11, premierRemainingTurnSecondsFromMillis(10_001))
        assertEquals(10, premierRemainingTurnSecondsFromMillis(10_000))
        assertEquals(1, premierRemainingTurnSecondsFromMillis(1))
        assertEquals(0, premierRemainingTurnSecondsFromMillis(0))
        assertEquals(0, premierRemainingTurnSecondsFromMillis(-1))
    }
}
