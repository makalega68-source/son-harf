package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SharedDictionaryService
import kotlinx.coroutines.delay

private data class PracticeBotProfile(val name: String, val gender: String)

private val WordSiegePracticeBots = listOf(
    PracticeBotProfile("Mesut", "erkek"),
    PracticeBotProfile("İmran", "kadın"),
    PracticeBotProfile("Ayaz", "erkek"),
    PracticeBotProfile("Eren", "erkek"),
    PracticeBotProfile("Esin", "kadın"),
    PracticeBotProfile("Can", "erkek"),
    PracticeBotProfile("Deniz", "erkek"),
    PracticeBotProfile("Mert", "erkek"),
    PracticeBotProfile("Selin", "kadın"),
    PracticeBotProfile("Burak", "erkek"),
    PracticeBotProfile("Elif", "kadın"),
    PracticeBotProfile("Kerem", "erkek"),
    PracticeBotProfile("Derya", "kadın"),
    PracticeBotProfile("Arda", "erkek"),
    PracticeBotProfile("Zeynep", "kadın"),
    PracticeBotProfile("Emre", "erkek"),
    PracticeBotProfile("Ceren", "kadın"),
)

@Composable
internal fun WordSiegePracticeScreen(onExit: () -> Unit) {
    val backend = remember { runCatching { OnlineGameBackend() }.getOrNull() }
    val me = remember(backend) { backend?.currentUserId() }
    var playerProfile by remember { mutableStateOf<ProfileDto?>(null) }
    var botProfile by remember { mutableStateOf(WordSiegePracticeBots.random()) }
    var state by remember { mutableStateOf(WordSiegePracticeEngine.newGame()) }
    val context = LocalContext.current.applicationContext
    val haptics = LocalHapticFeedback.current
    var dictionaryReady by remember { mutableStateOf(SharedDictionaryService.hasSnapshot(state.language)) }
    var dictionaryLoading by remember { mutableStateOf(false) }
    var dictionaryRetryKey by remember { mutableIntStateOf(0) }
    var placements by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var selectedRackIndex by remember { mutableStateOf<Int?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var lastMove by remember { mutableStateOf<WordSiegePracticeMove?>(null) }
    var botThinking by remember { mutableStateOf(false) }
    var showPass by remember { mutableStateOf(false) }
    var showForfeit by remember { mutableStateOf(false) }
    var showRestart by remember { mutableStateOf(false) }
    var showExchange by remember { mutableStateOf(false) }
    var exchangeSelection by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var shuffleSeed by remember { mutableIntStateOf(0) }
    var actionVfxEvent by remember { mutableIntStateOf(0) }
    var turnSecondsLeft by remember { mutableIntStateOf(WordSiegeZoneRules.TurnSeconds) }

    val playerTargetScore = WordSiegePracticeEngine.totalScore(state, 1)
    val botTargetScore = WordSiegePracticeEngine.totalScore(state, 2)
    val displayedPlayerScore by animateIntAsState(playerTargetScore, tween(260), label = "practice-player-score")
    val displayedBotScore by animateIntAsState(botTargetScore, tween(260), label = "practice-bot-score")
    val displayedOwner = state.currentOwner
    val canPlayerAct = dictionaryReady && state.status == "playing" && state.currentOwner == 1 && !botThinking
    val rackOrder = remember(state.playerRack, shuffleSeed) {
        if (shuffleSeed == 0) state.playerRack.indices.toList()
        else wordSiegeShuffledRackIndices(state.playerRack.length, shuffleSeed)
    }
    val readyFeedback = wordSiegeValidationFeedback(
        placementsCount = placements.size,
        turkish = !SonHarfUiState.isEnglish,
    )
    val previewZoneIds = remember(state.board, placements) {
        WordSiegeZoneRules.touchedZoneIds(placements.keys)
            .filterTo(linkedSetOf()) { WordSiegeZoneRules.zoneOwner(state.board, it) != 1 }
    }
    val previewZoneScore = remember(state.board, placements) {
        WordSiegeZoneRules.previewZoneGain(state.board, 1, placements.keys)
    }

    LaunchedEffect(me, backend) {
        val b = backend ?: return@LaunchedEffect
        if (me != null) playerProfile = runCatching { b.getProfile(me) }.getOrNull()
    }

    LaunchedEffect(state.language, dictionaryRetryKey) {
        dictionaryLoading = true
        dictionaryReady = SharedDictionaryService.restorePersisted(context, state.language)
        if (!dictionaryReady) {
            runCatching { SharedDictionaryService.preloadCanonical(context, state.language) }
                .onSuccess {
                    dictionaryReady = true
                    notice = sh("Ana sözlük hazır.", "Main dictionary ready.")
                }
                .onFailure {
                    dictionaryReady = false
                    notice = sh("Ana sözlük yüklenemedi. Yenile ve tekrar dene.", "Main dictionary could not be loaded. Refresh and try again.")
                }
        }
        dictionaryLoading = false
    }

    fun clearSelection() {
        placements = emptyMap()
        selectedRackIndex = null
        exchangeSelection = emptySet()
    }

    fun startAgain() {
        state = WordSiegePracticeEngine.newGame(state.language)
        botProfile = WordSiegePracticeBots.random()
        lastMove = null
        shuffleSeed = 0
        actionVfxEvent = 0
        turnSecondsLeft = WordSiegeZoneRules.TurnSeconds
        notice = sh("İlk hamle sende. Ortadaki 2K karesinden geç.", "Your first move must cover the center 2W cell.")
        clearSelection()
    }

    fun applyPlayerMove() {
        if (!dictionaryReady) {
            notice = sh("Ana sözlük henüz hazır değil.", "Main dictionary is not ready yet.")
            return
        }
        runCatching { WordSiegePracticeEngine.applyMove(state, 1, placements) }
            .onSuccess { (next, move) ->
                state = next
                lastMove = move
                actionVfxEvent += 1
                notice = wordSiegePracticeMoveNotice(move, turkish = !SonHarfUiState.isEnglish)
                clearSelection()
                SonHarfSoundFx.wordAccepted()
            }
            .onFailure {
                notice = wordSiegeFriendlyError(it.message.orEmpty())
                SonHarfSoundFx.warning()
            }
    }

    BackHandler(onBack = onExit)

    LaunchedEffect(state.currentOwner, state.moveCount, state.status) {
        if (state.status != "playing") {
            turnSecondsLeft = 0
            return@LaunchedEffect
        }
        turnSecondsLeft = WordSiegeZoneRules.TurnSeconds
        while (turnSecondsLeft > 0 && state.status == "playing") {
            delay(1_000)
            turnSecondsLeft -= 1
            if (turnSecondsLeft == WordSiegeZoneRules.UrgentTurnSeconds && state.currentOwner == 1) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                SonHarfSoundFx.warning()
            }
        }
        if (turnSecondsLeft <= 0 && state.status == "playing" && state.currentOwner == 1) {
            state = WordSiegePracticeEngine.pass(state, 1)
            clearSelection()
            notice = sh("Süren doldu • otomatik pas", "Time expired • automatic pass")
        }
    }

    LaunchedEffect(
        state.currentOwner,
        state.moveCount,
        state.status,
        dictionaryReady,
        playerProfile?.rating,
        playerProfile?.wins,
        playerProfile?.losses,
    ) {
        if (!dictionaryReady || state.status != "playing" || state.currentOwner != 2) return@LaunchedEffect
        botThinking = true
        delay(950)
        val planned = WordSiegePracticeEngine.bestBotMove(
            state = state,
            playerRating = playerProfile?.rating ?: 1000,
            playerWins = playerProfile?.wins ?: 0,
            playerLosses = playerProfile?.losses ?: 0,
        )
        if (planned == null) {
            state = WordSiegePracticeEngine.pass(state, 2)
            notice = sh("${botProfile.name} pas verdi. Sıra sende.", "${botProfile.name} passed. Your turn.")
        } else {
            val (next, move) = WordSiegePracticeEngine.applyMove(state, 2, planned.placements)
            state = next
            lastMove = move
            actionVfxEvent += 1
            notice = wordSiegePracticeMoveNotice(move, turkish = !SonHarfUiState.isEnglish)
            SonHarfSoundFx.scoreTick()
        }
        botThinking = false
    }

    Surface(Modifier.fillMaxSize(), color = MainUi.Background) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 6.dp, vertical = 3.dp),
        ) {
            val compact = maxHeight < 700.dp
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onExit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back"), tint = MainUi.Text, modifier = Modifier.size(20.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                        Text(sh("KELİME KUŞATMASI", "WORD SIEGE"), color = MainUi.Text, fontSize = 14.sp, lineHeight = 15.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        Text(
                            when {
                                dictionaryLoading -> sh("BOT • SÖZLÜK HAZIRLANIYOR", "BOT • LOADING DICTIONARY")
                                dictionaryReady -> sh("BOT ALIŞTIRMA • 45 SN", "BOT PRACTICE • 45 S")
                                else -> sh("SÖZLÜK GEREKLİ", "DICTIONARY REQUIRED")
                            },
                            color = MainUi.Blue,
                            fontSize = 7.sp,
                            lineHeight = 8.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                        )
                    }
                    IconButton(
                        onClick = { shuffleSeed = if (shuffleSeed == Int.MAX_VALUE) 1 else shuffleSeed + 1 },
                        enabled = canPlayerAct && state.playerRack.length > 1,
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(Icons.Rounded.Shuffle, sh("Karıştır", "Shuffle"), tint = MainUi.Blue, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = {
                            if (!dictionaryReady) dictionaryRetryKey += 1
                            else if (state.moveCount > 0 || placements.isNotEmpty()) showRestart = true else startAgain()
                        },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(Icons.Rounded.Refresh, sh("Yeni oyun", "New game"), tint = MainUi.Blue, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { showForfeit = true }, enabled = state.status == "playing", modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Rounded.Flag, sh("Pes et", "Forfeit"), tint = MainUi.Red, modifier = Modifier.size(18.dp))
                    }
                }

                WordSiegeLiveRivalryBar(
                    myScore = displayedPlayerScore,
                    opponentScore = displayedBotScore,
                    modifier = Modifier.fillMaxWidth().height(24.dp).padding(horizontal = 6.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    WordSiegePracticeScoreCard(
                        name = playerProfile?.displayName ?: sh("SEN", "YOU"),
                        score = displayedPlayerScore,
                        area = state.playerArea,
                        areaScore = state.playerAreaScore,
                        conquestMeter = state.playerConquestMeter,
                        onslaughtActive = state.playerOnslaughtActive,
                        accent = MainUi.Green,
                        active = displayedOwner == 1,
                        avatarPath = playerProfile?.avatarPath,
                        gender = playerProfile?.gender,
                        avatarVisible = playerProfile?.avatarVisibility != "hidden",
                        isBot = false,
                        modifier = Modifier.weight(1f),
                    )
                    WordSiegePracticeScoreCard(
                        name = botProfile.name,
                        score = displayedBotScore,
                        area = state.botArea,
                        areaScore = state.botAreaScore,
                        conquestMeter = state.botConquestMeter,
                        onslaughtActive = state.botOnslaughtActive,
                        accent = MainUi.Red,
                        active = displayedOwner == 2,
                        avatarPath = null,
                        gender = botProfile.gender,
                        avatarVisible = true,
                        isBot = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                WordSiegeTempoBanner(
                    isMyTurn = displayedOwner == 1,
                    secondsLeft = turnSecondsLeft,
                    opponentLabel = botProfile.name,
                    modifier = Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 6.dp),
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 0.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    WordSiegePracticeBoard(
                        board = state.board,
                        rack = state.playerRack,
                        placements = placements,
                        myOwner = 1,
                        enabled = canPlayerAct,
                        moveEventKey = actionVfxEvent.takeIf { it > 0 },
                        resolvedIndices = lastMove?.placements?.keys ?: emptySet(),
                        modifier = Modifier.fillMaxSize(),
                        onCell = { boardIndex ->
                            if (!canPlayerAct) return@WordSiegePracticeBoard
                            if (placements.containsKey(boardIndex)) {
                                selectedRackIndex = placements.getValue(boardIndex)
                                placements = placements - boardIndex
                            } else if (state.board[boardIndex].letter == null) {
                                val rackIndex = selectedRackIndex ?: return@WordSiegePracticeBoard
                                if (rackIndex !in placements.values) {
                                    placements = placements + (boardIndex to rackIndex)
                                    selectedRackIndex = null
                                }
                            }
                        },
                    )
                }

                if (state.status == "playing") {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (placements.isNotEmpty()) {
                            Text(readyFeedback.message, color = MainUi.Green, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                sh("${previewZoneIds.size} bölge • +$previewZoneScore", "${previewZoneIds.size} zones • +$previewZoneScore"),
                                color = MainUi.Gold,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text(sh("Torba ${state.bag.length}", "Bag ${state.bag.length}"), color = MainUi.Muted, fontSize = 7.sp, maxLines = 1)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().height(if (compact) 52.dp else 56.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        rackOrder.forEach { rackIndex ->
                            val letter = state.playerRack.getOrNull(rackIndex) ?: return@forEach
                            WordSiegePracticeRackTile(
                                letter = letter,
                                selected = selectedRackIndex == rackIndex,
                                used = rackIndex in placements.values,
                                enabled = canPlayerAct,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val pending = placements.entries.firstOrNull { it.value == rackIndex }?.key
                                    if (pending != null) placements = placements - pending
                                    selectedRackIndex = if (selectedRackIndex == rackIndex) null else rackIndex
                                },
                            )
                        }
                        repeat((7 - state.playerRack.length).coerceAtLeast(0)) { Spacer(Modifier.weight(1f).fillMaxHeight()) }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                placements.keys.lastOrNull()?.let { boardIndex ->
                                    selectedRackIndex = placements[boardIndex]
                                    placements = wordSiegeUndoPendingPlacement(placements, boardIndex)
                                }
                            },
                            enabled = canPlayerAct && placements.isNotEmpty(),
                            modifier = Modifier.weight(.85f).fillMaxHeight(),
                            contentPadding = PaddingValues(horizontal = 2.dp),
                        ) {
                            Icon(Icons.Rounded.Undo, null, Modifier.size(12.dp))
                            Spacer(Modifier.width(2.dp))
                            Text(sh("GERİ", "UNDO"), fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                        OutlinedButton(
                            onClick = { showPass = true },
                            enabled = canPlayerAct,
                            modifier = Modifier.weight(.70f).fillMaxHeight(),
                            contentPadding = PaddingValues(horizontal = 2.dp),
                        ) { Text(sh("PAS", "PASS"), fontSize = 9.sp, fontWeight = FontWeight.Black) }
                        OutlinedButton(
                            onClick = { exchangeSelection = emptySet(); showExchange = true },
                            enabled = canPlayerAct && state.bag.isNotEmpty(),
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            border = BorderStroke(1.dp, SiegePurple),
                            contentPadding = PaddingValues(horizontal = 2.dp),
                        ) { Text(sh("DEĞİŞTİR", "EXCHANGE"), color = SiegePurple, fontSize = 8.sp, fontWeight = FontWeight.Black) }
                        Button(
                            onClick = ::applyPlayerMove,
                            enabled = canPlayerAct && placements.isNotEmpty(),
                            modifier = Modifier.weight(1.10f).fillMaxHeight(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MainUi.Blue,
                                contentColor = Color.White,
                                disabledContainerColor = SonHarfTheme.DisabledBackground,
                                disabledContentColor = SonHarfTheme.DisabledContent,
                            ),
                            contentPadding = PaddingValues(horizontal = 3.dp),
                        ) { Text(sh("OYNA", "PLAY"), fontSize = 11.sp, fontWeight = FontWeight.Black) }
                    }
                } else {
                    val won = state.winnerOwner == 1
                    val draw = state.winnerOwner == null
                    val color = when { won -> MainUi.Green; draw -> MainUi.Gold; else -> MainUi.Red }
                    Surface(
                        Modifier.fillMaxWidth().height(54.dp),
                        color = color.copy(alpha = .08f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, color.copy(alpha = .35f)),
                    ) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(if (won) sh("KUŞATMA SENİN!", "SIEGE WON!") else if (draw) sh("BERABERE", "DRAW") else sh("${botProfile.name.uppercase()} KAZANDI", "${botProfile.name.uppercase()} WON"), color = color, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                Text(sh("Kelime + mevcut bölge puanı", "Word + current zone score"), color = MainUi.Muted, fontSize = 8.sp)
                            }
                            TextButton(onClick = ::startAgain) { Text(sh("YENİ OYUN", "NEW GAME"), color = MainUi.Blue, fontWeight = FontWeight.Black, fontSize = 8.sp) }
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    notice?.let { message ->
                        Text(message, color = MainUi.Text, fontSize = 8.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    } ?: lastMove?.let { move ->
                        Text(
                            sh("Son: ${move.formedWords.joinToString(" + ")} • +${move.wordScore}", "Last: ${move.formedWords.joinToString(" + ")} • +${move.wordScore}"),
                            color = MainUi.Muted,
                            fontSize = 8.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }

    if (showRestart) {
        AlertDialog(
            onDismissRequest = { showRestart = false },
            title = { Text(sh("Yeni oyun başlat?", "Start a new game?"), fontWeight = FontWeight.Black) },
            text = { Text(sh("Mevcut alıştırmadaki ilerleme sıfırlanacak.", "Current practice progress will be reset."), color = MainUi.Muted) },
            confirmButton = {
                TextButton(onClick = { showRestart = false; startAgain() }) {
                    Text(sh("YENİ OYUN", "NEW GAME"), color = MainUi.Blue, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = { TextButton(onClick = { showRestart = false }) { Text(sh("VAZGEÇ", "CANCEL")) } },
        )
    }

    if (showPass) {
        AlertDialog(
            onDismissRequest = { showPass = false },
            title = { Text(sh("Turu geç?", "Pass this turn?"), fontWeight = FontWeight.Black) },
            text = { Text(sh("İki oyuncu art arda pas verirse alıştırma biter.", "Two consecutive passes end practice."), color = MainUi.Muted) },
            confirmButton = {
                TextButton(onClick = {
                    showPass = false
                    state = WordSiegePracticeEngine.pass(state, 1)
                    clearSelection()
                }) { Text(sh("PAS VER", "PASS"), color = MainUi.Gold, fontWeight = FontWeight.Black) }
            },
            dismissButton = { TextButton(onClick = { showPass = false }) { Text(sh("VAZGEÇ", "CANCEL")) } },
        )
    }

    if (showForfeit) {
        AlertDialog(
            onDismissRequest = { showForfeit = false },
            title = { Text(sh("Pes etmek istiyor musun?", "Do you want to forfeit?"), fontWeight = FontWeight.Black) },
            text = { Text(sh("${botProfile.name} bu alıştırmayı kazanır.", "${botProfile.name} wins this practice round."), color = MainUi.Muted) },
            confirmButton = {
                TextButton(onClick = { showForfeit = false; state = WordSiegePracticeEngine.forfeit(state, 1); clearSelection() }) {
                    Text(sh("PES ET", "FORFEIT"), color = MainUi.Red, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = { TextButton(onClick = { showForfeit = false }) { Text(sh("VAZGEÇ", "CANCEL")) } },
        )
    }

    if (showExchange) {
        AlertDialog(
            onDismissRequest = { showExchange = false },
            title = { Text(sh("HARF DEĞİŞTİR", "EXCHANGE TILES"), fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(sh("Seçtiğin harfler torbaya döner ve turun biter.", "Selected tiles return to the bag and your turn ends."), color = MainUi.Muted, fontSize = 12.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        state.playerRack.forEachIndexed { index, letter ->
                            WordSiegePracticeRackTile(letter, index in exchangeSelection, false, true, Modifier.weight(1f)) {
                                exchangeSelection = if (index in exchangeSelection) exchangeSelection - index else exchangeSelection + index
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = exchangeSelection.isNotEmpty() && exchangeSelection.size <= state.bag.length,
                    onClick = {
                        state = WordSiegePracticeEngine.exchange(state, 1, exchangeSelection)
                        showExchange = false
                        clearSelection()
                    },
                ) { Text(sh("DEĞİŞTİR", "EXCHANGE"), color = SiegePurple, fontWeight = FontWeight.Black) }
            },
            dismissButton = { TextButton(onClick = { showExchange = false }) { Text(sh("VAZGEÇ", "CANCEL")) } },
        )
    }
}

@Composable
private fun WordSiegePracticeScoreCard(
    name: String,
    score: Int,
    area: Int,
    areaScore: Int,
    conquestMeter: Int,
    onslaughtActive: Boolean,
    accent: Color,
    active: Boolean,
    avatarPath: String?,
    gender: String?,
    avatarVisible: Boolean,
    isBot: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = if (active) accent.copy(alpha = .10f) else MainUi.Surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(11.dp),
        border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) accent else MainUi.Border),
    ) {
        Row(Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfilePhotoAvatarWithGender(
                avatarPath = avatarPath,
                gender = gender,
                name = name,
                size = 30.dp,
                accent = accent,
                visible = avatarVisible,
            )
            Spacer(Modifier.width(5.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, color = MainUi.Text, fontWeight = FontWeight.Black, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (isBot) Text("BOT", color = accent, fontSize = 7.sp, fontWeight = FontWeight.Black)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$score", color = accent, fontWeight = FontWeight.Black, fontSize = 15.sp, maxLines = 1)
                    Spacer(Modifier.width(5.dp))
                    Text(sh("$area bölge • +$areaScore", "$area zones • +$areaScore"), color = MainUi.Muted, fontSize = 7.sp, maxLines = 1)
                }
                WordSiegeConquestMeter(conquestMeter, onslaughtActive)
            }
        }
    }
}
