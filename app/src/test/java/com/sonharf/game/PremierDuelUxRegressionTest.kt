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

        // Real player profiles remain visible, but Son Harf has no level/XP badge.
        assertTrue(screen.contains("PremierCalmPlayerCard("))
        assertTrue(screen.contains("ProfilePhotoAvatarRectWithGender("))
        assertTrue(screen.contains("nameColor = SonHarfCosmetics.playerNameColor"))
        assertFalse(screen.contains("MageCatCompanion("))
        assertFalse(screen.contains("SyntheticBotPortrait("))
        assertFalse(screen.contains("Seviye"))

        // Chat remains typed/realtime and keeps the unread red indicator.
        assertTrue(screen.contains("Text(pt(language, \"Sohbet\", \"Chat\")"))
        assertFalse(screen.contains("enabled = !room.isBot"))
        assertTrue(screen.contains("var hasUnreadChat by remember { mutableStateOf(false) }"))
        assertTrue(screen.contains("if (latest != null && latest.id != previousId && latest.senderId != backend.currentUserId())"))
        assertTrue(screen.contains("hasUnreadChat = !showQuickChat"))
        assertTrue(screen.contains("unreadChat = hasUnreadChat"))
        assertTrue(screen.contains("if (unreadChat)"))
        assertTrue(screen.contains("background(PremierUi.Red)"))
        assertTrue(screen.contains("hasUnreadChat = false"))

        // Server-authoritative gameplay and recovery paths are unchanged.
        assertTrue(screen.contains("turnSeconds = 1"))
        assertTrue(screen.contains("backend.claimTurnTimeout(active.id)"))
        assertTrue(screen.contains("backend.botTakeTurn(active.id)"))
        assertTrue(screen.contains("backend.submitPremierWord(active.id, candidate)"))
        assertFalse(screen.contains("backend.validateCoreWordDetailed(candidate"))
        assertFalse(screen.contains("timeoutClaimKey"))
        assertTrue(backend.contains("submitWord(roomId, word)"))
        assertTrue(backend.contains("getRoom(roomId)"))
        assertTrue(backend.contains("botTakeTurn(roomId)"))
        assertFalse(backend.contains("submit_word_v4"))

        assertTrue(screen.contains("found?.isBot == true && found.isPremierLive()"))
        assertTrue(screen.contains("backend.resumePremierBotMatch(found.id)"))
        assertTrue(onlineBackend.contains("suspend fun resumePremierBotMatch(roomId: String): GameRoomDto"))
        assertTrue(onlineBackend.contains("\"resume_premier_bot_match_v1\""))
        assertTrue(onlineBackend.contains("put(\"p_room_id\", roomId)"))

        // Visible timer remains server-clock anchored and monotonic.
        assertTrue(screen.contains("private const val PREMIER_TURN_SECONDS = 15"))
        assertTrue(screen.contains("fetchPremierTurnClock(active.id)"))
        assertTrue(screen.contains("SystemClock.elapsedRealtime()"))
        assertTrue(screen.contains("premierRemainingTurnSecondsFromMillis(initialRemainingMs - elapsedMs)"))
        assertTrue(turnClock.contains("data class PremierTurnClockDto"))
        assertTrue(turnClock.contains("\"get_premier_turn_clock_v1\""))
        assertTrue(turnClock.contains("put(\"p_room_id\", roomId)"))

        // Approved best-of-three / 10-word round HUD is bound to live room state.
        assertTrue(screen.contains("Raund \${room.roundNo} / 3"))
        assertTrue(screen.contains("2 raund kazanan"))
        assertTrue(screen.contains("room.roundWordCount.coerceIn(0, 10)"))
        assertTrue(screen.contains("Raund Puanı"))
        assertTrue(screen.contains("Text(\"\$myRounds - \$rivalRounds\""))

        // Latest word is always playable context; history/found-word panels stay present but PRO-gated.
        assertTrue(screen.contains("val lastWord = words.lastOrNull()"))
        assertTrue(screen.contains("PremierLastWordBar(language = language, word = lastWord, meId = meId)"))
        assertTrue(screen.contains("private fun PremierProWordPanel("))
        assertTrue(screen.contains("Bulunan Kelimeler"))
        assertTrue(screen.contains("Kelime Geçmişi"))
        assertTrue(screen.contains("Sadece PRO üyeler görebilir"))

        // Target card has no previous/next arrow controls and remains compact on real devices.
        assertTrue(screen.contains("val targetSize = if (compact) 94.dp else 116.dp"))
        val targetStart = screen.indexOf("private fun PremierTargetCard(")
        val targetEnd = screen.indexOf("private fun PremierProWordPanel(", targetStart)
        assertTrue(targetStart >= 0 && targetEnd > targetStart)
        val target = screen.substring(targetStart, targetEnd)
        assertFalse(target.contains("ChevronLeft"))
        assertFalse(target.contains("ChevronRight"))
        assertFalse(target.contains("ArrowLeft"))
        assertFalse(target.contains("ArrowRight"))

        // Native Android IME owns the keyboard and Done action; there is no arena custom-keyboard call.
        assertTrue(screen.contains("LocalSoftwareKeyboardController.current"))
        assertTrue(screen.contains("KeyboardOptions("))
        assertTrue(screen.contains("ImeAction.Done"))
        assertTrue(screen.contains("onDone = { if (enabled && input.isNotBlank()) onSubmit() }"))
        val arenaStart = screen.indexOf("private fun PremierArena(")
        val headerStart = screen.indexOf("private fun PremierArenaHeader(", arenaStart)
        val arena = screen.substring(arenaStart, headerStart)
        assertFalse(arena.contains("PremierKeyboard("))

        // Purchased action VFX stays cosmetic on turn arrival and accepted moves.
        assertTrue(screen.contains("PurchasedVictoryVfx("))
        assertTrue(screen.contains("eventKey = \"turn:"))
        assertTrue(screen.contains("eventKey = \"accepted:"))

        // Send still clears the visible attempt before the authoritative server result arrives.
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
        assertFalse(screen.contains("PremierStatPill(pt(language, \"SUNUCU\", \"SERVER\")"))

        // Chat is a typed transcript for human and bot matches; canned quick-message UI stays gone.
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
