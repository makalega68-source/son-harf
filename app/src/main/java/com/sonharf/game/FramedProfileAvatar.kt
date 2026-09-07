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
 * Profile photos intentionally use the rounded-rectangle renderer so purchased frame artwork and
 * profile imagery share the same non-circular geometry across the app.
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
        ProfilePhotoAvatarRectWithGender(
            avatarPath = avatarPath,
            gender = gender,
            name = name,
            width = size,
            height = size,
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
