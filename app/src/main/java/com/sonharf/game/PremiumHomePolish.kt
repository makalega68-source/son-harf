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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*

private val PolishedHomeCardShape = RoundedCornerShape(20.dp)
private val PolishedHeroShape = RoundedCornerShape(24.dp)

@Composable
internal fun PremiumHomeCommandDeckPolished(
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
            shape = PolishedHomeCardShape,
            color = SonHarfTheme.Surface,
            border = BorderStroke(1.dp, SonHarfTheme.Border),
            shadowElevation = 3.dp,
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FramedProfilePhotoAvatar(
                        avatarPath = profile?.avatarPath,
                        gender = profile?.gender,
                        name = profile?.displayName ?: sh("Oyuncu", "Player"),
                        size = 56.dp,
                        frameId = SonHarfCosmetics.profileFrameId,
                        accent = if (profile?.isVip == true) SonHarfTheme.Purple else SonHarfTheme.Primary,
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
                                Surface(shape = RoundedCornerShape(8.dp), color = SonHarfTheme.Purple.copy(alpha = .12f)) {
                                    Text(
                                        "PRO",
                                        Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        color = SonHarfTheme.Purple,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                    )
                                }
                            }
                        }
                        Text(
                            profile?.let { polishedLeagueName(ratingLeagueProgress(it.rating).leagueName) }
                                ?: sh("Lig bilgisi", "League status"),
                            color = SonHarfTheme.TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Surface(
                        onClick = onSocial,
                        shape = CircleShape,
                        color = SonHarfTheme.PrimarySoft,
                    ) {
                        Icon(
                            Icons.Rounded.Notifications,
                            sh("Bildirimler ve davetler", "Notifications and invites"),
                            tint = SonHarfTheme.Primary,
                            modifier = Modifier.padding(10.dp).size(19.dp),
                        )
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    PolishedHomeStatChip(
                        icon = Icons.Rounded.MilitaryTech,
                        value = profile?.let { "${it.rating} RP" } ?: "— RP",
                        label = sh("Derece", "Rating"),
                        accent = SonHarfTheme.Purple,
                        modifier = Modifier.weight(1f),
                    )
                    PolishedHomeStatChip(
                        icon = Icons.Rounded.Toll,
                        value = "${profile?.diamonds?.toString() ?: "—"} Coin",
                        label = sh("Bakiye", "Balance"),
                        accent = SonHarfTheme.ActionOrange,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Surface(
            shape = PolishedHeroShape,
            color = Color.Transparent,
            border = BorderStroke(1.dp, Color.White.copy(alpha = .16f)),
            shadowElevation = 5.dp,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(SonHarfTheme.HeroStart, SonHarfTheme.HeroMiddle, SonHarfTheme.HeroEnd),
                        ),
                        PolishedHeroShape,
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
                        Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(18.dp))
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
                            color = Color.White.copy(alpha = .13f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = .20f)),
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
                        colors = ButtonDefaults.buttonColors(containerColor = SonHarfTheme.Primary, contentColor = SonHarfTheme.OnPrimary),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(sh("HEMEN OYNA", "PLAY NOW"), fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = .55.sp)
                    }
                }
            }
        }

        PolishedDailyTasksStrip(dailyDashboard, dailyPlayStreak, dailyLoading)
    }
}

