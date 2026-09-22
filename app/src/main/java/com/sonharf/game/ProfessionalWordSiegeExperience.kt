package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

private enum class ProfessionalSiegeSection { WAITING, YOUR_TURN, OPPONENT, SLEEPING, FINISHED }

@Composable
internal fun ProfessionalWordSiegeExperienceScreen(onExit: () -> Unit) {
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
            .onFailure {
                notice = if (currentGame?.status == "waiting") {
                    sh("Bağlantı yenileniyor • rakip araması sürüyor", "Reconnecting • opponent search continues")
                } else wordSiegeFriendlyError(it.message.orEmpty())
            }
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
            runCatching { backend.refreshWordSiegeGame(gameId) }
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

    Surface(Modifier.fillMaxSize(), color = GameColors.AppBackground) {
        if (selectedGameId == null) {
            ProfessionalWordSiegeLobby(
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
                    if (busy) return@ProfessionalWordSiegeLobby
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
                    CircularProgressIndicator(color = GameColors.PrimaryBlue)
                }
            } else {
                WordSiegePanMatch(
                    game = game,
                    me = me,
                    profiles = profiles,
                    moves = moves,
                    placements = placements,
                    selectedRackIndex = selectedRackIndex,
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
                    onSubmit = {
                        if (placements.isEmpty()) {
                            notice = sh("Önce raftan harf seçip tahtaya yerleştir.", "Place at least one rack tile on the board.")
                        } else {
                            val orientation = runCatching { WordSiegeFinalRules.detectOrientation(game.board, placements.keys) }
                            if (orientation.isFailure) {
                                notice = wordSiegeFriendlyError(orientation.exceptionOrNull()?.message.orEmpty())
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
        ProfessionalSiegeConfirmDialog(
            title = sh("Turu geç?", "Pass this turn?"),
            body = sh("Pas hakkın turunu bitirir. İki oyuncu art arda pas verirse oyun biter.", "Passing ends your turn. Two consecutive passes end the game."),
            confirm = sh("PAS VER", "PASS"),
            accent = GameColors.RewardAmber,
            onDismiss = { showPass = false },
            onConfirm = {
                showPass = false
                val gameId = currentGame?.id ?: return@ProfessionalSiegeConfirmDialog
                runGameAction { backend.passWordSiegeTurn(gameId) }
            },
        )
    }

    if (showForfeit && currentGame != null) {
        ProfessionalSiegeConfirmDialog(
            title = sh("Pes etmek istiyor musun?", "Do you want to forfeit?"),
            body = sh("Bu oyun rakibinin galibiyetiyle hemen biter.", "The game ends immediately with your rival as winner."),
            confirm = sh("PES ET", "FORFEIT"),
            accent = GameColors.Danger,
            onDismiss = { showForfeit = false },
            onConfirm = {
                showForfeit = false
                val gameId = currentGame?.id ?: return@ProfessionalSiegeConfirmDialog
                runGameAction { backend.forfeitWordSiegeGame(gameId) }
            },
        )
    }

    if (showExchange && currentGame != null) {
        val game = requireNotNull(currentGame)
        val rack = proSiegeRackFor(game, me)
        AlertDialog(
            onDismissRequest = { showExchange = false },
            containerColor = GameColors.PrimarySurface,
            titleContentColor = GameColors.TextPrimary,
            textContentColor = GameColors.TextSecondary,
            title = { Text(sh("HARF DEĞİŞTİR", "EXCHANGE TILES"), fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        sh("Değiştireceğin harfleri seç. Bu işlem turunu bitirir.", "Choose tiles to exchange. This ends your turn."),
                        color = GameColors.TextSecondary,
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
                        color = GameColors.TextSecondary,
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
                ) { Text(sh("DEĞİŞTİR", "EXCHANGE"), color = GameColors.TacticalTurquoise, fontWeight = FontWeight.Black) }
            },
            dismissButton = {
                TextButton(onClick = { showExchange = false }) {
                    Text(sh("VAZGEÇ", "CANCEL"), color = GameColors.TextSecondary)
                }
            },
        )
    }

    if (showChat && currentGame != null) {
        val gameId = requireNotNull(currentGame).id
        ProfessionalSiegeChatDialog(
            messages = messages,
            me = me,
            input = chatInput,
            busy = busy,
            onInput = { chatInput = it.take(300) },
            onDismiss = { showChat = false },
            onSend = { outgoing ->
                val text = outgoing.trim().take(300)
                if (text.isBlank() || busy) return@ProfessionalSiegeChatDialog
                busy = true
                scope.launch {
                    runCatching { backend.sendWordSiegeMessage(gameId, text) }
                        .onSuccess {
                            if (chatInput.trim() == text) chatInput = ""
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
private fun ProfessionalWordSiegeLobby(
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
    val grouped = games.groupBy { proSiegeSection(it, me) }
    val activeCount = games.count { it.status != "finished" && it.status != "cancelled" }
    val yourTurnCount = games.count { proSiegeSection(it, me) == ProfessionalSiegeSection.YOUR_TURN }
    val waitingCount = games.count { it.status == "waiting" }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = GameSpacing.ScreenHorizontal, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "siege-top") {
            GameTopBar(
                title = sh("Kelime Kuşatması", "Word Siege"),
                subtitle = sh("Taktik alan savaşı • 1v1", "Tactical territory battle • 1v1"),
                onBack = onBack,
                trailing = {
                    GameIconButton(
                        icon = Icons.Rounded.Refresh,
                        description = sh("Yenile", "Refresh"),
                        onClick = onRefresh,
                        enabled = !loading,
                    )
                },
            )
        }

        item(key = "siege-actions") {
            GameSurface(
                elevated = true,
                borderColor = GameColors.TacticalTurquoise.copy(alpha = .45f),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = GameColors.TacticalTurquoise.copy(alpha = .14f)) {
                        Icon(
                            Icons.Rounded.GridView,
                            contentDescription = null,
                            tint = GameColors.TacticalTurquoise,
                            modifier = Modifier.padding(11.dp).size(27.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            sh("Standart Kuşatma", "Standard Siege"),
                            color = GameColors.TextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            sh("Rakip bul veya botla hazırlan", "Find a rival or prepare with a bot"),
                            color = GameColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    GamePrimaryButton(
                        text = sh("RAKİP BUL", "FIND RIVAL"),
                        onClick = onNewGame,
                        modifier = Modifier.weight(1f),
                        enabled = !busy,
                        icon = Icons.Rounded.Groups,
                    )
                    GameSecondaryButton(
                        text = sh("ALIŞTIRMA", "PRACTICE"),
                        onClick = onPractice,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.SmartToy,
                    )
                }
            }
        }

        item(key = "siege-metrics") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfessionalSiegeMetric(activeCount.toString(), sh("AKTİF", "ACTIVE"), GameColors.PrimaryBlue, Modifier.weight(1f))
                ProfessionalSiegeMetric(yourTurnCount.toString(), sh("SIRA SENDE", "YOUR TURN"), GameColors.PlayGreen, Modifier.weight(1f))
                ProfessionalSiegeMetric(waitingCount.toString(), sh("ARANIYOR", "SEARCHING"), GameColors.RewardAmber, Modifier.weight(1f))
            }
        }

        if (loading && games.isEmpty()) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = GameColors.TacticalTurquoise,
                    trackColor = GameColors.SecondarySurface,
                )
            }
        }

        notice?.let { message -> item { WordSiegeNotice(message) } }

        if (!loading && games.isEmpty()) {
            item {
                GameEmptyState(
                    icon = Icons.Rounded.Shield,
                    title = sh("İlk kuşatmanı başlat", "Start your first siege"),
                    body = sh(
                        "Rakip bul ile çevrimiçi eşleş veya önce alıştırma botunda hazırlan.",
                        "Match online with Find Rival or prepare against the practice bot first.",
                    ),
                    actionText = sh("RAKİP BUL", "FIND RIVAL"),
                    onAction = onNewGame,
                )
            }
        }

        ProfessionalSiegeSection.entries.forEach { section ->
            val sectionGames = grouped[section].orEmpty()
            if (sectionGames.isNotEmpty()) {
                item(key = "header-${section.name}") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            proSiegeSectionLabel(section),
                            modifier = Modifier.weight(1f),
                            color = GameColors.TextPrimary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Surface(shape = GameShapes.Pill, color = proSiegeSectionColor(section).copy(alpha = .12f)) {
                            Text(
                                sectionGames.size.toString(),
                                Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                color = proSiegeSectionColor(section),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
                items(sectionGames, key = { it.id }) { game ->
                    ProfessionalSiegeGameRow(
                        game = game,
                        opponent = proSiegeOpponentId(game, me)?.let(profiles::get),
                        me = me,
                        onClick = { onOpen(game) },
                    )
                }
            }
        }

        item { Spacer(Modifier.height(10.dp)) }
    }
}

@Composable
private fun ProfessionalSiegeMetric(
    value: String,
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = GameShapes.Medium,
        color = GameColors.PrimarySurface,
        border = BorderStroke(1.dp, GameColors.Border),
    ) {
        Column(
            Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, color = accent, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Text(
                label,
                color = GameColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProfessionalSiegeGameRow(
    game: WordSiegeGameDto,
    opponent: ProfileDto?,
    me: String?,
    onClick: () -> Unit,
) {
    val myOwner = proSiegeOwnerFor(game, me)
    val rivalOwner = if (myOwner == 1) 2 else 1
    val myWord = proSiegeWordScore(game, myOwner)
    val rivalWord = proSiegeWordScore(game, rivalOwner)
    val myArea = proSiegeArea(game, myOwner)
    val rivalArea = proSiegeArea(game, rivalOwner)
    val myTotal = WordSiegeFinalRules.currentTerritoryScore(myWord, myArea)
    val rivalTotal = WordSiegeFinalRules.currentTerritoryScore(rivalWord, rivalArea)
    val section = proSiegeSection(game, me)
    val accent = proSiegeSectionColor(section)

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = GameShapes.Large,
        color = GameColors.PrimarySurface,
        border = BorderStroke(1.dp, accent.copy(alpha = .32f)),
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfilePhotoAvatarWithGender(
                avatarPath = opponent?.avatarPath,
                gender = opponent?.gender,
                name = opponent?.displayName ?: sh("Rakip", "Rival"),
                size = 46.dp,
                accent = accent,
                visible = opponent?.avatarVisibility != "hidden",
            )
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    opponent?.displayName ?: if (game.status == "waiting") sh("Rakip aranıyor", "Finding a rival") else sh("Rakip", "Rival"),
                    color = GameColors.TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    proSiegeCardStatus(game, me),
                    color = accent,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (game.status != "waiting") {
                    Text(
                        sh(
                            "Sen $myTotal • Rakip $rivalTotal • ${game.moveCount} tur",
                            "You $myTotal • Rival $rivalTotal • ${game.moveCount} turns",
                        ),
                        color = GameColors.TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (game.status != "waiting") {
                    Text(
                        "$myTotal : $rivalTotal",
                        color = GameColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        sh("Toplam", "Total"),
                        color = GameColors.TextTertiary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = GameColors.TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ProfessionalSiegeConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    accent: androidx.compose.ui.graphics.Color,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GameColors.PrimarySurface,
        titleContentColor = GameColors.TextPrimary,
        textContentColor = GameColors.TextSecondary,
        title = { Text(title, fontWeight = FontWeight.Black) },
        text = { Text(body, color = GameColors.TextSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirm, color = accent, fontWeight = FontWeight.Black) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(sh("VAZGEÇ", "CANCEL"), color = GameColors.TextSecondary) }
        },
    )
}

@Composable
private fun ProfessionalSiegeChatDialog(
    messages: List<WordSiegeMessageDto>,
    me: String?,
    input: String,
    busy: Boolean,
    onInput: (String) -> Unit,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GameColors.PrimarySurface,
        titleContentColor = GameColors.TextPrimary,
        textContentColor = GameColors.TextSecondary,
        title = { Text(sh("SOHBET", "CHAT"), fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.heightIn(min = 220.dp, max = 430.dp)) {
                if (messages.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(sh("Henüz mesaj yok.", "No messages yet."), color = GameColors.TextSecondary, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(messages.takeLast(40), key = { it.id }) { message ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = if (message.senderId == me) Arrangement.End else Arrangement.Start,
                            ) {
                                Surface(
                                    color = if (message.senderId == me) {
                                        GameColors.PrimaryBlue.copy(alpha = .15f)
                                    } else {
                                        GameColors.SecondarySurface
                                    },
                                    shape = GameShapes.Medium,
                                ) {
                                    Text(
                                        message.body,
                                        Modifier.padding(9.dp),
                                        color = GameColors.TextPrimary,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (SonHarfCosmetics.emojiPackId == "emoji_vip") {
                    Text(sh("VIP TEPKİLER", "VIP REACTIONS"), color = GameColors.PrestigeGold, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(5.dp))
                    VipEmojiReactionRow(enabled = !busy, onSend = onSend)
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = onInput,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !busy,
                    placeholder = { Text(sh("Mesaj yaz…", "Type a message…")) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val text = input.trim()
                                if (text.isNotEmpty()) onSend(text)
                            },
                            enabled = input.isNotBlank() && !busy,
                        ) {
                            Icon(Icons.Rounded.Send, sh("Gönder", "Send"), tint = GameColors.PrimaryBlue)
                        }
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(sh("KAPAT", "CLOSE"), color = GameColors.PrimaryBlue) }
        },
    )
}

private fun proSiegeOwnerFor(game: WordSiegeGameDto, userId: String?): Int =
    if (userId == game.playerOneId) 1 else 2

private fun proSiegeOpponentId(game: WordSiegeGameDto, userId: String?): String? =
    if (userId == game.playerOneId) game.playerTwoId else game.playerOneId

private fun proSiegeRackFor(game: WordSiegeGameDto, userId: String?): String =
    if (userId == game.playerOneId) game.playerOneRack else game.playerTwoRack.orEmpty()

private fun proSiegeWordScore(game: WordSiegeGameDto, owner: Int): Int =
    if (owner == 1) game.playerOneWordScore else game.playerTwoWordScore

private fun proSiegeArea(game: WordSiegeGameDto, owner: Int): Int =
    if (owner == 1) game.playerOneArea else game.playerTwoArea

private fun proSiegeSection(game: WordSiegeGameDto, me: String?): ProfessionalSiegeSection = when {
    game.status == "waiting" -> ProfessionalSiegeSection.WAITING
    game.status == "finished" -> ProfessionalSiegeSection.FINISHED
    proSiegeSleeping(game) -> ProfessionalSiegeSection.SLEEPING
    game.currentPlayerId == me -> ProfessionalSiegeSection.YOUR_TURN
    else -> ProfessionalSiegeSection.OPPONENT
}

private fun proSiegeSleeping(game: WordSiegeGameDto): Boolean {
    val stamp = game.lastMoveAt ?: game.createdAt
    return runCatching { Duration.between(Instant.parse(stamp), Instant.now()).toDays() >= 7 }.getOrDefault(false)
}

@Composable
private fun proSiegeSectionLabel(section: ProfessionalSiegeSection): String = when (section) {
    ProfessionalSiegeSection.WAITING -> sh("Rakip Aranıyor", "Finding a Rival")
    ProfessionalSiegeSection.YOUR_TURN -> sh("Sıra Sende", "Your Turn")
    ProfessionalSiegeSection.OPPONENT -> sh("Rakipte", "Rival's Turn")
    ProfessionalSiegeSection.SLEEPING -> sh("Uyuyan Oyunlar", "Sleeping Games")
    ProfessionalSiegeSection.FINISHED -> sh("Biten Oyunlar", "Finished Games")
}

private fun proSiegeSectionColor(section: ProfessionalSiegeSection): androidx.compose.ui.graphics.Color = when (section) {
    ProfessionalSiegeSection.WAITING -> GameColors.RewardAmber
    ProfessionalSiegeSection.YOUR_TURN -> GameColors.PlayGreen
    ProfessionalSiegeSection.OPPONENT -> GameColors.Lavender
    ProfessionalSiegeSection.SLEEPING -> GameColors.TextTertiary
    ProfessionalSiegeSection.FINISHED -> GameColors.TacticalTurquoise
}

@Composable
private fun proSiegeCardStatus(game: WordSiegeGameDto, me: String?): String = when (proSiegeSection(game, me)) {
    ProfessionalSiegeSection.WAITING -> sh("Eşleşme bekliyor", "Waiting to match")
    ProfessionalSiegeSection.YOUR_TURN -> sh("Hamleni yap", "Make your move")
    ProfessionalSiegeSection.OPPONENT -> sh("Rakibin hamlesi bekleniyor", "Waiting for your rival")
    ProfessionalSiegeSection.SLEEPING -> sh("İstediğinde devam et", "Resume whenever you want")
    ProfessionalSiegeSection.FINISHED -> proSiegeStatusLabel(game, me)
}

@Composable
private fun proSiegeStatusLabel(game: WordSiegeGameDto, me: String?): String = when {
    game.status == "cancelled" -> sh("İptal edildi", "Cancelled")
    game.status != "finished" -> sh("Devam ediyor", "In progress")
    game.winnerId == null -> sh("Berabere", "Draw")
    game.winnerId == me -> sh("Kazandın", "You won")
    else -> sh("Rakip kazandı", "Rival won")
}
