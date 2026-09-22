package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
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
import com.sonharf.game.data.*

private data class HomeRankItem(
    val name: String,
    val rating: Int,
    val avatarPath: String?,
)

@Composable
internal fun ProfessionalHomeScreen(
    backend: OnlineGameBackend,
    onSiege: () -> Unit,
    onLastLetter: () -> Unit,
    onLetterPath: () -> Unit,
    onProfile: () -> Unit,
    onSocial: () -> Unit,
    onLeague: () -> Unit,
    onPro: () -> Unit,
    onCollection: () -> Unit,
    onRetention: () -> Unit,
) {
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var weekly by remember { mutableStateOf<List<HomeRankItem>>(emptyList()) }
    var weeklyLoading by remember { mutableStateOf(SupabaseProvider.configured) }
    var streakDays by remember { mutableIntStateOf(0) }
    var dailyProgress by remember { mutableFloatStateOf(0f) }
    var dailyLabel by remember { mutableStateOf(gameText("Görevler yükleniyor", "Loading missions")) }

    LaunchedEffect(Unit) {
        if (!SupabaseProvider.configured) {
            weeklyLoading = false
            dailyLabel = gameText("Günlük görevler çevrimdışı", "Daily missions offline")
            return@LaunchedEffect
        }

        profile = backend.currentUserId()?.let { id ->
            runCatching { backend.getProfile(id) }.getOrNull()
        }

        runCatching { backend.getMetaProgressV2() }
            .onSuccess { streakDays = it.dailyPlayStreak }

        runCatching { backend.getGrowthDashboard() }
            .onSuccess { dashboard ->
                val matches = dashboard.matchesToday.coerceIn(0, 3)
                val checkIn = if (dashboard.dailyClaimed) 1f else 0f
                val challenge = if (dashboard.dailyChallengeClaimed) 1f else matches / 3f
                dailyProgress = ((checkIn + challenge) / 2f).coerceIn(0f, 1f)
                val completed = (if (dashboard.dailyClaimed) 1 else 0) +
                    (if (dashboard.dailyChallengeClaimed || matches >= 3) 1 else 0)
                dailyLabel = gameText("$completed / 2 görev tamamlandı", "$completed / 2 missions complete")
            }
            .onFailure {
                dailyLabel = gameText("Görevler yenilenemedi", "Missions unavailable")
            }

        weekly = runCatching { backend.getWeeklyTopV210(limit = 3) }
            .getOrDefault(emptyList())
            .take(3)
            .map { row -> HomeRankItem(row.username, row.rp, row.avatarUrl) }
        weeklyLoading = false
    }

    Box(Modifier.fillMaxSize().background(GameColors.AppBackground), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 620.dp),
            contentPadding = PaddingValues(horizontal = GameSpacing.ScreenHorizontal, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "player_bar") {
                HomePlayerBar(profile = profile, onProfile = onProfile, onSocial = onSocial, onPro = onPro)
            }
            item(key = "siege_hero") {
                SiegeHero(onPlay = onSiege)
            }
            item(key = "secondary_modes") {
                SecondaryModes(onLastLetter = onLastLetter, onLetterPath = onLetterPath)
            }
            item(key = "daily") {
                DailyProgressStrip(
                    progress = dailyProgress,
                    label = dailyLabel,
                    streakDays = streakDays,
                    onClick = onRetention,
                )
            }
            item(key = "weekly") {
                WeeklyTopThree(players = weekly, loading = weeklyLoading, onLeague = onLeague)
            }
            item(key = "quick_access") {
                HomeQuickAccess(onLeague = onLeague, onPro = onPro, onCollection = onCollection)
            }
            item { Spacer(Modifier.height(4.dp)) }
        }
    }
}

