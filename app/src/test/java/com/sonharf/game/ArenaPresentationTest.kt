package com.sonharf.game

import org.junit.Assert.*
import org.junit.Test

class ArenaPresentationTest {
    @Test fun impactAppearsAndFullyReleasesWithoutInvalidOpacity() {
        assertEquals(0f, arenaImpactAlpha(0f), .0001f)
        assertEquals(0f, arenaImpactAlpha(1f), .0001f)
        assertEquals(0f, arenaImpactAlpha(-1f), .0001f)
        assertEquals(0f, arenaImpactAlpha(2f), .0001f)
        assertEquals(1f, arenaImpactAlpha(.4f), .0001f)
        for (i in 0..1000) {
            val t = i / 1000f
            assertTrue(arenaImpactAlpha(t) in 0f..1f)
            assertTrue(kotlin.math.abs(arenaImpactAlpha(t) - arenaImpactAlpha(t + .001f)) < .01f)
        }
    }
}
