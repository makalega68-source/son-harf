package com.sonharf.game

import java.io.File
import org.junit.Test
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class DictionaryLanguageHelpContractTest {
    private fun source(path: String) = File(path).readText()

    @Test fun wordSiegeLanguageSelectorIsIndependentFromUiLocale() {
        val s = source("src/main/java/com/sonharf/game/WordSiegeExperience.kt")
        assertTrue(s.contains("🇹🇷 TÜRKÇE"))
        assertTrue(s.contains("🇬🇧 ENGLISH"))
        assertTrue(s.contains("findOrCreateWordSiegeGame(selectedMatchLanguage, hours)"))
        assertFalse(s.contains("findOrCreateWordSiegeGame(if (SonHarfUiState.isEnglish)"))
    }

    @Test fun practiceBagAndBotFollowMatchLanguage() {
        val engine = source("src/main/java/com/sonharf/game/WordSiegePracticeEngine.kt")
        val planner = source("src/main/java/com/sonharf/game/WordSiegeBotPlanner.kt")
        assertTrue(engine.contains("englishTileDistribution"))
        assertTrue(engine.contains("language = normalizedLanguage"))
        assertTrue(engine.contains("tileValue(it, state.language)"))
        assertTrue(planner.contains("validateWordSiegeDictionaryWords(it, state.language)"))
        assertTrue(planner.contains("fetchWordSiegeBotLexicon(rack, state.language"))
    }

    @Test fun helpVisibilityUsesGameRouteAllowlist() {
        val app = source("src/main/java/com/sonharf/game/MainExperienceApp.kt")
        val shell = source("src/main/java/com/sonharf/game/StableV1App.kt")
        assertTrue(app.contains("MainDestination.GAME -> FirstPlayerTutorialKind.SON_HARF"))
        assertTrue(app.contains("MainDestination.WORD_SIEGE -> FirstPlayerTutorialKind.WORD_SIEGE"))
        assertTrue(app.contains("else -> null"))
        assertTrue(shell.contains("tutorial == null && gameHelpKind != null"))
        assertTrue(shell.contains("Alignment.TopEnd"))
        assertTrue(shell.contains("statusBarsPadding()"))
    }

    @Test fun dictionaryMigrationIsFailClosedAndGameSpecific() {
        val sql = source("../supabase/migrations/20260901074000_dictionary_validation_language_v1.sql")
        assertTrue(sql.contains("validate_dictionary_word_v1"))
        assertTrue(sql.contains("abbreviation_not_allowed"))
        assertTrue(sql.contains("proper_noun_not_allowed"))
        assertTrue(sql.contains("legacy_synthetic_two_letter_fallback"))
        assertTrue(sql.contains("right(v_check.normalized_word, 1) = 'ğ'"))
        assertTrue(sql.contains("word_siege_word_allowed_v1"))
    }
}
