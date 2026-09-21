package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Composable
internal fun ProfessionalProfileScreen(
    backend: OnlineGameBackend,
    onEdit: () -> Unit,
    onPro: () -> Unit,
    onCollection: () -> Unit,
    onSettings: () -> Unit,
    onSocial: () -> Unit,
) {
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var growth by remember { mutableStateOf<GrowthDashboardDto?>(null) }
    var friendCount by remember { mutableIntStateOf(0) }
    var onlineFriendCount by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }

    suspend fun reload() = coroutineScope {
        loading = true
        val id = backend.currentUserId()
        val profileTask = async { id?.let { runCatching { backend.getProfile(it) }.getOrNull() } }
        val growthTask = async { runCatching { backend.getGrowthDashboard() }.getOrNull() }
        val cosmeticsTask = async { runCatching { backend.getEquippedCosmetics() }.getOrNull() }

        val loadedProfile = profileTask.await()
        profile = loadedProfile
        growth = growthTask.await()
        SonHarfCosmetics.apply(cosmeticsTask.await())

        if (loadedProfile?.isVip == true) {
            runCatching { backend.getFriends() }.getOrDefault(emptyList()).let { friends ->
                friendCount = friends.size
                onlineFriendCount = friends.count { (_, friend) -> friend.presenceStatus == "online" }
            }
        } else {
            friendCount = 0
            onlineFriendCount = 0
        }
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    val p = profile
    val g = growth
    val matches = g?.totalMatches ?: ((p?.wins ?: 0) + (p?.losses ?: 0))
    val wins = g?.wins ?: p?.wins ?: 0
    val losses = g?.losses ?: p?.losses ?: 0
    val winRate = if (matches <= 0) 0 else wins * 100 / matches
    val rating = p?.rating ?: 1000
    val league = ratingLeagueProgress(rating)
    val level = g?.level ?: 1
    val xp = g?.xp ?: 0
    val levelProgress = g?.levelProgress ?: 0
    val levelTarget = g?.levelTarget?.coerceAtLeast(1) ?: 500
    val xpProgress = (levelProgress.toFloat() / levelTarget).coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = GameSpacing.ScreenHorizontal, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "profile_header") {
            GameTopBar(
                title = gameText("Profil", "Profile"),
                subtitle = gameText("Oyuncu kimliğin ve ilerlemen", "Your player identity and progress"),
                trailing = {
                    GameIconButton(
                        icon = Icons.Rounded.Settings,
                        description = gameText("Ayarlar", "Settings"),
                        onClick = onSettings,
                    )
                },
            )
        }

        if (loading) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = GameColors.PrimaryBlue, trackColor = GameColors.SecondarySurface) }
        }

        item(key = "identity") {
            ProfileIdentityCard(
                profile = p,
                title = g?.nextTitle,
                rating = rating,
                league = league.leagueName,
                level = level,
                onEdit = onEdit,
                onPro = onPro,
            )
        }

        item(key = "xp") {
            GameSurface {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(gameText("Seviye $level", "Level $level"), color = GameColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(Modifier.weight(1f))
                        Text("$xp XP", color = GameColors.PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    XPProgress(xpProgress)
                    Text(
                        "$levelProgress / $levelTarget ${gameText("sonraki seviyeye", "to next level")}",
                        color = GameColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        item(key = "stats_title") { GameSectionHeader(gameText("İstatistikler", "Statistics")) }
        item(key = "stats") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileStatCard(Modifier.weight(1f), wins.toString(), gameText("Galibiyet", "Wins"), Icons.Rounded.EmojiEvents, GameColors.PlayGreen)
                    ProfileStatCard(Modifier.weight(1f), losses.toString(), gameText("Mağlubiyet", "Losses"), Icons.Rounded.Close, GameColors.Danger)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileStatCard(Modifier.weight(1f), "%$winRate", gameText("Kazanma", "Win rate"), Icons.Rounded.ShowChart, GameColors.TacticalTurquoise)
                    ProfileStatCard(Modifier.weight(1f), matches.toString(), gameText("Oyun", "Games"), Icons.Rounded.SportsEsports, GameColors.Lavender)
                }
            }
        }

        item(key = "profile_actions") {
            GameSurface {
                Column(Modifier.fillMaxWidth()) {
                    ProfileActionRow(
                        icon = Icons.Rounded.Palette,
                        title = gameText("Koleksiyonum", "My Collection"),
                        subtitle = gameText("Avatar, çerçeve ve kozmetiklerini yönet", "Manage avatars, frames and cosmetics"),
                        accent = GameColors.TacticalTurquoise,
                        onClick = onCollection,
                    )
                    HorizontalDivider(color = GameColors.Divider)
                    ProfileActionRow(
                        icon = Icons.Rounded.Groups,
                        title = gameText("Arkadaşlar", "Friends"),
                        subtitle = if (p?.isVip == true) {
                            "$friendCount ${gameText("arkadaş", "friends")} • $onlineFriendCount ${gameText("çevrimiçi", "online")}"
                        } else {
                            gameText("Sosyal merkezini aç", "Open your social hub")
                        },
                        accent = GameColors.PrimaryBlue,
                        onClick = onSocial,
                    )
                    HorizontalDivider(color = GameColors.Divider)
                    ProfileActionRow(
                        icon = Icons.Rounded.MilitaryTech,
                        title = gameText("Başarımlar ve Lig", "Achievements & League"),
                        subtitle = gameText("Prestijini ve ilerleme durumunu takip et", "Track prestige and competitive progress"),
                        accent = GameColors.RewardAmber,
                        onClick = onEdit,
                    )
                }
            }
        }

        item(key = "pro") {
            Surface(
                onClick = onPro,
                modifier = Modifier.fillMaxWidth(),
                shape = GameShapes.Large,
                color = GameColors.Lavender.copy(alpha = .12f),
                border = BorderStroke(1.dp, GameColors.Lavender.copy(alpha = .38f)),
            ) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = GameShapes.Medium, color = GameColors.Lavender.copy(alpha = .18f)) {
                        Icon(Icons.Rounded.WorkspacePremium, null, tint = GameColors.PrestigeGold, modifier = Modifier.padding(10.dp).size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (p?.isVip == true) gameText("PRO Üyeliğin", "Your PRO Membership") else gameText("PRO'yu Keşfet", "Explore PRO"),
                            color = GameColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                        Text(
                            gameText("Kozmetik, prestij ve konfor · pay-to-win yok", "Cosmetics, prestige and comfort · no pay-to-win"),
                            color = GameColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = GameColors.TextSecondary)
                }
            }
        }

        item { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
private fun ProfileIdentityCard(
    profile: ProfileDto?,
    title: String?,
    rating: Int,
    league: String,
    level: Int,
    onEdit: () -> Unit,
    onPro: () -> Unit,
) {
    val isPro = profile?.isVip == true
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = GameShapes.Hero,
        color = Color.Transparent,
        border = BorderStroke(1.dp, if (isPro) GameColors.PrestigeGold.copy(alpha = .38f) else GameColors.Border),
    ) {
        Box(
            Modifier.fillMaxWidth().background(
                Brush.linearGradient(
                    if (isPro) listOf(Color(0xFF27304A), Color(0xFF30274A), GameColors.PrimarySurface)
                    else listOf(Color(0xFF17345A), GameColors.PrimarySurface, GameColors.ElevatedBackground)
                ),
                GameShapes.Hero,
            ).padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FramedProfilePhotoAvatar(
                        avatarPath = profile?.avatarPath,
                        gender = profile?.gender,
                        name = profile?.displayName ?: gameText("Oyuncu", "Player"),
                        size = 88.dp,
                        frameId = SonHarfCosmetics.profileFrameId,
                        accent = if (isPro) GameColors.PrestigeGold else GameColors.PrimaryBlue,
                        visible = profile?.avatarVisibility != "hidden",
                        isPro = isPro,
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                profile?.displayName ?: gameText("Oyuncu", "Player"),
                                modifier = Modifier.weight(1f, fill = false),
                                color = GameColors.TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (isPro) {
                                Spacer(Modifier.width(7.dp))
                                Surface(onClick = onPro, shape = GameShapes.Pill, color = GameColors.PrestigeGold.copy(alpha = .16f)) {
                                    Text("PRO", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = GameColors.PrestigeGold, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                }
                            }
                        }
                        Text(title ?: gameText("OYUNCU", "PLAYER"), color = GameColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            LeagueBadge(league)
                            RatingBadge(rating)
                        }
                        TextButton(onClick = onEdit, contentPadding = PaddingValues(0.dp)) {
                            Icon(Icons.Rounded.Edit, null, tint = GameColors.PrimaryBlue, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(gameText("Profili düzenle", "Edit profile"), color = GameColors.PrimaryBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IdentityMiniStat(Modifier.weight(1f), rating.toString(), gameText("Rating", "Rating"))
                    IdentityMiniStat(Modifier.weight(1f), league, gameText("Lig", "League"))
                    IdentityMiniStat(Modifier.weight(1f), level.toString(), gameText("Seviye", "Level"))
                }
            }
        }
    }
}

@Composable
private fun IdentityMiniStat(modifier: Modifier, value: String, label: String) {
    Surface(modifier = modifier, shape = GameShapes.Medium, color = Color.Black.copy(alpha = .14f)) {
        Column(Modifier.padding(vertical = 9.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = GameColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, color = GameColors.TextSecondary, fontSize = 9.sp)
        }
    }
}

@Composable
private fun ProfileStatCard(
    modifier: Modifier,
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
) {
    Surface(
        modifier = modifier.heightIn(min = 86.dp),
        shape = GameShapes.Large,
        color = GameColors.PrimarySurface,
        border = BorderStroke(1.dp, GameColors.Border),
    ) {
        Row(Modifier.fillMaxSize().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = GameShapes.Medium, color = accent.copy(alpha = .13f)) {
                Icon(icon, null, tint = accent, modifier = Modifier.padding(8.dp).size(19.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(value, color = GameColors.TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(label, color = GameColors.TextSecondary, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ProfileActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = GameShapes.Medium, color = accent.copy(alpha = .12f)) {
                Icon(icon, null, tint = accent, modifier = Modifier.padding(8.dp).size(20.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = GameColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(subtitle, color = GameColors.TextSecondary, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = GameColors.TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}
