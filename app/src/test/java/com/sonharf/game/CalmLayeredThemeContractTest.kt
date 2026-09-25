package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalmLayeredThemeContractTest {
    @Test
    fun compatibilityThemeMapsToProfessionalSemanticLayers() {
        val theme = source("SonHarfTheme.kt")
        val design = source("GameDesignSystem.kt")

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
            "Color(0xFF101722)",
            "Color(0xFF151F2D)",
            "Color(0xFF1C2939)",
            "Color(0xFF243448)",
            "Color(0xFF3D8BFF)",
            "Color(0xFF20B6B0)",
            "Color(0xFF38C970)",
            "Color(0xFF9874E8)",
            "Color(0xFFF2A73B)",
            "Color(0xFFE75D65)",
            "Color(0xFFF4F7FB)",
            "Color(0xFFA8B5C6)",
        ).forEach { token -> assertTrue("Missing professional palette token: $token", design.contains(token)) }

        assertTrue(theme.contains("val IsDark: Boolean get() = true"))
        assertTrue(theme.contains("GameColors.AppBackground"))
        assertTrue(theme.contains("GameColors.PrimaryBlue"))
        assertTrue(theme.contains("GameColors.TacticalTurquoise"))
        assertTrue(theme.contains("GameColors.PlayGreen"))
        assertTrue(theme.contains("GameColors.Lavender"))
        assertFalse(theme.contains("Color(0xFFEFFF19)"))
        assertFalse(theme.contains("Color(0xFFFF245C)"))
    }

    @Test
    fun shellUsesProfessionalDarkChromeInsteadOfRetiredMonsterChrome() {
        val primitives = source("AppUiPrimitives.kt")
        val styles = projectFile("app/src/main/res/values/styles.xml").readText()

        assertTrue(primitives.contains("internal val PortalCard: Color get() = SonHarfTheme.Surface"))
        assertTrue(primitives.contains("internal val PortalBlue: Color get() = SonHarfTheme.SoftBlue"))
        assertTrue(primitives.contains("val Orange: Color get() = SonHarfTheme.ActionOrange"))
        assertTrue(primitives.contains("internal object MainUiShape"))
        assertFalse(primitives.contains("internal val PortalBlue = Color(0xFF1769E0)"))

        assertTrue(styles.contains("<item name=\"android:windowBackground\">#101722</item>"))
        assertTrue(styles.contains("<item name=\"android:statusBarColor\">#101722</item>"))
        assertTrue(styles.contains("<item name=\"android:navigationBarColor\">#151F2D</item>"))
        assertTrue(styles.contains("<item name=\"android:windowLightStatusBar\">false</item>"))
        assertTrue(styles.contains("<item name=\"android:windowLightNavigationBar\">false</item>"))
        assertFalse(styles.contains("#0D0F12"))
        assertFalse(styles.contains("#111318"))
    }

    private fun source(name: String) = projectFile("app/src/main/java/com/sonharf/game/$name").readText()

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
