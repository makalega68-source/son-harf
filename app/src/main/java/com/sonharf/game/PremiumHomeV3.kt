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

private val MonsterHeroShape = RoundedCornerShape(22.dp)
private val MonsterCardShape = RoundedCornerShape(18.dp)
private val MonsterControlShape = RoundedCornerShape(12.dp)

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
            shape = MonsterCardShape,
            color = SonHarfTheme.Surface,
            border = BorderStroke(1.dp, SonHarfTheme.Border),
            shadowElevation = 0.dp,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FramedProfilePhotoAvatar(
                    avatarPath = profile?.avatarPath,
                    gender = profile?.gender,
                    name = profile?.displayName ?: sh("Oyuncu", "Player"),
                    size = 56.dp,
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
                            Surface(shape = RoundedCornerShape(7.dp), color = SonHarfTheme.Primary) {
                                Text(
                                    "PRO",
                                    Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    color = SonHarfTheme.OnPrimary,
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
                        background = SonHarfTheme.ActionOrange.copy(alpha = .18f),
                        accent = SonHarfTheme.ActionOrange,
                    )
                    HomeStatChip(
                        icon = Icons.Rounded.Toll,
                        text = "${profile?.diamonds?.toString() ?: "—"} Coin",
                        background = SonHarfTheme.PrimarySoft,
                        accent = SonHarfTheme.Primary,
                    )
                }
                Spacer(Modifier.width(5.dp))
                Surface(
                    onClick = onSocial,
                    shape = CircleShape,
                    color = SonHarfTheme.SurfaceElevated,
                    border = BorderStroke(1.dp, SonHarfTheme.Border),
                ) {
                    Icon(
                        Icons.Rounded.Notifications,
                        sh("Bildirimler ve davetler", "Notifications and invites"),
                        tint = SonHarfTheme.Primary,
                        modifier = Modifier.padding(10.dp).size(19.dp),
                    )
                }
            }
        }

        Surface(
            shape = MonsterHeroShape,
            color = Color.Transparent,
            border = BorderStroke(1.dp, Color.White.copy(alpha = .08f)),
            shadowElevation = 0.dp,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(SonHarfTheme.HeroStart, SonHarfTheme.HeroMiddle, SonHarfTheme.HeroEnd),
                        ),
                        MonsterHeroShape,
                    ),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(99.dp), color = Color.Black.copy(alpha = .22f)) {
                            Row(
                                Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.size(7.dp).background(SonHarfTheme.Primary, CircleShape))
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
                        Icon(Icons.Rounded.Favorite, null, tint = SonHarfTheme.Primary, modifier = Modifier.size(18.dp))
                    }

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(
                                sh("KELİMELERLE ALAN SAVAŞI", "WORDS MEET TERRITORY"),
                                color = Color.White.copy(alpha = .82f),
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
                            color = Color.Black.copy(alpha = .20f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = .10f)),
                        ) {
                            Image(
                                painter = painterResource(R.drawable.word_siege_home_badge),
                                contentDescription = sh("Kelime Kuşatması", "Kelime Kuşatması"),
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
                        color = Color.White.copy(alpha = .88f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )

                    Button(
                        onClick = onSiege,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SonHarfTheme.Primary,
                            contentColor = SonHarfTheme.OnPrimary,
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            sh("HEMEN OYNA", "PLAY NOW"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = .55.sp,
                        )
                    }
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
        shape = MonsterCardShape,
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, SonHarfTheme.Border),
        shadowElevation = 0.dp,
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
        shape = MonsterCardShape,
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, SonHarfTheme.Border),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.EmojiEvents, null, tint = SonHarfTheme.Primary, modifier = Modifier.size(20.dp))
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
                            color = if (index == 0) SonHarfTheme.ActionOrange.copy(alpha = .16f) else SonHarfTheme.SurfaceElevated,
                        ) {
                            Column(Modifier.padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${index + 1}.", color = if (index == 0) SonHarfTheme.Primary else SonHarfTheme.TextSecondary, fontWeight = FontWeight.Black)
                                Spacer(Modifier.height(4.dp))
                                ProfilePhotoAvatarWithGender(
                                    avatarPath = if (item.profile?.avatarVisibility == "hidden") null else item.profile?.avatarPath,
                                    gender = item.profile?.gender,
                                    name = item.row.displayName,
                                    size = 38.dp,
                                    accent = if (index == 0) SonHarfTheme.Primary else SonHarfTheme.ActionOrange,
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
        shape = MonsterCardShape,
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, SonHarfTheme.Border),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.EmojiEvents, null, tint = SonHarfTheme.Primary, modifier = Modifier.size(26.dp))
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
                Icon(Icons.Rounded.ChevronRight, sh("Lige Git", "Go to League"), tint = SonHarfTheme.Primary)
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
                accent = SonHarfTheme.ActionOrange,
                onPlay = onLastLetter,
            )
            PremiumHomeModeCard(
                modifier = Modifier.weight(1f),
                logo = R.drawable.harf_yolu_logo,
                title = sh("KELİME YOLU", "WORD PATH"),
                subtitle = sh("Kelime rotanı tamamla", "Complete your word path"),
                accent = SonHarfTheme.Lavender,
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
        shape = MonsterCardShape,
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, SonHarfTheme.Border),
    ) {
        Column(
            Modifier.fillMaxSize().padding(13.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(13.dp), color = SonHarfTheme.SurfaceElevated) {
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
                Surface(shape = RoundedCornerShape(99.dp), color = accent.copy(alpha = .16f)) {
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
        shape = MonsterCardShape,
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, SonHarfTheme.Border),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.EmojiEvents, null, tint = SonHarfTheme.Primary, modifier = Modifier.size(21.dp))
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
                            color = if (first) SonHarfTheme.ActionOrange.copy(alpha = .18f) else SonHarfTheme.SurfaceElevated,
                        ) {
                            Column(Modifier.padding(horizontal = 6.dp, vertical = if (first) 11.dp else 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$place.", color = if (first) SonHarfTheme.Primary else SonHarfTheme.TextSecondary, fontSize = if (first) 13.sp else 10.sp, fontWeight = FontWeight.Black)
                                Spacer(Modifier.height(4.dp))
                                if (player != null) {
                                    ProfilePhotoAvatarWithGender(
                                        avatarPath = player.avatarUrl,
                                        gender = null,
                                        name = player.username,
                                        size = if (first) 50.dp else 42.dp,
                                        accent = if (first) SonHarfTheme.Primary else SonHarfTheme.ActionOrange,
                                        visible = true,
                                    )
                                } else {
                                    Surface(Modifier.size(if (first) 50.dp else 42.dp), shape = CircleShape, color = SonHarfTheme.SurfaceSecondary) {}
                                }
                                Spacer(Modifier.height(5.dp))
                                Text(player?.username?.ifBlank { sh("Oyuncu", "Player") } ?: "—", color = SonHarfTheme.TextPrimary, fontSize = if (first) 10.sp else 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(player?.let { "${it.rp} RP" } ?: "— RP", color = if (first) SonHarfTheme.Primary else SonHarfTheme.TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Black)
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
        Surface(onClick = onShop, shape = MonsterCardShape, color = SonHarfTheme.Surface, border = BorderStroke(1.dp, SonHarfTheme.Border)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CardGiftcard, null, tint = SonHarfTheme.Primary)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(sh("Günlük ücretsiz hediye", "Daily free gift"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SonHarfTheme.TextPrimary)
                    Text(sh("Bugünkü ödülünü mağazada kontrol et.", "Check today's reward in the shop."), fontSize = 11.sp, color = SonHarfTheme.TextSecondary)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.Primary)
            }
        }
        Surface(onClick = onShop, shape = MonsterCardShape, color = SonHarfTheme.Surface, border = BorderStroke(1.dp, SonHarfTheme.Border)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WorkspacePremium, null, tint = SonHarfTheme.ActionOrange)
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
                Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.Primary)
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
