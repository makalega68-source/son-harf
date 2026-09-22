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

private val HomeHeroShape = RoundedCornerShape(26.dp)
private val HomeCardShape = RoundedCornerShape(20.dp)
private val HomeControlShape = RoundedCornerShape(14.dp)
private val HomeDarkText = Color(0xFF0B1B33)

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
        // Marka başlığı: gerçek merkezde, bildirim düğmesi sağda sabit.
        Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "KELİME TAHTI",
                    color = SonHarfTheme.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                Box(
                    Modifier
                        .padding(top = 3.dp)
                        .size(width = 36.dp, height = 3.dp)
                        .background(SonHarfTheme.ActionOrange, RoundedCornerShape(99.dp)),
                )
            }
            Surface(
                onClick = onSocial,
                modifier = Modifier.align(Alignment.CenterEnd).size(44.dp),
                shape = CircleShape,
                color = SonHarfTheme.Surface,
                border = BorderStroke(1.dp, SonHarfTheme.Border),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Notifications,
                        sh("Bildirimler ve davetler", "Notifications and invites"),
                        tint = SonHarfTheme.TextPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        // Kompakt profil: avatar + ad, altında üç eşit sayaç.
        Surface(
            onClick = onProfile,
            shape = HomeCardShape,
            color = SonHarfTheme.Surface,
            border = BorderStroke(1.dp, SonHarfTheme.Border),
            shadowElevation = 1.dp,
        ) {
            Column(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FramedProfilePhotoAvatar(
                        avatarPath = profile?.avatarPath,
                        gender = profile?.gender,
                        name = profile?.displayName ?: sh("Oyuncu", "Player"),
                        size = 46.dp,
                        frameId = SonHarfCosmetics.profileFrameId,
                        accent = if (profile?.isVip == true) SonHarfTheme.Lavender else SonHarfTheme.Primary,
                        visible = profile?.avatarVisibility != "hidden",
                        isPro = profile?.isVip == true,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        profile?.displayName ?: sh("Profilin", "Your profile"),
                        modifier = Modifier.weight(1f),
                        color = SonHarfTheme.TextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (profile?.isVip == true) {
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(8.dp), color = SonHarfTheme.PremiumGoldLight) {
                            Text(
                                "PRO",
                                Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                color = HomeDarkText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.TextSecondary, modifier = Modifier.size(22.dp))
                }
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeCounter(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        icon = Icons.Rounded.MilitaryTech,
                        value = profile?.let { "${it.rating} RP" } ?: "— RP",
                        label = sh("PUAN", "RATING"),
                        background = Color(0xFFFFEBDA),
                        accent = Color(0xFFD9620F),
                    )
                    HomeCounter(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        icon = Icons.Rounded.EmojiEvents,
                        value = profile?.let { homeLeagueName(ratingLeagueProgress(it.rating).leagueName) } ?: "—",
                        label = sh("LİG", "LEAGUE"),
                        background = Color(0xFFEEE9FF),
                        accent = Color(0xFF6A4BD6),
                    )
                    HomeCounter(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        icon = Icons.Rounded.Toll,
                        value = "${profile?.diamonds?.toString() ?: "—"} Coin",
                        label = "COIN",
                        background = Color(0xFFDDF5F4),
                        accent = Color(0xFF0E8F89),
                    )
                }
            }
        }

        // Ana oyun: koyu lacivert-mavi sahne, ortalanmış başlık ve güçlü turuncu buton.
        Surface(
            shape = HomeHeroShape,
            color = Color.Transparent,
            shadowElevation = 4.dp,
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
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(shape = RoundedCornerShape(99.dp), color = SonHarfTheme.Turquoise) {
                        Text(
                            sh("ANA OYUN", "MAIN GAME"),
                            Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = HomeDarkText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = .8.sp,
                        )
                    }
                    Image(
                        painter = painterResource(R.drawable.word_siege_home_badge),
                        contentDescription = sh("Kelime Kuşatması", "Kelime Kuşatması"),
                        modifier = Modifier.size(width = 150.dp, height = 104.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Text(
                        "KELİME KUŞATMASI",
                        color = Color.White,
                        fontSize = 26.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                    Text(
                        sh(
                            "Kelimeyi kur. Alanı ele geçir. Rakibini geç.",
                            "Build your word. Claim territory. Outplay your rival.",
                        ),
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(2.dp))
                    Button(
                        onClick = onSiege,
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        shape = HomeControlShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SonHarfTheme.ActionOrange,
                            contentColor = HomeDarkText,
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            sh("HEMEN OYNA", "PLAY NOW"),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = .8.sp,
                            textAlign = TextAlign.Center,
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
private fun HomeCounter(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    background: Color,
    accent: Color,
) {
    Surface(modifier = modifier, shape = HomeControlShape, color = background) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
            Text(
                value,
                color = HomeDarkText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                label,
                color = SonHarfTheme.TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = .6.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
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

    Surface(
        shape = HomeCardShape,
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, SonHarfTheme.Border),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.CheckCircle,
                null,
                tint = SonHarfTheme.Primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        sh("GÜNLÜK GÖREVLER", "DAILY TASKS"),
                        modifier = Modifier.weight(1f),
                        color = SonHarfTheme.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                    )
                    Text(
                        if (loading) "…" else "$completedTasks / 2",
                        color = SonHarfTheme.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = SonHarfTheme.Primary,
                    trackColor = SonHarfTheme.SurfaceSecondary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Surface(shape = RoundedCornerShape(99.dp), color = Color(0xFFFFEBDA)) {
                Row(
                    Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.LocalFireDepartment,
                        null,
                        tint = Color(0xFFD9620F),
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        sh("$streakDays gün", "$streakDays days"),
                        color = HomeDarkText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                    )
                }
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
        shape = HomeCardShape,
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
            modifier = Modifier.fillMaxWidth(),
            color = SonHarfTheme.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = .8.sp,
            textAlign = TextAlign.Center,
        )
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PremiumHomeModeCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                logo = R.drawable.son_harf_app_icon_master,
                title = sh("SON HARF", "LAST LETTER"),
                subtitle = sh("Hızlı kelime düellosu", "A quick word duel"),
                container = Color(0xFFDDF5F4),
                button = SonHarfTheme.Turquoise,
                onPlay = onLastLetter,
            )
            PremiumHomeModeCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                logo = R.drawable.harf_yolu_logo,
                title = sh("KELİME YOLU", "WORD PATH"),
                subtitle = sh("Kelime rotanı tamamla", "Complete your word path"),
                container = Color(0xFFEEE9FF),
                button = SonHarfTheme.PremiumGoldLight,
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
    container: Color,
    button: Color,
    onPlay: () -> Unit,
) {
    Surface(
        modifier = modifier.heightIn(min = 196.dp),
        shape = HomeCardShape,
        color = container,
        border = BorderStroke(1.dp, SonHarfTheme.Border),
    ) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Surface(shape = CircleShape, color = SonHarfTheme.Surface, modifier = Modifier.size(60.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(logo),
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
            Text(
                title,
                color = HomeDarkText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                modifier = Modifier.weight(1f),
                color = SonHarfTheme.TextSecondary,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Button(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = HomeControlShape,
                colors = ButtonDefaults.buttonColors(containerColor = button, contentColor = HomeDarkText),
                contentPadding = PaddingValues(horizontal = 8.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(sh("OYNA", "PLAY"), fontSize = 13.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
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
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.EmojiEvents, null, tint = SonHarfTheme.ActionOrange, modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    sh("HAFTANIN İLK 3 OYUNCUSU", "WEEKLY TOP 3"),
                    Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
                    Text(sh("Haftalık sıralama yenilenemedi.", "Weekly ranking could not refresh."), Modifier.weight(1f), color = SonHarfTheme.TextSecondary, fontSize = 11.sp)
                    TextButton(onClick = { reloadKey += 1 }) { Text(sh("YENİLE", "RETRY"), color = SonHarfTheme.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black) }
                }
                else -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    listOf(2 to players.getOrNull(1), 1 to players.getOrNull(0), 3 to players.getOrNull(2)).forEach { (place, player) ->
                        val first = place == 1
                        Surface(
                            modifier = Modifier.weight(1f).then(if (first) Modifier.padding(bottom = 6.dp) else Modifier),
                            shape = RoundedCornerShape(13.dp),
                            color = if (first) Color(0xFFFFEBDA) else SonHarfTheme.SurfaceSecondary,
                        ) {
                            Column(Modifier.padding(horizontal = 6.dp, vertical = if (first) 11.dp else 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$place.", color = SonHarfTheme.TextPrimary, fontSize = if (first) 13.sp else 10.sp, fontWeight = FontWeight.Black)
                                Spacer(Modifier.height(4.dp))
                                if (player != null) {
                                    ProfilePhotoAvatarWithGender(
                                        avatarPath = player.avatarUrl,
                                        gender = null,
                                        name = player.username,
                                        size = if (first) 50.dp else 42.dp,
                                        accent = if (first) SonHarfTheme.ActionOrange else SonHarfTheme.Primary,
                                        visible = true,
                                    )
                                } else {
                                    Surface(Modifier.size(if (first) 50.dp else 42.dp), shape = CircleShape, color = SonHarfTheme.SurfaceSecondary) {}
                                }
                                Spacer(Modifier.height(5.dp))
                                Text(player?.username?.ifBlank { sh("Oyuncu", "Player") } ?: "—", color = SonHarfTheme.TextPrimary, fontSize = if (first) 10.sp else 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(player?.let { "${it.rp} RP" } ?: "— RP", color = SonHarfTheme.TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black)
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
        Surface(onClick = onShop, shape = HomeCardShape, color = SonHarfTheme.Surface, border = BorderStroke(1.dp, SonHarfTheme.Border)) {
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
        Surface(onClick = onShop, shape = HomeCardShape, color = SonHarfTheme.Surface, border = BorderStroke(1.dp, SonHarfTheme.Border)) {
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
