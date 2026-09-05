package com.sonharf.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single source of truth for rendering a user profile photo with an equipped Style frame.
 * The avatar remains centered and visible even when no frame is equipped or artwork falls back.
 */
@Composable
internal fun FramedProfilePhotoAvatar(
    avatarPath: String?,
    gender: String?,
    name: String,
    size: Dp,
    frameId: String?,
    accent: Color = SonHarfCyan,
    visible: Boolean = true,
    showGenderBadge: Boolean = false,
) {
    val frameSize = size + 16.dp
    Box(
        modifier = Modifier.size(frameSize),
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
        PurchasedProfileFrameOverlay(
            frameId = frameId,
            modifier = Modifier.size(frameSize),
        )
    }
}
