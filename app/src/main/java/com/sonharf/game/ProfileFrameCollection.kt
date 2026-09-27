package com.sonharf.game

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.sonharf.game.billing.ProductCatalog

/**
 * Live profile frames (collection v2). Simple rings are Son Coin items in the shop; the ornate
 * ones are permanent Google Play products whose store id equals the Play product id, granted to
 * user_inventory by the purchase verifier. Every owned frame is equipped through equip_shop_item.
 */
internal object ProfileFrameCollection {
    data class Frame(
        val id: String,
        @DrawableRes val drawable: Int,
        val nameTr: String,
        val nameEn: String,
        /** Non-null for frames sold for real money through Google Play. */
        val playProductId: String? = null,
    )

    val coinFrames = listOf(
        Frame("frame_round_starter_blue", R.drawable.profile_frame_round_starter_blue, "Başlangıç Mavi", "Starter Blue"),
        Frame("frame_round_starter_neutral", R.drawable.profile_frame_round_starter_neutral, "Başlangıç Nötr", "Starter Neutral"),
        Frame("frame_round_pearl", R.drawable.profile_frame_round_pearl, "Beyaz İnci", "White Pearl"),
        Frame("frame_round_ocean", R.drawable.profile_frame_round_ocean, "Okyanus", "Ocean"),
        Frame("frame_round_rose", R.drawable.profile_frame_round_rose, "Gül Pembesi", "Rose Pink"),
        Frame("frame_round_lilac", R.drawable.profile_frame_round_lilac, "Lavanta", "Lavender"),
        Frame("frame_round_botanic", R.drawable.profile_frame_round_botanic, "Botanik Yeşil", "Botanic Green"),
    )

    val premiumFrames = listOf(
        Frame(ProductCatalog.PROFILE_FRAME_ROYAL_GOLD, R.drawable.profile_frame_royal_gold, "Kraliyet Altın", "Royal Gold", ProductCatalog.PROFILE_FRAME_ROYAL_GOLD),
        Frame(ProductCatalog.PROFILE_FRAME_GOLD_CREST, R.drawable.profile_frame_premium_gold, "Altın Arma", "Gold Crest", ProductCatalog.PROFILE_FRAME_GOLD_CREST),
        Frame(ProductCatalog.PROFILE_FRAME_EMERALD, R.drawable.profile_frame_premium_emerald, "Zümrüt Arma", "Emerald Crest", ProductCatalog.PROFILE_FRAME_EMERALD),
        Frame(ProductCatalog.PROFILE_FRAME_AMETHYST, R.drawable.profile_frame_premium_amethyst, "Ametist Arma", "Amethyst Crest", ProductCatalog.PROFILE_FRAME_AMETHYST),
        Frame(ProductCatalog.PROFILE_FRAME_SAKURA, R.drawable.profile_frame_premium_sakura, "Sakura Çelengi", "Sakura Wreath", ProductCatalog.PROFILE_FRAME_SAKURA),
        Frame(ProductCatalog.PROFILE_FRAME_SAPPHIRE, R.drawable.profile_frame_premium_sapphire, "Safir Arma", "Sapphire Crest", ProductCatalog.PROFILE_FRAME_SAPPHIRE),
    )

    val all = coinFrames + premiumFrames
    val coinIds = coinFrames.map { it.id }.toSet()
    val allIds = all.map { it.id }.toSet()
    val premiumProductIds = premiumFrames.mapNotNull { it.playProductId }

    fun find(id: String?): Frame? = all.firstOrNull { it.id == id }
}

/**
 * Draws a frame around content of [size] without changing layout: the art is laid out larger
 * than its slot and simply overflows it, so headers and rows keep their measured height.
 */
@Composable
internal fun ProfileFrameArt(frameId: String?, size: Dp, modifier: Modifier = Modifier) {
    val frame = ProfileFrameCollection.find(frameId) ?: return
    Image(
        painter = painterResource(frame.drawable),
        contentDescription = null,
        modifier = modifier.requiredSize(size * 1.42f),
        contentScale = ContentScale.Fit,
    )
}
