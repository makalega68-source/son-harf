package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import kotlin.math.min
import kotlin.random.Random

private data class BotDuelWord(
    val word: String,
    val normalized: String,
    val points: Int,
    val combo: Int,
)

private enum class BotDuelPhase { COUNTDOWN, PLAYING, FINISHED }

private val botDuelLetterSets = listOf(
    "aelrmtskin",
    "aerltmison",
    "aeilmnorst",
    "aelrktsuin",
    "aeiklnrst",
    "aerlnmikt",
    "aeılnrstok",
    "aegilnrstu",
)

private val fallbackBotWords = listOf(
    "masa","masa","kalem","kale","alem","elma","selam","salim","liman","iman","mina",
    "tren","terim","metin","serin","resim","isim","sinema","roman","orman","onarım","nar",
    "salon","simit","sim","taksi","rast","risk","kart","kira","kral","sır","sıra","arı","ara",
    "kasa","kasa","kara","kare","krem","kira","kır","kir","tar","tam","mat","mart","martı",
    "usta","sarı","soru","suna","su","tur","tura","tuna","gül","gün","nur","run","kil","kin"
)

private fun trNorm(value: String): String =
    value.trim().lowercase(Locale.forLanguageTag("tr-TR"))

private fun fitsLettersLocal(word: String, letters: String): Boolean {
    val need = trNorm(word).groupingBy { it }.eachCount()
    val have = trNorm(letters).groupingBy { it }.eachCount()
    return need.all { (ch, count) -> count <= (have[ch] ?: 0) }
}

private fun duelBasePoints(length: Int, combo: Int): Int =
    length + ((length - 4).coerceAtLeast(0) * 2) + min((combo - 1).coerceAtLeast(0), 4)

private fun finalDuelScore(own: List<BotDuelWord>, other: List<BotDuelWord>): Int {
    val otherSet = other.map { it.normalized }.toSet()
    return own.sumOf { it.points + if (it.normalized !in otherSet) it.points else 0 }
}

