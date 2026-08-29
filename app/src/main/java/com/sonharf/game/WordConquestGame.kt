package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.CoreWordCandidateDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.getCoreWordCandidates
import com.sonharf.game.data.validateCoreWord
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.random.Random

private enum class ConquestPhase { COUNTDOWN, PLAYING, FINISHED }

private data class ConquestBoard(val rows: List<String>) {
    val chars: List<Char>
        get() = rows.flatMap { row -> trNormConquest(row).toList() }
}

private data class ConquestCandidate(
    val word: String,
    val normalized: String,
    val path: List<Int>,
)

private val conquestBoards = listOf(
    ConquestBoard(listOf("KALEM", "SERİN", "BALIK", "TOPAK", "YUVAR")),
    ConquestBoard(listOf("ARABA", "DENİZ", "KİTAP", "OYUNC", "MASAL")),
    ConquestBoard(listOf("GÜNEŞ", "BULUT", "ORMAN", "DENİZ", "KARLI")),
    ConquestBoard(listOf("ELMAS", "KAPAK", "RENKL", "SOKAK", "YAZAR")),
)

private val conquestFallbackWords = listOf(
    "kalem","serin","balık","topak","yuvar","ara","kale","elma","bal","sarı","sal",
    "araba","deniz","kitap","oyun","masal","ara","aba","ada","naz",
    "güneş","bulut","orman","deniz","kar","dal","nar","nur",
    "elmas","kapak","sokak","yazar","yaka","yaz","azar","masa"
)

private fun trNormConquest(value: String): String =
    value.trim().lowercase(Locale.forLanguageTag("tr-TR"))

private fun adjacentBoardCells(a: Int, b: Int): Boolean {
    if (a == b || a !in 0..24 || b !in 0..24) return false
    val ar = a / 5
    val ac = a % 5
    val br = b / 5
    val bc = b % 5
    return abs(ar - br) <= 1 && abs(ac - bc) <= 1
}

private fun findBoardPath(board: List<Char>, rawWord: String): List<Int>? {
    val word = trNormConquest(rawWord)
    if (word.length !in 3..10 || board.size != 25) return null

    fun dfs(index: Int, at: Int, used: BooleanArray, path: MutableList<Int>): Boolean {
        if (board[index] != word[at]) return false
        used[index] = true
        path.add(index)
        if (at == word.lastIndex) return true

        val row = index / 5
        val col = index % 5
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val nr = row + dr
                val nc = col + dc
                if (nr !in 0..4 || nc !in 0..4) continue
                val next = nr * 5 + nc
                if (!used[next] && board[next] == word[at + 1]) {
                    if (dfs(next, at + 1, used, path)) return true
                }
            }
        }

        used[index] = false
        path.removeAt(path.lastIndex)
        return false
    }

    board.indices.filter { board[it] == word.first() }.forEach { start ->
        val used = BooleanArray(25)
        val path = mutableListOf<Int>()
        if (dfs(start, 0, used, path)) return path.toList()
    }
    return null
}

private fun selectionWord(board: List<Char>, path: List<Int>): String =
    path.joinToString("") { board[it].toString() }

