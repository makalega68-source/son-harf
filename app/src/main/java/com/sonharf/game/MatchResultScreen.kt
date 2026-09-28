package com.sonharf.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** One side of the final score. */
internal data class ResultScore(val label: String, val score: String)

/**
 * The end-of-match screen every player sees: full screen (nothing of the game shows behind), the
 * victory or defeat clip with its backdrop removed in the centre, and underneath — symmetric and
 * centred — the title, a short line, the two scores side by side and two equal buttons.
 */
@Composable
internal fun MatchResultScreen(
    won: Boolean,
    title: String,
    subtitle: String?,
    mine: ResultScore,
    rival: ResultScore,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(500)) }
    val accent = if (won) Color(0xFFFFD36B) else Color(0xFF9CC3FF)
    val background = if (won) {
        listOf(Color(0xFF0B1430), Color(0xFF1B2F5E), Color(0xFF0E1A3A))
    } else {
        listOf(Color(0xFF10141F), Color(0xFF1E2638), Color(0xFF121826))
    }
    Dialog(
        onDismissRequest = onSecondary,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            Modifier.fillMaxSize().background(Brush.verticalGradient(background)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // The clip, centred, with a soft glow of the result's colour behind it.
                Box(
                    Modifier.fillMaxWidth().widthIn(max = 360.dp).aspectRatio(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Brush.radialGradient(listOf(accent.copy(alpha = .30f), Color.Transparent))),
                    )
                    ChromaKeyVideo(
                        raw = if (won) R.raw.mascot_victory else R.raw.mascot_defeat,
                        modifier = Modifier.fillMaxSize(),
                        // Smaller than the box so the mascot is not in the viewer's face; the
                        // defeat clip runs slower so its falling letters can be read.
                        scale = .74f,
                        speed = if (won) 1f else .65f,
                    )
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .graphicsLayer {
                            alpha = reveal.value
                            translationY = (1f - reveal.value) * 36f
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        title,
                        color = accent,
                        fontSize = 34.sp,
                        lineHeight = 38.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (subtitle != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            subtitle,
                            color = Color.White.copy(alpha = .8f),
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    // Two equal score columns around a thin divider.
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = .07f), RoundedCornerShape(18.dp))
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ResultScoreColumn(mine, if (won) accent else Color.White, Modifier.weight(1f))
                        Box(Modifier.width(1.dp).height(44.dp).background(Color.White.copy(alpha = .18f)))
                        ResultScoreColumn(rival, if (won) Color.White else accent, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(18.dp))
                    // Two equal buttons.
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = onSecondary,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, Color.White.copy(alpha = .5f)),
                        ) {
                            Text(secondaryLabel, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        }
                        Button(
                            onClick = onPrimary,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                contentColor = if (won) Color(0xFF3A2400) else Color(0xFF0E1A3A),
                            ),
                        ) {
                            Text(primaryLabel, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultScoreColumn(score: ResultScore, color: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            score.label,
            color = Color.White.copy(alpha = .7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Text(score.score, color = color, fontSize = 28.sp, lineHeight = 30.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}
