package com.sonharf.game

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MascotPolicyTest {
    @Test fun verifiedMascotIsEnabledAndFakeFallbackIsForbidden() {
        assertTrue(MascotPolicy.ENABLED)
        assertFalse(MascotPolicy.ALLOW_2D_OR_VIDEO_FALLBACK)
        assertTrue(MascotPolicy.SKELETAL_ASSET_READY)
        assertTrue(MascotPolicy.MODEL_ASSET.endsWith(".glb"))
        assertEquals(781848, MascotPolicy.MODEL_SIZE_BYTES)
        assertEquals(64, MascotPolicy.MODEL_SHA256.length)
    }

    @Test fun bundledGlbIsExactSkinnedAssetWithExpectedClips() {
        val candidates = listOf(
            File("src/main/assets/${MascotPolicy.MODEL_ASSET}"),
            File("app/src/main/assets/${MascotPolicy.MODEL_ASSET}"),
        )
        val file = candidates.firstOrNull { it.isFile }
        assertTrue("Bundled mascot GLB is missing", file != null)
        val bytes = requireNotNull(file).readBytes()
        assertEquals(MascotPolicy.MODEL_SIZE_BYTES, bytes.size)
        assertEquals("glTF", bytes.copyOfRange(0, 4).toString(Charsets.US_ASCII))

        val sha = MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
        assertEquals(MascotPolicy.MODEL_SHA256, sha)

        val jsonLength = ByteBuffer.wrap(bytes, 12, 4).order(ByteOrder.LITTLE_ENDIAN).int
        val json = bytes.copyOfRange(20, 20 + jsonLength).toString(Charsets.UTF_8)
        assertTrue(json.contains("\"skins\":["))
        assertTrue(json.contains("\"joints\":["))
        assertTrue(json.contains("\"JOINTS_0\""))
        assertTrue(json.contains("\"WEIGHTS_0\""))
        MascotAnimationRegistry.all.forEach { definition ->
            assertTrue("Missing GLB clip ${definition.clipName}", json.contains("\"name\":\"${definition.clipName}\""))
        }
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

    @Test fun clipNamesMatchVerifiedGlbContract() {
        assertEquals(
            setOf("Idle", "Walk", "Turn_Left", "Turn_Right", "Look_At_Player", "Greeting", "Thinking", "Critical", "Victory", "Defeat", "Sit", "Run"),
            MascotAnimationRegistry.all.map { it.clipName }.toSet(),
        )
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
