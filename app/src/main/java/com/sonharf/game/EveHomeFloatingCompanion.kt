package com.sonharf.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Home-only living companion layer.
 *
 * The real GLB is rendered on a transparent compact TextureSurface. The layer is deliberately
 * mounted above the Scaffold by ClassicPremiumApp, so Eve may overlap the blue game card and even
 * the bottom navigation. She stays near her current anchor with subtle drift instead of visibly
 * sliding across the screen without an in-place walk clip. Long-press dragging lets the player
 * place her anywhere in the allowed screen area.
 */
@Composable
internal fun EveHomeFloatingCompanion(
    modifier: Modifier = Modifier,
    onOpen: () -> Unit = { EveLivingRoomRuntime.show() },
) {
    val context = LocalContext.current
    val store = remember { EveCompanionStore(context) }
    var companionName by remember { mutableStateOf(store.name) }
    var renameOpen by remember { mutableStateOf(false) }
    var draftName by remember { mutableStateOf(companionName) }

    DisposableEffect(Unit) {
        EveMascotRuntime.startLivingBehavior()
        onDispose { EveMascotRuntime.stopLivingBehavior() }
    }

    // A low-energy/low-happiness companion may briefly look tired on entry. This is visual only:
    // no punishment, guilt message or competitive advantage is attached to the state.
    LaunchedEffect(Unit) {
        val vitals = store.vitals()
        if (vitals.energy < 25 || vitals.happiness < 25) {
            delay(900L)
            EveMascotRuntime.sadReaction()
        }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val scope = rememberCoroutineScope()

        val mascotSize = 148.dp
        val labelHeight = 24.dp
        val containerWidth = 154.dp
        val containerHeight = mascotSize + labelHeight

        val mascotPx = with(density) { containerWidth.toPx() }
        val containerHeightPx = with(density) { containerHeight.toPx() }
        val sideInsetPx = with(density) { 4.dp.toPx() }
        val topInsetPx = with(density) { 86.dp.toPx() }
        val bottomInsetPx = with(density) { 4.dp.toPx() }
        val driftPx = with(density) { 9.dp.toPx() }

        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val minX = sideInsetPx
        val maxX = (widthPx - mascotPx - sideInsetPx).coerceAtLeast(minX)
        val minY = topInsetPx.coerceAtMost((heightPx - containerHeightPx).coerceAtLeast(0f))
        val maxY = (heightPx - containerHeightPx - bottomInsetPx).coerceAtLeast(minY)

        val x = remember { Animatable(0f) }
        val y = remember { Animatable(0f) }
        var anchorX by remember { mutableStateOf(0f) }
        var anchorY by remember { mutableStateOf(0f) }
        var initialized by remember { mutableStateOf(false) }
        var dragging by remember { mutableStateOf(false) }

        LaunchedEffect(widthPx, heightPx, renameOpen) {
            if (!initialized) {
                // Starts where the approved home composition leaves room for Eve: left of the
                // blue Son Harf card, with enough size to overlap its edge.
                anchorX = minX
                anchorY = minY + (maxY - minY) * 0.46f
                x.snapTo(anchorX)
                y.snapTo(anchorY)
                initialized = true
            } else {
                anchorX = anchorX.coerceIn(minX, maxX)
                anchorY = anchorY.coerceIn(minY, maxY)
                x.snapTo(x.value.coerceIn(minX, maxX))
                y.snapTo(y.value.coerceIn(minY, maxY))
            }

            while (currentCoroutineContext().isActive) {
                if (!dragging && !renameOpen) {
                    // Tiny organic drift around the current anchor. Large autonomous screen travel
                    // would look like foot sliding because the accepted GLB walk clip has root
                    // translation and is intentionally not used until an in-place Blender export.
                    val targetX = (anchorX + Random.nextDouble(-driftPx.toDouble(), driftPx.toDouble()).toFloat())
                        .coerceIn(minX, maxX)
                    val targetY = (anchorY + Random.nextDouble(-driftPx.toDouble(), driftPx.toDouble()).toFloat())
                        .coerceIn(minY, maxY)
                    val durationMs = Random.nextInt(2_700, 4_201)

                    coroutineScope {
                        launch {
                            x.animateTo(
                                targetValue = targetX,
                                animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
                            )
                        }
                        launch {
                            y.animateTo(
                                targetValue = targetY,
                                animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
                            )
                        }
                    }
                    delay(Random.nextLong(120L, 401L))
                } else {
                    delay(180L)
                }
            }
        }

        Column(
            modifier = Modifier
                .offset { IntOffset(x.value.roundToInt(), y.value.roundToInt()) }
                .width(containerWidth)
                .height(containerHeight)
                .zIndex(50f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(mascotSize)
                    .clickable {
                        EveMascotRuntime.touchReaction()
                    }
                    .pointerInput(widthPx, heightPx) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                dragging = true
                                scope.launch {
                                    x.stop()
                                    y.stop()
                                }
                            },
                            onDragEnd = {
                                anchorX = x.value.coerceIn(minX, maxX)
                                anchorY = y.value.coerceIn(minY, maxY)
                                dragging = false
                            },
                            onDragCancel = {
                                anchorX = x.value.coerceIn(minX, maxX)
                                anchorY = y.value.coerceIn(minY, maxY)
                                dragging = false
                            },
                        ) { _, dragAmount ->
                            val nextX = (x.value + dragAmount.x).coerceIn(minX, maxX)
                            val nextY = (y.value + dragAmount.y).coerceIn(minY, maxY)
                            scope.launch {
                                x.snapTo(nextX)
                                y.snapTo(nextY)
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                // Keep HOME's TextureSurface completely out of composition while the rename dialog
                // is active. That preserves the earlier safety assumption that HOME TextureSurface
                // is not alive during IME interaction.
                if (!renameOpen) {
                    EveLive3DStage(
                        modifier = Modifier.fillMaxSize(),
                        compact = true,
                    )
                }
            }

            Spacer(Modifier.height(1.dp))
            Text(
                text = "$companionName ✎",
                modifier = Modifier.clickable {
                    draftName = companionName
                    renameOpen = true
                },
                color = Color(0xFF163B58),
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                maxLines = 1,
            )
        }
    }

    if (renameOpen) {
        AlertDialog(
            onDismissRequest = {
                draftName = companionName
                renameOpen = false
            },
            title = {
                Text(sh("Maskotun adını değiştir", "Rename companion"))
            },
            text = {
                OutlinedTextField(
                    value = draftName,
                    onValueChange = { draftName = it.take(18) },
                    singleLine = true,
                    label = { Text(sh("İsim", "Name")) },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        store.name = draftName
                        companionName = store.name
                        renameOpen = false
                        EveMascotRuntime.happyReaction()
                    },
                ) {
                    Text(sh("Kaydet", "Save"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        draftName = companionName
                        renameOpen = false
                    },
                ) {
                    Text(sh("İptal", "Cancel"))
                }
            },
        )
    }

    // Retain the existing callback contract for callers that explicitly want to open the room.
    // The production home uses the center bottom-bar paw for room navigation; tapping Eve reacts.
    @Suppress("UNUSED_VARIABLE")
    val retainedOpenCallback = onOpen
}
