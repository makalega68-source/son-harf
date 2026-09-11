package com.sonharf.game

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class SiegeDictionaryThemeRegressionTest {
    private fun appSource(name: String): String = File("src/main/java/com/sonharf/game/$name").readText()
    private fun dataSource(name: String): String = File("src/main/java/com/sonharf/game/data/$name").readText()
    private fun repoFile(path: String): File = sequenceOf(File(path), File("../$path"))
        .firstOrNull { it.exists() }
        ?: error("Missing repository file: $path")

    @Test fun phonePracticeLayoutPrioritizesTheFifteenByFifteenBoard() {
        val screen = appSource("WordSiegePracticeScreen.kt")
        val guidance = appSource("WordSiegePracticeGuidance.kt")
        val board = appSource("WordSiegeBoardSpec.kt")

        assertTrue(board.contains("const val Size = 15"))
        assertTrue(screen.contains("maxHeight < 700.dp || maxWidth < 600.dp"))
        assertTrue(screen.contains("height(if (compact) 46.dp else 52.dp)"))
        assertTrue(screen.contains("WordSiegePracticeStatusBar(statusMessage, compact)"))
        assertTrue(guidance.contains("maxLines = 2"))
    }

    @Test fun persistedDictionaryIsRefreshedAndUnicodeNormalizationMatchesServer() {
        val screen = appSource("WordSiegePracticeScreen.kt")
        val dictionary = dataSource("SharedDictionaryService.kt")

        assertTrue(screen.contains("SharedDictionaryService.preloadCanonical(context, state.language)"))
        assertTrue(screen.contains("dictionaryReady = restored"))
        assertTrue(dictionary.contains("Normalizer.Form.NFC"))
    }

    @Test fun canonicalSnapshotCoversFullWordSiegeBoardLength() {
        val migration = repoFile("supabase/migrations/20260908114500_dictionary_board_parity_and_default_theme.sql").readText()
        assertTrue(migration.contains("char_length(d.normalized_word) between 2 and 15"))
    }

    @Test fun profileAlwaysOffersBuiltInBlueWhiteThemeWithoutStorePurchase() {
        val profile = appSource("MainPlayerProfileScreen.kt")
        val themes = appSource("ProfileOwnedThemesSection.kt")
        val economy = dataSource("EconomyStore.kt")

        assertTrue(profile.contains("ProfileOwnedThemesSection(backend)"))
        assertTrue(themes.contains("Ana Mavi Beyaz"))
        assertTrue(themes.contains("Son Harf ana teması • Ücretsiz"))
        assertTrue(themes.contains("backend.equipDefaultGameTheme()"))
        assertTrue(economy.contains("equip_default_game_theme"))
    }
}