@Composable
internal fun WordDuelBotScreen(
    onExit: () -> Unit,
) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var gameId by remember { mutableIntStateOf(1) }
    val letters = remember(gameId) { botDuelLetterSets.random() }

    var phase by remember(gameId) { mutableStateOf(BotDuelPhase.COUNTDOWN) }
    var countdown by remember(gameId) { mutableIntStateOf(3) }
    var secondsLeft by remember(gameId) { mutableIntStateOf(60) }
    var input by remember(gameId) { mutableStateOf("") }
    var myWords by remember(gameId) { mutableStateOf<List<BotDuelWord>>(emptyList()) }
    var botWords by remember(gameId) { mutableStateOf<List<BotDuelWord>>(emptyList()) }
    var candidates by remember(gameId) { mutableStateOf<List<CoreWordCandidateDto>>(emptyList()) }
    var notice by remember(gameId) { mutableStateOf("") }
    var busy by remember(gameId) { mutableStateOf(false) }
    var myCombo by remember(gameId) { mutableIntStateOf(0) }
    var botCombo by remember(gameId) { mutableIntStateOf(0) }
    var lastMyAcceptedAt by remember(gameId) { mutableLongStateOf(0L) }
    var lastBotAcceptedAt by remember(gameId) { mutableLongStateOf(0L) }

    BackHandler { onExit() }

    LaunchedEffect(gameId, letters) {
        candidates = runCatching {
            backend?.getCoreWordCandidates(letters, SonHarfUiState.language, 240).orEmpty()
        }.getOrDefault(emptyList())
        if (candidates.isEmpty()) {
            candidates = fallbackBotWords
                .map { CoreWordCandidateDto(it, trNorm(it)) }
                .filter { fitsLettersLocal(it.normalizedWord, letters) && it.normalizedWord.length >= 3 }
                .distinctBy { it.normalizedWord }
        }
    }

    LaunchedEffect(gameId) {
        phase = BotDuelPhase.COUNTDOWN
        countdown = 3
        repeat(3) {
            SonHarfSoundFx.countdown()
            delay(650)
            countdown -= 1
        }
        countdown = 0
        SonHarfSoundFx.softNotify()
        delay(420)
        phase = BotDuelPhase.PLAYING

        while (secondsLeft > 0 && phase == BotDuelPhase.PLAYING) {
            delay(1000)
            secondsLeft -= 1
            if (secondsLeft in 1..10) SonHarfSoundFx.countdown()
        }
        phase = BotDuelPhase.FINISHED
        SonHarfSoundFx.softNotify()
    }

    LaunchedEffect(gameId, phase, candidates) {
        if (phase != BotDuelPhase.PLAYING || candidates.isEmpty()) return@LaunchedEffect
        while (phase == BotDuelPhase.PLAYING && secondsLeft > 0) {
            val myScore = myWords.sumOf { it.points }
            val botScore = botWords.sumOf { it.points }
            val baseDelay = when {
                botScore < myScore - 8 -> 2300L
                botScore > myScore + 12 -> 4300L
                secondsLeft <= 15 -> 2500L
                else -> 3300L
            }
            delay(baseDelay + Random.nextLong(150L, 850L))
            if (phase != BotDuelPhase.PLAYING || secondsLeft <= 0) break

            val used = botWords.map { it.normalized }.toSet()
            val available = candidates.filter { it.normalizedWord !in used }
            if (available.isEmpty()) continue

            val candidate = when {
                botScore < myScore -> available.sortedByDescending { it.normalizedWord.length }.take(24).random()
                else -> available.shuffled().take(40).random()
            }
            val now = System.currentTimeMillis()
            botCombo = if (now - lastBotAcceptedAt <= 8000L) (botCombo + 1).coerceAtMost(9) else 1
            lastBotAcceptedAt = now
            val points = duelBasePoints(candidate.normalizedWord.length, botCombo)
            botWords = botWords + BotDuelWord(candidate.word, candidate.normalizedWord, points, botCombo)
            SonHarfSoundFx.scoreTick()
        }
    }

    fun submitWord() {
        if (phase != BotDuelPhase.PLAYING || busy) return
        val raw = input.trim()
        val norm = trNorm(raw)
        when {
            norm.length < 3 -> {
                notice = "En az 3 harf."
                SonHarfSoundFx.warning()
            }
            !fitsLettersLocal(norm, letters) -> {
                notice = "Sadece verilen harfleri kullan."
                SonHarfSoundFx.warning()
            }
            myWords.any { it.normalized == norm } -> {
                notice = "Bu kelimeyi kullandın."
                SonHarfSoundFx.warning()
            }
            else -> scope.launch {
                busy = true
                val valid = runCatching {
                    backend?.validateCoreWord(raw, SonHarfUiState.language) ?: false
                }.getOrElse {
                    candidates.any { it.normalizedWord == norm }
                }
                if (!valid) {
                    notice = "Sözlükte yok."
                    SonHarfSoundFx.warning()
                } else {
                    val now = System.currentTimeMillis()
                    myCombo = if (now - lastMyAcceptedAt <= 8000L) (myCombo + 1).coerceAtMost(9) else 1
                    lastMyAcceptedAt = now
                    val points = duelBasePoints(norm.length, myCombo)
                    myWords = myWords + BotDuelWord(raw.uppercase(Locale.forLanguageTag("tr-TR")), norm, points, myCombo)
                    input = ""
                    notice = if (myCombo >= 2) "+$points • ×$myCombo" else "+$points"
                    SonHarfSoundFx.wordAccepted()
                    if (myCombo >= 2) SonHarfSoundFx.bonus()
                }
                busy = false
            }
        }
    }

    val myBase = myWords.sumOf { it.points }
    val botBase = botWords.sumOf { it.points }
    val myFinal = finalDuelScore(myWords, botWords)
    val botFinal = finalDuelScore(botWords, myWords)

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.White, SonHarfBg, Color(0xFFF2F6FF)))
        )
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onExit) { Icon(Icons.Rounded.ArrowBack, "Geri") }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("KELİME DÜELLOSU", color = SonHarfText, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("BOT ANTRENMANI", color = SonHarfBlue, fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
                IconButton(onClick = { gameId += 1 }) { Icon(Icons.Rounded.Refresh, "Yenile") }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                DuelMiniScore("SEN", if (phase == BotDuelPhase.FINISHED) myFinal else myBase, SonHarfBlue, Modifier.weight(1f))
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = if (secondsLeft <= 10) SonHarfPink.copy(alpha = .12f) else SonHarfBlue.copy(alpha = .10f),
                    border = BorderStroke(2.dp, if (secondsLeft <= 10) SonHarfPink else SonHarfBlue),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            secondsLeft.toString(),
                            color = if (secondsLeft <= 10) SonHarfPink else SonHarfText,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
                DuelMiniScore("BOT", if (phase == BotDuelPhase.FINISHED) botFinal else botBase, SonHarfPink, Modifier.weight(1f))
            }

            CompetitionLeadStrip(
                myScore = if (phase == BotDuelPhase.FINISHED) myFinal else myBase,
                opponentScore = if (phase == BotDuelPhase.FINISHED) botFinal else botBase,
                myAction = myWords.lastOrNull()?.word,
                opponentAction = botWords.lastOrNull()?.word,
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(letters.toList()) { letter ->
                    Surface(
                        modifier = Modifier.size(39.dp),
                        shape = RoundedCornerShape(11.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, SonHarfBlue.copy(alpha = .25f)),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(letter.uppercase(), color = SonHarfBlue, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                    }
                }
            }

            if (phase == BotDuelPhase.FINISHED) {
                Surface(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    shape = RoundedCornerShape(22.dp),
                    color = if (myFinal >= botFinal) Color(0xFFEAFBF0) else Color(0xFFFFEFF2),
                    border = BorderStroke(1.dp, if (myFinal >= botFinal) SonHarfGreen.copy(.3f) else SonHarfPink.copy(.3f)),
                ) {
                    Column(
                        Modifier.fillMaxSize().padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(if (myFinal > botFinal) "KAZANDIN" else if (myFinal == botFinal) "BERABERE" else "BOT KAZANDI", fontSize = 25.sp, fontWeight = FontWeight.Black, color = SonHarfText)
                        Text("$myFinal — $botFinal", fontSize = 28.sp, fontWeight = FontWeight.Black, color = SonHarfBlue)
                        Text("Benzersiz kelimeler 2×", fontSize = 9.sp, color = SonHarfMuted)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { gameId += 1 }) { Text("RÖVANŞ") }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, SonHarfMuted.copy(alpha = .18f)),
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            input.ifBlank { "Kelime yaz" },
                            modifier = Modifier.weight(1f),
                            color = if (input.isBlank()) SonHarfMuted else SonHarfText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        TextButton(onClick = ::submitWord, enabled = phase == BotDuelPhase.PLAYING && input.isNotBlank() && !busy) {
                            Text("GÖNDER", fontWeight = FontWeight.Black)
                        }
                    }
                }

                if (notice.isNotBlank()) {
                    Text(notice, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = if (notice.startsWith("+")) SonHarfGreen else SonHarfPink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DuelWordStrip("SEN", myWords.takeLast(3), SonHarfBlue, Modifier.weight(1f))
                    DuelWordStrip("BOT", botWords.takeLast(3), SonHarfPink, Modifier.weight(1f))
                }

                EmbeddedWordKeyboard(
                    value = input,
                    language = SonHarfUiState.language,
                    enabled = phase == BotDuelPhase.PLAYING && !busy,
                    maxLength = 10,
                    onValueChange = { input = it },
                    onSubmit = ::submitWord,
                )
            }
        }

        ModernCountdownOverlay(
            value = countdown,
            visible = phase == BotDuelPhase.COUNTDOWN,
        )
    }
}

@Composable
private fun DuelMiniScore(label: String, score: Int, accent: Color, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(15.dp), color = accent.copy(alpha = .09f), border = BorderStroke(1.dp, accent.copy(alpha = .25f))) {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black)
            Text(score.toString(), color = SonHarfText, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun DuelWordStrip(title: String, words: List<BotDuelWord>, accent: Color, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, accent.copy(alpha = .16f))) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black)
            if (words.isEmpty()) {
                Text("—", color = SonHarfMuted, fontSize = 10.sp)
            } else {
                words.reversed().forEach {
                    Text(it.word.uppercase(Locale.forLanguageTag("tr-TR")), color = SonHarfText, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}
