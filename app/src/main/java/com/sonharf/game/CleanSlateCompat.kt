package com.sonharf.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Temporary source-compatibility navigation hook for non-game app code that
 * previously returned to the classic Son Harf lobby. It now returns to HOME
 * only and contains no legacy matchmaking or game state.
 */
internal object SonHarfGameNavigation {
    fun requestLobby() {
        SonHarfUiState.homeRequest += 1
    }
}

/**
 * The old classic Son Harf bot engine was removed. Word Arena may still expose
 * its historical BOT entry until the new game code supplies a replacement;
 * this placeholder deliberately runs no game engine.
 */
@Composable
internal fun WordDuelBotScreen(onExit: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = SonHarfBg) {
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = sh("BOT modu yeniden inşa edilecek", "BOT mode will be rebuilt"),
                color = SonHarfText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Text(
                text = sh(
                    "Eski Son Harf bot motoru kaldırıldı. Yeni oyun koduyla yeniden bağlanacak.",
                    "The old Son Harf bot engine was removed. It will be reconnected with the new game code.",
                ),
                modifier = Modifier.padding(top = 8.dp, bottom = 18.dp),
                color = SonHarfMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onExit) {
                Text(sh("GERİ", "BACK"), fontWeight = FontWeight.Black)
            }
        }
    }
}
