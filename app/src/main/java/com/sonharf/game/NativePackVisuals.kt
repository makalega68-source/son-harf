package com.sonharf.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * Selected, recoloured visual fragments from the licensed UI/VFX packs.
 *
 * The original Unity prefab/shader runtime is deliberately not embedded in this Native Android app.
 * Only tiny raster assets are used as decorative layers so Compose remains the single UI runtime.
 */
@Composable
internal fun PackageLeagueBadge(
    modifier: Modifier = Modifier.size(28.dp),
    tint: Color = SonHarfTheme.SecondaryAccent,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Legacy package: retained only as a low-opacity prestige/rank frame.
        Image(
            painter = painterResource(R.drawable.ui_legacy_rank_frame),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(.28f),
            colorFilter = ColorFilter.tint(SonHarfTheme.PremiumGold),
        )
        // Current Casual Game UI #02 package: primary ranking glyph.
        Image(
            painter = painterResource(R.drawable.ui_ranking_icon),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(.94f),
            colorFilter = ColorFilter.tint(tint),
        )
    }
}

@Composable
internal fun NativePackBackdrop(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        // Adapted from the action/VFX package as a static, low-overdraw Compose accent.
        Image(
            painter = painterResource(R.drawable.vfx_twinkle),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopEnd).size(104.dp).alpha(.055f),
            colorFilter = ColorFilter.tint(SonHarfTheme.SoftBlue),
        )
        Image(
            painter = painterResource(R.drawable.vfx_twinkle),
            contentDescription = null,
            modifier = Modifier.align(Alignment.BottomStart).size(72.dp).alpha(.035f),
            colorFilter = ColorFilter.tint(SonHarfTheme.Lavender),
        )
    }
}
