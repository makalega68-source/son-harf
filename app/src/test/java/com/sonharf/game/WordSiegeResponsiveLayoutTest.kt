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
        assertTrue(source.contains("val compact = maxHeight < 700.dp"))
        assertTrue(source.contains("modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp)"))
        assertTrue(source.contains("Box(Modifier.fillMaxWidth().weight(1f)"))
        assertTrue(source.contains("WordSiegePracticeBoard("))
        assertTrue(source.contains("showPass = true"))
        assertTrue(source.contains("showExchange = true"))
        assertTrue(source.contains("onClick = ::applyPlayerMove"))
        assertTrue(source.contains("HAMLEYİ ONAYLA"))
        assertFalse("Main match surface must not scroll", source.contains("LazyColumn"))
    }

    @Test
    fun practiceChromeUsesShellInsetsAndKeepsTacticalHudAndFooterAligned() {
        val practice = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()
        val online = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()
        val shell = projectFile("app/src/main/java/com/sonharf/game/UnifiedProApp.kt").readText()

        assertFalse(practice.contains(".statusBarsPadding().navigationBarsPadding()"))
        assertTrue(shell.contains("topBar = { SonHarfTopAdBanner(isPremium = isPro) }"))
        assertTrue(practice.contains("modifier = Modifier.fillMaxWidth().height(if (compact) 46.dp else 52.dp)"))
        assertTrue(practice.contains("PracticeMapControlBar("))
        assertTrue(practice.contains("PracticeStrategicZoneLegend("))
        assertTrue(practice.contains("if (notice != null || lastMove != null)"))
        assertTrue(practice.indexOf("Box(Modifier.fillMaxWidth().weight(1f)") < practice.indexOf("notice?.let { message ->"))
        assertTrue(practice.indexOf("onClick = ::applyPlayerMove") < practice.indexOf("notice?.let { message ->"))

        assertTrue(online.contains(".statusBarsPadding()\n            .navigationBarsPadding()"))
        assertTrue(online.contains("modifier = Modifier.fillMaxWidth().weight(1f)"))
        assertTrue(online.indexOf("lastMove?.let { PanSiegeLastMoveInfo(it) }") > online.indexOf("onClick = onSubmit"))
        assertFalse(online.contains("modifier = modifier.heightIn(min ="))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
