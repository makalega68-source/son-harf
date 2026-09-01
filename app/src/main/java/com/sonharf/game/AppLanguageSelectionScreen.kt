package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppLanguageSelectionScreen(onSelect: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SonHarfBg, SonHarfSurface2))),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("SON HARF", color = SonHarfText, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text(
                "Dilini seç • Choose your language",
                color = SonHarfMuted,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            LanguageChoice("🇹🇷", "TÜRKÇE", "Türkçe arayüz", SonHarfBlue) { onSelect("tr") }
            Spacer(Modifier.height(12.dp))
            LanguageChoice("🇬🇧", "ENGLISH", "English interface", SonHarfPurple) { onSelect("en") }
            Spacer(Modifier.height(20.dp))
            Text(
                "Bu seçim yalnızca uygulama arayüzünü değiştirir.\nThis changes the app interface only.",
                color = SonHarfMuted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LanguageChoice(flag: String, title: String, subtitle: String, accent: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SonHarfSurface),
        border = BorderStroke(1.5.dp, accent.copy(alpha = .55f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(flag, fontSize = 30.sp)
            Column(Modifier.weight(1f)) {
                Text(title, color = SonHarfText, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = SonHarfMuted, fontSize = 12.sp)
            }
            Text("›", color = accent, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
}
