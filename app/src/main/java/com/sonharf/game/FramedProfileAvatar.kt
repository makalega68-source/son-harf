package com.sonharf.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * Profile avatar renderer kept source-compatible with older call sites.
 * Profile frames are intentionally disabled product-wide; frameId is ignored so existing
 * profiles cannot accidentally render legacy frame artwork while stored data is retired.
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
    @Suppress("UNUSED_VARIABLE")
    val legacyFrameId = frameId
    ProfilePhotoAvatarWithGender(
        avatarPath = avatarPath,
        gender = gender,
        name = name,
        size = size,
        accent = accent,
        visible = visible,
        showGenderBadge = showGenderBadge,
        modifier = Modifier,
    )
}
