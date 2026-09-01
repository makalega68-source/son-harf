package com.sonharf.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MiniAiMotivationTest {
    @Test
    fun matchWinLocalFallbackIsShortAndNonEmpty() {
        val value = MiniAiMotivation.localMatchWin("tr", seed = 1)
        assertTrue(value.isNotBlank())
        assertTrue(value.length <= 96)
    }

    @Test
    fun consecutiveLocalMessagesDoNotRepeat() {
        val first = MiniAiMotivation.localMatchWin("tr", seed = 0)
        val second = MiniAiMotivation.localMatchWin("tr", seed = 0)
        assertNotEquals(first, second)
    }

    @Test
    fun aiGateIsRareNotContinuous() {
        val attempts = (1..400).count { MiniAiMotivation.shouldAttemptAi("match-$it") }
        assertTrue(attempts > 0)
        assertTrue(attempts < 50)
    }

    @Test
    fun aiReplyIsReducedToOneShortSentence() {
        val value = MiniAiMotivation.sanitizeAiReply("Harika oynadın! Bir tane daha uzun cümle.")
        assertNotNull(value)
        assertTrue(value!!.endsWith("!"))
        assertFalse(value.contains("Bir tane daha"))
        assertTrue(value.length <= 96)
    }

    @Test
    fun onlyMatchWinTriggerIsWiredInFirstStage() {
        val online = File("src/main/java/com/sonharf/game/OnlineGameScreenV6.kt").readText()
        assertTrue(online.contains("active.status == \"finished\" && active.winnerId == me"))
        assertTrue(online.contains("MiniAiMotivation.localMatchWin()"))
        assertTrue(online.contains("MiniAiMotivation.maybeAiMatchWin(active.id, active.language)"))
        assertFalse(online.contains("MATCH_LOSS"))
        assertFalse(online.contains("PLAYER_RETURNED"))
    }

    @Test
    fun resultUiShowsMotivationOnlyForWinner() {
        val ui = File("src/main/java/com/sonharf/game/LightDuelUi.kt").readText()
        assertTrue(ui.contains("motivationMessage = if (room.winnerId == me) motivationMessage else null"))
        assertTrue(ui.contains("if (won && !motivationMessage.isNullOrBlank())"))
    }
}
