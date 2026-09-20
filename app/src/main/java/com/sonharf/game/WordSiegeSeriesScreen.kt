package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val SERIES_DEFAULT_TURN_MINUTES = 5

@Composable
internal fun WordSiegeSeriesScreen(verifiedAccess: Boolean = false, onExit: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    val me = remember { backend.currentUserId() }
    var entitlement by remember { mutableStateOf<VipEntitlementsDto?>(if (verifiedAccess) VipEntitlementsDto(seriesGameAccess = true) else null) }
    var games by remember { mutableStateOf<List<WordSiegeGameDto>>(emptyList()) }
    var friends by remember { mutableStateOf<List<Pair<FriendshipDto, ProfileDto>>>(emptyList()) }
    var invites by remember { mutableStateOf<List<WordSiegeSeriesInviteDto>>(emptyList()) }
    var profiles by remember { mutableStateOf<Map<String, ProfileDto>>(emptyMap()) }
    var selectedGameId by remember { mutableStateOf<String?>(null) }
    var currentGame by remember { mutableStateOf<WordSiegeGameDto?>(null) }
    var moves by remember { mutableStateOf<List<WordSiegeMoveDto>>(emptyList()) }
    var messages by remember { mutableStateOf<List<WordSiegeMessageDto>>(emptyList()) }
    var placements by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var selectedRackIndex by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var notice by remember { mutableStateOf<String?>(null) }
    var showChat by remember { mutableStateOf(false) }
    var chatInput by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var showForfeit by remember { mutableStateOf(false) }
    var showExchange by remember { mutableStateOf(false) }
    var exchangeSelection by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var inviteFriend by remember { mutableStateOf(false) }
    var turnMinutes by remember { mutableIntStateOf(SERIES_DEFAULT_TURN_MINUTES) }
    var clockTick by remember { mutableLongStateOf(0L) }

    suspend fun loadProfiles(ids: Collection<String?>) {
        val missing = ids.filterNotNull().distinct().filterNot(profiles::containsKey)
        if (missing.isEmpty()) return
        val loaded = missing.mapNotNull { id ->
            runCatching { backend.getProfile(id) }.getOrNull()?.let { id to it }
        }.toMap()
        if (loaded.isNotEmpty()) profiles = profiles + loaded
    }

    suspend fun refreshLobby(showProgress: Boolean = false) {
        if (showProgress) loading = true
        val nextGames = runCatching { backend.getWordSiegeSeriesGames() }
            .onFailure { notice = seriesFriendlyError(it.message.orEmpty()) }
            .getOrDefault(games)
        games = nextGames
        loadProfiles(nextGames.flatMap { listOf(it.playerOneId, it.playerTwoId) })
        friends = runCatching { backend.getFriends() }.getOrDefault(friends)
        invites = runCatching { backend.getIncomingWordSiegeSeriesInvites() }.getOrDefault(invites)
        loadProfiles(invites.map { it.senderId })
        if (showProgress) loading = false
    }

    fun applyGame(next: WordSiegeGameDto) {
        currentGame = next
        selectedGameId = next.id
        placements = emptyMap()
        selectedRackIndex = null
        exchangeSelection = emptySet()
        games = (games.filterNot { it.id == next.id } + next)
            .filterNot { it.status == "cancelled" }
            .sortedWith(seriesGameComparator(me))
    }

    fun runGameAction(action: suspend () -> WordSiegeGameDto) {
        if (busy) return
        scope.launch {
            busy = true
            runCatching { action() }
                .onSuccess { next -> applyGame(next); notice = null; refreshLobby() }
                .onFailure { notice = seriesFriendlyError(it.message.orEmpty()) }
            busy = false
        }
    }

    LaunchedEffect(Unit) {
        entitlement = runCatching { backend.getVipEntitlements() }.getOrElse { entitlement ?: VipEntitlementsDto() }
        if (entitlement?.seriesGameAccess == true) refreshLobby(showProgress = true) else loading = false
    }

    LaunchedEffect(entitlement?.seriesGameAccess, selectedGameId) {
        if (entitlement?.seriesGameAccess != true || selectedGameId != null) return@LaunchedEffect
        while (currentCoroutineContext().isActive) {
            refreshLobby()
            delay(5_000)
        }
    }

    LaunchedEffect(selectedGameId, showChat) {
        val gameId = selectedGameId ?: return@LaunchedEffect
        while (currentCoroutineContext().isActive) {
            runCatching { backend.refreshWordSiegeGame(gameId) }
                .onSuccess { next ->
                    val changed = currentGame?.moveCount != next.moveCount || currentGame?.currentPlayerId != next.currentPlayerId
                    currentGame = next
                    games = (games.filterNot { it.id == next.id } + next).sortedWith(seriesGameComparator(me))
                    loadProfiles(listOf(next.playerOneId, next.playerTwoId))
                    if (changed) {
                        placements = emptyMap()
                        selectedRackIndex = null
                    }
                }
                .onFailure { notice = seriesFriendlyError(it.message.orEmpty()) }
            moves = runCatching { backend.getWordSiegeMoves(gameId) }.getOrDefault(moves)
            if (showChat) messages = runCatching { backend.getWordSiegeMessages(gameId) }.getOrDefault(messages)
            clockTick = System.currentTimeMillis()
            delay(1_000)
        }
    }

    BackHandler {
        if (selectedGameId != null) {
            selectedGameId = null
            currentGame = null
            placements = emptyMap()
            selectedRackIndex = null
        } else onExit()
    }

    Surface(Modifier.fillMaxSize(), color = SonHarfTheme.Background) {
        when {
            entitlement == null || loading && games.isEmpty() -> SeriesLoading()
            entitlement?.seriesGameAccess != true -> SeriesLocked(onExit)
            selectedGameId == null -> SeriesLobby(
                games = games,
                friends = friends,
                invites = invites,
                profiles = profiles,
                me = me,
                busy = busy,
                notice = notice,
                turnMinutes = turnMinutes,
                onTurnMinutes = { turnMinutes = it },
                onBack = onExit,
                onRefresh = { scope.launch { refreshLobby(showProgress = true) } },
                onNewGame = {
                    if (busy) return@SeriesLobby
                    scope.launch {
                        busy = true
                        runCatching { backend.findOrCreateWordSiegeSeriesGame(SonHarfUiState.language, turnMinutes) }
                            .onSuccess { next ->
                                applyGame(next)
                                notice = if (next.status == "waiting") sh("Seri rakibi aranıyor.", "Searching for a Series rival.") else null
                            }
                            .onFailure { notice = seriesFriendlyError(it.message.orEmpty()) }
                        busy = false
                    }
                },
                onOpen = { game -> applyGame(game) },
                onInviteFriend = { inviteFriend = true },
                onAcceptInvite = { invite ->
                    if (busy) return@SeriesLobby
                    scope.launch {
                        busy = true
                        runCatching { backend.respondWordSiegeSeriesInvite(invite.id, true) }
                            .onSuccess { next ->
                                invites = invites.filterNot { it.id == invite.id }
                                if (next != null) applyGame(next)
                            }
                            .onFailure { notice = seriesFriendlyError(it.message.orEmpty()) }
                        busy = false
                    }
                },
                onDeclineInvite = { invite ->
                    if (busy) return@SeriesLobby
                    scope.launch {
                        busy = true
                        runCatching { backend.respondWordSiegeSeriesInvite(invite.id, false) }
                            .onSuccess { invites = invites.filterNot { it.id == invite.id } }
                            .onFailure { notice = seriesFriendlyError(it.message.orEmpty()) }
                        busy = false
                    }
                },
            )
            else -> {
                val game = currentGame
                if (game == null) {
                    SeriesLoading()
                } else if (game.status == "waiting") {
                    SeriesWaiting(
                        game = game,
                        onBack = { selectedGameId = null; currentGame = null },
                        onCancel = {
                            runGameAction { backend.cancelWordSiegeWaiting(game.id) }
                            selectedGameId = null
                            currentGame = null
                        },
                    )
                } else {
                    val deadlineText = seriesDeadlineText(game.turnDeadline, clockTick)
                    val myMisses = if (me == game.playerOneId) game.playerOneMissedTurns else game.playerTwoMissedTurns
                    Box(Modifier.fillMaxSize()) {
                        WordSiegePanMatch(
                            game = game,
                            me = me,
                            profiles = profiles,
                            moves = moves,
                            placements = placements,
                            selectedRackIndex = selectedRackIndex,
                            busy = busy,
                            notice = notice,
                            onBack = { selectedGameId = null; currentGame = null },
                            onBoardCell = { boardIndex ->
                                if (game.status != "playing" || game.currentPlayerId != me || busy) return@WordSiegePanMatch
                                if (placements.containsKey(boardIndex)) {
                                    val rackIndex = placements.getValue(boardIndex)
                                    placements = placements - boardIndex
                                    selectedRackIndex = rackIndex
                                } else if (game.board.getOrNull(boardIndex)?.letter == null) {
                                    val rackIndex = selectedRackIndex ?: return@WordSiegePanMatch
                                    if (rackIndex !in placements.values) {
                                        placements = placements + (boardIndex to rackIndex)
                                        selectedRackIndex = null
                                    }
                                }
                            },
                            onRackTile = { rackIndex ->
                                val pendingCell = placements.entries.firstOrNull { it.value == rackIndex }?.key
                                if (pendingCell != null) placements = placements - pendingCell
                                selectedRackIndex = if (selectedRackIndex == rackIndex) null else rackIndex
                            },
                            onSubmit = {
                                if (placements.isEmpty()) {
                                    notice = sh("Önce harf yerleştir.", "Place at least one tile.")
                                } else {
                                    val orientation = runCatching { WordSiegeFinalRules.detectOrientation(game.board, placements.keys) }
                                    if (orientation.isFailure) {
                                        notice = seriesFriendlyError(orientation.exceptionOrNull()?.message.orEmpty())
                                    } else {
                                        runGameAction {
                                            backend.submitWordSiegeMove(
                                                game.id,
                                                placements.entries.sortedBy { it.key }.map { WordSiegePlacement(it.key, it.value) },
                                                orientation.getOrThrow() == WordSiegeOrientation.HORIZONTAL,
                                            )
                                        }
                                    }
                                }
                            },
                            onPass = { showPass = true },
                            onExchange = { exchangeSelection = emptySet(); showExchange = true },
                            onChat = {
                                showChat = true
                                scope.launch { messages = runCatching { backend.getWordSiegeMessages(game.id) }.getOrDefault(emptyList()) }
                            },
                            onForfeit = { showForfeit = true },
                            onCancelWaiting = {},
                        )
                        SeriesTimerPill(
                            text = deadlineText,
                            misses = myMisses,
                            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 48.dp),
                        )
                    }
                }
            }
        }
    }

    if (inviteFriend) {
        SeriesFriendInviteDialog(
            friends = friends.map { it.second },
            busy = busy,
            onDismiss = { inviteFriend = false },
            onInvite = { friend ->
                scope.launch {
                    busy = true
                    runCatching { backend.inviteFriendToWordSiegeSeries(friend.id, SonHarfUiState.language, turnMinutes) }
                        .onSuccess {
                            notice = sh("${friend.displayName} Seri Oyun'a davet edildi.", "${friend.displayName} was invited to Series Game.")
                            inviteFriend = false
                        }
                        .onFailure { notice = seriesFriendlyError(it.message.orEmpty()) }
                    busy = false
                }
            },
        )
    }

    val dialogGame = currentGame
    if (showPass && dialogGame != null) {
        AlertDialog(
            onDismissRequest = { showPass = false },
            title = { Text(sh("Turu geç?", "Pass this turn?"), fontWeight = FontWeight.Black) },
            text = { Text(sh("Bu manuel pas sayılır; kaçırılmış tur sayacını sıfırlar ve sıra rakibe geçer.", "This is a manual pass; it resets your missed-turn streak and gives the turn to your rival.")) },
            confirmButton = { TextButton(onClick = { showPass = false; runGameAction { backend.passWordSiegeTurn(dialogGame.id) } }) { Text(sh("PAS VER", "PASS")) } },
            dismissButton = { TextButton(onClick = { showPass = false }) { Text(sh("VAZGEÇ", "CANCEL")) } },
        )
    }

    if (showForfeit && dialogGame != null) {
        AlertDialog(
            onDismissRequest = { showForfeit = false },
            title = { Text(sh("Pes et?", "Forfeit?"), fontWeight = FontWeight.Black) },
            text = { Text(sh("Oyun hemen rakibin galibiyetiyle biter.", "The game ends immediately with your rival as winner.")) },
            confirmButton = { TextButton(onClick = { showForfeit = false; runGameAction { backend.forfeitWordSiegeGame(dialogGame.id) } }) { Text(sh("PES ET", "FORFEIT"), color = SonHarfTheme.Error) } },
            dismissButton = { TextButton(onClick = { showForfeit = false }) { Text(sh("VAZGEÇ", "CANCEL")) } },
        )
    }

    if (showExchange && dialogGame != null) {
        val rack = if (me == dialogGame.playerOneId) dialogGame.playerOneRack else dialogGame.playerTwoRack.orEmpty()
        AlertDialog(
            onDismissRequest = { showExchange = false },
            title = { Text(sh("Harf değiştir", "Exchange tiles"), fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(sh("Değiştireceğin harfleri seç.", "Select the tiles to exchange."))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        rack.forEachIndexed { index, letter ->
                            FilterChip(
                                selected = index in exchangeSelection,
                                onClick = { exchangeSelection = if (index in exchangeSelection) exchangeSelection - index else exchangeSelection + index },
                                label = { Text(letter.toString(), fontWeight = FontWeight.Black) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = exchangeSelection.isNotEmpty() && exchangeSelection.size <= dialogGame.bag.length && !busy,
                    onClick = {
                        val selection = exchangeSelection
                        showExchange = false
                        runGameAction { backend.exchangeWordSiegeTiles(dialogGame.id, selection) }
                    },
                ) { Text(sh("DEĞİŞTİR", "EXCHANGE")) }
            },
            dismissButton = { TextButton(onClick = { showExchange = false }) { Text(sh("VAZGEÇ", "CANCEL")) } },
        )
    }

    if (showChat && dialogGame != null) {
        AlertDialog(
            onDismissRequest = { showChat = false },
            title = { Text(sh("Oyun sohbeti", "Game chat"), fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 260.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        items(messages, key = { it.id }) { message ->
                            val mine = message.senderId == me
                            Surface(
                                modifier = Modifier.fillMaxWidth(if (mine) .90f else .94f),
                                shape = RoundedCornerShape(12.dp),
                                color = if (mine) SonHarfTheme.Primary.copy(alpha = .12f) else SonHarfTheme.SurfaceSecondary,
                            ) {
                                Text(message.body, Modifier.padding(8.dp), color = SonHarfTheme.TextPrimary, fontSize = 11.sp)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it.take(300) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text(sh("Mesaj", "Message")) },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = chatInput.isNotBlank() && !busy,
                    onClick = {
                        val text = chatInput
                        scope.launch {
                            busy = true
                            runCatching { backend.sendWordSiegeMessage(dialogGame.id, text) }
                                .onSuccess {
                                    chatInput = ""
                                    messages = runCatching { backend.getWordSiegeMessages(dialogGame.id) }.getOrDefault(messages)
                                }
                                .onFailure { notice = seriesFriendlyError(it.message.orEmpty()) }
                            busy = false
                        }
                    },
                ) { Text(sh("GÖNDER", "SEND")) }
            },
            dismissButton = { TextButton(onClick = { showChat = false }) { Text(sh("KAPAT", "CLOSE")) } },
        )
    }
}

@Composable
private fun SeriesLobby(
    games: List<WordSiegeGameDto>,
    friends: List<Pair<FriendshipDto, ProfileDto>>,
    invites: List<WordSiegeSeriesInviteDto>,
    profiles: Map<String, ProfileDto>,
    me: String?,
    busy: Boolean,
    notice: String?,
    turnMinutes: Int,
    onTurnMinutes: (Int) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onNewGame: () -> Unit,
    onOpen: (WordSiegeGameDto) -> Unit,
    onInviteFriend: () -> Unit,
    onAcceptInvite: (WordSiegeSeriesInviteDto) -> Unit,
    onDeclineInvite: (WordSiegeSeriesInviteDto) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back")) }
                Column(Modifier.weight(1f)) {
                    Text(sh("SERİ OYUN", "SERIES GAME"), color = SonHarfTheme.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(sh("Dakikalık tur • Kaçan tur otomatik pas • 3 ardışık kaçırma = mağlubiyet", "Minute turns • Missed turn auto-passes • 3 consecutive misses = defeat"), color = SonHarfTheme.TextSecondary, fontSize = 9.sp)
                }
                IconButton(onClick = onRefresh, enabled = !busy) { Icon(Icons.Rounded.Refresh, sh("Yenile", "Refresh")) }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(18.dp), color = SonHarfTheme.Surface, border = BorderStroke(1.dp, SonHarfTheme.Border)) {
                Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(sh("TUR SÜRESİ", "TURN TIME"), color = SonHarfTheme.TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf(3, 5, 10).forEach { value ->
                            FilterChip(
                                selected = turnMinutes == value,
                                onClick = { onTurnMinutes(value) },
                                label = { Text(sh("$value dk", "$value min"), fontWeight = FontWeight.Black) },
                            )
                        }
                    }
                    Button(
                        onClick = onNewGame,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(15.dp),
                    ) {
                        if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else {
                            Icon(Icons.Rounded.Bolt, null)
                            Spacer(Modifier.width(7.dp))
                            Text(sh("SERİ RAKİP BUL", "FIND SERIES RIVAL"), fontWeight = FontWeight.Black)
                        }
                    }
                    OutlinedButton(
                        onClick = onInviteFriend,
                        enabled = !busy && friends.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp),
                    ) {
                        Icon(Icons.Rounded.PersonAdd, null)
                        Spacer(Modifier.width(7.dp))
                        Text(sh("ARKADAŞ DAVET ET", "INVITE FRIEND"), fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        notice?.let { message ->
            item {
                Surface(shape = RoundedCornerShape(12.dp), color = SonHarfTheme.SurfaceSecondary) {
                    Text(message, Modifier.fillMaxWidth().padding(10.dp), color = SonHarfTheme.TextPrimary, fontSize = 10.sp)
                }
            }
        }

        if (invites.isNotEmpty()) {
            item { Text(sh("SERİ OYUN DAVETLERİ", "SERIES INVITES"), color = SonHarfTheme.PremiumGold, fontSize = 10.sp, fontWeight = FontWeight.Black) }
            items(invites, key = { it.id }) { invite ->
                val sender = profiles[invite.senderId]
                Surface(shape = RoundedCornerShape(16.dp), color = SonHarfTheme.Surface, border = BorderStroke(1.dp, SonHarfTheme.Border)) {
                    Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Bolt, null, tint = SonHarfTheme.PremiumGold)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(sender?.displayName ?: sh("Oyuncu", "Player"), color = SonHarfTheme.TextPrimary, fontWeight = FontWeight.Black, maxLines = 1)
                            Text(sh("${invite.turnDurationMinutes} dk tur", "${invite.turnDurationMinutes} min turn"), color = SonHarfTheme.TextSecondary, fontSize = 9.sp)
                        }
                        IconButton(onClick = { onDeclineInvite(invite) }, enabled = !busy) { Icon(Icons.Rounded.Close, sh("Reddet", "Decline"), tint = SonHarfTheme.Error) }
                        IconButton(onClick = { onAcceptInvite(invite) }, enabled = !busy) { Icon(Icons.Rounded.CheckCircle, sh("Kabul et", "Accept"), tint = SonHarfTheme.Success) }
                    }
                }
            }
        }

        item { Text(sh("SERİ OYUNLARIN", "YOUR SERIES GAMES"), color = SonHarfTheme.TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Black) }
        if (games.isEmpty()) {
            item {
                Surface(shape = RoundedCornerShape(18.dp), color = SonHarfTheme.Surface, border = BorderStroke(1.dp, SonHarfTheme.Border)) {
                    Text(sh("Henüz Seri Oyun yok. Rakip bul veya bir arkadaşını davet et.", "No Series Game yet. Find a rival or invite a friend."), Modifier.fillMaxWidth().padding(18.dp), color = SonHarfTheme.TextSecondary, textAlign = TextAlign.Center)
                }
            }
        }
        items(games, key = { it.id }) { game ->
            val opponentId = if (me == game.playerOneId) game.playerTwoId else game.playerOneId
            val opponent = opponentId?.let(profiles::get)
            val myTurn = game.status == "playing" && game.currentPlayerId == me
            val misses = if (me == game.playerOneId) game.playerOneMissedTurns else game.playerTwoMissedTurns
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onOpen(game) },
                shape = RoundedCornerShape(17.dp),
                color = SonHarfTheme.Surface,
                border = BorderStroke(1.dp, if (myTurn) SonHarfTheme.Primary else SonHarfTheme.Border),
            ) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = if (myTurn) SonHarfTheme.Primary.copy(alpha = .12f) else SonHarfTheme.SurfaceSecondary) {
                        Icon(if (myTurn) Icons.Rounded.Bolt else Icons.Rounded.Timer, null, tint = if (myTurn) SonHarfTheme.Primary else SonHarfTheme.TextSecondary, modifier = Modifier.padding(10.dp))
                    }
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(opponent?.displayName ?: if (game.status == "waiting") sh("Rakip aranıyor", "Finding rival") else sh("Rakip", "Rival"), color = SonHarfTheme.TextPrimary, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            when {
                                game.status == "waiting" -> sh("Eşleşme bekliyor", "Waiting for match")
                                game.status == "finished" -> sh("Tamamlandı", "Finished")
                                myTurn -> sh("Sıra sende • ${seriesDeadlineText(game.turnDeadline, System.currentTimeMillis())}", "Your turn • ${seriesDeadlineText(game.turnDeadline, System.currentTimeMillis())}")
                                else -> sh("Rakipte • ${seriesDeadlineText(game.turnDeadline, System.currentTimeMillis())}", "Rival's turn • ${seriesDeadlineText(game.turnDeadline, System.currentTimeMillis())}")
                            },
                            color = if (myTurn) SonHarfTheme.Primary else SonHarfTheme.TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (game.status == "playing") Text(sh("Kaçırılan ardışık tur: $misses / 3", "Consecutive missed turns: $misses / 3"), color = SonHarfTheme.TextSecondary, fontSize = 8.sp)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.TextSecondary)
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun SeriesWaiting(game: WordSiegeGameDto, onBack: () -> Unit, onCancel: () -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = SonHarfTheme.Primary)
        Spacer(Modifier.height(16.dp))
        Text(sh("SERİ RAKİBİ ARANIYOR", "SEARCHING FOR SERIES RIVAL"), color = SonHarfTheme.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Text(sh("${game.turnDurationMinutes ?: SERIES_DEFAULT_TURN_MINUTES} dakikalık tur havuzunda eşleşiyorsun.", "Matching in the ${game.turnDurationMinutes ?: SERIES_DEFAULT_TURN_MINUTES}-minute turn pool."), color = SonHarfTheme.TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onCancel) { Text(sh("ARAMAYI İPTAL ET", "CANCEL SEARCH")) }
        TextButton(onClick = onBack) { Text(sh("LİSTEYE DÖN", "BACK TO LIST")) }
    }
}

