package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
 * Compatibility destination for an old navigation route.
 * The former mascot history/collection/store screen has been retired.
 */
@Composable
internal fun WizardHistoryScreen(
    backend: OnlineGameBackend?,
    onBack: () -> Unit,
    onOpenMascot: () -> Unit,
    onOpenShop: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.White, SonHarfBg, Color(0xFFF1F6FC)))
        )
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                        sh("Tek maskot • kelime oyunu desteği", "One mascot • word-game support"),
                        color = SonHarfMuted,
                        fontSize = 10.sp,
                    )
                }
                Spacer(Modifier.size(48.dp))
            }

            Surface(
                modifier = Modifier.fillMaxWidth().height(360.dp),
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
                        modifier = Modifier.fillMaxWidth().height(250.dp),
                        mascotId = MascotCatalog.CHIBI_WIZARD_ID,
                        motion = MascotMotion.GREETING,
                        displayScale = 1.60f,
                    )
                    Text(
                        sh("Hazırsan kelime oyununa dönelim.", "Ready? Let's get back to the word game."),
                        color = SonHarfText,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Button(
                onClick = onOpenMascot,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(sh("MASKOTA DÖN", "OPEN MASCOT"), fontWeight = FontWeight.Black)
            }

            Text(
                sh(
                    "Eski hikâye, karakter koleksiyonu ve maskot mağazası kaldırıldı.",
                    "The old story, character collection and mascot store have been removed."
                ),
                color = SonHarfMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
