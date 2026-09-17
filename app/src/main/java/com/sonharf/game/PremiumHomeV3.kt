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

@OptIn(ExperimentalLayoutApi::class)
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

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            onClick = onProfile,
            shape = RoundedCornerShape(24.dp),
            color = SonHarfTheme.Surface.copy(alpha = .96f),
            border = BorderStroke(1.dp, SonHarfTheme.Border.copy(alpha = .7f)),
            shadowElevation = 2.dp,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
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
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            profile?.displayName ?: sh("Profilin", "Your profile"),
                            modifier = Modifier.weight(1f, fill = false),
                            color = SonHarfCosmetics.playerNameColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (profile?.isVip == true) {
                            Spacer(Modifier.width(7.dp))
                            Surface(shape = RoundedCornerShape(8.dp), color = SonHarfTheme.PremiumGoldLight.copy(alpha = .22f)) {
                                Text(
                                    "PRO",
                                    Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    color = SonHarfTheme.ForestDeep,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = .7.sp,
                                )
                            }
                        }
                    }
                    Text(
                        profile?.let { homeLeagueName(ratingLeagueProgress(it.rating).leagueName) } ?: sh("Lig bilgisi", "League status"),
                        color = SonHarfTheme.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HomeStatChip(
                        icon = Icons.Rounded.MilitaryTech,
                        text = profile?.let { "${it.rating} RP" } ?: "— RP",
                        background = SonHarfTheme.Lavender.copy(alpha = .18f),
                    )
                    HomeStatChip(
                        icon = Icons.Rounded.Toll,
                        text = "${profile?.diamonds?.toString() ?: "—"} Coin",
                        background = SonHarfTheme.PrimarySoft.copy(alpha = .65f),
                    )
                }
                Spacer(Modifier.width(2.dp))
                IconButton(onClick = onSocial, modifier = Modifier.size(38.dp)) {
                    Icon(
                        Icons.Rounded.Notifications,
                        sh("Bildirimler ve davetler", "Notifications and invites"),
                        tint = SonHarfTheme.Primary,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = SonHarfTheme.ForestDeep,
            border = BorderStroke(1.dp, Color.White.copy(alpha = .08f)),
            shadowElevation = 5.dp,
        ) {
            Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Shield, null, tint = SonHarfTheme.PremiumGoldLight, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                sh("KELİMELERLE ALAN SAVAŞI", "A BATTLE OF WORDS & TERRITORY"),
                                color = Color.White.copy(alpha = .8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Text(
                            "KELİME KUŞATMASI",
                            color = Color.White,
                            fontSize = 27.sp,
                            lineHeight = 32.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-.5).sp,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Image(
                        painter = painterResource(R.drawable.kelime_kusatma_logo_hd),
                        contentDescription = sh("Kelime Kuşatması", "Kelime Kuşatması"),
                        modifier = Modifier.size(width = 158.dp, height = 104.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                Text(
                    sh(
                        "Kelimeyi kur. Alanı ele geçir. Rakibini geç.",
                        "Build your word. Claim territory. Outplay your rival.",
                    ),
                    color = Color.White.copy(alpha = .85f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                Button(
                    onClick = onSiege,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SonHarfTheme.PremiumGoldLight,
                        contentColor = SonHarfTheme.ForestDeep,
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(27.dp))
                        Spacer(Modifier.width(9.dp))
                        Text(
                            sh("HEMEN OYNA", "PLAY NOW"),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = .7.sp,
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
) {
    Surface(shape = RoundedCornerShape(10.dp), color = background) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = SonHarfTheme.Primary, modifier = Modifier.size(14.dp))
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
        shape = RoundedCornerShape(20.dp),
        color = SonHarfTheme.Surface.copy(alpha = .96f),
        border = BorderStroke(1.dp, SonHarfTheme.Border.copy(alpha = .7f)),
        shadowElevation = 2.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = SonHarfTheme.PrimarySoft.copy(alpha = .8f)) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    null,
                    tint = SonHarfTheme.Primary,
                    modifier = Modifier.padding(10.dp).size(22.dp),
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
                        letterSpacing = .5.sp,
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
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = SonHarfTheme.Primary,
                    trackColor = SonHarfTheme.PrimarySoft,
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
                    tint = Color(0xFFD98A4B),
                    modifier = Modifier.size(22.dp),
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
    players: List<HomePodiumEntry>, loading: Boolean, failed: Boolean,
    onOpenLeague: () -> Unit, onRetry: () -> Unit,
) {
    Surface(onClick = onOpenLeague, shape = RoundedCornerShape(24.dp), color = Color.Transparent, shadowElevation = 3.dp) {
        Box(Modifier.fillMaxWidth().height(164.dp)) {
            Image(
                painter = painterResource(R.drawable.weekly_elite_gold_panel),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.FillBounds,
            )
            Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(42.dp))
                    Text(sh("HAFTANIN ZİRVESİ", "WEEKLY ELITE"), Modifier.weight(1f), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp, textAlign = TextAlign.Center)
                    Row(Modifier.width(42.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(sh("LİG", "LEAGUE"), color = SonHarfTheme.PremiumGoldLight, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        Icon(Icons.Rounded.ChevronRight, null, Modifier.size(14.dp), tint = SonHarfTheme.PremiumGoldLight)
                    }
                }
                when {
                    loading -> Box(Modifier.fillMaxWidth().height(58.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = SonHarfTheme.PremiumGoldLight, strokeWidth = 2.dp)
                    }
                    failed -> Row(Modifier.fillMaxWidth().height(58.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(sh("Sıralama yenilenemedi", "Ranking could not refresh"), Modifier.weight(1f), color = Color.White.copy(alpha = .78f), fontSize = 11.sp)
                        TextButton(onClick = onRetry, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) { Text(sh("YENİLE", "RETRY"), color = SonHarfTheme.PremiumGoldLight, fontSize = 10.sp) }
                    }
                    else -> Row(Modifier.fillMaxWidth().weight(1f).semantics { isTraversalGroup = true }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PremiumPodiumPlace(1, players.getOrNull(0), Modifier.weight(1f))
                        PremiumPodiumPlace(2, players.getOrNull(1), Modifier.weight(1f))
                        PremiumPodiumPlace(3, players.getOrNull(2), Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumPodiumPlace(place: Int, player: HomePodiumEntry?, modifier: Modifier) {
    val frame = when (place) {
        1 -> listOf(Color(0xFFFFE7A1), Color(0xFFD2A23D), Color(0xFF7A5313))
        2 -> listOf(Color(0xFFF5FAFF), Color(0xFFB9C6D0), Color(0xFF62707B))
        else -> listOf(Color(0xFFE9F5FF), Color(0xFF8AB4C7), Color(0xFF345B70))
    }
    val accent = frame[1]
    val name = player?.row?.displayName?.ifBlank { sh("Oyuncu", "Player") } ?: "—"
    val spoken = player?.let { sh("$place. sıra, $name, ${it.row.rating} haftalık RP",
        "Rank $place, $name, ${it.row.rating} weekly RP") } ?: sh("$place. sıra henüz boş", "Rank $place is not filled yet")
    Column(modifier.clearAndSetSemantics { contentDescription = spoken; traversalIndex = place.toFloat() },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("$place.", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Black)
        Box(Modifier.size(if (place == 1) 54.dp else 46.dp).background(Brush.linearGradient(frame), CircleShape).padding(2.dp), contentAlignment = Alignment.Center) {
            if (player != null) {
                ProfilePhotoAvatarWithGender(
                    avatarPath = if (player.profile?.avatarVisibility == "hidden") null else player.profile?.avatarPath,
                    gender = player.profile?.gender, name = name, size = if (place == 1) 50.dp else 42.dp, accent = accent,
                    visible = player.profile?.avatarVisibility != "hidden",
                )
            } else Surface(Modifier.fillMaxSize(), shape = CircleShape, color = Color(0xFF31453B)) {
                Box(contentAlignment = Alignment.Center) { Text("$place", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black) }
            }
        }
        Spacer(Modifier.height(3.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Text(player?.let { "${it.row.rating} RP" } ?: "— RP", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

@Composable
internal fun PremiumLeagueProgress(profile: ProfileDto?, onLeague: () -> Unit) {
    val progress = profile?.let { ratingLeagueProgress(it.rating) }
    Surface(onClick = onLeague, shape = RoundedCornerShape(22.dp), color = SonHarfTheme.SurfaceSecondary,
        border = BorderStroke(1.dp, SonHarfTheme.Border.copy(alpha = .65f))) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.EmojiEvents, null, tint = SonHarfTheme.Primary, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(progress?.let { homeLeagueName(it.leagueName) } ?: sh("Lig durumun", "Your league"),
                        color = SonHarfTheme.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(profile?.let { "${it.rating} RP" } ?: "— RP", color = SonHarfTheme.TextSecondary, fontSize = 13.sp)
                }
                Icon(Icons.Rounded.ChevronRight, sh("Lige Git", "Go to League"), tint = SonHarfTheme.Primary)
            }
            if (progress != null) {
                val detail = if (progress.nextAt == null) sh("En üst ligdesin. Yerini koru.", "You're in the top league. Hold your place.")
                else sh("${homeLeagueName(progress.nextLeagueName)} ligine ${progress.pointsToNext} RP kaldı",
                    "${progress.pointsToNext} RP to ${homeLeagueName(progress.nextLeagueName)}")
                LinearProgressIndicator(progress = { progress.progress }, modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = SonHarfTheme.Primary, trackColor = SonHarfTheme.Border)
                Text(detail, color = SonHarfTheme.TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
            } else Text(sh("Lig bilgini sıralama ekranında kontrol et.", "Check your league on the standings screen."),
                color = SonHarfTheme.TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
internal fun PremiumOtherGames(onLastLetter: () -> Unit, onLetterPath: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            sh("DİĞER OYUNLAR", "MORE GAMES"),
            color = SonHarfTheme.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = .8.sp,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PremiumHomeModeCard(
                modifier = Modifier.weight(1f),
                logo = R.drawable.son_harf_app_icon_master,
                title = sh("SON HARF", "LAST LETTER"),
                subtitle = sh("Hızlı kelime düellosu", "A quick word duel"),
                accent = Color(0xFFA9C9E3),
                accentDark = Color(0xFF6F93B2),
                onPlay = onLastLetter,
            )
            PremiumHomeModeCard(
                modifier = Modifier.weight(1f),
                logo = R.drawable.harf_yolu_logo,
                title = sh("KELİME YOLU", "WORD PATH"),
                subtitle = sh("Kelime rotanı tamamla", "Complete your word path"),
                accent = Color(0xFFC8BDE2),
                accentDark = Color(0xFF8E80B0),
                onPlay = onLetterPath,
            )
        }
    }
}

@Composable
private fun PremiumHomeModeCard(
    modifier: Modifier = Modifier,
    logo: Int,
    title: String,
    subtitle: String,
    accent: Color,
    accentDark: Color,
    onPlay: () -> Unit,
) {
    Surface(
        onClick = onPlay,
        modifier = modifier.height(170.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        shadowElevation = 3.dp,
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(accentDark, accent)),
                RoundedCornerShape(22.dp),
            ),
        ) {
            Column(
                Modifier.fillMaxSize().padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = .2f)) {
                    Image(
                        painter = painterResource(logo),
                        contentDescription = null,
                        modifier = Modifier.padding(7.dp).size(46.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = .2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        subtitle,
                        color = Color.White.copy(alpha = .86f),
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(sh("OYNA", "PLAY"), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(3.dp))
                        Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
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

    PremiumWeeklyTopThree(
        players = players,
        loading = loading,
        failed = failed,
        onClick = onClick,
        onRetry = { reloadKey += 1 },
    )
}

@Composable
private fun PremiumWeeklyTopThree(
    players: List<WeeklyTopPlayerV210>,
    loading: Boolean,
    failed: Boolean,
    onClick: () -> Unit,
    onRetry: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = SonHarfTheme.Surface.copy(alpha = .97f),
        border = BorderStroke(1.dp, SonHarfTheme.Border.copy(alpha = .75f)),
        shadowElevation = 3.dp,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.EmojiEvents, null, tint = SonHarfTheme.PremiumGoldLight, modifier = Modifier.size(23.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    sh("HAFTANIN İLK 3 OYUNCUSU", "WEEKLY TOP 3"),
                    modifier = Modifier.weight(1f),
                    color = SonHarfTheme.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = .45.sp,
                )
                Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.TextSecondary, modifier = Modifier.size(20.dp))
            }
            when {
                loading -> Box(Modifier.fillMaxWidth().height(92.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = SonHarfTheme.Primary, strokeWidth = 2.dp)
                }
                failed -> Row(Modifier.fillMaxWidth().heightIn(min = 70.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        sh("Haftalık sıralama yenilenemedi.", "Weekly ranking could not refresh."),
                        modifier = Modifier.weight(1f),
                        color = SonHarfTheme.TextSecondary,
                        fontSize = 11.sp,
                    )
                    TextButton(onClick = onRetry) {
                        Text(sh("YENİLE", "RETRY"), color = SonHarfTheme.Primary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
                else -> Row(
                    Modifier.fillMaxWidth().semantics { isTraversalGroup = true },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    WeeklyTopPlayerPlace(2, players.getOrNull(1), Modifier.weight(1f))
                    WeeklyTopPlayerPlace(1, players.getOrNull(0), Modifier.weight(1f))
                    WeeklyTopPlayerPlace(3, players.getOrNull(2), Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun WeeklyTopPlayerPlace(
    place: Int,
    player: WeeklyTopPlayerV210?,
    modifier: Modifier,
) {
    val first = place == 1
    val accent = when (place) {
        1 -> Color(0xFFD6A743)
        2 -> Color(0xFF8FA5B2)
        else -> Color(0xFF9B7659)
    }
    val name = player?.username?.ifBlank { sh("Oyuncu", "Player") } ?: "—"
    val spoken = if (player == null) {
        sh("$place. sıra henüz boş", "Rank $place is not filled yet")
    } else {
        sh("$place. sıra, $name, ${player.rp} puan", "Rank $place, $name, ${player.rp} points")
    }

    Surface(
        modifier = modifier
            .then(if (first) Modifier.padding(bottom = 6.dp) else Modifier)
            .clearAndSetSemantics {
                contentDescription = spoken
                traversalIndex = place.toFloat()
            },
        shape = RoundedCornerShape(16.dp),
        color = if (first) SonHarfTheme.PremiumGoldLight.copy(alpha = .16f) else SonHarfTheme.PrimarySoft.copy(alpha = .45f),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = if (first) 11.dp else 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("$place.", color = accent, fontSize = if (first) 14.sp else 11.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .size(if (first) 56.dp else 46.dp)
                    .background(accent, CircleShape)
                    .padding(if (first) 3.dp else 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (player != null) {
                    ProfilePhotoAvatarWithGender(
                        avatarPath = player.avatarUrl,
                        gender = null,
                        name = name,
                        size = if (first) 50.dp else 42.dp,
                        accent = accent,
                        visible = true,
                    )
                } else {
                    Surface(Modifier.fillMaxSize(), shape = CircleShape, color = SonHarfTheme.SurfaceSecondary) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("$place", color = SonHarfTheme.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
            Spacer(Modifier.height(5.dp))
            Text(
                name,
                color = SonHarfTheme.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = if (first) 10.sp else 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                player?.let { "${it.rp} RP" } ?: "— RP",
                color = accent,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun PremiumHomeExtras(profile: ProfileDto?, onShop: () -> Unit, onSocial: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(onClick = onShop, shape = RoundedCornerShape(20.dp), color = SonHarfTheme.PrimarySoft.copy(alpha = .65f)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CardGiftcard, null, tint = SonHarfTheme.Primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(sh("Günlük ücretsiz hediye", "Daily free gift"), fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold, color = SonHarfTheme.TextPrimary)
                    Text(sh("Bugünkü ödülünü mağazada kontrol et.", "Check today's reward in the shop."),
                        fontSize = 13.sp, color = SonHarfTheme.TextSecondary)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.Primary)
            }
        }
        Surface(onClick = onShop, shape = RoundedCornerShape(20.dp), color = SonHarfTheme.Surface,
            border = BorderStroke(1.dp, SonHarfTheme.Border.copy(alpha = .6f))) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WorkspacePremium, null, tint = SonHarfTheme.Warning)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (profile?.isVip == true) sh("PRO koleksiyonun", "Your PRO collection") else sh("PRO'yu keşfet", "Explore PRO"),
                        color = SonHarfTheme.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(sh("Reklamsız kullanım · Kozmetik · Konfor", "Ad-free · Cosmetics · Comfort"),
                        color = SonHarfTheme.TextSecondary, fontSize = 13.sp)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.Primary)
            }
        }
        TextButton(onClick = onSocial, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Icon(Icons.Rounded.Groups, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(sh("Arkadaşların ve rakiplerin", "Friends and rivals"), fontSize = 14.sp)
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
