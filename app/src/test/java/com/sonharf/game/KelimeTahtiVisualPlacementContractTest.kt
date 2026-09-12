package com.sonharf.game

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KelimeTahtiVisualPlacementContractTest {
    @Test
    fun requestedBrandSurfacesUseLatestKelimeTahtiLogo() {
        val source = File("src/main/java/com/sonharf/game/SonHarfOfficialLogo.kt").readText()
        val latest = File("src/main/res/drawable-nodpi/kelime_tahti_logo_latest.webp")
        val authAlias = File("src/main/res/drawable-nodpi/son_harf_gold_teal_logo.webp")
        val gameEntryAlias = File("src/main/res/drawable/kelime_kusatma_logo_hd.webp")

        assertTrue(source.contains("R.drawable.kelime_tahti_logo_latest"))
        assertFalse(source.contains("R.drawable.kelime_tahti_app_icon"))
        assertValidWebp(latest)
        assertValidWebp(authAlias)
        assertValidWebp(gameEntryAlias)
        assertEquals(latest.readBytes().toList(), authAlias.readBytes().toList())
        assertEquals(latest.readBytes().toList(), gameEntryAlias.readBytes().toList())
        assertFalse(File("src/main/res/drawable-nodpi/son_harf_gold_teal_logo.png").exists())
        assertFalse(File("src/main/res/drawable/kelime_kusatma_logo_hd.png").exists())
    }

    @Test
    fun untouchedLegacySiegeLogoIdsRemainApprovedRasterPngs() {
        val approved = File("src/main/res/drawable-nodpi/kelime_tahti_logo.png")
        assertValidPng(approved)

        listOf(
            "src/main/res/drawable/kelime_kusatma_logo.png",
            "src/main/res/drawable/kelime_kusatmasi_brand.png",
        ).forEach { path ->
            val file = File(path)
            assertValidPng(file)
            assertEquals("Legacy drawable must use approved Kelime Tahtı raster bytes: $path", approved.readBytes().toList(), file.readBytes().toList())
        }

        assertFalse(File("src/main/res/drawable/kelime_kusatma_logo.xml").exists())
        assertFalse(File("src/main/res/drawable/kelime_kusatmasi_brand.xml").exists())
    }

    @Test
    fun wordThroneHubResourceResolvesToLatestWebpAlias() {
        val screen = File("src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()
        assertTrue(screen.contains("painterResource(R.drawable.kelime_kusatma_logo_hd)"))
        assertTrue(File("src/main/res/drawable/kelime_kusatma_logo_hd.webp").isFile)
        assertFalse(File("src/main/res/drawable/kelime_kusatma_logo_hd.xml").exists())
    }

    private fun assertValidPng(file: File) {
        assertTrue("Missing raster drawable: ${file.path}", file.isFile)
        val bytes = file.readBytes()
        assertTrue("PNG drawable is too small: ${file.path}", bytes.size >= 8)
        val signature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        assertTrue("Drawable is not a PNG: ${file.path}", bytes.copyOfRange(0, 8).contentEquals(signature))
    }

    private fun assertValidWebp(file: File) {
        assertTrue("Missing WebP drawable: ${file.path}", file.isFile)
        val bytes = file.readBytes()
        assertTrue("WebP drawable is too small: ${file.path}", bytes.size >= 12)
        assertEquals("RIFF", bytes.copyOfRange(0, 4).toString(Charsets.US_ASCII))
        assertEquals("WEBP", bytes.copyOfRange(8, 12).toString(Charsets.US_ASCII))
    }
}