package com.sonharf.game

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PremiumPostMatchAnalysisContractTest {
    private val data = File("src/main/java/com/sonharf/game/data/VipMatchAnalysis.kt").readText()
    private val center = File("src/main/java/com/sonharf/game/PremiumAnalysisCenter.kt").readText()
    private val profile = File("src/main/java/com/sonharf/game/ProfileExperience.kt").readText()
    private val migration = File("../supabase/migrations/20260912134500_vip_match_analysis_current_territory.sql").readText()

    @Test
    fun `analysis uses authoritative completed-match RPCs only`() {
        assertTrue(data.contains("get_vip_match_analysis_v1"))
        assertTrue(data.contains("get_vip_recent_completed_matches_v1"))
        assertTrue(migration.contains("g.status = 'finished'"))
        assertTrue(migration.contains("a.status = 'finished'"))
        assertTrue(migration.contains("s.status = 'finished'"))
        assertTrue(migration.contains("coalesce(g.is_bot, false) = false"))
        assertFalse(data.contains("from(\"profiles\")"))
        assertFalse(data.contains("from(\"word_siege_games\")"))
    }

    @Test
    fun `kelime tahtı score reconstructs current owned cells from capture deltas`() {
        assertTrue(migration.contains("from public.word_siege_moves m where m.game_id=r.id"))
        assertTrue(migration.contains("coalesce(m.neutral_captured,0)+coalesce(m.opponent_captured,0)"))
        assertTrue(migration.contains("-coalesce(m.opponent_captured,0)"))
        assertTrue(migration.contains("greatest(0,coalesce"))
        assertTrue(migration.contains(")*2 end my_area_score"))
        assertTrue(migration.contains(")*2 end opp_area_score"))
        assertFalse(migration.contains("array_length(r.player_one_area"))
        assertTrue(migration.contains("get_vip_match_analysis_v1_source_drift"))
        assertTrue(migration.contains("revoke all on function public.get_vip_match_analysis_v1(uuid,text) from public, anon"))
        assertTrue(migration.contains("grant execute on function public.get_vip_match_analysis_v1(uuid,text) to authenticated"))
    }

    @Test
    fun `premium analysis cannot become a live tactical assist`() {
        assertTrue(center.contains("Completed matches only"))
        assertTrue(center.contains("no word suggestions"))
        assertTrue(center.contains("territory preview"))
        assertTrue(center.contains("extra time"))
        assertTrue(center.contains("rating"))
        assertTrue(migration.contains("get_vip_recent_completed_matches_v1"))
        assertFalse(migration.contains("status = 'playing'"))
        assertFalse(center.contains("recommendedMove"))
        assertFalse(center.contains("suggestedWord"))
        assertFalse(center.contains("livePreview"))
    }

    @Test
    fun `profile exposes accessible post-match analysis launcher`() {
        assertTrue(profile.contains("PremiumAnalysisCenterLauncher"))
        assertTrue(center.contains(".height(48.dp)"))
        assertTrue(center.contains("getVipRecentCompletedMatches"))
        assertTrue(center.contains("getVipMatchAnalysis"))
    }
}
