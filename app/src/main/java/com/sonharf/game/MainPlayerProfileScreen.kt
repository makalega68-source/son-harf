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
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WorkspacePremium
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Premium profile surface aligned with the application-wide Canva design system. */
@Composable
internal fun MainPlayerProfileScreen(
    backend: OnlineGameBackend,
    onEdit: () -> Unit,
    onVip: () -> Unit,
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
        if (loadedProfile?.isVip == true) {
            runCatching { backend.getFriends() }.getOrDefault(emptyList()).let { friends ->
                friendCount = friends.size
                onlineFriendCount = friends.count { (_, friend) -> friend.presenceStatus == "online" }
            }
        } else {
            friendCount = 0
            onlineFriendCount = 0
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
    val isPro = p?.isVip == true
    val profileAccent = if (isPro) SonHarfTheme.Purple else SonHarfTheme.Primary

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
                color = SonHarfTheme.Turquoise,
                trackColor = SonHarfTheme.SurfaceSecondary,
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = SonHarfTheme.Surface,
            shadowElevation = 4.dp,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                SonHarfTheme.Surface,
                                SonHarfTheme.PrimarySoft.copy(alpha = .72f),
                                SonHarfTheme.Purple.copy(alpha = .07f),
                                SonHarfTheme.Turquoise.copy(alpha = .06f),
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                if (isPro) {
                    Surface(
                        onClick = onVip,
                        modifier = Modifier.align(Alignment.TopEnd),
                        shape = RoundedCornerShape(99.dp),
                        color = SonHarfTheme.Purple.copy(alpha = .11f),
                    ) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.WorkspacePremium, null, Modifier.size(14.dp), tint = SonHarfTheme.Purple)
                            Spacer(Modifier.width(4.dp))
                            Text("PRO ÜYE", color = SonHarfTheme.Purple, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FramedProfilePhotoAvatar(
                        avatarPath = p?.avatarPath,
                        gender = p?.gender,
                        name = p?.displayName ?: sh("Oyuncu", "Player"),
                        size = 96.dp,
                        frameId = SonHarfCosmetics.profileFrameId,
                        accent = profileAccent,
                        visible = p?.avatarVisibility != "hidden",
                        isPro = isPro,
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(
                            p?.displayName ?: sh("Oyuncu", "Player"),
                            color = SonHarfTheme.TextPrimary,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                            ProfilePill(text = g?.nextTitle ?: sh("OYUNCU", "PLAYER"), accent = SonHarfTheme.Purple)
                            if (isPro) ProfilePill(text = "PRO", accent = SonHarfTheme.ActionOrange)
                        }
                        TextButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)) {
                            Icon(Icons.Rounded.Edit, null, Modifier.size(16.dp), tint = SonHarfTheme.Primary)
                            Spacer(Modifier.width(5.dp))
                            Text(sh("Profili düzenle", "Edit profile"), color = SonHarfTheme.Primary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
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
            shadowElevation = 2.dp,
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
                        color = SonHarfTheme.Turquoise,
                        trackColor = SonHarfTheme.SurfaceSecondary,
                    )
                    Text("$levelProgress / $levelTarget ${sh("sonraki seviyeye", "to next level")}", color = SonHarfTheme.TextSecondary, fontSize = 8.sp)
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
            modifier = Modifier.fillMaxWidth().clickable(onClick = if (isPro) onSocial else onVip),
            shape = RoundedCornerShape(20.dp),
            color = SonHarfTheme.Surface,
            border = BorderStroke(1.dp, if (isPro) SonHarfTheme.Border else SonHarfTheme.Purple.copy(alpha = .28f)),
            shadowElevation = 2.dp,
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isPro) SonHarfTheme.Turquoise.copy(alpha = .10f) else SonHarfTheme.Purple.copy(alpha = .10f),
                ) {
                    Icon(
                        if (isPro) Icons.Rounded.Groups else Icons.Rounded.WorkspacePremium,
                        contentDescription = null,
                        tint = if (isPro) SonHarfTheme.Turquoise else SonHarfTheme.Purple,
                        modifier = Modifier.padding(10.dp).size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(sh("Arkadaşlar", "Friends"), color = SonHarfTheme.TextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Text(
                        if (isPro) "$friendCount ${sh("arkadaş", "friends")} • $onlineFriendCount ${sh("çevrimiçi", "online")}" else sh("PRO ile arkadaş listesi ve yönetimi", "Friends list and management with PRO"),
                        color = SonHarfTheme.TextSecondary,
                        fontSize = 9.sp,
                    )
                }
                if (!isPro) {
                    Surface(shape = RoundedCornerShape(99.dp), color = SonHarfTheme.Purple.copy(alpha = .11f)) {
                        Text("PRO", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = SonHarfTheme.Purple, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.TextSecondary)
            }
        }

        OutlinedButton(
            onClick = onCollection,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SonHarfTheme.Primary),
            border = BorderStroke(1.dp, SonHarfTheme.Primary.copy(alpha = .34f)),
        ) {
            Icon(Icons.Rounded.Palette, null, Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Text(sh("KOLEKSİYONUM", "MY COLLECTION"), fontWeight = FontWeight.Black, fontSize = 11.sp)
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
        Text(text, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
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
