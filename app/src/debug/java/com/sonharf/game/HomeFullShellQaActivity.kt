package com.sonharf.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme

/** Debug-source-only full active shell renderer. Never included in release builds. */
class HomeFullShellQaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SonHarfCosmetics.restore(this)
        setContent {
            MaterialTheme {
                PremiumAdultApp(onSignedOut = {})
            }
        }
    }
}
