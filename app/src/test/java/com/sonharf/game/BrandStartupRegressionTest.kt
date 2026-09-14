package com.sonharf.game

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrandStartupRegressionTest {
    @Test
    fun startupBrandUsesAndroidDecodableKelimeTahtiAssetsAndAdaptiveLauncherIcon() {
        val authGate = File("src/main/java/com/sonharf/game/RequiredAuthGate.kt").readText()
        val officialLogo = File("src/main/java/com/sonharf/game/SonHarfOfficialLogo.kt").readText()
        val siegeHub = File("src/main/java/com/sonharf/game/WordSiegeExperience.kt").readText()
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val latestLogo = File("src/main/res/drawable-nodpi/kelime_tahti_logo_latest.png")
        val authLogo = File("src/main/res/drawable-nodpi/son_harf_gold_teal_logo.png")
        val gameEntryLogo = File("src/main/res/drawable/kelime_kusatma_logo_hd.png")
        val approvedLogo = File("src/main/res/drawable-nodpi/kelime_tahti_logo.png")
        val adaptiveIcon = File("src/main/res/mipmap-anydpi-v26/ic_kelime_tahti.xml")
        val adaptiveFallback = File("src/main/res/mipmap-anydpi/ic_kelime_tahti.xml")
        val adaptiveBackground = File("src/main/res/drawable/kelime_tahti_launcher_background.xml")

        assertTrue(manifest.contains("android:label=\"Kelime Tahtı\""))
        assertTrue(manifest.contains("android:icon=\"@mipmap/ic_kelime_tahti\""))
        assertTrue(manifest.contains("android:roundIcon=\"@mipmap/ic_kelime_tahti\""))
        assertTrue(adaptiveIcon.isFile)
        assertTrue(adaptiveFallback.isFile)
        assertTrue(adaptiveBackground.isFile)
        assertTrue(adaptiveIcon.readText().contains("@drawable/kelime_tahti_app_icon"))

        assertTrue(officialLogo.contains("painterResource(R.drawable.kelime_tahti_logo_latest)"))
        assertTrue(authGate.contains("painterResource(R.drawable.son_harf_gold_teal_logo)"))
        assertTrue(siegeHub.contains("painterResource(R.drawable.kelime_kusatma_logo_hd)"))

        assertValidPng(approvedLogo)
        assertValidPng(latestLogo)
        assertValidPng(authLogo)
        assertValidPng(gameEntryLogo)
        assertEquals(approvedLogo.readBytes().toList(), latestLogo.readBytes().toList())
        assertEquals(approvedLogo.readBytes().toList(), authLogo.readBytes().toList())
        assertEquals(approvedLogo.readBytes().toList(), gameEntryLogo.readBytes().toList())

        assertFalse(File("src/main/res/drawable-nodpi/kelime_tahti_logo_latest.webp").exists())
        assertFalse(File("src/main/res/drawable-nodpi/son_harf_gold_teal_logo.webp").exists())
        assertFalse(File("src/main/res/drawable/kelime_kusatma_logo_hd.webp").exists())
        assertFalse(File("src/main/res/drawable/son_harf_app_icon_safe.xml").exists())
        assertFalse(File("src/main/res/drawable/son_harf_splash_logo.webp").exists())
        assertFalse(File("src/main/res/drawable/son_harf_app_icon.webp").exists())
    }

    private fun assertValidPng(file: File) {
        assertTrue("Missing branding asset: ${file.path}", file.isFile)
        val bytes = file.readBytes()
        assertTrue("PNG asset is too small: ${file.path}", bytes.size >= 8)
        val expected = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        assertTrue("Invalid PNG signature: ${file.path}", bytes.copyOfRange(0, 8).contentEquals(expected))
    }
}
