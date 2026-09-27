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
 * A frame from the live collection (coin rings or premium crests) is drawn around the photo
 * without changing the avatar's layout size. Without one, Pro members get a thin gold ring
 * and everyone else a soft grey ring.
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
    val legacyFrameId = frameId
    val framed = ProfileFrameCollection.find(legacyFrameId) != null
    val ringColor = if (isPro) ProGoldFrame else StandardGreyFrame
    val ringWidth = if (isPro) 3.dp else 2.dp
    Box(
        modifier = if (framed) Modifier else Modifier.border(BorderStroke(ringWidth, ringColor), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        ProfilePhotoAvatarWithGender(
            avatarPath = avatarPath,
            gender = gender,
            name = name,
            size = size,
            accent = accent,
            visible = visible,
            showGenderBadge = showGenderBadge && !framed,
        )
        if (framed) ProfileFrameArt(frameId = legacyFrameId, size = size)
    }
}
