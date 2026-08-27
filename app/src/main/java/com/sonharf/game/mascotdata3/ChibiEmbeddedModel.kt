package com.sonharf.game.mascotdata3

import android.content.Context
import java.io.File
import java.security.MessageDigest
import java.util.Base64
import java.util.zip.GZIPInputStream

internal object ChibiEmbeddedModel {
    private val encoded by lazy {
        listOf(
            CHIBI_CHUNK_000,
            CHIBI_CHUNK_001,
            CHIBI_CHUNK_002,
            CHIBI_CHUNK_003,
            CHIBI_CHUNK_004,
            CHIBI_CHUNK_005,
            CHIBI_CHUNK_006,
            CHIBI_CHUNK_007,
            CHIBI_CHUNK_008,
            CHIBI_CHUNK_009,
            CHIBI_CHUNK_010,
            CHIBI_CHUNK_011,
            CHIBI_CHUNK_012,
            CHIBI_CHUNK_013,
            CHIBI_CHUNK_014,
            CHIBI_CHUNK_015,
            CHIBI_CHUNK_016,
            CHIBI_CHUNK_017,
            CHIBI_CHUNK_018,
            CHIBI_CHUNK_019,
            CHIBI_CHUNK_020,
            CHIBI_CHUNK_021,
            CHIBI_CHUNK_022,
            CHIBI_CHUNK_023,
            CHIBI_CHUNK_024,
            CHIBI_CHUNK_025,
            CHIBI_CHUNK_026,
            CHIBI_CHUNK_027,
            CHIBI_CHUNK_028,
            CHIBI_CHUNK_029,
            CHIBI_CHUNK_030,
            CHIBI_CHUNK_031,
            CHIBI_CHUNK_032,
            CHIBI_CHUNK_033,
            CHIBI_CHUNK_034,
            CHIBI_CHUNK_035,
            CHIBI_CHUNK_036,
            CHIBI_CHUNK_037,
            CHIBI_CHUNK_038,
            CHIBI_CHUNK_039,
            CHIBI_CHUNK_040,
            CHIBI_CHUNK_041,
            CHIBI_CHUNK_042,
            CHIBI_CHUNK_043,
            CHIBI_CHUNK_044,
            CHIBI_CHUNK_045,
            CHIBI_CHUNK_046,
            CHIBI_CHUNK_047,
            CHIBI_CHUNK_048,
            CHIBI_CHUNK_049,
            CHIBI_CHUNK_050,
            CHIBI_CHUNK_051,
            CHIBI_CHUNK_052,
            CHIBI_CHUNK_053,
            CHIBI_CHUNK_054,
            CHIBI_CHUNK_055,
            CHIBI_CHUNK_056,
            CHIBI_CHUNK_057,
            CHIBI_CHUNK_058,
            CHIBI_CHUNK_059,
            CHIBI_CHUNK_060,
            CHIBI_CHUNK_061,
            CHIBI_CHUNK_062,
            CHIBI_CHUNK_063,
            CHIBI_CHUNK_064,
            CHIBI_CHUNK_065,
            CHIBI_CHUNK_066,
            CHIBI_CHUNK_067,
            CHIBI_CHUNK_068,
            CHIBI_CHUNK_069,
            CHIBI_CHUNK_070,
            CHIBI_CHUNK_071,
            CHIBI_CHUNK_072
        ).joinToString(separator = "")
    }

    internal fun decodeGlb(): ByteArray {
        val compressed = Base64.getDecoder().decode(encoded)
        return GZIPInputStream(compressed.inputStream()).use { it.readBytes() }
    }

    internal fun ensureFile(context: Context): File {
        val target = File(context.cacheDir, "son_harf_chibi_wizard_animated_v1.glb")
        if (target.isFile && target.length() == EXPECTED_BYTES && sha256(target.readBytes()) == EXPECTED_SHA256) return target
        val bytes = decodeGlb()
        check(bytes.size.toLong() == EXPECTED_BYTES) { "Embedded Chibi GLB size mismatch" }
        check(sha256(bytes) == EXPECTED_SHA256) { "Embedded Chibi GLB SHA-256 mismatch" }
        val tmp = File(context.cacheDir, "son_harf_chibi_wizard_animated_v1.tmp")
        tmp.writeBytes(bytes)
        if (target.exists()) target.delete()
        check(tmp.renameTo(target)) { "Could not publish Chibi mascot GLB" }
        return target
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    internal const val EXPECTED_BYTES = 1500344L
    internal const val EXPECTED_SHA256 = "b321193faea91bf6d75a78fb74f947bc4223892c3f5ede0be0c04a7a2be04db2"
    internal val ANIMATION_NAMES = setOf("Hurt", "Turn_Right", "Attack", "Walk", "Turn_Left", "Death", "Idle", "Special_Attack", "Run")
}