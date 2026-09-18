package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class WordSiegePremiumPreviewDto(
    @SerialName("word_score") val wordScore: Int = 0,
    @SerialName("area_score") val areaScore: Int = 0,
    @SerialName("total_score") val totalScore: Int = wordScore + areaScore,
    @SerialName("captured_cells") val capturedCells: Int = 0,
    @SerialName("neutral_captured") val neutralCaptured: Int = 0,
    @SerialName("opponent_captured") val opponentCaptured: Int = 0,
)

@Serializable
data class WordSiegeLetterCountDto(
    val letter: String,
    val remaining: Int = 0,
)

suspend fun OnlineGameBackend.previewPremiumWordSiegeMove(
    gameId: String,
    placements: List<WordSiegePlacement>,
    horizontal: Boolean,
): WordSiegePremiumPreviewDto = SupabaseProvider.client.postgrest.rpc(
    "preview_word_siege_move_pro_v1",
    buildJsonObject {
        put("p_game_id", gameId)
        put("p_horizontal", horizontal)
        put(
            "p_placements",
            buildJsonArray {
                placements.forEach { placement ->
                    add(
                        buildJsonObject {
                            put("index", placement.index)
                            put("rack_index", placement.rackIndex)
                        },
                    )
                }
            },
        )
    },
).decodeAs()

suspend fun OnlineGameBackend.getPremiumWordSiegeLetterTable(gameId: String): List<WordSiegeLetterCountDto> =
    SupabaseProvider.client.postgrest.rpc(
        "get_word_siege_letter_table_v1",
        buildJsonObject { put("p_game_id", gameId) },
    ).decodeAs()
