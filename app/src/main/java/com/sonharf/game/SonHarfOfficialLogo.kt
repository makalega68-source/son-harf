package com.sonharf.game

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/** Shared Kelime Kuşatması brand mark used by first-run language selection and brand surfaces. */
@Composable
fun SonHarfOfficialLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.kelime_kusatma_logo_hd),
        contentDescription = sh("Kelime Kuşatması logosu", "Kelime Kuşatması logo"),
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}
