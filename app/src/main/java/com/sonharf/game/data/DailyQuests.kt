package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * One quest row returned by public.get_or_issue_daily_quests().
 * Migration: supabase/migrations/20260917000000_daily_quests_v1.sql
 */
@Serializable
data class DailyQuestDto(
    @SerialName("quest_id") val questId: Long,
    val game: String,
    val metric: String,
    val target: Int,
    val progress: Int = 0,
    val completed: Boolean = false,
    @SerialName("reward_claimed") val rewardClaimed: Boolean = false,
    @SerialName("title_tr") val titleTr: String,
    @SerialName("title_en") val titleEn: String,
    @SerialName("reward_diamonds") val rewardDiamonds: Int = 3,
)

/** Today's 3 daily quests, issued if missing. */
suspend fun OnlineGameBackend.getOrIssueDailyQuests(): List<DailyQuestDto> =
    SupabaseProvider.client.postgrest.rpc("get_or_issue_daily_quests").decodeList()

/** Server-side progress counter. p_key is the client's idempotency key
 * so a re-send of the same event does not double-count. */
suspend fun OnlineGameBackend.recordQuestProgress(
    game: String,
    metric: String,
    delta: Int,
    idempotencyKey: String,
) {
    if (delta <= 0) return
    SupabaseProvider.client.postgrest.rpc(
        "record_quest_progress",
        buildJsonObject {
            put("p_game", game)
            put("p_metric", metric)
            put("p_delta", delta)
            put("p_key", idempotencyKey)
        },
    )
}
