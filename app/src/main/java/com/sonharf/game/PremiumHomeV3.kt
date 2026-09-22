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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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

private val HomeHeroShape = RoundedCornerShape(18.dp)
private val HomeCardShape = RoundedCornerShape(16.dp)
private val HomeControlShape = RoundedCornerShape(12.dp)
private val HomeMicroShape = RoundedCornerShape(10.dp)
private val HomeDarkText = Color(0xFF0B1B33)
private val HomeHairline = Color(0xFFD8E4EC)

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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeBrandHeader(onSocial = onSocial)
        HomePlayerCommandStrip(profile = profile, onProfile = onProfile)
        HomeSiegeHero(onSiege = onSiege)
        PremiumDailyTasksStrip(
            dashboard = dailyDashboard,
            streakDays = dailyPlayStreak,
            loading = dailyLoading,
        )
    }
}

@Composable
private fun HomeBrandHeader(onSocial: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "KELİME TAHTI",
                color = SonHarfTheme.TextPrimary,
                fontSize = 21.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.1.sp,
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .size(width = 42.dp, height = 3.dp)
                    .background(SonHarfTheme.ActionOrange, RoundedCornerShape(99.dp)),
            )
        }
        Surface(
            onClick = onSocial,
            modifier = Modifier.size(42.dp),
            shape = HomeControlShape,
            color = SonHarfTheme.Surface,
            border = BorderStroke(1.dp, SonHarfTheme.Border),
            shadowElevation = 1.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Notifications,
                    sh("Bildirimler ve davetler", "Notifications and invites"),
                    tint = SonHarfTheme.TextPrimary,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
    }
}

@Composable
private fun HomePlayerCommandStrip(profile: ProfileDto?, onProfile: () -> Unit) {
    Surface(
        onClick = onProfile,
        shape = HomeCardShape,
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, SonHarfTheme.Border),
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FramedProfilePhotoAvatar(
                    avatarPath = profile?.avatarPath,
                    gender = profile?.gender,
                    name = profile?.displayName ?: sh("Oyuncu", "Player"),
                    size = 44.dp,
                    frameId = SonHarfCosmetics.profileFrameId,
                    accent = if (profile?.isVip == true) SonHarfTheme.Lavender else SonHarfTheme.Primary,
                    visible = profile?.avatarVisibility != "hidden",
                    isPro = profile?.isVip == true,
                )
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            profile?.displayName ?: sh("Profilin", "Your profile"),
                            modifier = Modifier.weight(1f, fill = false),
                            color = SonHarfTheme.TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (profile?.isVip == true) {
                            Spacer(Modifier.width(7.dp))
                            Surface(shape = HomeMicroShape, color = SonHarfTheme.SurfaceElevated) {
                                Text(
                                    "PRO",
                                    Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    color = SonHarfTheme.Lavender,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = .5.sp,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        sh("Oyuncu profili ve ilerleme", "Player profile and progress"),
                        color = SonHarfTheme.TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                }
                Icon(
                    Icons.Rounded.ChevronRight,
                    null,
                    tint = SonHarfTheme.TextSecondary,
                    modifier = Modifier.size(21.dp),
                )
            }

            HorizontalDivider(color = HomeHairline)

            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HomeCounter(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.style_icon_trophy,
                    fallbackIcon = Icons.Rounded.MilitaryTech,
                    value = profile?.let { "${it.rating} RP" } ?: "— RP",
                    label = sh("PUAN", "RATING"),
                    accent = SonHarfTheme.ActionOrange,
                )
                HomeMetricDivider()
                HomeCounter(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.style_icon_trophy,
                    fallbackIcon = Icons.Rounded.EmojiEvents,
                    value = profile?.let { homeLeagueName(ratingLeagueProgress(it.rating).leagueName) } ?: "—",
                    label = sh("LİG", "LEAGUE"),
                    accent = SonHarfTheme.Lavender,
                )
                HomeMetricDivider()
                HomeCounter(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.style_icon_coin,
                    fallbackIcon = Icons.Rounded.Toll,
                    value = "${profile?.diamonds?.toString() ?: "—"} Coin",
                    label = "COIN",
                    accent = SonHarfTheme.Primary,
                )
            }
        }
    }
}

@Composable
private fun HomeMetricDivider() {
    Box(
        Modifier
            .fillMaxHeight()
            .width(1.dp)
            .padding(vertical = 2.dp)
            .background(HomeHairline),
    )
}

