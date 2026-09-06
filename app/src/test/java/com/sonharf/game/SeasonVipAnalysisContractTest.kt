package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeasonVipAnalysisContractTest {

    @Test
    fun seasonTrackIsServerAuthoritativeAndPowerFree() {
        val season = projectFile("supabase/migrations/20260904093500_season_store_tracks.sql").readText()
        val hardening = projectFile("supabase/migrations/20260904094000_season_style_equip_hardening.sql").readText()

        assertTrue(season.contains("extract(day from (v_season.ends_at-v_season.starts_at)) not between 30 and 45"))
        assertTrue(season.contains("coalesce(p.wins,0)*120"))
        assertTrue(season.contains("coalesce(p.valid_words,0)*3"))
        assertFalse(season.contains("coalesce(level,1)"))
        assertFalse(season.contains("reward_type text not null check (reward_type in (\n    'son_coin','rating'"))
        assertFalse(season.contains("joker"))
        assertFalse(season.contains("extra_time"))
        assertTrue(season.contains("revoke insert,update,delete on public.season_store_claims from anon,authenticated"))

        assertTrue(hardening.contains("delete from public.shop_items\nwhere id='frame_asset_blue_season'"))
        assertTrue(hardening.contains("active=true or rarity in ('SEASON','EVENT')"))
        assertTrue(hardening.contains("select exists(\n    select 1 from public.user_inventory"))
    }

    @Test
    fun ownedArchivedSeasonStyleRemainsVisibleWithoutOpeningInactiveCatalog() {
        val economy = projectFile("app/src/main/java/com/sonharf/game/data/EconomyStore.kt").readText()
        assertTrue(economy.contains("val owned = getInventory()"))
        assertTrue(economy.contains("item.active || item.id in owned"))
        assertFalse(economy.contains("item.active || item.rarity == \"SEASON\""))
    }

    @Test
    fun vipAnalysisIsPostMatchOnlyAndReachableFromHistory() {
        val migration = projectFile("supabase/migrations/20260904093000_vip_match_analysis.sql").readText()
        val dialog = projectFile("app/src/main/java/com/sonharf/game/VipMatchAnalysisDialog.kt").readText()
        val social = projectFile("app/src/main/java/com/sonharf/game/MainSocialScreen.kt").readText()

        assertTrue(migration.contains("if not v_vip then raise exception 'vip_required'"))
        assertTrue(migration.contains("r.status='finished'"))
        assertTrue(migration.contains("coalesce(r.is_bot,false)=false"))
        assertTrue(migration.contains("completed_match_not_available"))
        assertTrue(dialog.contains("Only completed-match data is used"))
        assertTrue(dialog.contains("getVipMatchAnalysis(match.matchId, match.mode)"))
        assertTrue(social.contains("analysisMatch = match"))
        assertTrue(social.contains("VipMatchAnalysisDialog("))
        assertFalse(social.contains("getVipMatchAnalysis("))
    }

    private fun projectFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        val file = candidates.firstOrNull(File::isFile)
        assertNotNull("Project file missing: $path", file)
        return requireNotNull(file)
    }
}
