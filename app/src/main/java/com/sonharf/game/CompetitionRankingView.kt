package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*

private data class RankingRow(
    val userId: String,
    val name: String,
    val score: Int,
    val avatarPath: String?,
)

private val RankSilver = Color(0xFFC9CED1)
private val RankBronze = Color(0xFFB0835A)

/** Rekabet ana görünümü (08): lig kartı, haftalık kürsü, Haftalık/Genel sıralama ve oyuncunun satırı. */
@Composable
internal fun CompetitionRankingView(onCup: () -> Unit, onRivals: () -> Unit) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var period by rememberSaveable { mutableIntStateOf(0) }
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var myId by remember { mutableStateOf<String?>(null) }
    var weekly by remember { mutableStateOf<List<RankingRow>>(emptyList()) }
    var overall by remember { mutableStateOf<List<RankingRow>?>(null) }
    var loading by remember { mutableStateOf(backend != null) }

    LaunchedEffect(backend) {
        val b = backend ?: return@LaunchedEffect
        myId = b.currentUserId()
        profile = myId?.let { id -> runCatching { b.getProfile(id) }.getOrNull() }
        weekly = runCatching { b.getWeeklyTopV210(limit = 50) }.getOrDefault(emptyList())
            .map { RankingRow(it.userId, it.username, it.rp, it.avatarUrl) }
        loading = false
    }
    LaunchedEffect(backend, period) {
        val b = backend ?: return@LaunchedEffect
        if (period == 1 && overall == null) {
            overall = runCatching { b.getLeaderboardV2(language = SonHarfUiState.language, period = "all", limit = 50) }
                .getOrDefault(emptyList())
                .map { RankingRow(it.userId, it.displayName, it.rating, null) }
        }
    }

    val rows = if (period == 0) weekly else overall.orEmpty()
    val myIndex = rows.indexOfFirst { it.userId == myId }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { HfTitleRule(sh("Rekabet", "Compete"), fontSize = 34.sp) }
        item { RankingLeagueCard(profile) }
        item { HfTitleRule(sh("Haftalık Sıralama", "Weekly Ranking"), fontSize = 24.sp) }
        item { RankingPodium(weekly.take(3), loading) }
        item {
            HfSegmentedTabs(
                labels = listOf(sh("Haftalık", "Weekly"), sh("Genel", "Overall")),
                selected = period,
                onSelect = { period = it },
            )
        }
        item {
            HfCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    when {
                        loading || (period == 1 && overall == null && backend != null) -> Box(
                            Modifier.fillMaxWidth().height(80.dp),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator(Modifier.size(22.dp), color = Hf.Gold, strokeWidth = 2.dp) }
                        rows.isEmpty() -> Text(
                            sh("Sıralama henüz oluşmadı.", "No ranking yet."),
                            Modifier.fillMaxWidth().padding(20.dp),
                            color = Hf.TextMuted,
                            textAlign = TextAlign.Center,
                        )
                        else -> rows.take(10).forEachIndexed { index, row ->
                            if (index > 0) HorizontalDivider(color = Hf.Gold.copy(alpha = .22f))
                            RankingListRow(index + 1, row.name, row.score, row.avatarPath, mine = row.userId == myId)
                        }
                    }
                }
            }
        }
        if (profile != null) {
            item {
                HfCard(modifier = Modifier.fillMaxWidth(), borderColor = Hf.Gold) {
                    RankingListRow(
                        rank = if (myIndex >= 0) myIndex + 1 else null,
                        name = sh("Sen", "You"),
                        score = if (myIndex >= 0) rows[myIndex].score else if (period == 1) profile?.rating else null,
                        avatarPath = profile?.avatarPath,
                        mine = true,
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HfSecondaryButton(sh("Haftalık Kupa", "Weekly Cup"), onClick = onCup, modifier = Modifier.weight(1f), fontSize = 15.sp)
                HfSecondaryButton(sh("Rakiplerin", "Rivals"), onClick = onRivals, modifier = Modifier.weight(1f), fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun RankingLeagueCard(profile: ProfileDto?) {
    val league = ratingLeagueProgress(profile?.rating ?: 1000)
    HfCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RankingLeagueGem(52.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1.2f)) {
                Text(rankingLeagueName(league.leagueName) + sh(" Lig", " League"), color = Hf.Text, fontSize = 24.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Text(sh("En iyiler burada yarışıyor", "The best compete here"), color = Hf.TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.width(1.dp).fillMaxHeight().padding(vertical = 4.dp).background(Hf.Gold.copy(alpha = .5f)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(sh("Lig ilerlemesi", "League progress"), color = Hf.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HfProgressBar(league.progress, Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    Text("%${(league.progress * 100).toInt()}", color = Hf.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RankingLeagueGem(size: Dp) {
    Canvas(Modifier.size(size)) {
        val c = this.size.width / 2f
        fun diamond(r: Float) = Path().apply { moveTo(c, c - r); lineTo(c + r, c); lineTo(c, c + r); lineTo(c - r, c); close() }
        drawPath(diamond(c), Brush.linearGradient(listOf(Hf.GoldLight, Hf.Gold, Hf.GoldDeep)))
        drawPath(diamond(c * .62f), Hf.Ground.copy(alpha = .35f))
        drawPath(diamond(c * .62f), Hf.GoldLight, style = Stroke(width = 1.5.dp.toPx()))
        drawPath(diamond(c * .30f), Hf.Gold)
    }
}

@Composable
private fun RankingPodium(top: List<RankingRow>, loading: Boolean) {
    if (loading) {
        Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(22.dp), color = Hf.Gold, strokeWidth = 2.dp)
        }
        return
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        RankingPodiumStep(2, top.getOrNull(1), pedestal = 120.dp, avatar = 66.dp, ring = RankSilver, modifier = Modifier.weight(1f))
        RankingPodiumStep(1, top.getOrNull(0), pedestal = 150.dp, avatar = 74.dp, ring = Hf.Gold, modifier = Modifier.weight(1f))
        RankingPodiumStep(3, top.getOrNull(2), pedestal = 108.dp, avatar = 66.dp, ring = RankSilver, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun RankingPodiumStep(place: Int, row: RankingRow?, pedestal: Dp, avatar: Dp, ring: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(avatar).border(3.dp, ring, CircleShape).padding(4.dp), contentAlignment = Alignment.Center) {
            if (row != null) {
                ProfilePhotoAvatarWithGender(
                    avatarPath = row.avatarPath,
                    gender = null,
                    name = row.name,
                    size = avatar - 12.dp,
                    accent = ring,
                    visible = true,
                    showGenderBadge = false,
                )
            } else {
                Box(Modifier.size(avatar - 12.dp).background(Hf.Surface, CircleShape))
            }
        }
        Spacer(Modifier.height(6.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().height(pedestal),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            color = Hf.Surface,
            border = BorderStroke(1.dp, Hf.Gold.copy(alpha = .35f)),
        ) {
            Column(Modifier.padding(top = 10.dp, start = 6.dp, end = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$place", color = Hf.Gold, fontSize = 34.sp, fontWeight = FontWeight.Black)
                Text(row?.name ?: "—", color = Hf.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(row?.let { rankingGrouped(it.score) } ?: "", color = Hf.Gold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RankingListRow(rank: Int?, name: String, score: Int?, avatarPath: String?, mine: Boolean) {
    val chip = when (rank) {
        1 -> Hf.Gold
        2 -> RankSilver
        3 -> RankBronze
        else -> if (mine) Hf.Gold else Hf.Surface
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).background(chip, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Text(rank?.toString() ?: "—", color = if (chip == Hf.Surface) Hf.Text else Hf.Ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(12.dp))
        ProfilePhotoAvatarWithGender(
            avatarPath = avatarPath,
            gender = null,
            name = name,
            size = 40.dp,
            accent = if (mine) Hf.Green else Hf.Gold,
            visible = true,
            showGenderBadge = false,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            name,
            Modifier.weight(1f),
            color = if (mine) Hf.Gold else Hf.Text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(score?.let { rankingGrouped(it) } ?: "—", color = Hf.Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

private fun rankingGrouped(value: Int): String = String.format(java.util.Locale("tr", "TR"), "%,d", value)

private fun rankingLeagueName(value: String): String = when (value) {
    "BRONZ" -> sh("Bronz", "Bronze")
    "GÜMÜŞ" -> sh("Gümüş", "Silver")
    "ALTIN" -> sh("Altın", "Gold")
    "PLATİN" -> sh("Platin", "Platinum")
    "ELMAS" -> sh("Elmas", "Diamond")
    "EFSANE" -> sh("Efsane", "Legend")
    else -> value
}
