package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyPodiumV210ContractTest {
    @Test
    fun weeklyPodiumIsNotRenderedFromTheHomeFeedUntilItIsRepaired() {
        val shell = projectFile("app/src/main/java/com/sonharf/game/UnifiedProApp.kt").readText()

        assertFalse(shell.contains("WeeklyPodiumCardV210("))
        assertFalse(shell.contains("getLeaderboardV2(language, \\"week\\", 3)"))
        assertTrue(shell.contains("PremiumPlayButton(onClick = onSiege)"))
        assertFalse(shell.contains("DİĞER OYUNLAR"))
    }

    private fun projectFile(path: String): File =
        listOf(File(path), File("../$path")).firstOrNull(File::exists)
            ?: error("Project path missing: $path")
}
