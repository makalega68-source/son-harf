package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MageCatPlacementTest {
    @Test
    fun homeMascotStaysBelowPlayCtaProminence() {
        val placement = MageCatPlacementPolicy.forScreen(MageCatScreen.HOME)
        assertEquals(MageCatAnchor.BOTTOM_END, placement.anchor)
        assertTrue(placement.heightFraction in 0.18f..0.22f)
    }

    @Test
    fun matchMascotRemainsCompactUntilReaction() {
        val placement = MageCatPlacementPolicy.forScreen(MageCatScreen.MATCH)
        assertTrue(placement.heightFraction <= 0.13f)
        assertTrue(placement.reactionHeightFraction <= 0.17f)
    }

    @Test
    fun resultMascotCanTakeHeroProminence() {
        val placement = MageCatPlacementPolicy.forScreen(MageCatScreen.RESULT)
        assertEquals(MageCatAnchor.RESULT_SIDE, placement.anchor)
        assertTrue(placement.reactionHeightFraction in 0.22f..0.28f)
    }
}
