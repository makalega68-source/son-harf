package com.sonharf.game.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
internal data class LastLetterDailyRun(
    val id: String? = null,
    val day: String,
    val language: String,
    @SerialName("start_word") val startWord: String,
    val words: List<String> = emptyList(),
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("ends_at") val endsAt: String? = null,
    val finished: Boolean = false,
    @SerialName("server_now") val serverNow: String,
)

internal suspend fun lastLetterDaily(action: String, language: String, runId: String? = null, word: String? = null): LastLetterDailyRun =
    SupabaseProvider.client.postgrest.rpc("last_letter_daily_v1", buildJsonObject {
        put("p_action", action); put("p_language", language)
        if (runId != null) put("p_run_id", runId)
        if (word != null) put("p_word", word)
    }).decodeAs()

@Serializable
internal data class LastLetterFairHints(val used: Int = 0, val left: Int = 3)

internal suspend fun lastLetterFairHintsUsed(roomId: String, userId: String): Int =
    SupabaseProvider.client.from("last_letter_hint_usage").select(columns = Columns.list("used")) {
        filter { eq("room_id", roomId); eq("user_id", userId) }
    }.decodeList<LastLetterFairHints>().firstOrNull()?.used ?: 0

internal suspend fun claimLastLetterFairHint(roomId: String, used: Int): Int =
    SupabaseProvider.client.postgrest.rpc("claim_last_letter_fair_hint_v1", buildJsonObject {
        put("p_room_id", roomId); put("p_min_used", used)
    }).decodeAs<LastLetterFairHints>().used
