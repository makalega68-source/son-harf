from pathlib import Path

ui = Path('app/src/main/java/com/sonharf/game/LightDuelUi.kt')
t = ui.read_text()

if 'import androidx.compose.foundation.text.KeyboardOptions' not in t:
    t = t.replace('import androidx.compose.foundation.shape.RoundedCornerShape\n', 'import androidx.compose.foundation.shape.RoundedCornerShape\nimport androidx.compose.foundation.text.KeyboardOptions\n', 1)
if 'import androidx.compose.ui.text.input.KeyboardType' not in t:
    t = t.replace('import androidx.compose.ui.text.font.FontWeight\n', 'import androidx.compose.ui.text.font.FontWeight\nimport androidx.compose.ui.text.input.KeyboardType\n', 1)

t = t.replace('triviaResolved -> 5; else -> 10', 'triviaResolved -> 3; else -> 10', 1)

old_call = 'LightBonusCard(activeTrivia, requireNotNull(triviaQuestion), (if (host) activeTrivia.hostAnswer else activeTrivia.guestAnswer) ?: triviaSelection, onTrivia, Modifier.padding(horizontal = 12.dp))'
new_call = 'LightBonusCard(activeTrivia, requireNotNull(triviaQuestion), (if (host) activeTrivia.hostAnswer else activeTrivia.guestAnswer) ?: triviaSelection, host, room.isBot, playerName, opponentName.removeSuffix(" BOT"), onTrivia, Modifier.padding(horizontal = 12.dp))'
if old_call not in t:
    raise SystemExit('LightBonusCard call target not found')
t = t.replace(old_call, new_call, 1)

