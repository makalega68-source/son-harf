package com.sonharf.game.data

import io.github.jan.supabase.postgrest.from

data class LeaderboardEntry(val profile: ProfileDto)

/**
 * Real backend-backed leaderboard fallback used by the home screen.
 * Profiles are unique by account UUID; ties prefer fewer losses and then name.
 * Weekly aggregation will replace this fallback when match history aggregation is available.
 */
suspend fun OnlineGameBackend.getLeaderboard(limit: Int): List<LeaderboardEntry> {
    if (limit <= 0) return emptyList()
    return SupabaseProvider.client
        .from("profiles")
        .select()
        .decodeList<ProfileDto>()
        .sortedWith(
            compareByDescending<ProfileDto> { it.wins }
                .thenBy { it.losses }
                .thenBy { it.displayName.lowercase() }
                .thenBy { it.id }
        )
        .take(limit)
        .map(::LeaderboardEntry)
}
