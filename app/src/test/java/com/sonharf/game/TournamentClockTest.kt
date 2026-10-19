package com.sonharf.game
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test
class TournamentClockTest {
 @Test fun twoHourClockResetsOnIstanbulEvenHour(){
  fun ms(v:String)=Instant.parse(v).toEpochMilli()
  assertEquals(ms("2026-10-02T20:00:00+03:00"),tournamentNextRegular(ms("2026-10-02T18:00:00+03:00")))
  assertEquals(ms("2026-10-02T20:00:00+03:00"),tournamentNextRegular(ms("2026-10-02T19:59:59+03:00")))
  assertEquals(ms("2026-10-03T00:00:00+03:00"),tournamentNextRegular(ms("2026-10-02T22:00:00+03:00")))
 }
 @Test fun countdownRoundsUpAndNeverGoesNegative(){
  assertEquals("02:00:00",tournamentClockText(7200000,0))
  assertEquals("00:00:01",tournamentClockText(1000,999))
  assertEquals("00:00:00",tournamentClockText(1000,1001))
 }
}
