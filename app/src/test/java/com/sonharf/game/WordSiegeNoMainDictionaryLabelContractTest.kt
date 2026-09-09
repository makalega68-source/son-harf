package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class WordSiegeNoMainDictionaryLabelContractTest {
    @Test fun practiceHeaderDoesNotExposeMainDictionaryLabel() {
        val source = projectFile("app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()
        assertFalse(source.contains("ANA SÖZLÜK"))
        assertFalse(source.contains("MAIN DICTIONARY"))
    }

    private fun projectFile(path: String): File {
        val file = listOf(File(path), File("../$path")).firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
