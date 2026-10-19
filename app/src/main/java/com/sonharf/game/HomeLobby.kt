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
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.launch

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
 * (Yeni Oyun / Oyunlarım) and twin cards for Kelime Atölyesi and Son Harf. Game lists live in Oyunlarım, not here;
 * the bottom bar (Mağaza, Taht, Oyna, Arkadaşlar, Profil) is drawn by the shell.
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
    onLastLetter: () -> Unit,
    gameLanguage: String,
    onGameLanguage: (String) -> Unit,
) {
    val me = backend.currentUserId()
    var profile by remember { mutableStateOf<ProfileDto?>(OwnProfile.snapshot()) }
    var waitingForMe by remember { mutableIntStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    var leagueInfo by remember { mutableStateOf(false) }
    var guide by remember { mutableStateOf<LobbyGuideTopic?>(null) }
    var adminOpen by remember { mutableStateOf(false) }
    // The admin list loads once; the menu entry appears only for admins.
    LaunchedEffect(Unit) { AdminRoster.refresh() }
    val foreground = rememberAppForeground()
    // Eve (earned on the throne podium, automatic for admins) replaces the floating mascot on
    // the home screen while she is switched on: never two mascots at once.
    val lobbyContext = LocalContext.current
    remember { EveOwnership.restore(lobbyContext) }
    LaunchedEffect(me, foreground) { if (foreground) EveOwnership.refresh(lobbyContext, me) }
    val eveHere = EveOwnership.showOnHome(me)
    val workshopStatus = if (GameAvailability.wordWorkshop) rememberWorkshopStatus() else sh("Yakında", "Coming soon")

    LaunchedEffect(Unit) {
        if (!SupabaseProvider.configured) return@LaunchedEffect
        profile = me?.let { id -> gameRequestResult { backend.getProfile(id) }.getOrNull() }
    }
    // The frame, styles and mascots shown come from this account's server data: whatever another
    // account left on this phone is dropped first, then the real ones are loaded.
    LaunchedEffect(me) {
        if (!SupabaseProvider.configured || me.isNullOrBlank()) return@LaunchedEffect
        AccountCaches.ensureFor(lobbyContext, me)
        gameRequestResult { backend.getEquippedCosmetics() }.onSuccess { SonHarfCosmetics.applyAndPersist(lobbyContext, it) }
        runCatching { WordSiegeMascotOwnership.refresh(lobbyContext) }
    }
    // Only a badge count: open games where it is your turn plus pending invitations.
    LaunchedEffect(me, foreground) {
        if (me == null || !foreground) return@LaunchedEffect
        while (true) {
            coroutineScope {
                val classic = async { if (GameAvailability.wordSiege) gameRequestResult { backend.getWordSiegeGameSummaries("classic", finishedLimit = 0) }.getOrNull() else emptyList() }
                val series = async { if (GameAvailability.wordSiege) gameRequestResult { backend.getWordSiegeGameSummaries("series", finishedLimit = 0) }.getOrNull() else emptyList() }
                val rooms = async { gameRequestResult { backend.getLastLetterRooms() }.getOrNull() }
                val inv1 = async { if (GameAvailability.wordSiege) gameRequestResult { backend.getIncomingWordSiegeInvites().map { "si:${it.id}" } }.getOrDefault(emptyList()) else emptyList() }
                val inv2 = async { gameRequestResult { backend.getIncomingGameInvites().map { "gi:${it.id}" } }.getOrDefault(emptyList()) }
                val rows = matchListRows(classic.await().orEmpty() + series.await().orEmpty(), rooms.await().orEmpty(), me)
                // Each waiting turn is keyed by its deadline, so a new turn in the same game counts again.
                val waiting = rows.filter { !it.finished && it.turn == HomeTurn.YOURS }.map { "${it.kind}:${it.id}:${it.date}" } +
                    inv1.await() + inv2.await()
                waitingForMe = SeenBadges.report(myGamesBadgeKey(me), waiting.toSet())
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
            // Top band: menu on the left (settings live there), coins on the right.
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
                        // Only admins see this; every admin action is also checked on the server.
                        if (isAdmin(p?.id)) LobbyMenuItem(Icons.Rounded.AdminPanelSettings, sh("Yönetim Paneli", "Admin Panel")) { menuOpen = false; adminOpen = true }
                        HorizontalDivider()
                        Text(sh("REHBER", "GUIDE"), Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        lobbyGuideTopics().forEach { topic ->
                            LobbyMenuItem(topic.icon, topic.title) { menuOpen = false; guide = topic }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                // App language (menus and game words); the old game centre that held it is gone.
                Surface(onClick = { onGameLanguage(if (gameLanguage == "en") "tr" else "en") }, shape = RoundedCornerShape(50),
                    color = LobbyBrand.Chip, modifier = Modifier.padding(end = 8.dp)) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Translate, sh("Dil", "Language"), tint = LobbyBrand.Gold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(if (gameLanguage == "en") "EN" else "TR", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                }
                Surface(onClick = onShop, shape = RoundedCornerShape(50), color = LobbyBrand.Chip) {
                    Row(Modifier.padding(start = 6.dp, end = 12.dp, top = 5.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        HfCoin(24.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(p?.diamonds?.let { lobbyGrouped(it) } ?: "—", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Rounded.AddCircle, sh("Altın al", "Get gold"), tint = LobbyBrand.Gold, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Brand stage.
            Box(Modifier.fillMaxWidth().height(150.dp).padding(horizontal = 32.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                Image(rememberArtPainter(R.drawable.kelime_tahti_brand_logo), sh("Kelime Tahtı", "Word Throne"),
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(p?.displayName?.ifBlank { null } ?: sh("Oyuncu", "Player"), Modifier.weight(1f, fill = false), color = Color.White,
                                fontSize = 18.sp, style = premiumNameStyle(SonHarfCosmetics.nameStyleId),
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            AdminBadge(p?.id, 17.dp)
                        }
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

            // A guest's progress lives on this phone only: ask them, right here, to keep it with Google.
            HomeSecureAccountBanner(me)

            // Son Harf is the main game: its two 3D tiles (new match, my games) lead the lobby.
            Text(sh("Son Harf", "Last Letter").uppercase(), Modifier.fillMaxWidth().padding(top = 14.dp),
                color = LobbyBrand.Gold, fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp,
                textAlign = TextAlign.Center)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LobbyBigButton(Icons.Rounded.AddCircle, sh("Yeni Oyun", "New Game"), LobbyBrand.Play, LobbyBrand.PlayEdge,
                    Modifier.weight(1f), onClick = onLastLetter)
                LobbyBigButton(Icons.Rounded.GridView, sh("Oyunlarım", "My Games"), LobbyBrand.Games, LobbyBrand.GamesEdge,
                    Modifier.weight(1f), badge = waitingForMe, onClick = {
                        // Opening My Games counts everything in it as seen: the badge clears at once.
                        me?.let { SeenBadges.markSeen(myGamesBadgeKey(it)) }
                        waitingForMe = 0
                        onMyGames()
                    })
            }

            // The other games as cards of equal size: Kelime Kuşatması (second game), then Kelime Atölyesi.
            Text(sh("Diğer Oyunlar", "More Games").uppercase(), Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
                color = Color.White.copy(alpha = .75f), fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center)
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LobbyGameCard(R.drawable.kelime_tahti_game_icon, sh("Kelime Kuşatması", "Word Siege"),
                    if (GameAvailability.wordSiege) sh("Kelimelerle tahtayı fethet", "Conquer the board with words") else sh("Yakında", "Coming soon"),
                    onClick = onNewGame, enabled = GameAvailability.wordSiege)
                LobbyGameCard(R.drawable.kelime_atolyesi_game_icon, sh("Kelime Atölyesi", "Word Workshop"),
                    workshopStatus, onClick = onWorkshop, enabled = GameAvailability.wordWorkshop)
            }
            // Room to scroll the last card clear of the home friend at the bottom.
            Spacer(Modifier.height(if (eveHere) 140.dp else 96.dp))
        }
        // Eve, the 3D forest friend, roams the lobby for her owners. Her 3D view is released
        // whenever the app is in the background (no GPU or battery use there).
        if (eveHere && foreground) EveHomeFriend(
            playerName = p?.displayName,
            modifier = Modifier.matchParentSize().padding(start = 2.dp, bottom = 2.dp),
            throneRank = EveOwnership.status?.currentRank ?: -1,
            tournamentLive = workshopStatus.startsWith("Turnuva açık") || workshopStatus.startsWith("Tournament live"),
        )
        // Otherwise owned mascots keep floating over the lobby on the right; they take no row.
        if (!eveHere) WordSiegeMascotCompanion(
            anchors = listOf(Offset(.88f, .92f), Offset(.88f, .62f)),
            mascotSize = 83.dp, positionKey = "home",
            moveId = null, lastMoveMine = false, playerTurn = false,
            modifier = Modifier.matchParentSize(),
            playerName = p?.displayName, playerGender = p?.gender,
            stageY = .5f, greetAloud = true,
        )
        // A brand-new player is welcomed by Obi once, with a short tour of the game.
        MascotWelcomeTour(playerName = p?.displayName, accountCreatedAt = p?.createdAt)
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
    guide?.let { LobbyGuideDialog(it) { guide = null } }
    if (adminOpen) AdminPanelDialog { adminOpen = false }
}

/** Shown to guests only: one tap links Google to this same account, so nothing earned is lost. */
@Composable
private fun HomeSecureAccountBanner(userId: String?) {
    if (!PlayerAccount.googleAvailable) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var guest by remember(userId) { mutableStateOf(PlayerAccount.isGuest()) }
    var linking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    if (!guest && message == null) return
    Surface(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp), shape = RoundedCornerShape(18.dp),
        color = LobbyBrand.Band, border = BorderStroke(1.5.dp, LobbyBrand.Gold)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.Shield, null, tint = LobbyBrand.Gold, modifier = Modifier.size(30.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (guest) sh("Hesabını güvenceye al", "Secure your account") else sh("Hesabın güvende", "Your account is safe"),
                        color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    Text(message ?: sh("Altınların, puanın ve ürünlerin kaybolmasın. Telefon değişse de devam et.",
                        "Keep your gold, rating and items, even on a new phone."),
                        color = Color.White.copy(alpha = .8f), fontSize = 12.sp)
                }
            }
            if (guest) Button(
                onClick = {
                    if (linking) return@Button
                    linking = true
                    scope.launch {
                        runCatching { PlayerAccount.linkGoogle(context) }
                            .onSuccess {
                                guest = PlayerAccount.isGuest()
                                message = sh("Google hesabına bağlandı. İlerlemen artık güvende.", "Linked to Google. Your progress is safe.")
                            }
                            .onFailure { error -> PlayerAccount.friendlyError(error.toString()).takeIf { it.isNotBlank() }?.let { message = it } }
                        linking = false
                    }
                },
                enabled = !linking,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LobbyBrand.Gold, contentColor = LobbyBrand.Band),
            ) { Text(if (linking) sh("BAĞLANIYOR…", "LINKING…") else sh("GOOGLE İLE GÜVENCEYE AL", "SECURE WITH GOOGLE"), fontWeight = FontWeight.Black) }
        }
    }
}

/** A game row: icon on the left, name and one live line in the middle, an arrow on the right. */
@Composable
private fun LobbyGameCard(icon: Int, title: String, detail: String, onClick: () -> Unit, enabled: Boolean = true) {
    Surface(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().height(76.dp), shape = RoundedCornerShape(18.dp),
        color = LobbyBrand.Band, border = BorderStroke(1.dp, LobbyBrand.Grid)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Image(painterResource(icon), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Text(detail, color = LobbyBrand.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(if (enabled) Icons.Rounded.ChevronRight else Icons.Rounded.Schedule, null,
                tint = if (enabled) Color.White.copy(alpha = .8f) else LobbyBrand.Gold, modifier = Modifier.size(26.dp))
        }
    }
}

/** Seen-memory key for the My Games badge, per player. */
internal fun myGamesBadgeKey(userId: String) = "my_games:$userId"

/** A faint word-board grid: our own backdrop, the board the game is played on. */
@Composable
internal fun LobbyBoardPattern(modifier: Modifier) {
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
        Box(Modifier.fillMaxWidth().height(84.dp).clip(RoundedCornerShape(18.dp)).background(edge)
            .padding(bottom = 5.dp).clip(RoundedCornerShape(18.dp))
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(color.copy(alpha = .85f).compositeOver(Color.White), color, color)))
            .clickable(onClickLabel = label, onClick = onClick), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
                Text(label, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1)
            }
        }
        CornerNotificationBadge(badge)
    }
}

/** Flat five-tab bar (Mağaza · Taht · Oyna · Arkadaşlar · Profil); Oyna is a green letter-tile. */
@Composable
internal fun LobbyBottomBar(selected: Int, badges: Map<Int, Int> = emptyMap(), onSelect: (Int) -> Unit) {
    val items = listOf(
        Icons.Rounded.ShoppingCart to sh("Mağaza", "Store"),
        Icons.Rounded.EmojiEvents to sh("Taht", "Throne"),
        Icons.Rounded.PlayArrow to sh("Oyna", "Play"),
        Icons.Rounded.Groups to sh("Arkadaşlar", "Friends"),
        Icons.Rounded.Person to sh("Profil", "Profile"),
    )
    Row(Modifier.fillMaxWidth().background(LobbyBrand.NavBar).navigationBarsPadding().height(72.dp),
        verticalAlignment = Alignment.CenterVertically) {
        items.forEachIndexed { index, (icon, label) ->
            val active = selected == index
            Column(Modifier.weight(1f).fillMaxHeight().clickable(onClickLabel = label) { onSelect(index) },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                // The badge sits outside the clipped icon plate, so it is never cut off.
                Box {
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
                    CornerNotificationBadge(badges[index] ?: 0, small = true)
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
