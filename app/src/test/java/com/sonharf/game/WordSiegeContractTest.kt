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
        assertTrue(siege.contains("Kelimeyi kur. Alanı ele geçir."))
        assertTrue(siege.contains("initialSiegeTerritory"))
        assertTrue(siege.contains("claimSiegeTerritory"))
        assertTrue(siege.contains("SiegeBonus.TREASURE"))
        assertTrue(siege.contains("SiegeBonus.BRIDGE"))
        assertTrue(siege.contains("SiegeBonus.FOG"))
        assertTrue(siege.contains("SiegeBonus.DOUBLE"))
        assertTrue(siege.contains("SiegeBonus.CASTLE"))
        assertTrue(siege.contains("siege_treasure"))
        assertTrue(siege.contains("siege_bridge"))
        assertTrue(siege.contains("siege_fog"))
        assertTrue(siege.contains("siege_castle_neutral"))
        assertTrue(siege.contains("kare ele geçirildi"))
        assertTrue(siege.contains("rememberInfiniteTransition"))
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
