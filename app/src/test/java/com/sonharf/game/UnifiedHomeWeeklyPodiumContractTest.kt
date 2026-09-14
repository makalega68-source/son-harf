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
        assertTrue(source.contains("title = sh(\"SON HARF\", \"LAST LETTER\")"))
        assertFalse(source.contains("title = sh(\"PREMIER 1v1\", \"PREMIER 1v1\")"))
        assertFalse(source.contains("ARENANI SEÇ"))
        assertFalse(source.contains("20 saniyelik baskı"))

        // Weekly podium is intentionally removed from the home feed until its live data presentation is repaired.
        assertFalse(source.contains("WeeklyPodiumCardV210("))
        assertFalse(source.contains("backend.getLeaderboardV2(language, \"week\", 3)"))

        // Secondary modes remain reachable with clear hierarchy.
        assertTrue(source.contains("DİĞER OYUNLAR"))
        assertTrue(source.contains("son_harf_app_icon_master"))
        assertTrue(source.contains("harf_yolu_logo"))
        assertTrue(source.contains("onClick = onPlay"))
        assertTrue(source.contains("onClick = onLetter"))
        assertTrue(source.contains("UnifiedDestination.SIEGE -> WordSiegeExperienceScreen"))
    }
}
