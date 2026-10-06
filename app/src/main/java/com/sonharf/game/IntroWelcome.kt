package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Immediate, silent entry. Language selection and the existing authentication flow stay intact. */
@Composable
internal fun IntroWelcomeScreen(onContinue: (String) -> Unit) {
    var selected by remember { mutableStateOf(if (SonHarfUiState.isEnglish) "en" else "tr") }
    val english = selected == "en"
    // Same petrol word-board, green/red letter tiles and gold accents as the lobby.
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(LobbyBrand.Sky, LobbyBrand.Band, LobbyBrand.NavBar)))) {
        LobbyBoardPattern(Modifier.matchParentSize())
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Image(rememberArtPainter(R.drawable.kelime_tahti_brand_logo), "Kelime Tahtı",
                modifier = Modifier.fillMaxWidth().height(190.dp), contentScale = ContentScale.Fit)
            Text(if (english) "YOUR WORDS. YOUR THRONE." else "KELİMELER SENİN. TAHT SENİN.",
                color = LobbyBrand.Gold, fontWeight = FontWeight.Black, fontSize = 19.sp, textAlign = TextAlign.Center)
            Text(if (english) "Lay words on the board, outplay rivals and claim the weekly throne."
                else "Tahtaya kelimeni diz, rakiplerini geç, haftanın tahtına otur.",
                color = Color.White.copy(alpha = .86f), fontSize = 14.sp, textAlign = TextAlign.Center)
            Surface(shape = RoundedCornerShape(24.dp), color = LobbyBrand.Band.copy(alpha = .92f), border = BorderStroke(1.dp, LobbyBrand.Grid)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(if (english) "Choose your language" else "Dilini seç",
                        color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontSize = 15.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("tr" to "TÜRKÇE", "en" to "ENGLISH").forEach { (language, label) ->
                            val chosen = selected == language
                            Surface(onClick = { selected = language }, modifier = Modifier.weight(1f).heightIn(min = 54.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = if (chosen) LobbyBrand.Play else LobbyBrand.NavBar,
                                border = BorderStroke(if (chosen) 2.dp else 1.dp, if (chosen) LobbyBrand.Gold else LobbyBrand.Grid)) {
                                Box(Modifier.padding(14.dp), contentAlignment = Alignment.Center) {
                                    Text(label, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                    Text(if (english) "Word Siege · Last Letter · Word Workshop" else "Kelime Kuşatması · Son Harf · Kelime Atölyesi",
                        color = Color.White.copy(alpha = .7f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontSize = 12.sp)
                }
            }
            Button(onClick = { onContinue(selected) }, modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = LobbyBrand.Play, contentColor = Color.White)) {
                Text(if (english) "LET'S PLAY" else "BAŞLA", fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}
