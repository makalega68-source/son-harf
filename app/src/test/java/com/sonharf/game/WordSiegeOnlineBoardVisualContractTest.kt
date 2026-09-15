package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
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

    @Test fun onlineBoardKeepsChatInHeaderAndUsesTwoLevelBonusTypography() {
        val source = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()
        assertTrue(source.contains("onClick = onChat"))
        assertTrue(source.contains("enabled = game.playerTwoId != null"))
        assertTrue(source.contains("modifier = Modifier.size(40.dp)"))
        assertTrue(source.contains("Icons.Rounded.Chat"))
        assertFalse(source.contains("Modifier.align(Alignment.BottomEnd).padding(7.dp).size(42.dp)"))
        assertTrue(source.contains("PanSiegeBonusLabel = Color(0xFF68716D)"))
        assertTrue(source.contains("fontSize = if (overview) 8.5.sp else 10.sp"))
        assertTrue(source.contains("fontSize = if (overview) 14.sp else 16.sp"))
        assertTrue(source.contains("fontWeight = FontWeight.SemiBold"))
        assertTrue(source.contains("fontWeight = FontWeight.Black"))
        assertTrue(source.contains("letterSpacing = .12.sp"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
