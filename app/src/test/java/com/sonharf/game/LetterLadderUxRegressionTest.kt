package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LetterLadderUxRegressionTest {
    @Test
    fun harfYoluStaysFixedAlignedAndUsesReliableHinting() {
        val source = projectFile("app/src/main/java/com/sonharf/game/LetterLadderGame.kt").readText()

        assertFalse(source.contains("verticalScroll("))
        assertFalse(source.contains("rememberScrollState"))
        assertFalse(source.contains("number: Int"))
        assertTrue(source.contains("completionPath("))
        assertTrue(source.contains("Bu hamle çıkmaza götürüyor"))
        assertTrue(source.contains("compact = true"))
        assertTrue(source.contains("keySound = { SonHarfSoundFx.puzzleKey() }"))
    }

    @Test
    fun harfYoluUsesQuietDedicatedFeedbackWithoutChangingOtherKeyboardDefaults() {
        val keyboard = projectFile("app/src/main/java/com/sonharf/game/EmbeddedGameKeyboard.kt").readText()
        val sound = projectFile("app/src/main/java/com/sonharf/game/SonHarfSoundFx.kt").readText()

        assertTrue(keyboard.contains("keySound: () -> Unit = { SonHarfSoundFx.typingClick() }"))
        assertTrue(keyboard.contains("actionSound: () -> Unit = { SonHarfSoundFx.tap() }"))
        assertTrue(sound.contains("fun puzzleKey()"))
        assertTrue(sound.contains("fun puzzleError()"))
        assertTrue(sound.contains("fun puzzleHint()"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
