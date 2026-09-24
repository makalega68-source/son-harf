package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HarfYoluProfessionalUiContractTest {
    @Test
    fun `Harf Yolu game surface uses the light Kelime Tahti palette`() {
        val game = source("LetterLadderGame.kt")

        assertTrue(game.contains("val Background = Color(0xFFEAF6F8)"))
        assertTrue(game.contains("val Accent = Color(0xFF14B8B0)"))
    }


    @Test
    fun `Harf Yolu keyboard keeps the light palette and exact Turkish rows`() {
        val keyboard = source("HarfYoluKeyboard.kt")

        listOf(
            "0xFF14B8B0",
            "listOf(\"Q\",\"W\",\"E\",\"R\",\"T\",\"Y\",\"U\",\"I\",\"O\",\"P\",\"Ğ\",\"Ü\")",
            "listOf(\"A\",\"S\",\"D\",\"F\",\"G\",\"H\",\"J\",\"K\",\"L\",\"Ş\",\"İ\")",
            "listOf(\"Z\",\"X\",\"C\",\"V\",\"B\",\"N\",\"M\",\"Ö\",\"Ç\")",
            "label = \"⌫\"",
        ).forEach { token -> assertTrue("Missing Harf Yolu keyboard contract: $token", keyboard.contains(token)) }

        assertFalse(keyboard.contains("?123"))
        assertFalse(keyboard.contains("GIF"))
    }


    @Test
    fun `Harf Yolu backdrop uses the light Kelime Tahti palette`() {
        val backdrop = source("HarfYoluBackdrop.kt")

        assertTrue(backdrop.contains("0xFF14B8B0"))
    }


    @Test
    fun `Harf Yolu keeps start four intermediate rows and target flow`() {
        val game = source("LetterLadderGame.kt")

        assertTrue(game.contains("for (move in 1 until LetterLadderEngine.MOVE_COUNT)"))
        // Harf Yolu redesign (PR #459): four moves per ladder.
        assertTrue(game.contains("const val MOVE_COUNT = 4"))
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
