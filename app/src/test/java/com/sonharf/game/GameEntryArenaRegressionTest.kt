package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEntryArenaRegressionTest {
    @Test
    fun everyGameHasItsOwnIndependentEntryDestination() {
        val shell = File("src/main/java/com/sonharf/game/PremiumUnifiedProApp.kt").readText()
        val entries = File("src/main/java/com/sonharf/game/PremiumGameEntryScreens.kt").readText()

        assertTrue(shell.contains("SIEGE_ENTRY, LAST_LETTER_ENTRY, LETTER_PATH_ENTRY"))
        assertTrue(shell.contains("onPrimary = { destination = PremiumDestination.SIEGE_ENTRY }"))
        assertTrue(shell.contains("onLastLetter = { destination = PremiumDestination.LAST_LETTER_ENTRY }"))
        assertTrue(shell.contains("onLetterPath = { destination = PremiumDestination.LETTER_PATH_ENTRY }"))
        assertTrue(entries.contains("PremiumSiegeEntryScreen"))
        assertTrue(entries.contains("PremiumLastLetterEntryScreen"))
        assertTrue(entries.contains("PremiumLetterPathEntryScreen"))
        assertFalse(shell.contains("PremiumGameCenter("))
        assertFalse(entries.contains("ARENA MERKEZİ"))
    }

    @Test
    fun eachEntryOffersTurkishAndEnglishWithoutLogoArtwork() {
        val entries = File("src/main/java/com/sonharf/game/PremiumGameEntryScreens.kt").readText()

        assertTrue(entries.contains("LanguageOption(\"tr\""))
        assertTrue(entries.contains("LanguageOption(\"en\""))
        assertTrue(entries.contains("TÜRKÇE DÜELLOYU BAŞLAT"))
        assertTrue(entries.contains("TÜRKÇE KUŞATMAYA GİR"))
        assertTrue(entries.contains("TÜRKÇE ROTAYI BAŞLAT"))
        assertFalse(entries.contains("painterResource"))
        assertFalse(entries.contains("R.drawable"))
    }

    @Test
    fun entriesExposeActualGameIdentityAndRules() {
        val entries = File("src/main/java/com/sonharf/game/PremiumGameEntryScreens.kt").readText()

        assertTrue(entries.contains("PremiumSiegeEntryScreen"))
        assertTrue(entries.contains("LastLetterRuleCard(\"10 + 10\""))
        assertTrue(entries.contains("LastLetterRuleCard(\"15→13→11\""))
        assertTrue(entries.contains("LetterPathPreview(language)"))
        assertTrue(entries.contains("CAPTURE THE MAP WITH WORDS"))
    }
}
