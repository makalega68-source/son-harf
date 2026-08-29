package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.LeaderboardV2Row
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.getLeaderboardV2

@Composable
fun LeaderboardExperienceScreen(onBack: () -> Unit) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var language by remember { mutableStateOf(if (SonHarfUiState.language == "en") "en" else "tr") }
    var period by remember { mutableStateOf("week") }
    var rows by remember { mutableStateOf<List<LeaderboardV2Row>>(emptyList()) }
    var profiles by remember { mutableStateOf<Map<String, ProfileDto?>>(emptyMap()) }
    var myProfile by remember { mutableStateOf<ProfileDto?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val me = backend?.currentUserId()

    LaunchedEffect(language, period) {
        val b = backend
        if (b == null) { rows = emptyList(); error = true; return@LaunchedEffect }
        loading = true
        error = false
        myProfile = me?.let { runCatching { b.getProfile(it) }.getOrNull() }
        runCatching { b.getLeaderboardV2(language, period, 50) }
            .onSuccess { loaded ->
                rows = loaded
                val loadedProfiles = linkedMapOf<String, ProfileDto?>()
                loaded.forEach { row ->
                    loadedProfiles[row.userId] = runCatching { b.getProfile(row.userId) }.getOrNull()
                }
                profiles = loadedProfiles
            }
            .onFailure { rows = emptyList(); profiles = emptyMap(); error = true }
        loading = false
        runCatching { b.logEvent("leaderboard_neon_open", "$language:$period") }
    }

    val myIndex = rows.indexOfFirst { it.userId == me }
    val myRow = rows.getOrNull(myIndex)
    val currentRating = myRow?.rating ?: myProfile?.rating ?: 1000
    val leagueProgress = ratingLeagueProgress(currentRating)
    val league = leagueProgress.leagueName

    LazyColumn(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(SonHarfBg, SonHarfSurface2, SonHarfBg))),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.size(42.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, SonHarfCyan.copy(alpha = .35f)),
                ) { Text("‹", fontSize = 28.sp, color = SonHarfCyan) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(sh("LİGLER", "LEAGUES"), color = SonHarfGold, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Text(sh("Oyuncu sıralaması", "Player ranking"), color = SonHarfMuted, fontSize = 9.sp)
                }
                Spacer(Modifier.size(42.dp))
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.dp, SonHarfBlue.copy(alpha = .24f)),
            ) {
                Column(
                    Modifier.fillMaxWidth().background(
                        Brush.radialGradient(listOf(SonHarfCyan.copy(alpha = .18f), SonHarfSurface2, SonHarfSurface))
                    ).padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NeonLeagueShield()
                    Text(if (SonHarfUiState.isEnglish) "$league LEAGUE" else "$league LİGİ", color = SonHarfCyan, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (myIndex >= 0) sh("SIRALAMAN: ${myIndex + 1}", "YOUR RANK: ${myIndex + 1}") else sh("Bu dönemde henüz sıran yok", "No rank this period yet"),
                        color = SonHarfText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                    )
                    LinearProgressIndicator(
                        progress = { leagueProgress.progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = SonHarfPurple,
                        trackColor = SonHarfMuted.copy(alpha = .16f),
                    )
                    Text(
                        if (leagueProgress.nextAt == null)
                            sh("$currentRating rating • En üst lig", "$currentRating rating • Top league")
                        else
                            sh(
                                "$currentRating rating • Sonraki lige ${leagueProgress.pointsToNext} puan",
                                "$currentRating rating • ${leagueProgress.pointsToNext} points to next league",
                            ),
                        color = SonHarfMuted,
                        fontSize = 10.sp,
                    )
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FilterChip(selected = language == "tr", onClick = { language = "tr" }, label = { Text("🇹🇷 TR", fontWeight = FontWeight.Bold) }, modifier = Modifier.weight(1f))
                FilterChip(selected = language == "en", onClick = { language = "en" }, label = { Text("🇬🇧 EN", fontWeight = FontWeight.Bold) }, modifier = Modifier.weight(1f))
            }
        }

        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(SonHarfSurface2).padding(4.dp)) {
                listOf("week" to sh("BU HAFTA", "THIS WEEK"), "month" to sh("BU AY", "THIS MONTH"), "total" to sh("TOPLAM", "TOTAL")).forEach { (key, title) ->
                    val selected = period == key
                    Button(
                        onClick = { period = key },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (selected) SonHarfBlue else Color.Transparent, contentColor = SonHarfText),
                        shape = RoundedCornerShape(11.dp),
                        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 8.dp),
                    ) { Text(title, fontSize = 9.sp, fontWeight = FontWeight.Black) }
                }
            }
        }

        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = SonHarfCyan) }

        item {
            Text(sh("ÖNDEKİ OYUNCULAR", "LEADING PLAYERS"), color = SonHarfGold, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }

        itemsIndexed(rows, key = { _, row -> row.userId }) { index, row ->
            val mine = row.userId == me
            Card(
                colors = CardDefaults.cardColors(containerColor = if (mine) SonHarfBlue.copy(alpha = .10f) else SonHarfSurface),
                shape = RoundedCornerShape(15.dp),
                border = BorderStroke(1.dp, when { mine -> SonHarfBlue; index == 0 -> SonHarfGold.copy(alpha = .55f); else -> SonHarfMuted.copy(alpha = .10f) }),
            ) {
                Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when (index) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "${index + 1}" },
                        Modifier.width(40.dp),
                        color = if (index == 0) SonHarfGold else SonHarfMuted,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Black,
                    )
                    ProfilePhotoAvatar(
                        avatarPath = profiles[row.userId]?.avatarPath,
                        name = row.displayName,
                        size = 36.dp,
                        visible = true,
                        accent = if (mine) SonHarfBlue else Color(0xFF6B4FD3),
                    )
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(row.displayName, color = SonHarfText, fontWeight = if (mine) FontWeight.Black else FontWeight.Bold, maxLines = 1)
                        val rate = if (row.winRate % 1.0 == 0.0) row.winRate.toInt().toString() else String.format("%.1f", row.winRate)
                        Text("${row.leagueName} • ${row.rating} rating • ${row.wins}W • %$rate", color = SonHarfMuted, fontSize = 9.sp)
                    }
                    if (mine) Text(sh("SEN", "YOU"), color = SonHarfCyan, fontWeight = FontWeight.Black, fontSize = 9.sp)
                }
            }
        }

        if (!loading && rows.isEmpty()) item {
            Text(
                if (error) sh("Liderlik verisi alınamadı.", "Leaderboard data could not be loaded.") else sh("Bu dönemde sıralama henüz oluşmadı.", "No ranking for this period yet."),
                color = SonHarfMuted,
                modifier = Modifier.fillMaxWidth().padding(26.dp),
                textAlign = TextAlign.Center,
            )
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun NeonLeagueShield() {
    Box(Modifier.size(126.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val top = size.height * .12f
            val bottom = size.height * .88f
            val outer = Path().apply {
                moveTo(cx, top)
                lineTo(size.width * .84f, size.height * .28f)
                lineTo(size.width * .76f, size.height * .67f)
                lineTo(cx, bottom)
                lineTo(size.width * .24f, size.height * .67f)
                lineTo(size.width * .16f, size.height * .28f)
                close()
            }
            drawPath(outer, brush = Brush.linearGradient(listOf(Color(0xFF8DE1FA), SonHarfBlue, Color(0xFF157FB0))))
            drawPath(outer, color = SonHarfCyan.copy(alpha = .65f), style = Stroke(width = 3f))
            val gem = Path().apply {
                moveTo(cx, size.height*.31f)
                lineTo(size.width*.70f, size.height*.48f)
                lineTo(cx, size.height*.72f)
                lineTo(size.width*.30f, size.height*.48f)
                close()
            }
            drawPath(gem, brush = Brush.linearGradient(listOf(Color(0xFF7FE2FA), SonHarfBlue, SonHarfCyan)))
            drawLine(SonHarfGold, Offset(size.width*.10f,size.height*.36f), Offset(size.width*.01f,size.height*.24f), strokeWidth=5f)
            drawLine(SonHarfGold, Offset(size.width*.90f,size.height*.36f), Offset(size.width*.99f,size.height*.24f), strokeWidth=5f)
        }
        Text("◆", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
    }
}

