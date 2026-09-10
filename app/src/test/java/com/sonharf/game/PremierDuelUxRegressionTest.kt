package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Locks the real-device fixes requested for the rebuilt Premier 1v1 arena.
class PremierDuelUxRegressionTest {
    @Test fun premierArenaKeepsLargeProfilesVisibleChatSymmetryAndTimeoutRecovery() {
        val screen = File("src/main/java/com/sonharf/game/PremierWordDuelScreen.kt").readText()
        val backend = File("src/main/java/com/sonharf/game/data/PremierDuelBackend.kt").readText()
        val onlineBackend = File("src/main/java/com/sonharf/game/data/OnlineGameBackend.kt").readText()

        assertTrue(screen.contains("ProfilePhotoAvatarWithGender(avatar, gender, name, 58.dp"))
        assertTrue(screen.contains("PremierBotAvatar(size = 58.dp"))
        assertTrue(screen.contains("PremierBotAvatar(size = 70.dp"))
        assertFalse(screen.contains("MageCatCompanion("))
        assertFalse(screen.contains("SyntheticBotPortrait("))
        assertTrue(screen.contains("Text(pt(language, \"SOHBET\", \"CHAT\")"))
        assertFalse(screen.contains("enabled = !room.isBot"))
        assertTrue(screen.contains("Alignment.CenterStart"))
        assertTrue(screen.contains("Alignment.CenterEnd"))
        assertTrue(screen.contains("premierRemainingTurnSeconds(deadline)"))
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

        // Small-screen gameplay must keep the target instruction visible above the keyboard.
        assertTrue(screen.contains("val veryCompact = maxHeight < 610.dp"))
        assertTrue(screen.contains("if (!veryCompact)"))
        assertTrue(screen.contains("“$required” ile başlayan bir kelime yaz"))

        // A completed authoritative submit clears the attempt and gives explicit correct/wrong feedback.
        assertTrue(screen.contains("input = \"\""))
        assertTrue(screen.contains("val accepted = next.validWordCount > active.validWordCount"))
        assertTrue(screen.contains("pt(language, \"DOĞRU\", \"CORRECT\")"))
        assertTrue(screen.contains("pt(language, \"YANLIŞ\", \"WRONG\")"))
        assertTrue(screen.contains("PremierMoveFeedback("))

        // The input bar no longer carries the redundant server badge.
        assertFalse(screen.contains("PremierStatPill(pt(language, \"SUNUCU\", \"SERVER\")"))

        // Chat keeps quick reactions but human matches also expose a real typed message field/history.
        assertTrue(screen.contains("private fun PremierChatSheet("))
        assertTrue(screen.contains("messages = chat"))
        assertTrue(screen.contains("OutlinedTextField("))
        assertTrue(screen.contains("\"Mesaj yaz…\""))
        assertTrue(screen.contains("backend.sendChat(active.id, message)"))
        assertTrue(screen.contains("backend.getChat(active.id)"))
        assertTrue(screen.contains("quickMessages.forEach"))
        assertTrue(screen.contains("Bot maçında gerçek mesajlaşma kapalıdır."))
    }
}
