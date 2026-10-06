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

        // The arena keeps its compact wells; match-found cards use a circular well for cosmetic frames.
        assertTrue(screen.contains("ProfilePhotoAvatarRectWithGender("))
        assertTrue(screen.contains("width = 70.dp"))
        assertTrue(screen.contains("height = 54.dp"))
        assertTrue(screen.contains("ProfilePhotoAvatarWithGender(avatar,gender,name,70.dp"))
        assertTrue(screen.contains("botGenderForName(name)"))
        assertTrue(screen.contains("PremierBotAvatar(size = 58.dp"))
        assertTrue(screen.contains("PremierBotAvatar(size = 70.dp"))
        assertFalse(screen.contains("MageCatCompanion("))
        assertFalse(screen.contains("SyntheticBotPortrait("))
        assertTrue(screen.contains("WordSiegeMascotCompanion("))
        assertTrue(screen.contains("WordSiegeMascotEmotion.FOCUS"))
        assertTrue(screen.contains("PremierKeyboard(language, input"))
        assertTrue(screen.contains("PremierArenaSky.BackgroundTop"))
        assertTrue(screen.contains("Color(0xFFE6ECF2)"))

        // Chat remains typed/realtime and now has an unread red indicator.
        assertTrue(screen.contains("Text(pt(language, \"SOHBET\", \"CHAT\")"))
        assertFalse(screen.contains("enabled = !room.isBot"))
        assertTrue(screen.contains("var unreadChatCount by remember { mutableIntStateOf(0) }"))
        assertTrue(screen.contains("if (latest != null && latest.id != previousId && latest.senderId != backend.currentUserId())"))
        assertTrue(screen.contains("unreadChatCount = GameChatBadge.unread"))
        assertTrue(screen.contains("unreadChat = unreadChatCount"))
        assertTrue(screen.contains("ChatUnreadDot(unreadChat)"))
        assertTrue(screen.contains("GameChatBadge.markRead()"))

        assertTrue(screen.contains("PremierSymmetricPlayerCard("))
        assertTrue(screen.contains("Modifier.weight(1f).height(cardHeight)"))
        // Large in-duel portraits: the cards may only grow.
        assertTrue(screen.contains("val cardHeight = 92.dp"))
        assertTrue(screen.contains("width = 70.dp,\n                height = 70.dp,"))
        assertTrue(screen.contains("PremierBotAvatar(size = 70.dp, accent = accent, name = name)"))
        assertTrue(screen.contains("Modifier.align(Alignment.CenterEnd).size(mascotSize)"))
        assertTrue(screen.contains("turnSeconds = 1"))
        assertTrue(screen.contains("backend.claimTurnTimeout(active.id)"))
        assertTrue(screen.contains("backend.botTakeTurn(active.id)"))
        assertTrue(screen.contains("backend.submitPremierWord(active.id, candidate)"))
        assertFalse(screen.contains("backend.validateCoreWordDetailed(candidate"))
        assertTrue(screen.contains("modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)"))
        assertFalse(screen.contains("timeoutClaimKey"))
        assertTrue(backend.contains("submitWord(roomId, word)"))
        assertTrue(backend.contains("getRoom(roomId)"))
        // The AI reply is requested only by the arena (after its pause / the round break).
        assertFalse(backend.contains("runCatching { botTakeTurn(roomId) }"))
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

        // The played-words chips were removed from the arena (unreadable on the petrol board).
        assertFalse(screen.contains("PremierWordTrail("))

        // Turn and accepted-word feedback is a light sweep on the target tile, not screen-centred rings.
        assertFalse(screen.contains("PurchasedVictoryVfx("))
        assertTrue(screen.contains("PremierTileShine(shine.value"))

        // The target tile is large, and its letter is sized in dp so it never leaves the tile.
        assertTrue(screen.contains("val targetSize = if (veryCompact) 86.dp else if (compact) 100.dp else if (tall) 128.dp else 116.dp"))
        assertTrue(screen.contains("val mascotSize = if (veryCompact) 64.dp"))
        assertTrue(screen.contains("required.length > 1 -> .34f"))
        assertTrue(screen.contains("}).toSp()"))
        // The hint chip and notices sit under the countdown (no strip of their own above the keyboard).
        val strip = screen.indexOf("val hintVisible = myTurn")
        assertTrue(strip in 0 until screen.indexOf("PremierPressureStrip(\n                language = language,"))
        assertTrue(screen.contains("below = clockLine,"))
        assertFalse(screen.contains("Box(Modifier.fillMaxWidth().height(34.dp).padding(horizontal = 10.dp)"))
        assertTrue(screen.contains("val keyHeight = if (veryCompact) 33.dp else if (compact) 35.dp else if (tall) 42.dp else 39.dp"))
        assertTrue(screen.contains("PremierPressureStrip("))
        assertTrue(screen.contains("KRİTİK 5 SANİYE"))
        assertTrue(screen.contains("HAMLE SIRASI SENDE"))
        assertFalse(screen.contains("SALDIR"))
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
        assertTrue(screen.contains("AI ile serbestçe yazış."))
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
