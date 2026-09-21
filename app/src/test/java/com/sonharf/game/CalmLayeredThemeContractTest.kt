package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalmLayeredThemeContractTest {
    @Test
    fun premiumSystemDefinesNativeDarkAndOptionalBlackSemanticLayers() {
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

        val colors = source("Color.kt")
        listOf("141923", "1E2538", "2ECC71", "3498DB", "9B59B6", "E67E22", "ECF0F1", "95A5A6")
            .forEach { token -> assertTrue("Missing native dark token: $token", colors.contains(token)) }

        assertTrue(theme.contains("internal object BlackThemePalette"))
        assertTrue(theme.contains("val IsDark: Boolean get() = SonHarfCosmetics.blackThemeActive"))
        assertFalse(colors.contains("0xFFF4F2EC"))
    }

    @Test
    fun launchChromeMatchesNativeDarkProductShell() {
        val primitives = source("AppUiPrimitives.kt")
        val styles = projectFile("app/src/main/res/values/styles.xml").readText()
        assertTrue(primitives.contains("internal fun AppCard("))
        assertTrue(primitives.contains("internal fun AppButton("))
        assertTrue(primitives.contains("PremiumPrimaryButton"))
        assertTrue(styles.contains("<item name=\"android:windowBackground\">#141923</item>"))
        assertTrue(styles.contains("<item name=\"android:navigationBarColor\">#171D2A</item>"))
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
