package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeResponsiveLayoutTest {
    @Test
    fun practiceGameUsesCompactFixedViewportAndGivesBoardRemainingHeight() {
        val source = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()

        assertTrue(source.contains("BoxWithConstraints"))
        assertTrue(source.contains("val compact = maxHeight < 700.dp"))
        assertTrue(source.contains(".statusBarsPadding()"))
        assertTrue(source.contains(".navigationBarsPadding()"))
        assertTrue(source.contains(".weight(1f)"))
        assertTrue(source.contains("WordSiegePracticeBoard("))
        assertTrue(source.contains("height(40.dp)"))
        assertTrue(source.contains("height(52.dp)"))
        assertTrue(source.contains("height(28.dp)"))
        assertTrue(source.contains("height(44.dp)"))
        assertTrue(source.contains("showPass = true"))
        assertTrue(source.contains("showExchange = true"))
        assertTrue(source.contains("onClick = ::applyPlayerMove"))
        assertFalse("Main match surface must not scroll", source.contains("LazyColumn"))
    }

    @Test
    fun onlineGameUsesSameCompactBoardBudgetAndSafeInsets() {
        val online = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()

        assertTrue(online.contains(".statusBarsPadding()"))
        assertTrue(online.contains(".navigationBarsPadding()"))
        assertTrue(online.contains("WordSiegeLiveRivalryBar("))
        assertTrue(online.contains("WordSiegeTempoBanner("))
        assertTrue(online.contains("WordSiegePracticeBoard("))
        assertTrue(online.contains("modifier = Modifier.fillMaxWidth().weight(1f)"))
        assertTrue(online.contains("modifier = Modifier.fillMaxWidth().height(44.dp)"))
        assertTrue(online.contains("lastMove != null -> PanSiegeLastMoveInfo(lastMove)"))
        assertFalse(online.contains("modifier = modifier.heightIn(min ="))
        assertFalse(online.contains("SÜRE YOK"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
