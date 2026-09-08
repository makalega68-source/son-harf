package com.sonharf.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

class MainActivity : ComponentActivity() {

    private lateinit var consent: ConsentInformation
    private var billing: Billing? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Google UMP onay akışı
        consent = UserMessagingPlatform.getConsentInformation(this)
        consent.requestConsentInfoUpdate(
            this, ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) {
                    Ads.preloadRewarded(this)
                }
            },
            { Ads.preloadRewarded(this) }
        )

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val vm: GameViewModel = viewModel()

                LaunchedEffect(Unit) {
                    billing = Billing(this@MainActivity) { days -> vm.grantPro(days) }
                    billing?.connect()
                }

                if (vm.loading) {
                    Box(Modifier.fillMaxSize().background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center) {
                        Text("Son Harf", color = Color.White, fontSize = 26.sp)
                    }
                } else when (vm.screen) {
                    Screen.LOBBY -> LobbyScreen(vm) { billing?.buyPro(this) }
                    Screen.GAME -> GameScreen(vm)
                    Screen.RESULT -> ResultScreen(vm) {
                        Ads.showRewarded(this) { vm.claimDoubleReward() }
                    }
                    Screen.STORE -> StoreScreen(vm)
                    Screen.CREDITS -> CreditsScreen(vm)
                }
            }
        }
    }
}
