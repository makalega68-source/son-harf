package com.sonharf.game

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sonharf.game.ui.premium.rememberReducedMotion

/**
 * Profile avatar renderer kept source-compatible with older call sites.
 *
 * G5.1:
 * - Pro members: 4-5dp gold sweep-gradient ring + outer glow. A short
 *   highlight sweeps across every ~4s (skipped when the system "remove
 *   animations" preference is on). At sizes <40dp only the plain gold
 *   halo is drawn (no sweep).
 * - Everyone else: no ring at all. The bare profile photo is shown so
 *   non-Pro accounts do not carry a cosmetic frame.
 *
 * Legacy [frameId] is accepted for source compatibility but is not drawn.
 */
private val ProGoldEdge = Color(0xFF8A6A1F)
private val ProGoldBright = Color(0xFFF5C542)
private val ProGoldPale = Color(0xFFFFF3C4)
private val ProGoldDeep = Color(0xFFD4AF37)

@Composable
internal fun FramedProfilePhotoAvatar(
    avatarPath: String?,
    gender: String?,
    name: String,
    size: Dp,
    frameId: String?,
    accent: Color = SonHarfCyan,
    visible: Boolean = true,
    showGenderBadge: Boolean = true,
    isPro: Boolean = false,
) {
    @Suppress("UNUSED_VARIABLE")
    val legacyFrameId = frameId
    if (!isPro) {
        // Non-Pro: bare photo, no ring (G5.1).
        ProfilePhotoAvatarWithGender(
            avatarPath = avatarPath,
            gender = gender,
            name = name,
            size = size,
            accent = accent,
            visible = visible,
            showGenderBadge = showGenderBadge,
        )
        return
    }

    val reducedMotion = rememberReducedMotion()
    val small = size < 40.dp
    val ringWidth: Dp = if (small) 2.dp else 4.dp
    val glowWidth: Dp = if (small) 0.dp else 3.dp

    // Sweep gradient: constant gold rainbow around the ring.
    val ringBrush = Brush.sweepGradient(
        listOf(ProGoldEdge, ProGoldBright, ProGoldPale, ProGoldDeep, ProGoldEdge),
    )

    // Every 4s: a short bright band travels once across the ring.
    val transition = rememberInfiniteTransition(label = "pro-frame")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pro-sweep",
    )
    val showSweep = !small && !reducedMotion && sweep < 0.25f
    val sweepAngle = sweep * 4f // 0..1 across the shine window

    Box(
        modifier = Modifier.size(size + glowWidth * 2 + 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (glowWidth > 0.dp) {
            // Outer soft glow.
            Box(
                modifier = Modifier
                    .size(size + glowWidth * 2 + 2.dp)
                    .clip(CircleShape)
                    .background(ProGoldBright.copy(alpha = 0.18f)),
            )
        }
        // Gold ring.
        Box(
            modifier = Modifier
                .size(size + ringWidth * 2)
                .clip(CircleShape)
                .background(ringBrush)
                .padding(ringWidth),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(size)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                ProfilePhotoAvatarWithGender(
                    avatarPath = avatarPath,
                    gender = gender,
                    name = name,
                    size = size,
                    accent = accent,
                    visible = visible,
                    showGenderBadge = showGenderBadge,
                )
            }
        }
        if (showSweep) {
            // Bright band that appears near the top of the ring during
            // the shine window. Cheap and non-invasive.
            Box(
                modifier = Modifier
                    .size(size + ringWidth * 2)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.55f - (sweepAngle * 0.5f).coerceAtMost(0.5f)),
                                Color.Transparent,
                            ),
                            start = androidx.compose.ui.geometry.Offset(0f, sweepAngle * 400f),
                            end = androidx.compose.ui.geometry.Offset(400f, sweepAngle * 400f + 60f),
                        ),
                    ),
            )
        }
    }
}