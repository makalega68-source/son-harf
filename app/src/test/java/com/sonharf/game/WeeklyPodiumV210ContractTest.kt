package com.sonharf.game

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyPodiumV210ContractTest {
    @Test
    fun podiumKeepsV210VisualContractAndWeeklyDataSource() {
        val podium = projectFile("app/src/main/java/com/sonharf/game/WeeklyPodiumV210.kt").readText()
        val shell = projectFile("app/src/main/java/com/sonharf/game/UnifiedProApp.kt").readText()

        assertTrue(podium.contains("player = players[1]"))
        assertTrue(podium.contains("player = players[0]"))
        assertTrue(podium.contains("player = players[2]"))
        assertTrue(podium.contains("slabHeight = 118.dp"))
        assertTrue(podium.contains("slabHeight = 162.dp"))
        assertTrue(podium.contains("slabHeight = 100.dp"))
        assertTrue(podium.contains("Modifier.weight(1.18f)"))
        assertTrue(podium.contains("elevation = 22.dp"))
        assertTrue(podium.contains("height(14.dp)"))
        assertTrue(podium.contains("val rnd = Random(7)"))
        assertTrue(podium.contains("if (players.size < 3) return"))
        assertTrue(shell.contains("getLeaderboardV2(language, \"week\", 3)"))
        assertTrue(shell.contains("WeeklyPodiumCardV210("))
    }

    private fun projectFile(path: String): File =
        listOf(File(path), File("../$path")).firstOrNull(File::exists)
            ?: error("Project path missing: $path")
}
