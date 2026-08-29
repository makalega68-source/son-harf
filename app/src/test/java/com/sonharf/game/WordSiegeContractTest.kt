package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeContractTest {
    @Test fun siegeReplacesConquestAndKeepsCoreVisuals() {
        val siege = projectFile("app/src/main/java/com/sonharf/game/WordSiegeGame.kt").readText()
        val home = projectFile("app/src/main/java/com/sonharf/game/LightWordThemeApp.kt").readText()

        assertTrue(siege.contains("KELİME KUŞATMASI"))
        assertTrue(siege.contains("castle_blue"))
        assertTrue(siege.contains("castle_red"))
        assertTrue(siege.contains("ProfilePhotoAvatar("))
        assertTrue(siege.contains("KRİTİK!"))
        assertTrue(siege.contains("KELİMEYİ GÖNDER"))
        assertTrue(siege.contains("BOT"))
        assertTrue(home.contains("KELİME KUŞATMASI"))
        assertFalse(home.contains("KELİME FETHİ"))
        assertFalse(projectFile("app/src/main/java/com/sonharf/game").resolve("WordConquestGame.kt").exists())
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::exists)
        assertNotNull("Project path missing: $path", file)
        return requireNotNull(file)
    }
}
