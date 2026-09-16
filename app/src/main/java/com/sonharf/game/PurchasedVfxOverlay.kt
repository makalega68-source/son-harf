package com.sonharf.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sonharf.game.data.WordSiegeVfxMoveRegistry
import com.sonharf.game.ui.vfx.LocalVfx
import com.sonharf.game.ui.vfx.VfxEvent
import kotlin.math.roundToInt

internal const val PURCHASED_DUEL_WORD_VFX_MS = 720
internal const val PURCHASED_DUEL_WORD_MAX_ALPHA = .92f
internal const val PURCHASED_DUEL_WORD_STAR_COUNT = 6
internal const val PURCHASED_DUEL_WORD_CENTER_STAR_DP = 34f

internal const val PURCHASED_BOARD_PLACE_VFX_MS = 650
internal const val PURCHASED_BOARD_RESOLVE_VFX_MS = 800
internal const val PURCHASED_BOARD_PLACE_MAX_ALPHA = .94f
internal const val PURCHASED_BOARD_RESOLVE_MAX_ALPHA = .98f
internal const val PURCHASED_BOARD_PLACE_STAR_COUNT = 5
internal const val PURCHASED_BOARD_RESOLVE_STAR_COUNT = 6
internal const val PURCHASED_BOARD_PLACE_MIN_STAR_DP = 14f
internal const val PURCHASED_BOARD_RESOLVE_MIN_STAR_DP = 16f
private const val BIG_SIEGE_CAPTURE_THRESHOLD = 4
private const val CASTLE_FALL_OPPONENT_CAPTURE_THRESHOLD = 3

internal enum class PurchasedBoardVfxKind { PLACEMENT, RESOLVED }

internal data class PurchasedBoardVfxEvent(
    val eventKey: String,
    val index: Int,
    val kind: PurchasedBoardVfxKind,
)

private val PurchasedDuelWordVfxDirections = listOf(
    -0.86f to -0.38f,
    0.86f to -0.44f,
    -0.68f to 0.62f,
    0.68f to 0.66f,
    0.00f to -1.00f,
    0.02f to 1.00f,
)

private val PurchasedBoardVfxDirections = listOf(
    -0.90f to -0.62f,
    0.90f to -0.52f,
    -0.72f to 0.72f,
    0.72f to 0.76f,
    0.05f to -1.00f,
    0.02f to 1.00f,
)
private val PurchasedPlacementCyan = Color(0xFF35D6FF)
private val PurchasedWordSuccessGreen = Color(0xFF4B765D)

/**
 * Cosmetic-only, bounded Compose adaptation of a purchased Eric Wang VFX texture.
 * The one-shot ring makes successful word feedback clearly readable without turning it into a
 * full-screen celebration or a persistent idle effect.
 *
 * G3 adoption: existing callers keep their purchased effect. Son Harf authoritative events are
 * emitted by PremierVfxBridge; the letter: event key continues to bridge Kelime Yolu progress.
 */
