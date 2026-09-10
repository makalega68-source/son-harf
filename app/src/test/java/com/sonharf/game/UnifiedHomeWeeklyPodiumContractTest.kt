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

        // Home podium is populated from the existing authoritative weekly leaderboard RPC.
        assertTrue(source.contains("backend.getLeaderboardV2(language, \"week\", 3)"))
        assertTrue(source.contains("WeeklyChampionPodium("))
        assertTrue(source.contains("HAFTANIN ZİRVESİ"))
        assertTrue(source.contains("players.getOrNull(0)"))
        assertTrue(source.contains("players.getOrNull(1)"))
        assertTrue(source.contains("players.getOrNull(2)"))

        // Premium podium presentation uses rectangular photos and explicit gold/silver/bronze rank frames.
        assertTrue(source.contains("ProfilePhotoAvatarRectWithGender("))
        assertTrue(source.contains("1 -> Color(0xFFFFD25A)"))
        assertTrue(source.contains("2 -> Color(0xFFC7CED8)"))
        assertTrue(source.contains("else -> Color(0xFFC88758)"))
        assertTrue(source.contains("avatarVisibility == \"hidden\""))

        // Other game modes remain reachable instead of being removed with the duplicate Premier card.
        assertTrue(source.contains("DİĞER OYUNLAR"))
        assertTrue(source.contains("KELİME KUŞATMASI"))
        assertTrue(source.contains("HARF YOLU"))
    }
}
