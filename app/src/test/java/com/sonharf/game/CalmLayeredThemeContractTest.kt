package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalmLayeredThemeContractTest {
    @Test
    fun premiumSystemDefinesSiegeRoyaleAndBlackSemanticLayers() {
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
            "val Turquoise: Color get()",
            "val ActionOrange: Color get()",
            "val PlayGreen: Color get()",
            "val HeroStart: Color get()",
            "val TextPrimary: Color get()",
            "val TextSecondary: Color get()",
        ).forEach { token -> assertTrue("Missing theme layer: $token", theme.contains(token)) }

        assertTrue(theme.contains("internal object SiegeRoyalePalette"))
        assertTrue(theme.contains("Color(0xFF246EDB)"))
        assertTrue(theme.contains("Color(0xFF17A7B8)"))
        assertTrue(theme.contains("Color(0xFF3AAF50)"))
        assertTrue(theme.contains("Color(0xFFF0A128)"))
        assertTrue(theme.contains("Color(0xFFEAF3FF)"))
        assertTrue(theme.contains("internal object BlackThemePalette"))
        assertTrue(theme.contains("val IsDark: Boolean get() = SonHarfCosmetics.blackThemeActive"))
        assertFalse(theme.contains("Color(0xFF365F53)"))
        assertFalse(theme.contains("Color(0xFFF4F2EC)"))
    }

    @Test
    fun launchChromeMatchesNewGameTheme() {
        val primitives = source("AppUiPrimitives.kt")
        val styles = projectFile("app/src/main/res/values/styles.xml").readText()
        assertTrue(primitives.contains("internal val PortalCard: Color get() = SonHarfTheme.Surface"))
        assertTrue(primitives.contains("internal val PortalBlue: Color get() = SonHarfTheme.Primary"))
        assertTrue(primitives.contains("PremiumPrimaryButton"))
        assertTrue(primitives.contains("SonHarfTheme.PlayGreen"))
        assertTrue(styles.contains("<item name=\"android:windowBackground\">#EAF3FF</item>"))
        assertTrue(styles.contains("<item name=\"android:navigationBarColor\">#F9FCFF</item>"))
        assertFalse(styles.contains("#F4F2EC"))
        assertFalse(styles.contains("#0D0F12"))
    }

    private fun source(name: String) = projectFile("app/src/main/java/com/sonharf/game/$name").readText()
    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
