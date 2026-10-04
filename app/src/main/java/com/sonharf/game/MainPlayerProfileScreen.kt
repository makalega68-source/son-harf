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
import kotlinx.coroutines.launch

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
    onRivals: () -> Unit = onSocial,
    onCompete: () -> Unit = {},
) {
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var growth by remember { mutableStateOf<GrowthDashboardDto?>(null) }
    var friendCount by remember { mutableIntStateOf(0) }
    var onlineFriendCount by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var renaming by remember { mutableStateOf(false) }

    suspend fun reload() = coroutineScope {
        loading = true
        val id = backend.currentUserId()
        val profileTask = async { id?.let { runCatching { backend.getProfile(it) }.getOrNull() } }
        val growthTask = async { runCatching { backend.getGrowthDashboard() }.getOrNull() }
        val cosmeticsTask = async { runCatching { backend.getEquippedCosmetics() }.getOrNull() }

        val loadedProfile = profileTask.await()
        profile = loadedProfile
        growth = growthTask.await()
        runCatching { backend.getFriends() }.getOrDefault(emptyList()).let { friends ->
            friendCount = friends.size
            onlineFriendCount = friends.count { (_, friend) -> friend.isRecentlyOnline() }
        }
        SonHarfCosmetics.apply(cosmeticsTask.await())
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    if (renaming) {
        ChangeNameDialog(
            current = profile?.displayName.orEmpty(),
            onDismiss = { renaming = false },
            onChanged = { newName ->
                profile = profile?.copy(displayName = newName)
                renaming = false
            },
        )
    }

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
        "mascot_hat" to sh("Obi", "Obi"),
        "profile_frame" to sh("Çerçeve", "Frame"),
        "game_theme" to sh("Tema", "Theme"),
        "board_skin" to sh("Tahta", "Board"),
        "keyboard_theme" to sh("Klavye", "Keys"),
        "name_style" to sh("İsim", "Name"),
    )

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MainScreenHeader(
            title = sh("Profil", "Profile"),
            subtitle = "",
            actionIcon = Icons.Rounded.Settings,
            actionDescription = sh("Ayarlar", "Settings"),
            onAction = onSettings,
        )

        if (loading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = Hf.Gold,
                trackColor = LobbyPalette.Paper,
            )
        }

        val hasEquippedNameStyle = !SonHarfCosmetics.nameStyleId.isNullOrBlank()
        val displayNameColor = when {
            hasEquippedNameStyle -> SonHarfCosmetics.playerNameColor
            else -> LobbyPalette.Ink
        }
        LobbyCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clickable(onClick = onEdit)) {
                    FramedProfilePhotoAvatar(
                        avatarPath = p?.avatarPath,
                        gender = p?.gender,
                        name = p?.displayName ?: sh("Oyuncu", "Player"),
                        size = 84.dp,
                        frameId = rememberPlayerFrame(p?.id),
                        accent = Hf.Gold,
                        visible = p?.avatarVisibility != "hidden",
                        isPro = p?.isVip == true,
                    )
                }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            p?.displayName ?: sh("Oyuncu", "Player"),
                            modifier = Modifier.weight(1f, fill = false),
                            color = displayNameColor,
                            fontSize = 24.sp,
                            style = premiumNameStyle(SonHarfCosmetics.nameStyleId),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (hasEquippedNameStyle) {
                            Spacer(Modifier.width(6.dp))
                            NameStyleEmblem(30.dp)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.hf_ic_club), null, tint = Hf.Gold, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            profileLeagueName(league.leagueName) + sh(" Lig", " League"),
                            modifier = Modifier.weight(1f), overflow = TextOverflow.Ellipsis,
                            color = LobbyPalette.Gold,
                            fontSize = 16.sp,
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
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                    border = BorderStroke(1.dp, LobbyPalette.Line)) {
                    Icon(Icons.Rounded.Edit, null, Modifier.size(16.dp), tint = LobbyPalette.Accent)
                    Spacer(Modifier.width(6.dp))
                    Text(sh("Profili düzenle", "Edit profile"), color = LobbyPalette.Ink, fontSize = 13.sp)
                }
                OutlinedButton(onClick = { renaming = true }, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                    border = BorderStroke(1.dp, LobbyPalette.Line)) {
                    Text(sh("Adı değiştir", "Change name"), color = LobbyPalette.Ink, fontSize = 13.sp)
                }
            }

            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProfileStatTile(Icons.Rounded.EmojiEvents, sh("Galibiyet", "Wins"), profileGrouped(wins), Modifier.weight(1f))
            ProfileStatTile(Icons.Rounded.SportsEsports, sh("Maç", "Matches"), profileGrouped(matches), Modifier.weight(1f))
            ProfileStatTile(Icons.Rounded.Star, sh("Puan", "Rating"), profileGrouped(rating), Modifier.weight(1f))
        }

        ProfileRecordsSection(backend, onRivals = onRivals)

        LobbyCard(
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
                    Text(sh("Arkadaşlar", "Friends"), color = LobbyPalette.Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(sh("Arkadaş listesini aç", "Open your friend list"), color = LobbyPalette.Muted, fontSize = 12.sp)
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
        OutlinedButton(onClick = onCompete, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(sh("Taht ve sıralama", "Throne and rankings"), color = LobbyPalette.Ink)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ProfileStatTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = LobbyPalette.Paper,
        border = BorderStroke(1.dp, LobbyPalette.Line),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, tint = LobbyPalette.Gold, modifier = Modifier.size(22.dp))
            Text(value, color = LobbyPalette.Ink, fontSize = 22.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(label, color = LobbyPalette.Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        Text(value, color = LobbyPalette.Ink, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(label, color = LobbyPalette.Muted, fontSize = 8.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun InlineProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(min = 72.dp)) {
        Text(value, color = LobbyPalette.Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text(label, color = LobbyPalette.Muted, fontSize = 8.sp, textAlign = TextAlign.Center)
    }
}


/** Player-name change; the server checks length, characters, uniqueness and the daily limit. */
@Composable
private fun ChangeNameDialog(current: String, onDismiss: () -> Unit, onChanged: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(current) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(sh("Oyuncu adını değiştir", "Change player name"), fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24); error = null },
                    singleLine = true,
                    label = { Text(sh("Yeni ad", "New name")) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    error ?: sh("2-24 karakter. Günde bir kez değiştirilebilir.", "2-24 characters. Can be changed once a day."),
                    color = if (error != null) Color(0xFFD9534F) else LobbyPalette.Muted,
                    fontSize = 12.sp,
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !busy && name.trim().length >= 2 && name.trim() != current,
                onClick = {
                    busy = true
                    scope.launch {
                        runCatching { DisplayNameBackend.change(name.trim()) }
                            .onSuccess { onChanged(it.displayName.ifBlank { name.trim() }) }
                            .onFailure { failure ->
                                val raw = failure.message.orEmpty()
                                error = when {
                                    "display_name_taken" in raw -> sh("Bu ad başka bir oyuncuda. Başka bir ad dene.", "That name is taken. Try another.")
                                    "display_name_cooldown" in raw -> sh("Adını en fazla günde bir kez değiştirebilirsin.", "You can change your name once a day.")
                                    "invalid_display_name_chars" in raw -> sh("Sadece harf, rakam, boşluk ve . - _ kullanılabilir.", "Only letters, digits, spaces and . - _ are allowed.")
                                    "invalid_display_name" in raw -> sh("Ad 2-24 karakter olmalı.", "The name must be 2-24 characters.")
                                    else -> sh("Ad değiştirilemedi. Bağlantını kontrol edip tekrar dene.", "Could not change the name. Check your connection and try again.")
                                }
                            }
                        busy = false
                    }
                },
            ) { Text(if (busy) sh("Kaydediliyor…", "Saving…") else sh("Kaydet", "Save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text(sh("Vazgeç", "Cancel")) } },
    )
}
