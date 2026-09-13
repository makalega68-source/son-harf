package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeOnlineBoardVisualContractTest {
    @Test fun onlineBoardRenders4KStarsAndPersistentLatestMoveCue() {
        val source = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()
        assertTrue(source.contains("PanSiegeBonus4K"))
        assertTrue(source.contains("PanSiegeBonusStar"))
        assertTrue(source.contains("WordSiegeBoardSpec.displayBonusLabel(activeBonus, !SonHarfUiState.isEnglish)"))
        assertTrue(source.contains("highlightAlpha.animateTo(0.42f"))
        assertTrue(source.contains("padding(horizontal = 6.dp, vertical = 4.dp)"))
    }

    @Test fun onlineBoardKeepsChatInsidePlayAreaAndUsesQuietBonusTypography() {
        val source = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()
        assertTrue(source.contains("onChat = onChat"))
        assertTrue(source.contains("Modifier.align(Alignment.BottomEnd).padding(7.dp).size(42.dp)"))
        assertTrue(source.contains("Icon(Icons.Rounded.Chat, sh(\"Oyun içi sohbet\", \"In-game chat\")"))
        assertTrue(source.contains("PanSiegeBonusLabel = Color(0xFF68716D)"))
        assertTrue(source.contains("SpanStyle(fontSize = 20.sp)"))
        assertTrue(source.contains("SpanStyle(fontSize = 10.sp)"))
        assertTrue(source.contains("fontWeight = FontWeight.Light"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
