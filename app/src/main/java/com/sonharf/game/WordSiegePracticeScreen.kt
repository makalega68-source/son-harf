package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    PracticeBotProfile("Mesut", "erkek"), PracticeBotProfile("İmran", "kadın"),
    PracticeBotProfile("Ayaz", "erkek"), PracticeBotProfile("Eren", "erkek"),
    PracticeBotProfile("Esin", "kadın"), PracticeBotProfile("Can", "erkek"),
    PracticeBotProfile("Deniz", "erkek"), PracticeBotProfile("Mert", "erkek"),
    PracticeBotProfile("Selin", "kadın"), PracticeBotProfile("Burak", "erkek"),
    PracticeBotProfile("Elif", "kadın"), PracticeBotProfile("Kerem", "erkek"),
    PracticeBotProfile("Derya", "kadın"), PracticeBotProfile("Arda", "erkek"),
    PracticeBotProfile("Zeynep", "kadın"), PracticeBotProfile("Emre", "erkek"),
    PracticeBotProfile("Ceren", "kadın"),
)

private val PracticePlayerAccent = Color(0xFF567A64)
private val PracticePlayerFill = Color(0xFFA8C7B1)
private val PracticeRivalAccent = Color(0xFF5C8299)
private val PracticeRivalFill = Color(0xFFAFCDE0)
private val PracticeNeutralFill = Color(0xFFE7E8E1)
private val PracticeSiegeWarm = Color(0xFFE3A64F)