@Composable
private fun SeriesTimerPill(text: String, misses: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(99.dp),
        color = SonHarfTheme.Surface.copy(alpha = .97f),
        border = BorderStroke(1.dp, if (misses >= 2) SonHarfTheme.Error else SonHarfTheme.PremiumGold),
        shadowElevation = 4.dp,
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Timer, null, tint = if (misses >= 2) SonHarfTheme.Error else SonHarfTheme.PremiumGold, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("$text  •  ${sh("Kaçan", "Missed")} $misses/3", color = SonHarfTheme.TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun SeriesFriendInviteDialog(
    friends: List<ProfileDto>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onInvite: (ProfileDto) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(sh("Seri Oyun daveti", "Series Game invite"), fontWeight = FontWeight.Black) },
        text = {
            if (friends.isEmpty()) {
                Text(sh("Davet edebileceğin arkadaş bulunamadı.", "No friend is available to invite."))
            } else {
                LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(friends, key = { it.id }) { friend ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable(enabled = !busy) { onInvite(friend) },
                            shape = RoundedCornerShape(12.dp),
                            color = SonHarfTheme.SurfaceSecondary,
                        ) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Person, null, tint = SonHarfTheme.Primary)
                                Spacer(Modifier.width(8.dp))
                                Text(friend.displayName, Modifier.weight(1f), color = SonHarfTheme.TextPrimary, fontWeight = FontWeight.Bold)
                                Icon(Icons.Rounded.Send, null, tint = SonHarfTheme.Primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(sh("KAPAT", "CLOSE")) } },
    )
}

