from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]

def replace_once(path: Path, old: str, new: str, label: str):
    text = path.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly 1 match, found {count}")
    path.write_text(text.replace(old, new, 1))


def regex_once(path: Path, pattern: str, replacement: str, label: str):
    text = path.read_text()
    new, count = re.subn(pattern, replacement, text, count=1, flags=re.S)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly 1 regex match, found {count}")
    path.write_text(new)

screen = ROOT / "app/src/main/java/com/sonharf/game/OnlineGameScreenV6.kt"
ui = ROOT / "app/src/main/java/com/sonharf/game/LightDuelUi.kt"

replace_once(screen,
'''        "chat_disabled" in raw -> sh("Maç sohbeti geçici olarak kapalı.", "Match chat is temporarily disabled.")
        else -> sh("İşlem tekrar deneniyor.", "Retrying the action.")''',
'''        "chat_disabled" in raw -> sh("Maç sohbeti geçici olarak kapalı.", "Match chat is temporarily disabled.")
        "voice_limit_reached" in raw -> sh("Bu maçtaki 5 sesli cevap hakkın doldu.", "You have used all 5 voice answers for this match.")
        "answer_already_submitted" in raw -> sh("Bonus cevabın zaten kilitlendi.", "Your bonus answer is already locked.")
        else -> sh("İşlem tekrar deneniyor.", "Retrying the action.")''',
"friendly errors")

replace_once(screen,
'''    var chatJob by remember { mutableStateOf<Job?>(null) }
    var matchJob by remember { mutableStateOf<Job?>(null) }
''',
'''    var chatJob by remember { mutableStateOf<Job?>(null) }
    var matchJob by remember { mutableStateOf<Job?>(null) }
    var voiceUses by remember { mutableIntStateOf(0) }
    var voiceRequestId by remember { mutableStateOf<String?>(null) }
    val voiceInput = rememberVoiceWordInput(language) { recognized, requestId ->
        wordInput = recognized.take(40)
        voiceRequestId = requestId
        notice = sh("Ses tanındı. Kelimeyi kontrol edip GÖNDER'e bas.", "Voice recognized. Check the word, then press SEND.")
    }
''',
"voice state")

replace_once(screen,
'''        LaunchedEffect(active.currentPlayerId, active.validWordCount, active.roundNo) { wordInput = "" }
        LaunchedEffect(active.id) { while (true) { if (!active.isBot && active.status != "waiting") runCatching { backend.heartbeatRoom(active.id) }.onSuccess { room = it }; delay(5000) } }
''',
'''        LaunchedEffect(active.currentPlayerId, active.validWordCount, active.roundNo) {
            wordInput = ""
            voiceRequestId = null
        }
        LaunchedEffect(active.id) {
            voiceUses = runCatching { backend.getVoiceUses(active.id) }.getOrDefault(0)
        }
        LaunchedEffect(active.id) { while (true) { if (!active.isBot && active.status != "waiting") runCatching { backend.heartbeatRoom(active.id) }.onSuccess { room = it }; delay(5000) } }
''',
"voice hydrate")

replace_once(screen,
'''            triviaSelection = triviaSelection?.takeIf { it.first == triviaRound?.id }?.second,
            onSubmit = {''',
'''            triviaSelection = triviaSelection?.takeIf { it.first == triviaRound?.id }?.second,
            voiceSupported = voiceInput.supported,
            voiceUses = voiceUses,
            onSubmit = {''',
"arena voice params")

replace_once(screen,
'''                    val shownWord = gameUppercase(submitted, active.language)
                    wordInput = ""
                    busy = true
                    SonHarfSoundFx.tap()
                    runCatching { backend.submitWord(active.id, submitted) }
                        .onSuccess { result ->
                            room = result
''',
'''                    val shownWord = gameUppercase(submitted, active.language)
                    val voiceToken = voiceRequestId
                    if (voiceToken == null) wordInput = ""
                    busy = true
                    SonHarfSoundFx.tap()
                    runCatching {
                        if (voiceToken != null) backend.submitVoiceWord(active.id, submitted, voiceToken)
                        else backend.submitWord(active.id, submitted)
                    }
                        .onSuccess { result ->
                            room = result
                            if (voiceToken != null) {
                                wordInput = ""
                                voiceRequestId = null
                                voiceUses = runCatching { backend.getVoiceUses(active.id) }.getOrDefault(voiceUses + 1)
                            }
''',
"voice submit")

