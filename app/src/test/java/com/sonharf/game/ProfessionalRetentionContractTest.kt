package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfessionalRetentionContractTest {
    @Test
    fun `home daily strip reaches professional missions and reward tabs without changing reward backend`() {
        val shell = repoFile("app/src/main/java/com/sonharf/game/ProfessionalUnifiedApp.kt").readText()
        val home = repoFile("app/src/main/java/com/sonharf/game/ProfessionalHomeScreen.kt").readText()
        val screen = repoFile("app/src/main/java/com/sonharf/game/ProfessionalRetentionScreen.kt").readText()

        assertTrue(shell.contains("ProfessionalDestination.RETENTION"))
        assertTrue(shell.contains("ProfessionalRetentionScreen("))
        assertTrue(home.contains("onRetention: () -> Unit"))
        assertTrue(home.contains("onClick = onRetention"))
        assertTrue(screen.contains("GÖREVLER"))
        assertTrue(screen.contains("GÜNLÜK ÖDÜL"))
        assertTrue(screen.contains("DailyStreakWeekStrip("))
        assertTrue(screen.contains("7 Günlük Seri"))
        assertTrue(screen.contains("val cycleDay = if (streak <= 0) 1 else ((streak - 1) % 7) + 1"))
        assertTrue(screen.contains("isCurrent -> GameColors.RewardAmber"))
        assertTrue(screen.contains("backend.getGrowthDashboard()"))
        assertTrue(screen.contains("backend.getUnifiedMissions()"))
        assertTrue(screen.contains("backend.claimDailyCheckin()"))
        assertTrue(screen.contains("backend.claimDailyChallenge()"))
        assertTrue(screen.contains("backend.claimUnifiedMission(mission.missionId)"))
        assertTrue(screen.contains("backend.claimGoal(goal.id)"))
        assertFalse(screen.contains("MainUi."))
        assertFalse(screen.contains("SonHarfTheme."))
    }

    private fun repoFile(path: String): File = sequenceOf(File(path), File("../$path"))
        .firstOrNull { it.exists() }
        ?: error("Missing repository file: $path")
}
