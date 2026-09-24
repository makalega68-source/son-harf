package com.sonharf.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/** Debug-only host used by CI to render the real production Compose shell for screenshots. */
class UiScreenshotActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SonHarfUiState.language = "tr"
        SonHarfCosmetics.restore(this)
        setContent {
            PremiumUnifiedProApp(onSignedOut = {})
        }
    }
}
