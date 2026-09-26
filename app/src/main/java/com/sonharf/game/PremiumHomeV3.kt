package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
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
private val HomeHairline = Color(0xFFD2DBE5)

@Composable
internal fun PremiumHomeCommandDeck(
    profile: ProfileDto?,
    onProfile: () -> Unit,
    onSiege: () -> Unit,
    onShop: () -> Unit,
    onPro: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
                        color = Hf.Text,
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
                color = Hf.Text,
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

/** The Kelime Tahtı logo is the main game's banner; it breathes gently above the play button. */
@Composable
private fun HomeSiegeHero(onSiege: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "home-logo")
    val breathe by pulse.animateFloat(1f, 1.035f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "home-logo-scale")
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HfGameArt(
            R.drawable.kelime_tahti_brand_logo,
            320.dp,
            170.dp,
            modifier = Modifier.clickable(onClick = onSiege).graphicsLayer { scaleX = breathe; scaleY = breathe },
            description = "KELİME TAHTI",
        )
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

    HfGamePanel(HfPanel.GoldSet, Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(50.dp).background(Color.White.copy(alpha = .9f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.AssignmentTurnedIn, null, tint = HfPanel.GoldSet[2], modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(sh("Günlük Görevler", "Daily Tasks"), color = Hf.Ink, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Text(
                    if (streakDays > 0) sh("🔥 $streakDays gün seri", "🔥 $streakDays-day streak") else sh("Bugünün ödüllerini topla", "Collect today's rewards"),
                    color = Hf.Ink.copy(alpha = .78f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(10.dp).background(Color.White.copy(alpha = .55f), CircleShape)) {
                    Box(Modifier.fillMaxWidth(progress.coerceAtLeast(.04f)).fillMaxHeight().background(Hf.Green, CircleShape))
                }
            }
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(52.dp).background(Hf.Ink.copy(alpha = .85f), CircleShape), contentAlignment = Alignment.Center) {
                Text(if (loading) "…" else "$completedTasks/2", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
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
internal fun PremiumOtherGames(onLastLetter: () -> Unit, onWorkshop: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PremiumHomeModeCard(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            title = "Son Harf",
            subtitle = sh("Son harfle yeni kelime bul", "Find a word from the last letter"),
            art = { HfGameArt(R.drawable.son_harf_game_icon, 86.dp, 86.dp, description = "Son Harf") },
            colors = HfPanel.GreenSet,
            onPlay = onLastLetter,
        )
        PremiumHomeModeCard(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            title = sh("Kelime Atölyesi", "Word Workshop"),
            subtitle = sh("7 harfle 3 görevi tamamla", "Finish 3 tasks with 7 letters"),
            art = { HfGameArt(R.drawable.kelime_atolyesi_game_icon, 86.dp, 86.dp, description = "Kelime Atölyesi") },
            colors = HfPanel.BlueSet,
            onPlay = onWorkshop,
        )
    }
}

@Composable
private fun PremiumHomeModeCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    art: @Composable () -> Unit,
    colors: List<Color>,
    onPlay: () -> Unit,
) {
    HfGamePanel(colors, modifier.heightIn(min = 214.dp), onClick = onPlay) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(96.dp).background(Brush.radialGradient(listOf(Color.White.copy(alpha = .55f), Color.Transparent)), CircleShape),
                contentAlignment = Alignment.Center,
            ) { art() }
            // One line for every title: long names get a smaller size so both cards stay symmetric.
            // (No BoxWithConstraints here: the card row measures intrinsics, which subcompose can't answer.)
            Text(
                title,
                color = Color.White,
                fontSize = if (title.length > 10) 16.sp else 20.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                color = Color.White.copy(alpha = .9f),
                fontSize = 12.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.heightIn(min = 30.dp),
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(8.dp))
            HfPanelPill(sh("OYNA ▶", "PLAY ▶"), ink = colors[2])
        }
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

    HfGamePanel(HfPanel.NavySet, Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Image(painterResource(R.drawable.style_icon_trophy), null, Modifier.size(30.dp), contentScale = ContentScale.Fit)
                Spacer(Modifier.width(8.dp))
                Text(
                    sh("HAFTANIN İLK 3'Ü", "WEEKLY TOP 3"),
                    color = Hf.GoldLight,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = .8.sp,
                    maxLines = 1,
                )
                Spacer(Modifier.width(8.dp))
                Image(painterResource(R.drawable.style_icon_trophy), null, Modifier.size(30.dp), contentScale = ContentScale.Fit)
            }
            when {
                loading -> Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Hf.GoldLight, strokeWidth = 2.dp)
                }
                failed -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        sh("Sıralama şu an güncellenemiyor", "Ranking is temporarily unavailable"),
                        Modifier.weight(1f),
                        color = Color.White.copy(alpha = .8f),
                        fontSize = 13.sp,
                    )
                    HfPill(onClick = { reloadKey += 1 }) {
                        Text(sh("YENİLE", "RETRY"), color = Hf.Text, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
                // Podium side by side: 2nd, 1st (raised, bigger), 3rd.
                else -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 0, 2).forEach { index ->
                        HomePodiumSpot(index, players.getOrNull(index), Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun HomePodiumSpot(index: Int, player: WeeklyTopPlayerV210?, modifier: Modifier) {
    val medal = HomeMedals[index]
    val first = index == 0
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                Modifier
                    .size(if (first) 76.dp else 60.dp)
                    .background(Brush.verticalGradient(listOf(medal.first, medal.second)), CircleShape)
                    .padding(3.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (player != null) {
                    ProfilePhotoAvatarWithGender(
                        avatarPath = player.avatarUrl,
                        gender = null,
                        name = player.username,
                        size = if (first) 70.dp else 54.dp,
                        accent = medal.first,
                        visible = true,
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = .15f), CircleShape))
                }
            }
            Box(
                Modifier.size(24.dp).background(Brush.verticalGradient(listOf(medal.first, medal.second)), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("${index + 1}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            player?.username?.ifBlank { sh("Oyuncu", "Player") } ?: sh("Boş", "Open"),
            color = if (player != null) Color.White else Color.White.copy(alpha = .5f),
            fontSize = if (first) 15.sp else 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        // The step of the podium: tallest for the winner.
        Box(
            Modifier
                .fillMaxWidth()
                .height(if (first) 54.dp else if (index == 1) 40.dp else 30.dp)
                .background(
                    Brush.verticalGradient(listOf(medal.first.copy(alpha = .55f), medal.second.copy(alpha = .25f))),
                    RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(player?.let { "${it.rp} RP" } ?: "—", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
    }
}

/** Gold, silver and bronze gradients for the weekly podium. */
private val HomeMedals = listOf(
    Color(0xFFFFD36B) to Color(0xFFD99A1E),
    Color(0xFFE3E8EE) to Color(0xFF9AA5B2),
    Color(0xFFF0B27A) to Color(0xFFB36A2E),
)

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

/** Home entry to the mascot room: shown when the player owns a mascot. */
@Composable
internal fun PremiumMascotRoomCard() {
    if (!WordSiegeMascotOwnership.hasAny) return
    var open by remember { mutableStateOf(false) }
    HfGamePanel(HfPanel.PurpleSet, Modifier.fillMaxWidth(), onClick = { open = true }) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(50.dp).background(Color.White.copy(alpha = .9f), CircleShape), contentAlignment = Alignment.Center) {
                Text("💞", fontSize = 26.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(sh("Maskot Odası", "Mascot Room"), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(
                    sh("Sev, oyna, besle: dostluğunu büyüt", "Love, play, feed: grow your bond"),
                    color = Color.White.copy(alpha = .9f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            HfPanelPill(sh("GİR", "ENTER"), ink = HfPanel.PurpleSet[2])
        }
    }
    if (open) MascotRoomDialog(onDismiss = { open = false })
}
