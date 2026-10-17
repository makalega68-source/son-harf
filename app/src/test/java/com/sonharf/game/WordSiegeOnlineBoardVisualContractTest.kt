package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeOnlineBoardVisualContractTest {
    @Test fun onlineBoardRenders4KStarsAndPersistentLatestMoveCue() {
        val source = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()
        assertTrue(source.contains("PanSiegeBonus4K"))
        assertTrue(source.contains("PanSiegeBonusStar"))
        assertTrue(source.contains("WordSiegeBoardSpec.displayBonusLabel(activeBonus, !SonHarfUiState.isEnglish)"))
        assertTrue(source.contains("highlightAlpha.animateTo(WORD_SIEGE_LAST_MOVE_REST_ALPHA"))
        assertTrue(source.contains("padding(horizontal = 6.dp, vertical = 4.dp)"))
    }

    @Test fun onlineBoardKeepsChatInsidePlayAreaAndUsesQuietBonusTypography() {
        val source = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()
        assertTrue(source.contains("onChat = onChat"))
        // Chat sits in the action row; nothing floats over the board.
        assertFalse(source.contains("Modifier.align(Alignment.BottomEnd).padding(7.dp).size(42.dp)"))
        assertTrue(source.contains("sh(\"SOHBET\", \"CHAT\")"))
        assertTrue(source.contains("sh(\"İPUCU (\$hintsLeft)\", \"HINT (\$hintsLeft)\")"))
        assertTrue(source.contains("WordSiegeWalnutIvory.bonusLabel else Color(0xFF3F4A5A)"))
        assertTrue(source.contains("WordSiegeBonusMark(activeBonus, label, overview, PanSiegeBonusLabel"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
