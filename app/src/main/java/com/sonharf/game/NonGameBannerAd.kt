package com.sonharf.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

/**
 * Thin banner used only by non-game navigation surfaces.
 * Gameplay screens decide visibility at the app-shell level and never render this component.
 */
@Composable
fun SonHarfTopAdBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val adUnitId = BuildConfig.ADMOB_BANNER_AD_UNIT_ID.ifBlank {
        if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/6300978111" else ""
    }
    if (adUnitId.isBlank()) return

    val adView = remember(context, adUnitId) {
        MobileAds.initialize(context.applicationContext) {}
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            this.adUnitId = adUnitId
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Color(0xFF071B2F)),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { adView },
            modifier = Modifier.wrapContentSize(),
        )
    }
}
