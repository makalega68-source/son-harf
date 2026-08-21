package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Shield
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
import com.sonharf.game.data.*

private data class V6LeaderboardUi(val row: LeaderboardV3Row, val avatar: String?)

@Composable
fun V6LeaderboardScreen(onBack: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    var language by remember { mutableStateOf(if (SonHarfUiState.language == "en") "en" else "tr") }
    var period by remember { mutableStateOf("week") }
    var rows by remember { mutableStateOf<List<V6LeaderboardUi>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    val me = backend.currentUserId()

    LaunchedEffect(language, period) {
        loading = true
        failed = false
        runCatching { backend.getLeaderboardV3(language, period, 50) }
            .onSuccess { board ->
                rows = board.map { row ->
                    V6LeaderboardUi(row, runCatching { AvatarSignedUrl.resolve(row.avatarUrl) }.getOrNull())
                }
            }
            .onFailure {
                rows = emptyList()
                failed = true
            }
        loading = false
    }

    val myIndex = rows.indexOfFirst { it.row.userId == me }
    val wins = rows.getOrNull(myIndex)?.row?.wins ?: 0
    val league = when {
        wins >= 200 -> "EFSANE"
        wins >= 100 -> "ELMAS"
        wins >= 50 -> "PLATİN"
        wins >= 25 -> "ALTIN"
        wins >= 10 -> "GÜMÜŞ"
        else -> "BRONZ"
    }
    val floor = when (league) {
        "GÜMÜŞ" -> 10
        "ALTIN" -> 25
        "PLATİN" -> 50
        "ELMAS" -> 100
        "EFSANE" -> 200
        else -> 0
    }
    val next = when (league) {
        "BRONZ" -> 10
        "GÜMÜŞ" -> 25
        "ALTIN" -> 50
        "PLATİN" -> 100
        "ELMAS" -> 200
        else -> 300
    }
    val progress = ((wins - floor).toFloat() / (next - floor).coerceAtLeast(1)).coerceIn(0f, 1f)

    LazyColumn(
        Modifier.fillMaxSize().background(V6Light.bg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Geri", tint = V6Light.text) }
                Text("LİG", Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Black, fontSize = 22.sp, color = V6Light.text)
                Spacer(Modifier.width(48.dp))
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = V6Light.blueLight,
                border = BorderStroke(1.5.dp, V6Light.blue.copy(alpha = .35f)),
            ) {
                Column(
                    Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        Modifier.size(94.dp).clip(CircleShape).background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Shield, null, tint = V6Light.blue, modifier = Modifier.size(64.dp)) }
                    Text("$league LİG", color = V6Light.blueDark, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (myIndex >= 0) "SIRALAMAN: ${myIndex + 1}" else "Bu dönemde henüz sıran yok",
                        color = V6Light.text,
                        fontWeight = FontWeight.Bold,
                    )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = V6Light.blue,
                        trackColor = Color.White,
                    )
                    Text("$wins galibiyet • sonraki lig: $next", color = V6Light.muted, fontSize = 11.sp)
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = language == "tr", onClick = { language = "tr" }, label = { Text("🇹🇷 TR", fontWeight = FontWeight.Bold) }, modifier = Modifier.weight(1f))
                FilterChip(selected = language == "en", onClick = { language = "en" }, label = { Text("🇬🇧 EN", fontWeight = FontWeight.Bold) }, modifier = Modifier.weight(1f))
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                listOf("week" to "BU HAFTA", "month" to "BU AY", "total" to "TOPLAM").forEach { (key, title) ->
                    Button(
                        onClick = { period = key },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (period == key) V6Light.blue else Color.Transparent,
                            contentColor = if (period == key) Color.White else V6Light.text,
                        ),
                        shape = RoundedCornerShape(10.dp),
                    ) { Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }

        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = V6Light.blue) }
        item { Text("LİDERLER", fontWeight = FontWeight.Black, fontSize = 16.sp, color = V6Light.text) }

        itemsIndexed(rows, key = { _, ui -> ui.row.userId }) { index, ui ->
            val mine = ui.row.userId == me
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = if (mine) V6Light.blueLight else Color.White,
                border = BorderStroke(1.dp, if (mine) V6Light.blue else V6Light.border),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when (index) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "${index + 1}." },
                        Modifier.width(42.dp),
                        textAlign = TextAlign.Center,
                    )
                    V6Avatar(ui.avatar, ui.row.displayName, 42)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(ui.row.displayName, fontWeight = if (mine) FontWeight.Black else FontWeight.Bold, color = V6Light.text, maxLines = 1)
                        val rate = if (ui.row.winRate % 1.0 == 0.0) ui.row.winRate.toInt().toString() else String.format("%.1f", ui.row.winRate)
                        Text("${ui.row.wins} galibiyet • %$rate", fontSize = 11.sp, color = V6Light.muted)
                    }
                    if (mine) Text("SEN", color = V6Light.blue, fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
            }
        }

        if (!loading && rows.isEmpty()) {
            item {
                Text(
                    if (failed) "Liderlik verisi alınamadı." else "Bu dönemde sıralama henüz oluşmadı.",
                    Modifier.fillMaxWidth().padding(28.dp),
                    textAlign = TextAlign.Center,
                    color = V6Light.muted,
                )
            }
        }
    }
}
