package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.LeaderboardV2Row
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.getLeaderboardV2

@Composable
fun LeaderboardExperienceScreen(onBack: () -> Unit) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var language by remember { mutableStateOf(if (SonHarfUiState.language == "en") "en" else "tr") }
    var period by remember { mutableStateOf("total") }
    var rows by remember { mutableStateOf<List<LeaderboardV2Row>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(language, period) {
        val b = backend
        if (b == null) { rows = emptyList(); error = true; return@LaunchedEffect }
        loading = true
        error = false
        runCatching { b.getLeaderboardV2(language, period, 50) }.onSuccess { rows = it }.onFailure { rows = emptyList(); error = true }
        loading = false
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("‹", fontSize = 30.sp, color = SonHarfPurple) }
                Text(sh("LİDERLİK TABLOSU", "LEADERBOARD"), fontWeight = FontWeight.Black, fontSize = 22.sp)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = language == "tr", onClick = { language = "tr" }, label = { Text("🇹🇷 TÜRKÇE") }, modifier = Modifier.weight(1f))
                FilterChip(selected = language == "en", onClick = { language = "en" }, label = { Text("🇬🇧 ENGLISH") }, modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(SonHarfSurface2).padding(4.dp)) {
                listOf("total" to sh("TOPLAM", "TOTAL"), "week" to sh("BU HAFTA", "THIS WEEK"), "month" to sh("BU AY", "THIS MONTH")).forEach { (key, title) ->
                    val selected = period == key
                    Button(
                        onClick = { period = key },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) SonHarfBlue else Color.Transparent,
                            contentColor = if (selected) Color.White else SonHarfText,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text(title, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.Bold) }
                }
            }
        }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                Text(sh("SIRA", "RANK"), Modifier.width(42.dp), color = SonHarfMuted, fontSize = 9.sp)
                Text(sh("OYUNCU", "PLAYER"), Modifier.weight(1f), color = SonHarfMuted, fontSize = 9.sp)
                Text(sh("GALİBİYET", "WINS"), Modifier.width(70.dp), color = SonHarfMuted, fontSize = 9.sp)
                Text(sh("KAZANMA %", "WIN %"), Modifier.width(72.dp), color = SonHarfMuted, fontSize = 9.sp)
            }
        }
        itemsIndexed(rows, key = { _, row -> row.userId }) { index, row ->
            Card(colors = CardDefaults.cardColors(containerColor = SonHarfSurface), shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, if (index == 0) SonHarfGold.copy(alpha = .35f) else SonHarfMuted.copy(alpha = .12f))) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (index < 3) listOf("♛", "♜", "♝")[index] else "${index + 1}", Modifier.width(42.dp), color = if (index == 0) SonHarfGold else SonHarfMuted, textAlign = TextAlign.Center)
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(SonHarfSurface2), contentAlignment = Alignment.Center) { Text(row.displayName.take(1).uppercase(), fontWeight = FontWeight.Black, color = SonHarfCyan) }
                        Spacer(Modifier.width(9.dp))
                        Text(row.displayName, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    Text(row.wins.toString(), Modifier.width(70.dp), textAlign = TextAlign.Center)
                    val rate = if (row.winRate % 1.0 == 0.0) row.winRate.toInt().toString() else String.format("%.1f", row.winRate)
                    Text("%$rate", Modifier.width(72.dp), textAlign = TextAlign.Center)
                }
            }
        }
        if (!loading && rows.isEmpty()) item {
            val message = when {
                error -> sh("Liderlik verisi alınamadı.", "Leaderboard data could not be loaded.")
                language == "en" -> sh("Henüz English maç sonucu yok.", "No English match results yet.")
                period == "week" -> sh("Bu hafta tamamlanmış maç yok.", "No completed matches this week.")
                period == "month" -> sh("Bu ay tamamlanmış maç yok.", "No completed matches this month.")
                else -> sh("Henüz sıralama oluşturacak maç yok.", "No matches available for ranking yet.")
            }
            Text(message, color = SonHarfMuted, modifier = Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center)
        }
    }
}
