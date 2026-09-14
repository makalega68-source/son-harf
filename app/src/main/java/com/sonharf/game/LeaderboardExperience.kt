package com.sonharf.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider

/**
 * Compatibility entry point kept for existing navigation calls.
 * The old multi-period league/leaderboard surface has been retired in favor of one competition
 * screen so league, rating and weekly ranking no longer live in separate experiences.
 */
@Composable
fun LeaderboardExperienceScreen(onBack: () -> Unit) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }

    if (backend == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SonHarfTheme.Primary)
        }
        return
    }

    PremiumCompetitionScreen(backend = backend)
}
