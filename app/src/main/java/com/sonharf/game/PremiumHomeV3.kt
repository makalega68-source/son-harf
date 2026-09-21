package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

private val HomeHeroShape = RoundedCornerShape(24.dp)
private val HomeCardShape = RoundedCornerShape(19.dp)

@Composable
internal fun PremiumHomeCommandDeck(
    profile: ProfileDto?,
    onProfile: () -> Unit,
    onSiege: () -> Unit,
    onSocial: () -> Unit,
) {
    val homeBackend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    var dailyDashboard by remember { mutableStateOf<GrowthDashboardDto?>(null) }
    var dailyPlayStreak by remember { mutableIntStateOf(0) }
    var dailyLoading by remember { mutableStateOf(homeBackend != null) }

    LaunchedEffect(homeBackend) {
        val backend = homeBackend
        if (backend == null) {
            dailyLoading = false
            return@LaunchedEffect
        }
        dailyDashboard = runCatching { backend.getGrowthDashboard() }.getOrNull()
        dailyPlayStreak = runCatching { backend.getMetaProgressV2().dailyPlayStreak }.getOrDefault(0)
        dailyLoading = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            onClick = onProfile,
            shape = HomeCardShape,
            color = SonHarfTheme.Surface,
            border = BorderStroke(1.dp, SonHarfTheme.Border),
            shadowElevation = 3.dp,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FramedProfilePhotoAvatar(
                    avatarPath = profile?.avatarPath,
                    gender = profile?.gender,
                    name = profile?.displayName ?: sh("Oyuncu", "Player"),
                    size = 58.dp,
                    frameId = SonHarfCosmetics.profileFrameId,
                    accent = if (profile?.isVip == true) SonHarfTheme.PremiumGoldLight else SonHarfTheme.Primary,
                    visible = profile?.avatarVisibility != "hidden",
                    isPro = profile?.isVip == true,
                )
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            profile?.displayName ?: sh("Profilin", "Your profile"),
                            modifier = Modifier.weight(1f, fill = false),
                            color = SonHarfTheme.TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (profile?.isVip == true) {
                            Spacer(Modifier.width(7.dp))
                            Surface(shape = RoundedCornerShape(7.dp), color = SonHarfTheme.PremiumGold) {
                                Text(
                                    "PRO",
                                    Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                )
                            }
                        }
                    }
                    Text(
                        profile?.let { homeLeagueName(ratingLeagueProgress(it.rating).leagueName) }
                            ?: sh("Lig bilgisi", "League status"),
                        color = SonHarfTheme.TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    HomeStatChip(
                        icon = Icons.Rounded.MilitaryTech,
                        text = profile?.let { "${it.rating} RP" } ?: "— RP",
                        background = PurchasedCasualUi2.Orange.copy(alpha = .13f),
                        accent = PurchasedCasualUi2.Orange,
                    )
                    HomeStatChip(
                        icon = Icons.Rounded.Toll,
                        text = "${profile?.diamonds?.toString() ?: "—"} Coin",
                        background = PurchasedCasualUi2.Blue.copy(alpha = .10f),
                        accent = PurchasedCasualUi2.Blue,
                    )
                }
                Spacer(Modifier.width(5.dp))
                Surface(
                    onClick = onSocial,
                    shape = CircleShape,
                    color = PurchasedCasualUi2.Blue.copy(alpha = .10f),
                    border = BorderStroke(1.dp, PurchasedCasualUi2.Blue.copy(alpha = .16f)),
                ) {
                    Icon(
                        Icons.Rounded.Notifications,
                        sh("Bildirimler ve davetler", "Notifications and invites"),
                        tint = PurchasedCasualUi2.Blue,
                        modifier = Modifier.padding(10.dp).size(19.dp),
                    )
                }
            }
        }

        Surface(
            shape = HomeHeroShape,
            color = Color.Transparent,
            border = BorderStroke(1.dp, Color.White.copy(alpha = .18f)),
            shadowElevation = 7.dp,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(SonHarfTheme.HeroStart, SonHarfTheme.HeroMiddle, SonHarfTheme.HeroEnd),
                        ),
                        HomeHeroShape,
                    ),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(99.dp), color = Color.White.copy(alpha = .16f)) {
                            Row(
                                Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.size(7.dp).background(Color.White, CircleShape))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    sh("ANA ARENA", "MAIN ARENA"),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = .6.sp,
                                )
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Rounded.EmojiEvents, null, tint = SonHarfTheme.PremiumGoldLight, modifier = Modifier.size(19.dp))
                    }

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(
                                sh("KELİMELERLE ALAN SAVAŞI", "WORDS MEET TERRITORY"),
                                color = Color.White.copy(alpha = .84f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "KELİME\nKUŞATMASI",
                                color = Color.White,
                                fontSize = 28.sp,
                                lineHeight = 29.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-.5).sp,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White.copy(alpha = .12f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = .18f)),
                        ) {
                            Image(
                                painter = painterResource(R.drawable.word_siege_home_badge),
                                contentDescription = sh("Kelime Kuşatması", "Word Siege"),
                                modifier = Modifier.padding(8.dp).size(width = 118.dp, height = 90.dp),
                                contentScale = ContentScale.Fit,
                            )
                        }
                    }

                    Text(
                        sh(
                            "Kelimeyi kur. Alanı ele geçir. Rakibini geç.",
                            "Build your word. Claim territory. Outplay your rival.",
                        ),
                        color = Color.White.copy(alpha = .9f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )

                    PurchasedGlossButton(
                        text = sh("HEMEN OYNA", "PLAY NOW"),
                        onClick = onSiege,
                        modifier = Modifier.fillMaxWidth(),
                        color = PurchasedCasualUi2.Green,
                        icon = Icons.Rounded.PlayArrow,
                        minHeight = 56.dp,
                    )
                }
            }
        }

        PremiumDailyTasksStrip(
            dashboard = dailyDashboard,
            streakDays = dailyPlayStreak,
            loading = dailyLoading,
        )
    }
}

