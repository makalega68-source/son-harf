package com.sonharf.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** Profile/avatar frames are retired from the product. */
internal object PurchasedFrameCatalog {
    const val STARTER_BLUE = "frame_round_starter_blue"
    const val STARTER_PINK = "frame_round_starter_pink"
    const val STARTER_NEUTRAL = "frame_round_starter_neutral"
    const val OCEAN = "frame_round_ocean"
    const val BOTANIC = "frame_round_botanic"
    const val LILAC = "frame_round_lilac"
    const val ROSE = "frame_round_rose"
    const val GOLDEN_AVATAR = "frame_round_golden_avatar"
    const val WING_SILVER = "frame_wing_silver"
    const val WING_GOLD = "frame_wing_gold"
    const val WING_AURORA = "frame_wing_aurora"
    const val WING_PINK = "frame_wing_pink"
    const val WING_BLUE = "frame_wing_blue"
    const val FLOWER_PINK_BLOSSOM = "frame_flower_pink_blossom"
    const val RED = "frame_asset_red"
    const val GREEN = "frame_asset_green"
    const val MINT = "frame_asset_mint"
    const val PURPLE = "frame_asset_purple"
    const val GOLD = "frame_asset_gold"
    const val GOLD_CROWN = "frame_asset_gold_crown"
    const val CHRISTMAS = "frame_asset_christmas"
    const val HALLOWEEN = "frame_asset_halloween"

    val ids: Set<String> = emptySet()
    fun isVectorFrame(id: String?): Boolean = false
    fun drawable(id: String?): Int? = null
    fun accent(id: String?): Color = Color.Transparent
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

@Composable
internal fun PurchasedStyleShopContent(
    modifier: Modifier = Modifier,
) = Unit
