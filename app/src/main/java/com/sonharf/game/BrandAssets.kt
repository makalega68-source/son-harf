package com.sonharf.game

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Locale

@DrawableRes
internal fun sonHarfBrandLogoForCurrentLocale(): Int =
    if (Locale.getDefault().language.equals("en", ignoreCase = true)) R.drawable.logo_last_letter_en
    else R.drawable.logo_son_harf_tr

@Composable
internal fun SonHarfBrandRoot(content: @Composable () -> Unit) {
    var splashVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1150L)
        splashVisible = false
    }

    if (splashVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color(0xFFF9F6F0)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(sonHarfBrandLogoForCurrentLocale()),
                contentDescription = if (Locale.getDefault().language.equals("en", true)) "Last Letter word game" else "Son Harf kelime oyunu",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            )
        }
    } else {
        content()
    }
}
