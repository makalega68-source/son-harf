package com.sonharf.game.mascotdata2

import android.content.Context
import java.io.File
import java.util.Base64
import java.util.zip.GZIPInputStream

internal object MascotEmbeddedModel {
    private val encoded by lazy {
        listOf(
            MASCOT_CHUNK_00, MASCOT_CHUNK_01, MASCOT_CHUNK_02, MASCOT_CHUNK_03,
            MASCOT_CHUNK_04, MASCOT_CHUNK_05, MASCOT_CHUNK_06, MASCOT_CHUNK_07,
            MASCOT_CHUNK_08, MASCOT_CHUNK_09, MASCOT_CHUNK_10, MASCOT_CHUNK_11,
            MASCOT_CHUNK_12, MASCOT_CHUNK_13, MASCOT_CHUNK_14, MASCOT_CHUNK_15,
            MASCOT_CHUNK_16, MASCOT_CHUNK_17, MASCOT_CHUNK_18, MASCOT_CHUNK_19,
            MASCOT_CHUNK_20, MASCOT_CHUNK_21, MASCOT_CHUNK_22, MASCOT_CHUNK_23,
            MASCOT_CHUNK_24, MASCOT_CHUNK_25, MASCOT_CHUNK_26, MASCOT_CHUNK_27,
            MASCOT_CHUNK_28,
        ).joinToString(separator = "")
    }

    internal fun decodeGlb(): ByteArray {
        val compressed = Base64.getDecoder().decode(encoded)
        return GZIPInputStream(compressed.inputStream()).use { it.readBytes() }
    }

    internal fun ensureFile(context: Context): File {
        val target = File(context.cacheDir, "son_harf_fox_3d_v1.glb")
        if (target.exists() && target.length() == EXPECTED_BYTES) return target

        val tmp = File(context.cacheDir, "son_harf_fox_3d_v1.tmp")
        tmp.writeBytes(decodeGlb())
        check(tmp.length() == EXPECTED_BYTES) { "Embedded mascot GLB size mismatch" }
        if (target.exists()) target.delete()
        check(tmp.renameTo(target)) { "Could not publish mascot GLB" }
        return target
    }

    private const val EXPECTED_BYTES = 113_328L
}
