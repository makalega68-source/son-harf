package com.sonharf.game
import java.io.File
import org.junit.Assert.*
import org.junit.Test
class ThroneIntegrationTest {
 private fun source(n:String)=File("src/main/java/com/sonharf/game/$n").readText()
 @Test fun liveShellRoutesTheThroneAndCountdown(){
  val shell=source("PremiumUnifiedProApp.kt")
  assertTrue(shell.contains("onThrone = { destination = PremiumDestination.COMPETE },"))
  // Throne and Events are two equal home shortcuts; Events also links to the Throne.
  assertTrue(source("HomeLobby.kt").contains("sh(\"Taht\", \"Throne\"), Modifier.weight(1f), onClick = onThrone"))
  assertTrue(shell.contains("onThrone = { destination = PremiumDestination.COMPETE }"))
  assertTrue(source("CompetitionHubScreen.kt").contains("ThroneScreen(onBack = onBack"))
  val home=source("PremiumHomeV3.kt")
  assertTrue(home.contains("R.drawable.kelime_atolyesi_game_icon, onWorkshop)"))
  val events=source("EventsCalendarScreen.kt")
  assertTrue(events.contains("item(key=\"atelier_tournament\")"))
  assertTrue(events.contains("ThroneBackend.tournament()"))
  val tournament=source("ThroneExperience.kt")
  assertTrue(tournament.contains("ThroneBackend.tournament()"))
  assertTrue(tournament.contains("tournamentClockText(tournamentNextRegular(now),now)"))
  assertTrue(home.contains("ThroneBackend.week().rows"))
  assertTrue(source("ThroneExperience.kt").contains("it.xp"));assertFalse(home.contains("getWeeklyTopV210(limit = 3)"))
 }
 @Test fun tournamentUsesServerEntryAndAcceptedWordTranscript(){
  val screen=source("KelimeAtolyesiScreen.kt")
  assertTrue(screen.contains("ThroneBackend.start(language)"))
  assertTrue(screen.contains("roundSeconds = entry.seconds"))
  assertTrue(screen.contains("dailySeed(language, entry.seedKey)"))
  assertTrue(screen.contains("ThroneBackend.finish(entry, finished.score, finished.words, finished.completedTasks)"))
  assertTrue(screen.contains("savingTournament"))
  assertTrue(source("data/ThroneBackend.kt").contains("p_transcript"))
 }
 @Test fun gameCompletionsAndMissionBonusesAreIdempotent(){
  val sql=File("../supabase/migrations/20261002062322_atelier_open_tournaments_and_weekly_throne.sql").readText()
  assertTrue(sql.contains("primary key(user_id,source,source_id)"))
  assertTrue(sql.contains("throne_last_letter_result"));assertTrue(sql.contains("throne_siege_result"));assertTrue(sql.contains("throne_atelier_daily_result"))
  assertTrue(sql.contains("Europe/Istanbul"));assertTrue(sql.contains("source<>'mission'"))
  assertTrue(sql.contains("pg_advisory_xact_lock"));assertTrue(sql.contains("enable row level security"))
 }
}
