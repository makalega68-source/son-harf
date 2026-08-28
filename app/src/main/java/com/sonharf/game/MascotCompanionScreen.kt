package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend

/**
 * Single-mascot companion surface.
 * Story, collection, care economy, alternate mascots and mascot sales are intentionally absent.
 */
@Composable
internal fun MascotCompanionScreen(
    backend: OnlineGameBackend?,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenRoom: () -> Unit,
    onOpenShop: () -> Unit,
) {
    var motion by remember { mutableStateOf(MascotMotion.GREETING) }
    var message by remember {
        mutableStateOf(
            if (SonHarfUiState.isEnglish) "Ready? Let's play a word game." else "Hazırsan kelime oyununa başlayalım."
        )
    }

    LaunchedEffect(Unit) {
        delay(1700)
        motion = MascotMotion.IDLE
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.White, SonHarfBg, Color(0xFFF1F6FC)))
        )
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back"), tint = SonHarfText)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        sh("OYUN YARDIMCISI", "GAME COMPANION"),
                        color = SonHarfText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        sh("Tek maskot • kısa ve faydalı tepkiler", "One mascot • concise helpful reactions"),
                        color = SonHarfMuted,
                        fontSize = 10.sp,
                    )
                }
                Spacer(Modifier.size(48.dp))
            }

            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                border = BorderStroke(1.dp, SonHarfCyan.copy(alpha = .24f)),
                shadowElevation = 4.dp,
            ) {
                Column(
                    Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    MascotLive3DStage(
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                        mascotId = MascotCatalog.CHIBI_WIZARD_ID,
                        motion = motion,
                        displayScale = 1.70f,
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = SonHarfSurface2,
                        border = BorderStroke(1.dp, SonHarfCyan.copy(alpha = .18f)),
                    ) {
                        Text(
                            message,
                            Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            color = SonHarfText,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        motion = MascotMotion.THINKING
                        message = if (SonHarfUiState.isEnglish) "Check the final letter first." else "Önce son harfi kontrol et."
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(15.dp),
                ) {
                    Text(sh("İPUCU", "TIP"), fontWeight = FontWeight.Black)
                }
                Button(
                    onClick = {
                        motion = MascotMotion.RUN
                        message = if (SonHarfUiState.isEnglish) "Come on, let's play!" else "Hadi, oyuna girelim!"
                        onBack()
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(15.dp),
                ) {
                    Text(sh("OYUNA DÖN", "BACK TO GAME"), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
