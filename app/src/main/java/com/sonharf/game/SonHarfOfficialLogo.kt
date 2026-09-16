package com.sonharf.game

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/** Shared brand mark used by first-run language selection and brand surfaces.
 *  Now sources the user-supplied app_logo.webp so intro screen + launcher
 *  icon stay in sync. */
@Composable
fun SonHarfOfficialLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.app_logo),
        contentDescription = sh("Kelime Kuşatması logosu", "Kelime Kuşatması logo"),
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}
