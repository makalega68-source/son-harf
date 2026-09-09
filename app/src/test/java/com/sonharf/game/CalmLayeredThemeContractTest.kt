package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalmLayeredThemeContractTest {
    @Test
    fun calmThemeDefinesIndependentSemanticLayers() {
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
            "val Lavender: Color get()",
            "val Sand: Color get()",
        ).forEach { token -> assertTrue("Missing theme layer: $token", theme.contains(token)) }

        assertTrue(theme.contains("Color(0xFFF4F7F2)"))
        assertTrue(theme.contains("Color(0xFFFFFDF7)"))
        assertTrue(theme.contains("Color(0xFFEAF2EE)"))
        assertTrue(theme.contains("Color(0xFF4F725E)"))
        assertTrue(theme.contains("Color(0xFF4A6E83)"))
        assertTrue(theme.contains("Color(0xFF477B78)"))
        assertTrue(theme.contains("Color(0xFF7B6B95)"))
        assertTrue(theme.contains("Color(0xFFF1E7D3)"))
    }

    @Test
    fun sharedShellAndNativeWindowNoLongerUseLegacyBrightPortalPalette() {
        val primitives = source("AppUiPrimitives.kt")
        val styles = projectFile("app/src/main/res/values/styles.xml").readText()

        assertTrue(primitives.contains("internal val PortalBg: Color get() = SonHarfTheme.Background"))
        assertTrue(primitives.contains("internal val PortalCard: Color get() = SonHarfTheme.Surface"))
        assertTrue(primitives.contains("internal val PortalBlue: Color get() = SonHarfTheme.SoftBlue"))
        assertFalse(primitives.contains("internal val PortalBlue = Color(0xFF1769E0)"))
        assertFalse(primitives.contains("internal val PortalGold = Color(0xFFF3A81A)"))

        assertTrue(styles.contains("<item name=\"android:windowBackground\">#F4F7F2</item>"))
        assertTrue(styles.contains("<item name=\"android:statusBarColor\">#F4F7F2</item>"))
        assertTrue(styles.contains("<item name=\"android:navigationBarColor\">#EEF3F0</item>"))
    }

    private fun source(name: String) = projectFile("app/src/main/java/com/sonharf/game/$name").readText()

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
