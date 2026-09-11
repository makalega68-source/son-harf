package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalmLayeredThemeContractTest {
    @Test
    fun premiumBotanicalThemeDefinesIndependentSemanticLayers() {
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
            "val HeroStart: Color get()",
            "val HeroMiddle: Color get()",
            "val HeroEnd: Color get()",
            "val PremiumGold: Color get()",
        ).forEach { token -> assertTrue("Missing theme layer: $token", theme.contains(token)) }

        // Approved premium botanical identity: warm ivory + sage + soft blue/teal + lavender/gold.
        assertTrue(theme.contains("Color(0xFFF8FAF4)"))
        assertTrue(theme.contains("Color(0xFFFFFEF8)"))
        assertTrue(theme.contains("Color(0xFFEEF5EF)"))
        assertTrue(theme.contains("Color(0xFF4F7964)"))
        assertTrue(theme.contains("Color(0xFF557A87)"))
        assertTrue(theme.contains("Color(0xFF4F8B82)"))
        assertTrue(theme.contains("Color(0xFF7A7396)"))
        assertTrue(theme.contains("Color(0xFFDCC8A0)"))
        assertTrue(theme.contains("Color(0xFFD7B35C)"))
    }

    @Test
    fun sharedShellUsesBotanicalThemeAndAllowsBackdropToRemainVisible() {
        val primitives = source("AppUiPrimitives.kt")
        val styles = projectFile("app/src/main/res/values/styles.xml").readText()

        assertTrue(primitives.contains("internal val PortalBg: Color get() = if (SonHarfTheme.IsDark)"))
        assertTrue(primitives.contains("SonHarfTheme.Background.copy(alpha = .94f)"))
        assertTrue(primitives.contains("internal val PortalCard: Color get() = SonHarfTheme.Surface"))
        assertTrue(primitives.contains("internal val PortalBlue: Color get() = SonHarfTheme.SoftBlue"))
        assertFalse(primitives.contains("internal val PortalBlue = Color(0xFF1769E0)"))
        assertFalse(primitives.contains("internal val PortalGold = Color(0xFFF3A81A)"))

        // Native startup background stays in the same calm family while Compose takes over.
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
