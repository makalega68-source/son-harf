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
 * Renders the exact production live SceneView/GLB stage used by the active room and home dock.
 * There is no Compose sway, PNG or EveMark here, so visible motion must come from the real GLB.
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
                        returnToIdleAfterMs = 0,
                    )
                    delay(2_000)
                    // Request the same clip again. animationVersion must force a true replay.
                    EveMascotRuntime.play(
                        cue = EveAnimationCue.IDLE_LOOK_AROUND,
                        returnToIdleAfterMs = 0,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF123D35)),
                ) {
                    EveLive3DStage(Modifier.fillMaxSize())
                }
            }
        }
    }
}
