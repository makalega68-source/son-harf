package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.CompetitiveSeasonDto
import com.sonharf.game.data.LeaderboardV2Row
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.getCompetitiveSeason
import com.sonharf.game.data.getLeaderboardV2
import com.sonharf.game.data.getSeasonLeaderboard

@Composable
fun LeaderboardExperienceScreen(onBack: () -> Unit) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var language by remember { mutableStateOf(if (SonHarfUiState.language == "en") "en" else "tr") }
    var period by remember { mutableStateOf("week") }
    var rows by remember { mutableStateOf<List<LeaderboardV2Row>>(emptyList()) }
    var profiles by remember { mutableStateOf<Map<String, ProfileDto?>>(emptyMap()) }
    var myProfile by remember { mutableStateOf<ProfileDto?>(null) }
    var seasonInfo by remember { mutableStateOf<CompetitiveSeasonDto?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val me = backend?.currentUserId()

    LaunchedEffect(language, period) {
        val b = backend
        if (b == null) {
            rows = emptyList(); error = true; return@LaunchedEffect
        }
        loading = true
        error = false
        myProfile = me?.let { runCatching { b.getProfile(it) }.getOrNull() }
        seasonInfo = if (period == "season") runCatching { b.getCompetitiveSeason() }.getOrNull() else null
        runCatching {
            if (period == "season") {
                b.getSeasonLeaderboard(50).map { s ->
                    LeaderboardV2Row(
                        userId = s.userId,
                        displayName = s.displayName,
                        wins = s.wins,
                        losses = s.losses,
                        matches = s.matches,
                        winRate = s.winRate,
                        rating = s.rating,
                        leagueName = s.leagueName,
                    )
                }
            } else b.getLeaderboardV2(language, period, 50)
        }.onSuccess { loaded ->
            rows = loaded
            profiles = loaded.associate { row -> row.userId to runCatching { b.getProfile(row.userId) }.getOrNull() }
        }.onFailure {
            rows = emptyList(); profiles = emptyMap(); error = true
        }
        loading = false
        runCatching { b.logEvent("leaderboard_open", "$language:$period") }
    }

    val myIndex = rows.indexOfFirst { it.userId == me }
    val myRow = rows.getOrNull(myIndex)
    val currentRating = if (period == "season") seasonInfo?.rating ?: myRow?.rating ?: 1000 else myRow?.rating ?: myProfile?.rating ?: 1000
    val leagueProgress = ratingLeagueProgress(currentRating)

    LazyColumn(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(SonHarfTheme.Surface, SonHarfTheme.Background, SonHarfTheme.SurfaceSecondary))),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(onClick = onBack, shape = RoundedCornerShape(12.dp), color = SonHarfSurface2, border = BorderStroke(1.dp, SonHarfMuted.copy(alpha = .18f))) {
                    Text("‹", Modifier.padding(horizontal = 13.dp, vertical = 5.dp), color = SonHarfText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(sh("LİG & SIRALAMA", "LEAGUE & RANKING"), color = SonHarfText, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text(sh("Canlı rekabet tablosu", "Live competition table"), color = SonHarfMuted, fontSize = 9.sp)
                }
                Surface(shape = RoundedCornerShape(10.dp), color = SonHarfPink.copy(alpha = .14f)) {
                    Row(Modifier.padding(horizontal = 9.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(SonHarfPink))
                        Spacer(Modifier.width(5.dp))
                        Text("LIVE", color = SonHarfPink, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(18.dp), color = SonHarfTheme.Surface, border = BorderStroke(1.dp, SonHarfTheme.Border)) {
                Box(
                    Modifier.fillMaxWidth().background(
                        Brush.horizontalGradient(listOf(SonHarfTheme.SurfaceSecondary, SonHarfTheme.Surface))
                    ).padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(14.dp), color = SonHarfBlue.copy(alpha = .12f), border = BorderStroke(1.dp, SonHarfBlue.copy(alpha = .26f))) {
                                Text("◆", Modifier.padding(horizontal = 15.dp, vertical = 11.dp), color = SonHarfBlue, fontSize = 24.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (SonHarfUiState.isEnglish) "${leagueProgress.leagueName} LEAGUE" else "${leagueProgress.leagueName} LİGİ", color = SonHarfText, fontSize = 18.sp, fontWeight = FontWeight.Black)
                                Text("$currentRating RATING", color = SonHarfBlue, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(if (myIndex >= 0) "#${myIndex + 1}" else "—", color = SonHarfText, fontSize = 25.sp, fontWeight = FontWeight.Black)
                                Text(sh("SIRAN", "YOUR RANK"), color = SonHarfMuted, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        LinearProgressIndicator(
                            progress = { leagueProgress.progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = SonHarfBlue,
                            trackColor = SonHarfSurface2,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                if (leagueProgress.nextAt == null) sh("En üst lig", "Top league") else sh("Sonraki lige ${leagueProgress.pointsToNext} puan", "${leagueProgress.pointsToNext} points to next league"),
                                color = SonHarfMuted,
                                fontSize = 8.sp,
                            )
                            Text(if (period == "season") sh("SEZON", "SEASON") else sh("AKTİF", "ACTIVE"), color = SonHarfBlue, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(SonHarfSurface).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                listOf(
                    "season" to sh("SEZON", "SEASON"),
                    "week" to sh("HAFTA", "WEEK"),
                    "month" to sh("AY", "MONTH"),
                    "total" to sh("TÜMÜ", "ALL"),
                ).forEach { (key, title) ->
                    val selected = period == key
                    Surface(
                        modifier = Modifier.weight(1f).clickable { period = key },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) SonHarfBlue else Color.Transparent,
                    ) {
                        Text(title, Modifier.padding(vertical = 9.dp), color = if (selected) Color.White else SonHarfMuted, fontSize = 8.5.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        if (period != "season") item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LeagueLanguagePill("🇹🇷 TR", language == "tr", Modifier.weight(1f)) { language = "tr" }
                LeagueLanguagePill("🇬🇧 EN", language == "en", Modifier.weight(1f)) { language = "en" }
            }
        }

        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = SonHarfBlue, trackColor = SonHarfSurface2) }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(sh("OYUNCULAR", "PLAYERS"), color = SonHarfText, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Text(sh("RATING", "RATING"), color = SonHarfMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }

        itemsIndexed(rows, key = { _, row -> row.userId }) { index, row ->
            val mine = row.userId == me
            val accent = when (index) { 0 -> SonHarfGold; 1 -> Color(0xFFC5C8D0); 2 -> Color(0xFFCD8D5C); else -> SonHarfMuted }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = if (mine) SonHarfBlue.copy(alpha = .08f) else SonHarfSurface,
                border = BorderStroke(1.dp, if (mine) SonHarfBlue.copy(alpha = .48f) else SonHarfMuted.copy(alpha = .10f)),
            ) {
                Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(9.dp), color = accent.copy(alpha = .10f)) {
                        Text("${index + 1}", Modifier.width(34.dp).padding(vertical = 8.dp), color = accent, textAlign = TextAlign.Center, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                    Spacer(Modifier.width(9.dp))
                    ProfilePhotoAvatar(
                        avatarPath = profiles[row.userId]?.avatarPath,
                        name = row.displayName,
                        size = 38.dp,
                        visible = profiles[row.userId]?.avatarVisibility != "hidden",
                        accent = if (profiles[row.userId]?.isVip == true) SonHarfGold else SonHarfBlue,
                    )
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(row.displayName, color = SonHarfText, fontSize = 12.sp, fontWeight = if (mine) FontWeight.Black else FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (mine) {
                                Spacer(Modifier.width(6.dp))
                                Surface(shape = RoundedCornerShape(6.dp), color = SonHarfBlue.copy(alpha = .12f)) {
                                    Text(sh("SEN", "YOU"), Modifier.padding(horizontal = 5.dp, vertical = 2.dp), color = SonHarfBlue, fontSize = 6.5.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                        val rate = if (row.winRate % 1.0 == 0.0) row.winRate.toInt().toString() else String.format("%.1f", row.winRate)
                        Text("${row.leagueName}  •  ${row.wins}W ${row.losses}L  •  %$rate", color = SonHarfMuted, fontSize = 8.sp, maxLines = 1)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(row.rating.toString(), color = SonHarfText, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        Text("RATING", color = SonHarfBlue, fontSize = 6.5.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        if (!loading && rows.isEmpty()) item {
            Surface(shape = RoundedCornerShape(16.dp), color = SonHarfSurface, border = BorderStroke(1.dp, SonHarfMuted.copy(alpha = .12f))) {
                Text(
                    if (error) sh("Liderlik verisi alınamadı.", "Leaderboard data could not be loaded.") else sh("Bu dönemde sıralama henüz oluşmadı.", "No ranking for this period yet."),
                    Modifier.fillMaxWidth().padding(26.dp),
                    color = SonHarfMuted,
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                )
            }
        }
        item { Spacer(Modifier.height(6.dp)) }
    }
}

@Composable
private fun LeagueLanguagePill(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(11.dp),
        color = if (selected) SonHarfSurface2 else SonHarfSurface,
        border = BorderStroke(1.dp, if (selected) SonHarfBlue.copy(alpha = .40f) else SonHarfMuted.copy(alpha = .10f)),
    ) {
        Text(label, Modifier.padding(vertical = 9.dp), color = if (selected) SonHarfBlue else SonHarfMuted, textAlign = TextAlign.Center, fontSize = 9.sp, fontWeight = FontWeight.Black)
    }
}