@Composable
private fun SeriesLocked(onExit: () -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.Lock, null, tint = SonHarfTheme.PremiumGold, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(sh("Seri Oyun kilitli", "Series Game is locked"), color = SonHarfTheme.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(sh("Seri Oyun veya PRO satın alındığında kalıcı olarak açılır.", "It unlocks permanently with Series Game or PRO."), color = SonHarfTheme.TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onExit) { Text(sh("MAĞAZAYA DÖN", "BACK TO STORE")) }
    }
}

@Composable
private fun SeriesLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = SonHarfTheme.Primary)
    }
}

private fun seriesGameComparator(me: String?): Comparator<WordSiegeGameDto> =
    compareBy<WordSiegeGameDto> { game ->
        when {
            game.status == "playing" && game.currentPlayerId == me -> 0
            game.status == "playing" -> 1
            game.status == "waiting" -> 2
            else -> 3
        }
    }.thenBy { it.turnDeadline ?: "9999" }.thenByDescending { it.updatedAt.ifBlank { it.createdAt } }

private fun seriesDeadlineText(deadline: String?, tick: Long): String {
    if (deadline.isNullOrBlank()) return sh("Süre bekleniyor", "Waiting for timer")
    val parsed = runCatching { Instant.parse(deadline) }.getOrNull() ?: return sh("Süre bekleniyor", "Waiting for timer")
    val now = Instant.ofEpochMilli(if (tick > 0L) tick else System.currentTimeMillis())
    val seconds = Duration.between(now, parsed).seconds.coerceAtLeast(0L)
    val minutesPart = seconds / 60L
    val secondsPart = seconds % 60L
    return String.format("%02d:%02d", minutesPart, secondsPart)
}

private fun seriesFriendlyError(raw: String): String = when {
    "series_game_required" in raw -> sh("Seri Oyun veya PRO erişimi gerekli.", "Series Game or PRO access is required.")
    "series_game_friend_required" in raw -> sh("Davet ettiğin arkadaşın da Seri Oyun erişimi olmalı.", "Your friend also needs Series Game access.")
    "word_siege_active_limit" in raw -> sh("Aktif oyun limitine ulaştın.", "You reached your active-game limit.")
    "word_siege_invite_pending" in raw -> sh("Bu oyuncuyla zaten bekleyen bir davet var.", "There is already a pending invite with this player.")
    "word_siege_invalid_word" in raw -> sh("Kelime sözlükte bulunamadı.", "The word is not in the dictionary.")
    "word_siege_not_your_turn" in raw -> sh("Sıra sende değil.", "It is not your turn.")
    "matchmaking_disabled" in raw -> sh("Eşleştirme geçici olarak kapalı.", "Matchmaking is temporarily disabled.")
    "maintenance_mode" in raw -> sh("Bakım modu etkin.", "Maintenance mode is active.")
    else -> sh("İşlem tamamlanamadı. Bağlantını kontrol edip tekrar dene.", "The action could not be completed. Check your connection and try again.")
}