replace_once(screen,
'''            onTrivia = { estimate ->
                val q = triviaRound
                if (q != null && q.resolvedAt == null && triviaSelection?.first != q.id) {
                    triviaSelection = q.id to estimate.toLong()
                    scope.launch {
                        runCatching { backend.answerTrivia(q.id, estimate) }
''',
'''            onBonus = {
                if (!busy && active.status == "playing") scope.launch {
                    busy = true
                    runCatching { backend.triggerBilBakalimBonus(active.id) }
                        .onSuccess { updated ->
                            room = updated
                            refreshQuiz(updated)
                            if (updated.status == "quiz") {
                                notice = sh("BİL BAKALIM başladı!", "GUESS IT started!")
                                SonHarfSoundFx.softNotify()
                            }
                        }
                        .onFailure { notice = friendly(it.message.orEmpty()) }
                    busy = false
                }
            },
            onVoice = {
                when {
                    !voiceInput.supported -> notice = sh("Bu cihazda ses tanıma desteklenmiyor.", "Speech recognition is not supported on this device.")
                    voiceUses >= 5 -> notice = sh("Bu maçtaki 5 sesli cevap hakkın doldu.", "You have used all 5 voice answers for this match.")
                    else -> voiceInput.launch()
                }
            },
            onTrivia = { estimate ->
                val q = triviaRound
                if (q != null && q.resolvedAt == null && triviaSelection?.first != q.id) {
                    triviaSelection = q.id to estimate
                    scope.launch {
                        runCatching { backend.answerBilBakalimNumeric(q.id, estimate) }
''',
"bonus and numeric callbacks")

replace_once(screen,
'''            onExit = { roomJob?.cancel(); wordsJob?.cancel(); chatJob?.cancel(); room = null; words = emptyList(); chat = emptyList(); notice = sh("Yeni düelloya hazırsın.", "You are ready for a new duel.") },
            onRematch = { scope.launch { runCatching { if (active.isBot) backend.restartBotMatch(active.id) else backend.requestRematch(active.id) }.onSuccess { room = it; words = emptyList(); chat = emptyList(); if (it.id != active.id) observe(it) }.onFailure { notice = friendly(it.message.orEmpty()) } } }
''',
'''            onExit = {
                roomJob?.cancel(); wordsJob?.cancel(); chatJob?.cancel(); matchJob?.cancel()
                voiceRequestId = null; voiceUses = 0; triviaRound = null; triviaQuestion = null; triviaSelection = null
                room = null; words = emptyList(); chat = emptyList(); feedbackWord = null; feedbackCorrect = null
                notice = sh("Yeni düelloya hazırsın.", "You are ready for a new duel.")
            },
            onRematch = { scope.launch {
                runCatching { if (active.isBot) backend.restartBotMatch(active.id) else backend.requestRematch(active.id) }
                    .onSuccess {
                        voiceRequestId = null; voiceUses = 0; triviaRound = null; triviaQuestion = null; triviaSelection = null
                        feedbackWord = null; feedbackCorrect = null; wordInput = ""
                        room = it; words = emptyList(); chat = emptyList()
                        if (it.id != active.id) observe(it)
                    }
                    .onFailure { notice = friendly(it.message.orEmpty()) }
            } }
''',
"rematch cleanup")

replace_once(ui,
'''import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*''',
'''import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*''',
"keyboard options import")
replace_once(ui,
'''import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp''',
'''import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp''',
"keyboard type import")

replace_once(ui,
'''    triviaSelection: Long?,
    onSubmit: () -> Unit,
    onTimeout: () -> Unit,
    onTrivia: (Int) -> Unit,
    onTriviaTimeout: () -> Unit,
    onChat: () -> Unit,''',
'''    triviaSelection: Long?,
    voiceSupported: Boolean,
    voiceUses: Int,
    onSubmit: () -> Unit,
    onTimeout: () -> Unit,
    onBonus: () -> Unit,
    onVoice: () -> Unit,
    onTrivia: (Long) -> Unit,
    onTriviaTimeout: () -> Unit,
    onChat: () -> Unit,''',
"arena signature")

replace_once(ui,
'''            myRounds = myRounds,
            oppRounds = oppRounds,
            onRematch = onRematch,''',
'''            myRounds = myRounds,
            oppRounds = oppRounds,
            words = words,
            isVip = isVip,
            language = room.language,
            onRematch = onRematch,''',
"result params")

