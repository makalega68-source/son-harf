package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrandStartupRegressionTest {
    @Test
    fun startupBrandCannotCrashOnRasterDrawableDecode() {
        val logo = File("src/main/java/com/sonharf/game/SonHarfBrandLogo.kt").readText()
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertFalse(logo.contains("painterResource("))
        assertFalse(logo.contains("son_harf_splash_logo"))
        assertTrue(logo.contains("Surface("))
        assertTrue(manifest.contains("@drawable/son_harf_app_icon_safe"))
        assertTrue(File("src/main/res/drawable/son_harf_app_icon_safe.xml").isFile)
        assertFalse(File("src/main/res/drawable/son_harf_splash_logo.webp").exists())
        assertFalse(File("src/main/res/drawable/son_harf_app_icon.webp").exists())
    }
}
