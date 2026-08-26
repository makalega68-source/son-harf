package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EveAssetPolicyTest {
    @Test
    fun realGlbPolicy_isLockedToAcceptedAsset() {
        assertEquals("models/eve/eve.glb", EveAssetPolicy.MODEL_ASSET)
        assertEquals(4_870_220L, EveAssetPolicy.MODEL_SIZE_BYTES)
        assertEquals("0c68ac4c4f5475332fac77ccb9bda4bb08bd202a5d596114552e37ab27d6c39e", EveAssetPolicy.MODEL_SHA256)
        assertTrue(EveAssetPolicy.SKELETAL_ASSET_READY)
        assertFalse(EveAssetPolicy.ALLOW_2D_OR_VIDEO_FALLBACK)
    }
}
