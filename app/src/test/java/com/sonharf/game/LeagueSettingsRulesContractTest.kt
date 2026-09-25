package com.sonharf.game

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeagueSettingsRulesContractTest {
    @Test
    fun `league screen shows the next target and real filters`() {
        val league = source("ProfessionalLeaderboardScreen.kt")
        assertTrue(league.contains("gameText(\"Lig\", \"League\")"))
        assertTrue(league.contains("LeagueProgress(progress.progress)"))
        assertTrue(league.contains("backend.getWeeklyTopV210(limit = 50)"))
        assertTrue(league.contains("backend.getFriends()"))
        // No country data exists on the server, so no Turkey filter is invented.
        assertFalse(league.contains("Türkiye"))
    }

    @Test
    fun `settings are grouped and keep the existing preferences`() {
        val settings = source("ProfessionalSettingsScreen.kt")
        val order = listOf("\"OYUN\"", "\"SOSYAL\"", "GÖRÜNÜM • TEMA", "\"HESAP\"", "\"UYGULAMA\"").map { settings.indexOf(it) }
        assertTrue("groups missing: $order", order.all { it >= 0 })
        assertEquals(order.sorted(), order)
        listOf(
            "SonHarfPreferences.setMusicEnabled(context, it)",
            "SonHarfPreferences.setSoundEnabled(context, it)",
            "SonHarfPreferences.setVibrationEnabled(context, it)",
            "SonHarfPreferences.setGameInviteNotificationsEnabled(context, it)",
            "SonHarfPreferences.setFriendRequestNotificationsEnabled(context, it)",
            "SonHarfPreferences.setSystemNotificationsEnabled(context, it)",
            "SonHarfPreferences.setLanguage(context, next)",
        ).forEach { assertTrue("missing $it", settings.contains(it)) }
        assertTrue(settings.contains("listOf(\"TÜRKÇE\", \"ENGLISH\")"))
        assertTrue(settings.contains("RulesScreen("))
    }

    @Test
    fun `rules state the server scoring rule`() {
        val topics = ruleTopics()
        assertTrue(topics.size >= 11)
        val all = topics.joinToString(" ") { it.body }
        assertTrue(all.contains("her küp 2 puandır") || all.contains("is worth 2 points"))
        assertTrue(all.contains("Kelime puanı kalıcıdır") || all.contains("Word points are permanent"))
        assertTrue(source("RulesScreen.kt").contains("Kelime puanı + Bölge puanı"))
    }

    private fun source(name: String): String =
        sequenceOf(File("src/main/java/com/sonharf/game/$name"), File("app/src/main/java/com/sonharf/game/$name"))
            .firstOrNull { it.exists() }
            ?.readText()
            ?: error("Missing source file: $name")
}
