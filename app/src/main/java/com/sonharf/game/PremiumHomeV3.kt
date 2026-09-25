package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*

internal data class HomePodiumEntry(val row: LeaderboardV2Row, val profile: ProfileDto?)

private val HomeHeroShape = RoundedCornerShape(18.dp)
private val HomeCardShape = RoundedCornerShape(16.dp)
private val HomeControlShape = RoundedCornerShape(12.dp)
private val HomeMicroShape = RoundedCornerShape(10.dp)
private val HomeHairline = Color(0xFF2E3533)

@Composable
internal fun PremiumHomeCommandDeck(
    profile: ProfileDto?,
    onProfile: () -> Unit,
    onSiege: () -> Unit,
    onShop: () -> Unit,
    onPro: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        HfTitleRule("Kelime Tahtı", fontSize = 30.sp)
        HomeStatusRow(profile, onProfile, onShop, onPro, onSettings)
        HomeSiegeHero(onSiege)
    }
}

@Composable
private fun HomeStatusRow(
    profile: ProfileDto?,
    onProfile: () -> Unit,
    onShop: () -> Unit,
    onPro: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1.25f).clickable(onClick = onProfile),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FramedProfilePhotoAvatar(
                avatarPath = profile?.avatarPath,
                gender = profile?.gender,
                name = profile?.displayName ?: sh("Oyuncu", "Player"),
                size = 46.dp,
                frameId = SonHarfCosmetics.profileFrameId,
                accent = Hf.Gold,
                visible = profile?.avatarVisibility != "hidden",
                isPro = profile?.isVip == true,
            )
            Spacer(Modifier.width(4.dp))
            Surface(shape = Hf.PillShape, color = Hf.Gold.copy(alpha = .22f), border = BorderStroke(1.dp, Hf.Gold.copy(alpha = .8f))) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.WorkspacePremium, null, tint = Hf.Gold, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        profile?.let { homeLeagueName(ratingLeagueProgress(it.rating).leagueName) + sh(" Lig", " League") } ?: sh("Lig", "League"),
                        color = Hf.Ivory,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        HfPill(onClick = onShop, modifier = Modifier.padding(horizontal = 6.dp)) {
            HfCoin(20.dp)
            Spacer(Modifier.width(7.dp))
            Text(
                profile?.diamonds?.let { homeGrouped(it) } ?: "—",
                color = Hf.Ivory,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }
        Surface(
            onClick = onPro,
            shape = Hf.PillShape,
            color = Hf.Gold,
            border = BorderStroke(1.dp, Hf.GoldLight),
        ) {
            Row(Modifier.padding(horizontal = 9.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WorkspacePremium, null, tint = Hf.Ink, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(3.dp))
                Text(
                    "PRO",
                    color = Hf.Ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        IconButton(onClick = onSettings, modifier = Modifier.size(44.dp)) {
            Icon(painterResource(R.drawable.hf_ic_settings), sh("Ayarlar", "Settings"), tint = Hf.Gold, modifier = Modifier.size(28.dp))
        }
    }
}

private fun homeGrouped(value: Long): String = String.format(java.util.Locale("tr", "TR"), "%,d", value)
private fun homeGrouped(value: Int): String = homeGrouped(value.toLong())

/** The KELİME wordmark on live ivory tiles with the vertical A–T crossing and a gold diamond frame. */
@Composable
internal fun HomeKelimeTileLogo(modifier: Modifier = Modifier) {
    val tile = 42.dp
    val gap = 3.dp
    val word = listOf("K", "E", "L", "İ", "M", "E")
    Box(modifier.fillMaxWidth().height(tile * 3 + gap * 2 + 10.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val hw = size.width * .22f
            val hh = size.height * .5f
            val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
            val diamond = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx, cy - hh); lineTo(cx + hw, cy); lineTo(cx, cy + hh); lineTo(cx - hw, cy); close()
            }
            drawPath(diamond, Brush.linearGradient(listOf(Hf.GoldLight, Hf.Gold, Hf.GoldDeep)), style = stroke)
            val inner = androidx.compose.ui.graphics.Path().apply {
                val k = .78f
                moveTo(cx, cy - hh * k); lineTo(cx + hw * k, cy); lineTo(cx, cy + hh * k); lineTo(cx - hw * k, cy); close()
            }
            drawPath(inner, Hf.Gold.copy(alpha = .45f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))
            val d = 6.dp.toPx()
            listOf(size.width * .15f, size.width * .85f).forEach { x ->
                val gem = androidx.compose.ui.graphics.Path().apply {
                    moveTo(x, cy - d); lineTo(x + d, cy); lineTo(x, cy + d); lineTo(x - d, cy); close()
                }
                drawPath(gem, Hf.Gold)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(gap)) {
            listOf("A", null, "T").forEach { cross ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    word.forEachIndexed { index, letter ->
                        when {
                            cross == null -> HfLetterTile(letter, tile, fontSize = 24.sp)
                            index == 3 -> HfLetterTile(cross, tile, fontSize = 24.sp)
                            else -> Spacer(Modifier.size(tile))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSiegeHero(onSiege: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        HomeKelimeTileLogo()
        HfTitleRule("Kuşatma", fontSize = 40.sp)
        HfPrimaryButton(
            sh("OYNA", "PLAY"),
            onClick = onSiege,
            modifier = Modifier.widthIn(max = 320.dp).padding(horizontal = 8.dp),
            height = 58.dp,
            fontSize = 24.sp,
        )
    }
}

@Composable
internal fun PremiumHomeDailyTasks(onClick: () -> Unit) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var dashboard by remember { mutableStateOf<GrowthDashboardDto?>(null) }
    var streakDays by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(backend != null) }
    LaunchedEffect(backend) {
        val active = backend
        if (active == null) {
            loading = false
            return@LaunchedEffect
        }
        dashboard = runCatching { active.getGrowthDashboard() }.getOrNull()
        streakDays = runCatching { active.getMetaProgressV2().dailyPlayStreak }.getOrDefault(0)
        loading = false
    }
    val matches = dashboard?.matchesToday?.coerceIn(0, 3) ?: 0
    val checkInDone = dashboard?.dailyClaimed == true
    val challengeDone = dashboard?.dailyChallengeClaimed == true || matches >= 3
    val completedTasks = (if (checkInDone) 1 else 0) + (if (challengeDone) 1 else 0)
    val progress = if (dashboard == null) 0f else (((if (checkInDone) 1f else 0f) + matches / 3f) / 2f).coerceIn(0f, 1f)

    HfCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AssignmentTurnedIn, null, tint = Hf.Gold, modifier = Modifier.size(38.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(sh("Günlük görevler", "Daily tasks"), color = Hf.Ivory, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text(
                    if (streakDays > 0) sh("Bugünün görevlerini tamamla · $streakDays gün seri", "Complete today's tasks · $streakDays-day streak")
                    else sh("Bugünün görevlerini tamamla", "Complete today's tasks"),
                    color = Hf.TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(84.dp)) {
                Text(if (loading) "…" else "$completedTasks / 2", color = Hf.Ivory, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                HfProgressBar(progress, Modifier.fillMaxWidth())
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.ChevronRight, null, tint = Hf.Gold, modifier = Modifier.size(26.dp))
        }
    }
}

/** Opens the mascot chat for players who own a mascot. */
@Composable
internal fun PremiumHomeMascotCard(skin: WordSiegeMascotSkin, onClick: () -> Unit) {
    HfCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            WordSiegeMascot(
                moveId = null,
                lastMoveMine = false,
                pendingCells = emptyList(),
                playerTurn = true,
                requestedEmotion = WordSiegeMascotEmotion.HAPPY,
                modifier = Modifier.size(54.dp),
                skin = skin,
                onTap = onClick,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    sh("${skin.titleTr} ile sohbet et", "Chat with ${skin.titleEn}"),
                    color = Hf.Ivory,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(sh("Yazarak ya da sesli konuş", "Talk by text or voice"), color = Hf.TextMuted, fontSize = 12.sp, maxLines = 1)
            }
            Icon(painterResource(R.drawable.hf_ic_chat), null, tint = Hf.Gold, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
internal fun PremiumWeeklyPodium(
    players: List<HomePodiumEntry>,
    loading: Boolean,
    failed: Boolean,
    onOpenLeague: () -> Unit,
    onRetry: () -> Unit,
) {
    Surface(
        onClick = onOpenLeague,
        shape = HomeCardShape,
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, SonHarfTheme.Border),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeSectionHeader(sh("HAFTANIN ZİRVESİ", "WEEKLY ELITE"))
            when {
                loading -> HomeLoadingRow()
                failed -> Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Refresh, null, tint = SonHarfTheme.TextSecondary, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(sh("Sıralama yenilenemedi", "Ranking could not refresh"), Modifier.weight(1f), color = SonHarfTheme.TextSecondary, fontSize = 10.sp)
                    TextButton(onClick = onRetry, modifier = Modifier.height(34.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text(sh("YENİLE", "RETRY"), color = SonHarfTheme.Primary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
                else -> Column(Modifier.fillMaxWidth().semantics { isTraversalGroup = true }) {
                    players.take(3).forEachIndexed { index, item ->
                        if (index > 0) HorizontalDivider(color = HomeHairline)
                        Row(
                            Modifier.fillMaxWidth().clearAndSetSemantics {
                                contentDescription = sh(
                                    "${index + 1}. sıra, ${item.row.displayName}, ${item.row.rating} RP",
                                    "Rank ${index + 1}, ${item.row.displayName}, ${item.row.rating} RP",
                                )
                            }.padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${index + 1}",
                                modifier = Modifier.width(24.dp),
                                color = if (index == 0) SonHarfTheme.ActionOrange else SonHarfTheme.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                            )
                            ProfilePhotoAvatarWithGender(
                                avatarPath = if (item.profile?.avatarVisibility == "hidden") null else item.profile?.avatarPath,
                                gender = item.profile?.gender,
                                name = item.row.displayName,
                                size = 34.dp,
                                accent = if (index == 0) SonHarfTheme.ActionOrange else SonHarfTheme.Primary,
                                visible = item.profile?.avatarVisibility != "hidden",
                            )
                            Spacer(Modifier.width(9.dp))
                            Text(
                                item.row.displayName,
                                Modifier.weight(1f),
                                color = SonHarfTheme.TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text("${item.row.rating} RP", color = SonHarfTheme.TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PremiumLeagueProgress(profile: ProfileDto?, onLeague: () -> Unit) {
    val progress = profile?.let { ratingLeagueProgress(it.rating) }
    Surface(
        onClick = onLeague,
        shape = HomeCardShape,
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, SonHarfTheme.Border),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.style_icon_trophy),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        progress?.let { homeLeagueName(it.leagueName) } ?: sh("Lig durumun", "Your league"),
                        color = SonHarfTheme.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(profile?.let { "${it.rating} RP" } ?: "— RP", color = SonHarfTheme.TextSecondary, fontSize = 11.sp)
                }
                Icon(Icons.Rounded.ChevronRight, sh("Lige Git", "Go to League"), tint = SonHarfTheme.TextSecondary)
            }
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress.progress },
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = SonHarfTheme.Primary,
                    trackColor = SonHarfTheme.SurfaceElevated,
                )
            }
        }
    }
}

@Composable
internal fun PremiumOtherGames(onLastLetter: () -> Unit, onLetterPath: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PremiumHomeModeCard(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            title = "Son Harf",
            subtitle = sh("Son harfle yeni kelime bul", "Find a word from the last letter"),
            art = { HomeLastLetterArt() },
            onPlay = onLastLetter,
        )
        PremiumHomeModeCard(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            title = sh("Kelime Yolu", "Word Path"),
            subtitle = sh("Bağlantılı kelimelerle hedefe ulaş", "Reach the goal through linked words"),
            art = { HomeWordPathArt() },
            onPlay = onLetterPath,
        )
    }
}

@Composable
private fun PremiumHomeModeCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    art: @Composable () -> Unit,
    onPlay: () -> Unit,
) {
    HfCard(onClick = onPlay, modifier = modifier.heightIn(min = 176.dp)) {
        Column(Modifier.fillMaxSize().padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 14.dp)) {
            Box(Modifier.fillMaxWidth().height(78.dp), contentAlignment = Alignment.Center) { art() }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    color = Hf.Ivory,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(Icons.Rounded.ChevronRight, null, tint = Hf.Gold, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Hf.TextMuted, fontSize = 12.sp, lineHeight = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun HomeLastLetterArt() {
    Box(Modifier.size(width = 120.dp, height = 74.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val stroke = 3.dp.toPx()
            listOf(-150f, 180f, 150f, -30f, 0f, 30f).forEach { deg ->
                val rad = Math.toRadians(deg.toDouble())
                val dir = Offset(kotlin.math.cos(rad).toFloat(), kotlin.math.sin(rad).toFloat())
                val start = c + dir * (size.height * .56f)
                val end = c + dir * (size.height * .72f)
                drawLine(Hf.Gold, start, end, strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
        }
        HfLetterTile("A", 50.dp, modifier = Modifier.rotate(-7f), fontSize = 30.sp)
    }
}

@Composable
private fun HomeWordPathArt() {
    Box(Modifier.size(width = 150.dp, height = 74.dp)) {
        Canvas(Modifier.matchParentSize()) {
            val gold = Hf.Gold
            val w = 2.dp.toPx()
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width * .30f, size.height * .22f)
                lineTo(size.width * .60f, size.height * .30f)
                moveTo(size.width * .18f, size.height * .30f)
                lineTo(size.width * .18f, size.height * .78f)
                lineTo(size.width * .38f, size.height * .78f)
                moveTo(size.width * .80f, size.height * .42f)
                lineTo(size.width * .80f, size.height * .78f)
                lineTo(size.width * .62f, size.height * .78f)
            }
            drawPath(path, gold, style = androidx.compose.ui.graphics.drawscope.Stroke(width = w))
        }
        HomePathChip("TOHUM", Modifier.align(Alignment.TopStart))
        HomePathChip("PARK", Modifier.align(Alignment.TopEnd).padding(top = 8.dp))
        HomePathChip("ŞEHİR", Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun HomePathChip(text: String, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(7.dp), color = Hf.Ivory, shadowElevation = 2.dp) {
        Text(text, Modifier.padding(horizontal = 7.dp, vertical = 3.dp), color = Hf.Ink, fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
internal fun PremiumDailyObjective(onClick: () -> Unit) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var players by remember { mutableStateOf<List<WeeklyTopPlayerV210>>(emptyList()) }
    var loading by remember { mutableStateOf(backend != null) }
    var failed by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(backend, reloadKey) {
        val activeBackend = backend
        if (activeBackend == null) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        failed = false
        runCatching { activeBackend.getWeeklyTopV210(limit = 3) }
            .onSuccess { players = it.take(3) }
            .onFailure { failed = true }
        loading = false
    }

    Surface(
        onClick = onClick,
        shape = HomeCardShape,
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, SonHarfTheme.Border),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.style_icon_trophy),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        sh("HAFTANIN İLK 3 OYUNCUSU", "WEEKLY TOP 3"),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = SonHarfTheme.TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = .3.sp,
                    )
                    Text(
                        sh("Haftalık rekabet sıralaması", "Weekly competitive ranking"),
                        color = SonHarfTheme.TextSecondary,
                        fontSize = 9.sp,
                        maxLines = 1,
                    )
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.TextSecondary, modifier = Modifier.size(20.dp))
            }
            when {
                loading -> HomeLoadingRow()
                failed -> Surface(
                    shape = HomeControlShape,
                    color = SonHarfTheme.SurfaceSecondary.copy(alpha = .72f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 10.dp, end = 5.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Refresh, null, tint = SonHarfTheme.TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(
                            sh("Sıralama şu an güncellenemiyor", "Ranking is temporarily unavailable"),
                            Modifier.weight(1f),
                            color = SonHarfTheme.TextSecondary,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        TextButton(
                            onClick = { reloadKey += 1 },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 7.dp),
                        ) {
                            Text(sh("YENİLE", "RETRY"), color = SonHarfTheme.TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                else -> Column(Modifier.fillMaxWidth()) {
                    listOf(1 to players.getOrNull(0), 2 to players.getOrNull(1), 3 to players.getOrNull(2)).forEachIndexed { index, (place, player) ->
                        if (index > 0) HorizontalDivider(color = HomeHairline)
                        Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(26.dp),
                                shape = HomeMicroShape,
                                color = if (place == 1) SonHarfTheme.ActionOrange.copy(alpha = .12f) else SonHarfTheme.SurfaceSecondary,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "$place",
                                        color = if (place == 1) SonHarfTheme.ActionOrange else SonHarfTheme.TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                    )
                                }
                            }
                            Spacer(Modifier.width(9.dp))
                            if (player != null) {
                                ProfilePhotoAvatarWithGender(
                                    avatarPath = player.avatarUrl,
                                    gender = null,
                                    name = player.username,
                                    size = 34.dp,
                                    accent = if (place == 1) SonHarfTheme.ActionOrange else SonHarfTheme.Primary,
                                    visible = true,
                                )
                            } else {
                                Surface(Modifier.size(34.dp), shape = CircleShape, color = SonHarfTheme.SurfaceSecondary) {}
                            }
                            Spacer(Modifier.width(9.dp))
                            Text(
                                player?.username?.ifBlank { sh("Oyuncu", "Player") } ?: "—",
                                modifier = Modifier.weight(1f),
                                color = SonHarfTheme.TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                player?.let { "${it.rp} RP" } ?: "— RP",
                                color = SonHarfTheme.TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeLoadingRow() {
    Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(18.dp), color = SonHarfTheme.Primary, strokeWidth = 2.dp)
    }
}

@Composable
private fun HomeSectionHeader(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.style_icon_trophy),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.width(7.dp))
        Text(title, Modifier.weight(1f), color = SonHarfTheme.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black)
        Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.TextSecondary, modifier = Modifier.size(18.dp))
    }
}

@Composable
internal fun PremiumHomeExtras(profile: ProfileDto?, onShop: () -> Unit, onSocial: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Surface(onClick = onShop, shape = HomeCardShape, color = SonHarfTheme.Surface, border = BorderStroke(1.dp, SonHarfTheme.Border)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CardGiftcard, null, tint = SonHarfTheme.Primary)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(sh("Günlük ücretsiz hediye", "Daily free gift"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SonHarfTheme.TextPrimary)
                    Text(sh("Bugünkü ödülünü mağazada kontrol et.", "Check today's reward in the shop."), fontSize = 11.sp, color = SonHarfTheme.TextSecondary)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.TextSecondary)
            }
        }
        Surface(onClick = onShop, shape = HomeCardShape, color = SonHarfTheme.Surface, border = BorderStroke(1.dp, SonHarfTheme.Border)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WorkspacePremium, null, tint = SonHarfTheme.Lavender)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (profile?.isVip == true) sh("PRO koleksiyonun", "Your PRO collection") else sh("PRO'yu keşfet", "Explore PRO"),
                        color = SonHarfTheme.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(sh("Reklamsız kullanım · Kozmetik · Konfor", "Ad-free · Cosmetics · Comfort"), color = SonHarfTheme.TextSecondary, fontSize = 11.sp)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.TextSecondary)
            }
        }
        TextButton(onClick = onSocial, modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)) {
            Icon(Icons.Rounded.Groups, null, modifier = Modifier.size(19.dp), tint = SonHarfTheme.Primary)
            Spacer(Modifier.width(7.dp))
            Text(sh("Arkadaşların ve rakiplerin", "Friends and rivals"), color = SonHarfTheme.Primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun homeLeagueName(value: String): String = when (value) {
    "BRONZ" -> sh("Bronz", "Bronze")
    "GÜMÜŞ" -> sh("Gümüş", "Silver")
    "ALTIN" -> sh("Altın", "Gold")
    "PLATİN" -> sh("Platin", "Platinum")
    "ELMAS" -> sh("Elmas", "Diamond")
    "EFSANE" -> sh("Efsane", "Legend")
    else -> value
}