replace_once(ui, 'triviaResolved -> 5', 'triviaResolved -> 3', "result timer")
replace_once(ui, 'fontSize = 62.sp,\n                        lineHeight = 64.sp,', 'fontSize = 52.sp,\n                        lineHeight = 54.sp,', "letter size")
replace_once(ui, 'fontSize = 14.sp,\n                        fontWeight = FontWeight.Black,\n                        maxLines = 1,', 'fontSize = 21.sp,\n                        fontWeight = FontWeight.Black,\n                        maxLines = 1,', "answer size")
replace_once(ui,
'''            LightActionButton(sh("⚑ PES ET", "⚑ FORFEIT"), LRed, Modifier.weight(1f), onForfeit)
            LightActionButton(sh("● SOHBET", "● CHAT"), LBlue, Modifier.weight(1f), onChat)
            LightActionButton("★ BONUS", LGold, Modifier.weight(1f)) { }''',
'''            LightActionButton(sh("⚑ PES ET", "⚑ FORFEIT"), LRed, Modifier.weight(1f), onForfeit)
            LightActionButton(sh("● SOHBET", "● CHAT"), LBlue, Modifier.weight(1f), onChat)
            LightActionButton("★ BONUS", LGold, Modifier.weight(1f), onBonus)''',
"bonus button")

replace_once(ui,
'''                myAnswer = (if (host) activeTrivia.hostAnswer else activeTrivia.guestAnswer) ?: triviaSelection,
                onTrivia = onTrivia,
''',
'''                myAnswer = (if (host) activeTrivia.hostAnswer else activeTrivia.guestAnswer) ?: triviaSelection,
                opponentAnswer = if (room.isBot) activeTrivia.botAnswer else if (host) activeTrivia.guestAnswer else activeTrivia.hostAnswer,
                myWon = activeTrivia.winnerSide == if (host) "host" else "guest",
                tied = activeTrivia.winnerSide == "tie",
                onTrivia = onTrivia,
''',
"bonus result args")

replace_once(ui,
'''            quiz = quizActive,
            onSubmit = onSubmit,
''',
'''            quiz = quizActive,
            voiceSupported = voiceSupported,
            voiceUses = voiceUses,
            onVoice = onVoice,
            onSubmit = onSubmit,
''',
"input voice args")

regex_once(ui,
r'''@Composable\nprivate fun LightVipWordHistory\(.*?\n}\n\n@Composable\nprivate fun LightBonusCard''',
'''@Composable
private fun LightVipWordHistory(
    isVip: Boolean,
    words: List<GameWordDto>,
    language: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(58.dp),
        shape = RoundedCornerShape(15.dp),
        color = Color.White,
        border = BorderStroke(1.dp, LBorder),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(sh("ZİNCİR", "CHAIN"), color = LBlue, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(8.dp))
            if (words.isEmpty()) {
                Text(sh("Henüz kelime yok.", "No words yet."), color = LMuted, fontSize = 10.sp)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    items(words.takeLast(6)) { word ->
                        Surface(shape = RoundedCornerShape(12.dp), color = LBlueSoft, border = BorderStroke(1.dp, LBlue.copy(alpha = .18f))) {
                            Text(
                                gameUppercase(word.word.trim().ifBlank { word.normalizedWord.trim() }, language),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                color = LText, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LightBonusCard''',
"ranked history fairness")