@Composable
private fun HomeStatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    background: Color,
    accent: Color,
) {
    Surface(shape = RoundedCornerShape(9.dp), color = background) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(text, color = SonHarfTheme.TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PremiumDailyTasksStrip(
    dashboard: GrowthDashboardDto?,
    streakDays: Int,
    loading: Boolean,
) {
    val matches = dashboard?.matchesToday?.coerceIn(0, 3) ?: 0
    val checkInDone = dashboard?.dailyClaimed == true
    val challengeDone = dashboard?.dailyChallengeClaimed == true || matches >= 3
    val completedTasks = (if (checkInDone) 1 else 0) + (if (challengeDone) 1 else 0)
    val checkInProgress = if (checkInDone) 1f else 0f
    val challengeProgress = matches / 3f
    val progress = if (dashboard == null) 0f else ((checkInProgress + challengeProgress) / 2f).coerceIn(0f, 1f)
    val percent = (progress * 100).toInt()

    Surface(
        shape = HomeCardShape,
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, SonHarfTheme.Border),
        shadowElevation = 2.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = SonHarfTheme.PrimarySoft) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    null,
                    tint = SonHarfTheme.Primary,
                    modifier = Modifier.padding(9.dp).size(21.dp),
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        sh("GÜNLÜK GÖREVLER", "DAILY TASKS"),
                        color = SonHarfTheme.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        if (loading) "…" else "$percent%",
                        color = SonHarfTheme.Primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = SonHarfTheme.Primary,
                    trackColor = SonHarfTheme.SurfaceElevated,
                )
                Text(
                    if (loading) sh("Görevler yükleniyor", "Loading tasks")
                    else sh("$completedTasks / 2 görev tamamlandı", "$completedTasks / 2 tasks completed"),
                    color = SonHarfTheme.TextSecondary,
                    fontSize = 10.sp,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Rounded.LocalFireDepartment,
                    null,
                    tint = SonHarfTheme.ActionOrange,
                    modifier = Modifier.size(21.dp),
                )
                Text(
                    sh("$streakDays gün", "$streakDays days"),
                    color = SonHarfTheme.TextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
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
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.EmojiEvents, null, tint = SonHarfTheme.PremiumGold, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    sh("HAFTANIN ZİRVESİ", "WEEKLY ELITE"),
                    Modifier.weight(1f),
                    color = SonHarfTheme.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                )
                Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.TextSecondary, modifier = Modifier.size(18.dp))
            }
            when {
                loading -> Box(Modifier.fillMaxWidth().height(68.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = SonHarfTheme.Primary, strokeWidth = 2.dp)
                }
                failed -> Row(Modifier.fillMaxWidth().heightIn(min = 60.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(sh("Sıralama yenilenemedi", "Ranking could not refresh"), Modifier.weight(1f), color = SonHarfTheme.TextSecondary, fontSize = 11.sp)
                    TextButton(onClick = onRetry) { Text(sh("YENİLE", "RETRY"), color = SonHarfTheme.Primary) }
                }
                else -> Row(Modifier.fillMaxWidth().semantics { isTraversalGroup = true }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    players.take(3).forEachIndexed { index, item ->
                        Surface(
                            modifier = Modifier.weight(1f).clearAndSetSemantics {
                                contentDescription = sh("${index + 1}. sıra, ${item.row.displayName}, ${item.row.rating} RP", "Rank ${index + 1}, ${item.row.displayName}, ${item.row.rating} RP")
                            },
                            shape = RoundedCornerShape(13.dp),
                            color = if (index == 0) PurchasedCasualUi2.Orange.copy(alpha = .12f) else SonHarfTheme.SurfaceElevated,
                        ) {
                            Column(Modifier.padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${index + 1}.", color = if (index == 0) SonHarfTheme.PremiumGold else SonHarfTheme.TextSecondary, fontWeight = FontWeight.Black)
                                Spacer(Modifier.height(4.dp))
                                ProfilePhotoAvatarWithGender(
                                    avatarPath = if (item.profile?.avatarVisibility == "hidden") null else item.profile?.avatarPath,
                                    gender = item.profile?.gender,
                                    name = item.row.displayName,
                                    size = 38.dp,
                                    accent = if (index == 0) SonHarfTheme.PremiumGold else PurchasedCasualUi2.Blue,
                                    visible = item.profile?.avatarVisibility != "hidden",
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(item.row.displayName, color = SonHarfTheme.TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${item.row.rating} RP", color = SonHarfTheme.TextSecondary, fontSize = 8.sp)
                            }
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
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.EmojiEvents, null, tint = SonHarfTheme.PremiumGold, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        progress?.let { homeLeagueName(it.leagueName) } ?: sh("Lig durumun", "Your league"),
                        color = SonHarfTheme.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(profile?.let { "${it.rating} RP" } ?: "— RP", color = SonHarfTheme.TextSecondary, fontSize = 12.sp)
                }
                Icon(Icons.Rounded.ChevronRight, sh("Lige Git", "Go to League"), tint = PurchasedCasualUi2.Blue)
            }
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress.progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = PurchasedCasualUi2.Green,
                    trackColor = SonHarfTheme.SurfaceElevated,
                )
            }
        }
    }
}

