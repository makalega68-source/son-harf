package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleKelimeTahtiRestoreContractTest {
    @Test fun simplifiedGameChromeAndCubeOwnershipColorsStayRestored() {
        val online = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()
        val practice = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()
        val board = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt").readText()
        val spec = projectFile("app/src/main/java/com/sonharf/game/WordSiegeBoardSpec.kt").readText()
        val logo = projectFile("app/src/main/java/com/sonharf/game/SonHarfOfficialLogo.kt").readText()

        assertFalse(online.contains("PanSiegeMapControl("))
        assertFalse(practice.contains("HARİTA KONTROLÜ"))
        assertTrue(online.contains("modifier = Modifier.weight(1f).height(52.dp)"))
        assertTrue(spec.contains("Harf\\n×2"))
        assertTrue(spec.contains("Kelime\\n×2"))

        assertTrue(online.contains("PanSiegeMine = Color(0xFFA8D5B5)"))
        assertTrue(online.contains("PanSiegeRival = Color(0xFFE4AEAA)"))
        assertTrue(board.contains("PracticeSiegeMine = Color(0xFFA8D5B5)"))
        assertTrue(board.contains("PracticeSiegeRival = Color(0xFFE4AEAA)"))
        assertTrue(board.contains("if (mode == WordSiegeBoardViewportMode.CLOSE)"))
        assertFalse(board.contains("WordSiegeBoardViewportMode.CLOSE && !enabled"))

        assertTrue(logo.contains("R.drawable.kelime_tahti_logo_latest"))
        assertTrue(projectFile("app/src/main/res/drawable-nodpi/kelime_tahti_logo_latest.png").isFile)
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        return candidates.firstOrNull(File::exists) ?: error("Missing project file: $path")
    }
}