@Composable
internal fun WordSiegePracticeScreen(
    onExit: () -> Unit,
    matchmakingFallback: Boolean = false,
) {
    // Profile enrichment is optional. Core practice/fallback actions never require backend availability.
    val backend = remember { runCatching { OnlineGameBackend() }.getOrNull() }
    val me = remember(backend) { backend?.currentUserId() }
    var playerProfile by remember { mutableStateOf<ProfileDto?>(null) }
    var botProfile by remember { mutableStateOf(WordSiegePracticeBots.random()) }
    var state by remember { mutableStateOf(WordSiegePracticeEngine.newGame()) }
    val context = LocalContext.current.applicationContext
    var dictionaryReady by remember { mutableStateOf(SharedDictionaryService.hasSnapshot(state.language)) }
    var dictionaryLoading by remember { mutableStateOf(false) }
    var dictionaryRetryKey by remember { mutableIntStateOf(0) }
    var placements by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var selectedRackIndex by remember { mutableStateOf<Int?>(null) }
    var notice by remember(matchmakingFallback) {
        mutableStateOf(
            if (matchmakingFallback) {
                sh(
                    "Geçici bot maçı başladı. Gerçek rakip araması arka planda sürüyor.",
                    "Temporary bot match started. Real matchmaking continues in the background.",
                )
            } else null,
        )
    }
    var lastMove by remember { mutableStateOf<WordSiegePracticeMove?>(null) }
    var botThinking by remember { mutableStateOf(false) }
    var showPass by remember { mutableStateOf(false) }
    var showForfeit by remember { mutableStateOf(false) }
    var showRestart by remember { mutableStateOf(false) }
    var showExchange by remember { mutableStateOf(false) }
    var exchangeSelection by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var shuffleSeed by remember { mutableIntStateOf(0) }
    var actionVfxEvent by remember { mutableIntStateOf(0) }
    var showSiegePulse by remember { mutableStateOf(false) }
    var zoneInfoCode by remember { mutableStateOf<String?>(null) }
    var tutorialStep by remember {
        mutableIntStateOf(
            if (!matchmakingFallback && !WordSiegePracticeTutorialPrefs.isCompleted(context)) 0 else -1,
        )
    }

    val playerTargetScore = WordSiegePracticeEngine.totalScore(state, 1)
    val botTargetScore = WordSiegePracticeEngine.totalScore(state, 2)
    val displayedPlayerScore by animateIntAsState(playerTargetScore, tween(260), label = "practice-player-score")
    val displayedBotScore by animateIntAsState(botTargetScore, tween(260), label = "practice-bot-score")
    val displayedOwner = state.currentOwner
    // Tile selection and board placement must stay responsive even while the dictionary snapshot is warming up.
    // Dictionary readiness is enforced only when the player submits the move.
    val canPlayerAct = state.status == "playing" && state.currentOwner == 1 && !botThinking
    val rackOrder = remember(state.playerRack, shuffleSeed) {
        if (shuffleSeed == 0) state.playerRack.indices.toList()
        else wordSiegeShuffledRackIndices(state.playerRack.length, shuffleSeed)
    }
    val readyFeedback = wordSiegeValidationFeedback(
        placementsCount = placements.size,
        turkish = !SonHarfUiState.isEnglish,
    )
    val previewCapturedCells = placements.keys.count { index -> state.board.getOrNull(index)?.owner != 1 }
    val playerTerritoryPoints = WordSiegeFinalRules.cubeTransfer(state.playerArea)
    val botTerritoryPoints = WordSiegeFinalRules.cubeTransfer(state.botArea)
    val playerMapControl = ((state.playerArea * 100f) / WordSiegeBoardSpec.CellCount).toInt().coerceIn(0, 100)
    val botMapControl = ((state.botArea * 100f) / WordSiegeBoardSpec.CellCount).toInt().coerceIn(0, 100)
    val latestAreaPoints = (lastMove?.capturedCells ?: 0) * WordSiegeFinalRules.CUBE_TRANSFER_POINTS

    LaunchedEffect(me, backend) {
        val b = backend ?: return@LaunchedEffect
        if (me != null) playerProfile = runCatching { b.getProfile(me) }.getOrNull()
    }

    LaunchedEffect(state.language, dictionaryRetryKey) {
        dictionaryLoading = true
        val restored = SharedDictionaryService.restorePersisted(context, state.language)
        dictionaryReady = restored
        runCatching { SharedDictionaryService.preloadCanonical(context, state.language) }
            .onSuccess {
                dictionaryReady = true
                if (!restored) {
                    notice = if (matchmakingFallback) {
                        sh(
                            "Sözlük hazır. Bot maçı sürerken gerçek rakip araması devam ediyor.",
                            "Dictionary ready. Real matchmaking continues during the bot match.",
                        )
                    } else {
                        sh(
                            "Sözlük hazır. İlk hamlede merkezden geç.",
                            "Dictionary ready. Your first move must cross the Crown Zone.",
                        )
                    }
                }
            }
            .onFailure {
                dictionaryReady = restored
                if (!restored) {
                    notice = sh(
                        "Sözlük yüklenemedi. Yenile düğmesine basıp tekrar dene.",
                        "Dictionary could not be loaded. Tap refresh and try again.",
                    )
                }
            }
        dictionaryLoading = false
    }

    LaunchedEffect(actionVfxEvent, latestAreaPoints) {
        if (actionVfxEvent > 0 && latestAreaPoints > 0) {
            showSiegePulse = true
            delay(1_300)
            showSiegePulse = false
        }
    }

    fun clearSelection() {
        placements = emptyMap()
        selectedRackIndex = null
        exchangeSelection = emptySet()
    }

    fun completeTutorial() {
        WordSiegePracticeTutorialPrefs.markCompleted(context)
        tutorialStep = -1
    }

    fun startAgain() {
        state = WordSiegePracticeEngine.newGame(state.language)
        botProfile = WordSiegePracticeBots.random()
        lastMove = null
        shuffleSeed = 0
        actionVfxEvent = 0
        notice = if (matchmakingFallback) {
            sh(
                "Yeni bot maçı başladı. Gerçek rakip araması sürüyor.",
                "New bot match started. Real matchmaking is still running.",
            )
        } else {
            sh(
                "İlk hamle sende. Kelimeni merkezden geçir.",
                "Your first move is yours. Cross the Crown Zone.",
            )
        }
        clearSelection()
    }

    fun applyPlayerMove() {
        if (!dictionaryReady) {
            if (!dictionaryLoading) dictionaryRetryKey += 1
            notice = sh(
                "Sözlük hazırlanıyor. Harflerini yerleştirebilirsin; doğrulama hazır olduğunda hamleni onayla.",
                "Dictionary is preparing. You can place tiles now and confirm once validation is ready.",
            )
            return
        }
        runCatching { WordSiegePracticeEngine.applyMove(state, 1, placements) }
            .onSuccess { (next, move) ->
                state = next
                lastMove = move
                actionVfxEvent += 1
                val areaPoints = move.capturedCells * WordSiegeFinalRules.CUBE_TRANSFER_POINTS
                notice = sh(
                    "${move.primaryWord} • Kelime +${move.wordScore} • Bölge +$areaPoints",
                    "${move.primaryWord} • Word +${move.wordScore} • Territory +$areaPoints",
                )
                if (tutorialStep == 3) tutorialStep = 4
                clearSelection()
                SonHarfSoundFx.wordAccepted()
            }
            .onFailure {
                notice = wordSiegeFriendlyError(it.message.orEmpty())
                SonHarfSoundFx.warning()
            }
    }

    BackHandler(onBack = onExit)

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
        try {
            delay(950)
            val planned = resilientPracticeBotMove(
                state = state,
                playerRating = playerProfile?.rating ?: 1000,
                playerWins = playerProfile?.wins ?: 0,
                playerLosses = playerProfile?.losses ?: 0,
            )
            if (planned == null) {
                val exchange = practiceBotExchangeIndices(state)
                if (exchange.isNotEmpty()) {
                    state = WordSiegePracticeEngine.exchange(state, 2, exchange)
                    notice = sh(
                        "${botProfile.name} uygun hamle bulamadı; 3 harf değiştirdi. Sıra sende.",
                        "${botProfile.name} found no legal move and exchanged 3 tiles. Your turn.",
                    )
                } else {
                    state = WordSiegePracticeEngine.pass(state, 2)
                    notice = sh(
                        "${botProfile.name} oynayacak hamle bulamadı ve pas verdi. Sıra sende.",
                        "${botProfile.name} found no legal move and passed. Your turn.",
                    )
                }
            } else {
                val (next, move) = WordSiegePracticeEngine.applyMove(state, 2, planned.placements)
                state = next
                lastMove = move
                actionVfxEvent += 1
                val areaPoints = move.capturedCells * WordSiegeFinalRules.CUBE_TRANSFER_POINTS
                notice = sh(
                    "${botProfile.name}: ${move.primaryWord} • Kelime +${move.wordScore} • Bölge +$areaPoints",
                    "${botProfile.name}: ${move.primaryWord} • Word +${move.wordScore} • Territory +$areaPoints",
                )
                SonHarfSoundFx.scoreTick()
            }
        } finally {
            botThinking = false
        }
    }

    Surface(Modifier.fillMaxSize(), color = MainUi.Background) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            val compact = maxHeight < 700.dp || maxWidth < 600.dp

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(if (compact) 46.dp else 52.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onExit, modifier = Modifier.size(if (compact) 40.dp else 46.dp)) {
                        Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back"), tint = MainUi.Text)
                    }
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            sh("KELİME TAHTI", "WORD THRONE"),
                            color = MainUi.Text,
                            fontSize = if (compact) 16.sp else 18.sp,
                            lineHeight = if (compact) 18.sp else 21.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                        )
                        Text(
                            when {
                                matchmakingFallback && dictionaryReady -> sh("BOT MAÇI • GERÇEK RAKİP ARANIYOR", "BOT MATCH • FINDING REAL RIVAL")
                                matchmakingFallback -> sh("BOT MAÇI", "BOT MATCH")
                                dictionaryLoading -> sh("ALIŞTIRMA • HAZIRLANIYOR", "PRACTICE • PREPARING")
                                else -> sh("ALIŞTIRMA", "PRACTICE")
                            },
                            color = PracticePlayerAccent,
                            fontSize = if (compact) 8.sp else 9.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                        )
                    }
                    if (!matchmakingFallback) {
                        IconButton(
                            onClick = { tutorialStep = 0 },
                            modifier = Modifier.size(if (compact) 38.dp else 44.dp),
                        ) {
                            Icon(Icons.Rounded.HelpOutline, sh("Nasıl oynanır?", "How to play?"), tint = PracticePlayerAccent)
                        }
                    }
                    IconButton(onClick = { showForfeit = true }, enabled = state.status == "playing", modifier = Modifier.size(if (compact) 40.dp else 46.dp)) {
                        Icon(Icons.Rounded.Flag, sh("Pes et", "Forfeit"), tint = MainUi.Red)
                    }
                    IconButton(
                        onClick = {
                            if (!dictionaryReady) dictionaryRetryKey += 1
                            else if (state.moveCount > 0 || placements.isNotEmpty()) showRestart = true else startAgain()
                        },
                        modifier = Modifier.size(if (compact) 40.dp else 46.dp),
                    ) {
                        Icon(Icons.Rounded.Refresh, sh("Yeni oyun", "New game"), tint = MainUi.Blue)
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    WordSiegePracticeScoreCard(
                        name = playerProfile?.displayName ?: sh("SEN", "YOU"),
                        score = displayedPlayerScore,
                        wordPoints = state.playerWordScore,
                        territoryPoints = playerTerritoryPoints,
                        area = state.playerArea,
                        accent = PracticePlayerAccent,
                        active = displayedOwner == 1,
                        compact = compact,
                        avatarPath = playerProfile?.avatarPath,
                        gender = playerProfile?.gender,
                        avatarVisible = playerProfile?.avatarVisibility != "hidden",
                        isBot = false,
                        modifier = Modifier.weight(1f),
                    )
                    WordSiegePracticeScoreCard(
                        name = botProfile.name,
                        score = displayedBotScore,
                        wordPoints = state.botWordScore,
                        territoryPoints = botTerritoryPoints,
                        area = state.botArea,
                        accent = PracticeRivalAccent,
                        active = displayedOwner == 2,
                        compact = compact,
                        avatarPath = null,
                        gender = botProfile.gender,
                        avatarVisible = true,
                        isBot = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (displayedOwner == 1) PracticePlayerAccent else PracticeRivalFill.copy(alpha = .42f),
                    shape = RoundedCornerShape(11.dp),
                    border = BorderStroke(1.dp, if (displayedOwner == 1) PracticePlayerAccent else PracticeRivalAccent),
                ) {
                    Row(
                        Modifier.padding(horizontal = 9.dp, vertical = if (compact) 3.dp else 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (botThinking) CircularProgressIndicator(Modifier.size(14.dp), color = PracticeRivalAccent, strokeWidth = 2.dp)
                        else Icon(
                            if (displayedOwner == 1) Icons.Rounded.TouchApp else Icons.Rounded.SmartToy,
                            null,
                            tint = if (displayedOwner == 1) Color.White else PracticeRivalAccent,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            when {
                                state.status == "finished" && matchmakingFallback -> sh("BOT MAÇI BİTTİ • RAKİP ARAMASI SÜRÜYOR", "BOT MATCH FINISHED • MATCHMAKING CONTINUES")
                                state.status == "finished" -> sh("ALIŞTIRMA BİTTİ", "PRACTICE FINISHED")
                                botThinking -> sh("${botProfile.name.uppercase()} HAMLESİNİ HAZIRLIYOR", "${botProfile.name.uppercase()} IS PREPARING A MOVE")
                                displayedOwner == 1 -> sh("SIRA SENDE • Kelimeni oluştur", "YOUR TURN • Build your word")
                                else -> sh("${botProfile.name.uppercase()} OYNUYOR", "${botProfile.name.uppercase()} IS PLAYING")
                            },
                            color = if (displayedOwner == 1) Color.White else MainUi.Text,
                            fontSize = if (compact) 11.sp else 13.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
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
                                    if (tutorialStep == 2) tutorialStep = 3
                                }
                            }
                        },
                    )

                    if (tutorialStep >= 0 && !matchmakingFallback) {
                        Box(
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                        ) {
                            WordSiegePracticeTutorialCard(
                                step = tutorialStep,
                                compact = compact,
                                onStart = { tutorialStep = 1 },
                                onFinish = ::completeTutorial,
                                onSkip = ::completeTutorial,
                            )
                        }
                    }
                }

                if (state.status == "playing") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (placements.isNotEmpty()) {
                            Text(readyFeedback.message, color = PracticePlayerAccent, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.width(5.dp))
                            Text(
                                sh(
                                    "Bölge +${previewCapturedCells * WordSiegeFinalRules.CUBE_TRANSFER_POINTS}",
                                    "Territory +${previewCapturedCells * WordSiegeFinalRules.CUBE_TRANSFER_POINTS}",
                                ),
                                color = MainUi.Muted,
                                fontSize = 8.sp,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text(sh("Torba ${state.bag.length}", "Bag ${state.bag.length}"), color = MainUi.Muted, fontSize = 8.sp, maxLines = 1)
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
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
                                    val selecting = selectedRackIndex != rackIndex
                                    selectedRackIndex = if (selecting) rackIndex else null
                                    if (selecting && tutorialStep == 1) tutorialStep = 2
                                },
                            )
                        }
                        repeat((7 - state.playerRack.length).coerceAtLeast(0)) {
                            Spacer(Modifier.weight(1f).height(if (compact) 40.dp else 44.dp))
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        OutlinedButton(
                            onClick = {
                                placements.keys.lastOrNull()?.let { boardIndex ->
                                    selectedRackIndex = placements[boardIndex]
                                    placements = wordSiegeUndoPendingPlacement(placements, boardIndex)
                                }
                            },
                            enabled = canPlayerAct && placements.isNotEmpty(),
                            modifier = Modifier.weight(1f).height(40.dp),
                            contentPadding = PaddingValues(horizontal = 3.dp),
                        ) {
                            Icon(Icons.Rounded.Undo, null, Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(sh("GERİ AL", "UNDO"), fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                        OutlinedButton(
                            onClick = { shuffleSeed = if (shuffleSeed == Int.MAX_VALUE) 1 else shuffleSeed + 1 },
                            enabled = canPlayerAct && state.playerRack.length > 1,
                            modifier = Modifier.weight(1f).height(40.dp),
                            contentPadding = PaddingValues(horizontal = 3.dp),
                        ) {
                            Icon(Icons.Rounded.Shuffle, null, Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(sh("KARIŞTIR", "SHUFFLE"), fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        OutlinedButton(
                            onClick = { showPass = true },
                            enabled = canPlayerAct,
                            modifier = Modifier.weight(.75f).height(if (compact) 40.dp else 45.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                        ) {
                            Text(sh("PAS", "PASS"), fontSize = if (compact) 11.sp else 12.sp, fontWeight = FontWeight.Black)
                        }
                        OutlinedButton(
                            onClick = { exchangeSelection = emptySet(); showExchange = true },
                            enabled = canPlayerAct && state.bag.isNotEmpty(),
                            modifier = Modifier.weight(1f).height(if (compact) 40.dp else 45.dp),
                            border = BorderStroke(1.dp, SiegePurple),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                        ) {
                            Text(sh("DEĞİŞTİR", "EXCHANGE"), color = SiegePurple, fontSize = if (compact) 10.sp else 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Button(
                            onClick = ::applyPlayerMove,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFE9D9A5), Color(0xFFAF8C45), Color(0xFFF6EAC7)))),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp),
                            enabled = canPlayerAct && placements.isNotEmpty(),
                            modifier = Modifier.weight(1f).height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PracticePlayerAccent,
                                contentColor = Color.White,
                                disabledContainerColor = SonHarfTheme.DisabledBackground,
                                disabledContentColor = SonHarfTheme.DisabledContent,
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                        ) {
                            Text(sh("HAMLEYİ ONAYLA", "CONFIRM MOVE"), fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    }
                } else {
                    val won = state.winnerOwner == 1
                    val draw = state.winnerOwner == null
                    val color = when { won -> PracticePlayerAccent; draw -> MainUi.Gold; else -> PracticeRivalAccent }
                    Surface(
                        Modifier.fillMaxWidth(),
                        color = color.copy(alpha = .08f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, color.copy(alpha = .35f)),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (won) sh("TAHT SENİN!", "THE THRONE IS YOURS!")
                                    else if (draw) sh("BERABERE", "DRAW")
                                    else sh("${botProfile.name.uppercase()} KAZANDI", "${botProfile.name.uppercase()} WON"),
                                    color = color,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                )
                                Text(
                                    sh("Kelime ve bölge puanların ayrı hesaplandı.", "Word and territory points were scored separately."),
                                    color = MainUi.Muted,
                                    fontSize = 8.sp,
                                )
                            }
                            TextButton(onClick = ::startAgain) {
                                Text(sh("YENİ OYUN", "NEW GAME"), color = MainUi.Blue, fontWeight = FontWeight.Black, fontSize = 9.sp)
                            }
                        }
                    }
                }

                val statusMessage = notice ?: lastMove?.let { move ->
                    sh(
                        "Son: ${move.formedWords.joinToString(" + ")} • Kelime +${move.wordScore} • Bölge +${move.capturedCells * WordSiegeFinalRules.CUBE_TRANSFER_POINTS}",
                        "Last: ${move.formedWords.joinToString(" + ")} • Word +${move.wordScore} • Territory +${move.capturedCells * WordSiegeFinalRules.CUBE_TRANSFER_POINTS}",
                    )
                } ?: sh(
                    "Harf seç → boş hücreye yerleştir → kelimeyi tamamla → HAMLEYİ ONAYLA",
                    "Pick a tile → place it → complete a word → CONFIRM MOVE",
                )
                WordSiegePracticeStatusBar(statusMessage, compact)
            }
        }
    }

    if (showRestart) {
        AlertDialog(
            onDismissRequest = { showRestart = false },
            title = { Text(sh("Yeni oyun başlat?", "Start a new game?"), fontWeight = FontWeight.Black) },
            text = {
                Text(
                    if (matchmakingFallback) {
                        sh("Bot maçı sıfırlanacak. Gerçek rakip araması devam edecek.", "The bot match will reset. Real matchmaking will continue.")
                    } else {
                        sh("Mevcut alıştırmadaki ilerleme sıfırlanacak.", "Current practice progress will be reset.")
                    },
                    color = MainUi.Muted,
                )
            },
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
            text = {
                Text(
                    sh("İki oyuncu art arda pas verirse maç biter.", "Two consecutive passes end the match."),
                    color = MainUi.Muted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPass = false
                    state = WordSiegePracticeEngine.pass(state, 1)
                    clearSelection()
                }) {
                    Text(sh("PAS VER", "PASS"), color = MainUi.Gold, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = { TextButton(onClick = { showPass = false }) { Text(sh("VAZGEÇ", "CANCEL")) } },
        )
    }

    if (showForfeit) {
        AlertDialog(
            onDismissRequest = { showForfeit = false },
            title = { Text(sh("Pes etmek istiyor musun?", "Do you want to forfeit?"), fontWeight = FontWeight.Black) },
            text = { Text(sh("Bu maçı rakibin kazanır.", "Your rival wins this match."), color = MainUi.Muted) },
            confirmButton = {
                TextButton(onClick = {
                    showForfeit = false
                    state = WordSiegePracticeEngine.forfeit(state, 1)
                    clearSelection()
                }) {
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
                    Text(
                        sh("Seçtiğin harfler torbaya döner ve turun biter.", "Selected tiles return to the bag and your turn ends."),
                        color = MainUi.Muted,
                        fontSize = 12.sp,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        state.playerRack.forEachIndexed { index, letter ->
                            WordSiegePracticeRackTile(
                                letter,
                                index in exchangeSelection,
                                false,
                                true,
                                Modifier.weight(1f),
                            ) {
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
                ) {
                    Text(sh("DEĞİŞTİR", "EXCHANGE"), color = SiegePurple, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = { TextButton(onClick = { showExchange = false }) { Text(sh("VAZGEÇ", "CANCEL")) } },
        )
    }

    zoneInfoCode?.let { code ->
        WordSiegePracticeZoneInfoDialog(
            code = code,
            onDismiss = { zoneInfoCode = null },
        )
    }
}

@Composable
private fun WordSiegePracticeScoreCard(
    name: String,
    score: Int,
    wordPoints: Int,
    territoryPoints: Int,
    area: Int,
    accent: Color,
    active: Boolean,
    compact: Boolean,
    avatarPath: String?,
    gender: String?,
    avatarVisible: Boolean,
    isBot: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(if (compact) 64.dp else 78.dp),
        color = if (active) accent.copy(alpha = .09f) else MainUi.Surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) accent else MainUi.Border),
    ) {
        Row(
            Modifier.padding(horizontal = 7.dp, vertical = if (compact) 3.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfilePhotoAvatarWithGender(
                avatarPath = avatarPath,
                gender = gender,
                name = name,
                size = if (compact) 36.dp else 44.dp,
                accent = accent,
                visible = avatarVisible,
            )
            Spacer(Modifier.width(if (compact) 5.dp else 6.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        name,
                        color = MainUi.Text,
                        fontWeight = FontWeight.Black,
                        fontSize = if (compact) 11.sp else 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isBot) {
                        Spacer(Modifier.width(4.dp))
                        Surface(shape = RoundedCornerShape(99.dp), color = accent.copy(alpha = .12f)) {
                            Text("BOT", Modifier.padding(horizontal = 5.dp, vertical = 1.dp), color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$score", color = accent, fontWeight = FontWeight.Black, fontSize = if (compact) 19.sp else 23.sp)
                    Spacer(Modifier.width(5.dp))
                    Text(sh("toplam", "total"), color = MainUi.Muted, fontSize = 7.sp, modifier = Modifier.padding(bottom = 3.dp))
                }
                Text(
                    sh("Kelime $wordPoints • Bölge $territoryPoints", "Word $wordPoints • Territory $territoryPoints"),
                    color = MainUi.Muted,
                    fontSize = if (compact) 7.sp else 8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
