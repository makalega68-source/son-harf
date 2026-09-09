package com.sonharf.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.findPremierLatestFinishedRoom
import com.sonharf.game.data.isPremierFinished
import kotlinx.coroutines.delay

/** Stable integration point for the rebuilt Unified Pro Premier duel. */
@Composable
fun OnlineGameScreenV6() {
    val backend = remember { OnlineGameBackend() }
    var hasEnteredLiveMatch by remember { mutableStateOf(false) }
    var postMatchRoom by remember { mutableStateOf<GameRoomDto?>(null) }
    var showHammyResult by remember { mutableStateOf(false) }
    val inMatch = SonHarfUiState.inMatch

    LaunchedEffect(inMatch) {
        if (inMatch) {
            hasEnteredLiveMatch = true
            postMatchRoom = null
            showHammyResult = false
            return@LaunchedEffect
        }

        if (!hasEnteredLiveMatch) return@LaunchedEffect

        // Vs -> Playing replaces the child's DisposableEffect and can briefly publish false.
        // Confirm that Premier really stayed out of live play before showing any mascot surface.
        delay(600)
        if (SonHarfUiState.inMatch) return@LaunchedEffect

        hasEnteredLiveMatch = false
        val finished = runCatching { backend.findPremierLatestFinishedRoom() }.getOrNull()
        if (finished != null && finished.isPremierFinished()) {
            postMatchRoom = finished
            showHammyResult = true
            delay(6000)
            showHammyResult = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        // Ranked Premier stays skill-only while the match is live.
        PremierWordDuelScreen()

        val finished = postMatchRoom
        if (finished != null) {
            AnimatedVisibility(
                visible = showHammyResult,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                ProfHammyPremierResultCard(
                    room = finished,
                    meId = backend.currentUserId(),
                    onDismiss = { showHammyResult = false },
                )
            }
        }
    }
}

/**
 * Post-match-only presentation. It consumes an already-final authoritative result and cannot mutate
 * Premier gameplay, score, rating, dictionary, inventory, boosters or economy state.
 */
@Composable
private fun ProfHammyPremierResultCard(
    room: GameRoomDto,
    meId: String?,
    onDismiss: () -> Unit,
) {
    val won = if (room.isBot) {
        room.winnerId == meId && !room.winnerIsBot
    } else {
        room.winnerId == meId
    }
    val mood = if (won) ProfHammyMood.EXCITED else ProfHammyMood.FOCUSED
    val surface = Color(0xFFFFFDF7)
    val border = Color(0xFFCCD8D1)
    val ink = Color(0xFF26382F)
    val muted = Color(0xFF65766D)
    val accent = Color(0xFF4F725E)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = surface,
        border = BorderStroke(1.dp, border),
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfHammyCompanion(
                mood = mood,
                size = 64.dp,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "PROF. HAMMY",
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = if (won) {
                        sh("Harika oyun! Ritmini koru; rövanşa hazırsın. 🏆", "Great game! Keep the rhythm; you’re ready for a rematch. 🏆")
                    } else {
                        sh("Çok yakındı. Yeni zincirde sakin kal; rövanş seni bekliyor.", "That was close. Stay composed in the next chain; your rematch is waiting.")
                    },
                    color = ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = sh("Maç bittikten sonra verilen koçluk", "Coaching shown only after the match"),
                    color = muted,
                    fontSize = 9.sp,
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = sh("Kapat", "Close"),
                    tint = muted,
                )
            }
        }
    }
}
