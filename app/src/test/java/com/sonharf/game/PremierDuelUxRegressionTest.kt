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

        assertTrue(screen.contains("ProfilePhotoAvatarWithGender(avatar, gender, name, 58.dp"))
        assertTrue(screen.contains("SyntheticBotPortrait(name, width = 58.dp, height = 58.dp"))
        assertTrue(screen.contains("Text(pt(language, \"SOHBET\", \"CHAT\")"))
        assertFalse(screen.contains("enabled = !room.isBot"))
        assertTrue(screen.contains("Alignment.CenterStart"))
        assertTrue(screen.contains("Alignment.CenterEnd"))
        assertTrue(screen.contains("backend.claimTurnTimeout(active.id)"))
        assertTrue(screen.contains("backend.botTakeTurn(active.id)"))
        assertTrue(screen.contains("backend.submitPremierWord(active.id, candidate)"))
        assertFalse(screen.contains("timeoutClaimKey"))
        assertTrue(backend.contains("submitWord(roomId, word)"))
        assertTrue(backend.contains("getRoom(roomId)"))
        assertTrue(backend.contains("botTakeTurn(roomId)"))
        assertFalse(backend.contains("submit_word_v4"))
    }
}
