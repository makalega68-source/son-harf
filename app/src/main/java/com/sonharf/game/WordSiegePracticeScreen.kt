package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    // Profile enrichment is optional. Core practice actions never require backend/network availability.
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
    var notice by remember { mutableStateOf<String?>(null) }
    var lastMove by remember { mutableStateOf<WordSiegePracticeMove?>(null) }
    var botThinking by remember { mutableStateOf(false) }
    var showPass by remember { mutableStateOf(false) }
    var showForfeit by remember { mutableStateOf(false) }
    var showRestart by remember { mutableStateOf(false) }
    var showExchange by remember { mutableStateOf(false) }
    var exchangeSelection by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var shuffleSeed by remember { mutableIntStateOf(0) }

    val playerTargetScore = WordSiegePracticeEngine.totalScore(state, 1)
    val botTargetScore = WordSiegePracticeEngine.totalScore(state, 2)
    var displayedPlayerScore by remember { mutableIntStateOf(playerTargetScore) }
    var displayedBotScore by remember { mutableIntStateOf(botTargetScore) }
    var displayedOwner by remember { mutableIntStateOf(state.currentOwner) }
    val canPlayerAct = dictionaryReady && state.status == "playing" && state.currentOwner == 1 && !botThinking
    val rackOrder = remember(state.playerRack, shuffleSeed) {
        if (shuffleSeed == 0) state.playerRack.indices.toList()
        else wordSiegeShuffledRackIndices(state.playerRack.length, shuffleSeed)
    }
    val readyFeedback = wordSiegeValidationFeedback(
        placementsCount = placements.size,
        turkish = !SonHarfUiState.isEnglish,
    )
    val previewCapturedCells = placements.keys.count { index -> state.board.getOrNull(index)?.owner != 1 }

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
                    notice = sh("Ana sözlük hazır. Çevrimdışı alıştırmada da aynı sözlük kullanılacak.", "Main dictionary ready. The same dictionary will be used for offline practice.")
                }
                .onFailure {
                    dictionaryReady = false
                    notice = sh("Ana sözlük yüklenemedi. Yenile düğmesine basıp tekrar dene.", "Main dictionary could not be loaded. Tap refresh to retry.")
                }
        }
        dictionaryLoading = false
    }

    LaunchedEffect(playerTargetScore, botTargetScore, state.currentOwner) {
        while (displayedPlayerScore != playerTargetScore || displayedBotScore != botTargetScore) {
            displayedPlayerScore += (playerTargetScore - displayedPlayerScore).coerceIn(-1, 1)
            displayedBotScore += (botTargetScore - displayedBotScore).coerceIn(-1, 1)
            delay(28)
        }
        displayedOwner = state.currentOwner
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
        notice = sh("İlk hamle sende. Ortadaki 2K karesinden geç.", "Your first move must cover the center 2W cell.")
        clearSelection()
    }

    fun applyPlayerMove() {
        if (!dictionaryReady) {
            notice = sh("Ana sözlük henüz hazır değil. Yenile düğmesine basıp tekrar dene.", "Main dictionary is not ready yet. Tap refresh and try again.")
            return
        }
        runCatching { WordSiegePracticeEngine.applyMove(state, 1, placements) }
            .onSuccess { (next, move) ->
                state = next
                lastMove = move
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

    LaunchedEffect(
        state.currentOwner,
        state.moveCount,
        state.status,
        displayedPlayerScore,
        displayedBotScore,
        displayedOwner,
        dictionaryReady,
        playerProfile?.rating,
        playerProfile?.wins,
        playerProfile?.losses,
    ) {
        if (!dictionaryReady || state.status != "playing" || state.currentOwner != 2) return@LaunchedEffect
        if (displayedPlayerScore != playerTargetScore || displayedBotScore != botTargetScore || displayedOwner != 2) return@LaunchedEffect
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
            notice = sh("${botProfile.name} ${move.primaryWord} oynadı • +${move.wordScore}", "${botProfile.name} played ${move.primaryWord} • +${move.wordScore}")
            SonHarfSoundFx.scoreTick()
        }
        botThinking = false
    }

    Surface(Modifier.fillMaxSize(), color = MainUi.Background) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            val compact = maxHeight < 700.dp
            val chromeHeight = if (compact) 226.dp else 246.dp
            val boardSize = minOf(maxWidth, (maxHeight - chromeHeight).coerceAtLeast(238.dp))

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onExit, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back"), tint = MainUi.Text)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(sh("KELİME KUŞATMASI", "WORD SIEGE"), color = MainUi.Text, fontSize = if (compact) 16.sp else 18.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        Text(
                            when {
                                dictionaryLoading -> sh("BOT İLE ALIŞTIRMA • ANA SÖZLÜK HAZIRLANIYOR", "BOT PRACTICE • LOADING MAIN DICTIONARY")
                                dictionaryReady -> sh("BOT İLE ALIŞTIRMA • ANA SÖZLÜK", "BOT PRACTICE • MAIN DICTIONARY")
                                else -> sh("BOT İLE ALIŞTIRMA • ANA SÖZLÜK GEREKLİ", "BOT PRACTICE • MAIN DICTIONARY REQUIRED")
                            },
                            color = MainUi.Blue, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1,
                        )
                    }
                    IconButton(onClick = { showForfeit = true }, enabled = state.status == "playing", modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Flag, sh("Pes et", "Forfeit"), tint = MainUi.Red)
                    }
                    IconButton(
                        onClick = {
                            if (!dictionaryReady) dictionaryRetryKey += 1
                            else if (state.moveCount > 0 || placements.isNotEmpty()) showRestart = true else startAgain()
                        },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Rounded.Refresh, sh("Yeni oyun", "New game"), tint = MainUi.Blue)
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WordSiegePracticeScoreCard(
                        name = playerProfile?.displayName ?: sh("SEN", "YOU"),
                        score = displayedPlayerScore,
                        area = state.playerArea,
                        accent = MainUi.Green,
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
                        area = state.botArea,
                        accent = MainUi.Red,
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
                    color = if (displayedOwner == 1) MainUi.Blue else SiegePurpleSoft,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (displayedOwner == 1) MainUi.Green.copy(alpha = .25f) else MainUi.Red.copy(alpha = .25f)),
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = if (compact) 5.dp else 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (botThinking) CircularProgressIndicator(Modifier.size(14.dp), color = SiegePurple, strokeWidth = 2.dp)
                        else Icon(if (displayedOwner == 1) Icons.Rounded.TouchApp else Icons.Rounded.SmartToy, null, tint = if (displayedOwner == 1) Color.White else MainUi.Red, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            when {
                                state.status == "finished" -> sh("ALIŞTIRMA BİTTİ", "PRACTICE FINISHED")
                                botThinking -> sh("${botProfile.name.uppercase()} HAMLESİNİ HAZIRLIYOR", "${botProfile.name.uppercase()} IS PREPARING A MOVE")
                                displayedOwner == 1 -> sh("SIRA SENDE • Harf seç, tahtaya bırak, OYNA", "YOUR TURN • Select tile, place it, PLAY")
                                else -> sh("${botProfile.name.uppercase()} OYNUYOR", "${botProfile.name.uppercase()} IS PLAYING")
                            },
                            color = if (displayedOwner == 1) Color.White else MainUi.Text,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                        )
                    }
                }

                Box(Modifier.fillMaxWidth().height(boardSize), contentAlignment = Alignment.Center) {
                    WordSiegePracticeBoard(
                        board = state.board,
                        rack = state.playerRack,
                        placements = placements,
                        myOwner = 1,
                        enabled = canPlayerAct,
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
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (placements.isNotEmpty()) {
                            Text(readyFeedback.message, color = MainUi.Green, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.width(5.dp))
                            Text(
                                sh(
                                    "Alan +${previewCapturedCells * WordSiegeFinalRules.CUBE_TRANSFER_POINTS}",
                                    "Area +${previewCapturedCells * WordSiegeFinalRules.CUBE_TRANSFER_POINTS}",
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
                                    selectedRackIndex = if (selectedRackIndex == rackIndex) null else rackIndex
                                },
                            )
                        }
                        repeat((7 - state.playerRack.length).coerceAtLeast(0)) { Spacer(Modifier.weight(1f).height(if (compact) 40.dp else 44.dp)) }
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
                            modifier = Modifier.weight(1f).height(34.dp),
                            contentPadding = PaddingValues(horizontal = 3.dp),
                        ) {
                            Icon(Icons.Rounded.Undo, null, Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(sh("GERİ AL", "UNDO"), fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                        OutlinedButton(
                            onClick = { shuffleSeed = if (shuffleSeed == Int.MAX_VALUE) 1 else shuffleSeed + 1 },
                            enabled = canPlayerAct && state.playerRack.length > 1,
                            modifier = Modifier.weight(1f).height(34.dp),
                            contentPadding = PaddingValues(horizontal = 3.dp),
                        ) {
                            Icon(Icons.Rounded.Shuffle, null, Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(sh("KARIŞTIR", "SHUFFLE"), fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        OutlinedButton(onClick = { showPass = true }, enabled = canPlayerAct, modifier = Modifier.weight(1f).height(if (compact) 40.dp else 43.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                            Text(sh("PAS", "PASS"), fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                        OutlinedButton(
                            onClick = { exchangeSelection = emptySet(); showExchange = true },
                            enabled = canPlayerAct && state.bag.isNotEmpty(),
                            modifier = Modifier.weight(1.25f).height(if (compact) 40.dp else 43.dp),
                            border = BorderStroke(1.dp, SiegePurple),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                        ) { Text(sh("DEĞİŞTİR", "EXCHANGE"), color = SiegePurple, fontSize = 8.sp, fontWeight = FontWeight.Black) }
                        Button(
                            onClick = ::applyPlayerMove,
                            enabled = canPlayerAct && placements.isNotEmpty(),
                            modifier = Modifier.weight(1.35f).height(if (compact) 40.dp else 43.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MainUi.Blue,
                                contentColor = Color.White,
                                disabledContainerColor = SonHarfTheme.DisabledBackground,
                                disabledContentColor = SonHarfTheme.DisabledContent,
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                        ) { Text(sh("OYNA", "PLAY"), fontSize = 10.sp, fontWeight = FontWeight.Black) }
                    }
                } else {
                    val won = state.winnerOwner == 1
                    val draw = state.winnerOwner == null
                    val color = when { won -> MainUi.Green; draw -> MainUi.Gold; else -> MainUi.Red }
                    Surface(Modifier.fillMaxWidth(), color = color.copy(alpha = .08f), shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp), border = BorderStroke(1.dp, color.copy(alpha = .35f))) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(if (won) sh("KUŞATMA SENİN!", "SIEGE WON!") else if (draw) sh("BERABERE", "DRAW") else sh("${botProfile.name.uppercase()} KAZANDI", "${botProfile.name.uppercase()} WON"), color = color, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                Text(sh("Yeni alıştırma ile tekrar dene.", "Try another practice round."), color = MainUi.Muted, fontSize = 8.sp)
                            }
                            TextButton(onClick = ::startAgain) { Text(sh("YENİ OYUN", "NEW GAME"), color = MainUi.Blue, fontWeight = FontWeight.Black, fontSize = 9.sp) }
                        }
                    }
                }

                notice?.let { message ->
                    WordSiegeNotice(message)
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

    if (showRestart) {
        AlertDialog(
            onDismissRequest = { showRestart = false },
            title = { Text(sh("Yeni oyun başlat?", "Start a new game?"), fontWeight = FontWeight.Black) },
            text = { Text(sh("Mevcut alıştırmadaki ilerleme sıfırlanacak.", "Current practice progress will be reset."), color = MainUi.Muted) },
            confirmButton = {
                TextButton(onClick = {
                    showRestart = false
                    startAgain()
                }) { Text(sh("YENİ OYUN", "NEW GAME"), color = MainUi.Blue, fontWeight = FontWeight.Black) }
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
        modifier = modifier,
        color = if (active) accent.copy(alpha = .09f) else MainUi.Surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) accent else MainUi.Border),
    ) {
        Row(Modifier.padding(horizontal = 7.dp, vertical = if (compact) 5.dp else 7.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfilePhotoAvatarWithGender(
                avatarPath = avatarPath,
                gender = gender,
                name = name,
                size = if (compact) 30.dp else 34.dp,
                accent = accent,
                visible = avatarVisible,
            )
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, color = MainUi.Text, fontWeight = FontWeight.Black, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (isBot) {
                        Spacer(Modifier.width(4.dp))
                        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(99.dp), color = accent.copy(alpha = .12f)) {
                            Text("BOT", Modifier.padding(horizontal = 4.dp, vertical = 1.dp), color = accent, fontSize = 6.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$score", color = accent, fontWeight = FontWeight.Black, fontSize = if (compact) 16.sp else 18.sp)
                    Spacer(Modifier.width(5.dp))
                    Text(sh("Alan $area", "Area $area"), color = MainUi.Muted, fontSize = 7.sp, maxLines = 1)
                }
            }
        }
    }
}
