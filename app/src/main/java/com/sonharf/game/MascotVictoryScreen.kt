package com.sonharf.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Mascot owners celebrate a win with the mascot's victory clip instead of the plain result card. */
internal val showMascotVictory: Boolean get() = WordSiegeMascotOwnership.hasAny

/**
 * Full-screen win screen for mascot owners: its own background covers the game (nothing shows
 * behind), the mascot's victory clip plays with its backdrop removed, and the texts and buttons
 * sit underneath it.
 */
@Composable
internal fun MascotVictoryScreen(
    title: String,
    lines: List<String>,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {},
    onDismiss: () -> Unit = onSecondary,
) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(500)) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0B1430), Color(0xFF1B2F5E), Color(0xFF0E1A3A)))),
            contentAlignment = Alignment.Center,
        ) {
            // Warm glow behind the clip.
            Box(
                Modifier
                    .fillMaxWidth(.95f)
                    .aspectRatio(1f)
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp)
                    .background(Brush.radialGradient(listOf(Color(0x55FFD36B), Color(0x00FFD36B)))),
            )
            Column(
                Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 22.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                ChromaKeyVideo(
                    raw = R.raw.mascot_victory,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp).aspectRatio(1f),
                )
                Spacer(Modifier.height(6.dp))
                Column(
                    Modifier.graphicsLayer {
                        alpha = reveal.value
                        translationY = (1f - reveal.value) * 40f
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        title,
                        color = Color(0xFFFFD36B),
                        fontSize = 36.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    lines.forEach { line ->
                        Text(
                            line,
                            color = Color.White.copy(alpha = .9f),
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onPrimary,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC93C), contentColor = Color(0xFF3A2400)),
                    ) {
                        Text(primaryLabel, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    }
                    if (secondaryLabel != null) {
                        TextButton(onClick = onSecondary) {
                            Text(secondaryLabel, color = Color.White.copy(alpha = .85f), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
