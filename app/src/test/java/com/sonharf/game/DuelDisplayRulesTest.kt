package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DuelDisplayRulesTest {

    @Test
    fun turkishDottedAndDotlessIAreDisplayedDistinctly() {
        assertEquals("İ", gameUppercase("i", "tr"))
        assertEquals("I", gameUppercase("ı", "tr"))
        assertEquals("İZMİR", gameUppercase("izmir", "tr"))
        assertEquals("ISIRIK", gameUppercase("ısırık", "tr"))
    }

    @Test
    fun duelScoreShrinksBeforeItCanWrap() {
        assertEquals(28, duelScoreFontSize(39))
        assertEquals(21, duelScoreFontSize(288))
        assertEquals(17, duelScoreFontSize(1200))
        assertEquals(14, duelScoreFontSize(12000))
    }

    @Test
    fun duelLeaderIsStableAcrossTiesAndScoreChanges() {
        assertEquals(1, duelLeader(43, 39))
        assertEquals(-1, duelLeader(43, 56))
        assertEquals(0, duelLeader(43, 43))
    }

    @Test
    fun staleFailureEventCannotRejectANewlyAcceptedWord() {
        assertFalse(
            shouldTreatSubmissionAsFailure(
                previousValidWordCount = 7,
                resultValidWordCount = 8,
                eventCode = "ends_with_soft_g",
                eventPlayerId = "me",
                currentPlayerId = "me",
            )
        )
        assertTrue(
            shouldTreatSubmissionAsFailure(
                previousValidWordCount = 7,
                resultValidWordCount = 7,
                eventCode = "ends_with_soft_g",
                eventPlayerId = "me",
                currentPlayerId = "me",
            )
        )
        assertFalse(
            shouldTreatSubmissionAsFailure(7, 7, "ends_with_soft_g", "rival", "me")
        )
    }

    @Test
    fun softGReasonRequiresExactOwnEventAndActualSubmittedEnding() {
        assertTrue(
            shouldShowSoftGReason(
                eventCode = "ends_with_soft_g",
                eventPlayerId = "me",
                currentPlayerId = "me",
                submittedWord = "DAĞ",
                language = "tr",
            )
        )
        assertFalse(shouldShowSoftGReason("ends_with_soft_g", "rival", "me", "DAĞ", "tr"))
        assertFalse(shouldShowSoftGReason("ends_with_soft_g", "me", "me", "İNMEK", "tr"))
        assertFalse(shouldShowSoftGReason("invalid_word", "me", "me", "DAĞ", "tr"))
        assertFalse(shouldShowSoftGReason(null, "me", "me", "DAĞ", "tr"))
    }

    @Test
    fun softGFailureTextCannotLeakIntoUnrelatedRpcErrors() {
        assertTrue(shouldShowSoftGReasonFromFailure("PostgREST: ends_with_soft_g", "sığ", "tr"))
        assertFalse(shouldShowSoftGReasonFromFailure("PostgREST: invalid_word", "sığ", "tr"))
        assertFalse(shouldShowSoftGReasonFromFailure("PostgREST: ends_with_soft_g", "masa", "tr"))
        assertFalse(shouldShowSoftGReasonFromFailure(null, "dağ", "tr"))
    }
}
