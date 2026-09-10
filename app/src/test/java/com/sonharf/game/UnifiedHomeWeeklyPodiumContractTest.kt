package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedHomeWeeklyPodiumContractTest {
    @Test
    fun homeUsesSinglePremierEntryAndShowsRealWeeklyTopThree() {
        val source = File("src/main/java/com/sonharf/game/UnifiedProApp.kt").readText()

        // Main PLAY remains the single Premier 1v1 entry point.
        assertTrue(source.contains("Premier 1v1 kelime düellosu"))
        assertFalse(source.contains("title = sh(\"PREMIER 1v1\", \"PREMIER 1v1\")"))
        assertFalse(source.contains("ARENANI SEÇ"))
        assertFalse(source.contains("20 saniyelik baskı"))

        // Home podium still uses the authoritative weekly top-three source.
        assertTrue(source.contains("backend.getLeaderboardV2(language, \"week\", 3)"))
        assertTrue(source.contains("WeeklyChampionPodium("))
        assertTrue(source.contains("HAFTANIN ZİRVESİ"))
        assertTrue(source.contains("WeeklyChampionHero(player = players.first()"))
        assertTrue(source.contains("players.getOrNull(1)"))
        assertTrue(source.contains("players.getOrNull(2)"))

        // Premium hierarchy: one champion hero, compact runners, and privacy-safe avatars.
        assertTrue(source.contains("private fun WeeklyChampionHero"))
        assertTrue(source.contains("private fun WeeklyRunnerCard"))
        assertTrue(source.contains("ProfilePhotoAvatarWithGender("))
        assertTrue(source.contains("HAFTA ŞAMPİYONU"))
        assertTrue(source.contains("Text(\"#1\""))
        assertTrue(source.contains("place = 2"))
        assertTrue(source.contains("place = 3"))
        assertTrue(source.contains("avatarVisibility == \"hidden\""))

        // Real empty/loading state replaces fabricated placeholder podium entries.
        assertTrue(source.contains("WeeklyPodiumEmptyState("))
        assertTrue(source.contains("players.isEmpty()"))

        // Other game modes remain reachable instead of being removed with the duplicate Premier card.
        assertTrue(source.contains("DİĞER OYUNLAR"))
        assertTrue(source.contains("KELİME KUŞATMASI"))
        assertTrue(source.contains("HARF YOLU"))
    }
}
