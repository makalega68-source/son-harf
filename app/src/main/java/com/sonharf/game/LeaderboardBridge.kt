package com.sonharf.game

import com.sonharf.game.data.LeaderboardEntry
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from

/** Makes the backend leaderboard available to screens in the app package without
 * requiring every screen to remember an extension-function import. */
suspend fun OnlineGameBackend.getLeaderboard(limit: Int = 50): List<LeaderboardEntry> =
    SupabaseProvider.client.from("profiles")
        .select()
        .decodeList<ProfileDto>()
        .map { profile ->
            val matches = profile.wins + profile.losses
            LeaderboardEntry(
                profile = profile,
                matches = matches,
                winRate = if (matches == 0) 0 else profile.wins * 100 / matches,
            )
        }
        .sortedWith(
            compareByDescending<LeaderboardEntry> { it.profile.wins }
                .thenByDescending { it.winRate }
                .thenBy { it.profile.losses }
                .thenBy { it.profile.id }
        )
        .take(limit.coerceAtLeast(0))
