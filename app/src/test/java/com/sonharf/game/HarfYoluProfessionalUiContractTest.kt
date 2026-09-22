package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HarfYoluProfessionalUiContractTest {
    @Test
    fun `Harf Yolu game surface uses professional palette`() {
        val game = source("LetterLadderGame.kt")

        listOf(
            "val Background = GameColors.AppBackground",
            "val Surface = GameColors.PrimarySurface",
            "val SurfaceRaised = GameColors.PrimarySurface",
            "val SurfaceSoft = GameColors.SecondarySurface",
            "val Text = GameColors.TextPrimary",
            "val Muted = GameColors.TextSecondary",
            "val Border = GameColors.Border",
            "val Accent = GameColors.PrimaryBlue",
            "val AccentStrong = GameColors.DeepBlue",
            "val Turquoise = GameColors.TacticalTurquoise",
            "val Orange = GameColors.RewardAmber",
            "val Purple = GameColors.Lavender",
            "val Green = GameColors.PlayGreen",
        ).forEach { token -> assertTrue("Missing professional Harf Yolu game token: $token", game.contains(token)) }

        listOf(
            "Color(0xFFF5FCFF)",
            "Color(0xFFFCFEFF)",
            "Color(0xFFEAF8FC)",
            "Color(0xFF123A4A)",
            "Color(0xFFA9DCE7)",
            "Color(0xFF278DC3)",
            "Color(0xFF22BFC4)",
            "Color(0xFFFF9F43)",
            "Color(0xFF8B5CF6)",
        ).forEach { legacy -> assertFalse("Legacy Harf Yolu game color remains: $legacy", game.contains(legacy)) }
    }

    @Test
    fun `Harf Yolu keyboard uses professional palette and exact Turkish rows`() {
        val keyboard = source("HarfYoluKeyboard.kt")

        listOf(
            "val Background = GameColors.ElevatedBackground",
            "val Key = GameColors.PrimarySurface",
            "val KeyAlt = GameColors.SecondarySurface",
            "val Text = GameColors.TextPrimary",
            "val Action = GameColors.PlayGreen",
            "listOf(\"Q\",\"W\",\"E\",\"R\",\"T\",\"Y\",\"U\",\"I\",\"O\",\"P\",\"Ğ\",\"Ü\")",
            "listOf(\"A\",\"S\",\"D\",\"F\",\"G\",\"H\",\"J\",\"K\",\"L\",\"Ş\",\"İ\")",
            "listOf(\"Z\",\"X\",\"C\",\"V\",\"B\",\"N\",\"M\",\"Ö\",\"Ç\")",
            "label = \"⌫\"",
            "if (isEnglish) \"SEND\" else \"GÖNDER\"",
        ).forEach { token -> assertTrue("Missing Harf Yolu keyboard contract: $token", keyboard.contains(token)) }

        assertFalse(keyboard.contains("TEMİZLE"))
        assertFalse(keyboard.contains("CLEAR"))
        assertFalse(keyboard.contains("?123"))
        assertFalse(keyboard.contains("GIF"))
    }

    @Test
    fun `Harf Yolu backdrop uses professional dark design system`() {
        val backdrop = source("HarfYoluBackdrop.kt")

        listOf(
            "GameColors.AppBackground",
            "GameColors.ElevatedBackground",
            "GameColors.PrimaryBlue",
            "GameColors.TacticalTurquoise",
            "GameColors.Lavender",
            "GameColors.RewardAmber",
        ).forEach { token -> assertTrue("Missing professional backdrop token: $token", backdrop.contains(token)) }

        listOf(
            "Color.White,",
            "Color(0xFFF7FCFE)",
            "Color(0xFFEAF8FC)",
            "Color(0xFFFDF9FF)",
        ).forEach { legacy -> assertFalse("Legacy light backdrop remains: $legacy", backdrop.contains(legacy)) }
    }

    @Test
    fun `Harf Yolu keeps start four intermediate rows and target flow`() {
        val game = source("LetterLadderGame.kt")

        assertTrue(game.contains("Text(sh(\"BAŞLANGIÇ\", \"START\")"))
        assertTrue(game.contains("for (move in 1 until LetterLadderEngine.MOVE_COUNT)"))
        assertTrue(game.contains("const val MOVE_COUNT = 5"))
        assertTrue(game.contains("Text(sh(\"HEDEF\", \"TARGET\")"))
        assertTrue(game.contains("path.size == LetterLadderEngine.MOVE_COUNT + 1"))
        assertFalse(game.contains("Text(\"5\""))
    }

    private fun source(name: String) = projectFile("app/src/main/java/com/sonharf/game/$name").readText()

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
