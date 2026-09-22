package com.sonharf.game

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeHudReadabilityContractTest {
    @Test
    fun `word siege hud keeps avatars map control and split score readable`() {
        val source = repoFile("app/src/main/java/com/sonharf/game/WordSiegeGameUi.kt").readText()

        assertTrue(source.contains("size = 40.dp"))
        assertTrue(source.contains("WordSiegeBoardSpec.CellCount"))
        assertTrue(source.contains("Harita %\$mapControl"))
        assertTrue(source.contains("Map \$mapControl%"))
        assertTrue(source.contains("Kelime Puanı"))
        assertTrue(source.contains("Bölge Puanı"))
    }

    private fun repoFile(path: String): File = sequenceOf(File(path), File("../$path"))
        .firstOrNull { it.exists() }
        ?: error("Missing repository file: $path")
}
