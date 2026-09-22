package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfessionalLeaderboardContractTest {
    @Test
    fun `leaderboard uses season backend top three podium player highlight and shell route`() {
        val screen = repoFile("app/src/main/java/com/sonharf/game/ProfessionalLeaderboardScreen.kt").readText()
        val entry = repoFile("app/src/main/java/com/sonharf/game/LeaderboardExperience.kt").readText()
        val shell = repoFile("app/src/main/java/com/sonharf/game/ProfessionalUnifiedApp.kt").readText()

        assertTrue(entry.contains("ProfessionalLeaderboardScreen"))
        assertTrue(screen.contains("backend.getCompetitiveSeason()"))
        assertTrue(screen.contains("backend.getSeasonLeaderboard(50)"))
        assertTrue(screen.contains("ProfessionalPodium("))
        assertTrue(screen.contains("rows.drop(3)"))
        assertTrue(screen.contains("row.userId == me"))
        assertTrue(screen.contains("Liderlik Tablosu"))
        assertTrue(screen.contains("HAFTALIK KUPA VE RAKİPLER"))
        assertTrue(shell.contains("ProfessionalDestination.LEADERBOARD"))
        assertTrue(shell.contains("onLeague = { destination = ProfessionalDestination.LEADERBOARD }"))
        assertTrue(shell.contains("onCompetition = { destination = ProfessionalDestination.COMPETE }"))
        assertFalse(screen.contains("MainUi."))
        assertFalse(screen.contains("SonHarfTheme."))
    }

    private fun repoFile(path: String): File = sequenceOf(File(path), File("../$path"))
        .firstOrNull { it.exists() }
        ?: error("Missing repository file: $path")
}
