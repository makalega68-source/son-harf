package com.sonharf.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * Named-argument adapter used only by the Store preview. It delegates to the existing
 * profile renderer, so Store previews exercise the same avatar/photo path as real profiles.
 */
@Composable
internal fun ProfilePhotoAvatarWithGender(
    avatarPath: String?,
    gender: String?,
    displayName: String,
    size: Dp,
    accent: Color,
    visible: Boolean,
    storePreview: Unit = Unit,
) {
    @Suppress("UNUSED_VARIABLE")
    val keepSignatureDistinct = storePreview
    ProfilePhotoAvatarWithGender(
        avatarPath = avatarPath,
        gender = gender,
        name = displayName,
        size = size,
        accent = accent,
        visible = visible,
    )
}
