package com.sonharf.game.ui.premium

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sonharf.game.R

/**
 * G4.7 Pro badge. Sized to 1.2x the surrounding text (default 18dp).
 * Callers should place it directly after a Pro user's display name.
 */
@Composable
fun ProBadge(
    modifier: Modifier = Modifier,
    sizeDp: Int = 18,
) {
    Image(
        painter = painterResource(R.drawable.ic_pro_badge),
        contentDescription = "PRO",
        modifier = modifier.size((sizeDp * 2).dp, sizeDp.dp),
    )
}
