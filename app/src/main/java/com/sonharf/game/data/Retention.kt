package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * G4.4 istemci-taraflı DTO'lar + RPC bağlayıcıları.
 * Migration: supabase/migrations/20260917020000_g44_retention_v1.sql
 */
@Serializable
data class DailyStreakDto(
    @SerialName("current_streak") val currentStreak: Int = 0,
    @SerialName("longest_streak") val longestStreak: Int = 0,
    @SerialName("last_played_day") val lastPlayedDay: String? = null,
    @SerialName("grace_used_week") val graceUsedWeek: Int = 0,
    @SerialName("played_today") val playedToday: Boolean = false,
    @SerialName("grace_available") val graceAvailable: Boolean = true,
)

@Serializable
data class WordCollectionDto(
    @SerialName("total_words") val totalWords: Long = 0,
    @SerialName("this_month") val thisMonth: Long = 0,
    @SerialName("tr_words") val trWords: Long = 0,
    @SerialName("en_words") val enWords: Long = 0,
)

@Serializable
data class SeasonalThemeDto(
    val id: Long,
    @SerialName("name_tr") val nameTr: String,
    @SerialName("name_en") val nameEn: String,
    val multiplier: Int = 2,
    @SerialName("word_count") val wordCount: Long = 0,
)

suspend fun OnlineGameBackend.getDailyStreak(): DailyStreakDto =
    SupabaseProvider.client.postgrest.rpc("get_player_daily_streak").decodeSingle()

suspend fun OnlineGameBackend.getWordCollection(): WordCollectionDto =
    SupabaseProvider.client.postgrest.rpc("get_player_word_collection").decodeSingle()

/**
 * Returns null when no seasonal theme is active. The RPC returns
 * zero rows in that case; we surface that as null instead of the
 * caller having to distinguish an empty list.
 */
suspend fun OnlineGameBackend.getActiveSeasonalTheme(): SeasonalThemeDto? =
    SupabaseProvider.client.postgrest.rpc("get_active_seasonal_theme")
        .decodeList<SeasonalThemeDto>()
        .firstOrNull()
