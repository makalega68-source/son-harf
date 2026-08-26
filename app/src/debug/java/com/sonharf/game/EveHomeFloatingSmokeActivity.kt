package com.sonharf.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Debug-only HOME compositor smoke.
 *
 * Hosts the exact production floating companion over opaque Compose cards. The compact stage uses
 * TextureSurface so Eve must render inline above these cards while its fixed-size native surface is
 * moved around the screen. CI starts this Activity in a fresh process, captures separated frames,
 * checks process stability/native crash signatures, then force-stops it before the room IME smoke.
 */
class EveHomeFloatingSmokeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DisposableEffect(Unit) {
                    EveMascotRuntime.startLivingBehavior()
                    onDispose { EveMascotRuntime.stopLivingBehavior() }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF3FAFF)),
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = 150.dp)
                            .size(width = 330.dp, height = 180.dp)
                            .background(Color(0xFF0E7490), RoundedCornerShape(30.dp)),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(width = 330.dp, height = 220.dp)
                            .background(Color(0xFF4D7C0F), RoundedCornerShape(30.dp)),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = (-130).dp)
                            .size(width = 330.dp, height = 180.dp)
                            .background(Color(0xFF7C3AED), RoundedCornerShape(30.dp)),
                    )

                    EveHomeFloatingCompanion(
                        modifier = Modifier.fillMaxSize(),
                        onOpen = {},
                    )
                }
            }
        }
    }
}
