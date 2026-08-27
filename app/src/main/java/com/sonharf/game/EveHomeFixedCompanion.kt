package com.sonharf.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Fixed HOME companion slot from the current mascot-layout branch.
 *
 * No viewport drift is introduced here. Eve uses real GLB skeletal clips while awake, enters
 * persistent Rest after 60 seconds without mascot interaction, and remains asleep until a direct
 * mascot touch or chat wakes her. The surrounding HOME layout remains unchanged.
 */
@Composable
internal fun EveHomeFixedCompanion(
    modifier: Modifier = Modifier,
    playerName: String? = null,
) {
    val context = LocalContext.current
    val store = remember { EveCompanionStore(context) }
    val scope = rememberCoroutineScope()
    var localPrompt by remember { mutableStateOf("") }

    val sleeping = EveMascotRuntime.sleepingByInactivity
    val behaviorState = EveMascotRuntime.behaviorState
    val homePrompt = EveMascotRuntime.homePromptText
    val runtimeBubble = EveMascotRuntime.bubbleText
    val visiblePrompt = localPrompt.ifBlank {
        homePrompt.ifBlank {
            if (behaviorState == EveBehaviorState.INTERACTING) runtimeBubble else ""
        }
    }

    DisposableEffect(Unit) {
        EveMascotRuntime.startLivingBehavior()
        onDispose { EveMascotRuntime.stopLivingBehavior() }
    }

    LaunchedEffect(store) {
        while (currentCoroutineContext().isActive) {
            if (store.shouldSleepForInactivity()) {
                EveMascotRuntime.sleepForInactivity()
            } else if (!EveMascotRuntime.sleepingByInactivity) {
                EveMascotRuntime.updateContext(store.behaviorContext())
            }
            delay(1_000L)
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            EveLive3DStage(
                modifier = Modifier.fillMaxSize(),
                compact = true,
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        store.markInteraction()
                        val resolvedPlayerName = playerName
                            ?.trim()
                            ?.take(32)
                            ?.takeIf { it.isNotBlank() }
                            ?: sh("Oyuncu", "Player")
                        val prompt = eveHomeXpPrompt(resolvedPlayerName)
                        localPrompt = prompt
                        EveMascotRuntime.homeTouchHappy(resolvedPlayerName)
                        scope.launch {
                            delay(3_200L)
                            if (localPrompt == prompt) localPrompt = ""
                        }
                    },
            )
        }

        Spacer(Modifier.height(2.dp))
        Text(
            text = when {
                sleeping -> "${store.name}  💤"
                visiblePrompt.isNotBlank() -> visiblePrompt
                else -> store.name
            },
            color = Color(0xFF163B58),
            fontWeight = FontWeight.Black,
            fontSize = if (visiblePrompt.isNotBlank()) 9.sp else 12.sp,
            lineHeight = 11.sp,
            maxLines = 2,
        )
    }
}
