package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeResponsiveLayoutTest {
    @Test
    fun practiceGameUsesFixedResponsiveViewportInsteadOfScrollingGameSurface() {
        val source = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()

        assertTrue(source.contains("BoxWithConstraints"))
        assertTrue(source.contains("val compact = maxHeight < 780.dp"))
        assertTrue(source.contains("val chromeHeight = if (compact) 390.dp else 420.dp"))
        assertTrue(source.contains("val boardSize = minOf(maxWidth"))
        assertTrue(source.contains("Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()"))
        assertTrue(source.contains("WordSiegePracticeBoard("))
        assertTrue(source.contains("heightIn(min = 26.dp, max = 38.dp)"))
        assertTrue(source.contains("showPass = true"))
        assertTrue(source.contains("showExchange = true"))
        assertTrue(source.contains("onClick = ::applyPlayerMove"))
        assertFalse("Main match surface must not scroll", source.contains("LazyColumn"))
    }

    @Test
    fun practiceBoardOwnsSingleAndDoubleTapSoCellClickCannotStealFirstTap() {
        val source = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt").readText()

        assertTrue(source.contains("detectTapGestures("))
        assertTrue(source.contains("onDoubleTap = { toggleMode() }"))
        assertTrue(source.contains("onTap = { tap ->"))
        assertTrue(source.contains("val nextMode = mode.toggle()"))
        assertTrue(source.contains("val x = (tap.x - pan.x) / currentScale"))
        assertFalse(source.contains(".clickable(enabled = enabled && (cell.letter == null || pending)"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