@Composable
private fun HomeCounter(
    modifier: Modifier,
    iconRes: Int,
    fallbackIcon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    accent: Color,
) {
    Row(
        modifier.padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(30.dp),
            shape = HomeMicroShape,
            color = accent.copy(alpha = .10f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                val painter = runCatching { painterResource(iconRes) }.getOrNull()
                if (painter != null) {
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Icon(fallbackIcon, null, tint = accent, modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)) {
            Text(
                value,
                color = HomeDarkText,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                label,
                color = SonHarfTheme.TextSecondary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = .5.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun HomeSiegeHero(onSiege: () -> Unit) {
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
                )
                .drawBehind {
                    val gridColor = Color.White.copy(alpha = .055f)
                    val step = size.minDimension / 7f
                    var x = size.width * .50f
                    while (x < size.width) {
                        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                        x += step
                    }
                    var y = 0f
                    while (y < size.height) {
                        drawLine(gridColor, Offset(size.width * .50f, y), Offset(size.width, y), strokeWidth = 1f)
                        y += step
                    }
                },
        ) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Surface(shape = HomeMicroShape, color = SonHarfTheme.Turquoise) {
                            Text(
                                sh("ANA OYUN", "MAIN GAME"),
                                Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                color = HomeDarkText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = .8.sp,
                            )
                        }
                        Text(
                            "KELİME KUŞATMASI",
                            color = Color.White,
                            fontSize = 24.sp,
                            lineHeight = 27.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Start,
                            maxLines = 2,
                        )
                        Text(
                            sh(
                                "Kelimeyi kur. Alanı ele geçir. Rakibini geç.",
                                "Build your word. Claim territory. Outplay your rival.",
                            ),
                            color = Color.White.copy(alpha = .88f),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Start,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Image(
                        painter = painterResource(R.drawable.word_siege_home_badge),
                        contentDescription = sh("Kelime Kuşatması", "Kelime Kuşatması"),
                        modifier = Modifier.size(width = 112.dp, height = 98.dp),
                        contentScale = ContentScale.Fit,
                    )
                }

                Button(
                    onClick = onSiege,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = HomeControlShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SonHarfTheme.ActionOrange,
                        contentColor = HomeDarkText,
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp, pressedElevation = 0.dp),
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        sh("HEMEN OYNA", "PLAY NOW"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = .7.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Rounded.ArrowForward, null, modifier = Modifier.size(19.dp))
                }
            }
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
            Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = HomeMicroShape, color = SonHarfTheme.Primary.copy(alpha = .10f), modifier = Modifier.size(34.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        null,
                        tint = SonHarfTheme.Primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        sh("GÜNLÜK GÖREVLER", "DAILY TASKS"),
                        modifier = Modifier.weight(1f),
                        color = SonHarfTheme.TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = .4.sp,
                        maxLines = 1,
                    )
                    Text(
                        if (loading) "…" else "$completedTasks / 2",
                        color = SonHarfTheme.TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = SonHarfTheme.Primary,
                    trackColor = SonHarfTheme.SurfaceSecondary,
                )
            }
            Spacer(Modifier.width(11.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.LocalFireDepartment,
                    null,
                    tint = SonHarfTheme.ActionOrange,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    sh("$streakDays gün", "$streakDays days"),
                    color = HomeDarkText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
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
                else -> Column(Modifier.fillMaxWidth().semantics { isTraversalGroup = true }) {
                    players.take(3).forEachIndexed { index, item ->
                        if (index > 0) HorizontalDivider(color = HomeHairline)
                        Row(
                            Modifier.fillMaxWidth().clearAndSetSemantics {
                                contentDescription = sh("${index + 1}. sıra, ${item.row.displayName}, ${item.row.rating} RP", "Rank ${index + 1}, ${item.row.displayName}, ${item.row.rating} RP")
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
                                modifier = Modifier.weight(1f),
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
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                sh("DİĞER OYUNLAR", "MORE GAMES"),
                modifier = Modifier.weight(1f),
                color = SonHarfTheme.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = .7.sp,
            )
            Text(
                sh("HIZLI MODLAR", "QUICK MODES"),
                color = SonHarfTheme.TextSecondary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = .6.sp,
            )
        }
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PremiumHomeModeCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                logo = R.drawable.son_harf_app_icon_master,
                title = sh("SON HARF", "LAST LETTER"),
                subtitle = sh("Hızlı kelime düellosu", "A quick word duel"),
                accent = SonHarfTheme.Turquoise,
                onPlay = onLastLetter,
            )
            PremiumHomeModeCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
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
        modifier = modifier.heightIn(min = 168.dp),
        shape = HomeCardShape,
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, SonHarfTheme.Border),
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().height(4.dp).background(accent))
            Column(
                Modifier.fillMaxSize().padding(12.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Surface(shape = HomeMicroShape, color = accent.copy(alpha = .10f), modifier = Modifier.size(46.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(logo),
                            contentDescription = null,
                            modifier = Modifier.size(34.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
                Text(
                    title,
                    color = HomeDarkText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    modifier = Modifier.weight(1f),
                    color = SonHarfTheme.TextSecondary,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    textAlign = TextAlign.Start,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Button(
                    onClick = onPlay,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = HomeControlShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent.copy(alpha = .14f),
                        contentColor = HomeDarkText,
                    ),
                    contentPadding = PaddingValues(horizontal = 9.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                ) {
                    Text(sh("OYNA", "PLAY"), fontSize = 11.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Rounded.ArrowForward, null, modifier = Modifier.size(16.dp), tint = accent)
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
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                loading -> Box(Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = SonHarfTheme.Primary, strokeWidth = 2.dp)
                }
                failed -> Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(sh("Haftalık sıralama yenilenemedi.", "Weekly ranking could not refresh."), Modifier.weight(1f), color = SonHarfTheme.TextSecondary, fontSize = 10.sp)
                    TextButton(onClick = { reloadKey += 1 }) {
                        Text(sh("YENİLE", "RETRY"), color = SonHarfTheme.TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
                else -> Column(Modifier.fillMaxWidth()) {
                    listOf(1 to players.getOrNull(0), 2 to players.getOrNull(1), 3 to players.getOrNull(2)).forEachIndexed { index, (place, player) ->
                        if (index > 0) HorizontalDivider(color = HomeHairline)
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
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
