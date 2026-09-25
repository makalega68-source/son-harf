package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.painterResource
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

    var collectionTab by rememberSaveable { mutableIntStateOf(1) }
    val collectionTabs = listOf(
        "profile_frame" to sh("Çerçeveler", "Frames"),
        "game_theme" to sh("Temalar", "Themes"),
        "keyboard_theme" to sh("Klavyeler", "Keyboards"),
        "name_style" to sh("İsim rengi", "Name color"),
    )

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MainScreenHeader(
            title = sh("Profil", "Profile"),
            subtitle = "",
            actionIcon = null,
            onAction = null,
        )

        if (loading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = Hf.Gold,
                trackColor = Hf.Surface,
            )
        }

        val hasEquippedNameStyle = !SonHarfCosmetics.nameStyleId.isNullOrBlank()
        val displayNameColor = when {
            hasEquippedNameStyle -> SonHarfCosmetics.playerNameColor
            else -> Hf.Ivory
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clickable(onClick = onEdit)) {
                FramedProfilePhotoAvatar(
                    avatarPath = p?.avatarPath,
                    gender = p?.gender,
                    name = p?.displayName ?: sh("Oyuncu", "Player"),
                    size = 104.dp,
                    frameId = SonHarfCosmetics.profileFrameId,
                    accent = Hf.Gold,
                    visible = p?.avatarVisibility != "hidden",
                    isPro = p?.isVip == true,
                )
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    p?.displayName ?: sh("Oyuncu", "Player"),
                    color = displayNameColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.hf_ic_club), null, tint = Hf.Gold, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        profileLeagueName(league.leagueName) + sh(" Lig", " League"),
                        color = Hf.Gold,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    if (p?.isVip == true) {
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = Hf.PillShape, color = Hf.Gold) {
                            Text("PRO", Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = Hf.Ink, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)) {
                        Icon(Icons.Rounded.Edit, null, Modifier.size(16.dp), tint = Hf.Gold)
                        Spacer(Modifier.width(5.dp))
                        Text(sh("Profili düzenle", "Edit profile"), color = Hf.TextMuted, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onSettings, modifier = Modifier.size(44.dp)) {
                        Icon(painterResource(R.drawable.hf_ic_settings), sh("Ayarlar", "Settings"), tint = Hf.Gold, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProfileStatTile(Icons.Rounded.EmojiEvents, sh("Galibiyet", "Wins"), profileGrouped(wins), Modifier.weight(1f))
            ProfileStatTile(Icons.Rounded.SportsEsports, sh("Maç", "Matches"), profileGrouped(matches), Modifier.weight(1f))
            ProfileStatTile(Icons.Rounded.Star, sh("Puan", "Rating"), profileGrouped(rating), Modifier.weight(1f))
        }

        HfSegmentedTabs(
            labels = collectionTabs.map { it.second },
            selected = collectionTab,
            onSelect = { collectionTab = it },
        )
        ProfileOwnedThemesSection(backend, category = collectionTabs[collectionTab].first)

        HfCard(
            onClick = onSocial,
            modifier = Modifier.fillMaxWidth(),
            borderColor = if (p?.isVip == true) Hf.Gold.copy(alpha = .75f) else Hf.Gold,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (p?.isVip == true) Icons.Rounded.Groups else Icons.Rounded.WorkspacePremium,
                    contentDescription = null,
                    tint = Hf.Gold,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(sh("Arkadaşlar", "Friends"), color = Hf.Ivory, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        if (p?.isVip == true) {
                            "$friendCount ${sh("arkadaş", "friends")} • $onlineFriendCount ${sh("çevrimiçi", "online")}"
                        } else {
                            sh("PRO ile arkadaş listesi ve yönetimi", "Friends list and management with PRO")
                        },
                        color = Hf.TextMuted,
                        fontSize = 12.sp,
                    )
                }
                if (p?.isVip != true) {
                    Surface(shape = Hf.PillShape, color = Hf.Gold) {
                        Text("PRO", Modifier.padding(horizontal = 8.dp, vertical = 3.dp), color = Hf.Ink, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = Hf.Gold)
            }
        }

        HfSecondaryButton(
            sh("Koleksiyonum", "My collection"),
            onClick = onCollection,
            modifier = Modifier.fillMaxWidth(),
            trailingChevron = true,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ProfileStatTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Hf.Ivory,
        border = BorderStroke(1.5.dp, Hf.Gold),
        shadowElevation = 2.dp,
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Hf.Gold, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(6.dp))
            Column {
                Text(label, color = Hf.Ink, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(value, color = Hf.Ink, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 1)
            }
        }
    }
}

private fun profileGrouped(value: Int): String = String.format(java.util.Locale("tr", "TR"), "%,d", value)

private fun profileLeagueName(value: String): String = when (value) {
    "BRONZ" -> sh("Bronz", "Bronze")
    "GÜMÜŞ" -> sh("Gümüş", "Silver")
    "ALTIN" -> sh("Altın", "Gold")
    "PLATİN" -> sh("Platin", "Platinum")
    "ELMAS" -> sh("Elmas", "Diamond")
    "EFSANE" -> sh("Efsane", "Legend")
    else -> value
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
