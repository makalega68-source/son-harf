package com.sonharf.game

import java.io.File
import org.junit.Assert.assertEquals
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
    fun activeClassicArenaUsesVerifiedRuntimePathAndSingleScreenHierarchy() {
        val online = projectFile("app/src/main/java/com/sonharf/game/OnlineGameScreenV6.kt").readText()
        val arena = projectFile("app/src/main/java/com/sonharf/game/LightDuelUi.kt").readText()

        assertTrue(online.contains("LightDuelArena("))
        assertTrue(arena.contains("CompactDuelHud("))
        assertTrue(arena.contains("DuelStatusLine("))
        assertTrue(arena.contains("DuelWordStage("))
        assertTrue(arena.contains("MatchWordHistory("))
        assertTrue(arena.contains("DuelGameKeyboard("))
        assertTrue(arena.contains("BoxWithConstraints("))
        assertTrue(arena.contains("WindowInsets.safeDrawing"))
        assertFalse(arena.contains(".verticalScroll("))
        assertFalse(arena.contains("imePadding()"))
    }

    @Test
    fun activeClassicArenaKeepsAllWordsAndFixedKeyboard() {
        val arena = projectFile("app/src/main/java/com/sonharf/game/LightDuelUi.kt").readText()
        val liveSection = arena.substring(arena.indexOf("internal fun LightDuelArena"), arena.indexOf("private fun CompetitiveResult"))

        assertFalse(liveSection.contains("takeLast("))
        assertTrue(liveSection.contains("LazyRow("))
        assertTrue(liveSection.contains("items(items = words, key = { it.id })"))
        assertTrue(liveSection.contains("animateScrollToItem(words.lastIndex)"))
        assertTrue(liveSection.contains("listOf(\"Q\", \"W\", \"E\", \"R\", \"T\", \"Y\", \"U\", \"I\", \"O\", \"P\", \"Ğ\", \"Ü\")"))
        assertTrue(liveSection.contains("sh(\"SİL\", \"DELETE\")"))
        assertTrue(liveSection.contains("sh(\"TEMİZLE\", \"CLEAR\")"))
        assertEquals(1, Regex("sh\\(\\\"GÖNDER\\\", \\\"SEND\\\"\\)").findAll(liveSection).count())
        assertFalse(liveSection.contains("➤"))
    }

    @Test
    fun activeClassicArenaShowsBothPlayerPhotosAndNoLiveAd() {
        val arena = projectFile("app/src/main/java/com/sonharf/game/LightDuelUi.kt").readText()
        val liveSection = arena.substring(arena.indexOf("internal fun LightDuelArena"), arena.indexOf("private fun CompetitiveResult"))

        assertTrue(liveSection.contains("ProfilePhotoAvatarRectWithGender("))
        assertTrue(liveSection.contains("SyntheticBotPortrait("))
        assertFalse(liveSection.contains("NonGameBannerAd"))
        assertFalse(liveSection.contains("Rewarded"))
        assertFalse(liveSection.contains("AdView"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
