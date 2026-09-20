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
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.getVipEntitlements

/** Central policy point for Premium/no-ads support. */
internal object SonHarfAdPolicy {
    var adsEnabled: Boolean = true
    var isPremium: Boolean = false

    fun canShowBanner(): Boolean = adsEnabled && !isPremium && AdPrivacyManager.adsAllowed

    fun canReserveBanner(): Boolean = adsEnabled && !isPremium
}

/**
 * Thin anchored adaptive banner for eligible non-gameplay destinations.
 * Premium state is revalidated before any slot or AdView is created. If that check fails,
 * the banner fails closed so a paid user is never shown an ad because of a transient profile
 * or entitlement lookup failure. The production navigation shell separately excludes gameplay.
 */
@Composable
fun SonHarfTopAdBanner(
    isPremium: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val backend = remember { OnlineGameBackend() }
    var resolvedPremium by remember(isPremium) { mutableStateOf<Boolean?>(if (isPremium) true else null) }

    LaunchedEffect(isPremium) {
        if (isPremium) {
            resolvedPremium = true
        } else {
            resolvedPremium = runCatching { backend.getVipEntitlements().isPro }.getOrNull()
        }
    }

    // Unknown membership must not temporarily become an ad-eligible state.
    val premium = resolvedPremium ?: return
    SonHarfAdPolicy.isPremium = premium

    val adUnitId = BuildConfig.ADMOB_BANNER_AD_UNIT_ID
    val slotVisible = SonHarfAdPolicy.canReserveBanner()
    val policyAllows = SonHarfAdPolicy.canShowBanner()
    val canLoadAd = policyAllows && adUnitId.isNotBlank()

    var loaded by remember(adUnitId) { mutableStateOf(false) }
    val widthDp = configuration.screenWidthDp.coerceAtLeast(1)
    val adView = remember(context, adUnitId, canLoadAd) {
        if (!canLoadAd) {
            null
        } else {
            runCatching {
                AdView(context).apply {
                    this.adUnitId = adUnitId
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                }
            }.getOrNull()
        }
    }

    LaunchedEffect(adView, widthDp, canLoadAd) {
        val view = adView ?: run {
            loaded = false
            return@LaunchedEffect
        }
        if (!canLoadAd) {
            view.pause()
            loaded = false
            return@LaunchedEffect
        }
        runCatching {
            view.resume()
            if (!loaded) {
                view.setAdSize(
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp),
                )
                view.adListener = object : AdListener() {
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
                view.loadAd(AdRequest.Builder().build())
            }
        }.onFailure {
            loaded = false
        }
    }

    DisposableEffect(adView) {
        onDispose {
            runCatching { adView?.destroy() }
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
        if (loaded && canLoadAd && adView != null) {
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
