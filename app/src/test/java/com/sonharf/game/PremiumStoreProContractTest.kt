package com.sonharf.game

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumStoreProContractTest {
    @Test
    fun `permanent premium catalog uses exact production skus`() {
        val catalog = repoFile("app/src/main/java/com/sonharf/game/billing/ProductCatalog.kt").readText()
        listOf("series_game", "letter_table", "score_calculator", "pro_lifetime").forEach {
            assertTrue("Missing premium SKU $it", catalog.contains("\"$it\""))
        }
        assertTrue(catalog.contains("129 TL"))
        assertTrue(catalog.contains("65 TL"))
        assertTrue(catalog.contains("479 TL"))
    }

    @Test
    fun `premium store has no restore purchases ui and uses text free runtime artwork`() {
        val store = repoFile("app/src/main/java/com/sonharf/game/GooglePlayProductsCard.kt").readText()
        assertFalse(store.contains("Satın Almaları Geri Yükle", ignoreCase = true))
        assertFalse(store.contains("Restore Purchases", ignoreCase = true))
        assertTrue(store.contains("R.drawable.premium_series_game"))
        assertTrue(store.contains("R.drawable.premium_letter_table"))
        assertTrue(store.contains("R.drawable.premium_score_calculator"))
        assertTrue(store.contains("R.drawable.premium_pro"))

        listOf(
            "premium_series_game.xml",
            "premium_letter_table.xml",
            "premium_score_calculator.xml",
            "premium_pro.xml",
        ).forEach { name ->
            val vector = repoFile("app/src/main/res/drawable/$name").readText()
            assertTrue(vector.contains("<vector"))
            assertFalse("Decorative premium asset must not bake text", vector.contains("<text"))
        }
    }

    @Test
    fun `server verified premium grant is idempotent and pro bonus is one time`() {
        val migration = repoFile("supabase/migrations/20260919013000_premium_permanent_products_v1.sql").readText()
        assertTrue(migration.contains("apply_verified_premium_purchase_v1"))
        assertTrue(migration.contains("on conflict(purchase_token) do nothing", ignoreCase = true))
        assertTrue(migration.contains("purchase_token_user_mismatch"))
        assertTrue(migration.contains("purchase_token_product_mismatch"))
        assertTrue(migration.contains("series_game"))
        assertTrue(migration.contains("letter_table"))
        assertTrue(migration.contains("score_calculator"))
        assertTrue(migration.contains("pro_bundle"))
        assertTrue(migration.contains("100"))
        assertTrue(migration.contains("diamond_ledger"))
        assertTrue(migration.contains("frame_round_golden_avatar"))
        assertTrue(migration.contains("active_game_limit"))
        assertTrue(migration.contains("50"))
        assertTrue(migration.contains("10"))
        assertTrue(migration.contains("ad_free"))
    }

    @Test
    fun `premium gameplay rpc gates score preview and letter table on server`() {
        val migration = repoFile("supabase/migrations/20260919013000_premium_permanent_products_v1.sql").readText()
        assertTrue(migration.contains("preview_word_siege_move_pro_v1"))
        assertTrue(migration.contains("private.word_siege_preview_move_v1"))
        assertTrue(migration.contains("get_word_siege_letter_table_v1"))
        assertTrue(migration.contains("score_calculator"))
        assertTrue(migration.contains("letter_table"))

        val backend = repoFile("app/src/main/java/com/sonharf/game/data/PremiumWordSiegeBackend.kt").readText()
        assertTrue(backend.contains("preview_word_siege_move_pro_v1"))
        assertTrue(backend.contains("get_word_siege_letter_table_v1"))
    }

    @Test
    fun `son harf history remains backend gated while latest word stays playable`() {
        val history = repoFile("supabase/migrations/20260919030000_pro_word_history_gate_v1.sql").readText()
        assertTrue(history.contains("can_view_sonharf_word_history_v1"))
        assertTrue(history.contains("latest_game_word_id_v1"))
        assertTrue(history.contains("game_words_participant_history_v1"))
    }

    @Test
    fun `series mode is separated from classic matchmaking and enforces missed turn defeat`() {
        val series = repoFile("supabase/migrations/20260919043000_word_siege_series_game_v1.sql").readText()
        assertTrue(series.contains("find_or_create_word_siege_series_game_v1"))
        assertTrue(series.contains("where g.status='waiting' and g.game_mode='classic'"))
        assertTrue(series.contains("where g.status='waiting' and g.game_mode='series'"))
        assertTrue(series.contains("v_minutes not in (3,5,10)"))
        assertTrue(series.contains("v_missed>=3"))
        assertTrue(series.contains("series_auto_pass"))
        assertTrue(series.contains("series_three_missed_turns"))
        assertTrue(series.contains("word_siege_series_reset_actor_v1"))
    }

    @Test
    fun `friend list is pro gated while Series gets only entitlement scoped invite candidates`() {
        val friends = repoFile("supabase/migrations/20260919120000_pro_friend_list_rls_v1.sql").readText()
        assertTrue(friends.contains("can_use_pro_friend_list_v1"))
        assertTrue(friends.contains("friendships pro accepted read v1"))
        assertTrue(friends.contains("status = 'pending'"))
        assertTrue(friends.contains("enforce_pro_friend_request_sender_v1"))
        assertTrue(friends.contains("enforce_pro_friend_game_invite_sender_v1"))
        assertTrue(friends.contains("word_siege_invites_pro_sender_guard_v1"))

        val v2 = repoFile("supabase/migrations/20260919121000_pro_friend_list_rls_perf_and_series_picker_v2.sql").readText()
        assertTrue(v2.contains("friendships pro and series scoped read v2"))
        assertTrue(v2.contains("public.has_series_game_access_v1((select auth.uid()))"))
        assertTrue(v2.contains("public.has_series_game_access_v1("))
        assertTrue(v2.contains("status = 'accepted'"))
        assertTrue(v2.contains("(select auth.uid())"))
    }

    @Test
    fun `pro reward center never loads or exposes rewarded ads`() {
        val rewards = repoFile("app/src/main/java/com/sonharf/game/RewardCenterScreen.kt").readText()
        assertTrue(rewards.contains("val isPro = profile?.isVip == true"))
        assertTrue(rewards.contains("profile?.isVip == false && AdPrivacyManager.adsAllowed"))
        assertTrue(rewards.contains("if (!isPro)"))
        assertTrue(rewards.contains("PRO hesabında reklam gösterilmez"))
        assertTrue(rewards.contains("adController.clear()"))
    }

    @Test
    fun `play verifier keeps permanent premium non consumable and server authoritative`() {
        val verify = repoFile("supabase/functions/verify-play-purchase/index.ts").readText()
        assertTrue(verify.contains("series_game"))
        assertTrue(verify.contains("letter_table"))
        assertTrue(verify.contains("score_calculator"))
        assertTrue(verify.contains("pro_lifetime"))
        assertTrue(verify.contains("purchases/productsv2/tokens"))
        assertTrue(verify.contains("apply_verified_premium_purchase_v1"))
        assertTrue(verify.contains(":acknowledge"))
        assertFalse(verify.contains("consumableProducts = new Set([\"series_game"))
    }

    private fun repoFile(path: String): File = sequenceOf(File(path), File("../$path"))
        .firstOrNull { it.exists() }
        ?: error("Missing repository file: $path")
}
