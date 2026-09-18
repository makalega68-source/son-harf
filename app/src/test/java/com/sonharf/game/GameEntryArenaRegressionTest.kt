package com.sonharf.game

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEntryArenaRegressionTest {
    @Test
    fun homeRoutesThroughArenaCenterBeforeStartingGames() {
        val source = File("src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()

        assertTrue(source.contains("onPrimary = { destination = PremiumDestination.GAMES }"))
        assertTrue(source.contains("onLastLetter = { destination = PremiumDestination.GAMES }"))
        assertTrue(source.contains("onLetterPath = { destination = PremiumDestination.GAMES }"))
        assertTrue(source.contains("ARENA MERKEZİ"))
        assertTrue(source.contains("OYUNUNU VE OYUN DİLİNİ SEÇ"))
    }

    @Test
    fun everyGameCardOffersExplicitTurkishAndEnglishChoices() {
        val source = File("src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()

        assertTrue(source.contains("TR  TÜRKÇE"))
        assertTrue(source.contains("EN  ENGLISH"))
        assertTrue(source.contains("OYUN DİLİ"))
        assertTrue(source.contains("onSiegeLanguage = { siegeLanguage = it }"))
        assertTrue(source.contains("onLastLetterLanguage = { lastLetterLanguage = it }"))
        assertTrue(source.contains("openGame(PremiumDestination.SIEGE, siegeLanguage)"))
        assertTrue(source.contains("openGame(PremiumDestination.LAST_LETTER, lastLetterLanguage)"))
    }

    @Test
    fun entryCardsExposeRealGameRulesInsteadOfGenericMenuCopy() {
        val source = File("src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()

        assertTrue(source.contains("KELİME + BÖLGE PUANI"))
        assertTrue(source.contains("ALAN ELE GEÇİRME"))
        assertTrue(source.contains("10 + 10 KELİME / ROUND"))
        assertTrue(source.contains("15 → 13 → 11"))
        assertTrue(source.contains("REKABET GÜÇ SATIN ALMAZ"))
    }
}
