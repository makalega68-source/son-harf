package com.sonharf.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
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
        ProfileHero(
            p = p,
            leagueLabel = profileLeagueName(league.leagueName) + sh(" Lig", " League"),
            rating = rating,
            winRate = winRate,
            nameColor = displayNameColor,
            hasNameStyle = hasEquippedNameStyle,
            onEdit = onEdit,
            onRename = { renaming = true },
        )

        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProfileStatTile(Icons.Rounded.EmojiEvents, sh("Galibiyet", "Wins"), profileGrouped(wins), Modifier.weight(1f).fillMaxHeight())
            ProfileStatTile(Icons.Rounded.SportsEsports, sh("Maç", "Matches"), profileGrouped(matches), Modifier.weight(1f).fillMaxHeight())
            ProfileStatTile(Icons.Rounded.Star, sh("Puan", "Rating"), profileGrouped(rating), Modifier.weight(1f).fillMaxHeight())
        }

        ProfileRecordsSection(backend, onRivals = onRivals)

        // Friends, Throne and events live on the home; the profile keeps only who you are.
        ProfileCollectionEntry(onCollection)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ProfileStatTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.premiumPanel(RoundedCornerShape(18.dp)).padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(LobbyBrand.Gold.copy(alpha = .16f))
            .border(1.dp, LobbyBrand.Gold.copy(alpha = .6f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = LobbyBrand.Gold, modifier = Modifier.size(20.dp))
        }
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black, maxLines = 1,
            style = androidx.compose.ui.text.TextStyle(brush = PremiumKit.goldText))
        Text(label, color = Color.White.copy(alpha = .78f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Identity stage: the framed photo inside a slowly turning gold halo, name, league and PRO. */
@Composable
private fun ProfileHero(
    p: ProfileDto?,
    leagueLabel: String,
    rating: Int,
    winRate: Int,
    nameColor: Color,
    hasNameStyle: Boolean,
    onEdit: () -> Unit,
    onRename: () -> Unit,
) {
    val t = rememberInfiniteTransition(label = "profile-halo")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "spin")
    val displayNameColor = if (hasNameStyle) nameColor else Color.White
    Column(Modifier.fillMaxWidth().premiumPanel(RoundedCornerShape(26.dp), glow = true).padding(vertical = 20.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize().rotate(spin)) {
                drawCircle(Brush.sweepGradient(listOf(Color.Transparent, PremiumKit.GoldLight, PremiumKit.Rim, Color.Transparent,
                    PremiumKit.GoldDeep, Color.Transparent)), radius = size.minDimension / 2, style = Stroke(3.dp.toPx()))
            }
            Canvas(Modifier.matchParentSize()) {
                drawCircle(Brush.radialGradient(listOf(LobbyBrand.Gold.copy(alpha = .30f), Color.Transparent)), radius = size.minDimension / 2)
            }
            Box(Modifier.clip(CircleShape).clickable(onClickLabel = sh("Profili düzenle", "Edit profile"), onClick = onEdit)) {
                FramedProfilePhotoAvatar(
                    avatarPath = p?.avatarPath,
                    gender = p?.gender,
                    name = p?.displayName ?: sh("Oyuncu", "Player"),
                    size = 108.dp,
                    frameId = rememberPlayerFrame(p?.id),
                    accent = Hf.Gold,
                    visible = p?.avatarVisibility != "hidden",
                    isPro = p?.isVip == true,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                p?.displayName ?: sh("Oyuncu", "Player"),
                modifier = Modifier.weight(1f, fill = false),
                color = displayNameColor,
                fontSize = 26.sp,
                style = premiumNameStyle(SonHarfCosmetics.nameStyleId),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (hasNameStyle) {
                Spacer(Modifier.width(6.dp))
                NameStyleEmblem(30.dp)
            }
        }
        PremiumRule(width = 160.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(50), color = LobbyBrand.NavBar.copy(alpha = .8f),
                border = BorderStroke(1.dp, LobbyBrand.Gold.copy(alpha = .7f))) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.hf_ic_club), null, tint = LobbyBrand.Gold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(leagueLabel, color = LobbyBrand.Gold, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1)
                }
            }
            if (p?.isVip == true) {
                Surface(shape = RoundedCornerShape(50), color = Color.Transparent,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(PremiumKit.goldText)) {
                    Text("PRO", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color(0xFF3A2A00), fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        Text(sh("$rating RP · Başarı %$winRate", "$rating RP · Win rate $winRate%"), color = Color.White.copy(alpha = .8f),
            fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProfileGoldOutlineButton(Icons.Rounded.Edit, sh("Profili düzenle", "Edit profile"), Modifier.weight(1f), onEdit)
            ProfileGoldOutlineButton(Icons.Rounded.Badge, sh("Adı değiştir", "Change name"), Modifier.weight(1f), onRename)
        }
    }
}

@Composable
private fun ProfileGoldOutlineButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.heightIn(min = 46.dp), shape = RoundedCornerShape(14.dp),
        color = LobbyBrand.NavBar.copy(alpha = .55f), border = BorderStroke(1.dp, LobbyBrand.Gold.copy(alpha = .75f))) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 12.dp), horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(17.dp), tint = LobbyBrand.Gold)
            Spacer(Modifier.width(6.dp))
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

/** Koleksiyonum entry as a gold-rimmed row with a preview of the shelves inside. */
@Composable
private fun ProfileCollectionEntry(onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().premiumPanel(RoundedCornerShape(20.dp)).clickable(onClickLabel = sh("Koleksiyonum", "My collection"), onClick = onClick)
        .padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(PremiumKit.goldText), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Palette, null, tint = Color(0xFF3A2A00), modifier = Modifier.size(26.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(sh("Koleksiyonum", "My collection"), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Text(sh("Temalar · Tahtalar · Çerçeveler · Maskot · Klavyeler · İsim Stili", "Themes · Boards · Frames · Mascot · Keyboards · Name Style"),
                color = LobbyBrand.Gold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = LobbyBrand.Gold, modifier = Modifier.size(26.dp))
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
