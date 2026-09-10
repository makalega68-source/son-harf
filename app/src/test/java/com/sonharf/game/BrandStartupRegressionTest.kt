package com.sonharf.game

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrandStartupRegressionTest {
    @Test
    fun startupBrandUsesValidatedCanonicalWebpResources() {
        val logoComposable = File("src/main/java/com/sonharf/game/SonHarfBrandLogo.kt").readText()
        val authGate = File("src/main/java/com/sonharf/game/RequiredAuthGate.kt").readText()
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val icon = File("src/main/res/drawable-nodpi/son_harf_app_icon_master.webp")
        val logo = File("src/main/res/drawable-nodpi/son_harf_gold_teal_logo.webp")

        // Keep the generated Compose brand primitive independent from bitmap decoding.
        assertFalse(logoComposable.contains("painterResource("))
        assertFalse(logoComposable.contains("son_harf_splash_logo"))
        assertTrue(logoComposable.contains("Surface("))

        // Launcher and auth screen must resolve only to the canonical user-supplied assets.
        assertTrue(manifest.contains("android:icon=\"@drawable/son_harf_app_icon_master\""))
        assertTrue(manifest.contains("android:roundIcon=\"@drawable/son_harf_app_icon_master\""))
        assertTrue(authGate.contains("painterResource(R.drawable.son_harf_gold_teal_logo)"))

        assertValidWebP(icon)
        assertValidWebP(logo)

        // Regression lock: the malformed/legacy startup resources must not return.
        assertFalse(File("src/main/res/drawable-nodpi/son_harf_gold_teal_logo.png").exists())
        assertFalse(File("src/main/res/drawable/son_harf_gold_teal_logo.xml").exists())
        assertFalse(File("src/main/res/drawable/son_harf_app_icon_safe.xml").exists())
        assertFalse(File("src/main/res/drawable/son_harf_splash_logo.webp").exists())
        assertFalse(File("src/main/res/drawable/son_harf_app_icon.webp").exists())
    }

    private fun assertValidWebP(file: File) {
        assertTrue("Missing branding asset: ${file.path}", file.isFile)
        val bytes = file.readBytes()
        assertTrue("WebP asset is too small: ${file.path}", bytes.size >= 12)
        assertEquals("RIFF", bytes.copyOfRange(0, 4).toString(Charsets.US_ASCII))
        assertEquals("WEBP", bytes.copyOfRange(8, 12).toString(Charsets.US_ASCII))
    }
}
