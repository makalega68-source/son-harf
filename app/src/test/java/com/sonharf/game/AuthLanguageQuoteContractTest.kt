package com.sonharf.game

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthLanguageQuoteContractTest {

    @Test
    fun appLanguageDefaultsToTurkishAndPersistsSelection() {
        val preferences = projectFile("app/src/main/java/com/sonharf/game/SonHarfPreferences.kt").readText()
        val state = projectFile("app/src/main/java/com/sonharf/game/SonHarfUiState.kt").readText()

        assertTrue(preferences.contains("getString(LANGUAGE, \"tr\")"))
        assertTrue(preferences.contains("val normalized = if (value == \"en\") \"en\" else \"tr\""))
        assertTrue(preferences.contains("SonHarfUiState.language = normalized"))
        assertTrue(state.contains("mutableStateOf(\"tr\")"))
    }

    @Test
    fun authScreenOffersBothLanguagesAndLocalizedHandwrittenQuote() {
        val auth = projectFile("app/src/main/java/com/sonharf/game/RequiredAuthGate.kt").readText()

        assertTrue(auth.contains("AuthLanguageSelector("))
        assertTrue(auth.contains("\"tr\" to \"Türkçe\""))
        assertTrue(auth.contains("\"en\" to \"English\""))
        assertTrue(auth.contains("SonHarfPreferences.setLanguage(context, it)"))
        assertTrue(auth.contains("Zincir uzadıkça ustalık ortaya çıkar."))
        assertTrue(auth.contains("As the chain grows, mastery reveals itself."))
        assertTrue(auth.contains("FontFamily.Cursive"))
        assertTrue(auth.contains("FontStyle.Italic"))
    }

    @Test
    fun activeShellAndCompetitiveModesFollowGlobalLanguage() {
        val home = projectFile("app/src/main/java/com/sonharf/game/LightWordThemeApp.kt").readText()
        val classic = projectFile("app/src/main/java/com/sonharf/game/TargetNeonGameScreen.kt").readText()
        val trivia = projectFile("app/src/main/java/com/sonharf/game/BilBakalimExcitementScreen.kt").readText()
        val widgets = projectFile("app/src/main/java/com/sonharf/game/UnifiedCompetitionWidgets.kt").readText()
        val profile = projectFile("app/src/main/java/com/sonharf/game/FinalProfileScreen.kt").readText()

        assertTrue(home.contains("Keep the Word Going, Beat Your Rival"))
        assertTrue(home.contains("sh(\"Ana Sayfa\", \"Home\")"))
        assertTrue(classic.contains("SonHarfPreferences.setLanguage(context, next)"))
        assertTrue(classic.contains("sh(\"DÜELLO\", \"DUEL\")"))
        assertTrue(trivia.contains("if (SonHarfUiState.isEnglish) bilBakalimQuestionsEn else bilBakalimQuestions"))
        assertTrue(widgets.contains("TOP 3 CLUBS THIS WEEK"))
        assertTrue(profile.contains("sh(\"OYUNCU PROFİLİ\", \"PLAYER PROFILE\")"))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
