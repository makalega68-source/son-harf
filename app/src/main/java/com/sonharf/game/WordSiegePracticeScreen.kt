package com.sonharf.game

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.validateWordSiegeDictionaryWord
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun WordSiegePracticeScreen(onExit: () -> Unit) {
    val scope = rememberCoroutineScope()
    val backend = remember { OnlineGameBackend() }
    var meProfile by remember { mutableStateOf<ProfileDto?>(null) }
    var state by remember { mutableStateOf(WordSiegePracticeEngine.newGame()) }
    var placements by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var selectedRackIndex by remember { mutableStateOf<Int?>(null) }
    var notice by remember { mutableStateOf(sh("İlk hamle ortadaki 2K karesinden geçmeli.", "Your first move must cover the center 2W cell.")) }
    var botThinking by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var showPass by remember { mutableStateOf(false) }
    var showForfeit by remember { mutableStateOf(false) }
    var showExchange by remember { mutableStateOf(false) }
    var showChatInfo by remember { mutableStateOf(false) }
    var exchangeSelection by remember { mutableStateOf<Set<Int>>(emptySet()) }

    fun clearSelection() {
        placements = emptyMap()
        selectedRackIndex = null
        exchangeSelection = emptySet()
    }

    fun startAgain() {
        state = WordSiegePracticeEngine.newGame()
        notice = sh("Yeni harfler dağıtıldı. İlk hamle ortadaki 2K karesinden geçmeli.", "New tiles dealt. Your first move must cover the center 2W cell.")
        clearSelection()
    }

    fun submitPlayerMove() {
        if (busy || state.currentOwner != 1 || placements.isEmpty()) return
        val direction = detectWordSiegeDirection(state.board, placements.keys)
        if (direction == null) {
            notice = sh("Harfleri aynı satır ya da sütuna yerleştir.", "Place tiles in the same row or column.")
            SonHarfSoundFx.warning()
            return
        }
        scope.launch {
            busy = true
            val horizontal = direction == WordSiegeDirection.HORIZONTAL
            val candidate = runCatching {
                WordSiegePracticeEngine.validateMove(state, 1, placements, horizontal) { true }
            }.getOrElse {
                notice = wordSiegeFriendlyError(it.message.orEmpty())
                SonHarfSoundFx.warning()
                busy = false
                return@launch
            }
            val validated = mutableSetOf<String>()
            var invalidWord: String? = null
            for (word in candidate.formedWords.distinct()) {
                var dictionaryError: Throwable? = null
                var allowed: Boolean? = null
                repeat(2) { attempt ->
                    if (allowed != null) return@repeat
                    runCatching { validateWordSiegeDictionaryWord(word, "tr") }
                        .onSuccess { allowed = it }
                        .onFailure { dictionaryError = it }
                    if (allowed == null && attempt == 0) delay(220)
                }
                if (allowed == null) {
                    notice = sh("Ana sözlüğe ulaşılamadı. Bağlantını kontrol edip tekrar dene.", "Could not reach the main dictionary. Check your connection and try again.")
                    SonHarfSoundFx.warning()
                    busy = false
                    return@launch
                }
                if (allowed == false) {
                    invalidWord = word
                    break
                }
                validated += word
            }
            if (invalidWord != null) {
                notice = sh("$invalidWord ana sözlükte bulunamadı.", "$invalidWord was not found in the main dictionary.")
                SonHarfSoundFx.warning()
                busy = false
                return@launch
            }
            runCatching {
                WordSiegePracticeEngine.applyMove(state, 1, placements, horizontal) { it in validated }
            }.onSuccess { (next, move) ->
                state = next
                notice = sh("${move.formedWords.joinToString(" + ")} ✓  • +${move.wordScore} puan", "${move.formedWords.joinToString(" + ")} ✓  • +${move.wordScore} points")
                clearSelection()
                SonHarfSoundFx.wordAccepted()
            }.onFailure {
                notice = wordSiegeFriendlyError(it.message.orEmpty())
                SonHarfSoundFx.warning()
            }
            busy = false
        }
    }

    BackHandler(onBack = onExit)

    LaunchedEffect(Unit) {
        val id = backend.currentUserId()
        if (id != null) meProfile = runCatching { backend.getProfile(id) }.getOrNull()
    }

    LaunchedEffect(state.currentOwner, state.moveCount, state.status) {
        if (state.status != "playing" || state.currentOwner != 2) return@LaunchedEffect
        botThinking = true
        delay(650)
        val planned = WordSiegePracticeEngine.bestBotMove(state)
        if (planned == null) {
            state = WordSiegePracticeEngine.pass(state, 2)
            notice = sh("Bot pas verdi. Sıra sende.", "Bot passed. Your turn.")
        } else {
            val (next, move) = WordSiegePracticeEngine.applyMove(state, 2, planned.placements, planned.horizontal)
            state = next
            notice = sh("Bot ${move.primaryWord} oynadı • +${move.wordScore}", "Bot played ${move.primaryWord} • +${move.wordScore}")
            SonHarfSoundFx.scoreTick()
        }
        botThinking = false
    }

    Surface(Modifier.fillMaxSize(), color = MainUi.Background) {
        BoxWithConstraints(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            val compact = maxHeight < 720.dp || maxWidth < 380.dp
            val gap = if (compact) 4.dp else 7.dp
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onExit, modifier = Modifier.size(if (compact) 38.dp else 42.dp)) {
                        Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back"), tint = MainUi.Text)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(sh("KELİME KUŞATMASI", "WORD SIEGE"), color = MainUi.Text, fontSize = if (compact) 16.sp else 18.sp, fontWeight = FontWeight.Black)
                        Text(sh("BOT ALIŞTIRMASI • ANA SÖZLÜK", "BOT PRACTICE • MAIN DICTIONARY"), color = MainUi.Blue, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                    IconButton(onClick = ::startAgain, modifier = Modifier.size(if (compact) 38.dp else 42.dp)) {
                        Icon(Icons.Rounded.Refresh, sh("Yeni oyun", "New game"), tint = MainUi.Blue)
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    PracticePlayerCard(
                        name = meProfile?.displayName ?: sh("SEN", "YOU"), avatarPath = meProfile?.avatarPath, gender = meProfile?.gender,
                        score = state.playerWordScore, area = state.playerArea, active = state.currentOwner == 1, accent = MainUi.Blue, modifier = Modifier.weight(1f),
                    )
                    PracticePlayerCard(
                        name = "BOT", avatarPath = null, gender = null, score = state.botWordScore, area = state.botArea,
                        active = state.currentOwner == 2, accent = SiegePurple, modifier = Modifier.weight(1f),
                    )
                }

                Surface(
                    Modifier.fillMaxWidth(),
                    color = if (state.currentOwner == 1) SiegeBlueSoft else SiegePurpleSoft,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, (if (state.currentOwner == 1) MainUi.Blue else SiegePurple).copy(alpha = .22f)),
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = if (compact) 5.dp else 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (botThinking || busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = if (botThinking) SiegePurple else MainUi.Blue)
                        else Icon(Icons.Rounded.TouchApp, null, tint = if (state.currentOwner == 1) MainUi.Blue else SiegePurple, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(
                            if (state.status != "playing") sh("MAÇ BİTTİ", "MATCH FINISHED")
                            else if (state.currentOwner == 1) sh("SIRA SENDE • Harf seç, tahtaya bırak, OYNA", "YOUR TURN • Pick a tile, place it, PLAY")
                            else sh("BOT DÜŞÜNÜYOR…", "BOT IS THINKING…"),
                            color = MainUi.Text, fontSize = if (compact) 10.sp else 11.sp, fontWeight = FontWeight.Black, maxLines = 1,
                        )
                    }
                }

                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val boardSize = minOf(maxWidth, maxHeight)
                    PracticeBoard(
                        state = state, placements = placements, selectedRackIndex = selectedRackIndex,
                        enabled = state.status == "playing" && state.currentOwner == 1 && !busy,
                        modifier = Modifier.size(boardSize),
                        onCellClick = { index ->
                            val placedRack = placements[index]
                            if (placedRack != null) {
                                placements = placements - index
                                selectedRackIndex = placedRack
                            } else if (state.board[index].letter == null) {
                                val rackIndex = selectedRackIndex ?: return@PracticeBoard
                                placements = placements.filterValues { it != rackIndex } + (index to rackIndex)
                                selectedRackIndex = null
                            }
                        },
                    )
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(sh("Torba ${state.bag.length}", "Bag ${state.bag.length}"), color = MainUi.Muted, fontSize = 10.sp)
                    Spacer(Modifier.weight(1f))
                    Text(sh("Yön otomatik", "Direction automatic"), color = MainUi.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                PracticeRack(
                    rack = state.playerRack,
                    placements = placements,
                    selected = selectedRackIndex,
                    enabled = state.currentOwner == 1 && state.status == "playing" && !busy,
                    onSelect = { rackIndex -> selectedRackIndex = if (selectedRackIndex == rackIndex) null else rackIndex },
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedButton(onClick = { showPass = true }, modifier = Modifier.weight(1f), enabled = state.currentOwner == 1 && state.status == "playing" && !busy) {
                        Text(sh("PAS", "PASS"), fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = { showExchange = true; exchangeSelection = emptySet() }, modifier = Modifier.weight(1f), enabled = state.currentOwner == 1 && state.status == "playing" && !busy && placements.isEmpty()) {
                        Text(sh("DEĞİŞTİR", "EXCHANGE"), fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                    Button(onClick = ::submitPlayerMove, modifier = Modifier.weight(1.15f), enabled = state.currentOwner == 1 && state.status == "playing" && placements.isNotEmpty() && !busy) {
                        Text(sh("OYNA", "PLAY"), fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    TextButton(onClick = { showForfeit = true }, modifier = Modifier.weight(1f), enabled = state.status == "playing") {
                        Icon(Icons.Rounded.Flag, null, Modifier.size(15.dp)); Spacer(Modifier.width(4.dp)); Text(sh("PES ET", "FORFEIT"), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { showChatInfo = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.ChatBubbleOutline, null, Modifier.size(15.dp)); Spacer(Modifier.width(4.dp)); Text(sh("SOHBET", "CHAT"), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                notice?.let {
                    Surface(Modifier.fillMaxWidth(), color = SiegePurpleSoft, shape = RoundedCornerShape(10.dp)) {
                        Text(it, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = MainUi.Text, fontSize = if (compact) 9.sp else 10.sp, maxLines = 2)
                    }
                }
            }
        }
    }

    if (showPass) AlertDialog(
        onDismissRequest = { showPass = false },
        title = { Text(sh("Pas ver?", "Pass?")) },
        text = { Text(sh("Sıra bota geçecek.", "The turn will pass to the bot.")) },
        confirmButton = { TextButton(onClick = { state = WordSiegePracticeEngine.pass(state, 1); clearSelection(); showPass = false }) { Text(sh("PAS", "PASS")) } },
        dismissButton = { TextButton(onClick = { showPass = false }) { Text(sh("VAZGEÇ", "CANCEL")) } },
    )

    if (showForfeit) AlertDialog(
        onDismissRequest = { showForfeit = false },
        title = { Text(sh("Pes etmek istiyor musun?", "Forfeit?")) },
        text = { Text(sh("Alıştırma maçı hemen sona erer.", "The practice match will end immediately.")) },
        confirmButton = { TextButton(onClick = { state = WordSiegePracticeEngine.forfeit(state, 1); clearSelection(); showForfeit = false }) { Text(sh("PES ET", "FORFEIT"), color = MainUi.Red) } },
        dismissButton = { TextButton(onClick = { showForfeit = false }) { Text(sh("VAZGEÇ", "CANCEL")) } },
    )

    if (showExchange) AlertDialog(
        onDismissRequest = { showExchange = false; exchangeSelection = emptySet() },
        title = { Text(sh("Harf değiştir", "Exchange tiles"), fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(sh("Değiştirmek istediğin harfleri aşağıdan seç. Torbada ${state.bag.length} harf var.", "Select the tiles to exchange below. The bag has ${state.bag.length} tiles."))
                ExchangeRackSelector(
                    rack = state.playerRack,
                    selected = exchangeSelection,
                    onToggle = { index -> exchangeSelection = if (index in exchangeSelection) exchangeSelection - index else exchangeSelection + index },
                )
                if (exchangeSelection.isNotEmpty()) Text(sh("${exchangeSelection.size} harf seçildi", "${exchangeSelection.size} tiles selected"), color = MainUi.Blue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    runCatching { WordSiegePracticeEngine.exchange(state, 1, exchangeSelection) }
                        .onSuccess { state = it; clearSelection(); notice = sh("Harfler değiştirildi.", "Tiles exchanged.") }
                        .onFailure { notice = wordSiegeFriendlyError(it.message.orEmpty()) }
                    showExchange = false
                },
                enabled = exchangeSelection.isNotEmpty(),
            ) { Text(sh("DEĞİŞTİR", "EXCHANGE"), fontWeight = FontWeight.Black) }
        },
        dismissButton = { TextButton(onClick = { showExchange = false; exchangeSelection = emptySet() }) { Text(sh("VAZGEÇ", "CANCEL")) } },
    )

    if (showChatInfo) AlertDialog(
        onDismissRequest = { showChatInfo = false },
        title = { Text(sh("Sohbet", "Chat")) },
        text = { Text(sh("Bot alıştırmasında sohbet kapalıdır. Çevrimiçi Kelime Kuşatması maçında SOHBET butonu gerçek maç sohbetini açar.", "Chat is disabled in bot practice. In an online Word Siege match, CHAT opens the real match chat.")) },
        confirmButton = { TextButton(onClick = { showChatInfo = false }) { Text(sh("TAMAM", "OK")) } },
    )
}

@Composable
private fun ExchangeRackSelector(rack: String, selected: Set<Int>, onToggle: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        rack.forEachIndexed { index, char ->
            val active = index in selected
            Surface(
                modifier = Modifier.weight(1f).aspectRatio(.82f).clickable { onToggle(index) },
                color = if (active) Color(0xFFFFE2A1) else MainUi.SurfaceSoft,
                shape = RoundedCornerShape(9.dp),
                border = BorderStroke(if (active) 2.dp else 1.dp, if (active) MainUi.Gold else MainUi.Border),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(char.toString(), color = MainUi.Text, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Text(WordSiegePracticeEngine.tileValue(char).toString(), modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp), color = MainUi.Muted, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PracticePlayerCard(
    name: String,
    avatarPath: String?,
    gender: String?,
    score: Int,
    area: Int,
    active: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(if (active) 2.dp else 1.dp, if (active) accent else MainUi.Border),
    ) {
        Row(Modifier.padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
            if (avatarPath != null) {
                ProfilePhotoAvatarRectWithGender(avatarPath, gender, name, width = 56.dp, height = 74.dp, accent = accent)
            } else {
                Surface(Modifier.size(56.dp, 74.dp), shape = RoundedCornerShape(14.dp), color = accent.copy(alpha = .10f)) {
                    Box(contentAlignment = Alignment.Center) { Text(name.take(3), color = accent, fontWeight = FontWeight.Black, fontSize = 12.sp) }
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(name, color = MainUi.Text, fontWeight = FontWeight.Black, fontSize = 12.sp, maxLines = 1)
                Text("$score", color = MainUi.Text, fontWeight = FontWeight.Black, fontSize = 21.sp)
                Text(sh("Alan $area", "Area $area"), color = MainUi.Muted, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun PracticeBoard(
    state: WordSiegePracticeState,
    placements: Map<Int, Int>,
    selectedRackIndex: Int?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onCellClick: (Int) -> Unit,
) {
    val rack = state.playerRack
    Surface(modifier, color = Color.White, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MainUi.Border)) {
        Column(Modifier.fillMaxSize().padding(3.dp)) {
            repeat(9) { row ->
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    repeat(9) { column ->
                        val index = row * 9 + column
                        val cell = state.board[index]
                        val tempRack = placements[index]
                        val letter = tempRack?.let { rack.getOrNull(it)?.toString() } ?: cell.letter
                        val owner = if (tempRack != null) 1 else cell.owner
                        val fill = when (owner) {
                            1 -> MainUi.Blue.copy(alpha = if (tempRack != null) .25f else .16f)
                            2 -> SiegePurple.copy(alpha = .18f)
                            else -> if (cell.bonus != null) MainUi.BlueSoft else Color(0xFFF9FBFD)
                        }
                        Box(
                            Modifier.weight(1f).fillMaxHeight().padding(1.dp).background(fill, RoundedCornerShape(5.dp)).then(if (enabled) Modifier.clickable { onCellClick(index) } else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (letter != null) Text(letter, color = MainUi.Text, fontWeight = FontWeight.Black, fontSize = 17.sp)
                            else if (cell.bonus != null) Text(cell.bonus, color = if (cell.bonus.endsWith("K")) SiegePurple else MainUi.Blue, fontWeight = FontWeight.Black, fontSize = 7.sp)
                            if (tempRack != null && tempRack == selectedRackIndex) Box(Modifier.fillMaxSize().padding(1.dp).background(MainUi.Blue.copy(alpha = .08f), RoundedCornerShape(5.dp)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PracticeRack(
    rack: String,
    placements: Map<Int, Int>,
    selected: Int?,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        rack.forEachIndexed { index, char ->
            val used = index in placements.values
            val active = index == selected
            Surface(
                modifier = Modifier.weight(1f).aspectRatio(.78f).then(if (enabled && !used) Modifier.clickable { onSelect(index) } else Modifier),
                color = when { used -> MainUi.SurfaceSoft; active -> Color(0xFFFFF0C9); else -> Color(0xFFFFE8B4) },
                shape = RoundedCornerShape(9.dp),
                border = BorderStroke(if (active) 2.dp else 1.dp, if (active) MainUi.Gold else MainUi.Gold.copy(alpha = .55f)),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(char.toString(), color = if (used) MainUi.Muted.copy(alpha = .45f) else MainUi.Text, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text(
                        WordSiegePracticeEngine.tileValue(char).toString(),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                        color = if (used) MainUi.Muted.copy(alpha = .30f) else MainUi.Muted,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
