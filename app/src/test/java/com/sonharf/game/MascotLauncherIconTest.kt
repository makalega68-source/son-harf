package com.sonharf.game
import org.junit.Assert.assertEquals
import org.junit.Test
class MascotLauncherIconTest {
 @Test fun `four faces repeat with smile on fifth day`() {
  val cycle=listOf(MascotLauncherIcon.Mood.HAPPY,MascotLauncherIcon.Mood.SAD,MascotLauncherIcon.Mood.ANGRY,MascotLauncherIcon.Mood.SLEEPY)
  repeat(16) { assertEquals(cycle[it%4],MascotLauncherIcon.moodForAwayDays(it.toLong())) }
  assertEquals(cycle[0],MascotLauncherIcon.moodForAwayDays(-1))
 }
 @Test fun `every day schedules the next face`() {
  repeat(16) { assertEquals(it.toLong()+1,MascotLauncherIcon.nextTransitionDay(it.toLong())) }
 }
}
