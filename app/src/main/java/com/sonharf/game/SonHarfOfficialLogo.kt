package com.sonharf.game

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/**
 * Main Kelime Tahtı wordmark.
 *
 * The legacy function name is intentionally retained so existing startup/navigation
 * call sites remain source-compatible while the visible product brand uses the
 * approved Kelime Tahtı artwork.
 */
@Composable
fun SonHarfOfficialLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.kelime_tahti_logo),
        contentDescription = sh("Kelime Tahtı logosu", "Kelime Tahtı logo"),
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}
