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
    Column(
        Modifier.fillMaxSize().background(SonHarfTheme.Background).statusBarsPadding()
            .navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Spacer(Modifier.height(30.dp))
        Image(painterResource(R.drawable.kelime_tahti_brand_logo), "Kelime Tahtı",
            modifier = Modifier.fillMaxWidth().height(190.dp), contentScale = ContentScale.Fit)
        Text(if (english) "YOUR WORDS. YOUR THRONE." else "KELİMELER SENİN. TAHT SENİN.",
            color = SonHarfTheme.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
        Surface(shape = RoundedCornerShape(24.dp), color = SonHarfTheme.Surface, border = BorderStroke(1.dp, SonHarfTheme.Border)) {
            Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(if (english) "Word Siege · Last Letter · Workshop" else "Kelime Kuşatması · Son Harf · Atölye",
                    color = SonHarfTheme.TextPrimary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontSize = 14.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("tr" to "TÜRKÇE", "en" to "ENGLISH").forEach { (language, label) ->
                        Surface(onClick = { selected = language }, modifier = Modifier.weight(1f).heightIn(min = 54.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected == language) SonHarfTheme.PrimarySoft else SonHarfTheme.SurfaceSecondary,
                            border = BorderStroke(if (selected == language) 2.dp else 1.dp, if (selected == language) SonHarfTheme.Primary else SonHarfTheme.Border)) {
                            Box(Modifier.padding(14.dp), contentAlignment = Alignment.Center) {
                                Text(label, color = SonHarfTheme.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
        Button(onClick = { onContinue(selected) }, modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = SonHarfTheme.Primary, contentColor = SonHarfTheme.OnPrimary)) {
            Text(if (english) "LET'S PLAY" else "BAŞLA", fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}
