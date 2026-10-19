package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Test

class EveHintEntitlementTest {
    @Test fun eveGrantsThreeHintsWithoutAddingAnExtraAllowance() {
        assertEquals(3, MascotHints.freeHintsLeft(realOpponent = false, used = 0, ownsMascot = false, ownsEve = true))
        assertEquals(1, MascotHints.freeHintsLeft(realOpponent = false, used = 2, ownsMascot = false, ownsEve = true))
        assertEquals(0, MascotHints.freeHintsLeft(realOpponent = false, used = 3, ownsMascot = true, ownsEve = true))
        assertEquals(0, MascotHints.freeHintsLeft(realOpponent = false, used = 0, ownsMascot = false, ownsEve = false))
    }

    @Test fun realOpponentStillGetsTheSameThreeStrategyTips() {
        assertEquals(3, MascotHints.freeHintsLeft(realOpponent = true, used = 0, ownsMascot = false, ownsEve = false))
        assertEquals(3, MascotHints.freeHintsLeft(realOpponent = true, used = 0, ownsMascot = false, ownsEve = true))
    }
}
