package com.sonharf.game

import com.sonharf.game.data.MatchHistoryDto
import com.sonharf.game.data.WordSiegeGameDto
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class MenuPrivacyRegressionTest {
    @Test fun staleMissingAndInvalidPresenceAreOffline() {
        val now=Instant.parse("2026-10-04T08:00:00Z").toEpochMilli()
        assertTrue(presenceIsFresh("online","2026-10-04T07:59:30Z",now))
        assertTrue(presenceIsFresh("in_game","2026-10-04T07:59:59Z",now))
        assertFalse(presenceIsFresh("online","2026-10-04T07:58:29Z",now))
        assertFalse(presenceIsFresh("offline","2026-10-04T08:00:00Z",now))
        assertFalse(presenceIsFresh("online",null,now))
        assertFalse(presenceIsFresh("online","invalid",now))
        assertFalse(presenceIsFresh("online","2026-10-05T08:00:00Z",now))
    }

    @Test fun rivalsMergeModesBeforeKeepingOneLatestMatchPerPlayer() {
        val siege=WordSiegeGameDto("s","rival",playerTwoId="me",status="finished",
            playerOneWordScore=10,playerTwoWordScore=20,playerTwoAreaScore=5,finishedAt="2026-10-04T08:00:00Z")
        val old=MatchHistoryDto("old","classic","rival","Rival","loss",playedAt="2026-10-03T08:00:00Z")
        val other=old.copy(matchId="other",opponentId="second",playedAt="2026-10-04T09:00:00Z")
        val rows=latestRivalMatches(listOf(siege,siege,siege.copy(id="live",status="playing")),listOf(old,other),"me")
        assertEquals(listOf("second","rival"),rows.map { it.opponentId })
        assertEquals(25,rows.last().mine)
        assertEquals(10,rows.last().theirs)
        assertEquals("s",rows.last().siege?.id)
        assertTrue(latestRivalMatches(listOf(siege),emptyList(),null).isEmpty())
    }

    @Test fun dailyPuzzlesAreStableDistinctAndProgressIsIsolated() {
        for(english in listOf(false,true)) for(event in DailyWordEvent.entries) {
            val a=dailyEventWords("2026-10-04",english,event)
            assertEquals(a,dailyEventWords("2026-10-04",english,event))
            assertEquals(5,a.map { it.word }.distinct().size)
            assertTrue(a.all { it.word.isNotBlank() && it.clue.isNotBlank() })
        }
        assertEquals(60,eventScore("10101"))
        assertEquals(100,eventScore("11111111"))
        val key=eventProgressKey("a","2026-10-04",false,DailyWordEvent.MIX)
        assertNotEquals(key,eventProgressKey("b","2026-10-04",false,DailyWordEvent.MIX))
        assertNotEquals(key,eventProgressKey("a","2026-10-05",false,DailyWordEvent.MIX))
        assertNotEquals(key,eventProgressKey("a","2026-10-04",false,DailyWordEvent.MEMORY))
    }
}
