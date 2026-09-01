package com.sonharf.game

import org.junit.Test
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class SonHarfTurnResolutionContractTest {
    private fun source(path: String): String = java.io.File(path).readText()

    @Test
    fun submitIsLockedBeforeAsyncValidationAndBotWaitsForFinalization() {
        val online = source("src/main/java/com/sonharf/game/OnlineGameScreenV6.kt")
        assertTrue(online.contains("if (submitted.isNotBlank() && !isResolvingTurn && !busy)"))
        assertTrue(online.contains("isResolvingTurn = true"))
        assertTrue(online.contains("LaunchedEffect(active.id, active.status, active.botTurn, active.validWordCount, active.roundNo, isResolvingTurn)"))
        assertTrue(online.contains("if (!isResolvingTurn && active.isBot && active.botTurn"))
        assertTrue(online.contains("if (isResolvingTurn) pendingRoomAfterResolution = it else room = it"))
        val submitStart = online.indexOf("val result = backend.submitWord(active.id, submitted)")
        val classify = online.indexOf("val rejected = failedEvent(result.lastEvent)", submitStart)
        val publishRoom = online.indexOf("room = finalizedRoom", classify)
        val unlock = online.indexOf("isResolvingTurn = false", publishRoom)
        assertTrue(submitStart >= 0 && classify > submitStart && publishRoom > classify && unlock > publishRoom)
    }

    @Test
    fun everySubmitPathProducesVisibleResolutionAndStructuredDevelopmentLog() {
        val online = source("src/main/java/com/sonharf/game/OnlineGameScreenV6.kt")
        assertTrue(online.contains("validation_result="))
        assertTrue(online.contains("ACCEPTED"))
        assertTrue(online.contains("REJECTED"))
        assertTrue(online.contains("validation_result=REJECTED"))
        listOf("submitted_word=", "normalized_word=", "validation_started=", "validation_result=", "score_delta=", "turn_before=", "turn_after=", "bot_triggered=", "error_code=").forEach {
            assertTrue("missing log field $it", online.contains(it))
        }
    }

    @Test
    fun scoreFeedbackUsesRealRoomDeltaAndWordRenderIsResponsive() {
        val online = source("src/main/java/com/sonharf/game/OnlineGameScreenV6.kt")
        val ui = source("src/main/java/com/sonharf/game/LightDuelUi.kt")
        assertTrue(online.contains("val scoreDelta = playerScore(result, me) - scoreBefore"))
        assertTrue(online.contains("feedbackScoreDelta = scoreDelta.takeIf { it != 0 }"))
        assertTrue(ui.contains("BoxWithConstraints(Modifier.fillMaxWidth()"))
        assertTrue(ui.contains("overflow = TextOverflow.Ellipsis"))
        assertTrue(ui.contains("feedbackScoreDelta > 0"))
        assertTrue(ui.contains("PUAN"))
        assertFalse(ui.contains("shownLastWord.ifBlank { sh(\"İLK KELİMEYİ YAZ\", \"ENTER FIRST WORD\") },\n                        color = shownLastWordColor,\n                        fontSize = 14.sp"))
    }

    @Test
    fun botPracticeAndExistingRatingProgressStayExplicit() {
        val ui = source("src/main/java/com/sonharf/game/LightDuelUi.kt")
        assertTrue(ui.contains("ANTRENMAN • RATING ETKİLENMEZ"))
        assertTrue(ui.contains("ratingLeagueProgress(playerRating)"))
        assertTrue(ui.contains("val league = ratingLeagueProgress(rating)"))
        assertTrue(ui.contains("seconds in 1..10"))
    }
}
