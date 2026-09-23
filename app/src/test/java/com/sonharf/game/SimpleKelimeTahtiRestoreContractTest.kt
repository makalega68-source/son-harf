package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleKelimeKusatmasiRestoreContractTest {
    @Test fun simplifiedGameChromeAndCubeOwnershipColorsStayRestored() {
        val online = projectFile("app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt").readText()
        val practice = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()
        val board = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt").readText()
        val spec = projectFile("app/src/main/java/com/sonharf/game/WordSiegeBoardSpec.kt").readText()
        val logo = projectFile("app/src/main/java/com/sonharf/game/SonHarfOfficialLogo.kt").readText()

        assertFalse(online.contains("PanSiegeMapControl("))
        assertFalse(practice.contains("HARİTA KONTROLÜ"))
        assertTrue(online.contains("modifier = Modifier.weight(1f).height(40.dp)"))
        assertTrue(spec.contains("Harf\\n×2"))
        assertTrue(spec.contains("Kelime\\n×2"))

        assertTrue(online.contains("PanSiegeMine = Color(0xFF8BD8AA)"))
        assertTrue(online.contains("PanSiegeRival = Color(0xFFEDA09B)"))
        assertTrue(board.contains("PracticeSiegeMine = Color(0xFF8BD8AA)"))
        assertTrue(board.contains("PracticeSiegeRival = Color(0xFFEDA09B)"))
        assertTrue(board.contains("if (mode == WordSiegeBoardViewportMode.CLOSE)"))
        assertFalse(board.contains("WordSiegeBoardViewportMode.CLOSE && !enabled"))

        assertTrue(logo.contains("R.drawable.kelime_kusatma_logo_hd"))
        assertTrue(projectFile("app/src/main/res/drawable/kelime_kusatma_logo_hd.xml").isFile)
        assertFalse(File("app/src/main/res/drawable/kelime_kusatma_logo_hd.png").exists())
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        return candidates.firstOrNull(File::exists) ?: error("Missing project file: $path")
    }
}
