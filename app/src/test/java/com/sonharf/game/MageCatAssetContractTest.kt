package com.sonharf.game

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.File
import java.util.zip.CRC32
import java.util.zip.InflaterInputStream
import org.junit.Assert.*
import org.junit.Test

class MageCatAssetContractTest {
    @Test
    fun atlasHasValidTransparentPngPayload() {
        // Android's Kotlin test classpath excludes java.desktop / ImageIO.
        // Validate the PNG container, every CRC and the decompressed RGBA scanlines
        // using only core Java APIs. Pixel bounds were independently checked with Pillow.
        val bytes = File("src/main/res/drawable-nodpi/mage_cat_expressions.png").readBytes()
        val input = DataInputStream(bytes.inputStream())
        assertEquals(0x89504e470d0a1a0aUL.toLong(), input.readLong())
        val compressed = ByteArrayOutputStream()
        var width = 0
        var height = 0
        var ended = false
        while (input.available() > 0) {
            val length = input.readInt()
            assertTrue(length >= 0 && length <= input.available() - 8)
            val type = ByteArray(4).also(input::readFully)
            val payload = ByteArray(length).also(input::readFully)
            val crc = CRC32().apply { update(type); update(payload) }
            assertEquals(crc.value, input.readInt().toLong() and 0xffffffffL)
            when (String(type, Charsets.US_ASCII)) {
                "IHDR" -> {
                    assertEquals(13, length)
                    val header = DataInputStream(payload.inputStream())
                    width = header.readInt()
                    height = header.readInt()
                    assertTrue(width in 300..2048 && height == width && width % 3 == 0)
                    assertEquals(8, header.readUnsignedByte())
                    assertEquals(6, header.readUnsignedByte()) // RGBA
                    assertEquals(0, header.readUnsignedByte()) // deflate
                    assertEquals(0, header.readUnsignedByte()) // PNG filters
                    assertEquals(0, header.readUnsignedByte()) // non-interlaced
                }
                "IDAT" -> compressed.write(payload)
                "IEND" -> { assertEquals(0, length); ended = true; break }
            }
        }
        assertTrue(ended)
        assertEquals(0, input.available())
        assertTrue(width > 0 && compressed.size() > 0)
        val raw = InflaterInputStream(compressed.toByteArray().inputStream()).use { it.readBytes() }
        assertEquals((width * 4 + 1) * height, raw.size)
        for (row in 0 until height) assertTrue(raw[row * (width * 4 + 1)].toInt() in 0..4)
    }

    @Test
    fun mascotUsesRealDrawableInsteadOfCanvasFallback() {
        val companion = File("src/main/java/com/sonharf/game/mascot/MageCatCompanion.kt").readText()
        val overlay = File("src/main/java/com/sonharf/game/mascot/ReactiveMageCatOverlay.kt").readText()
        assertTrue(companion.contains("R.drawable.mage_cat_expressions"))
        assertTrue(companion.contains("BitmapFactory.decodeStream"))
        assertFalse(companion.contains("Canvas("))
        assertFalse(companion.contains("drawCircle"))
        assertFalse(overlay.contains("OnlineGameBackend"))
        assertFalse(overlay.contains("findPremierActiveRoom"))
        assertFalse(overlay.contains("submitWord("))
        assertFalse(overlay.contains("claimTurnTimeout("))
        assertFalse(overlay.contains("botTakeTurn("))
    }
}