@Composable
internal fun PurchasedVictoryVfx(eventKey: String, modifier: Modifier = Modifier) {
    val progress = remember(eventKey) { Animatable(0f) }
    val density = LocalDensity.current
    val vfx = LocalVfx.current
    var hostSize by remember(eventKey) { mutableStateOf(IntSize.Zero) }
    var routedToLocalVfx by remember(eventKey) { mutableStateOf(false) }

    LaunchedEffect(eventKey) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(PURCHASED_DUEL_WORD_VFX_MS))
    }
    LaunchedEffect(eventKey, hostSize) {
        if (routedToLocalVfx || hostSize.width <= 0 || hostSize.height <= 0) return@LaunchedEffect
        routedToLocalVfx = true
        val center = Offset(hostSize.width / 2f, hostSize.height / 2f)
        if (eventKey.startsWith("letter:")) {
            val step = eventKey.substringAfterLast(':').toIntOrNull() ?: 0
            if (step >= 4) {
                vfx.play(VfxEvent.UnlockLevel(center))
                vfx.play(VfxEvent.Reward(center))
                vfx.play(VfxEvent.PathComplete(center))
            } else {
                vfx.play(VfxEvent.PathStep(center))
            }
        }
    }

    val p = progress.value
    val envelope = if (p < .16f) p / .16f else ((1f - p) / .84f).coerceIn(0f, 1f)
    val alpha = envelope * PURCHASED_DUEL_WORD_MAX_ALPHA

    Box(
        modifier.fillMaxSize().onGloballyPositioned { hostSize = it.size },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val ringRadiusPx = with(density) { (30f + 58f * p).dp.toPx() }
            val innerRadiusPx = with(density) { (20f + 38f * p).dp.toPx() }
            val glowStrokePx = with(density) { 11.dp.toPx() }
            val ringStrokePx = with(density) { 4.dp.toPx() }
            drawCircle(
                color = PurchasedWordSuccessGreen.copy(alpha = alpha * .22f),
                radius = ringRadiusPx,
                center = center,
                style = Stroke(width = glowStrokePx),
            )
            drawCircle(
                color = PurchasedWordSuccessGreen.copy(alpha = alpha * .88f),
                radius = ringRadiusPx,
                center = center,
                style = Stroke(width = ringStrokePx),
            )
            drawCircle(
                color = PurchasedWordSuccessGreen.copy(alpha = alpha * .44f),
                radius = innerRadiusPx,
                center = center,
                style = Stroke(width = ringStrokePx),
            )
        }

        repeat(PURCHASED_DUEL_WORD_STAR_COUNT) { index ->
            val (xDirection, yDirection) = PurchasedDuelWordVfxDirections[index]
            val distance = 36f + 56f * p
            val starSize = 17f + (index % 2) * 3f + p * 4f
            Image(
                painter = painterResource(R.drawable.vfx_twinkle),
                contentDescription = null,
                colorFilter = ColorFilter.tint(PurchasedWordSuccessGreen),
                modifier = Modifier
                    .offset((xDirection * distance).dp, (yDirection * distance).dp)
                    .size(starSize.dp)
                    .rotate(index * 43f + p * 92f)
                    .alpha(alpha),
            )
        }
        Image(
            painter = painterResource(R.drawable.vfx_twinkle),
            contentDescription = null,
            colorFilter = ColorFilter.tint(PurchasedWordSuccessGreen),
            modifier = Modifier
                .offset(y = (-42).dp)
                .size(PURCHASED_DUEL_WORD_CENTER_STAR_DP.dp)
                .rotate(p * 90f)
                .alpha(alpha),
        )
    }
}

/**
 * Input-transparent screen-space overlay for board action feedback.
 * The overlay is clipped only at the board viewport, while event centers follow board pan/scale.
 *
 * G3.1 adoption: the already-authoritative resolved move id is encoded in the existing event key.
 * The fetched DTO cache supplies score/capture metadata without another network call or gameplay
 * mutation. Practice mode still receives WordAccepted because its resolved event has no DTO cache.
 */