@Composable
internal fun PremiumOtherGames(onLastLetter: () -> Unit, onLetterPath: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            sh("DİĞER OYUNLAR", "MORE GAMES"),
            color = SonHarfTheme.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = .5.sp,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumHomeModeCard(
                modifier = Modifier.weight(1f),
                logo = R.drawable.son_harf_app_icon_master,
                title = sh("SON HARF", "LAST LETTER"),
                subtitle = sh("Hızlı kelime düellosu", "A quick word duel"),
                accent = PurchasedCasualUi2.Blue,
                onPlay = onLastLetter,
            )
            PremiumHomeModeCard(
                modifier = Modifier.weight(1f),
                logo = R.drawable.harf_yolu_logo,
                title = sh("HARF YOLU", "LETTER PATH"),
                subtitle = sh("5 harfli kelime rotanı tamamla", "Complete your five-letter path"),
                accent = PurchasedCasualUi2.Purple,
                onPlay = onLetterPath,
            )
        }
    }
}

@Composable
private fun PremiumHomeModeCard(
    modifier: Modifier,
    logo: Int,
    title: String,
    subtitle: String,
    accent: Color,
    onPlay: () -> Unit,
) {
    Surface(
        onClick = onPlay,
        modifier = modifier.height(156.dp),
        shape = HomeCardShape,
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, accent.copy(alpha = .17f)),
        shadowElevation = 2.dp,
    ) {
        Column(
            Modifier.fillMaxSize().padding(13.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(13.dp), color = accent.copy(alpha = .09f)) {
                    Image(
                        painter = painterResource(logo),
                        contentDescription = null,
                        modifier = Modifier.padding(7.dp).size(42.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(8.dp).background(accent, CircleShape))
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = SonHarfTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = SonHarfTheme.TextSecondary, fontSize = 9.sp, lineHeight = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Surface(shape = RoundedCornerShape(99.dp), color = accent.copy(alpha = .12f)) {
                    Row(
                        Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(sh("OYNA", "PLAY"), color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(3.dp))
                        Icon(Icons.Rounded.PlayArrow, null, tint = accent, modifier = Modifier.size(14.dp))
                    }
                }
            }
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

    Surface(
        onClick = onClick,
        shape = HomeCardShape,
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, SonHarfTheme.Border),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.EmojiEvents, null, tint = SonHarfTheme.PremiumGold, modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    sh("HAFTANIN İLK 3 OYUNCUSU", "WEEKLY TOP 3"),
                    Modifier.weight(1f),
                    color = SonHarfTheme.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                )
                Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.TextSecondary)
            }
            when {
                loading -> Box(Modifier.fillMaxWidth().height(84.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = SonHarfTheme.Primary, strokeWidth = 2.dp)
                }
                failed -> Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(sh("Haftalık sıralama yenilenemedi.", "Weekly ranking could not refresh."), Modifier.weight(1f), color = SonHarfTheme.TextSecondary, fontSize = 10.sp)
                    TextButton(onClick = { reloadKey += 1 }) { Text(sh("YENİLE", "RETRY"), color = SonHarfTheme.Primary, fontSize = 10.sp) }
                }
                else -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    listOf(2 to players.getOrNull(1), 1 to players.getOrNull(0), 3 to players.getOrNull(2)).forEach { (place, player) ->
                        val first = place == 1
                        Surface(
                            modifier = Modifier.weight(1f).then(if (first) Modifier.padding(bottom = 6.dp) else Modifier),
                            shape = RoundedCornerShape(13.dp),
                            color = if (first) PurchasedCasualUi2.Orange.copy(alpha = .12f) else SonHarfTheme.SurfaceElevated,
                        ) {
                            Column(Modifier.padding(horizontal = 6.dp, vertical = if (first) 11.dp else 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$place.", color = if (first) SonHarfTheme.PremiumGold else SonHarfTheme.TextSecondary, fontSize = if (first) 13.sp else 10.sp, fontWeight = FontWeight.Black)
                                Spacer(Modifier.height(4.dp))
                                if (player != null) {
                                    ProfilePhotoAvatarWithGender(
                                        avatarPath = player.avatarUrl,
                                        gender = null,
                                        name = player.username,
                                        size = if (first) 50.dp else 42.dp,
                                        accent = if (first) SonHarfTheme.PremiumGold else PurchasedCasualUi2.Blue,
                                        visible = true,
                                    )
                                } else {
                                    Surface(Modifier.size(if (first) 50.dp else 42.dp), shape = CircleShape, color = SonHarfTheme.SurfaceSecondary) {}
                                }
                                Spacer(Modifier.height(5.dp))
                                Text(player?.username?.ifBlank { sh("Oyuncu", "Player") } ?: "—", color = SonHarfTheme.TextPrimary, fontSize = if (first) 10.sp else 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(player?.let { "${it.rp} RP" } ?: "— RP", color = if (first) SonHarfTheme.PremiumGold else SonHarfTheme.TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PremiumHomeExtras(profile: ProfileDto?, onShop: () -> Unit, onSocial: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Surface(onClick = onShop, shape = HomeCardShape, color = SonHarfTheme.Surface, border = BorderStroke(1.dp, SonHarfTheme.Border), shadowElevation = 2.dp) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CardGiftcard, null, tint = PurchasedCasualUi2.Orange)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(sh("Günlük ücretsiz hediye", "Daily free gift"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SonHarfTheme.TextPrimary)
                    Text(sh("Bugünkü ödülünü mağazada kontrol et.", "Check today's reward in the shop."), fontSize = 11.sp, color = SonHarfTheme.TextSecondary)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = PurchasedCasualUi2.Blue)
            }
        }
        Surface(onClick = onShop, shape = HomeCardShape, color = SonHarfTheme.Surface, border = BorderStroke(1.dp, SonHarfTheme.Border), shadowElevation = 2.dp) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WorkspacePremium, null, tint = SonHarfTheme.PremiumGold)
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
                Icon(Icons.Rounded.ChevronRight, null, tint = PurchasedCasualUi2.Blue)
            }
        }
        TextButton(onClick = onSocial, modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)) {
            Icon(Icons.Rounded.Groups, null, modifier = Modifier.size(19.dp), tint = PurchasedCasualUi2.Blue)
            Spacer(Modifier.width(7.dp))
            Text(sh("Arkadaşların ve rakiplerin", "Friends and rivals"), color = PurchasedCasualUi2.Blue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
