package com.sonharf.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MascotCompanionCoachTest {
    @Test
    fun onboardingStopsAfterThreeVerifiedMatches() {
        assertTrue(MascotCompanionCoach.onboardingHint(MascotVerifiedContext(), "tr")!!.contains("İlk mühür"))
        assertTrue(
            MascotCompanionCoach.onboardingHint(
                MascotVerifiedContext(wins = 1, losses = 1),
                "tr",
            )!!.contains("Üçüncü mühür")
        )
        assertNull(
            MascotCompanionCoach.onboardingHint(
                MascotVerifiedContext(wins = 2, losses = 1),
                "tr",
            )
        )
    }

    @Test
    fun dailyQuestUsesVerifiedProgressInsteadOfRandomOnlySelection() {
        val first = MascotCompanionCoach.dailyQuest(MascotVerifiedContext(), "tr", 100)
        assertTrue(first.contains("İlk düellonu"))

        val lossHeavy = MascotCompanionCoach.dailyQuest(
            MascotVerifiedContext(wins = 3, losses = 8, friendshipLevel = 5),
            "tr",
            100,
        )
        assertTrue(lossHeavy.contains("güvenilir kısa kelimeleri"))
    }

    @Test
    fun verifiedSummarySanitizesNamesAndKeepsOnlyProvidedGameFacts() {
        val summary = MascotVerifiedContext(
            wins = 12,
            losses = 4,
            friendshipLevel = 6,
            memoryFragments = 21,
            rivalName = "Rakip\nInjected",
            rivalMatches = 5,
            rivalWins = 3,
            rivalLosses = 2,
            longestWord = "kelime<script>",
        ).safeSummary("tr")

        assertTrue(summary.contains("12 wins, 4 losses"))
        assertTrue(summary.contains("Gümüş Mührü"))
        assertTrue(summary.contains("Rakip Injected"))
        assertFalse(summary.contains("\n"))
        assertFalse(summary.contains("<script>"))
    }

    @Test
    fun sixSealFallbackVoicesStayDistinct() {
        val context = MascotVerifiedContext(wins = 5, losses = 2)
        val lyra = MascotCompanionCoach.localReply(
            LetharaLore.character("lyra"), "merhaba", "tr", context, 0
        ).reply
        val neris = MascotCompanionCoach.localReply(
            LetharaLore.character("neris"), "merhaba", "tr", context, 0
        ).reply
        val mivo = MascotCompanionCoach.localReply(
            LetharaLore.character("mivo"), "merhaba", "tr", context, 0
        ).reply

        assertNotEquals(lyra, neris)
        assertNotEquals(neris, mivo)
        assertTrue(lyra.contains("yıldız", ignoreCase = true))
        assertTrue(neris.contains("Gölge", ignoreCase = true) || neris.contains("gölge", ignoreCase = true))
        assertTrue(mivo.contains("Varkhor") || mivo.contains("rün", ignoreCase = true))
    }

    @Test
    fun rivalAdviceUsesOnlyVerifiedRivalRecord() {
        val reply = MascotCompanionCoach.localReply(
            character = LetharaLore.character("neris"),
            message = "Rakibim nasıl?",
            language = "tr",
            context = MascotVerifiedContext(
                wins = 9,
                losses = 7,
                rivalName = "MaviMühür",
                rivalMatches = 6,
                rivalWins = 4,
                rivalLosses = 2,
            ),
            historySize = 0,
        )
        assertTrue(reply.reply.contains("MaviMühür"))
        assertTrue(reply.reply.contains("6"))
        assertTrue(reply.reply.contains("4 galibiyet"))
        assertEquals("", reply.memoryNote)
        assertTrue(reply.usedFallback)
    }

    @Test
    fun leagueThresholdsMatchSealProgression() {
        assertEquals("Bronz Mührü", MascotVerifiedContext(wins = 9).leagueName("tr"))
        assertEquals("Gümüş Mührü", MascotVerifiedContext(wins = 10).leagueName("tr"))
        assertEquals("Altın Mührü", MascotVerifiedContext(wins = 25).leagueName("tr"))
        assertEquals("Platin Mührü", MascotVerifiedContext(wins = 50).leagueName("tr"))
        assertEquals("Elmas Mührü", MascotVerifiedContext(wins = 100).leagueName("tr"))
        assertEquals("Usta Mührü", MascotVerifiedContext(wins = 200).leagueName("tr"))
    }
}
