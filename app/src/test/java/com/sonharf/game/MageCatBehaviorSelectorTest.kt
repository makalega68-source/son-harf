package com.sonharf.game

import kotlin.random.Random
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MageCatBehaviorSelectorTest {
    @Test
    fun sameEventDoesNotRepeatSameMotionBackToBack() {
        val selector = MageCatBehaviorSelector(random = Random(7), historySize = 3)
        var previous = selector.choose(MageCatEvent.CORRECT_WORD)

        repeat(20) {
            val current = selector.choose(MageCatEvent.CORRECT_WORD)
            assertNotEquals(previous, current)
            previous = current
        }
    }

    @Test
    fun defeatAlternatesAvailableSadReactions() {
        val selector = MageCatBehaviorSelector(random = Random(11), historySize = 3)
        val first = selector.choose(MageCatEvent.DEFEAT)
        val second = selector.choose(MageCatEvent.DEFEAT)

        assertNotEquals(first, second)
    }
}
