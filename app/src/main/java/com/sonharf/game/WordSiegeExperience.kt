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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal val SiegePurple = MainUi.Purple
internal val SiegePurpleSoft = Color(0xFFF0ECFF)
internal val SiegeBlueSoft = MainUi.BlueSoft
private val SiegeTile = Color(0xFFFFE3A5)
private val SiegeTileBorder = Color(0xFFD99818)

private enum class SiegeListSection { WAITING, YOUR_TURN, OPPONENT, SLEEPING, FINISHED }

@Composable
internal fun WordSiegeExperienceScreen(onExit: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    val me = remember { backend.currentUserId() }
    var games by remember { mutableStateOf<List<WordSiegeGameDto>>(emptyList()) }
    var profiles by remember { mutableStateOf<Map<String, ProfileDto>>(emptyMap()) }
    var selectedGameId by remember { mutableStateOf<String?>(null) }
    var currentGame by remember { mutableStateOf<WordSiegeGameDto?>(null) }
    var moves by remember { mutableStateOf<List<WordSiegeMoveDto>>(emptyList()) }
    var messages by remember { mutableStateOf<List<WordSiegeMessageDto>>(emptyList()) }
    var notice by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }
    var chatInput by remember { mutableStateOf("") }
    var showForfeit by remember { mutableStateOf(false) }
    var showPass by remember { mutableStateOf(false) }
    var showExchange by remember { mutableStateOf(false) }
    var exchangeSelection by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var horizontal by remember { mutableStateOf(true) }
    var selectedRackIndex by remember { mutableStateOf<Int?>(null) }
    var placements by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var practiceActive by remember { mutableStateOf(false) }

    if (practiceActive) {
        WordSiegePracticeScreen(onExit = { practiceActive = false })
        return
    }

    suspend fun loadProfiles(ids: Collection<String?>) {
        val missing = ids.filterNotNull().distinct().filterNot(profiles::containsKey)
        if (missing.isEmpty()) return
        val loaded = missing.mapNotNull { id ->
            runCatching { backend.getProfile(id) }.getOrNull()?.let { id to it }
        }.toMap()
        if (loaded.isNotEmpty()) profiles = profiles + loaded
    }

    suspend fun refreshGames(showProgress: Boolean = false) {
        if (showProgress) loading = true
        runCatching { backend.getWordSiegeGames() }
            .onSuccess { next ->
                games = next
                loadProfiles(next.flatMap { listOf(it.playerOneId, it.playerTwoId) })
                selectedGameId?.let { id ->
                    if (next.none { it.id == id }) selectedGameId = null
                }
            }
            .onFailure { notice = wordSiegeFriendlyError(it.message.orEmpty()) }
        if (showProgress) loading = false
    }

    fun applyGame(next: WordSiegeGameDto) {
        currentGame = next
        games = (games.filterNot { it.id == next.id } + next)
            .filterNot { it.status == "cancelled" }
            .sortedByDescending { it.updatedAt.ifBlank { it.createdAt } }
        placements = emptyMap()
        selectedRackIndex = null
        exchangeSelection = emptySet()
    }

    fun runGameAction(
        successNotice: String? = null,
        action: suspend () -> WordSiegeGameDto,
    ) {
        if (busy) return
        scope.launch {
            busy = true
            runCatching { action() }
                .onSuccess { next ->
                    applyGame(next)
                    notice = successNotice
                    refreshGames()
                }
                .onFailure { notice = wordSiegeFriendlyError(it.message.orEmpty()) }
            busy = false
        }
    }

    BackHandler {
        if (selectedGameId != null) {
            selectedGameId = null
            currentGame = null
            placements = emptyMap()
            selectedRackIndex = null
        } else {
            onExit()
        }
    }

    LaunchedEffect(Unit) {
        refreshGames(showProgress = true)
        while (currentCoroutineContext().isActive) {
            delay(5_000)
            refreshGames()
        }
    }

    LaunchedEffect(selectedGameId, showChat) {
        val gameId = selectedGameId ?: return@LaunchedEffect
        while (currentCoroutineContext().isActive) {
            runCatching { backend.getWordSiegeGame(gameId) }
                .onSuccess { next ->
                    val turnChanged = currentGame?.moveCount != next.moveCount ||
                        currentGame?.currentPlayerId != next.currentPlayerId
                    currentGame = next
                    loadProfiles(listOf(next.playerOneId, next.playerTwoId))
                    if (turnChanged) {
                        placements = emptyMap()
                        selectedRackIndex = null
                    }
                }
                .onFailure { notice = wordSiegeFriendlyError(it.message.orEmpty()) }
            moves = runCatching { backend.getWordSiegeMoves(gameId) }.getOrDefault(moves)
            if (showChat) {
                messages = runCatching { backend.getWordSiegeMessages(gameId) }.getOrDefault(messages)
            }
            delay(2_500)
        }
    }

    Surface(Modifier.fillMaxSize(), color = MainUi.Background) {
        if (selectedGameId == null) {
            WordSiegeGamesList(
                games = games,
                profiles = profiles,
                me = me,
                loading = loading,
                busy = busy,
                notice = notice,
                onBack = onExit,
                onRefresh = { scope.launch { refreshGames(showProgress = true) } },
                onPractice = { practiceActive = true },
                onNewGame = {
                    if (busy) return@WordSiegeGamesList
                    busy = true
                    scope.launch {
                        runCatching { backend.findOrCreateWordSiegeGame(if (SonHarfUiState.isEnglish) "en" else "tr") }
                            .onSuccess { next ->
                                applyGame(next)
                                selectedGameId = next.id
                                notice = if (next.status == "waiting") {
                                    sh("Rakip aranıyor. Oyun açık kalmak zorunda değil.", "Looking for a rival. You may leave this screen.")
                                } else null
                            }
                            .onFailure { notice = wordSiegeFriendlyError(it.message.orEmpty()) }
                        busy = false
                    }
                },
                onOpen = { game ->
                    currentGame = game
                    selectedGameId = game.id
                    notice = null
                },
            )
        } else {
            val game = currentGame
            if (game == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MainUi.Blue)
                }
            } else {
                WordSiegePanMatch(
                    game = game,
                    me = me,
                    profiles = profiles,
                    moves = moves,
                    placements = placements,
                    selectedRackIndex = selectedRackIndex,
                    horizontal = horizontal,
                    busy = busy,
                    notice = notice,
                    onBack = {
                        selectedGameId = null
                        currentGame = null
                    },
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
                    onHorizontal = { horizontal = it },
                    onSubmit = {
                        if (placements.isEmpty()) {
                            notice = sh("Önce raftan harf seçip tahtaya yerleştir.", "Place at least one rack tile on the board.")
                        } else {
                            runGameAction {
                                backend.submitWordSiegeMove(
                                    game.id,
                                    placements.entries.sortedBy { it.key }.map { WordSiegePlacement(it.key, it.value) },
                                    horizontal,
                                )
                            }
                        }
                    },
                    onPass = { showPass = true },
                    onExchange = {
                        exchangeSelection = emptySet()
                        showExchange = true
                    },
                    onChat = {
                        showChat = true
                        scope.launch {
                            messages = runCatching { backend.getWordSiegeMessages(game.id) }.getOrDefault(emptyList())
                        }
                    },
                    onForfeit = { showForfeit = true },
                    onCancelWaiting = {
                        runGameAction(sh("Rakip arama iptal edildi.", "Opponent search cancelled.")) {
                            backend.cancelWordSiegeWaiting(game.id)
                        }
                        selectedGameId = null
                        currentGame = null
                    },
                )
            }
        }
    }

    if (showPass && currentGame != null) {
        WordSiegeConfirmDialog(
            title = sh("Turu geç?", "Pass this turn?"),
            body = sh("Pas hakkın turunu bitirir. İki oyuncu art arda pas verirse oyun biter.", "Passing ends your turn. Two consecutive passes end the game."),
            confirm = sh("PAS VER", "PASS"),
            accent = MainUi.Gold,
            onDismiss = { showPass = false },
            onConfirm = {
                showPass = false
                val gameId = currentGame?.id ?: return@WordSiegeConfirmDialog
                runGameAction { backend.passWordSiegeTurn(gameId) }
            },
        )
    }

    if (showForfeit && currentGame != null) {
        WordSiegeConfirmDialog(
            title = sh("Pes etmek istiyor musun?", "Do you want to forfeit?"),
            body = sh("Bu oyun rakibinin galibiyetiyle hemen biter.", "The game ends immediately with your rival as winner."),
            confirm = sh("PES ET", "FORFEIT"),
            accent = MainUi.Red,
            onDismiss = { showForfeit = false },
            onConfirm = {
                showForfeit = false
                val gameId = currentGame?.id ?: return@WordSiegeConfirmDialog
                runGameAction { backend.forfeitWordSiegeGame(gameId) }
            },
        )
    }

    if (showExchange && currentGame != null) {
        val game = requireNotNull(currentGame)
        val rack = game.rackFor(me)
        AlertDialog(
            onDismissRequest = { showExchange = false },
            title = { Text(sh("HARF DEĞİŞTİR", "EXCHANGE TILES"), fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        sh("Değiştireceğin harfleri seç. Bu işlem turunu bitirir.", "Choose tiles to exchange. This ends your turn."),
                        color = MainUi.Muted,
                        fontSize = 13.sp,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        rack.forEachIndexed { index, letter ->
                            WordSiegeRackTile(
                                letter = letter,
                                selected = index in exchangeSelection,
                                used = false,
                                enabled = !busy,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    exchangeSelection = if (index in exchangeSelection) {
                                        exchangeSelection - index
                                    } else exchangeSelection + index
                                },
                            )
                        }
                    }
                    Text(
                        sh("Torba: ${game.bag.length} harf", "Bag: ${game.bag.length} tiles"),
                        color = MainUi.Muted,
                        fontSize = 11.sp,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = exchangeSelection.isNotEmpty() && exchangeSelection.size <= game.bag.length && !busy,
                    onClick = {
                        val selected = exchangeSelection
                        showExchange = false
                        runGameAction { backend.exchangeWordSiegeTiles(game.id, selected) }
                    },
                ) { Text(sh("DEĞİŞTİR", "EXCHANGE"), color = SiegePurple, fontWeight = FontWeight.Black) }
            },
            dismissButton = { TextButton(onClick = { showExchange = false }) { Text(sh("VAZGEÇ", "CANCEL")) } },
        )
    }

    if (showChat && currentGame != null) {
        val gameId = requireNotNull(currentGame).id
        WordSiegeChatDialog(
            messages = messages,
            me = me,
            input = chatInput,
            busy = busy,
            onInput = { chatInput = it.take(300) },
            onDismiss = { showChat = false },
            onSend = {
                if (chatInput.isBlank() || busy) return@WordSiegeChatDialog
                val outgoing = chatInput
                busy = true
                scope.launch {
                    runCatching { backend.sendWordSiegeMessage(gameId, outgoing) }
                        .onSuccess {
                            chatInput = ""
                            messages = runCatching { backend.getWordSiegeMessages(gameId) }.getOrDefault(messages)
                        }
                        .onFailure { notice = wordSiegeFriendlyError(it.message.orEmpty()) }
                    busy = false
                }
            },
        )
    }
}