@Composable
internal fun PurchasedBoardActionVfxOverlay(
    events: List<PurchasedBoardVfxEvent>,
    transform: WordSiegeBoardTransform,
    cellSizePx: Float,
    modifier: Modifier = Modifier,
) {
    val vfx = LocalVfx.current
    val resolvedEvents = events.filter { it.kind == PurchasedBoardVfxKind.RESOLVED }
    val resolvedSignature = resolvedEvents.joinToString("|") { it.eventKey }

    // Only a new resolved-event signature may start G3 VFX. Pan/zoom changes update the legacy
    // overlay coordinates but must never replay the one-shot LocalVfx event.
    LaunchedEffect(resolvedSignature) {
        if (resolvedEvents.isEmpty()) return@LaunchedEffect

        val groups = resolvedEvents.groupBy { event ->
            event.eventKey.split(':').getOrNull(1)?.toLongOrNull()
        }
        groups.forEach { (moveId, moveEvents) ->
            val anchors = moveEvents
                .map { event -> wordSiegeCellCenterInViewport(event.index, transform, cellSizePx) }
                .filterNot { it == Offset.Unspecified }
            val anchor = anchors.firstOrNull() ?: return@forEach
            val move = moveId?.let(WordSiegeVfxMoveRegistry::get)

            vfx.play(
                VfxEvent.WordAccepted(
                    score = move?.wordScore ?: 0,
                    anchor = anchor,
                    tint = SonHarfTheme.KusatmaPurple,
                ),
            )

            val captured = move?.capturedCells ?: 0
            when {
                captured >= BIG_SIEGE_CAPTURE_THRESHOLD -> {
                    vfx.play(VfxEvent.BigSiege(score = move?.totalScore ?: 0, anchors = anchors))
                }
                captured > 0 -> {
                    vfx.play(VfxEvent.CellCaptured(anchors = anchors, tint = SonHarfTheme.KusatmaPurple))
                }
            }

            if ((move?.opponentCaptured ?: 0) >= CASTLE_FALL_OPPONENT_CAPTURE_THRESHOLD) {
                vfx.play(VfxEvent.CastleFall(anchor))
            } else if (captured > 0) {
                vfx.play(VfxEvent.MapWave(anchor))
            }
        }
    }

    Box(modifier.fillMaxSize().clipToBounds()) {
        events.forEach { event ->
            key(event.eventKey) {
                PurchasedBoardActionVfx(
                    eventKey = event.eventKey,
                    kind = event.kind,
                    centerPx = wordSiegeCellCenterInViewport(event.index, transform, cellSizePx),
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
    }
}

/** One-shot action effect. Pan/recomposition only updates centerPx and never restarts progress. */
@Composable
private fun PurchasedBoardActionVfx(
    eventKey: String,
    kind: PurchasedBoardVfxKind,
    centerPx: Offset,
    modifier: Modifier = Modifier,
) {
    val progress = remember(eventKey, kind) { Animatable(0f) }
    val durationMs = when (kind) {
        PurchasedBoardVfxKind.PLACEMENT -> PURCHASED_BOARD_PLACE_VFX_MS
        PurchasedBoardVfxKind.RESOLVED -> PURCHASED_BOARD_RESOLVE_VFX_MS
    }
    LaunchedEffect(eventKey, kind) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMs))
    }

    val p = progress.value
    val envelope = if (p < .14f) p / .14f else ((1f - p) / .86f).coerceIn(0f, 1f)
    val tint = if (kind == PurchasedBoardVfxKind.PLACEMENT) PurchasedPlacementCyan else MainUi.Gold
    val maxAlpha = if (kind == PurchasedBoardVfxKind.PLACEMENT) PURCHASED_BOARD_PLACE_MAX_ALPHA else PURCHASED_BOARD_RESOLVE_MAX_ALPHA
    val count = if (kind == PurchasedBoardVfxKind.PLACEMENT) PURCHASED_BOARD_PLACE_STAR_COUNT else PURCHASED_BOARD_RESOLVE_STAR_COUNT
    val minStarDp = if (kind == PurchasedBoardVfxKind.PLACEMENT) PURCHASED_BOARD_PLACE_MIN_STAR_DP else PURCHASED_BOARD_RESOLVE_MIN_STAR_DP
    val ringStartDp = if (kind == PurchasedBoardVfxKind.PLACEMENT) 16f else 20f
    val ringTravelDp = if (kind == PurchasedBoardVfxKind.PLACEMENT) 26f else 32f
    val particleStartDp = if (kind == PurchasedBoardVfxKind.PLACEMENT) 15f else 18f
    val particleTravelDp = if (kind == PurchasedBoardVfxKind.PLACEMENT) 24f else 29f
    val density = LocalDensity.current

    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.matchParentSize()) {
            val ringRadiusPx = with(density) { (ringStartDp + ringTravelDp * p).dp.toPx() }
            val ringStrokePx = with(density) {
                (if (kind == PurchasedBoardVfxKind.PLACEMENT) 3.2f else 3.5f).dp.toPx()
            }
            val glowStrokePx = with(density) {
                (if (kind == PurchasedBoardVfxKind.PLACEMENT) 7f else 8f).dp.toPx()
            }
            drawCircle(
                color = tint.copy(alpha = envelope * maxAlpha * .26f),
                radius = ringRadiusPx,
                center = centerPx,
                style = Stroke(width = glowStrokePx),
            )
            drawCircle(
                color = tint.copy(alpha = envelope * maxAlpha),
                radius = ringRadiusPx,
                center = centerPx,
                style = Stroke(width = ringStrokePx),
            )
        }

        repeat(count) { index ->
            val (xDirection, yDirection) = PurchasedBoardVfxDirections[index]
            val starDp = minStarDp + p * 9f + (index % 2) * 2.5f
            val starPx = with(density) { starDp.dp.toPx() }
            val distancePx = with(density) { (particleStartDp + particleTravelDp * p).dp.toPx() }
            Image(
                painter = painterResource(R.drawable.vfx_twinkle),
                contentDescription = null,
                colorFilter = ColorFilter.tint(tint),
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (centerPx.x + xDirection * distancePx - starPx / 2f).roundToInt(),
                            y = (centerPx.y + yDirection * distancePx - starPx / 2f).roundToInt(),
                        )
                    }
                    .size(starDp.dp)
                    .rotate(index * 41f + p * 105f)
                    .alpha(envelope * maxAlpha),
            )
        }
    }
}
