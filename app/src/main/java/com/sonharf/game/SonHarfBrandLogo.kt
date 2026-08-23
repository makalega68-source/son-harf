package com.sonharf.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SonHarfBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
) {
    Image(
        painter = painterResource(R.drawable.son_harf_brand_logo),
        contentDescription = "Son Harf",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp)),
    )
}
