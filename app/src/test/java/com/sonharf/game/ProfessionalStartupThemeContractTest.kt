package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfessionalStartupThemeContractTest {
    @Test
    fun `startup language screen uses professional design tokens`() {
        val startup = projectFile("app/src/main/java/com/sonharf/game/StableV1App.kt").readText()

        assertTrue(startup.contains("GameTheme {"))
        assertTrue(startup.contains("GameColors.AppBackground"))
        assertTrue(startup.contains("GameColors.PrimaryBlue"))
        assertTrue(startup.contains("GameColors.TextPrimary"))
        assertTrue(startup.contains("GameColors.TextSecondary"))
        assertTrue(startup.contains("GameShapes.Medium"))
        assertFalse(startup.contains("MainUi.Background"))
        assertFalse(startup.contains("MainUi.Blue"))
        assertFalse(startup.contains("MainUi.Text"))
        assertFalse(startup.contains("MainUi.Muted"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
