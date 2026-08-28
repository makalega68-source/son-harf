package com.sonharf.game

import com.sonharf.game.mascotdata3.ChibiEmbeddedModel
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MascotPolicyTest {
    @Test
    fun whiteMascotIsDefaultAndEveIsParked() {
        assertTrue(MascotPolicy.ENABLED)
        assertFalse(MascotPolicy.ALLOW_2D_OR_VIDEO_FALLBACK)
        assertFalse(MascotPolicy.EVE_ACTIVE)
        assertEquals(MascotCatalog.DEFAULT_ID, MascotPolicy.DEFAULT_MASCOT_ID)
        assertTrue(MascotCatalog.item(MascotCatalog.DEFAULT_ID).standard)
        assertTrue(MascotCatalog.item(MascotCatalog.DEFAULT_ID).licensedForCommercialGame)
    }

    @Test
    fun verifiedWhiteLyraUsesExactLicensedChibiAsset() {
        val bytes = ChibiEmbeddedModel.decodeGlb()
        assertEquals(MascotPolicy.WHITE_MASCOT_SIZE_BYTES.toLong(), bytes.size.toLong())
        assertEquals(MascotPolicy.WHITE_MASCOT_SHA256, sha256(bytes))
        assertEquals(ChibiEmbeddedModel.EXPECTED_BYTES, bytes.size.toLong())
        assertEquals(ChibiEmbeddedModel.EXPECTED_SHA256, sha256(bytes))
        assertEquals("embedded:chibi-wizard-v1", MascotPolicy.WHITE_MASCOT_ASSET)
        assertGlb2(bytes)

        val json = glbJson(bytes)
        assertTrue(json.contains("\"skins\""))
        assertTrue(json.contains("\"animations\""))
        ChibiEmbeddedModel.ANIMATION_NAMES.forEach { name ->
            assertTrue("Missing Lyra Chibi animation: $name", json.contains("\"name\":\"$name\""))
        }
    }

    @Test
    fun animatedChibiDecodesWithExactIntegrityAndNineRuntimeAnimations() {
        val bytes = ChibiEmbeddedModel.decodeGlb()
        assertEquals(ChibiEmbeddedModel.EXPECTED_BYTES, bytes.size.toLong())
        assertEquals(ChibiEmbeddedModel.EXPECTED_SHA256, sha256(bytes))
        assertGlb2(bytes)

        val json = glbJson(bytes)
        ChibiEmbeddedModel.ANIMATION_NAMES.forEach { name ->
            assertTrue("Missing Chibi animation: $name", json.contains("\"name\":\"$name\""))
        }
        assertTrue(json.contains("\"skins\""))
        assertTrue(json.contains("\"animations\""))
    }

    @Test
    fun onlyCommerciallyApprovedCatalogEntriesCanBeActivated() {
        assertTrue(MascotCatalog.all.all { it.licensedForCommercialGame })
        assertTrue(MascotPolicy.CHIBI_WIZARD_LICENSE_APPROVED)
        assertTrue(MascotPolicy.CHIBI_WIZARD_ASSET_READY)
    }

    @Test
    fun genericMotionRegistryCoversEveryMotion() {
        assertEquals(MascotMotion.entries.toSet(), MascotAnimationRegistry.all.map { it.motion }.toSet())
    }

    @Test
    fun oneShotMotionsCannotBeInterruptedByIdle() {
        listOf(
            MascotMotion.GREETING,
            MascotMotion.CRITICAL,
            MascotMotion.VICTORY,
            MascotMotion.DEFEAT,
        ).forEach { motion ->
            assertTrue(MascotMotionPolicy.isOneShot(motion))
            assertFalse(MascotMotionPolicy.loops(motion))
            assertTrue((MascotMotionPolicy.durationMs(motion) ?: 0L) > 0L)
            assertFalse(MascotMotionPolicy.canInterrupt(motion, MascotMotion.IDLE))
        }
    }

    @Test
    fun matchResultOverridesLowerPriorityMascotReactions() {
        assertTrue(MascotMotionPolicy.canInterrupt(MascotMotion.CRITICAL, MascotMotion.VICTORY))
        assertTrue(MascotMotionPolicy.canInterrupt(MascotMotion.GREETING, MascotMotion.DEFEAT))
        assertFalse(MascotMotionPolicy.canInterrupt(MascotMotion.VICTORY, MascotMotion.CRITICAL))
        assertTrue(MascotMotionPolicy.priority(MascotMotion.VICTORY) > MascotMotionPolicy.priority(MascotMotion.CRITICAL))
    }

    @Test
    fun runtimeHonorsOneShotProtectionAndForceReset() {
        MascotRuntime.react(MascotMotion.IDLE, force = true)
        MascotRuntime.react(MascotMotion.GREETING, force = true)
        MascotRuntime.react(MascotMotion.IDLE)
        assertEquals(MascotMotion.GREETING, MascotRuntime.motion)

        MascotRuntime.react(MascotMotion.VICTORY)
        assertEquals(MascotMotion.VICTORY, MascotRuntime.motion)

        MascotRuntime.react(MascotMotion.IDLE, force = true)
        assertEquals(MascotMotion.IDLE, MascotRuntime.motion)
    }

    private fun assertGlb2(bytes: ByteArray) {
        assertTrue(bytes.size >= 20)
        assertEquals("glTF", bytes.copyOfRange(0, 4).toString(Charsets.US_ASCII))
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(2, bb.getInt(4))
        assertEquals(bytes.size, bb.getInt(8))
    }

    private fun glbJson(bytes: ByteArray): String {
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val jsonLength = bb.getInt(12)
        val jsonType = bb.getInt(16)
        assertEquals(0x4E4F534A, jsonType)
        return bytes.copyOfRange(20, 20 + jsonLength).toString(Charsets.UTF_8).trimEnd(' ', '\u0000')
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
