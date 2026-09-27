package com.sonharf.game

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.billing.ProductCatalog

/**
 * Live profile frames (collection v2). Simple rings are Son Coin items in the shop; the ornate
 * ones are permanent Google Play products, and the ruby-crowned Royal Gold is reserved for PRO:
 * the server grants and equips it the moment a player becomes PRO and takes it back when PRO ends.
 * A Play frame's store id equals its Play product id; the verifier grants it to
 * user_inventory. Every owned frame is equipped through equip_shop_item.
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
        Frame(ProductCatalog.PROFILE_FRAME_GOLD_CREST, R.drawable.profile_frame_premium_gold, "Altın Arma", "Gold Crest", ProductCatalog.PROFILE_FRAME_GOLD_CREST),
        Frame(ProductCatalog.PROFILE_FRAME_EMERALD, R.drawable.profile_frame_premium_emerald, "Zümrüt Arma", "Emerald Crest", ProductCatalog.PROFILE_FRAME_EMERALD),
        Frame(ProductCatalog.PROFILE_FRAME_AMETHYST, R.drawable.profile_frame_premium_amethyst, "Ametist Arma", "Amethyst Crest", ProductCatalog.PROFILE_FRAME_AMETHYST),
        Frame(ProductCatalog.PROFILE_FRAME_SAKURA, R.drawable.profile_frame_premium_sakura, "Sakura Çelengi", "Sakura Wreath", ProductCatalog.PROFILE_FRAME_SAKURA),
        Frame(ProductCatalog.PROFILE_FRAME_SAPPHIRE, R.drawable.profile_frame_premium_sapphire, "Safir Arma", "Sapphire Crest", ProductCatalog.PROFILE_FRAME_SAPPHIRE),
    )

    /** PRO-only frame: never sold, granted with PRO membership. */
    val proFrame = Frame("frame_round_golden_avatar", R.drawable.profile_frame_round_golden_avatar, "PRO Kraliyet Altın", "PRO Royal Gold")

    val all = coinFrames + premiumFrames + proFrame
    val coinIds = coinFrames.map { it.id }.toSet()
    val allIds = all.map { it.id }.toSet()
    val premiumProductIds = premiumFrames.mapNotNull { it.playProductId }

    fun find(id: String?): Frame? = all.firstOrNull { it.id == id }
}

/**
 * Draws a frame around content of [size] without changing layout: the art is laid out larger
 * than its slot and simply overflows it, so headers and rows keep their measured height.
 * The PRO frame carries a "PRO" plate on its bottom jewel, so everyone sees how it was earned.
 */
@Composable
internal fun ProfileFrameArt(frameId: String?, size: Dp, modifier: Modifier = Modifier) {
    val frame = ProfileFrameCollection.find(frameId) ?: return
    Box(modifier.requiredSize(size * 1.42f), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(frame.drawable),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
        if (frame == ProfileFrameCollection.proFrame) ProFramePlate(size)
    }
}

/** Ruby-and-gold "PRO" plate sitting on the PRO frame's bottom jewel; scales with the avatar. */
@Composable
private fun ProFramePlate(size: Dp) {
    val scale = size.value / 100f
    Box(
        Modifier
            .offset(y = size * .47f)
            .border(
                (1.5f * scale).coerceAtLeast(1f).dp,
                Brush.verticalGradient(listOf(Color(0xFFFFF1B8), Color(0xFFD4A21F), Color(0xFF8A5A0B))),
                RoundedCornerShape(50),
            )
            .background(
                Brush.verticalGradient(listOf(Color(0xFFD7263D), Color(0xFF8E0F24))),
                RoundedCornerShape(50),
            )
            .padding(horizontal = (9f * scale).dp, vertical = (1.5f * scale).dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "PRO",
            color = Color(0xFFFFE6A0),
            fontSize = (13f * scale).coerceAtLeast(6f).sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (1.2f * scale).sp,
            maxLines = 1,
        )
    }
}
