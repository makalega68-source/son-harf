from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]

def read(rel): return (ROOT / rel).read_text()
def write(rel, text): (ROOT / rel).write_text(text)
def replace_once(text, old, new, label):
    if old not in text:
        raise SystemExit(f"missing pattern: {label}")
    return text.replace(old, new, 1)
def sub_once(text, pattern, repl, label, flags=0):
    out, n = re.subn(pattern, repl, text, count=1, flags=flags)
    if n != 1:
        raise SystemExit(f"pattern {label} matched {n}")
    return out

# ---------------- Practice engine: auto direction + strict dictionary + 2-point transfer ----------------
p = "app/src/main/java/com/sonharf/game/WordSiegePracticeEngine.kt"
s = read(p)
s = replace_once(s,
"    val playerWordScore: Int = 0,\n    val botWordScore: Int = 0,\n    val playerArea: Int = 0,",
"    val playerWordScore: Int = 0,\n    val botWordScore: Int = 0,\n    val playerAreaScore: Int = 0,\n    val botAreaScore: Int = 0,\n    val playerArea: Int = 0,",
"practice score ledgers")
old_header = """    fun applyMove(\n        state: WordSiegePracticeState,\n        owner: Int,\n        placements: Map<Int, Int>,\n        horizontal: Boolean,\n    ): Pair<WordSiegePracticeState, WordSiegePracticeMove> {\n"""
new_header = """    fun applyMove(\n        state: WordSiegePracticeState,\n        owner: Int,\n        placements: Map<Int, Int>,\n    ): Pair<WordSiegePracticeState, WordSiegePracticeMove> {\n        val horizontal = WordSiegeFinalRules.detectOrientation(state.board, placements.keys) == WordSiegeOrientation.HORIZONTAL\n        return applyMoveResolved(state, owner, placements, horizontal)\n    }\n\n    /** Compatibility overload: direction is deliberately ignored; final rules always auto-detect it. */\n    fun applyMove(\n        state: WordSiegePracticeState,\n        owner: Int,\n        placements: Map<Int, Int>,\n        @Suppress(\"UNUSED_PARAMETER\") horizontal: Boolean,\n    ): Pair<WordSiegePracticeState, WordSiegePracticeMove> = applyMove(state, owner, placements)\n\n    private fun applyMoveResolved(\n        state: WordSiegePracticeState,\n        owner: Int,\n        placements: Map<Int, Int>,\n        horizontal: Boolean,\n    ): Pair<WordSiegePracticeState, WordSiegePracticeMove> {\n"""
s = replace_once(s, old_header, new_header, "practice applyMove")
s = replace_once(s,
"        val playerArea = board.count { it.owner == 1 }\n        val botArea = board.count { it.owner == 2 }\n        val next = state.copy(",
"        val gainedCells = board.indices.count { index -> state.board[index].owner != owner && board[index].owner == owner }\n        val areaTransfer = WordSiegeFinalRules.cubeTransfer(gainedCells)\n        val playerArea = board.count { it.owner == 1 }\n        val botArea = board.count { it.owner == 2 }\n        val next = state.copy(",
"practice gained cubes")
s = replace_once(s,
"            playerWordScore = state.playerWordScore + if (owner == 1) score else 0,\n            botWordScore = state.botWordScore + if (owner == 2) score else 0,\n            playerArea = playerArea,",
"            playerWordScore = state.playerWordScore + if (owner == 1) score else 0,\n            botWordScore = state.botWordScore + if (owner == 2) score else 0,\n            playerAreaScore = state.playerAreaScore + if (owner == 1) areaTransfer else 0,\n            botAreaScore = state.botAreaScore + if (owner == 2) areaTransfer else 0,\n            playerArea = playerArea,",
"practice apply transfer")
s = replace_once(s,
"        return finished to WordSiegePracticeMove(placements, horizontal, primary.orEmpty(), words, score, captured.size)",
"        return finished to WordSiegePracticeMove(placements, horizontal, primary.orEmpty(), words, score, gainedCells)",
"practice move gained count")
s = s.replace("applyMove(state, 2, placements, horizontal)", "applyMove(state, 2, placements)")
s = replace_once(s,
"        val winner = forcedWinner ?: when {\n            state.playerWordScore + state.playerArea > state.botWordScore + state.botArea -> 1\n            state.botWordScore + state.botArea > state.playerWordScore + state.playerArea -> 2",
"        val winner = forcedWinner ?: when {\n            totalScore(state, 1) > totalScore(state, 2) -> 1\n            totalScore(state, 2) > totalScore(state, 1) -> 2",
"practice winner net score")
s = replace_once(s,
"    private fun requireActiveTurn(state: WordSiegePracticeState, owner: Int) {",
"    fun totalScore(state: WordSiegePracticeState, owner: Int): Int = if (owner == 1) {\n        WordSiegeFinalRules.netScore(state.playerWordScore, state.playerAreaScore, state.botAreaScore)\n    } else {\n        WordSiegeFinalRules.netScore(state.botWordScore, state.botAreaScore, state.playerAreaScore)\n    }\n\n    private fun requireActiveTurn(state: WordSiegePracticeState, owner: Int) {",
"practice total helper")
s = sub_once(s,
r"    private fun isPracticeWord\(word: String\): Boolean \{.*?\n    \}\n\n    private fun letterValue",
"""    private fun isPracticeWord(word: String): Boolean {\n        val normalized = word.trim().uppercase(trLocale)\n        return normalized in practiceDictionary\n    }\n\n    private fun letterValue""",
"strict practice dictionary", re.S)
s = replace_once(s,
"    private val botWords = listOf(\n        \"MASA\", \"KALEM\", \"KALE\", \"ELMA\", \"SİMA\", \"İSİM\", \"LİMAN\", \"MİNİ\", \"SİNEK\",",
"    private val botWords = listOf(\n        \"MASA\", \"KALEM\", \"KALE\", \"ELMA\", \"SİMA\", \"İSİM\", \"LİMAN\", \"MİNİ\", \"SİNEK\",\n        \"KARA\", \"PARA\", \"SEL\", \"KAT\", \"MAKALE\",",
"practice dictionary examples")
s = replace_once(s,
"    )\n}\n",
"    )\n\n    private val practiceDictionary: Set<String> = botWords.toSet()\n}\n",
"practice dictionary set")
write(p, s)

