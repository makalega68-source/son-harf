package com.sonharf.game

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrandStartupRegressionTest {
    @Test
    fun startupBrandUsesValidatedKelimeTahtiAssets() {
        val authGate = File("src/main/java/com/sonharf/game/RequiredAuthGate.kt").readText()
        val officialLogo = File("src/main/java/com/sonharf/game/SonHarfOfficialLogo.kt").readText()
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val icon = File("src/main/res/drawable-nodpi/kelime_tahti_app_icon.png")
        val logo = File("src/main/res/drawable-nodpi/kelime_tahti_logo.png")
        val authLogo = File("src/main/res/drawable-nodpi/son_harf_gold_teal_logo.png")

        assertTrue(manifest.contains("android:label=\"Kelime Tahtı\""))
        assertTrue(manifest.contains("android:icon=\"@drawable/kelime_tahti_app_icon\""))
        assertTrue(manifest.contains("android:roundIcon=\"@drawable/kelime_tahti_app_icon\""))
        assertTrue(officialLogo.contains("painterResource(R.drawable.kelime_tahti_logo)"))
        assertTrue(authGate.contains("painterResource(R.drawable.son_harf_gold_teal_logo)"))

        assertValidPng(icon)
        assertValidPng(logo)
        assertValidPng(authLogo)

        // Authentication keeps its stable resource identifier while its bytes are the
        // approved Kelime Tahtı logo, avoiding a risky auth-screen source rewrite.
        assertFalse(File("src/main/res/drawable-nodpi/son_harf_gold_teal_logo.webp").exists())
        assertEquals(logo.readBytes().toList(), authLogo.readBytes().toList())

        // Legacy malformed startup resources must not become launcher assets again.
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
