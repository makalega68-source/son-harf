package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
internal fun WordSiegePracticeScreen(onExit: () -> Unit) {
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

    LaunchedEffect(playerTargetScore, botTargetScore) {
        while (displayedPlayerScore != playerTargetScore || displayedBotScore != botTargetScore) {
            displayedPlayerScore += (playerTargetScore - displayedPlayerScore).coerceIn(-1, 1)
            displayedBotScore += (botTargetScore - displayedBotScore).coerceIn(-1, 1)
            delay(28)
        }
    }

    fun clearSelection() {
        placements = emptyMap()
        selectedRackIndex = null
        exchangeSelection = emptySet()
    }

    fun startAgain() {
        state = WordSiegePracticeEngine.newGame()
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

    LaunchedEffect(state.currentOwner, state.moveCount, state.status) {
        if (state.status != "playing" || state.currentOwner != 2) return@LaunchedEffect
        botThinking = true
        delay(950)
        val planned = WordSiegePracticeEngine.bestBotMove(state)
        if (planned == null) {
            state = WordSiegePracticeEngine.pass(state, 2)
            notice = sh("Bot pas verdi. Sıra sende.", "Bot passed. Your turn.")
        } else {
            val (next, move) = WordSiegePracticeEngine.applyMove(state, 2, planned.placements)
            state = next
            lastMove = move
            notice = sh("Bot ${move.primaryWord} oynadı • +${move.wordScore}", "Bot played ${move.primaryWord} • +${move.wordScore}")
            SonHarfSoundFx.scoreTick()
        }
        botThinking = false
    }

    Surface(Modifier.fillMaxSize(), color = MainUi.Background) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onExit) { Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back"), tint = MainUi.Text) }
                    Column(Modifier.weight(1f)) {
                        Text(sh("KELİME KUŞATMASI", "WORD SIEGE"), color = MainUi.Text, fontSize = 19.sp, fontWeight = FontWeight.Black)
                        Text(sh("BOT İLE ALIŞTIRMA • ÇEVRİMDIŞI", "BOT PRACTICE • OFFLINE"), color = MainUi.Blue, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    IconButton(onClick = { showForfeit = true }, enabled = state.status == "playing") {
                        Icon(Icons.Rounded.Flag, sh("Pes et", "Forfeit"), tint = MainUi.Red)
                    }
                    IconButton(onClick = ::startAgain) { Icon(Icons.Rounded.Refresh, sh("Yeni oyun", "New game"), tint = MainUi.Blue) }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WordSiegePracticeScoreCard(sh("SEN", "YOU"), displayedPlayerScore, state.playerArea, MainUi.Green, active = state.currentOwner == 1, modifier = Modifier.weight(1f))
                    WordSiegePracticeScoreCard(sh("BOT", "BOT"), displayedBotScore, state.botArea, MainUi.Red, active = state.currentOwner == 2, modifier = Modifier.weight(1f))
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (state.currentOwner == 1) SiegeBlueSoft else SiegePurpleSoft,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (state.currentOwner == 1) MainUi.Blue.copy(alpha = .25f) else SiegePurple.copy(alpha = .25f)),
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (botThinking) CircularProgressIndicator(Modifier.size(16.dp), color = SiegePurple, strokeWidth = 2.dp)
                        else Icon(if (state.currentOwner == 1) Icons.Rounded.TouchApp else Icons.Rounded.SmartToy, null, tint = if (state.currentOwner == 1) MainUi.Blue else SiegePurple, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(
                            when {
                                state.status == "finished" -> sh("ALIŞTIRMA BİTTİ", "PRACTICE FINISHED")
                                botThinking -> sh("BOT HAMLESİNİ HAZIRLIYOR", "BOT IS PREPARING A MOVE")
                                state.currentOwner == 1 -> sh("SIRA SENDE • Harf seç, tahtaya bırak, OYNA", "YOUR TURN • Select tile, place it, PLAY")
                                else -> sh("BOTUN SIRASI", "BOT'S TURN")
                            },
                            color = MainUi.Text,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }

            item {
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

            if (state.status == "playing") {
                item {
                    Text(
                        sh("Yön otomatik algılanır • Torba ${state.bag.length}", "Direction is detected automatically • Bag ${state.bag.length}"),
                        color = MainUi.Muted,
                        fontSize = 9.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                        repeat((7 - state.playerRack.length).coerceAtLeast(0)) { Spacer(Modifier.weight(1f).height(48.dp)) }
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { showPass = true }, enabled = state.currentOwner == 1 && !botThinking, modifier = Modifier.weight(1f).height(45.dp)) {
                            Text(sh("PAS", "PASS"), fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                        OutlinedButton(
                            onClick = { exchangeSelection = emptySet(); showExchange = true },
                            enabled = state.currentOwner == 1 && !botThinking && state.bag.isNotEmpty(),
                            modifier = Modifier.weight(1.25f).height(45.dp),
                            border = BorderStroke(1.dp, SiegePurple),
                        ) { Text(sh("DEĞİŞTİR", "EXCHANGE"), color = SiegePurple, fontSize = 9.sp, fontWeight = FontWeight.Black) }
                        Button(
                            onClick = ::applyPlayerMove,
                            enabled = state.currentOwner == 1 && placements.isNotEmpty() && !botThinking,
                            modifier = Modifier.weight(1.35f).height(45.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MainUi.Blue),
                        ) { Text(sh("OYNA", "PLAY"), fontSize = 11.sp, fontWeight = FontWeight.Black) }
                    }
                }
            } else {
                item {
                    val won = state.winnerOwner == 1
                    val draw = state.winnerOwner == null
                    val color = when { won -> MainUi.Green; draw -> MainUi.Gold; else -> MainUi.Red }
                    Surface(Modifier.fillMaxWidth(), color = color.copy(alpha = .08f), shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp), border = BorderStroke(1.dp, color.copy(alpha = .35f))) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(if (won) sh("KUŞATMA SENİN!", "SIEGE WON!") else if (draw) sh("BERABERE", "DRAW") else sh("BOT KAZANDI", "BOT WON"), color = color, fontWeight = FontWeight.Black, fontSize = 17.sp)
                            Text(sh("Yeni bir alıştırma ile yeniden deneyebilirsin.", "Start a new practice round and try again."), color = MainUi.Muted, fontSize = 10.sp)
                            Button(onClick = ::startAgain, colors = ButtonDefaults.buttonColors(containerColor = MainUi.Blue)) { Text(sh("YENİ ALIŞTIRMA", "NEW PRACTICE"), fontWeight = FontWeight.Black) }
                        }
                    }
                }
            }

            notice?.let { message -> item { WordSiegeNotice(message) } }
            lastMove?.let { move ->
                item {
                    Text(
                        sh("Son hamle: ${move.formedWords.joinToString(" + ")} • +${move.wordScore} kelime • ${move.capturedCells} alan", "Last move: ${move.formedWords.joinToString(" + ")} • +${move.wordScore} word • ${move.capturedCells} territory"),
                        color = MainUi.Muted,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                Text(
                    sh("Alıştırmada yerleşim, bonus, alan ele geçirme, pas ve harf değiştirme çalışır. Çevrimiçi maçta kelimeler ayrıca sunucu sözlüğüyle doğrulanır.", "Practice includes placement, bonuses, territory capture, pass and exchange. Online matches also validate words on the server."),
                    color = MainUi.Muted,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
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
            text = { Text(sh("Bot bu alıştırmayı kazanır.", "Bot wins this practice round."), color = MainUi.Muted) },
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
    label: String,
    score: Int,
    area: Int,
    accent: Color,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = if (active) accent.copy(alpha = .09f) else MainUi.Surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) accent else MainUi.Border),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = androidx.compose.foundation.shape.CircleShape, color = accent.copy(alpha = .13f)) {
                Icon(if (label == sh("SEN", "YOU")) Icons.Rounded.Person else Icons.Rounded.SmartToy, null, Modifier.padding(7.dp).size(18.dp), tint = accent)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = MainUi.Text, fontWeight = FontWeight.Black, fontSize = 11.sp)
                Text("$score", color = accent, fontWeight = FontWeight.Black, fontSize = 19.sp)
                Text(sh("Alan $area • Küp başına ±2", "Area $area • ±2 per cube"), color = MainUi.Muted, fontSize = 7.sp, maxLines = 1)
            }
        }
    }
}
