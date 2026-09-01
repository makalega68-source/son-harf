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
    fun playerReturnFallbackIsShortAndNonEmpty() {
        val value = MiniAiMotivation.localPlayerReturned("tr", seed = 2)
        assertTrue(value.isNotBlank())
        assertTrue(value.length <= 96)
    }

    @Test
    fun matchContinueFallbackIsShortAndNonEmpty() {
        val value = MiniAiMotivation.localMatchContinue("tr", seed = 2)
        assertTrue(value.isNotBlank())
        assertTrue(value.length <= 96)
    }

    @Test
    fun secondaryAiGatesAreRarerThanWinLossGate() {
        val primary = (1..800).count { MiniAiMotivation.shouldAttemptAi("primary-$it") }
        val secondary = (1..800).count { MiniAiMotivation.shouldAttemptAi("secondary-$it", divisor = 40) }
        assertTrue(primary > 0)
        assertTrue(secondary > 0)
        assertTrue(secondary < primary)
        assertTrue(secondary < 40)
    }

    @Test
    fun allFourMiniAiEventsAreWiredAndReturnNeedsSixHours() {
        val online = File("src/main/java/com/sonharf/game/OnlineGameScreenV6.kt").readText()
        val helper = File("src/main/java/com/sonharf/game/MiniAiMotivation.kt").readText()
        assertTrue(online.contains("MiniAiMotivation.maybeAiMatchWin(active.id, active.language)"))
        assertTrue(online.contains("MiniAiMotivation.maybeAiMatchLoss(active.id, active.language)"))
        assertTrue(online.contains("MiniAiMotivation.maybeAiPlayerReturned"))
        assertTrue(online.contains("MiniAiMotivation.maybeAiMatchContinue(active.id, active.language)"))
        assertTrue(online.contains("returnedAfterAbsence"))
        assertTrue(online.contains("6 * 60 * 60 * 1000L"))
        assertTrue(helper.contains("PLAYER_RETURNED:"))
        assertTrue(helper.contains("MATCH_CONTINUE:"))
    }

    @Test
    fun resultUiShowsContinueMessageForEveryFinishedResult() {
        val ui = File("src/main/java/com/sonharf/game/LightDuelUi.kt").readText()
        assertTrue(ui.contains("continueMessage = continueMessage"))
        assertTrue(ui.contains("if (!continueMessage.isNullOrBlank())"))
        assertTrue(ui.contains("motivationMessage = if (room.winnerId == null) null else motivationMessage"))
    }
}
