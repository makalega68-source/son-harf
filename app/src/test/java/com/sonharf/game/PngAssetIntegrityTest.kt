package com.sonharf.game

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.CRC32
import java.util.zip.Inflater
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Every committed PNG must be a complete, valid file: correct chunk CRCs, an IEND chunk and
 * image data that fully inflates. Android decodes a damaged PNG only partially, so a broken
 * asset shows up as a half-drawn button on a phone instead of failing loudly.
 */
class PngAssetIntegrityTest {
    @Test
    fun `all drawable PNGs are complete and decodable`() {
        val res = sequenceOf(File("src/main/res"), File("app/src/main/res")).firstOrNull { it.isDirectory }
            ?: error("Missing res directory")
        val pngs = res.walkTopDown().filter { it.isFile && it.extension == "png" }.toList()
        assertTrue("No PNG files found", pngs.isNotEmpty())
        val broken = pngs.mapNotNull { file -> problem(file.readBytes())?.let { "${file.relativeTo(res)}: $it" } }
        if (broken.isNotEmpty()) fail("Broken PNG assets:\n" + broken.joinToString("\n"))
    }

    private fun problem(bytes: ByteArray): String? {
        val signature = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 13, 10, 26, 10)
        if (bytes.size < 8 || !bytes.copyOfRange(0, 8).contentEquals(signature)) return "bad signature"
        var offset = 8
        var sawEnd = false
        val idat = ByteArrayOutputStream()
        while (offset + 12 <= bytes.size) {
            val length = readInt(bytes, offset)
            if (length < 0 || offset + 12 + length > bytes.size) return "truncated chunk"
            val type = String(bytes, offset + 4, 4, Charsets.ISO_8859_1)
            val crc = CRC32().apply { update(bytes, offset + 4, 4 + length) }.value
            if (crc != (readInt(bytes, offset + 8 + length).toLong() and 0xffffffffL)) return "bad CRC in $type"
            if (type == "IDAT") idat.write(bytes, offset + 8, length)
            offset += 12 + length
            if (type == "IEND") {
                sawEnd = true
                break
            }
        }
        if (!sawEnd) return "missing IEND"
        val inflater = Inflater()
        return try {
            inflater.setInput(idat.toByteArray())
            val buffer = ByteArray(64 * 1024)
            while (!inflater.finished()) {
                if (inflater.inflate(buffer) == 0 && (inflater.needsInput() || inflater.needsDictionary())) return "image data ends early"
            }
            null
        } catch (error: java.util.zip.DataFormatException) {
            "image data does not inflate (${error.message})"
        } finally {
            inflater.end()
        }
    }

    private fun readInt(bytes: ByteArray, at: Int): Int =
        ((bytes[at].toInt() and 0xff) shl 24) or ((bytes[at + 1].toInt() and 0xff) shl 16) or
            ((bytes[at + 2].toInt() and 0xff) shl 8) or (bytes[at + 3].toInt() and 0xff)
}
