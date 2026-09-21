package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalmLayeredThemeContractTest {
    @Test
    fun purchasedCasualThemeDefinesIndependentSemanticLayers() {
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
            "Color(0xFF102A56)",
            "Color(0xFFF5F8FC)",
            "Color(0xFFFFFFFF)",
            "Color(0xFF52AD56)",
            "Color(0xFF4D83DA)",
            "Color(0xFF8A62D3)",
            "Color(0xFFF5A623)",
            "Color(0xFF38B8BD)",
        ).forEach { token -> assertTrue("Missing purchased-theme palette token $token", theme.contains(token)) }
        assertTrue(theme.contains("val IsDark: Boolean get() = false"))
    }

    @Test
    fun shellUsesSharedThemePrimitivesAndStableLaunchChrome() {
        val primitives = source("AppUiPrimitives.kt")
        val styles = projectFile("app/src/main/res/values/styles.xml").readText()

        assertTrue(primitives.contains("internal val PortalCard: Color get() = SonHarfTheme.Surface"))
        assertTrue(primitives.contains("internal val PortalBlue: Color get() = SonHarfTheme.SoftBlue"))
        assertTrue(primitives.contains("val Orange: Color get() = SonHarfTheme.ActionOrange"))
        assertTrue(primitives.contains("internal object MainUiShape"))
        assertFalse(primitives.contains("internal val PortalBlue = Color(0xFF1769E0)"))

        assertTrue(styles.contains("<item name=\"android:windowBackground\">#0D0F12</item>"))
        assertTrue(styles.contains("<item name=\"android:statusBarColor\">#0D0F12</item>"))
        assertTrue(styles.contains("<item name=\"android:navigationBarColor\">#111318</item>"))
        assertTrue(styles.contains("<item name=\"android:windowLightStatusBar\">false</item>"))
        assertTrue(styles.contains("<item name=\"android:windowLightNavigationBar\">false</item>"))
    }

    private fun source(name: String) = projectFile("app/src/main/java/com/sonharf/game/$name").readText()

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
