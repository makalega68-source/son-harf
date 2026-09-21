package com.sonharf.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sonharf.game.data.OnlineGameBackend

/** Debug-source-only real Home renderer. Never included in release builds. */
class HomeRealQaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                RealHomeQaScreen()
            }
        }
    }
}

@Composable
private fun RealHomeQaScreen() {
    val backend = remember { OnlineGameBackend() }
    var siegeLanguage by remember { mutableStateOf("tr") }
    var lastLetterLanguage by remember { mutableStateOf("tr") }
    var letterPathLanguage by remember { mutableStateOf("tr") }
    var seriesLanguage by remember { mutableStateOf("tr") }

    Scaffold(
        containerColor = Color(0xFFF4F7F6),
        bottomBar = {
            ModernHomeBottomNavigation(
                onHome = {},
                onSocial = {},
                onShop = {},
                onProfile = {},
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .systemBarsPadding(),
        ) {
            ModernAdultHome(
                backend = backend,
                profile = null,
                siegeLanguage = siegeLanguage,
                lastLetterLanguage = lastLetterLanguage,
                letterPathLanguage = letterPathLanguage,
                seriesLanguage = seriesLanguage,
                seriesAccess = false,
                onSiegeLanguage = { siegeLanguage = it },
                onLastLetterLanguage = { lastLetterLanguage = it },
                onLetterPathLanguage = { letterPathLanguage = it },
                onSeriesLanguage = { seriesLanguage = it },
                onSiege = {},
                onLastLetter = {},
                onLetterPath = {},
                onSeries = {},
                onCompete = {},
                onProfile = {},
                onSocial = {},
                onPro = {},
                onCollection = {},
            )
        }
    }
}
