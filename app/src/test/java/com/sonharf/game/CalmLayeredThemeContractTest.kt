package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalmLayeredThemeContractTest {
    @Test
    fun matureWordGameThemeDefinesIndependentSemanticLayers() {
        val theme = source("SonHarfTheme.kt")

        listOf(
            "val Background: Color get()",
            "val Surface: Color get()",
            "val SurfaceSecondary: Color get()",
            "val SurfaceElevated: Color get()",
            "val NavigationSurface: Color get()",
            "val ModalSurface: Color get()",
            "val GameSurface: Color get()",
            "val GameTile: Color get()",
            "val GameTileBorder: Color get()",
            "val Primary: Color get()",
            "val SoftBlue: Color get()",
            "val Turquoise: Color get()",
            "val ActionOrange: Color get()",
            "val HeroStart: Color get()",
            "val HeroMiddle: Color get()",
            "val HeroEnd: Color get()",
            "val TextPrimary: Color get()",
            "val TextSecondary: Color get()",
        ).forEach { token -> assertTrue("Missing theme layer: $token", theme.contains(token)) }

        listOf(
            "0xFFF6F4EE", // warm paper
            "0xFFFFFEFA", // surface
            "0xFF285943", // forest primary
            "0xFF173B2E", // forest deep
            "0xFF77977F", // sage
            "0xFF6F8794", // mist blue
            "0xFFAA6255", // restrained rival/error
            "0xFFB58A39", // sparse gold
            "0xFF18322A", // ink
            "0xFF66766F", // muted text
        ).forEach { token -> assertTrue("Missing mature palette token $token", theme.contains(token)) }
        assertTrue(theme.contains("private val alternateDark"))
        assertFalse(theme.contains("0xFFEFFF19"))
        assertFalse(theme.contains("0xFFFF245C"))
    }

    @Test
    fun shellAndAndroidWindowUseLightQuietChrome() {
        val primitives = source("AppUiPrimitives.kt")
        val styles = projectFile("app/src/main/res/values/styles.xml").readText()

        assertTrue(primitives.contains("internal val PortalCard: Color get() = SonHarfTheme.Surface"))
        assertTrue(primitives.contains("internal val PortalBlue: Color get() = SonHarfTheme.SoftBlue"))
        assertTrue(primitives.contains("val Orange: Color get() = SonHarfTheme.ActionOrange"))
        assertTrue(primitives.contains("internal object MainUiShape"))
        assertFalse(primitives.contains("internal val PortalBlue = Color(0xFF1769E0)"))

        assertTrue(styles.contains("<item name=\"android:windowBackground\">#F6F4EE</item>"))
        assertTrue(styles.contains("<item name=\"android:statusBarColor\">#F6F4EE</item>"))
        assertTrue(styles.contains("<item name=\"android:navigationBarColor\">#FBFAF6</item>"))
        assertTrue(styles.contains("<item name=\"android:windowLightStatusBar\">true</item>"))
        assertTrue(styles.contains("<item name=\"android:windowLightNavigationBar\">true</item>"))
    }

    private fun source(name: String) = projectFile("app/src/main/java/com/sonharf/game/$name").readText()

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
