package com.sonharf.game

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class MascotPolicyTest {
    @Test fun eveIsTheOnlyMascotContractAndFakeFallbackIsForbidden() {
        assertTrue(MascotPolicy.ENABLED)
        assertFalse(MascotPolicy.ALLOW_2D_OR_VIDEO_FALLBACK)
        assertEquals("models/eve/eve.glb", MascotPolicy.MODEL_ASSET)
        assertEquals(EveAssetPolicy.MODEL_ASSET, MascotPolicy.MODEL_ASSET)

        val oldCandidates = listOf(
            File("src/main/assets/models/son_harf_white_pet_rigged.glb"),
            File("app/src/main/assets/models/son_harf_white_pet_rigged.glb"),
        )
        assertTrue("Legacy white-pet GLB must be removed", oldCandidates.none { it.exists() })
    }

    @Test fun eveAnimationRegistryUsesPurchasedClipContract() {
        assertEquals(
            setOf(
                "IdleBreathe",
                "IdleLookAround",
                "IdleGraze",
                "Rest",
                "GoToRest",
                "RestToGoBackUp",
                "Walk",
                "Run",
                "GetHit",
                "Attack",
            ),
            EveAnimationCue.entries.map { it.clipName }.toSet(),
        )
    }

    @Test fun legacyMotionApiMapsOnlyToEveClipNames() {
        val allowed = setOf("IdleBreathe", "IdleLookAround", "Rest", "Walk", "WalkTurnL", "WalkTurnR", "Run")
        assertTrue(MascotAnimationRegistry.all.all { it.clipName in allowed })
        assertEquals(MascotMotion.entries.toSet(), MascotAnimationRegistry.all.map { it.motion }.toSet())
    }

    @Test fun finalEveGlbWhenBundledIsSkinnedAndContainsCoreAnimations() {
        val candidates = listOf(
            File("src/main/assets/${EveAssetPolicy.MODEL_ASSET}"),
            File("app/src/main/assets/${EveAssetPolicy.MODEL_ASSET}"),
        )
        val file = candidates.firstOrNull { it.isFile }
        assumeTrue("Final Eve GLB has not been bundled yet", file != null)

        val bytes = requireNotNull(file).readBytes()
        assertEquals("glTF", bytes.copyOfRange(0, 4).toString(Charsets.US_ASCII))
        val jsonLength = ByteBuffer.wrap(bytes, 12, 4).order(ByteOrder.LITTLE_ENDIAN).int
        val json = bytes.copyOfRange(20, 20 + jsonLength).toString(Charsets.UTF_8)
        assertTrue(json.contains("\"skins\":["))
        assertTrue(json.contains("\"joints\":["))
        assertTrue(json.contains("\"JOINTS_0\""))
        assertTrue(json.contains("\"WEIGHTS_0\""))
        assertTrue(json.contains("\"name\":\"IdleBreathe\""))
        assertTrue(json.contains("\"name\":\"IdleLookAround\""))
        assertTrue(json.contains("\"name\":\"Walk\""))
        assertTrue(json.contains("\"name\":\"Run\""))
        assertTrue(json.contains("\"name\":\"Rest\""))
    }
}
