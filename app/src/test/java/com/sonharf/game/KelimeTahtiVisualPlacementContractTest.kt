package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KelimeTahtiVisualPlacementContractTest {
    @Test
    fun compactHomeBrandUsesDedicatedKelimeTahtiIconAndReadableWordmark() {
        val source = File("src/main/java/com/sonharf/game/SonHarfOfficialLogo.kt").readText()
        assertTrue(source.contains("R.drawable.kelime_tahti_app_icon"))
        assertTrue(source.contains("KELİME\\nTAHTI"))
        assertFalse(source.contains("painterResource(R.drawable.kelime_tahti_logo)"))
    }

    @Test
    fun legacySiegeLogoIdsResolveToKelimeTahtiLogo() {
        listOf(
            "src/main/res/drawable/kelime_kusatma_logo_hd.xml",
            "src/main/res/drawable/kelime_kusatma_logo.xml",
            "src/main/res/drawable/kelime_kusatmasi_brand.xml",
        ).forEach { path ->
            val file = File(path)
            assertTrue("Missing alias: $path", file.isFile)
            assertTrue(file.readText().contains("@drawable/kelime_tahti_logo"))
        }
        assertFalse(File("src/main/res/drawable-nodpi/kelime_kusatma_logo_hd.webp").exists())
        assertFalse(File("src/main/res/drawable-nodpi/kelime_kusatma_logo.webp").exists())
        assertFalse(File("src/main/res/drawable-nodpi/kelime_kusatmasi_brand.webp").exists())
    }
}
