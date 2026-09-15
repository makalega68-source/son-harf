package com.sonharf.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * Explicit-frame entry point for profile surfaces. The shared avatar renderer owns sizing and keeps
 * the cropped profile photo centered inside exactly one equipped frame.
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
    ProfilePhotoAvatarWithGender(
        avatarPath = avatarPath,
        gender = gender,
        name = name,
        size = size,
        accent = accent,
        visible = visible,
        showGenderBadge = showGenderBadge,
        frameId = frameId,
    )
}