@Composable
private fun WordSiegeGamesList(
    games: List<WordSiegeGameDto>,
    profiles: Map<String, ProfileDto>,
    me: String?,
    loading: Boolean,
    busy: Boolean,
    notice: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onPractice: () -> Unit,
    onNewGame: () -> Unit,
    onOpen: (WordSiegeGameDto) -> Unit,
) {
    val grouped = games.groupBy { it.listSection(me) }
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back"), tint = MainUi.Text)
                }
                Column(Modifier.weight(1f)) {
                    Text(sh("KELİME KUŞATMASI", "WORD SIEGE"), color = MainUi.Text, fontSize = 23.sp, fontWeight = FontWeight.Black)
                    Text(
                        sh("Süre yok • 1v1 • En fazla 10 devam eden oyun", "No timer • 1v1 • Up to 10 ongoing games"),
                        color = MainUi.Muted,
                        fontSize = 10.sp,
                    )
                }
                IconButton(onClick = onRefresh, enabled = !loading) {
                    Icon(Icons.Rounded.Refresh, sh("Yenile", "Refresh"), tint = MainUi.Blue)
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    sh("OYUN SEÇ", "CHOOSE A GAME"),
                    color = MainUi.Gold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = .8.sp,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    WordSiegeModeCard(
                        title = sh("RAKİP BUL", "FIND RIVAL"),
                        subtitle = sh("Çevrimiçi 1v1", "Online 1v1"),
                        icon = Icons.Rounded.Groups,
                        color = SiegePurple,
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                        onClick = onNewGame,
                        loading = busy,
                    )
                    WordSiegeModeCard(
                        title = sh("BOT İLE\nALIŞTIR", "PRACTICE\nWITH BOT"),
                        subtitle = sh("Hemen başla", "Start now"),
                        icon = Icons.Rounded.SmartToy,
                        color = MainUi.Blue,
                        enabled = true,
                        modifier = Modifier.weight(1f),
                        onClick = onPractice,
                    )
                }
            }
        }

        if (loading && games.isEmpty()) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = SiegePurple, trackColor = SiegePurpleSoft) }
        }
        notice?.let { message -> item { WordSiegeNotice(message) } }

        if (!loading && games.isEmpty()) {
            item {
                Surface(
                    color = SiegePurpleSoft,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, SiegePurple.copy(alpha = .2f)),
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = Color.White) {
                            Icon(Icons.Rounded.TipsAndUpdates, null, Modifier.padding(9.dp).size(21.dp), tint = SiegePurple)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(sh("İlk kuşatmanı kur", "Build your first siege"), color = MainUi.Text, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            Text(
                                sh("Bonuslar sadece yeni harfte çalışır; rakibin karesini kelimene katarsan alan sana geçer.", "Bonuses work on new tiles; use a rival tile in your word to capture its territory."),
                                color = MainUi.Muted,
                                fontSize = 10.sp,
                            )
                        }
                    }
                }
            }
        }

        SiegeListSection.entries.forEach { section ->
            val sectionGames = grouped[section].orEmpty()
            if (sectionGames.isNotEmpty()) {
                item {
                    Text(
                        section.label(),
                        color = section.color(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = .8.sp,
                    )
                }
                items(sectionGames, key = { it.id }) { game ->
                    WordSiegeGameCard(game, profiles, me) { onOpen(game) }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun WordSiegeModeCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.height(112.dp).clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = color,
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.fillMaxSize().padding(13.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
                if (loading) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                else Icon(Icons.Rounded.ArrowForward, null, tint = Color.White.copy(alpha = .82f), modifier = Modifier.size(18.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp, lineHeight = 15.sp)
                Text(subtitle, color = Color.White.copy(alpha = .78f), fontWeight = FontWeight.SemiBold, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun WordSiegeGameCard(
    game: WordSiegeGameDto,
    profiles: Map<String, ProfileDto>,
    me: String?,
    onClick: () -> Unit,
) {
    val opponentId = game.opponentId(me)
    val opponent = opponentId?.let(profiles::get)
    val myOwner = game.ownerFor(me)
    val myTotal = if (myOwner == 1) game.playerOneWordScore + game.playerOneAreaScore else game.playerTwoWordScore + game.playerTwoAreaScore
    val rivalTotal = if (myOwner == 1) game.playerTwoWordScore + game.playerTwoAreaScore else game.playerOneWordScore + game.playerOneAreaScore
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MainUi.Surface,
        border = BorderStroke(1.dp, MainUi.Border),
    ) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfilePhotoAvatarWithGender(
                avatarPath = opponent?.avatarPath,
                gender = opponent?.gender,
                name = opponent?.displayName ?: sh("Rakip", "Rival"),
                size = 44.dp,
                accent = SiegePurple,
                visible = opponent?.avatarVisibility != "hidden",
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    opponent?.displayName ?: if (game.status == "waiting") sh("Rakip aranıyor", "Finding a rival") else sh("Rakip", "Rival"),
                    color = MainUi.Text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(game.cardStatus(me), color = game.listSection(me).color(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                if (game.status != "waiting") {
                    Text(
                        sh("Sen $myTotal • Rakip $rivalTotal • ${game.moveCount} tur", "You $myTotal • Rival $rivalTotal • ${game.moveCount} turns"),
                        color = MainUi.Muted,
                        fontSize = 9.sp,
                    )
                }
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = MainUi.Muted)
        }
    }
}

@Composable
private fun WordSiegeMatch(
    game: WordSiegeGameDto,
    me: String?,
    profiles: Map<String, ProfileDto>,
    moves: List<WordSiegeMoveDto>,
    placements: Map<Int, Int>,
    selectedRackIndex: Int?,
    horizontal: Boolean,
    busy: Boolean,
    notice: String?,
    onBack: () -> Unit,
    onBoardCell: (Int) -> Unit,
    onRackTile: (Int) -> Unit,
    onHorizontal: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onPass: () -> Unit,
    onExchange: () -> Unit,
    onChat: () -> Unit,
    onForfeit: () -> Unit,
    onCancelWaiting: () -> Unit,
) {
    val mine = me?.let(profiles::get)
    val opponent = game.opponentId(me)?.let(profiles::get)
    val myOwner = game.ownerFor(me)
    val myTurn = game.status == "playing" && game.currentPlayerId == me
    val rack = game.rackFor(me)
    val canAct = myTurn && !busy
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, sh("Oyunlar", "Games"), tint = MainUi.Text) }
                Column(Modifier.weight(1f)) {
                    Text(sh("KELİME KUŞATMASI", "WORD SIEGE"), color = MainUi.Text, fontSize = 19.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (game.status == "playing") {
                            if (myTurn) sh("SIRA SENDE", "YOUR TURN") else sh("RAKİPTE", "RIVAL'S TURN")
                        } else game.statusLabel(me),
                        color = if (myTurn) MainUi.Blue else SiegePurple,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Surface(shape = RoundedCornerShape(99.dp), color = SiegePurpleSoft) {
                    Text(
                        sh("SÜRE YOK", "NO TIMER"),
                        Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        color = SiegePurple,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                WordSiegePlayerCard(
                    profile = mine,
                    fallbackName = sh("Sen", "You"),
                    wordScore = game.scoreFor(myOwner),
                    area = game.areaFor(myOwner),
                    accent = MainUi.Blue,
                    active = game.currentPlayerId == me,
                    modifier = Modifier.weight(1f),
                )
                WordSiegePlayerCard(
                    profile = opponent,
                    fallbackName = if (game.status == "waiting") sh("Rakip aranıyor", "Finding rival") else sh("Rakip", "Rival"),
                    wordScore = game.scoreFor(if (myOwner == 1) 2 else 1),
                    area = game.areaFor(if (myOwner == 1) 2 else 1),
                    accent = SiegePurple,
                    active = game.currentPlayerId == game.opponentId(me),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (game.status == "waiting") {
            item {
                Surface(color = MainUi.Surface, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, MainUi.Border)) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(color = SiegePurple)
                        Text(sh("RAKİP ARANIYOR", "FINDING A RIVAL"), color = MainUi.Text, fontWeight = FontWeight.Black)
                        Text(
                            sh("Beklerken çıkabilirsin. Rakip bulunduğunda oyun listende görünür.", "You can leave while waiting. The match will stay in your game list."),
                            color = MainUi.Muted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                        )
                        OutlinedButton(onClick = onCancelWaiting, enabled = !busy, border = BorderStroke(1.dp, MainUi.Red)) {
                            Text(sh("ARAMAYI İPTAL ET", "CANCEL SEARCH"), color = MainUi.Red, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            notice?.let { item { WordSiegeNotice(it) } }
            return@LazyColumn
        }

        item {
            WordSiegeBoard(
                board = game.board,
                rack = rack,
                placements = placements,
                myOwner = myOwner,
                enabled = canAct,
                onCell = onBoardCell,
            )
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedButton(
                    onClick = onChat,
                    enabled = game.playerTwoId != null,
                    modifier = Modifier.weight(1f).height(40.dp),
                    border = BorderStroke(1.dp, MainUi.Blue),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                ) {
                    Icon(Icons.Rounded.Chat, null, Modifier.size(16.dp), tint = MainUi.Blue)
                    Spacer(Modifier.width(4.dp))
                    Text(sh("SOHBET", "CHAT"), color = MainUi.Blue, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
                OutlinedButton(
                    onClick = onForfeit,
                    enabled = game.status == "playing" && !busy,
                    modifier = Modifier.weight(1f).height(40.dp),
                    border = BorderStroke(1.dp, MainUi.Red),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                ) {
                    Icon(Icons.Rounded.Flag, null, Modifier.size(16.dp), tint = MainUi.Red)
                    Spacer(Modifier.width(4.dp))
                    Text(sh("PES ET", "FORFEIT"), color = MainUi.Red, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        if (game.status == "playing") {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = horizontal,
                        onClick = { onHorizontal(true) },
                        enabled = canAct,
                        label = { Text(sh("YATAY", "HORIZONTAL"), fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Rounded.SwapHoriz, null, Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SiegeBlueSoft, selectedLabelColor = MainUi.Blue),
                    )
                    FilterChip(
                        selected = !horizontal,
                        onClick = { onHorizontal(false) },
                        enabled = canAct,
                        label = { Text(sh("DİKEY", "VERTICAL"), fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Rounded.SwapVert, null, Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SiegePurpleSoft, selectedLabelColor = SiegePurple),
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        sh("Torba ${game.bag.length}", "Bag ${game.bag.length}"),
                        color = MainUi.Muted,
                        fontSize = 9.sp,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    rack.forEachIndexed { index, letter ->
                        WordSiegeRackTile(
                            letter = letter,
                            selected = selectedRackIndex == index,
                            used = index in placements.values,
                            enabled = canAct,
                            modifier = Modifier.weight(1f),
                            onClick = { onRackTile(index) },
                        )
                    }
                    repeat((7 - rack.length).coerceAtLeast(0)) {
                        Spacer(Modifier.weight(1f).height(48.dp))
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onPass,
                        enabled = canAct,
                        modifier = Modifier.weight(1f).height(46.dp),
                        contentPadding = PaddingValues(horizontal = 3.dp),
                    ) { Text(sh("PAS", "PASS"), fontSize = 10.sp, fontWeight = FontWeight.Black) }
                    OutlinedButton(
                        onClick = onExchange,
                        enabled = canAct && game.bag.isNotEmpty(),
                        modifier = Modifier.weight(1.15f).height(46.dp),
                        border = BorderStroke(1.dp, SiegePurple),
                        contentPadding = PaddingValues(horizontal = 3.dp),
                    ) { Text(sh("DEĞİŞTİR", "EXCHANGE"), color = SiegePurple, fontSize = 9.sp, fontWeight = FontWeight.Black) }
                    Button(
                        onClick = onSubmit,
                        enabled = canAct && placements.isNotEmpty(),
                        modifier = Modifier.weight(1.45f).height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MainUi.Blue),
                        contentPadding = PaddingValues(horizontal = 5.dp),
                    ) {
                        if (busy) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text(sh("OYNA", "PLAY"), fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        } else {
            item { WordSiegeFinishedCard(game, me) }
        }

        notice?.let { item { WordSiegeNotice(it) } }

        moves.lastOrNull()?.let { lastMove ->
            item {
                Text(
                    sh(
                        "Son hamle: ${lastMove.formedWords.joinToString(" + ")} • +${lastMove.wordScore} kelime • ${lastMove.capturedCells} alan",
                        "Last move: ${lastMove.formedWords.joinToString(" + ")} • +${lastMove.wordScore} word • ${lastMove.capturedCells} territory",
                    ),
                    color = MainUi.Muted,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item { Spacer(Modifier.height(10.dp)) }
    }
}

@Composable
private fun WordSiegePlayerCard(
    profile: ProfileDto?,
    fallbackName: String,
    wordScore: Int,
    area: Int,
    accent: Color,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        color = if (active) accent.copy(alpha = .08f) else MainUi.Surface,
        border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) accent else MainUi.Border),
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfilePhotoAvatarWithGender(
                avatarPath = profile?.avatarPath,
                gender = profile?.gender,
                name = profile?.displayName ?: fallbackName,
                size = 36.dp,
                accent = accent,
                visible = profile?.avatarVisibility != "hidden",
            )
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    profile?.displayName ?: fallbackName,
                    color = MainUi.Text,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("${wordScore + area}", color = accent, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(
                    sh("Kelime $wordScore • Alan $area", "Word $wordScore • Area $area"),
                    color = MainUi.Muted,
                    fontSize = 7.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
internal fun WordSiegeBoard(
    board: List<WordSiegeCellDto>,
    rack: String,
    placements: Map<Int, Int>,
    myOwner: Int,
    enabled: Boolean,
    onCell: (Int) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFE7EDF5),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MainUi.Border),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(3.dp)) {
            val cellSize = maxWidth / 9
            Column {
                repeat(9) { row ->
                    Row {
                        repeat(9) { column ->
                            val index = row * 9 + column
                            WordSiegeBoardCell(
                                cell = board.getOrElse(index) { WordSiegeCellDto() },
                                pendingLetter = placements[index]?.let { rackIndex -> rack.getOrNull(rackIndex) },
                                pending = placements.containsKey(index),
                                myOwner = myOwner,
                                enabled = enabled,
                                size = cellSize,
                                onClick = { onCell(index) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WordSiegeBoardCell(
    cell: WordSiegeCellDto,
    pendingLetter: Char?,
    pending: Boolean,
    myOwner: Int,
    enabled: Boolean,
    size: Dp,
    onClick: () -> Unit,
) {
    val owner = if (pending) myOwner else cell.owner
    val territory = when (owner) {
        1 -> MainUi.Blue.copy(alpha = if (pending) .30f else .17f)
        2 -> SiegePurple.copy(alpha = if (pending) .30f else .17f)
        else -> MainUi.Surface
    }
    val border = when {
        pending -> SiegeTileBorder
        owner == 1 -> MainUi.Blue.copy(alpha = .45f)
        owner == 2 -> SiegePurple.copy(alpha = .45f)
        else -> MainUi.Border
    }
    val letter = pendingLetter?.toString() ?: cell.letter
    Box(
        Modifier
            .size(size)
            .padding(1.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (letter != null) territory else MainUi.Surface)
            .clickable(enabled = enabled && (cell.letter == null || pending), onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (pending) SiegeTile.copy(alpha = .92f) else Color.Transparent,
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(if (pending) 1.5.dp else .7.dp, border),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (letter != null) {
                    Text(letter, color = MainUi.Text, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    Text(
                        wordSiegeLetterValue(letter),
                        color = MainUi.Muted,
                        fontSize = 5.sp,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp),
                    )
                } else if (!cell.bonusUsed && cell.bonus != null) {
                    Text(
                        cell.bonus,
                        color = when (cell.bonus) {
                            "2H", "3H" -> MainUi.Blue
                            else -> SiegePurple
                        },
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@Composable
internal fun WordSiegeRackTile(
    letter: Char,
    selected: Boolean,
    used: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.height(48.dp).clickable(enabled = enabled, onClick = onClick),
        color = when {
            used -> MainUi.SurfaceSoft
            selected -> SiegeTile
            else -> Color(0xFFFFF1C9)
        },
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MainUi.Blue else SiegeTileBorder.copy(alpha = .7f)),
        shadowElevation = if (selected) 3.dp else 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(letter.toString(), color = if (used) MainUi.Muted.copy(alpha = .45f) else MainUi.Text, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(
                wordSiegeLetterValue(letter.toString()),
                color = MainUi.Muted,
                fontSize = 7.sp,
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
            )
        }
    }
}

@Composable
private fun WordSiegeFinishedCard(game: WordSiegeGameDto, me: String?) {
    val won = game.winnerId == me
    val draw = game.winnerId == null
    val accent = when { draw -> MainUi.Gold; won -> MainUi.Green; else -> MainUi.Red }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = .08f),
        border = BorderStroke(1.dp, accent.copy(alpha = .45f)),
    ) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                when { draw -> sh("BERABERE", "DRAW"); won -> sh("KUŞATMA SENİN!", "SIEGE WON!"); else -> sh("OYUN BİTTİ", "GAME OVER") },
                color = accent,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                sh("Sonuç = kelime puanı + sahip olunan alan", "Result = word score + owned territory"),
                color = MainUi.Muted,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
internal fun WordSiegeNotice(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        color = SiegePurpleSoft,
        border = BorderStroke(1.dp, SiegePurple.copy(alpha = .25f)),
    ) {
        Text(message, Modifier.padding(horizontal = 12.dp, vertical = 9.dp), color = MainUi.Text, fontSize = 11.sp)
    }
}

@Composable
private fun WordSiegeConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    accent: Color,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Black) },
        text = { Text(body, color = MainUi.Muted) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = accent, fontWeight = FontWeight.Black) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(sh("VAZGEÇ", "CANCEL")) } },
    )
}

@Composable
private fun WordSiegeChatDialog(
    messages: List<WordSiegeMessageDto>,
    me: String?,
    input: String,
    busy: Boolean,
    onInput: (String) -> Unit,
    onDismiss: () -> Unit,
    onSend: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(sh("SOHBET", "CHAT"), fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.heightIn(min = 220.dp, max = 430.dp)) {
                if (messages.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(sh("Henüz mesaj yok.", "No messages yet."), color = MainUi.Muted, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(messages.takeLast(40), key = { it.id }) { message ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = if (message.senderId == me) Arrangement.End else Arrangement.Start,
                            ) {
                                Surface(
                                    color = if (message.senderId == me) SiegeBlueSoft else SiegePurpleSoft,
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text(message.body, Modifier.padding(9.dp), color = MainUi.Text, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = onInput,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !busy,
                    placeholder = { Text(sh("Mesaj yaz…", "Type a message…")) },
                    trailingIcon = {
                        IconButton(onClick = onSend, enabled = input.isNotBlank() && !busy) {
                            Icon(Icons.Rounded.Send, sh("Gönder", "Send"), tint = MainUi.Blue)
                        }
                    },
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(sh("KAPAT", "CLOSE")) } },
    )
}

private fun WordSiegeGameDto.ownerFor(userId: String?): Int = if (userId == playerOneId) 1 else 2

private fun WordSiegeGameDto.opponentId(userId: String?): String? =
    if (userId == playerOneId) playerTwoId else playerOneId

private fun WordSiegeGameDto.rackFor(userId: String?): String =
    if (userId == playerOneId) playerOneRack else playerTwoRack.orEmpty()

private fun WordSiegeGameDto.scoreFor(owner: Int): Int =
    if (owner == 1) playerOneWordScore else playerTwoWordScore

private fun WordSiegeGameDto.areaFor(owner: Int): Int =
    if (owner == 1) playerOneArea else playerTwoArea

private fun WordSiegeGameDto.listSection(me: String?): SiegeListSection = when {
    status == "waiting" -> SiegeListSection.WAITING
    status == "finished" -> SiegeListSection.FINISHED
    isSleeping() -> SiegeListSection.SLEEPING
    currentPlayerId == me -> SiegeListSection.YOUR_TURN
    else -> SiegeListSection.OPPONENT
}

private fun WordSiegeGameDto.isSleeping(): Boolean {
    val stamp = lastMoveAt ?: createdAt
    return runCatching { Duration.between(Instant.parse(stamp), Instant.now()).toDays() >= 7 }.getOrDefault(false)
}

@Composable
private fun SiegeListSection.label(): String = when (this) {
    SiegeListSection.WAITING -> sh("RAKİP ARANIYOR", "FINDING A RIVAL")
    SiegeListSection.YOUR_TURN -> sh("SIRA SENDE", "YOUR TURN")
    SiegeListSection.OPPONENT -> sh("RAKİPTE", "RIVAL'S TURN")
    SiegeListSection.SLEEPING -> sh("UYUYAN OYUNLAR", "SLEEPING GAMES")
    SiegeListSection.FINISHED -> sh("BİTEN OYUNLAR", "FINISHED GAMES")
}

private fun SiegeListSection.color(): Color = when (this) {
    SiegeListSection.WAITING -> MainUi.Gold
    SiegeListSection.YOUR_TURN -> MainUi.Blue
    SiegeListSection.OPPONENT -> SiegePurple
    SiegeListSection.SLEEPING -> MainUi.Muted
    SiegeListSection.FINISHED -> MainUi.Green
}

@Composable
private fun WordSiegeGameDto.cardStatus(me: String?): String = when (listSection(me)) {
    SiegeListSection.WAITING -> sh("Eşleşme bekliyor", "Waiting to match")
    SiegeListSection.YOUR_TURN -> sh("Hamleni yap", "Make your move")
    SiegeListSection.OPPONENT -> sh("Rakibin hamlesi bekleniyor", "Waiting for your rival")
    SiegeListSection.SLEEPING -> sh("İstediğinde devam et", "Resume whenever you want")
    SiegeListSection.FINISHED -> statusLabel(me)
}

@Composable
private fun WordSiegeGameDto.statusLabel(me: String?): String = when {
    status == "cancelled" -> sh("İptal edildi", "Cancelled")
    status != "finished" -> sh("Devam ediyor", "In progress")
    winnerId == null -> sh("Berabere", "Draw")
    winnerId == me -> sh("Kazandın", "You won")
    else -> sh("Rakip kazandı", "Rival won")
}

private fun wordSiegeLetterValue(letter: String): String = when (letter) {
    "A", "E", "İ", "K", "L", "N", "R", "T" -> "1"
    "I", "M", "O", "S", "U" -> "2"
    "B", "D", "Ü", "Y" -> "3"
    "C", "Ç", "Ş", "Z" -> "4"
    "G", "H", "P" -> "5"
    "F", "Ö", "V" -> "7"
    "Ğ" -> "8"
    "J" -> "10"
    else -> "1"
}

internal fun wordSiegeFriendlyError(raw: String): String {
    val invalidWord = raw.substringAfter("word_siege_invalid_word:", "")
        .substringBefore(' ')
        .substringBefore('"')
        .trim(' ', '.', ',', ':')
    return when {
        invalidWord.isNotBlank() -> sh("$invalidWord sözlükte bulunamadı.", "$invalidWord is not in the dictionary.")
        "word_siege_active_limit" in raw -> sh("Aynı anda en fazla 10 devam eden oyunun olabilir.", "You can have at most 10 ongoing games.")
        "word_siege_not_your_turn" in raw -> sh("Şu anda sıra rakibinde.", "It is your rival's turn.")
        "word_siege_first_word_must_cover_center" in raw -> sh("İlk kelime ortadaki 2K karesinden geçmeli.", "The first word must cover the center 2W cell.")
        "word_siege_move_must_connect" in raw -> sh("Yeni kelime tahtadaki harflerden birine bağlanmalı.", "The new word must connect to the board.")
        "word_siege_gap_between_tiles" in raw -> sh("Harflerin arasında boş kare bırakamazsın.", "You cannot leave a gap between tiles.")
        "word_siege_not_in_one_row" in raw -> sh("Harfleri aynı yatay sıraya yerleştir.", "Place tiles in one horizontal row.")
        "word_siege_not_in_one_column" in raw -> sh("Harfleri aynı dikey sütuna yerleştir.", "Place tiles in one vertical column.")
        "word_siege_cell_occupied" in raw -> sh("Bu karede zaten bir harf var.", "That cell already has a tile.")
        "word_siege_not_enough_tiles" in raw -> sh("Torbada bu değişim için yeterli harf yok.", "The bag does not have enough tiles for this exchange.")
        "word_siege_word_required" in raw -> sh("En az iki harfli geçerli bir kelime oluşturmalısın.", "You must form a valid word of at least two letters.")
        "word_siege_not_playing" in raw -> sh("Bu oyun artık aktif değil.", "This game is no longer active.")
        "chat" in raw.lowercase() && "suspend" in raw.lowercase() -> sh("Sohbet erişimin geçici olarak kapalı.", "Your chat access is temporarily suspended.")
        raw.isBlank() -> sh("Bağlantı kurulamadı. Tekrar dene.", "Could not connect. Try again.")
        else -> sh("İşlem tamamlanamadı. Bağlantını kontrol edip tekrar dene.", "Could not complete the action. Check your connection and try again.")
    }
}
