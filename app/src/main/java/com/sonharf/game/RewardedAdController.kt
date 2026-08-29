package com.sonharf.game

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.util.UUID

/** Optional rewarded ads. The thin non-game banner is handled separately by the app shell. */
class RewardedAdController(private val context: Context) {
    private var rewardedAd: RewardedAd? = null
    private var loading = false
    var ready: Boolean = false
        private set

    fun load(onState: (() -> Unit)? = null) {
        if (!AdPrivacyManager.adsAllowed) {
            rewardedAd = null
            loading = false
            ready = false
            onState?.invoke()
            return
        }
        if (loading || rewardedAd != null) {
            onState?.invoke()
            return
        }
        loading = true
        RewardedAd.load(
            context.applicationContext,
            BuildConfig.ADMOB_REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    loading = false
                    ready = true
                    onState?.invoke()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    loading = false
                    ready = false
                    onState?.invoke()
                }
            },
        )
    }

    fun clear() {
        rewardedAd = null
        loading = false
        ready = false
    }

    fun show(
        activity: Activity,
        onEarned: (String) -> Unit,
        onUnavailable: () -> Unit,
        onClosed: (() -> Unit)? = null,
    ) {
        if (!AdPrivacyManager.adsAllowed) {
            onUnavailable()
            return
        }
        val ad = rewardedAd
        if (ad == null) {
            onUnavailable()
            load()
            return
        }
        rewardedAd = null
        ready = false
        val responseId = ad.responseInfo.responseId.orEmpty().ifBlank { "reward-${UUID.randomUUID()}" }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                onClosed?.invoke()
                load()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                onUnavailable()
                onClosed?.invoke()
                load()
            }
        }
        ad.show(activity) { onEarned(responseId) }
    }
}
