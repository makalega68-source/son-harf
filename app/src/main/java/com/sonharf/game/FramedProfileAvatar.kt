package com.sonharf.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * Runtime avatar frame wrapper.
 * Historical frame IDs remain retired; only Profile Frames V2 paid IDs may override
 * the automatic standard/PRO frame.
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
    showGenderBadge: Boolean = true,
    isPro: Boolean = false,
) {
    // Keep the historical compatibility marker used by the retirement regression contract.
    val legacyFrameId = frameId
    val activePaidFrame = legacyFrameId?.takeIf { it in ProfileFrameV2Catalog.paidIds }
    ProfileFrameAvatarPathV2(
        avatarPath = avatarPath,
        gender = gender,
        name = name,
        outerSize = size,
        equippedPaidFrameId = activePaidFrame,
        isPro = isPro,
        accent = accent,
        visible = visible,
        showGenderBadge = showGenderBadge,
    )
}
