package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MascotCompanionCoachTest {
    @Test
    fun onboardingStopsAfterThreeVerifiedMatches() {
        assertTrue(MascotCompanionCoach.onboardingHint(MascotVerifiedContext(), "tr")!!.contains("son harfi"))
        assertNull(MascotCompanionCoach.onboardingHint(MascotVerifiedContext(wins = 2, losses = 1), "tr"))
    }

    @Test
    fun verifiedSummaryUsesNeutralLeagueNames() {
        val summary = MascotVerifiedContext(
            wins = 12,
            losses = 4,
            rivalName = "Rakip\nInjected",
            rivalMatches = 5,
            rivalWins = 3,
            rivalLosses = 2,
            longestWord = "kelime<script>",
        ).safeSummary("tr")

        assertTrue(summary.contains("12 wins, 4 losses"))
        assertTrue(summary.contains("Gümüş"))
        assertTrue(summary.contains("Rakip Injected"))
        assertFalse(summary.contains("\n"))
        assertFalse(summary.contains("<script>"))
    }

    @Test
    fun fallbackHasNoLegacyStoryNames() {
        val reply = MascotCompanionCoach.localReply(
            character = LetharaLore.character(null),
            message = "merhaba",
            language = "tr",
            context = MascotVerifiedContext(wins = 5, losses = 2),
            historySize = 0,
        )
        assertTrue(reply.reply.contains("Tek iyi kelimeyle") || reply.reply.contains("Buradayım"))
        assertFalse(reply.reply.contains("Varkhor", ignoreCase = true))
        assertFalse(reply.reply.contains("Lyra", ignoreCase = true))
        assertFalse(reply.reply.contains("mühür", ignoreCase = true))
        assertEquals("", reply.memoryNote ?: "")
        assertTrue(reply.usedFallback)
    }

    @Test
    fun leagueThresholdsStayStable() {
        assertEquals("Bronz", MascotVerifiedContext(wins = 9).leagueName("tr"))
        assertEquals("Gümüş", MascotVerifiedContext(wins = 10).leagueName("tr"))
        assertEquals("Altın", MascotVerifiedContext(wins = 25).leagueName("tr"))
        assertEquals("Platin", MascotVerifiedContext(wins = 50).leagueName("tr"))
        assertEquals("Elmas", MascotVerifiedContext(wins = 100).leagueName("tr"))
        assertEquals("Usta", MascotVerifiedContext(wins = 200).leagueName("tr"))
    }
}