start = t.index('@Composable\nprivate fun LightBonusCard(')
end = t.index('\n@Composable\nprivate fun LightInputBar', start)
replacement = r'''@Composable
private fun LightBonusCard(
    round: TriviaRoundDto,
    question: TriviaQuestionDto,
    myAnswer: Long?,
    host: Boolean,
    botMatch: Boolean,
    playerName: String,
    opponentName: String,
    onTrivia: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resolved = round.resolvedAt != null
    val mySide = if (host) "host" else "guest"
    val opponentSide = if (host) { if (botMatch) "bot" else "guest" } else "host"
    val opponentAnswer = when {
        !resolved -> null
        host && botMatch -> round.botAnswer
        host -> round.guestAnswer
        else -> round.hostAnswer
    }
    val correct = if (resolved) round.correctAnswer else null
    val myWon = resolved && round.winnerSide == mySide
    val opponentWon = resolved && round.winnerSide == opponentSide
    val tie = resolved && round.winnerSide == "tie"
    val nobody = resolved && round.winnerSide == "none"
    var estimateText by remember(round.id) { mutableStateOf("") }
    val locked = myAnswer != null || resolved
    val parsed = estimateText.toLongOrNull()
    val canSubmit = !locked && parsed != null && parsed in 0L..Int.MAX_VALUE.toLong()

    fun distance(answer: Long?): Long? = if (answer != null && correct != null) kotlin.math.abs(answer - correct) else null

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F2FF)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, LPurple.copy(alpha = .35f)),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("★ BİL BAKALIM • +10 PUAN", color = LGold, fontWeight = FontWeight.Black, fontSize = 11.sp)
            Text(question.question, color = LText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            if (question.answerUnit.isNotBlank()) {
                Text(question.answerUnit, color = LMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
            }

            if (!resolved) {
                OutlinedTextField(
                    value = if (locked) myAnswer?.toString().orEmpty() else estimateText,
                    onValueChange = { raw -> if (!locked) estimateText = raw.filter(Char::isDigit).take(10) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !locked,
                    singleLine = true,
                    label = { Text(sh("TAHMİNİNİ YAZ", "ENTER YOUR ESTIMATE")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LBlue, unfocusedBorderColor = LBorder),
                )
                Button(
                    onClick = { parsed?.takeIf { it in 0L..Int.MAX_VALUE.toLong() }?.let { onTrivia(it.toInt()) } },
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    enabled = canSubmit,
                    colors = ButtonDefaults.buttonColors(containerColor = LBlue),
                    shape = RoundedCornerShape(13.dp),
                ) { Text(if (locked) sh("TAHMİN KİLİTLENDİ", "ESTIMATE LOCKED") else sh("TAHMİNİ KİLİTLE", "LOCK ESTIMATE"), fontWeight = FontWeight.Black) }
                Text(
                    if (locked) sh("Cevabın kilitlendi. Rakibin tahmini sonuç açıklanana kadar gizli.", "Your answer is locked. The opponent estimate stays hidden until the result.")
                    else sh("10 saniye içinde sayısal tahminini gir. Rakibin cevabı önceden görünmez.", "Enter a numeric estimate within 10 seconds. The opponent answer is hidden until reveal."),
                    color = if (locked) LBlue else LMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold,
                )
            } else {
                Surface(Modifier.fillMaxWidth(), color = LGold.copy(alpha = .12f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, LGold.copy(alpha = .35f))) {
                    Column(Modifier.padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(sh("DOĞRU CEVAP", "CORRECT ANSWER"), color = LGold, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        Text(correct?.toString() ?: "—", color = LText, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    }
                }

                fun answerColor(answer: Long?, won: Boolean): Color = when {
                    answer == null -> LRed
                    tie -> LGold
                    won -> LGreen
                    else -> LRed
                }

                BilBakalimResultRow(playerName, myAnswer, distance(myAnswer), answerColor(myAnswer, myWon), myWon)
                BilBakalimResultRow(opponentName, opponentAnswer, distance(opponentAnswer), answerColor(opponentAnswer, opponentWon), opponentWon)

                val resultText = when {
                    nobody -> sh("İKİ OYUNCU DA CEVAP VERMEDİ • PUAN YOK", "BOTH PLAYERS GAVE NO ANSWER • NO POINTS")
                    tie -> sh("BERABERE • PUAN YOK", "TIE • NO POINTS")
                    myWon -> sh("EN YAKIN TAHMİN SENİN • +10 PUAN", "YOUR ESTIMATE IS CLOSEST • +10 POINTS")
                    opponentWon -> sh("RAKİP DAHA YAKIN • +10 PUAN RAKİBE", "OPPONENT IS CLOSER • +10 POINTS TO OPPONENT")
                    else -> sh("SONUÇ HESAPLANDI", "RESULT CALCULATED")
                }
                Text(resultText, Modifier.fillMaxWidth(), color = when { myWon -> LGreen; opponentWon -> LRed; else -> LGold }, fontSize = 10.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Text(sh("Sonuç 3 saniye sonra otomatik kapanır.", "Result closes automatically after 3 seconds."), Modifier.fillMaxWidth(), color = LMuted, fontSize = 8.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun BilBakalimResultRow(name: String, answer: Long?, distance: Long?, accent: Color, winner: Boolean) {
    Surface(Modifier.fillMaxWidth(), color = accent.copy(alpha = .10f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, accent.copy(alpha = .45f))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, color = LText, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Text(answer?.toString() ?: sh("CEVAP YOK", "NO ANSWER"), color = accent, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (distance != null) Text(sh("Fark: $distance", "Diff: $distance"), color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                if (winner) Text("+10", color = LGreen, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}
'''
t = t[:start] + replacement + t[end:]
ui.write_text(t)

backend = Path('app/src/main/java/com/sonharf/game/data/OnlineGameBackend.kt')
b = backend.read_text()
needle = '''    @SerialName("option_d") val optionD: String,\n)'''
replace = '''    @SerialName("option_d") val optionD: String,\n    @SerialName("answer_unit") val answerUnit: String = "",\n    @SerialName("question_kind") val questionKind: String = "legacy",\n)'''
if needle not in b:
    raise SystemExit('TriviaQuestionDto target not found')
b = b.replace(needle, replace, 1)
backend.write_text(b)