regex_once(ui,
r'''@Composable\nprivate fun LightBonusCard\(.*?\n}\n\n@Composable\nprivate fun LightInputBar''',
'''@Composable
private fun LightBonusCard(
    round: TriviaRoundDto,
    question: TriviaQuestionDto,
    myAnswer: Long?,
    opponentAnswer: Long?,
    myWon: Boolean,
    tied: Boolean,
    onTrivia: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resolved = round.resolvedAt != null
    var value by remember(round.id) { mutableStateOf("") }
    val parsed = value.toLongOrNull()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F2FF)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, LPurple.copy(alpha = .35f)),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("★ BİL BAKALIM +${round.bonusPoints}", color = LGold, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Text(question.question, color = LText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            if (!resolved && myAnswer == null) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { raw -> value = raw.filter(Char::isDigit).take(16) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = LocalTextStyle.current.copy(fontSize = 28.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center),
                    placeholder = { Text(sh("Sayı gir", "Enter a number"), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                )
                Button(
                    onClick = { parsed?.let(onTrivia) },
                    enabled = parsed != null,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LPurple),
                ) { Text(sh("CEVABI KİLİTLE", "LOCK ANSWER"), fontWeight = FontWeight.Black) }
            } else if (!resolved) {
                Text(sh("Cevabın alındı. Rakibin cevabı sonuçtan önce gizli.", "Answer received. Opponent answer stays hidden until the result."), color = LBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            } else {
                Text(sh("ASIL CEVAP ${round.correctAnswer ?: "—"}", "ACTUAL ANSWER ${round.correctAnswer ?: "—"}"), color = LText, fontSize = 17.sp, fontWeight = FontWeight.Black)
                val mineLabel = when { myAnswer == null -> sh("YANLIŞ • Cevap verilmedi", "WRONG • No answer"); tied -> sh("BERABERE • $myAnswer", "TIE • $myAnswer"); myWon -> sh("DOĞRU • $myAnswer", "CORRECT • $myAnswer"); else -> sh("YANLIŞ • $myAnswer", "WRONG • $myAnswer") }
                Text(mineLabel, color = when { tied -> LMuted; myWon -> LGreen; else -> LRed }, fontSize = 12.sp, fontWeight = FontWeight.Black)
                val oppLabel = when { opponentAnswer == null -> sh("RAKİP • Cevap verilmedi", "OPPONENT • No answer"); tied -> sh("BERABERE • $opponentAnswer", "TIE • $opponentAnswer"); myWon -> sh("RAKİP • $opponentAnswer", "OPPONENT • $opponentAnswer"); else -> sh("DOĞRU • $opponentAnswer", "CORRECT • $opponentAnswer") }
                Text(oppLabel, color = when { tied -> LMuted; myWon -> LRed; else -> LGreen }, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun LightInputBar''',
"numeric bonus UI")

regex_once(ui,
r'''@Composable\nprivate fun LightInputBar\(.*?\n}\n\n@Composable\nprivate fun LightGameKeyboard''',
'''@Composable
private fun LightInputBar(
    value: String,
    myTurn: Boolean,
    busy: Boolean,
    quiz: Boolean,
    voiceSupported: Boolean,
    voiceUses: Int,
    onVoice: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.5.dp, if (myTurn && !quiz) LBlue else LBorder),
    ) {
        Row(Modifier.fillMaxWidth().height(52.dp).padding(start = 10.dp, end = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = onVoice,
                enabled = myTurn && !busy && !quiz && voiceSupported && voiceUses < 5,
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                border = BorderStroke(1.dp, LBlue.copy(alpha = .35f)),
            ) { Text("🎙 ${5 - voiceUses}", fontSize = 11.sp, fontWeight = FontWeight.Black) }
            Spacer(Modifier.width(6.dp))
            Text(
                when {
                    value.isNotBlank() -> value
                    quiz -> sh("Bonus turu devam ediyor…", "Bonus round in progress…")
                    myTurn -> sh("Kelimenizi yazın…", "Type your word…")
                    else -> sh("Kelimeyi hazırlayabilirsin…", "Prepare your word…")
                },
                color = if (value.isBlank()) LMuted else LText,
                fontSize = if (value.isBlank()) 13.sp else 18.sp,
                fontWeight = if (value.isBlank()) FontWeight.Medium else FontWeight.Black,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            Button(
                onClick = onSubmit,
                enabled = myTurn && value.isNotBlank() && !busy && !quiz,
                modifier = Modifier.height(40.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LBlue, contentColor = Color.White, disabledContainerColor = LCard2, disabledContentColor = LMuted),
            ) { Text("➤", fontSize = 18.sp, fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun LightGameKeyboard''',
"voice input UI")

replace_once(ui,
'''    myRounds: Int,
    oppRounds: Int,
    onRematch: () -> Unit,''',
'''    myRounds: Int,
    oppRounds: Int,
    words: List<GameWordDto>,
    isVip: Boolean,
    language: String,
    onRematch: () -> Unit,''',
"result signature")

replace_once(ui,
'''                Text("$playerName  $myRounds : $oppRounds  $opponentName", color = LText, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                Button(''',
'''                Text("$playerName  $myRounds : $oppRounds  $opponentName", color = LText, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                if (isVip && words.isNotEmpty()) {
                    Text(sh("VIP • TAM KELİME GEÇMİŞİ", "VIP • FULL WORD HISTORY"), color = LGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(words) { word ->
                            Surface(shape = RoundedCornerShape(10.dp), color = LBlueSoft) {
                                Text(gameUppercase(word.word.trim().ifBlank { word.normalizedWord.trim() }, language), Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = LText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Text(sh("Maçta ${words.size} geçerli kelime oynandı.", "${words.size} valid words were played."), color = LMuted, fontSize = 10.sp)
                }
                Button(''',
"vip postmatch history")

print("Secure match source patch applied successfully")
