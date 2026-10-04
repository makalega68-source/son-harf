package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A pending game invitation shown on the home list. */
internal data class HomeInvite(val id: String, val kind: String, val senderId: String)

/** Home sections, Kelimelik-style: what needs you first, then what waits for the rival, then results. */
internal data class HomeLobbySections(
    val yourTurn: List<MatchListRow>,
    val theirTurn: List<MatchListRow>,
    val finished: List<MatchListRow>,
)

internal fun homeLobbySections(rows: List<MatchListRow>, finishedShown: Int = 5): HomeLobbySections =
    HomeLobbySections(
        yourTurn = rows.filter { !it.finished && it.turn == HomeTurn.YOURS },
        theirTurn = rows.filter { !it.finished && it.turn != HomeTurn.YOURS },
        finished = rows.filter { it.finished }.sortedByDescending { it.date }.take(finishedShown),
    )

/**
 * The whole home in one calm page: top bar, one big NEW GAME button, four equal shortcuts and the
 * game lists. No bottom bar, no nested menus; every other page is one tap away and returns here.
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
    onFriends: () -> Unit,
    onThrone: () -> Unit,
    onEvents: () -> Unit,
    onLastLetter: () -> Unit,
    onWorkshop: () -> Unit,
    onOpen: (kind: String, id: String) -> Unit,
    onAllGames: () -> Unit,
) {
    val me = backend.currentUserId()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var games by remember { mutableStateOf<List<WordSiegeGameDto>>(emptyList()) }
    var rooms by remember { mutableStateOf<List<GameRoomDto>>(emptyList()) }
    var invites by remember { mutableStateOf<List<HomeInvite>>(emptyList()) }
    var profiles by remember { mutableStateOf<Map<String, ProfileDto>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val foreground = rememberAppForeground()

    LaunchedEffect(Unit) {
        if (!SupabaseProvider.configured) return@LaunchedEffect
        profile = me?.let { id -> gameRequestResult { backend.getProfile(id) }.getOrNull() }
    }
    LaunchedEffect(me, foreground, retry) {
        if (me == null) { loading = false; return@LaunchedEffect }
        if (!foreground) return@LaunchedEffect
        while (true) {
            coroutineScope {
                // Light list rows only: boards, bags and racks load when a game is opened.
                val classic = async { gameRequestResult { backend.getWordSiegeGameSummaries("classic", finishedLimit = 10) } }
                val series = async { gameRequestResult { backend.getWordSiegeGameSummaries("series", finishedLimit = 10) } }
                val last = async { gameRequestResult { backend.getLastLetterRooms() } }
                val siegeInvites = async { gameRequestResult { backend.getIncomingWordSiegeInvites() } }
                val seriesInvites = async { gameRequestResult { backend.getIncomingWordSiegeSeriesInvites() } }
                val lastInvites = async { gameRequestResult { backend.getIncomingGameInvites() } }
                val a = classic.await(); val b = series.await(); val c = last.await()
                games = a.getOrElse { games.filter { it.gameMode != "series" } } + b.getOrElse { games.filter { it.gameMode == "series" } }
                c.onSuccess { rooms = it }
                val i1 = siegeInvites.await(); val i2 = seriesInvites.await(); val i3 = lastInvites.await()
                if (i1.isSuccess && i2.isSuccess && i3.isSuccess) {
                    invites = i1.getOrThrow().map { HomeInvite(it.id, "siege", it.senderId) } +
                        i2.getOrThrow().map { HomeInvite(it.id, "series", it.senderId) } +
                        i3.getOrThrow().map { HomeInvite(it.id, "son_harf", it.senderId) }
                }
                failed = a.isFailure || b.isFailure || c.isFailure
                loading = false
                val ids = (matchListRows(games, rooms, me).mapNotNull { it.rivalId } + invites.map { it.senderId })
                    .distinct().filterNot(profiles::containsKey)
                if (ids.isNotEmpty()) profiles = profiles + backend.getProfilesParallel(ids).associateBy { it.id }
            }
            delay(15_000)
        }
    }

    fun respond(invite: HomeInvite, accept: Boolean) {
        if (busy != null) return
        busy = invite.id
        scope.launch {
            gameRequestResult {
                when (invite.kind) {
                    "siege" -> backend.respondWordSiegeInvite(invite.id, accept)?.id
                    "series" -> backend.respondWordSiegeSeriesInvite(invite.id, accept)?.id
                    else -> backend.respondGameInvite(invite.id, accept)?.id
                }
            }.onSuccess { openedId ->
                invites = invites.filterNot { it.id == invite.id && it.kind == invite.kind }
                if (accept && openedId != null) onOpen(invite.kind, openedId)
            }.onFailure { notice = sh("Davet yanıtlanamadı. Tekrar dene.", "Could not answer the invitation. Try again.") }
            busy = null
        }
    }

    val sections = homeLobbySections(matchListRows(games, rooms, me))
    Box(Modifier.fillMaxSize().background(LobbyPalette.Ground), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 520.dp).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "top") { PremiumHomeCommandDeck(profile, onProfile, onShop, onPro, onSettings) }
            item(key = "new_game") { HomeNewGameButton(onNewGame) }
            item(key = "shortcuts") {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeShortcut(Icons.Rounded.Groups, sh("Arkadaşlar", "Friends"), Modifier.weight(1f), locked = !isPro, onClick = onFriends)
                    HomeShortcut(Icons.Rounded.EmojiEvents, sh("Taht", "Throne"), Modifier.weight(1f), onClick = onThrone)
                    HomeShortcut(Icons.Rounded.Event, sh("Etkinlik", "Events"), Modifier.weight(1f), onClick = onEvents)
                    HomeShortcut(Icons.Rounded.WorkspacePremium, "PRO", Modifier.weight(1f), gold = true, onClick = onPro)
                }
            }
            if (loading) item(key = "loading") { LinearProgressIndicator(Modifier.fillMaxWidth(), color = LobbyPalette.Accent, trackColor = LobbyPalette.Line) }
            if (failed) item(key = "failed") {
                TextButton(onClick = { retry++ }, modifier = Modifier.fillMaxWidth()) {
                    Text(sh("Oyunlar yenilenemedi · Tekrar dene", "Games unavailable · Retry"), color = Hf.Red)
                }
            }
            notice?.let { item(key = "notice") { Text(it, color = Hf.Red, fontSize = 13.sp) } }

            if (invites.isNotEmpty()) {
                homeSection("invites", sh("DAVETLER", "INVITATIONS"), invites.size)
                items(invites, key = { "invite:${it.kind}:${it.id}" }) { invite ->
                    HomeInviteRow(invite, profiles[invite.senderId], busy == null, { respond(invite, true) }, { respond(invite, false) })
                }
            }
            homeSection("yours", sh("SIRA SENDE", "YOUR TURN"), sections.yourTurn.size)
            if (sections.yourTurn.isEmpty() && !loading) item(key = "yours_empty") { HomeEmptyLine(sh("Şu an sıra sende olan oyun yok.", "No game is waiting for you.")) }
            items(sections.yourTurn, key = { "y:${it.kind}:${it.id}" }) { row -> HomeMatchCard(row, profiles[row.rivalId]) { onOpen(row.kind, row.id) } }

            homeSection("theirs", sh("SIRA RAKİPTE", "THEIR TURN"), sections.theirTurn.size)
            if (sections.theirTurn.isEmpty() && !loading) item(key = "theirs_empty") { HomeEmptyLine(sh("Rakip bekleyen oyun yok.", "No game is waiting for a rival.")) }
            items(sections.theirTurn, key = { "t:${it.kind}:${it.id}" }) { row -> HomeMatchCard(row, profiles[row.rivalId]) { onOpen(row.kind, row.id) } }

            if (sections.finished.isNotEmpty()) {
                item(key = "finished_header") {
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(sh("BİTEN OYUNLAR", "FINISHED GAMES"), color = LobbyPalette.Muted, fontSize = 13.sp,
                            fontWeight = FontWeight.Black, letterSpacing = .6.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = onAllGames) { Text(sh("Tümü", "All"), color = LobbyPalette.Accent, fontWeight = FontWeight.Bold) }
                    }
                }
                items(sections.finished, key = { "f:${it.kind}:${it.id}" }) { row -> HomeMatchCard(row, profiles[row.rivalId]) { onOpen(row.kind, row.id) } }
            }

            homeSection("other_games", sh("DİĞER OYUNLAR", "OTHER GAMES"), null)
            item(key = "other_games_row") {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeGameTile(R.drawable.son_harf_game_icon, sh("Son Harf", "Last Letter"), Modifier.weight(1f), onLastLetter)
                    HomeGameTile(R.drawable.kelime_atolyesi_game_icon, sh("Kelime Atölyesi", "Word Workshop"), Modifier.weight(1f), onWorkshop)
                }
            }
            // Room at the end so the floating mascot never covers the last row.
            item(key = "mascot_room") { Spacer(Modifier.height(72.dp)) }
        }
        // Owned mascots keep floating over the home, exactly as before; they take no row.
        WordSiegeMascotCompanion(
            anchors = listOf(androidx.compose.ui.geometry.Offset(.88f, .92f), androidx.compose.ui.geometry.Offset(.12f, .92f)),
            mascotSize = 83.dp, positionKey = "home",
            moveId = null, lastMoveMine = false, playerTurn = false,
            modifier = Modifier.matchParentSize(),
            playerName = profile?.displayName, playerGender = profile?.gender,
            stageY = .5f,
        )
    }
}

private fun LazyListScope.homeSection(key: String, title: String, count: Int?) {
    item(key = "section:$key") {
        Text(
            if (count != null && count > 0) "$title ($count)" else title,
            color = LobbyPalette.Muted, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = .6.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
        )
    }
}

@Composable
private fun HomeNewGameButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(64.dp).sonHarfPressScale(pressedScale = .985f),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E8B45), contentColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
    ) {
        Icon(Icons.Rounded.Add, null, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(10.dp))
        Text(sh("YENİ OYUN", "NEW GAME"), fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
    }
}

@Composable
private fun HomeShortcut(icon: ImageVector, label: String, modifier: Modifier, locked: Boolean = false,
    gold: Boolean = false, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.fillMaxHeight(), shape = RoundedCornerShape(16.dp),
        color = LobbyPalette.Paper, border = BorderStroke(1.dp, if (gold) LobbyPalette.Gold.copy(alpha = .6f) else LobbyPalette.Line)) {
        Box {
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, null, tint = if (gold) LobbyPalette.Gold else LobbyPalette.Accent, modifier = Modifier.size(28.dp))
                Text(label, color = LobbyPalette.Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
            // Friends is a PRO feature: a small lock, never a hidden button.
            if (locked) Icon(Icons.Rounded.Lock, sh("PRO", "PRO"), tint = LobbyPalette.Gold,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(14.dp))
        }
    }
}

@Composable
private fun HomeMatchCard(row: MatchListRow, rival: ProfileDto?, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = LobbyPalette.Paper, border = BorderStroke(1.dp, LobbyPalette.Line)) {
        CompactMatchRow(row, rival, onClick)
    }
}

@Composable
private fun HomeInviteRow(invite: HomeInvite, sender: ProfileDto?, enabled: Boolean, onAccept: () -> Unit, onDecline: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = LobbyPalette.Soft, border = BorderStroke(1.dp, LobbyPalette.Accent.copy(alpha = .35f))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FramedProfilePhotoAvatar(avatarPath = sender?.avatarPath, gender = sender?.gender,
                name = sender?.displayName ?: sh("Oyuncu", "Player"), size = 48.dp, frameId = rememberPlayerFrame(sender?.id),
                visible = sender?.avatarVisibility != "hidden", isPro = sender?.isVip == true)
            Column(Modifier.weight(1f)) {
                Text(sender?.displayName ?: sh("Oyuncu", "Player"), color = LobbyPalette.Ink, fontWeight = FontWeight.Bold,
                    fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(when (invite.kind) { "son_harf" -> "Son Harf"; "series" -> sh("Hızlı Düello", "Quick Duel"); else -> sh("Kelime Tahtı", "Word Throne") },
                    color = LobbyPalette.Muted, fontSize = 12.sp)
            }
            IconButton(onClick = onDecline, enabled = enabled, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Rounded.Close, sh("Reddet", "Decline"), tint = Hf.Red)
            }
            FilledIconButton(onClick = onAccept, enabled = enabled, modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF2E8B45), contentColor = Color.White)) {
                Icon(Icons.Rounded.Check, sh("Kabul et", "Accept"))
            }
        }
    }
}

@Composable
private fun HomeEmptyLine(text: String) {
    Text(text, color = LobbyPalette.Muted, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
}

@Composable
private fun HomeGameTile(art: Int, title: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.fillMaxHeight(), shape = RoundedCornerShape(16.dp),
        color = LobbyPalette.Paper, border = BorderStroke(1.dp, LobbyPalette.Line)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HfGameArt(art, 44.dp, 44.dp, description = null)
            Text(title, color = LobbyPalette.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
    }
}
