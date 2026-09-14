package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyPodiumV210BackendContractTest {
    @Test
    fun `weekly podium uses current-week XP as RP`() {
        val migration = projectFile("supabase/migrations/20260912143000_weekly_podium_v210.sql").readText()
        val data = projectFile("app/src/main/java/com/sonharf/game/data/WeeklyPodiumV210Data.kt").readText()
        val leaderboard = projectFile("app/src/main/java/com/sonharf/game/data/LeaderboardV2.kt").readText()

        assertTrue(migration.contains("create or replace view public.weekly_rp"))
        assertTrue(migration.contains("date_trunc('week', now())"))
        assertTrue(migration.contains("then 120 else 35"))
        assertTrue(migration.contains("coalesce(w.words, 0) * 3"))
        assertTrue(migration.contains("p.rounds * 5"))
        assertTrue(migration.contains("create or replace function public.weekly_top"))
        assertTrue(migration.contains("create or replace function public.my_weekly_rp"))
        assertTrue(migration.contains("p.display_name"))
        assertFalse(migration.lowercase().contains("from public.match_history"))
        assertFalse(migration.contains("grant execute on function public.weekly_top(integer) to anon"))
        assertFalse(migration.contains("grant execute on function public.my_weekly_rp() to anon"))

        assertTrue(data.contains("weekly_top"))
        assertTrue(data.contains("my_weekly_rp"))
        assertTrue(leaderboard.contains("period.lowercase() == \"week\""))
        assertTrue(leaderboard.contains("getWeeklyTopV210(limit)"))
        assertTrue(leaderboard.contains("rating = row.rp"))
    }

    @Test
    fun `weekly league shows RP and Monday reset copy`() {
        val entry = projectFile("app/src/main/java/com/sonharf/game/LeaderboardExperience.kt").readText()
        val screen = projectFile("app/src/main/java/com/sonharf/game/PremiumCompetitionSimple.kt").readText()
        val podium = projectFile("app/src/main/java/com/sonharf/game/WeeklyPodiumV210.kt").readText()

        assertTrue(entry.contains("PremiumCompetitionScreen(backend = backend)"))
        assertTrue(screen.contains("HAFTALIK SIRALAMA"))
        assertTrue(screen.contains("WEEKLY RANKING"))
        assertTrue(screen.contains("Pazartesi yenilenir"))
        assertTrue(screen.contains("Resets Monday"))
        assertTrue(screen.contains("getMyWeeklyRpV210()"))
        assertTrue(screen.contains("${'$'}{row.rating} RP"))
        assertTrue(podium.contains("sh(\"Haftanın Zirvesi\", \"Weekly podium\")"))
        assertTrue(podium.contains("Bu haftanın en güçlü oyuncuları"))
        assertTrue(podium.contains("sh(\"Tümü\", \"View all\")"))
        assertTrue(podium.contains("player = players.getOrNull(1)"))
        assertTrue(podium.contains("player = players.getOrNull(0)"))
        assertTrue(podium.contains("player = players.getOrNull(2)"))
    }

    private fun projectFile(path: String): File =
        sequenceOf(File(path), File("../$path"))
            .firstOrNull { it.exists() }
            ?: error("Missing repository file: $path")
}
