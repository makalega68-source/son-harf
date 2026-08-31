package com.sonharf.game.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put

@Serializable
data class WordSiegeCellDto(
    val letter: String? = null,
    val owner: Int = 0,
    val bonus: String? = null,
    @SerialName("bonus_used") val bonusUsed: Boolean = false,
)

@Serializable
data class WordSiegeGameDto(
    val id: String,
    @SerialName("player_one_id") val playerOneId: String,
    @SerialName("player_two_id") val playerTwoId: String? = null,
    val status: String,
    val language: String = "tr",
    @SerialName("current_player_id") val currentPlayerId: String? = null,
    @SerialName("winner_id") val winnerId: String? = null,
    val board: List<WordSiegeCellDto> = emptyList(),
    val bag: String = "",
    @SerialName("player_one_rack") val playerOneRack: String = "",
    @SerialName("player_two_rack") val playerTwoRack: String? = null,
    @SerialName("player_one_word_score") val playerOneWordScore: Int = 0,
    @SerialName("player_two_word_score") val playerTwoWordScore: Int = 0,
    @SerialName("player_one_area") val playerOneArea: Int = 0,
    @SerialName("player_two_area") val playerTwoArea: Int = 0,
    @SerialName("consecutive_passes") val consecutivePasses: Int = 0,
    @SerialName("move_count") val moveCount: Int = 0,
    @SerialName("last_action") val lastAction: String? = null,
    @SerialName("last_action_player_id") val lastActionPlayerId: String? = null,
    @SerialName("last_move_at") val lastMoveAt: String? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("finished_at") val finishedAt: String? = null,
)

