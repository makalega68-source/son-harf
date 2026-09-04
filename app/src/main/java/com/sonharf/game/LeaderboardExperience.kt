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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
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
                    Text(sh("Canlı rekabet tablosu", "Live competition table"), color = SonHarfMuted, fontSize = 13.sp)
                }
                Surface(shape = RoundedCornerShape(10.dp), color = SonHarfPink.copy(alpha = .14f)) {
                    Row(Modifier.padding(horizontal = 9.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(SonHarfPink))
                        Spacer(Modifier.width(5.dp))
                        Text("LIVE", color = SonHarfPink, fontSize = 13.sp, fontWeight = FontWeight.Black)
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
                            Surface(shape = RoundedCornerShape(16.dp), color = SonHarfTheme.BrandGoldSoft, border = BorderStroke(1.dp, SonHarfGold.copy(alpha = .32f))) {
                                Icon(Icons.Rounded.EmojiEvents, null, Modifier.padding(14.dp).size(28.dp), tint = SonHarfGold)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (SonHarfUiState.isEnglish) "${leagueProgress.leagueName} LEAGUE" else "${leagueProgress.leagueName} LİGİ", color = SonHarfText, fontSize = 18.sp, fontWeight = FontWeight.Black)
                                Text("$currentRating RATING", color = SonHarfBlue, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(if (myIndex >= 0) "#${myIndex + 1}" else "—", color = SonHarfText, fontSize = 25.sp, fontWeight = FontWeight.Black)
                                Text(sh("SIRAN", "YOUR RANK"), color = SonHarfMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                                fontSize = 13.sp,
                            )
                            Text(if (period == "season") sh("SEZON", "SEASON") else sh("AKTİF", "ACTIVE"), color = SonHarfBlue, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        item {
            LeagueRoadmap(
                currentLeague = leagueProgress.leagueName,
                nextLeague = leagueProgress.nextLeagueName,
                pointsToNext = leagueProgress.pointsToNext,
                progress = leagueProgress.progress,
            )
        }

        if (rows.isNotEmpty()) {
            item { LeaguePodium(rows.take(3), profiles, me) }
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
                        Text(title, Modifier.padding(vertical = 11.dp), color = if (selected) Color.White else SonHarfMuted, fontSize = 12.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
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
                Text(sh("RATING", "RATING"), color = SonHarfMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                        Icon(if (index < 3) Icons.Rounded.MilitaryTech else Icons.Rounded.Leaderboard, null, Modifier.padding(8.dp).size(20.dp), tint = accent)
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
                                    Text(sh("SEN", "YOU"), Modifier.padding(horizontal = 6.dp, vertical = 3.dp), color = SonHarfBlue, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                        val rate = if (row.winRate % 1.0 == 0.0) row.winRate.toInt().toString() else String.format("%.1f", row.winRate)
                        Text("${row.leagueName}  •  ${row.wins}W ${row.losses}L  •  %$rate", color = SonHarfMuted, fontSize = 13.sp, maxLines = 1)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(row.rating.toString(), color = SonHarfText, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        Text("RATING", color = SonHarfBlue, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        if (!loading && rows.isEmpty()) item {
            Surface(shape = RoundedCornerShape(16.dp), color = SonHarfSurface, border = BorderStroke(1.dp, SonHarfMuted.copy(alpha = .12f))) {
                Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.EmojiEvents, null, Modifier.size(44.dp), tint = SonHarfGold)
                    Text(sh("LİG YOLCULUĞUN BAŞLIYOR", "YOUR LEAGUE JOURNEY STARTS"), color = SonHarfText, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text(if (error) sh("Sıralama şu an yenileniyor; biraz sonra tekrar kontrol et.", "Ranking is refreshing; check again shortly.") else sh("İlk maçını oynadığında sıran ve rakiplerin burada görünecek.", "Play your first match to reveal your rank and rivals."), color = SonHarfMuted, textAlign = TextAlign.Center, fontSize = 13.sp)
                }
            }
        }
        item { Spacer(Modifier.height(6.dp)) }
    }
}

@Composable
private fun LeagueRoadmap(currentLeague: String, nextLeague: String?, pointsToNext: Int, progress: Float) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, SonHarfTheme.BrandPurple.copy(alpha = .30f)),
    ) {
        Column(
            Modifier.fillMaxWidth().background(
                Brush.horizontalGradient(listOf(Color(0xFF151B3B), Color(0xFF30225F), Color(0xFF121A36)))
            ).padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Route, null, Modifier.size(23.dp), tint = SonHarfGold)
                Spacer(Modifier.width(8.dp))
                Text(sh("LİG YOLCULUĞU", "LEAGUE JOURNEY"), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Text("${(progress * 100).toInt()}%", color = SonHarfGold, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                LeagueRoadmapNode(currentLeague, Icons.Rounded.Shield, SonHarfBlue, true, Modifier.weight(1f))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.weight(.8f).height(5.dp).clip(CircleShape),
                    color = SonHarfGold,
                    trackColor = Color.White.copy(alpha = .12f),
                )
                LeagueRoadmapNode(nextLeague ?: sh("ZİRVE", "SUMMIT"), Icons.Rounded.WorkspacePremium, SonHarfGold, false, Modifier.weight(1f))
            }
            Text(
                if (nextLeague == null) sh("En üst ligdesin", "You are in the top league") else sh("$nextLeague için $pointsToNext puan kaldı", "$pointsToNext points to $nextLeague"),
                color = Color(0xFFBDC7DE),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LeagueRoadmapNode(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, active: Boolean, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(shape = RoundedCornerShape(12.dp), color = accent.copy(alpha = if (active) .25f else .12f), border = BorderStroke(1.dp, accent.copy(alpha = .55f))) {
            Icon(icon, null, Modifier.padding(8.dp).size(21.dp), tint = accent)
        }
        Text(label, color = if (active) Color.White else Color(0xFFBDC7DE), fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun LeaguePodium(rows: List<LeaderboardV2Row>, profiles: Map<String, ProfileDto?>, me: String?) {
    Surface(shape = RoundedCornerShape(22.dp), color = SonHarfTheme.Surface, border = BorderStroke(1.dp, SonHarfTheme.Border)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(sh("HAFTANIN ZİRVESİ", "TOP OF THE WEEK"), color = SonHarfText, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                rows.forEachIndexed { index, row ->
                    val accent = when (index) { 0 -> SonHarfGold; 1 -> Color(0xFF9EA9BA); else -> Color(0xFFCD8D5C) }
                    Surface(
                        modifier = Modifier.weight(1f).height(if (index == 0) 142.dp else 126.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = accent.copy(alpha = .10f),
                        border = BorderStroke(if (row.userId == me) 2.dp else 1.dp, if (row.userId == me) SonHarfBlue else accent.copy(alpha = .35f)),
                    ) {
                        Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text("#${index + 1}", color = accent, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            ProfilePhotoAvatarRectWithGender(profiles[row.userId]?.avatarPath, profiles[row.userId]?.gender, row.displayName, 50.dp, 61.dp, accent, profiles[row.userId]?.avatarVisibility != "hidden")
                            Text(row.displayName, color = SonHarfText, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${row.rating}", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
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
        Text(label, Modifier.padding(vertical = 9.dp), color = if (selected) SonHarfBlue else SonHarfMuted, textAlign = TextAlign.Center, fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}
