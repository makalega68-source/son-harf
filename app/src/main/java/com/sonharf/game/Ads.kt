package com.sonharf.game

import android.app.Activity
import android.content.Context
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * REKLAM POLİTİKASI
 *  - Maç ekranında SIFIR reklam. Banner yok, interstitial yok.
 *  - Banner sadece lobi / mağaza / profil üstünde.
 *  - Ödüllü video tamamen isteğe bağlı.
 *  - PRO üyede hiçbir reklam gösterilmez.
 */
object Ads {
    private var rewarded: RewardedAd? = null

    fun preloadRewarded(ctx: Context) {
        RewardedAd.load(ctx, Config.ADMOB_REWARDED_ID, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewarded = ad }
                override fun onAdFailedToLoad(e: LoadAdError) { rewarded = null }
            })
    }

    fun showRewarded(activity: Activity, onReward: () -> Unit) {
        val ad = rewarded
        if (ad == null) { preloadRewarded(activity); onReward(); return }
        ad.show(activity) { onReward() }
        rewarded = null
        preloadRewarded(activity)
    }
}

@Composable
fun TopBanner(isPro: Boolean, modifier: Modifier = Modifier) {
    if (isPro) return
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            FrameLayout(ctx).apply {
                val view = AdView(ctx)
                view.adUnitId = Config.ADMOB_BANNER_ID
                view.setAdSize(AdSize.BANNER)
                view.loadAd(AdRequest.Builder().build())
                addView(view)
            }
        }
    )
}
