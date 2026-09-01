package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clipToBounds
import kotlin.math.roundToInt
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

private enum class WordSiegeHubTab { ACTIVE, FINISHED, INVITES }
private enum class WordSiegeInfoDialog { MOVES, PROFILE, RULES, SOUND, REPORT }

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
    var loading by remember { mutableStateOf(true) }
    var openingGame by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var hubTab by remember { mutableStateOf(WordSiegeHubTab.ACTIVE) }
    var showDurationPicker by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var languagePickerForPractice by remember { mutableStateOf(false) }
    var selectedMatchLanguage by remember { mutableStateOf("tr") }
    var practiceLanguage by remember { mutableStateOf("tr") }
    var practiceActive by remember { mutableStateOf(false) }

    var selectedRackIndex by remember { mutableStateOf<Int?>(null) }
    var placements by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var rackOrder by remember { mutableStateOf<List<Int>>(emptyList()) }
    var preview by remember { mutableStateOf<WordSiegeMovePreviewDto?>(null) }
    var showChat by remember { mutableStateOf(false) }
    var chatInput by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var infoDialog by remember { mutableStateOf<WordSiegeInfoDialog?>(null) }
    var showForfeit by remember { mutableStateOf(false) }
    var showPass by remember { mutableStateOf(false) }
    var showExchange by remember { mutableStateOf(false) }
    var exchangeSelection by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var clockTick by remember { mutableLongStateOf(System.currentTimeMillis()) }

    if (practiceActive) {
        WordSiegePracticeScreen(language = practiceLanguage, onExit = { practiceActive = false })
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
            }
            .onFailure { notice = wordSiegeFriendlyError(it.message.orEmpty()) }
        if (showProgress) loading = false
    }

    fun resetTransientMoveState(game: WordSiegeGameDto? = currentGame) {
        placements = emptyMap()
        preview = null
        selectedRackIndex = null
        exchangeSelection = emptySet()
        rackOrder = List(game?.rackForAsync(me)?.length ?: 0) { it }
    }

    fun applyGame(next: WordSiegeGameDto) {
        currentGame = next
        games = (games.filterNot { it.id == next.id } + next)
            .filterNot { it.status == "cancelled" }
            .sortedByDescending { it.updatedAt.ifBlank { it.createdAt } }
        resetTransientMoveState(next)
    }

    fun leaveMatchScreen() {
        selectedGameId = null
        currentGame = null
        moves = emptyList()
        messages = emptyList()
        showMenu = false
        showChat = false
        infoDialog = null
        resetTransientMoveState(null)
        scope.launch { refreshGames() }
    }

    fun runGameAction(successNotice: String? = null, action: suspend () -> WordSiegeGameDto) {
        if (busy) return
        scope.launch {
            busy = true
            runCatching { action() }
                .onSuccess { next ->
                    applyGame(next)
                    notice = successNotice ?: if (next.finishReason == "timeout") {
                        if (next.winnerId == me) sh("Rakibin süresi doldu. Maçı kazandın.", "Your rival ran out of time. You won.")
                        else sh("Hamle süren dolduğu için maç sonuçlandı.", "The match ended because your turn time expired.")
                    } else null
                    refreshGames()
                }
                .onFailure { notice = wordSiegeFriendlyError(it.message.orEmpty()) }
            busy = false
        }
    }

    fun openFreshGame(gameId: String) {
        if (openingGame) return
        openingGame = true
        scope.launch {
            runCatching { backend.getWordSiegeGame(gameId) }
                .onSuccess { fresh ->
                    currentGame = fresh
                    selectedGameId = fresh.id
                    loadProfiles(listOf(fresh.playerOneId, fresh.playerTwoId))
                    moves = runCatching { backend.getWordSiegeMoves(fresh.id) }.getOrDefault(emptyList())
                    resetTransientMoveState(fresh)
                    notice = if (fresh.finishReason == "timeout") {
                        if (fresh.winnerId == me) sh("Rakibin süresi doldu.", "Your rival ran out of time.")
                        else sh("Hamle süren dolduğu için maç bitti.", "The match ended because your turn time expired.")
                    } else null
                }
                .onFailure { notice = wordSiegeFriendlyError(it.message.orEmpty()) }
            openingGame = false
        }
    }

    BackHandler {
        if (selectedGameId != null) leaveMatchScreen() else onExit()
    }

    LaunchedEffect(Unit) {
        refreshGames(showProgress = true)
        while (currentCoroutineContext().isActive) {
            delay(5_000)
            refreshGames()
        }
    }

    LaunchedEffect(Unit) {
        while (currentCoroutineContext().isActive) {
            clockTick = System.currentTimeMillis()
            delay(1_000)
        }
    }

    LaunchedEffect(selectedGameId, showChat) {
        val gameId = selectedGameId ?: return@LaunchedEffect
        while (currentCoroutineContext().isActive) {
            runCatching { backend.getWordSiegeGame(gameId) }
                .onSuccess { next ->
                    val old = currentGame
                    val turnChanged = old?.moveCount != next.moveCount || old?.currentPlayerId != next.currentPlayerId || old?.status != next.status
                    currentGame = next
                    loadProfiles(listOf(next.playerOneId, next.playerTwoId))
                    if (turnChanged) resetTransientMoveState(next)
                }
                .onFailure { notice = wordSiegeFriendlyError(it.message.orEmpty()) }
            moves = runCatching { backend.getWordSiegeMoves(gameId) }.getOrDefault(moves)
            if (showChat) messages = runCatching { backend.getWordSiegeMessages(gameId) }.getOrDefault(messages)
            delay(2_500)
        }
    }

    LaunchedEffect(currentGame?.id, currentGame?.moveCount, currentGame?.currentPlayerId, placements) {
        val game = currentGame
        if (game == null || game.status != "playing" || game.currentPlayerId != me || placements.isEmpty()) {
            preview = null
            return@LaunchedEffect
        }
        val direction = detectWordSiegeDirection(game.board, placements.keys) ?: run {
            preview = null
            return@LaunchedEffect
        }
        delay(120)
        val request = placements.entries.sortedBy { it.key }.map { WordSiegePlacement(it.key, it.value) }
        preview = runCatching {
            backend.previewWordSiegeMove(game.id, request, direction == WordSiegeDirection.HORIZONTAL)
        }.getOrNull()?.takeIf { it.valid }
    }

    Surface(Modifier.fillMaxSize(), color = MainUi.Background) {
        when {
            selectedGameId == null -> WordSiegeAsyncHub(
                games = games,
                profiles = profiles,
                me = me,
                tab = hubTab,
                loading = loading,
                busy = busy || openingGame,
                notice = notice,
                clockTick = clockTick,
                onTab = { hubTab = it },
                onBack = onExit,
                onRefresh = { scope.launch { refreshGames(showProgress = true) } },
                onNewGame = { languagePickerForPractice = false; showLanguagePicker = true },
                onPractice = { languagePickerForPractice = true; showLanguagePicker = true },
                onOpen = { openFreshGame(it.id) },
                onCancelWaiting = { game ->
                    runGameAction(sh("Eşleşme araması iptal edildi.", "Match search cancelled.")) {
                        backend.cancelWordSiegeWaiting(game.id)
                    }
                },
            )
            currentGame == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MainUi.Blue)
            }
            else -> WordSiegeAsyncMatch(
                game = requireNotNull(currentGame),
                me = me,
                profiles = profiles,
                moves = moves,
                placements = placements,
                preview = preview,
                selectedRackIndex = selectedRackIndex,
                rackOrder = rackOrder,
                busy = busy,
                notice = notice,
                clockTick = clockTick,
                showMenu = showMenu,
                onMenuChange = { showMenu = it },
                onExitMatch = ::leaveMatchScreen,
                onRackTile = { index ->
                    val game = currentGame ?: return@WordSiegeAsyncMatch
                    if (game.status == "playing" && game.currentPlayerId == me && !busy && index !in placements.values) {
                        selectedRackIndex = if (selectedRackIndex == index) null else index
                    }
                },
                onBoardCell = { boardIndex ->
                    val game = currentGame ?: return@WordSiegeAsyncMatch
                    if (game.status != "playing" || game.currentPlayerId != me || busy) return@WordSiegeAsyncMatch
                    if (placements.containsKey(boardIndex)) {
                        val rackIndex = placements.getValue(boardIndex)
                        placements = placements - boardIndex
                        selectedRackIndex = rackIndex
                    } else if (game.board.getOrNull(boardIndex)?.letter == null) {
                        val rackIndex = selectedRackIndex ?: return@WordSiegeAsyncMatch
                        if (rackIndex !in placements.values) {
                            placements = placements + (boardIndex to rackIndex)
                            selectedRackIndex = null
                        }
                    }
                },
                onUndo = { resetTransientMoveState(currentGame) },
                onShuffle = {
                    if (placements.isEmpty()) rackOrder = rackOrder.shuffled()
                    else notice = sh("Karıştırmadan önce geçici harfleri geri al.", "Undo temporary tiles before shuffling.")
                },
                onChat = {
                    showChat = true
                    scope.launch { messages = runCatching { backend.getWordSiegeMessages(requireNotNull(currentGame).id) }.getOrDefault(messages) }
                },
                onSubmit = {
                    val game = currentGame ?: return@WordSiegeAsyncMatch
                    if (placements.isEmpty()) return@WordSiegeAsyncMatch
                    val direction = detectWordSiegeDirection(game.board, placements.keys)
                    if (direction == null) {
                        notice = sh("Yeni harfler tek bir satır veya sütunda olmalı.", "New tiles must be in one row or column.")
                    } else {
                        val request = placements.entries.sortedBy { it.key }.map { WordSiegePlacement(it.key, it.value) }
                        runGameAction {
                            backend.submitWordSiegeMove(game.id, request, direction == WordSiegeDirection.HORIZONTAL)
                        }
                    }
                },
                onHistory = { infoDialog = WordSiegeInfoDialog.MOVES },
                onProfile = { infoDialog = WordSiegeInfoDialog.PROFILE },
                onRules = { infoDialog = WordSiegeInfoDialog.RULES },
                onSound = { infoDialog = WordSiegeInfoDialog.SOUND },
                onReport = { infoDialog = WordSiegeInfoDialog.REPORT },
                onPass = { showPass = true },
                onExchange = { showExchange = true; exchangeSelection = emptySet() },
                onForfeit = { showForfeit = true },
                onCancelWaiting = {
                    val game = currentGame ?: return@WordSiegeAsyncMatch
                    runGameAction(sh("Eşleşme araması iptal edildi.", "Match search cancelled.")) { backend.cancelWordSiegeWaiting(game.id) }
                    leaveMatchScreen()
                },
            )
        }
    }

    if (showLanguagePicker) {
        AlertDialog(
            onDismissRequest = { showLanguagePicker = false },
            title = { Text(sh("Oyun dilini seç", "Choose game language"), fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(sh("Arayüz dilinden bağımsızdır ve maç başladıktan sonra değişmez.", "Independent from UI language and locked when the match starts."), color = MainUi.Muted, fontSize = 12.sp)
                    listOf("tr" to "🇹🇷 TÜRKÇE", "en" to "🇬🇧 ENGLISH").forEach { (language, label) ->
                        Button(
                            onClick = {
                                showLanguagePicker = false
                                if (languagePickerForPractice) {
                                    practiceLanguage = language
                                    practiceActive = true
                                } else {
                                    selectedMatchLanguage = language
                                    showDurationPicker = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (language == "tr") MainUi.Blue else SiegePurple),
                        ) { Text(label, fontWeight = FontWeight.Black) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showLanguagePicker = false }) { Text(sh("VAZGEÇ", "CANCEL")) } },
        )
    }

    if (showDurationPicker) {
        AlertDialog(
            onDismissRequest = { showDurationPicker = false },
            title = { Text(sh("Tur süresini seç", "Choose turn time"), fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(sh("Bu süre toplam maç süresi değil; sıra sana geldiğinde hamle yapmak için sahip olduğun süredir.", "This is not total match time; it is how long you have to move when it is your turn."), color = MainUi.Muted, fontSize = 12.sp)
                    listOf(12, 72).forEach { hours ->
                        Button(
                            onClick = {
                                showDurationPicker = false
                                busy = true
                                scope.launch {
                                    runCatching { backend.findOrCreateWordSiegeGame(selectedMatchLanguage, hours) }
                                        .onSuccess { next ->
                                            applyGame(next)
                                            selectedGameId = next.id
                                            loadProfiles(listOf(next.playerOneId, next.playerTwoId))
                                            notice = if (next.status == "waiting") sh("$hours saatlik havuzda rakip aranıyor. Ekrandan çıkabilirsin; oyun korunur.", "Finding a rival in the ${hours}h pool. You may leave; the game is preserved.") else null
                                        }
                                        .onFailure { notice = wordSiegeFriendlyError(it.message.orEmpty()) }
                                    busy = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (hours == 12) MainUi.Blue else SiegePurple),
                        ) { Text("$hours ${sh("SAAT", "HOURS")}", fontWeight = FontWeight.Black) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showDurationPicker = false }) { Text(sh("VAZGEÇ", "CANCEL")) } },
        )
    }

    if (showChat) {
        WordSiegeAsyncChatDialog(
            messages = messages,
            me = me,
            input = chatInput,
            busy = busy,
            onInput = { chatInput = it.take(300) },
            onDismiss = { showChat = false },
            onSend = {
                val game = currentGame ?: return@WordSiegeAsyncChatDialog
                val body = chatInput.trim()
                if (body.isBlank()) return@WordSiegeAsyncChatDialog
                scope.launch {
                    busy = true
                    runCatching { backend.sendWordSiegeMessage(game.id, body) }
                        .onSuccess {
                            chatInput = ""
                            messages = runCatching { backend.getWordSiegeMessages(game.id) }.getOrDefault(messages)
                        }
                        .onFailure { notice = wordSiegeFriendlyError(it.message.orEmpty()) }
                    busy = false
                }
            },
        )
    }

    if (showForfeit) {
        WordSiegeAsyncConfirmDialog(
            title = sh("Teslim olmak istiyor musun?", "Forfeit this match?"),
            body = sh("Teslim Ol maçı kesin olarak bitirir ve rakibine galibiyet verir. Sadece ekrandan çıkmak için Oyundan Çık kullan.", "Forfeit permanently ends the match and gives your rival the win. Use Exit Game only to leave the screen."),
            confirm = sh("TESLİM OL", "FORFEIT"),
            accent = MainUi.Red,
            onDismiss = { showForfeit = false },
            onConfirm = {
                showForfeit = false
                val game = currentGame ?: return@WordSiegeAsyncConfirmDialog
                runGameAction { backend.forfeitWordSiegeGame(game.id) }
            },
        )
    }

    if (showPass) {
        WordSiegeAsyncConfirmDialog(
            title = sh("Hamleni pas geç?", "Pass your turn?"),
            body = sh("Sıra rakibine geçer ve rakibin yeni 12/72 saatlik cevap süresi başlar.", "The turn passes to your rival and their new 12/72-hour response window begins."),
            confirm = sh("PAS", "PASS"),
            accent = MainUi.Blue,
            onDismiss = { showPass = false },
            onConfirm = {
                showPass = false
                val game = currentGame ?: return@WordSiegeAsyncConfirmDialog
                runGameAction { backend.passWordSiegeTurn(game.id) }
            },
        )
    }

    if (showExchange) {
        val game = currentGame
        val rack = game?.rackForAsync(me).orEmpty()
        AlertDialog(
            onDismissRequest = { showExchange = false },
            title = { Text(sh("Harf Değiştir", "Exchange Tiles"), fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(sh("Değiştirmek istediğin harfleri seç. Bu işlem hamle sayılır ve sıra rakibine geçer.", "Select tiles to exchange. This counts as a turn and passes play to your rival."), color = MainUi.Muted)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        rack.forEachIndexed { index, letter ->
                            FilterChip(
                                selected = index in exchangeSelection,
                                onClick = { exchangeSelection = if (index in exchangeSelection) exchangeSelection - index else exchangeSelection + index },
                                label = { Text(letter.toString()) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = exchangeSelection.isNotEmpty() && !busy,
                    onClick = {
                        showExchange = false
                        val target = game ?: return@TextButton
                        val selected = exchangeSelection
                        runGameAction { backend.exchangeWordSiegeTiles(target.id, selected) }
                    },
                ) { Text(sh("DEĞİŞTİR", "EXCHANGE"), color = SiegePurple, fontWeight = FontWeight.Black) }
            },
            dismissButton = { TextButton(onClick = { showExchange = false }) { Text(sh("VAZGEÇ", "CANCEL")) } },
        )
    }

    infoDialog?.let { dialog ->
        WordSiegeAsyncInfoDialog(
            dialog = dialog,
            game = currentGame,
            moves = moves,
            opponent = currentGame?.opponentIdAsync(me)?.let(profiles::get),
            onDismiss = { infoDialog = null },
        )
    }
}

@Composable
private fun WordSiegeAsyncHub(
    games: List<WordSiegeGameDto>,
    profiles: Map<String, ProfileDto>,
    me: String?,
    tab: WordSiegeHubTab,
    loading: Boolean,
    busy: Boolean,
    notice: String?,
    clockTick: Long,
    onTab: (WordSiegeHubTab) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onNewGame: () -> Unit,
    onPractice: () -> Unit,
    onOpen: (WordSiegeGameDto) -> Unit,
    onCancelWaiting: (WordSiegeGameDto) -> Unit,
) {
    val active = games.filter { it.status == "waiting" || it.status == "playing" }
    val finished = games.filter { it.status == "finished" }
    val yourTurn = active.count { it.status == "playing" && it.currentPlayerId == me }
    val shown = when (tab) {
        WordSiegeHubTab.ACTIVE -> active
        WordSiegeHubTab.FINISHED -> finished
        WordSiegeHubTab.INVITES -> emptyList()
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back"), tint = MainUi.Text) }
            Column(Modifier.weight(1f)) {
                Text(sh("KELİME KUŞATMASI", "WORD SIEGE"), color = MainUi.Text, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(sh("12/72 saatlik asenkron alan oyunu", "12/72-hour asynchronous territory game"), color = MainUi.Muted, fontSize = 9.sp)
            }
            IconButton(onClick = onRefresh, enabled = !loading) { Icon(Icons.Rounded.Refresh, sh("Yenile", "Refresh"), tint = MainUi.Blue) }
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onNewGame,
                        enabled = !busy,
                        modifier = Modifier.weight(1.5f).height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MainUi.Blue),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Rounded.Add, null)
                        Spacer(Modifier.width(5.dp))
                        Text(sh("OYNA / YENİ OYUN", "PLAY / NEW GAME"), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                    OutlinedButton(onClick = onPractice, modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Rounded.School, null, tint = SiegePurple)
                        Spacer(Modifier.width(4.dp))
                        Text(sh("ALIŞTIR", "PRACTICE"), color = SiegePurple, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            item {
                Surface(Modifier.fillMaxWidth(), color = MainUi.Surface, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, MainUi.Border)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(sh("OYUNLARIM (${active.size})", "MY GAMES (${active.size})"), color = MainUi.Text, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            Text(if (yourTurn > 0) sh("$yourTurn maçta sıra sende", "Your turn in $yourTurn matches") else sh("Şu an bekleyen hamlen yok", "No moves waiting for you"), color = if (yourTurn > 0) MainUi.Blue else MainUi.Muted, fontSize = 9.sp, fontWeight = if (yourTurn > 0) FontWeight.Bold else FontWeight.Normal)
                        }
                        if (yourTurn > 0) Badge(containerColor = MainUi.Red) { Text(yourTurn.toString()) }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WordSiegeHubTab.entries.forEach { item ->
                        val label = when (item) {
                            WordSiegeHubTab.ACTIVE -> sh("AKTİF", "ACTIVE")
                            WordSiegeHubTab.FINISHED -> sh("BİTEN", "FINISHED")
                            WordSiegeHubTab.INVITES -> sh("DAVETLER", "INVITES")
                        }
                        FilterChip(selected = tab == item, onClick = { onTab(item) }, label = { Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold) }, modifier = Modifier.weight(1f))
                    }
                }
            }

            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = MainUi.Blue, trackColor = MainUi.BlueSoft) }

            if (tab == WordSiegeHubTab.INVITES) {
                item {
                    WordSiegeNotice(sh("Davet sistemi bu sürümde yeni bir paralel altyapı kurmadan boş bırakıldı. Rastgele oyun 12 ve 72 saat havuzlarında çalışır.", "Invites are intentionally left empty in this version rather than creating a parallel system. Random play uses separate 12h and 72h pools."))
                }
            } else if (!loading && shown.isEmpty()) {
                item {
                    Surface(Modifier.fillMaxWidth(), color = MainUi.Surface, shape = RoundedCornerShape(18.dp)) {
                        Text(
                            if (tab == WordSiegeHubTab.ACTIVE) sh("Aktif oyunun yok. Yeni bir 12 veya 72 saatlik oyun açabilirsin.", "No active games. Start a new 12h or 72h game.") else sh("Henüz biten oyun yok.", "No finished games yet."),
                            Modifier.padding(20.dp), color = MainUi.Muted, textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            items(shown, key = { it.id }) { game ->
                WordSiegeAsyncGameCard(
                    game = game,
                    me = me,
                    opponent = game.opponentIdAsync(me)?.let(profiles::get),
                    clockTick = clockTick,
                    onOpen = { onOpen(game) },
                    onCancelWaiting = { onCancelWaiting(game) },
                )
            }

            notice?.let { item { WordSiegeNotice(it) } }
            item { Spacer(Modifier.height(14.dp)) }
        }
    }
}

@Composable
private fun WordSiegeAsyncGameCard(
    game: WordSiegeGameDto,
    me: String?,
    opponent: ProfileDto?,
    clockTick: Long,
    onOpen: () -> Unit,
    onCancelWaiting: () -> Unit,
) {
    val myOwner = game.ownerForAsync(me)
    val myScore = game.scoreForAsync(myOwner) + game.areaForAsync(myOwner)
    val rivalOwner = if (myOwner == 1) 2 else 1
    val rivalScore = game.scoreForAsync(rivalOwner) + game.areaForAsync(rivalOwner)
    val waiting = game.status == "waiting"
    val myTurn = game.status == "playing" && game.currentPlayerId == me
    val remaining = wordSiegeRemainingText(game.turnDeadline, clockTick)

    Surface(Modifier.fillMaxWidth(), color = MainUi.Surface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, if (myTurn) MainUi.Blue.copy(alpha = .45f) else MainUi.Border)) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfilePhotoAvatarWithGender(
                    avatarPath = opponent?.avatarPath,
                    gender = opponent?.gender,
                    name = opponent?.displayName ?: if (waiting) sh("Rakip aranıyor", "Finding rival") else sh("Rakip", "Rival"),
                    size = 42.dp,
                    accent = if (myTurn) MainUi.Blue else SiegePurple,
                    visible = opponent?.avatarVisibility != "hidden",
                )
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(opponent?.displayName ?: if (waiting) sh("Rakip aranıyor", "Finding rival") else sh("Rakip", "Rival"), color = MainUi.Text, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    Text("$myScore - $rivalScore", color = MainUi.Text, fontSize = 17.sp, fontWeight = FontWeight.Black)
                }
                Surface(color = if (game.turnDurationHours == 12) MainUi.BlueSoft else SiegePurpleSoft, shape = RoundedCornerShape(9.dp)) {
                    Text("${game.turnDurationHours} ${sh("SAAT", "H")}", Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = if (game.turnDurationHours == 12) MainUi.Blue else SiegePurple, fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }

            when {
                waiting -> Text(sh("EŞLEŞME BEKLENİYOR • Bu ekrandan çıkabilirsin", "WAITING TO MATCH • You may leave this screen"), color = MainUi.Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                game.status == "finished" -> Text(game.finishedLabel(me), color = if (game.winnerId == me) MainUi.Green else MainUi.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                myTurn -> {
                    Text(sh("SIRA SENDE", "YOUR TURN"), color = MainUi.Blue, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text(sh("Kalan: $remaining", "Remaining: $remaining"), color = MainUi.Text, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
                else -> {
                    Text(sh("RAKİP BEKLENİYOR", "WAITING FOR RIVAL"), color = SiegePurple, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text(sh("Kalan: $remaining", "Remaining: $remaining"), color = MainUi.Text, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }

            Text(sh("Son hamle: ${wordSiegeRelativeTime(game.lastMoveAt ?: game.createdAt, clockTick)}", "Last move: ${wordSiegeRelativeTime(game.lastMoveAt ?: game.createdAt, clockTick)}"), color = MainUi.Muted, fontSize = 8.sp)

            if (waiting) {
                OutlinedButton(onClick = onCancelWaiting, Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MainUi.Red)) { Text(sh("ARAMAYI İPTAL ET", "CANCEL SEARCH"), color = MainUi.Red, fontWeight = FontWeight.Bold) }
            } else {
                Button(onClick = onOpen, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = if (game.status == "finished") MainUi.Text else MainUi.Blue)) {
                    Text(if (game.status == "finished") sh("SONUCU GÖR", "VIEW RESULT") else sh("DEVAM ET", "CONTINUE"), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun WordSiegeAsyncMatch(
    game: WordSiegeGameDto,
    me: String?,
    profiles: Map<String, ProfileDto>,
    moves: List<WordSiegeMoveDto>,
    placements: Map<Int, Int>,
    preview: WordSiegeMovePreviewDto?,
    selectedRackIndex: Int?,
    rackOrder: List<Int>,
    busy: Boolean,
    notice: String?,
    clockTick: Long,
    showMenu: Boolean,
    onMenuChange: (Boolean) -> Unit,
    onExitMatch: () -> Unit,
    onRackTile: (Int) -> Unit,
    onBoardCell: (Int) -> Unit,
    onUndo: () -> Unit,
    onShuffle: () -> Unit,
    onChat: () -> Unit,
    onSubmit: () -> Unit,
    onHistory: () -> Unit,
    onProfile: () -> Unit,
    onRules: () -> Unit,
    onSound: () -> Unit,
    onReport: () -> Unit,
    onPass: () -> Unit,
    onExchange: () -> Unit,
    onForfeit: () -> Unit,
    onCancelWaiting: () -> Unit,
) {
    val rack = game.rackForAsync(me)
    val owner = game.ownerForAsync(me)
    val canAct = game.status == "playing" && game.currentPlayerId == me && !busy
    val meProfile = me?.let(profiles::get)
    val opponent = game.opponentIdAsync(me)?.let(profiles::get)
    val myScore = game.scoreForAsync(owner) + game.areaForAsync(owner)
    val otherOwner = if (owner == 1) 2 else 1
    val rivalScore = game.scoreForAsync(otherOwner) + game.areaForAsync(otherOwner)
    val deadline = wordSiegeRemainingText(game.turnDeadline, clockTick)
    val vip = meProfile?.isVip == true

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExitMatch) { Icon(Icons.Rounded.ArrowBack, sh("Oyundan Çık", "Exit Game"), tint = MainUi.Text) }
            Column(Modifier.weight(1f)) {
                Text(sh("KELİME KUŞATMASI", "WORD SIEGE"), color = MainUi.Text, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Text(
                    when {
                        game.status == "waiting" -> sh("Rakip aranıyor", "Finding a rival")
                        game.status == "finished" -> game.finishedLabel(me)
                        game.currentPlayerId == me -> sh("SIRA SENDE • $deadline", "YOUR TURN • $deadline")
                        else -> sh("RAKİPTE • $deadline", "RIVAL'S TURN • $deadline")
                    },
                    color = if (game.currentPlayerId == me) MainUi.Blue else MainUi.Muted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Surface(color = if (game.turnDurationHours == 12) MainUi.BlueSoft else SiegePurpleSoft, shape = RoundedCornerShape(9.dp)) {
                Text("${game.turnDurationHours}H", Modifier.padding(horizontal = 7.dp, vertical = 5.dp), color = if (game.turnDurationHours == 12) MainUi.Blue else SiegePurple, fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
            Box {
                IconButton(onClick = { onMenuChange(true) }) { Icon(Icons.Rounded.MoreVert, sh("Menü", "Menu"), tint = MainUi.Text) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { onMenuChange(false) }) {
                    WordSiegeMenuItem(Icons.Rounded.History, sh("Hamleler / Hamle Geçmişi", "Moves / Move History")) { onMenuChange(false); onHistory() }
                    WordSiegeMenuItem(Icons.Rounded.Person, sh("Rakip Profili", "Rival Profile")) { onMenuChange(false); onProfile() }
                    WordSiegeMenuItem(Icons.Rounded.HelpOutline, sh("Nasıl Oynanır / Kurallar", "How to Play / Rules")) { onMenuChange(false); onRules() }
                    WordSiegeMenuItem(Icons.Rounded.VolumeUp, sh("Ses ve Müzik", "Sound & Music")) { onMenuChange(false); onSound() }
                    WordSiegeMenuItem(Icons.Rounded.Flag, sh("Şikâyet Et", "Report")) { onMenuChange(false); onReport() }
                    if (game.status == "playing") {
                        HorizontalDivider()
                        WordSiegeMenuItem(Icons.Rounded.SkipNext, sh("Pas", "Pass"), enabled = canAct) { onMenuChange(false); onPass() }
                        WordSiegeMenuItem(Icons.Rounded.SwapHoriz, sh("Harf Değiştir", "Exchange Tiles"), enabled = canAct && game.bag.isNotEmpty()) { onMenuChange(false); onExchange() }
                        WordSiegeMenuItem(Icons.Rounded.OutlinedFlag, sh("Teslim Ol", "Forfeit"), color = MainUi.Red) { onMenuChange(false); onForfeit() }
                    }
                    if (game.status == "waiting") {
                        WordSiegeMenuItem(Icons.Rounded.Close, sh("Eşleşmeyi İptal Et", "Cancel Match Search"), color = MainUi.Red) { onMenuChange(false); onCancelWaiting() }
                    }
                    HorizontalDivider()
                    WordSiegeMenuItem(Icons.Rounded.Logout, sh("Oyundan Çık", "Exit Game")) { onMenuChange(false); onExitMatch() }
                }
            }
        }

        if (game.status == "waiting") {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Surface(color = MainUi.Surface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MainUi.Border)) {
                    Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(color = SiegePurple)
                        Text(sh("${game.turnDurationHours} saatlik rakip aranıyor", "Finding a ${game.turnDurationHours}-hour rival"), color = MainUi.Text, fontWeight = FontWeight.Black)
                        Text(sh("Uygulamadan veya bu ekrandan çıkabilirsin. Eşleşme backend'de korunur.", "You may leave the app or this screen. Matchmaking is preserved on the backend."), color = MainUi.Muted, fontSize = 10.sp, textAlign = TextAlign.Center)
                        OutlinedButton(onClick = onExitMatch) { Text(sh("OYUNLARIMA DÖN", "BACK TO MY GAMES")) }
                    }
                }
            }
            return@Column
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WordSiegeCompactPlayer(meProfile, sh("Sen", "You"), myScore, owner == 1, game.currentPlayerId == me, Modifier.weight(1f))
            WordSiegeCompactPlayer(opponent, sh("Rakip", "Rival"), rivalScore, otherOwner == 1, game.currentPlayerId != me && game.status == "playing", Modifier.weight(1f))
        }

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val size = minOf(maxWidth, maxHeight)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                WordSiegeBoard(
                    board = game.board,
                    rack = rack,
                    placements = placements,
                    previewCells = preview?.previewCells?.toSet().orEmpty(),
                    myOwner = owner,
                    enabled = canAct,
                    onCell = onBoardCell,
                    modifier = Modifier.size(size),
                )
            }
        }

        if (game.status == "finished") {
            WordSiegeAsyncFinishedCard(game, me, moves, vip)
        } else {
            WordSiegeMoveAnalysisBar(preview, placements.isNotEmpty(), vip, game, moves, me, tight = true)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val order = if (rackOrder.size == rack.length) rackOrder else List(rack.length) { it }
                order.forEach { originalIndex ->
                    val letter = rack.getOrNull(originalIndex) ?: return@forEach
                    WordSiegeRackTile(
                        letter = letter,
                        selected = selectedRackIndex == originalIndex,
                        used = originalIndex in placements.values,
                        enabled = canAct,
                        modifier = Modifier.weight(1f),
                        tileHeight = 42.dp,
                        onClick = { onRackTile(originalIndex) },
                    )
                }
                repeat((7 - rack.length).coerceAtLeast(0)) { Spacer(Modifier.weight(1f).height(42.dp)) }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                OutlinedButton(onClick = onUndo, enabled = canAct && placements.isNotEmpty(), modifier = Modifier.weight(1f).height(42.dp), contentPadding = PaddingValues(2.dp)) {
                    Icon(Icons.Rounded.Undo, null, Modifier.size(15.dp)); Spacer(Modifier.width(2.dp)); Text(sh("GERİ AL", "UNDO"), fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
                OutlinedButton(onClick = onShuffle, enabled = canAct && rack.length > 1, modifier = Modifier.weight(1f).height(42.dp), contentPadding = PaddingValues(2.dp)) {
                    Icon(Icons.Rounded.Shuffle, null, Modifier.size(15.dp)); Spacer(Modifier.width(2.dp)); Text(sh("KARIŞTIR", "SHUFFLE"), fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
                OutlinedButton(onClick = onChat, enabled = game.playerTwoId != null, modifier = Modifier.weight(1f).height(42.dp), contentPadding = PaddingValues(2.dp)) {
                    Icon(Icons.Rounded.Chat, null, Modifier.size(15.dp)); Spacer(Modifier.width(2.dp)); Text(sh("SOHBET", "CHAT"), fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
                Button(onClick = onSubmit, enabled = canAct && placements.isNotEmpty(), modifier = Modifier.weight(1.35f).height(42.dp), colors = ButtonDefaults.buttonColors(containerColor = MainUi.Blue), contentPadding = PaddingValues(horizontal = 4.dp)) {
                    if (busy) CircularProgressIndicator(Modifier.size(15.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text(sh("OYNA", "PLAY"), fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        notice?.let { Text(it, Modifier.fillMaxWidth(), color = MainUi.Text, fontSize = 8.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
    }
}

@Composable
private fun WordSiegeMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, enabled: Boolean = true, color: Color = MainUi.Text, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(text, color = if (enabled) color else MainUi.Muted, fontSize = 12.sp) }, leadingIcon = { Icon(icon, null, tint = if (enabled) color else MainUi.Muted) }, enabled = enabled, onClick = onClick)
}

@Composable
private fun WordSiegeCompactPlayer(profile: ProfileDto?, fallback: String, score: Int, blue: Boolean, active: Boolean, modifier: Modifier) {
    val accent = if (blue) MainUi.Blue else SiegePurple
    Surface(modifier, color = if (active) accent.copy(alpha = .08f) else MainUi.Surface, shape = RoundedCornerShape(14.dp), border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) accent else MainUi.Border)) {
        Row(Modifier.fillMaxWidth().padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfilePhotoAvatarWithGender(profile?.avatarPath, profile?.gender, profile?.displayName ?: fallback, 30.dp, accent, visible = profile?.avatarVisibility != "hidden")
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(profile?.displayName ?: fallback, color = MainUi.Text, fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(score.toString(), color = accent, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun WordSiegeAsyncFinishedCard(game: WordSiegeGameDto, me: String?, moves: List<WordSiegeMoveDto>, vip: Boolean) {
    val won = game.winnerId == me
    val draw = game.winnerId == null
    val accent = when { draw -> MainUi.Gold; won -> MainUi.Green; else -> MainUi.Red }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Surface(Modifier.fillMaxWidth(), color = accent.copy(alpha = .08f), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, accent.copy(alpha = .35f))) {
            Column(Modifier.fillMaxWidth().padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(game.finishedLabel(me), color = accent, fontSize = 13.sp, fontWeight = FontWeight.Black)
                if (game.finishReason == "timeout") Text(sh("Süre aşımı", "Timeout"), color = MainUi.Muted, fontSize = 8.sp)
            }
        }
        if (vip) WordSiegeVipFinishedAnalysis(game, moves, me)
    }
}

@Composable
private fun WordSiegeAsyncInfoDialog(dialog: WordSiegeInfoDialog, game: WordSiegeGameDto?, moves: List<WordSiegeMoveDto>, opponent: ProfileDto?, onDismiss: () -> Unit) {
    val title = when (dialog) {
        WordSiegeInfoDialog.MOVES -> sh("HAMLE GEÇMİŞİ", "MOVE HISTORY")
        WordSiegeInfoDialog.PROFILE -> sh("RAKİP PROFİLİ", "RIVAL PROFILE")
        WordSiegeInfoDialog.RULES -> sh("NASIL OYNANIR", "HOW TO PLAY")
        WordSiegeInfoDialog.SOUND -> sh("SES VE MÜZİK", "SOUND & MUSIC")
        WordSiegeInfoDialog.REPORT -> sh("ŞİKÂYET ET", "REPORT")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Black) },
        text = {
            when (dialog) {
                WordSiegeInfoDialog.MOVES -> LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    if (moves.isEmpty()) item { Text(sh("Henüz hamle yok.", "No moves yet."), color = MainUi.Muted) }
                    items(moves.asReversed(), key = { it.id }) { move ->
                        Surface(color = MainUi.SurfaceSoft, shape = RoundedCornerShape(10.dp)) {
                            Column(Modifier.fillMaxWidth().padding(8.dp)) {
                                Text(move.formedWords.joinToString(" + ").ifBlank { move.primaryWord }, color = MainUi.Text, fontWeight = FontWeight.Bold)
                                Text("+${move.wordScore} • ${sh("alan", "territory")} ${move.capturedCells} • ${wordSiegeRelativeTime(move.createdAt, System.currentTimeMillis())}", color = MainUi.Muted, fontSize = 9.sp)
                            }
                        }
                    }
                }
                WordSiegeInfoDialog.PROFILE -> Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ProfilePhotoAvatarWithGender(opponent?.avatarPath, opponent?.gender, opponent?.displayName ?: sh("Rakip", "Rival"), 64.dp, SiegePurple, visible = opponent?.avatarVisibility != "hidden")
                    Text(opponent?.displayName ?: sh("Rakip", "Rival"), color = MainUi.Text, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("${opponent?.rating ?: 1000} rating", color = MainUi.Muted)
                }
                WordSiegeInfoDialog.RULES -> Text(sh("Harflerini tahtaya yerleştir. Yeni harfler aynı satır veya sütunda olmalı; oluşan bütün yatay/dikey kelimeler geçerli olmalı. Mevcut harfleri kullanabilir ve rakip alanını kelimene katarak ele geçirebilirsin. Sıra sana geçtiğinde ${game?.turnDurationHours ?: 12} saat içinde hamle, pas veya değişim yapmalısın; süre dolarsa sunucu maçı otomatik kaybettirir.", "Place rack tiles on the board. New tiles must share one row or column and every horizontal/vertical word formed must be valid. You may reuse existing tiles and capture rival territory through your word. When your turn starts you have ${game?.turnDurationHours ?: 12} hours to play, pass or exchange; the server awards a timeout loss when the deadline expires."), color = MainUi.Text)
                WordSiegeInfoDialog.SOUND -> Text(sh("Ses ve müzik tercihleri uygulamanın Ayarlar bölümünden yönetilir. Maçtan çıkmak bu tercihleri veya aktif oyunu değiştirmez.", "Sound and music preferences are managed from app Settings. Leaving the match does not change them or the active game."), color = MainUi.Text)
                WordSiegeInfoDialog.REPORT -> Text(sh("Bu sürümde ayrı bir Kelime Kuşatması şikâyet backend'i bulunmuyor. Yanlış bir güvenlik kaydı oluşturmamak için yeni paralel moderation tablosu eklenmedi. Mevcut destek/şikâyet kanalı kullanılmalıdır.", "There is no dedicated Word Siege report backend in this version. A parallel moderation table was not invented; use the existing support/report channel."), color = MainUi.Text)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(sh("KAPAT", "CLOSE")) } },
    )
}

@Composable
private fun WordSiegeAsyncConfirmDialog(title: String, body: String, confirm: String, accent: Color, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Black) },
        text = { Text(body, color = MainUi.Muted) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = accent, fontWeight = FontWeight.Black) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(sh("VAZGEÇ", "CANCEL")) } },
    )
}

@Composable
private fun WordSiegeAsyncChatDialog(messages: List<WordSiegeMessageDto>, me: String?, input: String, busy: Boolean, onInput: (String) -> Unit, onDismiss: () -> Unit, onSend: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(sh("SOHBET", "CHAT"), fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.heightIn(min = 220.dp, max = 430.dp)) {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (messages.isEmpty()) item { Text(sh("Henüz mesaj yok.", "No messages yet."), color = MainUi.Muted) }
                    items(messages.takeLast(40), key = { it.id }) { message ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.senderId == me) Arrangement.End else Arrangement.Start) {
                            Surface(color = if (message.senderId == me) SiegeBlueSoft else SiegePurpleSoft, shape = RoundedCornerShape(12.dp)) {
                                Text(message.body, Modifier.padding(9.dp), color = MainUi.Text, fontSize = 11.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = input, onValueChange = onInput, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy, placeholder = { Text(sh("Mesaj yaz…", "Type a message…")) }, trailingIcon = { IconButton(onClick = onSend, enabled = input.isNotBlank() && !busy) { Icon(Icons.Rounded.Send, sh("Gönder", "Send"), tint = MainUi.Blue) } })
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(sh("KAPAT", "CLOSE")) } },
    )
}

private fun WordSiegeGameDto.ownerForAsync(userId: String?): Int = if (userId == playerOneId) 1 else 2
private fun WordSiegeGameDto.opponentIdAsync(userId: String?): String? = if (userId == playerOneId) playerTwoId else playerOneId
private fun WordSiegeGameDto.rackForAsync(userId: String?): String = if (userId == playerOneId) playerOneRack else playerTwoRack.orEmpty()
private fun WordSiegeGameDto.scoreForAsync(owner: Int): Int = if (owner == 1) playerOneWordScore else playerTwoWordScore
private fun WordSiegeGameDto.areaForAsync(owner: Int): Int = if (owner == 1) playerOneArea else playerTwoArea

@Composable
private fun WordSiegeGameDto.finishedLabel(me: String?): String = when {
    status != "finished" -> sh("Devam ediyor", "In progress")
    finishReason == "timeout" && winnerId == me -> sh("SÜRE AŞIMI • KAZANDIN", "TIMEOUT • YOU WON")
    finishReason == "timeout" -> sh("SÜRE AŞIMI • MAĞLUBİYET", "TIMEOUT • DEFEAT")
    finishReason == "forfeit" && winnerId == me -> sh("RAKİP TESLİM OLDU • KAZANDIN", "RIVAL FORFEITED • YOU WON")
    finishReason == "forfeit" -> sh("TESLİM OLDUN", "YOU FORFEITED")
    winnerId == null -> sh("BERABERE", "DRAW")
    winnerId == me -> sh("KAZANDIN", "YOU WON")
    else -> sh("MAĞLUBİYET", "DEFEAT")
}

private fun wordSiegeRemainingText(deadline: String?, clockTick: Long): String {
    if (deadline.isNullOrBlank()) return "—"
    val remaining = runCatching { Duration.between(Instant.ofEpochMilli(clockTick), Instant.parse(deadline)) }.getOrNull() ?: return "—"
    if (remaining.isNegative || remaining.isZero) return "00:00:00"
    val seconds = remaining.seconds
    val days = seconds / 86_400
    val hours = (seconds % 86_400) / 3_600
    val minutes = (seconds % 3_600) / 60
    val secs = seconds % 60
    return if (days > 0) "%dd %02d:%02d:%02d".format(days, hours, minutes, secs) else "%02d:%02d:%02d".format(hours, minutes, secs)
}

private fun wordSiegeRelativeTime(timestamp: String?, clockTick: Long): String {
    if (timestamp.isNullOrBlank()) return "—"
    val duration = runCatching { Duration.between(Instant.parse(timestamp), Instant.ofEpochMilli(clockTick)) }.getOrNull() ?: return "—"
    val seconds = duration.seconds.coerceAtLeast(0)
    return when {
        seconds < 60 -> shPlain("az önce", "just now")
        seconds < 3_600 -> shPlain("${seconds / 60} dk önce", "${seconds / 60}m ago")
        seconds < 86_400 -> shPlain("${seconds / 3_600} sa önce", "${seconds / 3_600}h ago")
        else -> shPlain("${seconds / 86_400} gün önce", "${seconds / 86_400}d ago")
    }
}

private fun shPlain(tr: String, en: String): String = if (SonHarfUiState.isEnglish) en else tr

@Composable
internal fun WordSiegeBoard(
    board: List<WordSiegeCellDto>, rack: String, placements: Map<Int, Int>, previewCells: Set<Int> = emptySet(),
    myOwner: Int, enabled: Boolean, onCell: (Int) -> Unit, modifier: Modifier = Modifier.fillMaxWidth(),
) {
    val density = LocalDensity.current
    Surface(modifier = modifier, color = Color(0xFFE7EDF5), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MainUi.Border)) {
        BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
            val viewportWidth = maxWidth; val viewportHeight = maxHeight
            val viewportShortSide = minOf(viewportWidth, viewportHeight)
            val boardSize = maxOf(432.dp, viewportShortSide + 96.dp)
            val cellSize = (boardSize - 6.dp) / 9
            val viewportWidthPx = with(density) { viewportWidth.toPx() }
            val viewportHeightPx = with(density) { viewportHeight.toPx() }
            val boardSizePx = with(density) { boardSize.toPx() }
            val minX = (viewportWidthPx - boardSizePx).coerceAtMost(0f)
            val minY = (viewportHeightPx - boardSizePx).coerceAtMost(0f)
            var offsetX by remember(boardSizePx, viewportWidthPx) { mutableFloatStateOf(minX / 2f) }
            var offsetY by remember(boardSizePx, viewportHeightPx) { mutableFloatStateOf(minY / 2f) }
            Box(Modifier.fillMaxSize().pointerInput(boardSizePx, viewportWidthPx, viewportHeightPx) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX = (offsetX + dragAmount.x).coerceIn(minX, 0f)
                    offsetY = (offsetY + dragAmount.y).coerceIn(minY, 0f)
                }
            }) {
                Surface(Modifier.size(boardSize).offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }, color = Color(0xFFE7EDF5), shape = RoundedCornerShape(10.dp)) {
                    Column(Modifier.fillMaxSize().padding(3.dp)) {
                        repeat(9) { row -> Row { repeat(9) { column ->
                            val index = row * 9 + column
                            WordSiegeBoardCell(
                                cell = board.getOrElse(index) { WordSiegeCellDto() },
                                pendingLetter = placements[index]?.let { rackIndex -> rack.getOrNull(rackIndex) },
                                pending = placements.containsKey(index), previewArea = index in previewCells,
                                myOwner = myOwner, enabled = enabled, size = cellSize, onClick = { onCell(index) },
                            )
                        } } }
                    }
                }
            }
        }
    }
}

@Composable
private fun WordSiegeBoardCell(cell: WordSiegeCellDto, pendingLetter: Char?, pending: Boolean, previewArea: Boolean, myOwner: Int, enabled: Boolean, size: Dp, onClick: () -> Unit) {
    val owner = if (pending) myOwner else cell.owner
    val relation = TrainingBotSupport.ownershipRelation(owner, myOwner)
    val targetFill = when (relation) {
        WordSiegeOwnershipRelation.SELF -> Color(TrainingBotSupport.OWN_FILL_ARGB)
        WordSiegeOwnershipRelation.OPPONENT -> Color(TrainingBotSupport.OPPONENT_FILL_ARGB)
        WordSiegeOwnershipRelation.NEUTRAL -> Color(TrainingBotSupport.NEUTRAL_FILL_ARGB)
    }
    val targetBorder = when (relation) {
        WordSiegeOwnershipRelation.SELF -> Color(TrainingBotSupport.OWN_BORDER_ARGB)
        WordSiegeOwnershipRelation.OPPONENT -> Color(TrainingBotSupport.OPPONENT_BORDER_ARGB)
        WordSiegeOwnershipRelation.NEUTRAL -> MainUi.Border
    }
    val fill by animateColorAsState(targetFill, tween(220), label = "siege-owner-fill")
    val border by animateColorAsState(targetBorder, tween(220), label = "siege-owner-border")
    val letter = pendingLetter?.toString() ?: cell.letter
    val shape = RoundedCornerShape(4.dp)
    Box(
        Modifier.size(size).padding(1.dp).clip(shape)
            .background(if (letter != null) fill else MainUi.Surface)
            .border(if (owner != 0) 1.2.dp else .7.dp, if (previewArea) MainUi.Green else border, shape)
            .clickable(enabled = enabled && (cell.letter == null || pending), onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (pending) Box(Modifier.fillMaxSize().background(SiegeTile.copy(alpha = .34f), shape))
        if (previewArea && !pending) Box(Modifier.fillMaxSize().background(MainUi.Green.copy(alpha = .08f), shape))
        if (letter != null) {
            Text(letter, color = Color(0xFF111827), fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text(wordSiegeLetterValue(letter), color = Color(0xFF374151), fontSize = 5.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp))
            if (owner != 0 && !pending) Box(Modifier.align(Alignment.TopEnd).padding(2.dp).size(4.dp).background(border, CircleShape))
        } else if (!cell.bonusUsed && cell.bonus != null) {
            Text(cell.bonus, color = if (cell.bonus in setOf("2H", "3H")) MainUi.Blue else SiegePurple, fontSize = 7.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
internal fun WordSiegeRackTile(letter: Char, selected: Boolean, used: Boolean, enabled: Boolean, modifier: Modifier = Modifier, tileHeight: Dp = 48.dp, onClick: () -> Unit) {
    Surface(modifier = modifier.height(tileHeight).clickable(enabled = enabled, onClick = onClick), color = when { used -> MainUi.SurfaceSoft; selected -> SiegeTile; else -> Color(0xFFFFF1C9) }, shape = RoundedCornerShape(9.dp), border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MainUi.Blue else SiegeTileBorder.copy(alpha = .7f)), shadowElevation = if (selected) 3.dp else 0.dp) {
        Box(contentAlignment = Alignment.Center) {
            Text(letter.toString(), color = if (used) MainUi.Muted.copy(alpha = .45f) else MainUi.Text, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(wordSiegeLetterValue(letter.toString()), color = MainUi.Muted, fontSize = 7.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp))
        }
    }
}

@Composable
internal fun WordSiegeNotice(message: String) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp), color = SiegePurpleSoft, border = BorderStroke(1.dp, SiegePurple.copy(alpha = .25f))) {
        Text(message, Modifier.padding(horizontal = 12.dp, vertical = 9.dp), color = MainUi.Text, fontSize = 11.sp)
    }
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
    val invalidWord = raw.substringAfter("word_siege_invalid_word:", "").substringBefore(' ').substringBefore('"').trim(' ', '.', ',', ':')
    return when {
        invalidWord.isNotBlank() -> shPlain("$invalidWord sözlükte bulunamadı.", "$invalidWord is not in the dictionary.")
        "word_siege_active_limit" in raw -> shPlain("Aynı anda en fazla 10 devam eden oyunun olabilir.", "You can have at most 10 ongoing games.")
        "word_siege_invalid_turn_duration" in raw -> shPlain("Oyun süresi 12 veya 72 saat olmalı.", "Turn time must be 12 or 72 hours.")
        "word_siege_not_your_turn" in raw -> shPlain("Şu anda sıra rakibinde.", "It is your rival's turn.")
        "word_siege_first_word_must_cover_center" in raw -> shPlain("İlk kelime ortadaki 2K karesinden geçmeli.", "The first word must cover the center 2W cell.")
        "word_siege_move_must_connect" in raw -> shPlain("Yeni kelime tahtadaki harflerden birine bağlanmalı.", "The new word must connect to the board.")
        "word_siege_gap_between_tiles" in raw -> shPlain("Harflerin arasında boş kare bırakamazsın.", "You cannot leave a gap between tiles.")
        "word_siege_not_in_one_row" in raw -> shPlain("Harfleri aynı yatay sıraya yerleştir.", "Place tiles in one horizontal row.")
        "word_siege_not_in_one_column" in raw -> shPlain("Harfleri aynı dikey sütuna yerleştir.", "Place tiles in one vertical column.")
        "word_siege_cell_occupied" in raw -> shPlain("Bu karede zaten bir harf var.", "That cell already has a tile.")
        "word_siege_not_enough_tiles" in raw -> shPlain("Torbada bu değişim için yeterli harf yok.", "The bag does not have enough tiles for this exchange.")
        "word_siege_word_required" in raw -> shPlain("En az iki harfli geçerli bir kelime oluşturmalısın.", "You must form a valid word of at least two letters.")
        "word_siege_not_playing" in raw -> shPlain("Bu oyun artık aktif değil. Oyunlarım listesini yenile.", "This game is no longer active. Refresh My Games.")
        "word_siege_not_participant" in raw -> shPlain("Bu oyuna erişim yetkin yok.", "You do not have access to this game.")
        "chat" in raw.lowercase() && "suspend" in raw.lowercase() -> shPlain("Sohbet erişimin geçici olarak kapalı.", "Your chat access is temporarily suspended.")
        raw.isBlank() -> shPlain("Bağlantı kurulamadı. Tekrar dene.", "Could not connect. Try again.")
        else -> shPlain("İşlem tamamlanamadı. Bağlantını kontrol edip tekrar dene.", "Could not complete the action. Check your connection and try again.")
    }
}
