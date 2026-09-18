package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalmLayeredThemeContractTest {
    @Test
    fun purchasedCompetitiveThemeDefinesIndependentSemanticLayers() {
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
            "val Lavender: Color get()",
            "val Sand: Color get()",
            "val HeroStart: Color get()",
            "val HeroMiddle: Color get()",
            "val HeroEnd: Color get()",
            "val PremiumGold: Color get()",
        ).forEach { token -> assertTrue("Missing theme layer: $token", theme.contains(token)) }

        // Purchased sports-dashboard language adapted to the requested blue + turquoise + orange identity.
        assertTrue(theme.contains("Color(0xFF1559D6)"))
        assertTrue(theme.contains("Color(0xFF0A347A)"))
        assertTrue(theme.contains("Color(0xFF15C7C4)"))
        assertTrue(theme.contains("Color(0xFFFF8A24)"))
        assertTrue(theme.contains("Color(0xFFEAF4FF)"))
        assertTrue(theme.contains("Color(0xFFF8FBFF)"))
        assertTrue(theme.contains("Color(0xFF53677D)"))
        assertTrue(theme.contains("Color(0xFFDDF8F5)"))
        assertTrue(theme.contains("Color(0xFF728BE8)"))
        assertTrue(theme.contains("Color(0xFFD9AD45)"))
    }

    @Test
    fun sharedShellUsesPurchasedThemeAndKeepsBackdropVisible() {
        val primitives = source("AppUiPrimitives.kt")
        val backdrop = source("SonHarfLeafBackdrop.kt")
        val styles = projectFile("app/src/main/res/values/styles.xml").readText()

        assertTrue(primitives.contains("internal val PortalBg: Color get() = if (SonHarfTheme.IsDark)"))
        assertTrue(primitives.contains("SonHarfTheme.Background.copy(alpha = .95f)"))
        assertTrue(primitives.contains("internal val PortalCard: Color get() = SonHarfTheme.Surface"))
        assertTrue(primitives.contains("internal val PortalBlue: Color get() = SonHarfTheme.SoftBlue"))
        assertTrue(primitives.contains("val Orange: Color get() = SonHarfTheme.ActionOrange"))
        assertTrue(primitives.contains("internal object MainUiShape"))
        assertFalse(primitives.contains("internal val PortalBlue = Color(0xFF1769E0)"))

        assertTrue(backdrop.contains("Color(0xFF1559D6)"))
        assertTrue(backdrop.contains("Color(0xFF15C7C4)"))
        assertTrue(backdrop.contains("Color(0xFFFF8A24)"))
        assertFalse(backdrop.contains("drawSprig("))

        // Native startup chrome now matches the default blue/sky shell while Compose takes over.
        assertTrue(styles.contains("<item name=\"android:windowBackground\">#F8FBFF</item>"))
        assertTrue(styles.contains("<item name=\"android:statusBarColor\">#F8FBFF</item>"))
        assertTrue(styles.contains("<item name=\"android:navigationBarColor\">#EDF6FF</item>"))
    }

    private fun source(name: String) = projectFile("app/src/main/java/com/sonharf/game/$name").readText()

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
