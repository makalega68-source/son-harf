package com.sonharf.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Profile/avatar frames are retired from the product. */
internal object PurchasedFrameCatalog {
    const val GOLDEN_AVATAR = "frame_round_golden_avatar"
    const val WING_SILVER = "frame_wing_silver"
    const val WING_GOLD = "frame_wing_gold"
    const val WING_AURORA = "frame_wing_aurora"
    const val WING_PINK = "frame_wing_pink"
    const val WING_BLUE = "frame_wing_blue"
    const val FLOWER_PINK_BLOSSOM = "frame_flower_pink_blossom"

    val ids: Set<String> = emptySet()
    fun drawable(id: String?): Int? = null
}

@Composable
internal fun PurchasedProfileFrameOverlay(
    frameId: String?,
    modifier: Modifier = Modifier,
) = Unit

@Composable
internal fun PurchasedProfileFramesSection(
    modifier: Modifier = Modifier,
    onFrameEquipped: ((String?) -> Unit)? = null,
) = Unit
