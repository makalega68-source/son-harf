package com.sonharf.game

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EveMindTest {
    private class MapMemory : EveMemory {
        val values = HashMap<String, Any>()
        override fun getInt(key: String, default: Int) = values[key] as? Int ?: default
        override fun putInt(key: String, value: Int) { values[key] = value }
        override fun getLong(key: String, default: Long) = values[key] as? Long ?: default
        override fun putLong(key: String, value: Long) { values[key] = value }
        override fun getString(key: String, default: String) = values[key] as? String ?: default
        override fun putString(key: String, value: String) { values[key] = value }
    }

    @Test fun everyTopicHasTurkishAndEnglishLines() {
        for (topic in EveTopic.entries) {
            val pool = EveLinesBook.of(topic)
            assertTrue("$topic has lines", pool.isNotEmpty())
            pool.forEach { (tr, en) -> assertTrue(tr.isNotBlank() && en.isNotBlank()) }
        }
    }

    @Test fun linesDoNotRepeatUntilThePoolIsUsedUp() {
        val mind = EveMind(MapMemory(), Random(7))
        val pool = EveLinesBook.of(EveTopic.IDLE)
        val said = (1..pool.size).map { mind.line(EveTopic.IDLE, "Ümit").first }
        assertEquals(pool.size, said.toSet().size)
        // The next one is the line said longest ago, never the one just said.
        assertNotEquals(said.last(), mind.line(EveTopic.IDLE, "Ümit").first)
    }

    @Test fun memoryOfRecentLinesSurvivesARestart() {
        val memory = MapMemory()
        val first = EveMind(memory, Random(1)).line(EveTopic.TIP, null).first
        val again = (1..4).map { EveMind(memory, Random(it)).line(EveTopic.TIP, null).first }
        assertFalse(first in again)
    }

    @Test fun nameIsNeverGivenASuffixAndDropsOutCleanly() {
        assertEquals("Yine geldin, Ümit! Seni bekliyordum 🌿", EveMind.fill("Yine geldin, %s! Seni bekliyordum 🌿", "Ümit", 0, ""))
        assertEquals("Yine geldin! Seni bekliyordum 🌿", EveMind.fill("Yine geldin, %s! Seni bekliyordum 🌿", null, 0, ""))
        assertEquals("Günaydın! Sabah 🌤️", EveMind.fill("Günaydın %s! Sabah 🌤️", "", 0, ""))
        assertEquals("Kuyruğumu sallıyorum", EveMind.fill("%s, kuyruğumu sallıyorum", null, 0, ""))
        assertEquals("Üst üste 3 galibiyet!", EveMind.fill("Üst üste %d galibiyet!", null, 3, ""))
        EveTopic.entries.forEach { topic ->
            EveLinesBook.of(topic).forEach { (tr, en) ->
                assertFalse(tr, EveMind.fill(tr, null, 2, "kelime").contains("%"))
                assertFalse(en, EveMind.fill(en, null, 2, "word").contains("%"))
                // A name is followed only by punctuation or a space, never glued to a suffix.
                assertFalse(tr, Regex("%s[A-Za-zÇĞİÖŞÜçğıöşü'’]").containsMatchIn(tr))
            }
        }
    }

    @Test fun friendshipGrowsThroughDailyMeetingsAndCappedTaps() {
        val mind = EveMind(MapMemory())
        assertEquals(0, mind.level)
        val first = mind.meet(100)
        assertEquals(1, first.meetings)
        assertEquals(0, mind.meet(100).daysAway) // same day counts once
        assertEquals(1, mind.meet(100).meetings)
        repeat(20) { mind.pet() }
        assertEquals(4 + 6, mind.points) // only six taps a day count
        val away = mind.meet(103)
        assertEquals(3L, away.daysAway)
        assertEquals(EveTopic.MISSED, mind.helloTopic(away))
        assertEquals(1, mind.addPoints(30))
        assertEquals(EveTopic.TAP_FRIEND, mind.tapTopic())
        assertEquals(2, EveMind.levelOf(120))
    }

    @Test fun seesWinsLossesAndStreaksSinceLastTime() {
        val mind = EveMind(MapMemory())
        assertNull(mind.gameNews(wins = 4, winStreak = 1)) // first look only remembers
        assertEquals(EveTopic.WIN_SEEN, mind.gameNews(wins = 5, winStreak = 1))
        assertEquals(EveTopic.STREAK, mind.gameNews(wins = 6, winStreak = 2))
        assertEquals(EveTopic.LOSS_SEEN, mind.gameNews(wins = 6, winStreak = 0))
        assertNull(mind.gameNews(wins = 6, winStreak = 0))
    }

    @Test fun announcementsRespectCurrentCompetitionStateAndCoolDown() {
        val memory = MapMemory()
        val mind = EveMind(memory, Random(11))
        val outsideHour = EveSituation(hour = 20, dayOfWeek = 3, throneHourActive = false)
        repeat(30) { assertNotEquals(EveTopic.THRONE_HOUR, mind.idleTopic(outsideHour, 1_000_000L)) }

        val live = outsideHour.copy(throneHourActive = true, throneRank = 2, tournamentLive = true)
        val topics = (1..60).map { mind.idleTopic(live, 2_000_000L) }
        assertTrue(topics.count { it == EveTopic.THRONE_HOUR } <= 1)
        assertTrue(topics.count { it == EveTopic.THRONE_TOP } <= 1)
        assertTrue(topics.count { it == EveTopic.TOURNAMENT_LIVE } <= 1)

        val reopened = EveMind(memory, Random(12))
        repeat(30) {
            val topic = reopened.idleTopic(live, 2_001_000L)
            assertFalse(topic in setOf(EveTopic.THRONE_HOUR, EveTopic.THRONE_TOP, EveTopic.TOURNAMENT_LIVE))
        }
    }

    @Test fun idleTalkAndActsVaryWithoutRepeating() {
        val mind = EveMind(MapMemory(), Random(3))
        val situation = EveSituation(hour = 20, dayOfWeek = 7, throneRank = 2, tournamentLive = true, winStreak = 3, longestWord = "kelebek")
        val topics = (1..12).map { mind.idleTopic(situation) }
        topics.windowed(2).forEach { (a, b) -> assertNotEquals(a, b) }
        assertTrue(topics.toSet().size >= 6)
        val weights = FloatArray(10) { 1f }
        val acts = (1..40).map { mind.pickAct(weights) }
        acts.windowed(4).forEach { assertEquals(4, it.toSet().size) }
    }
}
