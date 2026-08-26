package com.sonharf.game

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.delay

/**
 * Debug-only native Surface/IME regression surface.
 *
 * Uses the exact production live Eve stage. The 3D Surface keeps constant bounds but moves across
 * the Compose window while safe GLB clips cross-fade. Only the TextField overlay consumes IME
 * insets, so CI simultaneously stresses Vulkan surface positioning, skeletal blending and five
 * keyboard show/hide cycles without resizing the 3D surface.
 */
class EveImeSmokeActivity : ComponentActivity() {
    private var lastImeVisible: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            val visible = insets.isVisible(WindowInsetsCompat.Type.ime())
            if (visible != lastImeVisible) {
                lastImeVisible = visible
                Log.i("EveImeSmoke", "EVE_IME_VISIBLE=$visible")
            }
            insets
        }

        setContent {
            MaterialTheme {
                EveImeStressSurface()
            }
        }
        ViewCompat.requestApplyInsets(window.decorView)
    }
}

@Composable
private fun EveImeStressSurface() {
    var text by remember { mutableStateOf("") }
    val roaming = rememberInfiniteTransition(label = "eve-surface-roam")
    val roamX by roaming.animateFloat(
        initialValue = -32f,
        targetValue = 32f,
        animationSpec = infiniteRepeatable(
            animation = tween(3_600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "eve-roam-x",
    )

    LaunchedEffect(Unit) {
        while (true) {
            EveMascotRuntime.play(
                cue = EveAnimationCue.IDLE_LOOK_AROUND,
                returnToIdleAfterMs = 0,
            )
            delay(1_800)
            EveMascotRuntime.play(
                cue = EveAnimationCue.GRAZE_ONCE,
                returnToIdleAfterMs = 0,
            )
            delay(1_300)
            EveMascotRuntime.play(
                cue = EveAnimationCue.IDLE_BREATHE,
                returnToIdleAfterMs = 0,
            )
            delay(1_900)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B4639)),
    ) {
        // Simple opaque Compose cards behind the SurfaceView make overlay/compositor regressions
        // visible in CI screenshots as Eve moves over normal UI, not just a flat background.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-180).dp)
                .size(width = 330.dp, height = 120.dp)
                .background(Color(0xFF155E75), RoundedCornerShape(28.dp)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 170.dp)
                .size(width = 330.dp, height = 120.dp)
                .background(Color(0xFF365314), RoundedCornerShape(28.dp)),
        )

        EveLive3DStage(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = roamX.dp, y = (-20).dp)
                .size(width = 300.dp, height = 520.dp),
            compact = false,
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .padding(12.dp),
            color = Color(0xEE10261D),
            shape = RoundedCornerShape(24.dp),
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                placeholder = { Text("EVE ile konuş...") },
                singleLine = true,
            )
        }
    }
}
