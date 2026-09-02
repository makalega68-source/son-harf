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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
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
    val backend = remember { OnlineGameBackend() }
    val me = remember { backend.currentUserId() }
    var playerProfile by remember { mutableStateOf<ProfileDto?>(null) }
    var botProfile by remember { mutableStateOf(WordSiegePracticeBots.random()) }
    var state by remember { mutableStateOf(WordSiegePracticeEngine.newGame()) }
    var placements by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var selectedRackIndex by remember { mutableStateOf<Int?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var lastMove by remember { mutableStateOf<WordSiegePracticeMove?>(null) }
    var botThinking by remember { mutableStateOf(false) }
    var showPass by remember { mutableStateOf(false) }
    var showForfeit by remember { mutableStateOf(false) }
    var showExchange by remember { mutableStateOf(false) }
    var exchangeSelection by remember { mutableStateOf<Set<Int>>(emptySet()) }

    val playerTargetScore = WordSiegePracticeEngine.totalScore(state, 1)
    val botTargetScore = WordSiegePracticeEngine.totalScore(state, 2)
    var displayedPlayerScore by remember { mutableIntStateOf(playerTargetScore) }
    var displayedBotScore by remember { mutableIntStateOf(botTargetScore) }
    var displayedOwner by remember { mutableIntStateOf(state.currentOwner) }

    LaunchedEffect(me) {
        if (me != null) playerProfile = runCatching { backend.getProfile(me) }.getOrNull()
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
        notice = sh("İlk hamle sende. Ortadaki 2K karesinden geç.", "Your first move must cover the center 2W cell.")
        clearSelection()
    }

    fun applyPlayerMove() {
        runCatching { WordSiegePracticeEngine.applyMove(state, 1, placements) }
            .onSuccess { (next, move) ->
                state = next
                lastMove = move
                notice = sh("+${move.wordScore} kelime • ${move.capturedCells} küp • +${move.capturedCells * 2}/-${move.capturedCells * 2} transfer", "+${move.wordScore} word • ${move.capturedCells} cubes • +${move.capturedCells * 2}/-${move.capturedCells * 2} transfer")
                clearSelection()
                SonHarfSoundFx.wordAccepted()
            }
            .onFailure {
                notice = wordSiegeFriendlyError(it.message.orEmpty())
                SonHarfSoundFx.warning()
            }
    }

    BackHandler(onBack = onExit)

    LaunchedEffect(state.currentOwner, state.moveCount, state.status, displayedPlayerScore, displayedBotScore, displayedOwner) {
        if (state.status != "playing" || state.currentOwner != 2) return@LaunchedEffect
        if (displayedPlayerScore != playerTargetScore || displayedBotScore != botTargetScore || displayedOwner != 2) return@LaunchedEffect
        botThinking = true
        delay(950)
        val planned = WordSiegePracticeEngine.bestBotMove(state)
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
                    modifier = Modifier.fillMaxWidth().height(if (compact) 40.dp else 44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onExit, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back"), tint = MainUi.Text)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(sh("KELİME KUŞATMASI", "WORD SIEGE"), color = MainUi.Text, fontSize = if (compact) 16.sp else 18.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        Text(sh("BOT İLE ALIŞTIRMA • ÇEVRİMDIŞI", "BOT PRACTICE • OFFLINE"), color = MainUi.Blue, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    }
                    IconButton(onClick = { showForfeit = true }, enabled = state.status == "playing", modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Rounded.Flag, sh("Pes et", "Forfeit"), tint = MainUi.Red)
                    }
                    IconButton(onClick = ::startAgain, modifier = Modifier.size(38.dp)) {
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
                    color = if (displayedOwner == 1) SiegeBlueSoft else SiegePurpleSoft,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (displayedOwner == 1) MainUi.Green.copy(alpha = .25f) else MainUi.Red.copy(alpha = .25f)),
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = if (compact) 5.dp else 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (botThinking) CircularProgressIndicator(Modifier.size(14.dp), color = SiegePurple, strokeWidth = 2.dp)
                        else Icon(if (displayedOwner == 1) Icons.Rounded.TouchApp else Icons.Rounded.SmartToy, null, tint = if (displayedOwner == 1) MainUi.Green else MainUi.Red, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            when {
                                state.status == "finished" -> sh("ALIŞTIRMA BİTTİ", "PRACTICE FINISHED")
                                botThinking -> sh("${botProfile.name.uppercase()} HAMLESİNİ HAZIRLIYOR", "${botProfile.name.uppercase()} IS PREPARING A MOVE")
                                displayedOwner == 1 -> sh("SIRA SENDE • Harf seç, tahtaya bırak, OYNA", "YOUR TURN • Select tile, place it, PLAY")
                                else -> sh("${botProfile.name.uppercase()} OYNUYOR", "${botProfile.name.uppercase()} IS PLAYING")
                            },
                            color = MainUi.Text,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                        )
                    }
                }

                Box(Modifier.fillMaxWidth().height(boardSize), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(boardSize)) {
                        WordSiegeBoard(
                            board = state.board,
                            rack = state.playerRack,
                            placements = placements,
                            myOwner = 1,
                            enabled = state.status == "playing" && state.currentOwner == 1 && !botThinking,
                            onCell = { boardIndex ->
                                if (state.status != "playing" || state.currentOwner != 1 || botThinking) return@WordSiegeBoard
                                if (placements.containsKey(boardIndex)) {
                                    selectedRackIndex = placements.getValue(boardIndex)
                                    placements = placements - boardIndex
                                } else if (state.board[boardIndex].letter == null) {
                                    val rackIndex = selectedRackIndex ?: return@WordSiegeBoard
                                    if (rackIndex !in placements.values) {
                                        placements = placements + (boardIndex to rackIndex)
                                        selectedRackIndex = null
                                    }
                                }
                            },
                        )
                    }
                }

                if (state.status == "playing") {
                    Text(
                        sh("Yön otomatik algılanır • Torba ${state.bag.length}", "Direction is detected automatically • Bag ${state.bag.length}"),
                        color = MainUi.Muted,
                        fontSize = 8.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        maxLines = 1,
                    )

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        state.playerRack.forEachIndexed { index, letter ->
                            WordSiegeRackTile(
                                letter = letter,
                                selected = selectedRackIndex == index,
                                used = index in placements.values,
                                enabled = state.currentOwner == 1 && !botThinking,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val pending = placements.entries.firstOrNull { it.value == index }?.key
                                    if (pending != null) placements = placements - pending
                                    selectedRackIndex = if (selectedRackIndex == index) null else index
                                },
                            )
                        }
                        repeat((7 - state.playerRack.length).coerceAtLeast(0)) { Spacer(Modifier.weight(1f).height(if (compact) 40.dp else 44.dp)) }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        OutlinedButton(onClick = { showPass = true }, enabled = state.currentOwner == 1 && !botThinking, modifier = Modifier.weight(1f).height(if (compact) 40.dp else 43.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                            Text(sh("PAS", "PASS"), fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                        OutlinedButton(
                            onClick = { exchangeSelection = emptySet(); showExchange = true },
                            enabled = state.currentOwner == 1 && !botThinking && state.bag.isNotEmpty(),
                            modifier = Modifier.weight(1.25f).height(if (compact) 40.dp else 43.dp),
                            border = BorderStroke(1.dp, SiegePurple),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                        ) { Text(sh("DEĞİŞTİR", "EXCHANGE"), color = SiegePurple, fontSize = 8.sp, fontWeight = FontWeight.Black) }
                        Button(
                            onClick = ::applyPlayerMove,
                            enabled = state.currentOwner == 1 && placements.isNotEmpty() && !botThinking,
                            modifier = Modifier.weight(1.35f).height(if (compact) 40.dp else 43.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MainUi.Blue),
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
                            WordSiegeRackTile(letter, index in exchangeSelection, false, true, Modifier.weight(1f)) {
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
