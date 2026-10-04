package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

/** Lobby colours: deep petrol behind the green/red logo, two bold action buttons, gold accents. */
internal object LobbyBrand {
    val Sky = Color(0xFF14596A)
    val SkyStripe = Color(0xFF1A6A7D)
    val Band = Color(0xFF0D3C48)
    val NavBar = Color(0xFF0B323C)
    val Chip = Color(0xFF1F7489)
    val Play = Color(0xFF3FAE49)
    val PlayEdge = Color(0xFF2B7F33)
    val Games = Color(0xFFE07B1C)
    val GamesEdge = Color(0xFFA35711)
    val Gold = Color(0xFFF2C14E)
}

/**
 * Kelimelik-style lobby: brand stage, a profile strip with the avatar in the middle, two big
 * buttons (Yeni Oyun / Oyunlarım) and one event card. Game lists live in Oyunlarım, not here;
 * the bottom bar (Mağaza, Taht, Oyna, Oyunlar, Profil) is drawn by the shell.
 */
@Composable
internal fun HomeLobbyScreen(
    backend: OnlineGameBackend,
    isPro: Boolean,
    onProfile: () -> Unit,
    onShop: () -> Unit,
    onPro: () -> Unit,
    onSettings: () -> Unit,
    onNewGame: () -> Unit,
    onMyGames: () -> Unit,
    onFriends: () -> Unit,
    onEvents: () -> Unit,
    onWorkshop: () -> Unit,
) {
    val me = backend.currentUserId()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var waitingForMe by remember { mutableIntStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    var leagueInfo by remember { mutableStateOf(false) }
    val foreground = rememberAppForeground()

    LaunchedEffect(Unit) {
        if (!SupabaseProvider.configured) return@LaunchedEffect
        profile = me?.let { id -> gameRequestResult { backend.getProfile(id) }.getOrNull() }
    }
    // Only a badge count: open games where it is your turn plus pending invitations.
    LaunchedEffect(me, foreground) {
        if (me == null || !foreground) return@LaunchedEffect
        while (true) {
            coroutineScope {
                val classic = async { gameRequestResult { backend.getWordSiegeGameSummaries("classic", finishedLimit = 0) }.getOrNull() }
                val series = async { gameRequestResult { backend.getWordSiegeGameSummaries("series", finishedLimit = 0) }.getOrNull() }
                val rooms = async { gameRequestResult { backend.getLastLetterRooms() }.getOrNull() }
                val inv1 = async { gameRequestResult { backend.getIncomingWordSiegeInvites().size }.getOrDefault(0) }
                val inv2 = async { gameRequestResult { backend.getIncomingGameInvites().size }.getOrDefault(0) }
                val rows = matchListRows(classic.await().orEmpty() + series.await().orEmpty(), rooms.await().orEmpty(), me)
                waitingForMe = rows.count { !it.finished && it.turn == HomeTurn.YOURS } + inv1.await() + inv2.await()
            }
            delay(20_000)
        }
    }

    val p = profile
    val matches = (p?.wins ?: 0) + (p?.losses ?: 0)
    val winRate = if (matches == 0) 0 else (p?.wins ?: 0) * 100 / matches
    val league = ratingLeagueProgress(p?.rating ?: 1000)

    Box(Modifier.fillMaxSize().background(LobbyBrand.Sky)) {
        LobbyStripes(Modifier.matchParentSize())
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Top band: menu on the left, coins and settings on the right.
            Row(Modifier.fillMaxWidth().background(LobbyBrand.Band).padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Rounded.Menu, sh("Menü", "Menu"), tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        LobbyMenuItem(Icons.Rounded.Groups, sh("Arkadaşlar", "Friends") + if (isPro) "" else " · PRO") { menuOpen = false; onFriends() }
                        LobbyMenuItem(Icons.Rounded.Event, sh("Etkinlikler", "Events")) { menuOpen = false; onEvents() }
                        LobbyMenuItem(Icons.Rounded.WorkspacePremium, "PRO") { menuOpen = false; onPro() }
                        LobbyMenuItem(Icons.Rounded.Settings, sh("Ayarlar", "Settings")) { menuOpen = false; onSettings() }
                    }
                }
                Spacer(Modifier.weight(1f))
                Surface(onClick = onShop, shape = RoundedCornerShape(50), color = LobbyBrand.Chip) {
                    Row(Modifier.padding(start = 6.dp, end = 12.dp, top = 5.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        HfCoin(24.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(p?.diamonds?.let { lobbyGrouped(it) } ?: "—", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Rounded.AddCircle, sh("Jeton al", "Get coins"), tint = LobbyBrand.Gold, modifier = Modifier.size(20.dp))
                    }
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Rounded.Settings, sh("Ayarlar", "Settings"), tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }

            // Brand stage.
            Box(Modifier.fillMaxWidth().height(170.dp).padding(horizontal = 32.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.kelime_tahti_brand_logo), sh("Kelime Tahtı", "Word Throne"),
                    contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            }

            // Profile strip: name and success on the left, avatar in the middle, league on the right.
            Box(Modifier.fillMaxWidth().height(116.dp)) {
                Row(Modifier.fillMaxWidth().align(Alignment.Center).height(84.dp).background(LobbyBrand.Band)
                    .padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(p?.displayName?.ifBlank { null } ?: sh("Oyuncu", "Player"), color = Color.White,
                            fontSize = 17.sp, style = premiumNameStyle(SonHarfCosmetics.nameStyleId),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        LobbyPill(sh("Başarı ", "Win rate ") + "%$winRate")
                    }
                    Spacer(Modifier.width(112.dp))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${p?.rating ?: 1000} RP", color = LobbyBrand.Gold, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        LobbyPill(lobbyLeagueName(league.leagueName) + sh(" Lig", " League"), info = true) { leagueInfo = true }
                    }
                }
                Box(Modifier.align(Alignment.Center).size(112.dp).clip(CircleShape).background(LobbyBrand.Band)
                    .clickable(onClickLabel = sh("Profili aç", "Open profile"), onClick = onProfile), contentAlignment = Alignment.Center) {
                    FramedProfilePhotoAvatar(avatarPath = p?.avatarPath, gender = p?.gender,
                        name = p?.displayName ?: sh("Oyuncu", "Player"), size = 100.dp,
                        frameId = rememberPlayerFrame(p?.id), accent = LobbyBrand.Gold,
                        visible = p?.avatarVisibility != "hidden", isPro = p?.isVip == true)
                }
            }

            // The two big buttons.
            Surface(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), shape = RoundedCornerShape(18.dp),
                color = Color.White, shadowElevation = 4.dp) {
                Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LobbyBigButton(Icons.Rounded.PlayArrow, sh("Yeni Oyun", "New Game"), LobbyBrand.Play, LobbyBrand.PlayEdge,
                        Modifier.weight(1f), onClick = onNewGame)
                    LobbyBigButton(Icons.Rounded.FormatListBulleted, sh("Oyunlarım", "My Games"), LobbyBrand.Games, LobbyBrand.GamesEdge,
                        Modifier.weight(1f), badge = waitingForMe, onClick = onMyGames)
                }
            }

            // One event card, like the monthly reward banner.
            Box(Modifier.padding(horizontal = 20.dp)) { HomeTournamentCard(onOpen = onWorkshop) }
            Spacer(Modifier.height(96.dp))
        }
        // Owned mascots keep floating over the lobby; they take no row.
        WordSiegeMascotCompanion(
            anchors = listOf(Offset(.88f, .92f), Offset(.12f, .92f)),
            mascotSize = 83.dp, positionKey = "home",
            moveId = null, lastMoveMine = false, playerTurn = false,
            modifier = Modifier.matchParentSize(),
            playerName = p?.displayName, playerGender = p?.gender,
            stageY = .5f,
        )
    }

    if (leagueInfo) AlertDialog(
        onDismissRequest = { leagueInfo = false },
        title = { Text(lobbyLeagueName(league.leagueName) + sh(" Lig", " League"), fontWeight = FontWeight.Black) },
        text = {
            Text(
                if (league.nextAt != null) sh("Puanın ${p?.rating ?: 1000}. ${lobbyLeagueName(league.nextLeagueName)} Lig için ${league.pointsToNext} puan daha kazan. Kazandıkça puan artar, kaybedince azalır.",
                    "Your rating is ${p?.rating ?: 1000}. Earn ${league.pointsToNext} more to reach ${lobbyLeagueName(league.nextLeagueName)} League. Wins raise it, losses lower it.")
                else sh("En üst ligdesin. Puanını korumak için oynamaya devam et.", "You are in the top league. Keep playing to hold your rating."),
            )
        },
        confirmButton = { TextButton(onClick = { leagueInfo = false }) { Text(sh("TAMAM", "OK")) } },
    )
}

