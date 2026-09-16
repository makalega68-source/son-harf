package com.sonharf.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * Compatibility wrapper kept so existing call sites continue to compile.
 * Profile/avatar frames are intentionally disabled across the entire app.
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
    )
}
