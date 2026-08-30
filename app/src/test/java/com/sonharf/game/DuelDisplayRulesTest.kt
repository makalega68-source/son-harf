package com.sonharf.game

import org.junit.Assert.assertEquals
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
}
