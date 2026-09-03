package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Test

class WordSiegeMoveFeedbackTest {
    @Test fun `pending move is visibly ready`() {
        assertEquals(WordSiegeValidationState.READY, wordSiegeValidationFeedback(2, turkish = true).state)
    }

    @Test fun `accepted word uses positive feedback`() {
        val feedback = wordSiegeValidationFeedback(0, acceptedWord = "KALE", turkish = true)
        assertEquals(WordSiegeValidationState.ACCEPTED, feedback.state)
        assertEquals("✓ KALE kabul edildi", feedback.message)
    }

    @Test fun `invalid word uses negative feedback`() {
        val feedback = wordSiegeValidationFeedback(2, error = "Kelime sözlükte yok", turkish = true)
        assertEquals(WordSiegeValidationState.REJECTED, feedback.state)
        assertEquals("✕ Kelime sözlükte yok", feedback.message)
    }
}
