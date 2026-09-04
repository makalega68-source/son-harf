package com.sonharf.game

import org.junit.Assert.*
import org.junit.Test

class ClassicCompetitionRulesTest {
    @Test fun requiredLetterValidationIsImmediateAndTextSafe() {
        assertTrue(ClassicCompetitionRules.inputStartsWithRequired("Kedi", "K", "tr") == true)
        assertTrue(ClassicCompetitionRules.inputStartsWithRequired("masa", "K", "tr") == false)
        assertNull(ClassicCompetitionRules.inputStartsWithRequired("", "K", "tr"))
        assertNull(ClassicCompetitionRules.inputStartsWithRequired("masa", "•", "tr"))
    }

    @Test fun urgentModeStartsExactlyAtTenAndHapticAtFive() {
        assertFalse(ClassicCompetitionRules.isUrgent(11))
        assertTrue(ClassicCompetitionRules.isUrgent(10))
        assertFalse(ClassicCompetitionRules.shouldHaptic(6))
        assertTrue(ClassicCompetitionRules.shouldHaptic(5))
        assertTrue(ClassicCompetitionRules.timerCadenceMs(3) < ClassicCompetitionRules.timerCadenceMs(10))
    }

    @Test fun scoreDifferenceLabelsAreBilingual() {
        assertEquals("+18 ÖNDESİN", ClassicCompetitionRules.scoreDifferenceText(40, 22, "tr"))
        assertEquals("7 PUAN GERİDESİN", ClassicCompetitionRules.scoreDifferenceText(20, 27, "tr"))
        assertEquals("BERABERE", ClassicCompetitionRules.scoreDifferenceText(20, 20, "tr"))
        assertEquals("+18 AHEAD", ClassicCompetitionRules.scoreDifferenceText(40, 22, "en"))
        assertEquals("7 POINTS BEHIND", ClassicCompetitionRules.scoreDifferenceText(20, 27, "en"))
        assertEquals("TIED", ClassicCompetitionRules.scoreDifferenceText(20, 20, "en"))
    }

    @Test fun leaderChangeOnlyExistsOnActualTransition() {
        assertNull(ClassicCompetitionRules.leadChangeText(1, 1, "tr"))
        assertEquals("ÖNE GEÇTİN", ClassicCompetitionRules.leadChangeText(-1, 1, "tr"))
        assertEquals("RAKİP ÖNE GEÇTİ", ClassicCompetitionRules.leadChangeText(1, -1, "tr"))
        assertEquals("SKORLAR EŞİT", ClassicCompetitionRules.leadChangeText(1, 0, "tr"))
    }

    @Test fun comboLongAndStrongThresholdsAreCentralized() {
        assertFalse(ClassicCompetitionRules.isLongWord("altı"))
        assertTrue(ClassicCompetitionRules.isLongWord("yedigün"))
        assertFalse(ClassicCompetitionRules.isStrongScoreDelta(19))
        assertTrue(ClassicCompetitionRules.isStrongScoreDelta(20))
        assertNull(ClassicCompetitionRules.comboLabel(2, "tr"))
        assertEquals("3 KELİME COMBO", ClassicCompetitionRules.comboLabel(3, "tr"))
        assertEquals("5 KELİME SERİ", ClassicCompetitionRules.comboLabel(5, "tr"))
    }

    @Test fun criticalModeUsesTenPointGapOrFinalMoves() {
        assertTrue(ClassicCompetitionRules.isCritical(40, 30, 0))
        assertFalse(ClassicCompetitionRules.isCritical(41, 30, 0))
        assertTrue(ClassicCompetitionRules.isCritical(60, 20, 3))
    }
}