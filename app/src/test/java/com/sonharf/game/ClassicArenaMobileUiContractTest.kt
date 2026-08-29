package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassicArenaMobileUiContractTest {

    @Test
    fun loginNoLongerUsesLegacyBitmapBackground() {
        val auth = projectFile("app/src/main/java/com/sonharf/game/RequiredAuthGate.kt").readText()

        assertFalse(auth.contains("son_harf_login_bg"))
        assertTrue(auth.contains("Brush.radialGradient"))
        assertTrue(auth.contains("Brush.verticalGradient"))
    }

    @Test
    fun classicArenaStaysSingleScreenAndShowsOnlyCurrentWord() {
        val arena = projectFile("app/src/main/java/com/sonharf/game/TargetNeonGameScreen.kt").readText()

        assertFalse(arena.contains(".verticalScroll(rememberScrollState())"))
        assertFalse(arena.contains("words.takeLast(3)"))
        assertTrue(arena.contains("val current = words.last()"))
        assertTrue(arena.contains("Modifier.fillMaxWidth().weight(1f, fill = true)"))
        assertTrue(arena.contains("Sıradaki kelime bununla başlar"))
    }

    @Test
    fun classicArenaKeepsFixedKeyboardAndPlayerPhotosInMatch() {
        val arena = projectFile("app/src/main/java/com/sonharf/game/TargetNeonGameScreen.kt").readText()

        assertTrue(arena.contains("EmbeddedWordKeyboard("))
        assertTrue(arena.contains("readOnly = true"))
        assertFalse(arena.contains("keyboard?.show()"))
        assertTrue(arena.contains("ProfilePhotoAvatarWithGender("))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(
            File(path),
            File("../$path"),
        )
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
