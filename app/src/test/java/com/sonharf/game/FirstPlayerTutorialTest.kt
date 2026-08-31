package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstPlayerTutorialTest {
    @Test
    fun sonHarfTutorialContainsRequiredChainAndFinish() {
        val steps = tutorialSteps(FirstPlayerTutorialKind.SON_HARF)

        assertEquals(4, steps.size)
        assertEquals(TutorialVisual.KALEM, steps[0].visual)
        assertTrue(steps[0].bodyTr.contains("son harfiyle"))
        assertEquals(TutorialVisual.MASA, steps[1].visual)
        assertTrue(steps[1].bodyTr.contains("A harfiyle"))
        assertEquals(TutorialVisual.TIMER, steps[2].visual)
        assertTrue(steps[2].bodyTr.contains("10 saniye"))
        assertTrue(steps[2].bodyTr.contains("−1"))
        assertEquals("Kelimeyi Sürdür, Rakibini Geç", steps.last().bodyTr)
    }

    @Test
    fun wordSiegeTutorialContainsAllRequestedValidationExamples() {
        val steps = tutorialSteps(FirstPlayerTutorialKind.WORD_SIEGE)
        val visuals = steps.map { it.visual }

        assertEquals(7, steps.size)
        assertTrue(TutorialVisual.RACK_TO_BOARD in visuals)
        assertTrue(TutorialVisual.TAM in visuals)
        assertTrue(TutorialVisual.INVALID_EXTENSION in visuals)
        assertTrue(TutorialVisual.VALID_EXTENSION in visuals)
        assertTrue(TutorialVisual.CROSS_WORDS in visuals)
        assertTrue(TutorialVisual.CAPTURE in visuals)
        assertEquals("Hazırsın. Kuşatmayı başlat!", steps.last().bodyTr)
    }

    @Test
    fun wordSiegeTutorialNeverRestoresManualDirectionSelection() {
        val copy = tutorialSteps(FirstPlayerTutorialKind.WORD_SIEGE)
            .flatMap { listOf(it.titleTr, it.bodyTr, it.titleEn, it.bodyEn) }
            .joinToString(" ")
            .uppercase()

        assertFalse(copy.contains("YATAY"))
        assertFalse(copy.contains("DİKEY"))
        assertFalse(copy.contains("HORIZONTAL"))
        assertFalse(copy.contains("VERTICAL"))
    }

    @Test
    fun completedTutorialDoesNotAutoShowAgain() {
        assertTrue(shouldAutoShowTutorial(completed = false))
        assertFalse(shouldAutoShowTutorial(completed = true))
    }
}