@Composable
private fun LobbyStripes(modifier: Modifier) {
    Canvas(modifier) {
        val step = 120.dp.toPx()
        var x = -size.height
        while (x < size.width + size.height) {
            val path = Path().apply {
                moveTo(x, size.height); lineTo(x + step / 2, size.height)
                lineTo(x + step / 2 + size.height, 0f); lineTo(x + size.height, 0f); close()
            }
            drawPath(path, LobbyBrand.SkyStripe.copy(alpha = .45f))
            x += step
        }
    }
}

@Composable
private fun LobbyMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(label, fontSize = 16.sp) }, onClick = onClick, leadingIcon = { Icon(icon, null) })
}

@Composable
private fun LobbyPill(text: String, info: Boolean = false, onClick: (() -> Unit)? = null) {
    Surface(onClick = onClick ?: {}, enabled = onClick != null, shape = RoundedCornerShape(10.dp), color = LobbyBrand.Chip) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            if (info) {
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Rounded.HelpOutline, sh("Bu ne?", "What is this?"), tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun LobbyBigButton(icon: ImageVector, label: String, color: Color, edge: Color, modifier: Modifier,
    badge: Int = 0, onClick: () -> Unit) {
    Box(modifier) {
        Box(Modifier.fillMaxWidth().height(66.dp).clip(RoundedCornerShape(14.dp)).background(edge)
            .padding(bottom = 5.dp).clip(RoundedCornerShape(14.dp)).background(color)
            .clickable(onClickLabel = label, onClick = onClick), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(6.dp))
                Text(label, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
        if (badge > 0) Surface(shape = CircleShape, color = Color(0xFFD83B35), border = BorderStroke(2.dp, Color.White),
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-8).dp)) {
            Text(if (badge > 9) "9+" else "$badge", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp))
        }
    }
}

