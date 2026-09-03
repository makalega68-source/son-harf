from pathlib import Path

path = Path('app/src/main/java/com/sonharf/game/LightDuelUi.kt')
text = path.read_text()

def replace_once(old: str, new: str, label: str):
    global text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly 1 match, found {count}')
    text = text.replace(old, new, 1)

replace_once(
    'import androidx.compose.ui.unit.sp\n',
    'import androidx.compose.ui.unit.sp\nimport androidx.compose.ui.window.Dialog\nimport androidx.compose.ui.window.DialogProperties\n',
    'dialog imports',
)

old_inline = '''        if (quizActive) {
            val activeTrivia = requireNotNull(triviaRound)
            LightBonusCard(
                round = activeTrivia,
                question = requireNotNull(triviaQuestion),
                myAnswer = (if (host) activeTrivia.hostAnswer else activeTrivia.guestAnswer) ?: triviaSelection,
                opponentAnswer = if (room.isBot) activeTrivia.botAnswer else if (host) activeTrivia.guestAnswer else activeTrivia.hostAnswer,
                myWon = activeTrivia.winnerSide == if (host) "host" else "guest",
                tied = activeTrivia.winnerSide == "tie",
                onTrivia = onTrivia,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
'''
new_inline = '''        if (quizActive) {
            val activeTrivia = requireNotNull(triviaRound)
            LightBonusOverlay(
                round = activeTrivia,
                question = requireNotNull(triviaQuestion),
                myAnswer = (if (host) activeTrivia.hostAnswer else activeTrivia.guestAnswer) ?: triviaSelection,
                opponentAnswer = if (room.isBot) activeTrivia.botAnswer else if (host) activeTrivia.guestAnswer else activeTrivia.hostAnswer,
                myWon = activeTrivia.winnerSide == if (host) "host" else "guest",
                tied = activeTrivia.winnerSide == "tie",
                onTrivia = onTrivia,
            )
        }
'''
replace_once(old_inline, new_inline, 'inline bonus card')

marker = '''@Composable
private fun LightBonusCard(
'''
overlay = '''@Composable
private fun LightBonusOverlay(
    round: TriviaRoundDto,
    question: TriviaQuestionDto,
    myAnswer: Long?,
    opponentAnswer: Long?,
    myWon: Boolean,
    tied: Boolean,
    onTrivia: (Long) -> Unit,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = .46f))
                .padding(horizontal = 18.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            LightBonusCard(
                round = round,
                question = question,
                myAnswer = myAnswer,
                opponentAnswer = opponentAnswer,
                myWon = myWon,
                tied = tied,
                onTrivia = onTrivia,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LightBonusCard(
'''
replace_once(marker, overlay, 'bonus overlay insertion')

old_result = '''                Text("$playerName  $myRounds : $oppRounds  $opponentName", color = LText, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                if (isVip && words.isNotEmpty()) {
'''
new_result = '''                Text("$playerName  $myRounds : $oppRounds  $opponentName", color = LText, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                val longestWord = words
                    .map { it.word.trim().ifBlank { it.normalizedWord.trim() } }
                    .filter { it.isNotBlank() }
                    .maxByOrNull { it.length }
                    ?.let { gameUppercase(it, language) }
                    ?: "—"
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LightResultMetric(
                        label = sh("GEÇERLİ KELİME", "VALID WORDS"),
                        value = words.size.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    LightResultMetric(
                        label = sh("EN UZUN KELİME", "LONGEST WORD"),
                        value = longestWord,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (isVip && words.isNotEmpty()) {
'''
replace_once(old_result, new_result, 'result metrics')

result_marker = '''@Composable
private fun LightResult(
'''
metric = '''@Composable
private fun LightResultMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(62.dp),
        shape = RoundedCornerShape(16.dp),
        color = LCard2,
        border = BorderStroke(1.dp, LBorder),
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(label, color = LMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(value, color = LText, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun LightResult(
'''
replace_once(result_marker, metric, 'result metric component')

path.write_text(text)
print('UI polish patch applied successfully')