@Composable
internal fun WordConquestGameScreen(onExit: () -> Unit) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    val tr = remember { Locale.forLanguageTag("tr-TR") }

    var gameId by remember { mutableIntStateOf(1) }
    val board = remember(gameId) { conquestBoards.random() }
    val boardChars = remember(gameId, board) { board.chars }
    val allLetters = remember(gameId, boardChars) { boardChars.joinToString("") }

    var phase by remember(gameId) { mutableStateOf(ConquestPhase.COUNTDOWN) }
    var countdown by remember(gameId) { mutableIntStateOf(3) }
    var secondsLeft by remember(gameId) { mutableIntStateOf(60) }
    var owners by remember(gameId) { mutableStateOf(List(25) { 0 }) }
    var selection by remember(gameId) { mutableStateOf<List<Int>>(emptyList()) }
    var input by remember(gameId) { mutableStateOf("") }
    var notice by remember(gameId) { mutableStateOf("") }
    var busy by remember(gameId) { mutableStateOf(false) }
    var myWords by remember(gameId) { mutableStateOf<List<String>>(emptyList()) }
    var botWords by remember(gameId) { mutableStateOf<List<String>>(emptyList()) }
    var candidates by remember(gameId) { mutableStateOf<List<ConquestCandidate>>(emptyList()) }

    BackHandler { onExit() }

    LaunchedEffect(gameId, allLetters) {
        val raw = runCatching {
            backend?.getCoreWordCandidates(allLetters, SonHarfUiState.language, 300).orEmpty()
        }.getOrDefault(emptyList())

        val source = if (raw.isNotEmpty()) raw else conquestFallbackWords.map {
            CoreWordCandidateDto(it, trNormConquest(it))
        }
        candidates = source
            .distinctBy { it.normalizedWord }
            .mapNotNull { item ->
                findBoardPath(boardChars, item.normalizedWord)?.let {
                    ConquestCandidate(item.word, item.normalizedWord, it)
                }
            }
            .filter { it.normalized.length >= 3 }
    }

    LaunchedEffect(gameId) {
        phase = ConquestPhase.COUNTDOWN
        countdown = 3
        repeat(3) {
            SonHarfSoundFx.countdown()
            delay(650)
            countdown -= 1
        }
        countdown = 0
        SonHarfSoundFx.softNotify()
        delay(420)
        phase = ConquestPhase.PLAYING

        while (secondsLeft > 0 && phase == ConquestPhase.PLAYING) {
            delay(1000)
            secondsLeft -= 1
            if (secondsLeft in 1..10) SonHarfSoundFx.countdown()
        }
        phase = ConquestPhase.FINISHED
        selection = emptyList()
        SonHarfSoundFx.softNotify()
    }

    LaunchedEffect(gameId, phase, candidates) {
        if (phase != ConquestPhase.PLAYING) return@LaunchedEffect
        while (phase == ConquestPhase.PLAYING && secondsLeft > 0) {
            val myTiles = owners.count { it == 1 }
            val botTiles = owners.count { it == 2 }
            val delayMs = when {
                botTiles < myTiles - 4 -> 2350L
                botTiles > myTiles + 6 -> 4300L
                secondsLeft <= 15 -> 2550L
                else -> 3400L
            }
            delay(delayMs + Random.nextLong(150L, 850L))
            if (phase != ConquestPhase.PLAYING || secondsLeft <= 0 || candidates.isEmpty()) continue

            val used = botWords.map(::trNormConquest).toSet()
            val available = candidates.filter { it.normalized !in used }
            if (available.isEmpty()) continue

            val ranked = available.sortedByDescending { candidate ->
                candidate.path.count { owners[it] == 1 } * 5 +
                    candidate.path.count { owners[it] == 0 } * 2 +
                    candidate.path.size
            }
            val choice = ranked.take(18.coerceAtMost(ranked.size)).random()
            owners = owners.toMutableList().also { next ->
                choice.path.forEach { next[it] = 2 }
            }
            botWords = botWords + choice.word
            SonHarfSoundFx.scoreTick()
        }
    }

    fun submitPath(path: List<Int>) {
        if (phase != ConquestPhase.PLAYING || busy) return
        val raw = selectionWord(boardChars, path).uppercase(tr)
        val norm = trNormConquest(raw)

        when {
            path.size < 3 -> {
                notice = "En az 3 harf bağla."
                selection = emptyList()
                input = ""
                SonHarfSoundFx.warning()
            }
            myWords.any { trNormConquest(it) == norm } -> {
                notice = "Bu kelimeyi kullandın."
                selection = emptyList()
                input = ""
                SonHarfSoundFx.warning()
            }
            else -> scope.launch {
                busy = true
                val valid = runCatching {
                    backend?.validateCoreWord(raw, SonHarfUiState.language) ?: false
                }.getOrElse {
                    candidates.any { it.normalized == norm }
                }
                if (!valid) {
                    notice = "Sözlükte yok."
                    SonHarfSoundFx.warning()
                } else {
                    owners = owners.toMutableList().also { next ->
                        path.forEach { next[it] = 1 }
                    }
                    myWords = myWords + raw
                    notice = "$raw • FETHEDİLDİ"
                    SonHarfSoundFx.wordAccepted()
                    SonHarfSoundFx.bonus()
                }
                input = ""
                selection = emptyList()
                busy = false
            }
        }
    }

    val myTiles = owners.count { it == 1 }
    val botTiles = owners.count { it == 2 }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.White, SonHarfBg, Color(0xFFF2F8F5)))
        )
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onExit) { Icon(Icons.Rounded.ArrowBack, "Geri") }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("KELİME FETHİ", color = SonHarfText, fontSize = 19.sp, fontWeight = FontWeight.Black)
                    Text("Kelime bul • kareleri ele geçir", color = SonHarfMuted, fontSize = 8.sp)
                }
                IconButton(onClick = { gameId += 1 }) { Icon(Icons.Rounded.Refresh, "Yenile") }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ConquestMiniScore("SEN", myTiles, SonHarfBlue, Modifier.weight(1f))
                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = CircleShape,
                    color = if (secondsLeft <= 10) SonHarfPink.copy(alpha = .12f) else SonHarfGreen.copy(alpha = .11f),
                    border = BorderStroke(2.dp, if (secondsLeft <= 10) SonHarfPink else SonHarfGreen),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(secondsLeft.toString(), color = SonHarfText, fontWeight = FontWeight.Black, fontSize = 23.sp)
                    }
                }
                ConquestMiniScore("BOT", botTiles, SonHarfPink, Modifier.weight(1f))
            }

            CompetitionLeadStrip(myScore = myTiles, opponentScore = botTiles)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = SonHarfGreen.copy(alpha = .09f),
                border = BorderStroke(1.dp, SonHarfGreen.copy(alpha = .25f)),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "HARFLERİN ÜZERİNDE PARMAĞINI SÜRÜKLE",
                        color = SonHarfGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        "Kelimeyi tamamlayınca parmağını bırak.",
                        color = SonHarfMuted,
                        fontSize = 8.sp,
                    )
                }
            }

            Box(
                Modifier.fillMaxWidth().aspectRatio(1f).pointerInput(gameId, phase, boardChars) {
                    if (phase != ConquestPhase.PLAYING) return@pointerInput
                    fun indexAt(x: Float, y: Float): Int {
                        val cellW = size.width / 5f
                        val cellH = size.height / 5f
                        val col = (x / cellW).toInt().coerceIn(0, 4)
                        val row = (y / cellH).toInt().coerceIn(0, 4)
                        return row * 5 + col
                    }

                    var dragPath = emptyList<Int>()
                    detectDragGestures(
                        onDragStart = { offset ->
                            val index = indexAt(offset.x, offset.y)
                            dragPath = listOf(index)
                            selection = dragPath
                            input = boardChars[index].toString().uppercase(tr)
                        },
                        onDrag = { change, _ ->
                            val index = indexAt(change.position.x, change.position.y)
                            val last = dragPath.lastOrNull()
                            if (last != null && index != last && index !in dragPath && adjacentBoardCells(last, index)) {
                                dragPath = dragPath + index
                                selection = dragPath
                                input = selectionWord(boardChars, dragPath).uppercase(tr)
                                SonHarfSoundFx.typingClick()
                            }
                        },
                        onDragEnd = {
                            val completedPath = dragPath
                            dragPath = emptyList()
                            submitPath(completedPath)
                        },
                        onDragCancel = {
                            dragPath = emptyList()
                            selection = emptyList()
                            input = ""
                        },
                    )
                },
            ) {
                Column(Modifier.fillMaxSize()) {
                    repeat(5) { row ->
                        Row(Modifier.fillMaxWidth().weight(1f)) {
                            repeat(5) { col ->
                                val index = row * 5 + col
                                val selected = index in selection
                                val owner = owners[index]
                                val accent = when {
                                    selected -> SonHarfGold
                                    owner == 1 -> SonHarfBlue
                                    owner == 2 -> SonHarfPink
                                    else -> Color(0xFFDDE5EE)
                                }
                                val fill = when {
                                    selected -> SonHarfGold.copy(alpha = .20f)
                                    owner == 1 -> SonHarfBlue.copy(alpha = .15f)
                                    owner == 2 -> SonHarfPink.copy(alpha = .14f)
                                    else -> Color.White
                                }
                                Surface(
                                    modifier = Modifier.weight(1f).fillMaxHeight().padding(2.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = fill,
                                    border = BorderStroke(if (selected) 2.dp else 1.dp, accent),
                                    shadowElevation = if (selected) 4.dp else 1.dp,
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            boardChars[index].toString().uppercase(tr),
                                            color = if (owner == 1) SonHarfBlue else if (owner == 2) SonHarfPink else SonHarfText,
                                            fontSize = 21.sp,
                                            fontWeight = FontWeight.Black,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (phase == ConquestPhase.FINISHED) {
                Surface(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    color = if (myTiles >= botTiles) Color(0xFFEAFBF0) else Color(0xFFFFEFF2),
                    border = BorderStroke(1.dp, if (myTiles >= botTiles) SonHarfGreen.copy(alpha = .30f) else SonHarfPink.copy(alpha = .30f)),
                ) {
                    Column(
                        Modifier.fillMaxSize().padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(if (myTiles > botTiles) "TAHTAYI FETHETTİN" else if (myTiles == botTiles) "BERABERE" else "BOT FETHETTİ", color = SonHarfText, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("$myTiles — $botTiles", color = SonHarfBlue, fontSize = 27.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { gameId += 1 }) { Text("RÖVANŞ") }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    color = Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (selection.isNotEmpty()) SonHarfGreen.copy(alpha = .45f) else SonHarfMuted.copy(alpha = .18f),
                    ),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            input.ifBlank { "Bir harften başla → sürükle" },
                            modifier = Modifier.weight(1f),
                            color = if (input.isBlank()) SonHarfMuted else SonHarfText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (input.isNotBlank()) {
                            Text(
                                input.length.toString() + " HARF",
                                color = SonHarfGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }

                if (notice.isNotBlank()) {
                    Text(
                        notice,
                        Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = if ("FETHEDİLDİ" in notice) SonHarfGreen else SonHarfPink,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        ModernCountdownOverlay(value = countdown, visible = phase == ConquestPhase.COUNTDOWN)
    }
}

@Composable
private fun ConquestMiniScore(label: String, score: Int, accent: Color, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(14.dp), color = accent.copy(alpha = .08f), border = BorderStroke(1.dp, accent.copy(alpha = .24f))) {
        Column(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black)
            Text(score.toString(), color = SonHarfText, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}
