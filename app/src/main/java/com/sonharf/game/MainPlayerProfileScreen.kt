package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Settings
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
import com.sonharf.game.data.GrowthDashboardDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Deliberately compact profile surface.
 * Identity, progression, competition and social status are visible without turning the profile
 * into a second dashboard. Detailed editing and settings stay behind explicit actions.
 */
@Composable
internal fun MainPlayerProfileScreen(
    backend: OnlineGameBackend,
    onEdit: () -> Unit,
    onVip: () -> Unit,
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
        val friendsTask = async { runCatching { backend.getFriends() }.getOrDefault(emptyList()) }
        val cosmeticsTask = async { runCatching { backend.getEquippedCosmetics() }.getOrNull() }

        profile = profileTask.await()
        growth = growthTask.await()
        friendsTask.await().let { friends ->
            friendCount = friends.size
            onlineFriendCount = friends.count { (_, friend) -> friend.presenceStatus == "online" }
        }
        SonHarfCosmetics.apply(cosmeticsTask.await())
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

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MainScreenHeader(
            title = sh("Profil", "Profile"),
            subtitle = sh("Oyuncu kimliğin ve temel ilerlemen", "Your identity and core progress"),
            actionIcon = Icons.Rounded.Settings,
            actionDescription = sh("Ayarlar", "Settings"),
            onAction = onSettings,
        )

        if (loading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = SonHarfTheme.Primary,
                trackColor = SonHarfTheme.SurfaceSecondary,
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = SonHarfTheme.Surface,
            border = BorderStroke(1.dp, SonHarfTheme.Primary.copy(alpha = .22f)),
            shadowElevation = 4.dp,
        ) {
            Box(
                Modifier.fillMaxWidth().background(
                    Brush.horizontalGradient(
                        listOf(
                            SonHarfTheme.Primary.copy(alpha = .13f),
                            SonHarfTheme.Surface,
                            SonHarfTheme.Turquoise.copy(alpha = .08f),
                        )
                    )
                ).padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FramedProfilePhotoAvatar(
                        avatarPath = p?.avatarPath,
                        gender = p?.gender,
                        name = p?.displayName ?: sh("Oyuncu", "Player"),
                        size = 88.dp,
                        frameId = SonHarfCosmetics.profileFrameId,
                        accent = if (p?.isVip == true) SonHarfTheme.PremiumGold else SonHarfTheme.Primary,
                        visible = p?.avatarVisibility != "hidden",
                        showGenderBadge = false,
                    )
                    Spacer(Modifier.width(15.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            p?.displayName ?: sh("Oyuncu", "Player"),
                            color = SonHarfCosmetics.playerNameColor,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                            ProfilePill(
                                text = g?.nextTitle ?: sh("OYUNCU", "PLAYER"),
                                accent = SonHarfTheme.Primary,
                            )
                            if (p?.isVip == true) {
                                ProfilePill(text = "VIP", accent = SonHarfTheme.PremiumGold)
                            }
                        }
                        TextButton(
                            onClick = onEdit,
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp),
                        ) {
                            Icon(Icons.Rounded.Edit, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(sh("Profili düzenle", "Edit profile"), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = SonHarfTheme.Surface,
            border = BorderStroke(1.dp, SonHarfTheme.Border),
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SimpleProfileMetric(rating.toString(), sh("Puan", "Rating"), Modifier.weight(1f))
                    SimpleProfileMetric(league.leagueName, sh("Lig", "League"), Modifier.weight(1f))
                    SimpleProfileMetric(level.toString(), sh("Seviye", "Level"), Modifier.weight(1f))
                }

                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(sh("İLERLEME", "PROGRESS"), color = SonHarfTheme.TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        Text("$xp XP", color = SonHarfTheme.TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { xpProgress },
                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                        color = SonHarfTheme.Primary,
                        trackColor = SonHarfTheme.SurfaceSecondary,
                    )
                    Text(
                        "$levelProgress / $levelTarget ${sh("sonraki seviyeye", "to next level")}",
                        color = SonHarfTheme.TextSecondary,
                        fontSize = 8.sp,
                    )
                }

                HorizontalDivider(color = SonHarfTheme.Border)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    InlineProfileStat(wins.toString(), sh("Galibiyet", "Wins"))
                    InlineProfileStat(losses.toString(), sh("Mağlubiyet", "Losses"))
                    InlineProfileStat("%$winRate", sh("Kazanma", "Win rate"))
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onSocial),
            shape = RoundedCornerShape(20.dp),
            color = SonHarfTheme.Surface,
            border = BorderStroke(1.dp, SonHarfTheme.Border),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(shape = RoundedCornerShape(14.dp), color = SonHarfTheme.Primary.copy(alpha = .11f)) {
                    Icon(
                        Icons.Rounded.Groups,
                        contentDescription = null,
                        tint = SonHarfTheme.Primary,
                        modifier = Modifier.padding(10.dp).size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(sh("Arkadaşlar", "Friends"), color = SonHarfTheme.TextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Text(
                        "$friendCount ${sh("arkadaş", "friends")} • $onlineFriendCount ${sh("çevrimiçi", "online")}",
                        color = SonHarfTheme.TextSecondary,
                        fontSize = 9.sp,
                    )
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.TextSecondary)
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            sh("Profil yalnızca gerekli bilgileri gösterir. Ayrıntılar düzenleme ve ayarlar içinde.", "Profile shows only what matters. Details live in edit and settings."),
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            color = SonHarfTheme.TextSecondary,
            fontSize = 8.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ProfilePill(text: String, accent: Color) {
    Surface(shape = RoundedCornerShape(9.dp), color = accent.copy(alpha = .12f)) {
        Text(
            text,
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = accent,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun SimpleProfileMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = SonHarfTheme.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(label, color = SonHarfTheme.TextSecondary, fontSize = 8.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun InlineProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(min = 72.dp)) {
        Text(value, color = SonHarfTheme.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text(label, color = SonHarfTheme.TextSecondary, fontSize = 8.sp, textAlign = TextAlign.Center)
    }
}
