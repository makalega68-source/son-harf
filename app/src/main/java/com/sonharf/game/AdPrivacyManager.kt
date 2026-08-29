package com.sonharf.game

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Central Google UMP privacy gate.
 *
 * The app stays usable when consent cannot be collected; only ad requests are gated.
 * Consent information is refreshed on every app launch from MainActivity.
 */
object AdPrivacyManager {
    private var consentInformation: ConsentInformation? = null
    private var mobileAdsInitialized = false

    var adsAllowed by mutableStateOf(false)
        private set

    var privacyOptionsRequired by mutableStateOf(false)
        private set

    var consentRequestFinished by mutableStateOf(false)
        private set

    fun requestConsent(activity: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation = info
        consentRequestFinished = false

        val parameters = ConsentRequestParameters.Builder().build()
        info.requestConsentInfoUpdate(
            activity,
            parameters,
            {
                refreshState(activity.applicationContext)
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    consentRequestFinished = true
                    refreshState(activity.applicationContext)
                }
            },
            {
                // A previous valid consent state can still allow ads after a network/update error.
                consentRequestFinished = true
                refreshState(activity.applicationContext)
            },
        )

        // UMP permits checking the previous-session state immediately after requesting an update.
        refreshState(activity.applicationContext)
    }

    fun showPrivacyOptions(
        activity: Activity,
        onComplete: (Boolean) -> Unit = {},
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            refreshState(activity.applicationContext)
            onComplete(error == null)
        }
    }

    fun findActivity(context: Context): Activity? = when (context) {
        is Activity -> context
        is ContextWrapper -> findActivity(context.baseContext)
        else -> null
    }

    private fun refreshState(context: Context) {
        val info = consentInformation ?: return
        privacyOptionsRequired =
            info.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

        adsAllowed = info.canRequestAds()
        if (adsAllowed) ensureMobileAdsInitialized(context)
    }

    @Synchronized
    private fun ensureMobileAdsInitialized(context: Context) {
        if (mobileAdsInitialized) return
        mobileAdsInitialized = true
        MobileAds.initialize(context.applicationContext) {}
    }
}
