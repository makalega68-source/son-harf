package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedHomeWeeklyPodiumContractTest {
    @Test
    fun homeUsesSinglePremierEntryAndPremiumRealWeeklyPodium() {
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
        assertTrue(source.contains("players.getOrNull(0)"))
        assertTrue(source.contains("players.getOrNull(1)"))
        assertTrue(source.contains("players.getOrNull(2)"))

        // Approved premium hierarchy: center champion, two runners, glow/confetti and privacy-safe avatars.
        assertTrue(source.contains("private fun PodiumColumn"))
        assertTrue(source.contains("private fun PodiumAmbientDecor"))
        assertTrue(source.contains("ProfilePhotoAvatarWithGender("))
        assertTrue(source.contains("ŞAMPİYON"))
        assertTrue(source.contains("place = 1"))
        assertTrue(source.contains("place = 2"))
        assertTrue(source.contains("place = 3"))
        assertTrue(source.contains("avatarVisibility == \"hidden\""))

        // No fabricated player names are injected when the backend has no weekly rows.
        assertTrue(source.contains("players.isEmpty()"))
        assertTrue(source.contains("player = players.getOrNull(0)"))
        assertTrue(source.contains("player = players.getOrNull(1)"))
        assertTrue(source.contains("player = players.getOrNull(2)"))

        // Other game modes remain reachable and use their actual branded artwork.
        assertTrue(source.contains("DİĞER OYUNLAR"))
        assertTrue(source.contains("kelime_kusatma_logo_hd"))
        assertTrue(source.contains("harf_yolu_logo"))
        assertTrue(source.contains("onClick = onSiege"))
        assertTrue(source.contains("onClick = onLetter"))
    }
}
