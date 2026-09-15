package com.sonharf.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single source of truth for rendering a user profile photo with an equipped Style frame.
 * Large-canvas stock artwork keeps its original wings/foliage/ornament geometry while the
 * avatar stays centered in the transparent opening. The parent layout still reserves the
 * actual photo size; decorative artwork may extend beyond it without shrinking the photo.
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
    val usesLargeArtworkCanvas =
        frameId?.startsWith("frame_wing_") == true ||
            frameId == "frame_flower_pink_blossom" ||
            frameId == "frame_round_golden_avatar"
    // The verified 512x512 stock artwork has an approximately 248px avatar opening.
    // 512 / 248 = 2.064516, so the photo fits the opening without clipping the wings.
    val frameSize = if (usesLargeArtworkCanvas) size * 2.064516f else size + 16.dp

    Box(
        modifier = Modifier.size(size),
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
            modifier = Modifier.requiredSize(frameSize),
        )
    }
}
