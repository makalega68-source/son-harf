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
import androidx.compose.material3.Text
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

/**
 * Debug-only HOME compositor smoke.
 *
 * Uses the exact production floating companion but accelerates the requested 60s DIG -> 60s SIT
 * -> SLEEP routine so CI can visually and log-wise prove all three states without a two-minute wait.
 */
class EveHomeFloatingSmokeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var forcedBehaviorContext by remember { mutableStateOf<EveBehaviorContext?>(null) }

                LaunchedEffect(Unit) {
                    // Do not call EveMascotRuntime directly: the production HOME component must
                    // evaluate this override and cancel/restart its own baseline routine.
                    kotlinx.coroutines.delay(25_000L)
                    forcedBehaviorContext = EveBehaviorContext(
                        fullness = 80,
                        happiness = 80,
                        energy = 80,
                        minutesSinceInteraction = 90,
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF3FAFF)),
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-18).dp, y = 126.dp)
                            .size(width = 278.dp, height = 224.dp)
                            .background(Color(0xFF0E7490), RoundedCornerShape(30.dp)),
                    ) {
                        Text(
                            text = "SON HARF",
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 25.sp,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(width = 330.dp, height = 250.dp)
                            .background(Color(0xFF4D7C0F), RoundedCornerShape(30.dp)),
                    ) {
                        Text(
                            text = "BİL BAKALIM",
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 23.sp,
                        )
                    }

                    EveHomeFloatingCompanion(
                        modifier = Modifier.fillMaxSize(),
                        routineTiming = EveHomeRoutineTiming(
                            digMs = 8_000L,
                            sitMs = 5_000L,
                            happyReactionMs = 2_500L,
                        ),
                        playerNameOverride = "TestOyuncu",
                        behaviorContextOverride = forcedBehaviorContext,
                    )

                    Text(
                        text = "🐾  EVE EVİ",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = (-36).dp),
                        color = Color(0xFF163B58),
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}