@Composable
private fun PolishedHomeStatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(13.dp),
        color = accent.copy(alpha = .08f),
        border = BorderStroke(1.dp, accent.copy(alpha = .16f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Surface(shape = CircleShape, color = accent.copy(alpha = .12f)) {
                Icon(icon, null, tint = accent, modifier = Modifier.padding(6.dp).size(15.dp))
            }
            Spacer(Modifier.width(7.dp))
            Column {
                Text(value, color = SonHarfTheme.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Text(label, color = SonHarfTheme.TextSecondary, fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun PolishedDailyTasksStrip(dashboard: GrowthDashboardDto?, streakDays: Int, loading: Boolean) {
    val matches = dashboard?.matchesToday?.coerceIn(0, 3) ?: 0
    val checkInDone = dashboard?.dailyClaimed == true
    val challengeDone = dashboard?.dailyChallengeClaimed == true || matches >= 3
    val completedTasks = (if (checkInDone) 1 else 0) + (if (challengeDone) 1 else 0)
    val progress = if (dashboard == null) 0f else (((if (checkInDone) 1f else 0f) + matches / 3f) / 2f).coerceIn(0f, 1f)
    val percent = (progress * 100).toInt()

    Surface(
        shape = PolishedHomeCardShape,
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, SonHarfTheme.Border),
        shadowElevation = 2.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = SonHarfTheme.Turquoise.copy(alpha = .11f)) {
                Icon(Icons.Rounded.CheckCircle, null, tint = SonHarfTheme.Turquoise, modifier = Modifier.padding(9.dp).size(21.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(sh("GÜNLÜK GÖREVLER", "DAILY TASKS"), color = SonHarfTheme.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text(if (loading) "…" else "$percent%", color = SonHarfTheme.Turquoise, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = SonHarfTheme.Turquoise,
                    trackColor = SonHarfTheme.SurfaceElevated,
                )
                Text(
                    if (loading) sh("Görevler yükleniyor", "Loading tasks") else sh("$completedTasks / 2 görev tamamlandı", "$completedTasks / 2 tasks completed"),
                    color = SonHarfTheme.TextSecondary,
                    fontSize = 10.sp,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.LocalFireDepartment, null, tint = SonHarfTheme.ActionOrange, modifier = Modifier.size(21.dp))
                Text(sh("$streakDays gün", "$streakDays days"), color = SonHarfTheme.TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
internal fun PremiumWeeklyBestPolished(onClick: () -> Unit) {
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
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, SonHarfTheme.Purple.copy(alpha = .18f)),
        shadowElevation = 4.dp,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            SonHarfTheme.Surface,
                            SonHarfTheme.Purple.copy(alpha = .06f),
                            SonHarfTheme.PrimarySoft.copy(alpha = .62f),
                        )
                    )
                )
                .padding(15.dp)
        ) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PackageLeagueBadge(Modifier.size(36.dp), tint = SonHarfTheme.Lavender)
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            sh("HAFTANIN EN İYİLERİ", "WEEKLY BEST"),
                            color = SonHarfTheme.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(sh("Haftalık rekabet sıralaması", "Weekly competition ranking"), color = SonHarfTheme.TextSecondary, fontSize = 8.sp)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.Purple, modifier = Modifier.size(21.dp))
                }

                when {
                    loading -> Box(Modifier.fillMaxWidth().height(110.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(24.dp), color = SonHarfTheme.Primary, strokeWidth = 2.dp)
                    }
                    failed -> Row(Modifier.fillMaxWidth().heightIn(min = 82.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(sh("Haftalık sıralama yenilenemedi.", "Weekly ranking could not refresh."), Modifier.weight(1f), color = SonHarfTheme.TextSecondary, fontSize = 10.sp)
                        TextButton(onClick = { reloadKey += 1 }) { Text(sh("YENİLE", "RETRY"), color = SonHarfTheme.Primary, fontSize = 10.sp) }
                    }
                    else -> Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        listOf(2 to players.getOrNull(1), 1 to players.getOrNull(0), 3 to players.getOrNull(2)).forEach { (place, player) ->
                            val first = place == 1
                            val accent = when (place) {
                                1 -> SonHarfTheme.Purple
                                2 -> SonHarfTheme.Turquoise
                                else -> SonHarfTheme.ActionOrange
                            }
                            Surface(
                                modifier = Modifier.weight(1f).then(if (first) Modifier.padding(bottom = 4.dp) else Modifier),
                                shape = RoundedCornerShape(16.dp),
                                color = if (first) SonHarfTheme.Purple.copy(alpha = .10f) else SonHarfTheme.Surface.copy(alpha = .94f),
                                border = BorderStroke(1.dp, accent.copy(alpha = .15f)),
                            ) {
                                Column(
                                    Modifier.padding(horizontal = 6.dp, vertical = if (first) 13.dp else 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text("$place.", color = accent, fontSize = if (first) 15.sp else 11.sp, fontWeight = FontWeight.Black)
                                    Spacer(Modifier.height(5.dp))
                                    if (player != null) {
                                        ProfilePhotoAvatarWithGender(
                                            avatarPath = player.avatarUrl,
                                            gender = null,
                                            name = player.username,
                                            size = if (first) 64.dp else 54.dp,
                                            accent = accent,
                                            visible = true,
                                        )
                                    } else {
                                        Surface(
                                            Modifier.size(if (first) 64.dp else 54.dp),
                                            shape = CircleShape,
                                            color = SonHarfTheme.SurfaceSecondary,
                                        ) {}
                                    }
                                    Spacer(Modifier.height(7.dp))
                                    Text(
                                        player?.username?.ifBlank { sh("Oyuncu", "Player") } ?: "—",
                                        color = SonHarfTheme.TextPrimary,
                                        fontSize = if (first) 10.sp else 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        player?.let { "${it.rp} RP" } ?: "— RP",
                                        color = accent,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun polishedLeagueName(value: String): String = when (value) {
    "BRONZ" -> sh("Bronz", "Bronze")
    "GÜMÜŞ" -> sh("Gümüş", "Silver")
    "ALTIN" -> sh("Altın", "Gold")
    "PLATİN" -> sh("Platin", "Platinum")
    "ELMAS" -> sh("Elmas", "Diamond")
    "EFSANE" -> sh("Efsane", "Legend")
    else -> value
}