# ---------------- Practice screen: no direction chips, animated net counters ----------------
p = "app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt"
s = read(p)
s = s.replace("    var horizontal by remember { mutableStateOf(true) }\n", "")
s = s.replace("WordSiegePracticeEngine.applyMove(state, 1, placements, horizontal)", "WordSiegePracticeEngine.applyMove(state, 1, placements)")
s = s.replace("WordSiegePracticeEngine.applyMove(state, 2, planned.placements, planned.horizontal)", "WordSiegePracticeEngine.applyMove(state, 2, planned.placements)")
insert_after = "    var exchangeSelection by remember { mutableStateOf<Set<Int>>(emptySet()) }\n"
anim = """\n    val playerTargetScore = WordSiegePracticeEngine.totalScore(state, 1)\n    val botTargetScore = WordSiegePracticeEngine.totalScore(state, 2)\n    var displayedPlayerScore by remember { mutableIntStateOf(playerTargetScore) }\n    var displayedBotScore by remember { mutableIntStateOf(botTargetScore) }\n\n    LaunchedEffect(playerTargetScore, botTargetScore) {\n        while (displayedPlayerScore != playerTargetScore || displayedBotScore != botTargetScore) {\n            displayedPlayerScore += (playerTargetScore - displayedPlayerScore).coerceIn(-1, 1)\n            displayedBotScore += (botTargetScore - displayedBotScore).coerceIn(-1, 1)\n            delay(28)\n        }\n    }\n"""
s = replace_once(s, insert_after, insert_after + anim, "practice animation")
s = replace_once(s,
"WordSiegePracticeScoreCard(sh(\"SEN\", \"YOU\"), state.playerWordScore, state.playerArea, MainUi.Blue, active = state.currentOwner == 1, modifier = Modifier.weight(1f))\n                    WordSiegePracticeScoreCard(sh(\"BOT\", \"BOT\"), state.botWordScore, state.botArea, SiegePurple, active = state.currentOwner == 2, modifier = Modifier.weight(1f))",
"WordSiegePracticeScoreCard(sh(\"SEN\", \"YOU\"), displayedPlayerScore, state.playerArea, MainUi.Green, active = state.currentOwner == 1, modifier = Modifier.weight(1f))\n                    WordSiegePracticeScoreCard(sh(\"BOT\", \"BOT\"), displayedBotScore, state.botArea, MainUi.Red, active = state.currentOwner == 2, modifier = Modifier.weight(1f))",
"practice cards")
s = sub_once(s,
r"                item \{\n                    Row\(Modifier\.fillMaxWidth\(\), horizontalArrangement = Arrangement\.spacedBy\(6\.dp\), verticalAlignment = Alignment\.CenterVertically\) \{\n                        FilterChip\(.*?\n                    \}\n                \}\n\n                item \{\n                    Row\(Modifier\.fillMaxWidth\(\), horizontalArrangement = Arrangement\.spacedBy\(4\.dp\)\)",
"""                item {\n                    Text(\n                        sh(\"Yön otomatik algılanır • Torba ${state.bag.length}\", \"Direction is detected automatically • Bag ${state.bag.length}\"),\n                        color = MainUi.Muted,\n                        fontSize = 9.sp,\n                        modifier = Modifier.fillMaxWidth(),\n                        textAlign = TextAlign.End,\n                    )\n                }\n\n                item {\n                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp))""",
"practice direction chips", re.S)
s = s.replace("+${move.wordScore} kelime puanı • ${move.capturedCells} alan ele geçirildi", "+${move.wordScore} kelime • ${move.capturedCells} küp • +${move.capturedCells * 2}/-${move.capturedCells * 2} transfer")
s = s.replace("+${move.wordScore} word points • ${move.capturedCells} territory captured", "+${move.wordScore} word • ${move.capturedCells} cubes • +${move.capturedCells * 2}/-${move.capturedCells * 2} transfer")
s = replace_once(s,
"    wordScore: Int,\n    area: Int,",
"    score: Int,\n    area: Int,",
"practice card signature")
s = replace_once(s,
"                Text(\"${wordScore + area}\", color = accent, fontWeight = FontWeight.Black, fontSize = 19.sp)\n                Text(sh(\"Kelime $wordScore • Alan $area\", \"Word $wordScore • Area $area\"), color = MainUi.Muted, fontSize = 7.sp, maxLines = 1)",
"                Text(\"$score\", color = accent, fontWeight = FontWeight.Black, fontSize = 19.sp)\n                Text(sh(\"Alan $area • Küp başına ±2\", \"Area $area • ±2 per cube\"), color = MainUi.Muted, fontSize = 7.sp, maxLines = 1)",
"practice card content")
write(p, s)

