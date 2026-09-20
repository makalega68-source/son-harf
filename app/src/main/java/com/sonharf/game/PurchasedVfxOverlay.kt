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
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Native Android/Compose adaptation of the purchased UI & interaction VFX bundle.
 *
 * The source package was authored for Unity; no Unity runtime or dependency is used here.
 * Effects are intentionally short and sparse so feedback feels premium rather than arcade-like.
 */
internal const val PURCHASED_DUEL_WORD_VFX_MS = 620
internal const val PURCHASED_DUEL_WORD_MAX_ALPHA = .78f
internal const val PURCHASED_DUEL_WORD_STAR_COUNT = 5
internal const val PURCHASED_DUEL_WORD_CENTER_STAR_DP = 27f

internal const val PURCHASED_BOARD_PLACE_VFX_MS = 520
internal const val PURCHASED_BOARD_RESOLVE_VFX_MS = 760
internal const val PURCHASED_BOARD_PLACE_MAX_ALPHA = .72f
internal const val PURCHASED_BOARD_RESOLVE_MAX_ALPHA = .82f
internal const val PURCHASED_BOARD_PLACE_STAR_COUNT = 4
internal const val PURCHASED_BOARD_RESOLVE_STAR_COUNT = 5
internal const val PURCHASED_BOARD_PLACE_MIN_STAR_DP = 11f
internal const val PURCHASED_BOARD_RESOLVE_MIN_STAR_DP = 13f

internal const val PURCHASED_REWARD_VFX_MS = 1050
internal const val PURCHASED_WIN_VFX_MS = 1250

internal enum class PurchasedBoardVfxKind { PLACEMENT, RESOLVED }
internal enum class PurchasedMomentVfxKind { REWARD, LEVEL_UP, LEAGUE_UP, PRO, WIN }

internal data class PurchasedBoardVfxEvent(
    val eventKey: String,
    val index: Int,
    val kind: PurchasedBoardVfxKind,
)

private val PurchasedDuelWordVfxDirections = listOf(
    -0.82f to -0.34f,
    0.82f to -0.40f,
    -0.58f to 0.64f,
    0.58f to 0.66f,
    0.00f to -1.00f,
)

private val PurchasedBoardVfxDirections = listOf(
    -0.88f to -0.56f,
    0.88f to -0.50f,
    -0.64f to 0.70f,
    0.64f to 0.72f,
    0.03f to -1.00f,
)

private val PurchasedPlacementTint = Color(0xFF6F8794)
private val PurchasedWordSuccessGreen = Color(0xFF3F7C53)
private val PurchasedResolvedGold = Color(0xFFB58A39)
private val PurchasedRewardGold = Color(0xFFC09A52)
private val PurchasedLeagueBlue = Color(0xFF6F8794)
private val PurchasedProGold = Color(0xFFB58A39)

/**
 * Correct-word / accepted-move feedback used by Son Harf.
 * A compact ring + a handful of particles; never a full-screen confetti shower.
 */
@Composable
internal fun PurchasedVictoryVfx(eventKey: String, modifier: Modifier = Modifier) {
    val progress = remember(eventKey) { Animatable(0f) }
    val density = LocalDensity.current
    LaunchedEffect(eventKey) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(PURCHASED_DUEL_WORD_VFX_MS))
    }
    val p = progress.value
    val envelope = if (p < .18f) p / .18f else ((1f - p) / .82f).coerceIn(0f, 1f)
    val alpha = envelope * PURCHASED_DUEL_WORD_MAX_ALPHA

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val ringRadiusPx = with(density) { (25f + 47f * p).dp.toPx() }
            val innerRadiusPx = with(density) { (17f + 30f * p).dp.toPx() }
            val glowStrokePx = with(density) { 8.dp.toPx() }
            val ringStrokePx = with(density) { 3.dp.toPx() }
            drawCircle(
                color = PurchasedWordSuccessGreen.copy(alpha = alpha * .16f),
                radius = ringRadiusPx,
                center = center,
                style = Stroke(width = glowStrokePx),
            )
            drawCircle(
                color = PurchasedWordSuccessGreen.copy(alpha = alpha * .74f),
                radius = ringRadiusPx,
                center = center,
                style = Stroke(width = ringStrokePx),
            )
            drawCircle(
                color = PurchasedWordSuccessGreen.copy(alpha = alpha * .28f),
                radius = innerRadiusPx,
                center = center,
                style = Stroke(width = ringStrokePx),
            )
        }

        repeat(PURCHASED_DUEL_WORD_STAR_COUNT) { index ->
            val (xDirection, yDirection) = PurchasedDuelWordVfxDirections[index]
            val distance = 29f + 43f * p
            val starSize = 13f + (index % 2) * 2f + p * 3f
            Image(
                painter = painterResource(R.drawable.vfx_twinkle),
                contentDescription = null,
                colorFilter = ColorFilter.tint(PurchasedWordSuccessGreen),
                modifier = Modifier
                    .offset((xDirection * distance).dp, (yDirection * distance).dp)
                    .size(starSize.dp)
                    .rotate(index * 37f + p * 72f)
                    .alpha(alpha),
            )
        }
        Image(
            painter = painterResource(R.drawable.vfx_twinkle),
            contentDescription = null,
            colorFilter = ColorFilter.tint(PurchasedWordSuccessGreen),
            modifier = Modifier
                .offset(y = (-35).dp)
                .size(PURCHASED_DUEL_WORD_CENTER_STAR_DP.dp)
                .rotate(p * 70f)
                .alpha(alpha),
        )
    }
}

