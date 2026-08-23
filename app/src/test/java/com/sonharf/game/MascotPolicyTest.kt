package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MascotPolicyTest {
    @Test fun mascotFeatureIsEnabledButFakeFallbackIsForbidden() {
        assertTrue(MascotPolicy.ENABLED)
        assertFalse(MascotPolicy.ALLOW_2D_OR_VIDEO_FALLBACK)
        assertFalse(MascotPolicy.SKELETAL_ASSET_READY)
        assertTrue(MascotPolicy.MODEL_ASSET.endsWith(".glb"))
    }

    @Test fun requiredStateMachineMotionsExist() {
        val expected = setOf(
            MascotMotion.IDLE,
            MascotMotion.WALK,
            MascotMotion.TURN_LEFT,
            MascotMotion.TURN_RIGHT,
            MascotMotion.LOOK_AT_PLAYER,
            MascotMotion.GREETING,
            MascotMotion.THINKING,
            MascotMotion.CRITICAL,
            MascotMotion.VICTORY,
            MascotMotion.DEFEAT,
            MascotMotion.SIT,
            MascotMotion.RUN,
        )
        assertEquals(expected, MascotAnimationRegistry.all.map { it.motion }.toSet())
    }

    @Test fun advancedAnimationsUnlockOnTenLevelSteps() {
        val advanced = MascotAnimationRegistry.all.filter { it.unlockLevel > 1 }
        assertTrue(advanced.isNotEmpty())
        assertTrue(advanced.all { it.unlockLevel % 10 == 0 })
        assertTrue(MascotAnimationRegistry.unlocked(9).none { it.motion == MascotMotion.SIT })
        assertTrue(MascotAnimationRegistry.unlocked(10).any { it.motion == MascotMotion.SIT })
        assertTrue(MascotAnimationRegistry.unlocked(20).any { it.motion == MascotMotion.RUN })
    }
}
