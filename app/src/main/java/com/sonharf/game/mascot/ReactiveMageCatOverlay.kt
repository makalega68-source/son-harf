package com.sonharf.game.mascot

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** A reserved inline companion dock. Consumes the screen's existing snapshot; never polls. */
@Composable
fun ReactiveMageCatOverlay(
    snapshot: CompanionSnapshot,
    english: Boolean = false,
    modifier: Modifier = Modifier,
    statusText: String? = null,
) {
    val brain = remember(snapshot.matchId) { MageCatBrain() }
    var reaction by remember(snapshot.matchId) { mutableStateOf(CompanionReaction(MageCatMood.IDLE, "Yanındayım.", "I'm with you.")) }
    LaunchedEffect(snapshot) {
        reaction = brain.observe(snapshot, SystemClock.elapsedRealtime())
        delay(2800)
        reaction = brain.observe(snapshot, SystemClock.elapsedRealtime())
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            MageCatCompanion(size = 44.dp, moodOverride = reaction.mood, eventKey = reaction.sequence, animateIdle = false)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                // Turn ownership stays visible even while a celebration/comfort reaction plays.
                val status = statusText ?: when {
                    snapshot.finished -> if (english) "MATCH COMPLETE" else "MAÇ TAMAMLANDI"
                    snapshot.myTurn -> if (english) "YOUR TURN" else "SIRA SENDE"
                    else -> if (english) "RIVAL'S TURN" else "RAKİPTE"
                }
                Text(status, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, maxLines = 2)
                Text(if (english) reaction.en else reaction.tr,
                    color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 2)
            }
        }
    }
}
