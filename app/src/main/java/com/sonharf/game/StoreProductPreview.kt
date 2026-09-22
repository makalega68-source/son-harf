package com.sonharf.game

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sonharf.game.data.ShopItemDto

/**
 * Product art is intentionally text-free and transparent. Product names, prices and ownership
 * states remain real Compose UI text outside the artwork. Profile frames keep using their actual
 * packaged runtime assets.
 */
@Composable
internal fun StoreProductPreview(
    item: ShopItemDto,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
) {
    val shape = if (expanded) GameShapes.Large else GameShapes.Medium
    Box(
        modifier = modifier.clip(shape),
        contentAlignment = Alignment.Center,
    ) {
        when {
            item.kind == "profile_frame" -> RealFramePreview(item.id, expanded)
            storeArtworkRes(item.id) != null -> {
                Image(
                    painter = painterResource(storeArtworkRes(item.id)!!),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (expanded) 7.dp else 3.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            else -> PremiumArtworkFallback(expanded)
        }
    }
}

@DrawableRes
private fun storeArtworkRes(itemId: String): Int? = when (itemId) {
    "name_cyan" -> R.drawable.store_art_name_cyan
    "name_sapphire" -> R.drawable.store_art_name_sapphire
    "name_amethyst" -> R.drawable.store_art_name_amethyst
    "name_aurelia" -> R.drawable.store_art_name_aurelia
    "keyboard_crystal" -> R.drawable.store_art_keyboard_crystal
    "keyboard_obsidian" -> R.drawable.store_art_keyboard_obsidian
    "keyboard_midnight" -> R.drawable.store_art_keyboard_midnight
    "keyboard_black_gold" -> R.drawable.store_art_keyboard_black_gold
    "keyboard_premium_white" -> R.drawable.store_art_keyboard_premium_white
    "theme_black", "theme_dark_arena" -> R.drawable.store_art_theme_black
    "victory_crown" -> R.drawable.store_art_victory_crown
    "emoji_vip" -> R.drawable.store_art_emoji_vip
    else -> null
}

@Composable
private fun RealFramePreview(
    frameId: String,
    expanded: Boolean,
) {
    val frameSize = if (expanded) 108.dp else 66.dp
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (expanded) 8.dp else 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(frameSize * .72f),
            shape = CircleShape,
            color = GameColors.ElevatedBackground,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = null,
                    modifier = Modifier.size(frameSize * .40f),
                    tint = GameColors.TextTertiary,
                )
            }
        }
        PurchasedProfileFrameOverlay(
            frameId = frameId,
            modifier = Modifier.size(frameSize),
        )
    }
}

@Composable
private fun PremiumArtworkFallback(expanded: Boolean) {
    Surface(
        modifier = Modifier.size(if (expanded) 72.dp else 46.dp),
        shape = CircleShape,
        color = Color.Transparent,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(if (expanded) 38.dp else 26.dp),
                tint = GameColors.Lavender,
            )
        }
    }
}