@Serializable
data class WordSiegeMoveDto(
    val id: Long,
    @SerialName("game_id") val gameId: String,
    @SerialName("player_id") val playerId: String,
    @SerialName("primary_word") val primaryWord: String,
    @SerialName("formed_words") val formedWords: List<String> = emptyList(),
    @SerialName("placed_tiles") val placedTiles: List<WordSiegePlacedTileDto> = emptyList(),
    @SerialName("word_score") val wordScore: Int = 0,
    @SerialName("captured_cells") val capturedCells: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class WordSiegePlacedTileDto(
    val index: Int,
    val letter: String,
    val owner: Int,
)

@Serializable
data class WordSiegeMessageDto(
    val id: Long,
    @SerialName("game_id") val gameId: String,
    @SerialName("sender_id") val senderId: String,
    val body: String,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class WordSiegeMovePreviewDto(
    val valid: Boolean = false,
    val reason: String? = null,
    @SerialName("formed_words") val formedWords: List<String> = emptyList(),
    @SerialName("base_word_score") val baseWordScore: Int = 0,
    @SerialName("word_score") val wordScore: Int = 0,
    @SerialName("bonus_score") val bonusScore: Int = 0,
    @SerialName("area_score") val areaScore: Int = 0,
    @SerialName("area_cells") val areaCells: Int = 0,
    @SerialName("captured_cells") val capturedCells: Int = 0,
    @SerialName("bonus_cells") val bonusCells: Int = 0,
    @SerialName("preview_cells") val previewCells: List<Int> = emptyList(),
    @SerialName("total_score") val totalScore: Int = 0,
)

data class WordSiegePlacement(
    val index: Int,
    val rackIndex: Int,
)

@Serializable
private data class WordSiegeMessageWrite(
    @SerialName("game_id") val gameId: String,
    @SerialName("sender_id") val senderId: String,
    val body: String,
)

private fun wordSiegePlacementsJson(placements: List<WordSiegePlacement>) = buildJsonArray {
    placements.forEach { placement ->
        add(
            buildJsonObject {
                put("index", placement.index)
                put("rack_index", placement.rackIndex)
            },
        )
    }
}

suspend fun OnlineGameBackend.getWordSiegeGames(): List<WordSiegeGameDto> =
    SupabaseProvider.client.from("word_siege_games")
        .select()
        .decodeList<WordSiegeGameDto>()
        .filterNot { it.status == "cancelled" }
        .sortedByDescending { it.updatedAt.ifBlank { it.createdAt } }

suspend fun OnlineGameBackend.getWordSiegeGame(gameId: String): WordSiegeGameDto =
    SupabaseProvider.client.from("word_siege_games")
        .select { filter { eq("id", gameId) } }
        .decodeSingle()

suspend fun OnlineGameBackend.findOrCreateWordSiegeGame(language: String): WordSiegeGameDto =
    SupabaseProvider.client.postgrest.rpc(
        "find_or_create_word_siege_game_v1",
        buildJsonObject { put("p_language", if (language.lowercase() == "en") "en" else "tr") },
    ).decodeSingle()

suspend fun OnlineGameBackend.cancelWordSiegeWaiting(gameId: String): WordSiegeGameDto =
    SupabaseProvider.client.postgrest.rpc(
        "cancel_word_siege_waiting_v1",
        buildJsonObject { put("p_game_id", gameId) },
    ).decodeSingle()

suspend fun OnlineGameBackend.previewWordSiegeMove(
    gameId: String,
    placements: List<WordSiegePlacement>,
    horizontal: Boolean,
): WordSiegeMovePreviewDto = SupabaseProvider.client.postgrest.rpc(
    "preview_word_siege_move_v1",
    buildJsonObject {
        put("p_game_id", gameId)
        put("p_horizontal", horizontal)
        put("p_placements", wordSiegePlacementsJson(placements))
    },
).decodeSingle()

suspend fun OnlineGameBackend.submitWordSiegeMove(
    gameId: String,
    placements: List<WordSiegePlacement>,
    horizontal: Boolean,
): WordSiegeGameDto = SupabaseProvider.client.postgrest.rpc(
    "submit_word_siege_move_v1",
    buildJsonObject {
        put("p_game_id", gameId)
        put("p_horizontal", horizontal)
        put("p_placements", wordSiegePlacementsJson(placements))
    },
).decodeSingle()

suspend fun OnlineGameBackend.passWordSiegeTurn(gameId: String): WordSiegeGameDto =
    SupabaseProvider.client.postgrest.rpc(
        "pass_word_siege_turn_v1",
        buildJsonObject { put("p_game_id", gameId) },
    ).decodeSingle()

suspend fun OnlineGameBackend.exchangeWordSiegeTiles(
    gameId: String,
    rackIndices: Set<Int>,
): WordSiegeGameDto = SupabaseProvider.client.postgrest.rpc(
    "exchange_word_siege_tiles_v1",
    buildJsonObject {
        put("p_game_id", gameId)
        put("p_rack_indices", buildJsonArray { rackIndices.sorted().forEach { add(JsonPrimitive(it)) } })
    },
).decodeSingle()

suspend fun OnlineGameBackend.forfeitWordSiegeGame(gameId: String): WordSiegeGameDto =
    SupabaseProvider.client.postgrest.rpc(
        "forfeit_word_siege_game_v1",
        buildJsonObject { put("p_game_id", gameId) },
    ).decodeSingle()

suspend fun OnlineGameBackend.getWordSiegeMoves(gameId: String): List<WordSiegeMoveDto> =
    SupabaseProvider.client.from("word_siege_moves")
        .select { filter { eq("game_id", gameId) } }
        .decodeList<WordSiegeMoveDto>()
        .sortedBy { it.id }

suspend fun OnlineGameBackend.getWordSiegeMessages(gameId: String): List<WordSiegeMessageDto> =
    SupabaseProvider.client.from("word_siege_messages")
        .select { filter { eq("game_id", gameId) } }
        .decodeList<WordSiegeMessageDto>()
        .sortedBy { it.id }

suspend fun OnlineGameBackend.sendWordSiegeMessage(gameId: String, text: String) {
    val senderId = requireNotNull(currentUserId())
    val body = text.trim().take(300)
    require(body.isNotEmpty())
    SupabaseProvider.client.from("word_siege_messages")
        .insert(WordSiegeMessageWrite(gameId, senderId, body))
}
