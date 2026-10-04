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
import androidx.compose.ui.graphics.compositeOver
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

/** Lobby colours: deep petrol board behind the logo; the action tiles reuse its green and red. */
internal object LobbyBrand {
    val Sky = Color(0xFF14596A)
    val Grid = Color(0xFF2A7486)
    val Band = Color(0xFF0D3C48)
    val NavBar = Color(0xFF0B323C)
    val Chip = Color(0xFF1F7489)
    val Play = Color(0xFF2E8B45)      // the logo's green tiles
    val PlayEdge = Color(0xFF1D5E2D)
    val Games = Color(0xFFC9372C)     // the logo's red tiles
    val GamesEdge = Color(0xFF8A2119)
    val Gold = Color(0xFFF2C14E)
}

/**
 * Lobby: brand stage on a word-board backdrop, a player card, two big letter-tile buttons
 * (Yeni Oyun / Oyunlarım) and one event card. Game lists live in Oyunlarım, not here;
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
        LobbyBoardPattern(Modifier.matchParentSize())
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

            // Player card: photo, name with win rate and rating, and the league badge with its "?".
            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(20.dp),
                color = LobbyBrand.Band, border = BorderStroke(1.5.dp, LobbyBrand.Gold.copy(alpha = .7f))) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.clip(CircleShape).clickable(onClickLabel = sh("Profili aç", "Open profile"), onClick = onProfile)) {
                        FramedProfilePhotoAvatar(avatarPath = p?.avatarPath, gender = p?.gender,
                            name = p?.displayName ?: sh("Oyuncu", "Player"), size = 76.dp,
                            frameId = rememberPlayerFrame(p?.id), accent = LobbyBrand.Gold,
                            visible = p?.avatarVisibility != "hidden", isPro = p?.isVip == true)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(p?.displayName?.ifBlank { null } ?: sh("Oyuncu", "Player"), color = Color.White,
                            fontSize = 18.sp, style = premiumNameStyle(SonHarfCosmetics.nameStyleId),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            LobbyPill(sh("Başarı ", "Win ") + "%$winRate")
                            LobbyPill("${p?.rating ?: 1000} RP", gold = true)
                        }
                    }
                    Surface(onClick = { leagueInfo = true }, shape = RoundedCornerShape(14.dp), color = LobbyBrand.Chip) {
                        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.EmojiEvents, null, tint = LobbyBrand.Gold, modifier = Modifier.size(26.dp))
                            Text(lobbyLeagueName(league.leagueName), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Icon(Icons.Rounded.HelpOutline, sh("Lig nedir?", "What is the league?"), tint = Color.White.copy(alpha = .8f),
                                modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Two big 3D tiles in the logo's green and red.
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LobbyBigButton(Icons.Rounded.AddCircle, sh("Yeni Oyun", "New Game"), LobbyBrand.Play, LobbyBrand.PlayEdge,
                    Modifier.weight(1f), onClick = onNewGame)
                LobbyBigButton(Icons.Rounded.GridView, sh("Oyunlarım", "My Games"), LobbyBrand.Games, LobbyBrand.GamesEdge,
                    Modifier.weight(1f), badge = waitingForMe, onClick = onMyGames)
            }

            // One event card.
            Box(Modifier.padding(horizontal = 16.dp)) { HomeTournamentCard(onOpen = onWorkshop) }
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

/** A faint word-board grid: our own backdrop, the board the game is played on. */
@Composable
private fun LobbyBoardPattern(modifier: Modifier) {
    Canvas(modifier) {
        val cell = 44.dp.toPx()
        val line = LobbyBrand.Grid.copy(alpha = .35f)
        var x = 0f
        while (x < size.width) { drawLine(line, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx()); x += cell }
        var y = 0f
        while (y < size.height) { drawLine(line, Offset(0f, y), Offset(size.width, y), 1.dp.toPx()); y += cell }
    }
}

@Composable
private fun LobbyMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(label, fontSize = 16.sp) }, onClick = onClick, leadingIcon = { Icon(icon, null) })
}

@Composable
private fun LobbyPill(text: String, info: Boolean = false, gold: Boolean = false, onClick: (() -> Unit)? = null) {
    Surface(onClick = onClick ?: {}, enabled = onClick != null, shape = RoundedCornerShape(10.dp), color = LobbyBrand.Chip) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = if (gold) LobbyBrand.Gold else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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
        // A letter-tile: darker base underneath gives the 3D edge, a soft highlight on top.
        Box(Modifier.fillMaxWidth().height(118.dp).clip(RoundedCornerShape(22.dp)).background(edge)
            .padding(bottom = 7.dp).clip(RoundedCornerShape(22.dp))
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(color.copy(alpha = .85f).compositeOver(Color.White), color, color)))
            .clickable(onClickLabel = label, onClick = onClick), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(40.dp))
                Text(label, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 1)
            }
        }
        if (badge > 0) Surface(shape = CircleShape, color = LobbyBrand.Gold, border = BorderStroke(2.dp, Color.White),
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-6).dp)) {
            Text(if (badge > 9) "9+" else "$badge", color = Color(0xFF3A2A00), fontSize = 13.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
        }
    }
}

/** Flat five-tab bar (Mağaza · Taht · Oyna · Oyunlar · Profil); Oyna is a green letter-tile. */
@Composable
internal fun LobbyBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        Icons.Rounded.ShoppingCart to sh("Mağaza", "Store"),
        Icons.Rounded.EmojiEvents to sh("Taht", "Throne"),
        Icons.Rounded.PlayArrow to sh("Oyna", "Play"),
        Icons.Rounded.SportsEsports to sh("Oyunlar", "Games"),
        Icons.Rounded.Person to sh("Profil", "Profile"),
    )
    Row(Modifier.fillMaxWidth().background(LobbyBrand.NavBar).navigationBarsPadding().height(72.dp),
        verticalAlignment = Alignment.CenterVertically) {
        items.forEachIndexed { index, (icon, label) ->
            val active = selected == index
            Column(Modifier.weight(1f).fillMaxHeight().clickable(onClickLabel = label) { onSelect(index) },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(width = if (index == 2) 46.dp else 52.dp, height = if (index == 2) 40.dp else 30.dp)
                    .clip(RoundedCornerShape(if (index == 2) 12.dp else 15.dp))
                    .background(when {
                        index == 2 -> LobbyBrand.Play
                        active -> LobbyBrand.Gold.copy(alpha = .22f)
                        else -> Color.Transparent
                    }), contentAlignment = Alignment.Center) {
                    Icon(icon, label, tint = if (active && index != 2) LobbyBrand.Gold else Color.White,
                        modifier = Modifier.size(if (index == 2) 30.dp else 26.dp))
                }
                Spacer(Modifier.height(3.dp))
                Text(label, color = if (active) LobbyBrand.Gold else Color.White.copy(alpha = .85f), fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
            }
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