# ---------------- Pan online screen: no direction chips, exact green/red cubes, animated net score ----------------
p = "app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt"
s = read(p)
s = replace_once(s, "import kotlin.math.min\n", "import kotlin.math.min\nimport kotlinx.coroutines.delay\n", "pan delay import")
s = s.replace("private val PanSiegeMine = Color(0xFF9FD5A5)", "private val PanSiegeMine = Color(0xFF35C878)")
s = s.replace("private val PanSiegeRival = Color(0xFFEAA4A4)", "private val PanSiegeRival = Color(0xFFFF5F57)")
s = s.replace("    horizontal: Boolean,\n", "")
s = s.replace("    onHorizontal: (Boolean) -> Unit,\n", "")
anchor = "    val lastMove = moves.lastOrNull()\n"
anim = """    val myEarnedCubePoints = WordSiegeFinalRules.earnedCubePoints(moves, me)\n    val rivalEarnedCubePoints = WordSiegeFinalRules.earnedCubePoints(moves, opponentId)\n    val myTargetScore = WordSiegeFinalRules.netScore(panSiegeWordScore(game, myOwner), myEarnedCubePoints, rivalEarnedCubePoints)\n    val rivalTargetScore = WordSiegeFinalRules.netScore(panSiegeWordScore(game, rivalOwner), rivalEarnedCubePoints, myEarnedCubePoints)\n    var displayedMyScore by remember(game.id) { mutableIntStateOf(myTargetScore) }\n    var displayedRivalScore by remember(game.id) { mutableIntStateOf(rivalTargetScore) }\n\n    LaunchedEffect(myTargetScore, rivalTargetScore) {\n        while (displayedMyScore != myTargetScore || displayedRivalScore != rivalTargetScore) {\n            displayedMyScore += (myTargetScore - displayedMyScore).coerceIn(-1, 1)\n            displayedRivalScore += (rivalTargetScore - displayedRivalScore).coerceIn(-1, 1)\n            delay(28)\n        }\n    }\n"""
s = replace_once(s, anchor, anchor + anim, "pan score animation")
s = replace_once(s,
"                wordScore = panSiegeWordScore(game, myOwner),\n                areaScore = panSiegeAreaScore(game, myOwner),",
"                score = displayedMyScore,\n                earnedCubePoints = myEarnedCubePoints,",
"pan mine card")
s = replace_once(s,
"                accent = MainUi.Blue,",
"                accent = MainUi.Green,",
"pan mine accent")
s = replace_once(s,
"                wordScore = panSiegeWordScore(game, rivalOwner),\n                areaScore = panSiegeAreaScore(game, rivalOwner),",
"                score = displayedRivalScore,\n                earnedCubePoints = rivalEarnedCubePoints,",
"pan rival card")
s = replace_once(s,
"                accent = SiegePurple,\n                active = game.currentPlayerId == opponentId,",
"                accent = MainUi.Red,\n                active = game.currentPlayerId == opponentId,",
"pan rival accent")
s = sub_once(s,
r"            Row\(Modifier\.fillMaxWidth\(\), verticalAlignment = Alignment\.CenterVertically\) \{\n                FilterChip\(.*?\n            \}\n\n            Row\(Modifier\.fillMaxWidth\(\), horizontalArrangement = Arrangement\.spacedBy\(4\.dp\)\)",
"""            Text(\n                sh(\"Yön otomatik algılanır • Torba ${game.bag.length}\", \"Direction is detected automatically • Bag ${game.bag.length}\"),\n                color = MainUi.Muted,\n                fontSize = 8.sp,\n                modifier = Modifier.fillMaxWidth(),\n                textAlign = TextAlign.End,\n            )\n\n            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp))""",
"pan direction chips", re.S)
s = replace_once(s,
"    wordScore: Int,\n    areaScore: Int,\n    areaCount: Int,",
"    score: Int,\n    earnedCubePoints: Int,\n    areaCount: Int,",
"pan card signature")
s = replace_once(s,
"                Text(\"${wordScore + areaScore}\", color = accent, fontSize = 18.sp, fontWeight = FontWeight.Black)\n                Text(\n                    sh(\"Kelime $wordScore • Alan puanı $areaScore • Alan $areaCount\", \"Word $wordScore • Area score $areaScore • Area $areaCount\"),",
"                Text(\"$score\", color = accent, fontSize = 18.sp, fontWeight = FontWeight.Black)\n                Text(\n                    sh(\"Küp +$earnedCubePoints • Alan $areaCount • küp başına ±2\", \"Cubes +$earnedCubePoints • Area $areaCount • ±2 per cube\"),",
"pan card content")
s = s.replace("sh(\"Sonuç = kelime puanı + alan puanı\", \"Result = word score + area score\")", "sh(\"Sonuç = kelime + kendi küp puanı - rakip küp puanı\", \"Result = word + own cube points - rival cube points\")")
write(p, s)

