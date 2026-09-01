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
    fun matchLossLocalFallbackIsShortAndNonEmpty() {
        val value = MiniAiMotivation.localMatchLoss("tr", seed = 1)
        assertTrue(value.isNotBlank())
        assertTrue(value.length <= 96)
    }

    @Test
    fun winAndLossTriggersAreWiredWithoutOtherTriggers() {
        val online = File("src/main/java/com/sonharf/game/OnlineGameScreenV6.kt").readText()
        assertTrue(online.contains("active.status == \"finished\" && active.winnerId == me"))
        assertTrue(online.contains("active.winnerId != null && active.winnerId != me"))
        assertTrue(online.contains("MiniAiMotivation.maybeAiMatchWin(active.id, active.language)"))
        assertTrue(online.contains("MiniAiMotivation.maybeAiMatchLoss(active.id, active.language)"))
        assertFalse(online.contains("PLAYER_RETURNED"))
        assertFalse(online.contains("MATCH_CONTINUE"))
    }

    @Test
    fun resultUiShowsMotivationForWinAndLossButNotDraw() {
        val ui = File("src/main/java/com/sonharf/game/LightDuelUi.kt").readText()
        assertTrue(ui.contains("motivationMessage = if (room.winnerId == null) null else motivationMessage"))
        assertTrue(ui.contains("if (!draw && !motivationMessage.isNullOrBlank())"))
    }
}