@Composable
private fun HomePlayerBar(
    profile: ProfileDto?,
    onProfile: () -> Unit,
    onSocial: () -> Unit,
    onPro: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = GameShapes.Large,
        color = GameColors.ElevatedBackground,
        border = BorderStroke(1.dp, GameColors.Divider),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(onClick = onProfile, shape = CircleShape, color = Color.Transparent) {
                FramedProfilePhotoAvatar(
                    avatarPath = profile?.avatarPath,
                    gender = profile?.gender,
                    name = profile?.displayName ?: gameText("Oyuncu", "Player"),
                    size = 52.dp,
                    frameId = SonHarfCosmetics.profileFrameId,
                    accent = if (profile?.isVip == true) GameColors.PrestigeGold else GameColors.PrimaryBlue,
                    visible = profile?.avatarVisibility != "hidden",
                    isPro = profile?.isVip == true,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    profile?.displayName ?: gameText("Profil", "Profile"),
                    color = GameColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (profile != null) {
                        LeagueBadge(ratingLeagueProgress(profile.rating).leagueName)
                        Text("${profile.rating} RP", color = GameColors.TextSecondary, style = MaterialTheme.typography.labelSmall)
                    } else {
                        Text(gameText("Lig bilgisi yükleniyor", "Loading league"), color = GameColors.TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            if (profile != null) CurrencyChip(profile.diamonds)
            Spacer(Modifier.width(6.dp))
            Surface(
                onClick = onPro,
                shape = GameShapes.Pill,
                color = GameColors.Lavender.copy(alpha = .16f),
                border = BorderStroke(1.dp, GameColors.Lavender.copy(alpha = .5f)),
            ) {
                Text(
                    "PRO",
                    Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                    color = GameColors.TextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onSocial, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Rounded.NotificationsNone, gameText("Bildirimler", "Notifications"), tint = GameColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun SiegeHero(onPlay: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 190.dp),
        shape = GameShapes.Hero,
        color = Color.Transparent,
        border = BorderStroke(1.dp, GameColors.TacticalTurquoise.copy(alpha = .35f)),
    ) {
        Box(
            Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(GameColors.HeroStart, GameColors.HeroMiddle, GameColors.HeroEnd)),
                GameShapes.Hero,
            ),
        ) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 18.dp, end = 18.dp)
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(GameColors.TacticalTurquoise.copy(alpha = .10f)),
            )
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 24.dp, y = 20.dp)
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(GameColors.Lavender.copy(alpha = .08f)),
            )
            Column(
                Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = GameShapes.Pill, color = GameColors.TacticalTurquoise.copy(alpha = .17f)) {
                        Row(Modifier.padding(horizontal = 9.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Public, null, tint = GameColors.TacticalTurquoise, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(gameText("ANA OYUN", "MAIN GAME"), color = GameColors.TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(4) { index ->
                            Box(
                                Modifier.size(9.dp)
                                    .clip(GameShapes.Small)
                                    .background(if (index < 3) GameColors.TacticalTurquoise else GameColors.Lavender),
                            )
                        }
                    }
                }
                Text(
                    "KELİME KUŞATMASI",
                    color = GameColors.TextPrimary,
                    style = MaterialTheme.typography.displaySmall,
                    maxLines = 2,
                )
                Text(
                    gameText("Kelime oyunu + taktik alan savaşı", "Word game + tactical territory battle"),
                    color = GameColors.TextPrimary.copy(alpha = .82f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                GamePrimaryButton(
                    text = gameText("HEMEN OYNA", "PLAY NOW"),
                    onClick = onPlay,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.PlayArrow,
                )
            }
        }
    }
}

@Composable
private fun SecondaryModes(onLastLetter: () -> Unit, onLetterPath: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        GameSectionHeader(gameText("Diğer Oyunlar", "Other Games"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModeCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Bolt,
                title = gameText("SON HARF", "LAST LETTER"),
                subtitle = gameText("Hızlı rekabetçi düello", "Fast competitive duel"),
                accent = GameColors.Danger,
                onClick = onLastLetter,
            )
            ModeCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Route,
                title = gameText("HARF YOLU", "LETTER PATH"),
                subtitle = gameText("Zekâ ve kelime rotası", "Word logic puzzle"),
                accent = GameColors.Lavender,
                onClick = onLetterPath,
            )
        }
    }
}

