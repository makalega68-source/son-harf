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
    fun singleDarkChibiMascotIsTheOnlyCatalogEntry() {
        assertTrue(MascotPolicy.ENABLED)
        assertFalse(MascotPolicy.ALLOW_2D_OR_VIDEO_FALLBACK)
        assertEquals(MascotCatalog.CHIBI_WIZARD_ID, MascotPolicy.DEFAULT_MASCOT_ID)
        assertEquals(1, MascotCatalog.all.size)
        assertEquals(MascotCatalog.CHIBI_WIZARD_ID, MascotCatalog.all.single().id)
        assertTrue(MascotCatalog.all.single().standard)
        assertTrue(MascotCatalog.all.single().licensedForCommercialGame)
    }

    @Test
    fun darkChibiDecodesWithExactIntegrityAndRuntimeAnimations() {
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
    fun whiteMascotIdentifiersAreGoneFromCatalogAndPolicy() {
        val catalog = MascotCatalog.all.joinToString(" ") { it.id + it.nameTr + it.nameEn }
        assertFalse(catalog.contains("white", ignoreCase = true))
        assertFalse(catalog.contains("beyaz", ignoreCase = true))
        assertFalse(catalog.contains("lyra", ignoreCase = true))
    }

    @Test
    fun genericMotionRegistryCoversEveryMotion() {
        assertEquals(MascotMotion.entries.toSet(), MascotAnimationRegistry.all.map { it.motion }.toSet())
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