/** Bottom bar with a raised centre "Oyna" button (Mağaza · Taht · Oyna · Oyunlar · Profil). */
@Composable
internal fun LobbyBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        Icons.Rounded.ShoppingCart to sh("Mağaza", "Store"),
        Icons.Rounded.EmojiEvents to sh("Taht", "Throne"),
        Icons.Rounded.PlayArrow to sh("Oyna", "Play"),
        Icons.Rounded.SportsEsports to sh("Oyunlar", "Games"),
        Icons.Rounded.Person to sh("Profil", "Profile"),
    )
    Box(Modifier.fillMaxWidth().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(top = 14.dp).height(66.dp).background(LobbyBrand.NavBar),
            verticalAlignment = Alignment.CenterVertically) {
            items.forEachIndexed { index, (icon, label) ->
                Column(Modifier.weight(1f).fillMaxHeight().clickable(onClickLabel = label) { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    if (index == 2) {
                        Spacer(Modifier.height(30.dp))
                        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(icon, label, tint = if (selected == index) LobbyBrand.Gold else Color.White, modifier = Modifier.size(30.dp))
                        if (selected == index) Text(label, color = LobbyBrand.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        // Raised centre button.
        Box(Modifier.align(Alignment.TopCenter).size(62.dp).clip(CircleShape)
            .background(if (selected == 2) LobbyBrand.Gold else LobbyBrand.Play)
            .border(4.dp, LobbyBrand.NavBar, CircleShape)
            .clickable(onClickLabel = items[2].second) { onSelect(2) }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.PlayArrow, items[2].second, tint = Color.White, modifier = Modifier.size(36.dp))
        }
    }
}

private fun lobbyGrouped(value: Int): String = String.format(java.util.Locale("tr", "TR"), "%,d", value)

private fun lobbyLeagueName(value: String): String = when (value) {
    "BRONZ" -> sh("Bronz", "Bronze")
    "GÜMÜŞ" -> sh("Gümüş", "Silver")
    "ALTIN" -> sh("Altın", "Gold")
    "PLATİN" -> sh("Platin", "Platinum")
    "ELMAS" -> sh("Elmas", "Diamond")
    "EFSANE" -> sh("Efsane", "Legend")
    else -> value
}