@Composable
private fun ModeCard(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(124.dp),
        shape = GameShapes.Large,
        color = GameColors.PrimarySurface,
        border = BorderStroke(1.dp, GameColors.Border),
    ) {
        Column(Modifier.fillMaxSize().padding(13.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Surface(shape = GameShapes.Medium, color = accent.copy(alpha = .14f)) {
                Icon(icon, null, tint = accent, modifier = Modifier.padding(9.dp).size(22.dp))
            }
            Column {
                Text(title, color = GameColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(subtitle, color = GameColors.TextSecondary, fontSize = 10.sp, lineHeight = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun DailyProgressStrip(
    progress: Float,
    label: String,
    streakDays: Int,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = GameShapes.Large,
        color = GameColors.PrimarySurface,
        border = BorderStroke(1.dp, GameColors.Border),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = GameColors.PlayGreen.copy(alpha = .14f)) {
                Icon(Icons.Rounded.CheckCircle, null, tint = GameColors.PlayGreen, modifier = Modifier.padding(9.dp).size(21.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(gameText("Günlük İlerleme", "Daily Progress"), color = GameColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("${(progress * 100).toInt()}%", color = GameColors.PlayGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                MissionProgress(progress)
                Text(label, color = GameColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.LocalFireDepartment, null, tint = GameColors.RewardAmber, modifier = Modifier.size(22.dp))
                Text(gameText("$streakDays gün", "$streakDays days"), color = GameColors.TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Rounded.ChevronRight, null, tint = GameColors.TextTertiary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun WeeklyTopThree(players: List<HomeRankItem>, loading: Boolean, onLeague: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        GameSectionHeader(
            title = gameText("Haftanın En İyi 3 Oyuncusu", "Weekly Top 3"),
            actionLabel = gameText("Lig", "League"),
            onAction = onLeague,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = GameShapes.Large,
            color = GameColors.PrimarySurface,
            border = BorderStroke(1.dp, GameColors.Border),
        ) {
            when {
                loading -> Box(Modifier.fillMaxWidth().height(116.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = GameColors.PrimaryBlue, strokeWidth = 2.dp)
                }
                players.isEmpty() -> Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Rounded.EmojiEvents, null, tint = GameColors.TextSecondary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(7.dp))
                    Text(gameText("Haftalık sıralama henüz oluşmadı", "Weekly ranking is not available yet"), color = GameColors.TextSecondary, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                }
                else -> Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    val podium = listOf(2 to players.getOrNull(1), 1 to players.getOrNull(0), 3 to players.getOrNull(2))
                    podium.forEach { (place, player) ->
                        if (player != null) {
                            val first = place == 1
                            Surface(
                                modifier = Modifier.weight(1f).then(if (first) Modifier.padding(bottom = 8.dp) else Modifier),
                                shape = GameShapes.Medium,
                                color = if (first) GameColors.PrestigeGold.copy(alpha = .12f) else GameColors.SecondarySurface,
                                border = BorderStroke(1.dp, if (first) GameColors.PrestigeGold.copy(alpha = .35f) else GameColors.Divider),
                            ) {
                                Column(Modifier.padding(horizontal = 6.dp, vertical = if (first) 12.dp else 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$place", color = if (first) GameColors.PrestigeGold else GameColors.TextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Spacer(Modifier.height(5.dp))
                                    ProfilePhotoAvatarWithGender(
                                        avatarPath = player.avatarPath,
                                        gender = null,
                                        name = player.name,
                                        size = if (first) 48.dp else 40.dp,
                                        accent = if (first) GameColors.PrestigeGold else GameColors.PrimaryBlue,
                                        visible = true,
                                    )
                                    Spacer(Modifier.height(5.dp))
                                    Text(player.name, color = GameColors.TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${player.rating} RP", color = GameColors.TextSecondary, fontSize = 8.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeQuickAccess(onLeague: () -> Unit, onPro: () -> Unit, onCollection: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        GameSectionHeader(gameText("Hızlı Erişim", "Quick Access"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickAccessItem(Modifier.weight(1f), Icons.Rounded.EmojiEvents, gameText("Lig", "League"), GameColors.RewardAmber, onLeague)
            QuickAccessItem(Modifier.weight(1f), Icons.Rounded.WorkspacePremium, "PRO", GameColors.Lavender, onPro)
            QuickAccessItem(Modifier.weight(1f), Icons.Rounded.CollectionsBookmark, gameText("Koleksiyon", "Collection"), GameColors.TacticalTurquoise, onCollection)
        }
    }
}

@Composable
private fun QuickAccessItem(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 68.dp),
        shape = GameShapes.Medium,
        color = GameColors.PrimarySurface,
        border = BorderStroke(1.dp, GameColors.Border),
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(5.dp))
            Text(text, color = GameColors.TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
