package com.sonharf.game

import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EveCompanionRulesTest {
    @Test
    fun xpCurveUsesRequestedPowerFormula() {
        assertEquals(100, EveCompanionRules.xpTarget(1))
        assertEquals((100 * 2.0.pow(1.25)).toInt(), EveCompanionRules.xpTarget(2))
        assertEquals((100 * 10.0.pow(1.25)).toInt(), EveCompanionRules.xpTarget(10))
        assertTrue(EveCompanionRules.xpTarget(50) > EveCompanionRules.xpTarget(10))
    }

    @Test
    fun interactionQuotasAndRewardsArePinned() {
        assertEquals(5, EveCompanionRules.MAX_DAILY_FEED)
        assertEquals(10, EveCompanionRules.MAX_DAILY_PET)
        assertEquals(25, EveCompanionRules.FEED_XP)
        assertEquals(10, EveCompanionRules.PET_XP)
        assertEquals(500, EveCompanionRules.levelRewardGold(10))
        assertEquals(11, EveCompanionRules.levelRewardDiamonds(10))
    }

    @Test
    fun milestoneFeaturesRemainAvailable() {
        assertEquals("Kişisel Giydirme & Kostüm Odası", EveCompanionRules.featureUnlocks[10])
        assertEquals("Akıllı Soru İpucu Radarı (Ekstra %20 Netlik)", EveCompanionRules.featureUnlocks[20])
        assertEquals("Efsanevi Yoldaş Rozeti & Altın Çark", EveCompanionRules.featureUnlocks[50])
    }
}