# ---------------- Experience: auto-detect direction; shared board green/red ----------------
p = "app/src/main/java/com/sonharf/game/WordSiegeExperience.kt"
s = read(p)
s = s.replace("    var horizontal by remember { mutableStateOf(true) }\n", "")
s = s.replace("                    horizontal = horizontal,\n", "")
s = s.replace("                    onHorizontal = { horizontal = it },\n", "")
old_submit = """                            runGameAction {\n                                backend.submitWordSiegeMove(\n                                    game.id,\n                                    placements.entries.sortedBy { it.key }.map { WordSiegePlacement(it.key, it.value) },\n                                    horizontal,\n                                )\n                            }\n"""
new_submit = """                            val orientation = runCatching { WordSiegeFinalRules.detectOrientation(game.board, placements.keys) }\n                            if (orientation.isFailure) {\n                                notice = wordSiegeFriendlyError(orientation.exceptionOrNull()?.message.orEmpty())\n                            } else {\n                                runGameAction {\n                                    backend.submitWordSiegeMove(\n                                        game.id,\n                                        placements.entries.sortedBy { it.key }.map { WordSiegePlacement(it.key, it.value) },\n                                        orientation.getOrThrow() == WordSiegeOrientation.HORIZONTAL,\n                                    )\n                                }\n                            }\n"""
s = replace_once(s, old_submit, new_submit, "online auto orientation")
# Remove any legacy direction selector row while leaving legacy parameters harmless for binary/source compatibility.
s, n = re.subn(
    r"\n            item \{\n                Row\(Modifier\.fillMaxWidth\(\), horizontalArrangement = Arrangement\.spacedBy\(6\.dp\)\) \{\n                    FilterChip\(.*?\n                \}\n            \}\n\n            item \{\n                Row\(Modifier\.fillMaxWidth\(\), horizontalArrangement = Arrangement\.spacedBy\(4\.dp\)\)",
    "\n            item {\n                Text(sh(\"Yön otomatik algılanır • Torba ${game.bag.length}\", \"Direction is detected automatically • Bag ${game.bag.length}\"), color = MainUi.Muted, fontSize = 9.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)\n            }\n\n            item {\n                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp))",
    s, flags=re.S)
# Shared practice/legacy board colors are relative to current viewer.
s = sub_once(s,
r"    val territory = when \(owner\) \{\n        1 -> MainUi\.Blue\.copy\(alpha = if \(pending\) \.30f else \.17f\)\n        2 -> SiegePurple\.copy\(alpha = if \(pending\) \.30f else \.17f\)\n        else -> MainUi\.Surface\n    \}\n    val border = when \{\n        pending -> SiegeTileBorder\n        owner == 1 -> MainUi\.Blue\.copy\(alpha = \.45f\)\n        owner == 2 -> SiegePurple\.copy\(alpha = \.45f\)\n        else -> MainUi\.Border\n    \}",
"""    val territory = when {\n        owner == 0 -> MainUi.Surface\n        owner == myOwner -> Color(0xFF35C878)\n        else -> Color(0xFFFF5F57)\n    }\n    val border = when {\n        pending -> SiegeTileBorder\n        owner == myOwner -> MainUi.Green\n        owner != 0 -> MainUi.Red\n        else -> MainUi.Border\n    }""",
"shared ownership colors")
write(p, s)

print("Final Word Siege Kotlin patch applied")
