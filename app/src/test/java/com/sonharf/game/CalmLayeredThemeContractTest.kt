package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalmLayeredThemeContractTest {
    @Test
    fun purchasedMonsterThemeDefinesIndependentSemanticLayers() {
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

        assertTrue(theme.contains("Color(0xFFEAF6F8)"))
        assertTrue(theme.contains("Color(0xFFFFFFFF)"))
        assertTrue(theme.contains("Color(0xFF14B8B0)"))
        assertTrue(theme.contains("Color(0xFFE8622C)"))
        assertTrue(theme.contains("Color(0xFF8B6CF0)"))
        assertTrue(theme.contains("Color(0xFF0B1B33)"))
        assertTrue(theme.contains("Color(0xFF3B4B66)"))
        assertTrue(theme.contains("val IsDark: Boolean get() = false"))
    }

    @Test
    fun shellUsesMonsterDarkChromeInsteadOfPreviousBlueLightShell() {
        val primitives = source("AppUiPrimitives.kt")
        val styles = projectFile("app/src/main/res/values/styles.xml").readText()

        assertTrue(primitives.contains("internal val PortalCard: Color get() = SonHarfTheme.Surface"))
        assertTrue(primitives.contains("internal val PortalBlue: Color get() = SonHarfTheme.SoftBlue"))
        assertTrue(primitives.contains("val Orange: Color get() = SonHarfTheme.ActionOrange"))
        assertTrue(primitives.contains("internal object MainUiShape"))
        assertFalse(primitives.contains("internal val PortalBlue = Color(0xFF1769E0)"))

        assertTrue(styles.contains("<item name=\"android:windowBackground\">#071714</item>"))
        assertTrue(styles.contains("<item name=\"android:statusBarColor\">#071714</item>"))
        assertTrue(styles.contains("<item name=\"android:navigationBarColor\">#091A17</item>"))
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
