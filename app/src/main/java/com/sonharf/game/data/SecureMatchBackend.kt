package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

suspend fun OnlineGameBackend.triggerBilBakalimBonus(roomId: String): GameRoomDto =
    SupabaseProvider.client.postgrest.rpc(
        "trigger_bilbakalim_bonus_v2",
        buildJsonObject { put("p_room_id", roomId) },
    ).decodeSingle()

suspend fun OnlineGameBackend.answerBilBakalimNumeric(roundId: String, value: Long): GameRoomDto =
    SupabaseProvider.client.postgrest.rpc(
        "answer_bilbakalim_numeric_v4",
        buildJsonObject { put("p_round_id", roundId); put("p_value", value) },
    ).decodeSingle()

suspend fun OnlineGameBackend.getVoiceUses(roomId: String): Int =
    SupabaseProvider.client.postgrest.rpc(
        "get_voice_uses_v1",
        buildJsonObject { put("p_room_id", roomId) },
    ).decodeSingle()

suspend fun OnlineGameBackend.submitVoiceWord(roomId: String, word: String, requestId: String): GameRoomDto =
    SupabaseProvider.client.postgrest.rpc(
        "submit_word_voice_v1",
        buildJsonObject {
            put("p_room_id", roomId)
            put("p_word", word.trim())
            put("p_request_id", requestId)
        },
    ).decodeSingle()
