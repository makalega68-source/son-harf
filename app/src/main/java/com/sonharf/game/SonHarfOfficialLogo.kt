package com.sonharf.game

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/** User-facing official Son Harf logo backed by the approved WebP asset. */
@Composable
fun SonHarfOfficialLogo(
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(id = R.drawable.son_harf_brand_logo),
        contentDescription = "Son Harf",
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}
