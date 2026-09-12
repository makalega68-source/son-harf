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

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
