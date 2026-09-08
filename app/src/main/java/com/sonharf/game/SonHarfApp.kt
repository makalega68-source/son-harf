package com.sonharf.game

import android.app.Application
import com.google.android.gms.ads.MobileAds

class SonHarfApp : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching { MobileAds.initialize(this) {} }
        Supa.boot(this)
    }
}
