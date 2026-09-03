package com.sonharf.game

import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/** Central policy point for future Premium/no-ads support. */
internal object SonHarfAdPolicy {
    var adsEnabled: Boolean = true
    var isPremium: Boolean = false

    fun canShowBanner(isGameplay: Boolean): Boolean =
        adsEnabled && !isPremium && !isGameplay && AdPrivacyManager.adsAllowed
}

/**
 * Anchored adaptive banner for non-game surfaces only.
 * The view is retained while navigating between eligible screens, paused/hidden during gameplay,
 * and occupies no layout space until a banner has actually loaded.
 */
@Composable
fun SonHarfTopAdBanner(
    visible: Boolean,
    isPremium: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    SonHarfAdPolicy.isPremium = isPremium

    val adUnitId = BuildConfig.ADMOB_BANNER_AD_UNIT_ID
    val policyAllows = SonHarfAdPolicy.canShowBanner(isGameplay = !visible)
    if (adUnitId.isBlank()) return

    var loaded by remember(adUnitId) { mutableStateOf(false) }
    val widthDp = configuration.screenWidthDp.coerceAtLeast(1)
    val adView = remember(context, adUnitId) {
        AdView(context.applicationContext).apply {
            this.adUnitId = adUnitId
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
    }

    LaunchedEffect(adView, widthDp, policyAllows) {
        if (!policyAllows) {
            adView.pause()
            return@LaunchedEffect
        }
        adView.resume()
        if (!loaded) {
            adView.setAdSize(
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp),
            )
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    loaded = true
                    SonHarfBannerAnalytics.onImpressionReady()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loaded = false
                }

                override fun onAdClicked() {
                    SonHarfBannerAnalytics.onClick()
                }
            }
            adView.loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(adView) {
        onDispose {
            adView.adListener = null
            adView.destroy()
        }
    }

    if (!policyAllows || !loaded) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { adView },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Clean hooks; wire these into the product analytics provider when one is selected. */
internal object SonHarfBannerAnalytics {
    fun onImpressionReady() = Unit
    fun onClick() = Unit
}
