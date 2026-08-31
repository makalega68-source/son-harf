from pathlib import Path
p=Path('app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt')
t=p.read_text()
def rep(old,new):
    global t
    if t.count(old)!=1: raise SystemExit('target mismatch: '+old[:60])
    t=t.replace(old,new,1)
rep('import androidx.compose.foundation.BorderStroke\nimport androidx.compose.foundation.background','import androidx.compose.animation.animateColorAsState\nimport androidx.compose.animation.core.tween\nimport androidx.compose.foundation.BorderStroke\nimport androidx.compose.foundation.background\nimport androidx.compose.foundation.border')
rep('import androidx.compose.runtime.*','import androidx.compose.runtime.*\nimport androidx.compose.runtime.saveable.rememberSaveable')
rep('    var state by remember { mutableStateOf(WordSiegePracticeEngine.newGame()) }','    var state by remember { mutableStateOf(WordSiegePracticeEngine.newGame()) }\n    var botName by rememberSaveable { mutableStateOf(TrainingBotSupport.chooseBotName()) }\n    val botDifficulty = TrainingBotDifficulty.MEDIUM')
rep('        state = WordSiegePracticeEngine.newGame()\n        notice = sh("Yeni harfler dağıtıldı.','        state = WordSiegePracticeEngine.newGame()\n        botName = TrainingBotSupport.chooseBotName(botName)\n        notice = sh("Yeni harfler dağıtıldı.')
rep('''        botThinking = true
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
        botThinking = false''','''        botThinking = true
        val planned = WordSiegePracticeEngine.bestBotMove(state, botDifficulty)
        delay(TrainingBotSupport.reactionDelayMs(botDifficulty, planned?.placements?.size ?: 1))
        if (planned == null) {
            state = WordSiegePracticeEngine.pass(state, 2)
            notice = sh("$botName pas verdi. Sıra sende.", "$botName passed. Your turn.")
        } else {
            val (next, move) = WordSiegePracticeEngine.applyMove(state, 2, planned.placements, planned.horizontal)
            state = next
            notice = sh("$botName ${move.primaryWord} oynadı • +${move.wordScore}", "$botName played ${move.primaryWord} • +${move.wordScore}")
            SonHarfSoundFx.scoreTick()
        }
        botThinking = false''')
rep('Text(sh("BOT ALIŞTIRMASI • ANA SÖZLÜK", "BOT PRACTICE • MAIN DICTIONARY"), color = MainUi.Blue, fontSize = 8.sp, fontWeight = FontWeight.Black)','Text(sh("ANTRENMAN MAÇI • RATING/LİG ETKİSİ YOK", "TRAINING MATCH • NO RATING/LEAGUE EFFECT"), color = MainUi.Blue, fontSize = 8.sp, fontWeight = FontWeight.Black)')
rep('                        name = "BOT", avatarPath = null, gender = null, score = state.botWordScore, area = state.botArea,','                        name = botName, avatarPath = null, gender = null, score = state.botWordScore, area = state.botArea,')
rep('else sh("BOT DÜŞÜNÜYOR…", "BOT IS THINKING…"),','else sh("$botName düşünüyor…", "$botName is thinking…"),')
old='''                        val owner = if (tempRack != null) 1 else cell.owner
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
                        }'''
new='''                        val owner = if (tempRack != null) 1 else cell.owner
                        val relation = TrainingBotSupport.ownershipRelation(owner, 1)
                        val targetFill = when (relation) {
                            WordSiegeOwnershipRelation.SELF -> Color(TrainingBotSupport.OWN_FILL_ARGB)
                            WordSiegeOwnershipRelation.OPPONENT -> Color(TrainingBotSupport.OPPONENT_FILL_ARGB)
                            WordSiegeOwnershipRelation.NEUTRAL -> if (cell.bonus != null) MainUi.BlueSoft else Color(TrainingBotSupport.NEUTRAL_FILL_ARGB)
                        }
                        val targetBorder = when (relation) {
                            WordSiegeOwnershipRelation.SELF -> Color(TrainingBotSupport.OWN_BORDER_ARGB)
                            WordSiegeOwnershipRelation.OPPONENT -> Color(TrainingBotSupport.OPPONENT_BORDER_ARGB)
                            WordSiegeOwnershipRelation.NEUTRAL -> MainUi.Border
                        }
                        val fill by animateColorAsState(targetFill, tween(220), label = "practice-owner-fill-$index")
                        val border by animateColorAsState(targetBorder, tween(220), label = "practice-owner-border-$index")
                        val shape = RoundedCornerShape(5.dp)
                        Box(
                            Modifier.weight(1f).fillMaxHeight().padding(1.dp).background(fill, shape)
                                .border(if (owner == 0) .7.dp else 1.2.dp, border, shape)
                                .then(if (enabled) Modifier.clickable { onCellClick(index) } else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (letter != null) {
                                Text(letter, color = Color(0xFF111827), fontWeight = FontWeight.Black, fontSize = 17.sp)
                                if (owner != 0 && tempRack == null) Box(Modifier.align(Alignment.TopEnd).padding(2.dp).size(4.dp).background(border, androidx.compose.foundation.shape.CircleShape))
                            } else if (cell.bonus != null) Text(cell.bonus, color = if (cell.bonus.endsWith("K")) SiegePurple else MainUi.Blue, fontWeight = FontWeight.Black, fontSize = 7.sp)
                            if (tempRack != null && tempRack == selectedRackIndex) Box(Modifier.fillMaxSize().padding(1.dp).background(MainUi.Blue.copy(alpha = .08f), shape))
                        }'''
rep(old,new)
p.write_text(t)
