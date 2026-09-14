package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedHomeWeeklyPodiumContractTest {
    @Test
    fun homeKeepsKelimeKusatmasiPrimaryEntryWithoutWeeklyPodium() {
        val source = File("src/main/java/com/sonharf/game/UnifiedProApp.kt").readText()

        // Kelime Kuşatması is the primary play entry; Son Harf remains a secondary quick mode.
        assertTrue(source.contains("PremiumPlayButton(onClick = onSiege)"))
        assertTrue(source.contains("KELİME KUŞATMASI"))
        assertTrue(source.contains("SAVAŞA GİR"))
        assertFalse(source.contains("title = sh(\"PREMIER 1v1\", \"PREMIER 1v1\")"))
        assertFalse(source.contains("ARENANI SEÇ"))
        assertFalse(source.contains("20 saniyelik baskı"))

        // Weekly podium is intentionally removed from the home feed until its live data presentation is repaired.
        assertFalse(source.contains("WeeklyPodiumCardV210("))
        assertFalse(source.contains("backend.getLeaderboardV2(language, \"week\", 3)"))

        // The home lobby deliberately avoids duplicate mode-navigation cards.
        assertFalse(source.contains("DİĞER OYUNLAR"))
        assertTrue(source.contains("DailyObjectiveCard(onClick = onTasks)"))
        assertTrue(source.contains("UnifiedDestination.SIEGE -> WordSiegeExperienceScreen"))
    }
}
