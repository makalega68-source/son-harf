package com.sonharf.game

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeEntryHierarchyContractTest {
    @Test
    fun `standard siege stays visually primary while series and fair play remain reachable`() {
        val source = repoFile("app/src/main/java/com/sonharf/game/WordSiegeEntryScreen.kt").readText()

        assertTrue(source.contains("ANA REKABET MODU"))
        assertTrue(source.contains("MAIN COMPETITIVE MODE"))
        assertTrue(source.contains("featured = true"))
        assertTrue(source.contains("accent = GameColors.PlayGreen"))
        assertTrue(source.contains("mode = WordSiegeEntryMode.STANDARD"))
        assertTrue(source.contains("SERİ / HIZLI OYUN"))
        assertTrue(source.contains("mode = WordSiegeEntryMode.SERIES"))
        assertTrue(source.contains("Kelime puanın kalıcıdır"))
        assertTrue(source.contains("each cube is worth 2 points"))
        assertTrue(source.contains("ADİL REKABET"))
        assertTrue(source.contains("Purchased options never provide match power"))
        assertTrue(source.contains(".verticalScroll(rememberScrollState())"))
        assertTrue(source.contains(".weight(1f)"))
    }

    private fun repoFile(path: String): File = sequenceOf(File(path), File("../$path"))
        .firstOrNull { it.exists() }
        ?: error("Missing repository file: $path")
}
