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
 * Compatibility route after retiring the old mascot-room/story system.
 */
@Composable
internal fun MascotRoomScreen(
    backend: OnlineGameBackend?,
    onBack: () -> Unit,
    onOpenCompanion: () -> Unit,
    onOpenHistory: () -> Unit,
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
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back"), tint = SonHarfText)
                }
                Text(
                    sh("MASKOT", "MASCOT"),
                    modifier = Modifier.weight(1f),
                    color = SonHarfText,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.size(48.dp))
            }

            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                border = BorderStroke(1.dp, SonHarfCyan.copy(alpha = .24f)),
            ) {
                Column(
                    Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    MascotLive3DStage(
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                        mascotId = MascotCatalog.CHIBI_WIZARD_ID,
                        motion = MascotMotion.IDLE,
                        displayScale = 1.70f,
                    )
                    Text(
                        sh("Tek oyun arkadaşın burada.", "Your single game companion is here."),
                        color = SonHarfText,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Button(
                onClick = onOpenCompanion,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(sh("MASKOTU AÇ", "OPEN MASCOT"), fontWeight = FontWeight.Black)
            }
        }
    }
}
