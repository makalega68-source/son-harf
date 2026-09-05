package com.sonharf.game

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    fun canReserveBanner(isGameplay: Boolean): Boolean =
        adsEnabled && !isPremium && !isGameplay
}

/**
 * Anchored adaptive banner for non-game surfaces only.
 * The view is retained while navigating between eligible screens, paused/hidden during gameplay,
 * and reserves a thin, labelled slot while the network banner is loading.
 */
@Composable
fun SonHarfTopAdBanner(
    visible: Boolean = true,
    isPremium: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    SonHarfAdPolicy.isPremium = isPremium

    val adUnitId = BuildConfig.ADMOB_BANNER_AD_UNIT_ID
    val slotVisible = SonHarfAdPolicy.canReserveBanner(isGameplay = !visible)
    val policyAllows = SonHarfAdPolicy.canShowBanner(isGameplay = !visible)
    val canLoadAd = policyAllows && adUnitId.isNotBlank()

    var loaded by remember(adUnitId) { mutableStateOf(false) }
    val widthDp = configuration.screenWidthDp.coerceAtLeast(1)
    val adView = remember(context, adUnitId) {
        AdView(context).apply {
            this.adUnitId = adUnitId
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
    }

    LaunchedEffect(adView, widthDp, canLoadAd) {
        if (!canLoadAd) {
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
            adView.destroy()
        }
    }

    if (!slotVisible) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(50.dp)
            .background(Color(0xFFF1F4F8)),
        contentAlignment = Alignment.Center,
    ) {
        if (loaded && canLoadAd) {
            AndroidView(
                factory = { adView },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text(
                text = sh("REKLAM", "AD"),
                color = Color(0xFF718096),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Clean hooks; wire these into the product analytics provider when one is selected. */
internal object SonHarfBannerAnalytics {
    fun onImpressionReady() = Unit
    fun onClick() = Unit
}
