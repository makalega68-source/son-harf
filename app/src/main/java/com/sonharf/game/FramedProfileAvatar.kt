package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Profile avatar renderer kept source-compatible with older call sites.
 * Legacy frame artwork is retired; the only trim we still draw is a thin ring
 * that marks Pro members in gold. Everyone else gets a soft grey ring so the
 * avatar still looks framed without introducing paid cosmetics.
 */
private val ProGoldFrame = Color(0xFFD4AF37)
private val StandardGreyFrame = Color(0xFFBDBDBD)

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
    val ringColor = if (isPro) ProGoldFrame else StandardGreyFrame
    val ringWidth = if (isPro) 3.dp else 2.dp
    Box(
        modifier = Modifier.border(BorderStroke(ringWidth, ringColor), CircleShape),
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
