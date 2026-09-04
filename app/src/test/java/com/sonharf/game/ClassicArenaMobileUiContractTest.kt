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
    fun activeArenaStaysSingleScreenAndKeepsTheWholeWordChain() {
        val arena = projectFile("app/src/main/java/com/sonharf/game/PremiumDuelArena.kt").readText()

        assertFalse(arena.contains(".verticalScroll(rememberScrollState())"))
        assertFalse(arena.contains("words.takeLast("))
        assertTrue(arena.contains("items(items = words, key = { it.id })"))
        assertTrue(arena.contains("PremiumMatchWordHistory("))
        assertTrue(arena.contains("KELİME ZİNCİRİ"))
    }

    @Test
    fun activeArenaKeepsFixedKeyboardSymmetricPhotosAndVisibleChat() {
        val arena = projectFile("app/src/main/java/com/sonharf/game/PremiumDuelArena.kt").readText()

        assertTrue(arena.contains("PremiumAndroidGameKeyboard("))
        assertFalse(arena.contains("LocalSoftwareKeyboardController"))
        assertTrue(arena.contains("ProfilePhotoAvatarRectWithGender("))
        assertTrue(arena.contains("ChatBubbleOutline"))
        assertTrue(arena.contains("SOHBET"))
        assertTrue(arena.contains("MoreVert"))
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
