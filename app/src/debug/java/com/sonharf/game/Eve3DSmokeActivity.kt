package com.sonharf.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

/**
 * Debug-only smoke surface for CI.
 *
 * Deliberately renders only the production SceneView/GLB stage on a static background. There is no
 * Compose sway, PNG, EveMark or other moving UI here, so two different screenshots are evidence
 * that the actual GLB viewport is producing motion.
 */
class Eve3DSmokeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EveMascotRuntime.calm()
        setContent {
            MaterialTheme {
                LaunchedEffect(Unit) {
                    delay(5_000)
                    EveMascotRuntime.play(
                        cue = EveAnimationCue.IDLE_LOOK_AROUND,
                        returnToIdleAfterMs = 8_000,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF123D35)),
                ) {
                    Eve3DStage(Modifier.fillMaxSize())
                }
            }
        }
    }
}
