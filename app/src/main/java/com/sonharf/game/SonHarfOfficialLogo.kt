package com.sonharf.game

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/** Shared Kelime Tahtı brand mark used by first-run language selection and the home header. */
@Composable
fun SonHarfOfficialLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.kelime_tahti_logo_latest),
        contentDescription = sh("Kelime Tahtı logosu", "Kelime Tahtı logo"),
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}
