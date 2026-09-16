package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * G4.2 istemci-tarafı DTO'lar.
 * Migration: supabase/migrations/20260917030000_g42_weekly_league_v1.sql
 */
@Serializable
data class WeeklyLeaguePositionDto(
    @SerialName("week_start") val weekStart: String,
    @SerialName("rating_start") val ratingStart: Int,
    @SerialName("rating_now") val ratingNow: Int,
    @SerialName("league_start") val leagueStart: String,
    @SerialName("league_now") val leagueNow: String,
    val delta: Int = 0,
    @SerialName("is_bronze") val isBronze: Boolean = false,
)

@Serializable
data class ActiveWeeklyTournamentDto(
    @SerialName("tournament_id") val tournamentId: Long,
    @SerialName("week_start") val weekStart: String,
    val language: String = "tr",
    val status: String = "scheduled",
    @SerialName("my_seed") val mySeed: Int? = null,
    @SerialName("winner_id") val winnerId: String? = null,
    @SerialName("my_next_room_id") val myNextRoomId: String? = null,
)

suspend fun OnlineGameBackend.getWeeklyLeaguePosition(): WeeklyLeaguePositionDto? =
    SupabaseProvider.client.postgrest.rpc("get_my_weekly_league_position")
        .decodeList<WeeklyLeaguePositionDto>()
        .firstOrNull()

suspend fun OnlineGameBackend.getActiveWeeklyTournament(): ActiveWeeklyTournamentDto? =
    SupabaseProvider.client.postgrest.rpc("get_my_active_weekly_tournament")
        .decodeList<ActiveWeeklyTournamentDto>()
        .firstOrNull()
