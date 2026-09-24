package com.sonharf.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Premium game-style icon: a glossy gradient badge with a white glyph, used across every page
 * (navigation, cards, rows, empty states) so the UI reads as one polished game.
 */
@Composable
internal fun GameBadgeIcon(
    icon: ImageVector,
    accent: Color,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val shape = RoundedCornerShape(size * 0.32f)
    val top = lerp(accent, Color.White, .28f)
    val bottom = lerp(accent, Color.Black, .18f)
    Box(
        modifier
            .size(size)
            .shadow(size * 0.12f, shape, clip = false, ambientColor = accent, spotColor = accent)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(top, accent, bottom)))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = .55f), bottom.copy(alpha = .4f))), shape),
        contentAlignment = Alignment.Center,
    ) {
        // Soft top gloss.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .fillMaxHeight(.48f)
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = .30f), Color.Transparent))),
        )
        Icon(icon, contentDescription, tint = Color.White, modifier = Modifier.size(size * 0.56f))
    }
}

/** Gold Son Coin used by every currency display. */
@Composable
internal fun GameCoinIcon(size: Dp = 16.dp, modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.ic_game_coin), contentDescription = null, modifier = modifier.size(size))
}
