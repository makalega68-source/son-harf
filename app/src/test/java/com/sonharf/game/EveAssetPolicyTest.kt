package com.sonharf.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EveAssetPolicyTest {
    @Test
    fun eveIsRetainedForFutureButNotActive() {
        assertFalse(MascotPolicy.EVE_ACTIVE)
        assertTrue(EveAssetPolicy.MODEL_ASSET.contains("eve"))
        assertTrue(MascotPolicy.ENABLED)
    }
}
