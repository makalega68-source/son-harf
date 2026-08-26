package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EveAssetPolicyTest {
    @Test
    fun realGlbPolicy_isLockedToAcceptedAsset() {
        assertEquals("models/eve/eve.glb", MascotPolicy.MODEL_ASSET)
        assertEquals(4_870_220L, MascotPolicy.MODEL_SIZE_BYTES)
        assertEquals(
            "0c68ac4c4f5475332fac77ccb9bda4bb08bd202a5d596114552e37ab27d6c39e",
            MascotPolicy.MODEL_SHA256,
        )
        assertEquals(38, MascotPolicy.JOINT_COUNT)
        assertEquals(16, MascotPolicy.ANIMATION_COUNT)
        assertTrue(MascotPolicy.SKELETAL_ASSET_READY)
        assertFalse(MascotPolicy.ALLOW_2D_OR_VIDEO_FALLBACK)
    }
}
