package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The game has no level system: competitive progress is league, rating, streaks and match results. */
class NoLevelSystemContractTest {
    @Test
    fun `professional shell screens show no level or XP progress`() {
        listOf(
            "ProfessionalProfileScreen.kt",
            "ProfessionalRetentionScreen.kt",
            "ProfessionalHomeScreen.kt",
        ).forEach { name ->
            val screen = source(name)
            assertFalse("$name shows a level", screen.contains("Seviye"))
            assertFalse("$name shows XP progress", screen.contains("XPProgress("))
        }
        val profile = source("ProfessionalProfileScreen.kt")
        assertTrue(profile.contains("LeagueProgress(league.progress)"))
    }

    private fun source(name: String): String =
        sequenceOf(File("src/main/java/com/sonharf/game/$name"), File("app/src/main/java/com/sonharf/game/$name"))
            .firstOrNull { it.exists() }
            ?.readText()
            ?: error("Missing source file: $name")
}