/**
 * Input-transparent board overlay. Placement is a muted blue selection pulse; resolved moves use
 * restrained gold to emphasize capture/score resolution. Pan and zoom only move the center.
 */
@Composable
internal fun PurchasedBoardActionVfxOverlay(
    events: List<PurchasedBoardVfxEvent>,
    transform: WordSiegeBoardTransform,
    cellSizePx: Float,
    modifier: Modifier = Modifier,
) {
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

/** One-shot action effect. Recomposition never restarts the animation. */
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
    val envelope = if (p < .16f) p / .16f else ((1f - p) / .84f).coerceIn(0f, 1f)
    val tint = if (kind == PurchasedBoardVfxKind.PLACEMENT) PurchasedPlacementTint else PurchasedResolvedGold
    val maxAlpha = if (kind == PurchasedBoardVfxKind.PLACEMENT) PURCHASED_BOARD_PLACE_MAX_ALPHA else PURCHASED_BOARD_RESOLVE_MAX_ALPHA
    val count = if (kind == PurchasedBoardVfxKind.PLACEMENT) PURCHASED_BOARD_PLACE_STAR_COUNT else PURCHASED_BOARD_RESOLVE_STAR_COUNT
    val minStarDp = if (kind == PurchasedBoardVfxKind.PLACEMENT) PURCHASED_BOARD_PLACE_MIN_STAR_DP else PURCHASED_BOARD_RESOLVE_MIN_STAR_DP
    val ringStartDp = if (kind == PurchasedBoardVfxKind.PLACEMENT) 14f else 18f
    val ringTravelDp = if (kind == PurchasedBoardVfxKind.PLACEMENT) 20f else 27f
    val particleStartDp = if (kind == PurchasedBoardVfxKind.PLACEMENT) 13f else 16f
    val particleTravelDp = if (kind == PurchasedBoardVfxKind.PLACEMENT) 18f else 24f
    val density = LocalDensity.current

    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.matchParentSize()) {
            val ringRadiusPx = with(density) { (ringStartDp + ringTravelDp * p).dp.toPx() }
            val ringStrokePx = with(density) { (if (kind == PurchasedBoardVfxKind.PLACEMENT) 2.4f else 3f).dp.toPx() }
            val glowStrokePx = with(density) { (if (kind == PurchasedBoardVfxKind.PLACEMENT) 5f else 7f).dp.toPx() }
            drawCircle(
                color = tint.copy(alpha = envelope * maxAlpha * .18f),
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
            val starDp = minStarDp + p * 6f + (index % 2) * 1.5f
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
                    .rotate(index * 35f + p * 82f)
                    .alpha(envelope * maxAlpha),
            )
        }
    }
}

/**
 * Reusable high-value moment effect for rewards, level/league progression, PRO and match victory.
 * Callers provide a stable event key so it plays once per server-confirmed event.
 */
@Composable
internal fun PurchasedMomentVfx(
    eventKey: String,
    kind: PurchasedMomentVfxKind,
    modifier: Modifier = Modifier,
) {
    val progress = remember(eventKey, kind) { Animatable(0f) }
    val density = LocalDensity.current
    val duration = if (kind == PurchasedMomentVfxKind.WIN) PURCHASED_WIN_VFX_MS else PURCHASED_REWARD_VFX_MS
    val tint = when (kind) {
        PurchasedMomentVfxKind.REWARD -> PurchasedRewardGold
        PurchasedMomentVfxKind.LEVEL_UP -> PurchasedWordSuccessGreen
        PurchasedMomentVfxKind.LEAGUE_UP -> PurchasedLeagueBlue
        PurchasedMomentVfxKind.PRO -> PurchasedProGold
        PurchasedMomentVfxKind.WIN -> PurchasedWordSuccessGreen
    }
    val particleCount = if (kind == PurchasedMomentVfxKind.WIN) 8 else 6

    LaunchedEffect(eventKey, kind) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(duration))
    }
    val p = progress.value
    val envelope = if (p < .14f) p / .14f else ((1f - p) / .86f).coerceIn(0f, 1f)

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = with(density) { (42f + 92f * p).dp.toPx() }
            drawCircle(
                color = tint.copy(alpha = envelope * .18f),
                radius = radius,
                center = center,
                style = Stroke(with(density) { 10.dp.toPx() }),
            )
            drawCircle(
                color = tint.copy(alpha = envelope * .62f),
                radius = radius,
                center = center,
                style = Stroke(with(density) { 2.5.dp.toPx() }),
            )
        }
        repeat(particleCount) { index ->
            val angleSlot = index % PurchasedBoardVfxDirections.size
            val (dx, dy) = PurchasedBoardVfxDirections[angleSlot]
            val distance = 58f + 86f * p + (index / PurchasedBoardVfxDirections.size) * 12f
            val size = 14f + (index % 3) * 2f + 5f * (1f - p)
            Image(
                painter = painterResource(R.drawable.vfx_twinkle),
                contentDescription = null,
                colorFilter = ColorFilter.tint(tint),
                modifier = Modifier
                    .offset((dx * distance).dp, (dy * distance).dp)
                    .size(size.dp)
                    .rotate(index * 31f + p * 120f)
                    .alpha(envelope * .82f),
            )
        }
    }
}
