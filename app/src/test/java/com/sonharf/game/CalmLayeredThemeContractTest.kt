package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalmLayeredThemeContractTest {
    @Test
    fun premiumSystemDefinesLightAndBlackSemanticLayers() {
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
            "val HeroStart: Color get()",
            "val TextPrimary: Color get()",
            "val TextSecondary: Color get()",
        ).forEach { token -> assertTrue("Missing theme layer: $token", theme.contains(token)) }

        assertTrue(theme.contains("Color(0xFF2563EB)"))
        assertTrue(theme.contains("Color(0xFF12B8A6)"))
        assertTrue(theme.contains("Color(0xFF7C3AED)"))
        assertTrue(theme.contains("Color(0xFFF97316)"))
        assertTrue(theme.contains("internal object BlackThemePalette"))
        assertTrue(theme.contains("Color(0xFF090B10)"))
        assertTrue(theme.contains("val IsDark: Boolean get() = SonHarfCosmetics.blackThemeActive"))
        assertFalse(theme.contains("Color(0xFFEFFF19)"))
    }

    @Test
    fun launchChromeRemainsPremiumAndAvoidsLegacyMonsterFlash() {
        val primitives = source("AppUiPrimitives.kt")
        val styles = projectFile("app/src/main/res/values/styles.xml").readText()
        assertTrue(primitives.contains("internal val PortalCard: Color get() = SonHarfTheme.Surface"))
        assertTrue(primitives.contains("internal val PortalBlue: Color get() = SonHarfTheme.Primary"))
        assertTrue(primitives.contains("PremiumPrimaryButton"))
        assertTrue(styles.contains("<item name=\"android:windowBackground\">#F6F9FF</item>"))
        assertFalse(styles.contains("#0D0F12"))
    }

    private fun source(name: String) = projectFile("app/src/main/java/com/sonharf/game/$name").readText()
    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
