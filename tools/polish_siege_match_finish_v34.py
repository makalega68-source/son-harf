from pathlib import Path

ROOT = Path('.')
GAME = ROOT / 'app/src/main/java/com/sonharf/game'
TEST = ROOT / 'app/src/test/java/com/sonharf/game'


def read(path):
    return Path(path).read_text()


def write(path, text):
    Path(path).write_text(text)


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly 1 match, found {count}')
    return text.replace(old, new, 1)


def replace_all_required(text, old, new, label, minimum=1):
    count = text.count(old)
    if count < minimum:
        raise SystemExit(f'{label}: expected at least {minimum} match(es), found {count}')
    return text.replace(old, new)


# Version ---------------------------------------------------------------------
path = ROOT / 'app/build.gradle.kts'
text = read(path)
text = replace_once(text, 'versionCode = 33', 'versionCode = 34', 'version code')
text = replace_once(text, 'versionName = "0.9.17"', 'versionName = "0.9.18"', 'version name')
write(path, text)


# Shared score card: remove the clipped BOT pill and draw a premium centered crown. --------
path = GAME / 'WordSiegeGameUi.kt'
text = read(path)
text = replace_once(
    text,
    'import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.graphics.Path',
    'import androidx.compose.ui.graphics.Brush\nimport androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.graphics.Path',
    'crown brush import',
)
text = replace_once(
    text,
    '''                Box {
                    ProfilePhotoAvatarWithGender(
                        avatarPath = avatarPath, gender = gender, name = name,
                        size = 52.dp, accent = accent, visible = avatarVisible,
                    )
                    if (leading) {
                        Box(Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp)) {
                            WordSiegeLeaderCrown()
                        }
                    }
                }
''',
    '''                Box(Modifier.padding(top = if (leading) 4.dp else 0.dp)) {
                    ProfilePhotoAvatarWithGender(
                        avatarPath = avatarPath, gender = gender, name = name,
                        size = 52.dp, accent = accent, visible = avatarVisible,
                    )
                    if (leading) {
                        Box(Modifier.align(Alignment.TopCenter).offset(y = (-7).dp)) {
                            WordSiegeLeaderCrown()
                        }
                    }
                }
''',
    'center crown over avatar',
)
text = replace_once(
    text,
    '''                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(sh("$area küp", "$area cubes"), color = WordSiegeGameUi.Muted, fontSize = 9.sp, lineHeight = 11.sp, maxLines = 1)
                        if (isBot) {
                            Surface(shape = RoundedCornerShape(99.dp), color = accent.copy(alpha = .12f)) {
                                Text("BOT", Modifier.padding(horizontal = 5.dp, vertical = 1.dp), color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
                            }
                        }
                    }
''',
    '''                    Text(
                        if (isBot) sh("$area küp • BOT", "$area cubes • BOT") else sh("$area küp", "$area cubes"),
                        color = WordSiegeGameUi.Muted,
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        fontWeight = if (isBot) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
''',
    'integrated bot label',
)
text = replace_once(
    text,
    '''@Composable
private fun WordSiegeLeaderCrown() {
    val description = sh("Lider", "Leader")
    Canvas(Modifier.size(13.dp).semantics { contentDescription = description }) {
        val crown = Path().apply {
            moveTo(size.width * .08f, size.height * .28f)
            lineTo(size.width * .3f, size.height * .48f)
            lineTo(size.width * .5f, size.height * .1f)
            lineTo(size.width * .7f, size.height * .48f)
            lineTo(size.width * .92f, size.height * .28f)
            lineTo(size.width * .8f, size.height * .86f)
            lineTo(size.width * .2f, size.height * .86f)
            close()
        }
        drawPath(crown, WordSiegeGameUi.Gold)
    }
}
''',
    '''@Composable
private fun WordSiegeLeaderCrown() {
    val description = sh("Lider", "Leader")
    Canvas(
        Modifier
            .size(width = 23.dp, height = 17.dp)
            .semantics { contentDescription = description },
    ) {
        val crown = Path().apply {
            moveTo(size.width * .08f, size.height * .32f)
            lineTo(size.width * .28f, size.height * .55f)
            lineTo(size.width * .39f, size.height * .19f)
            lineTo(size.width * .50f, size.height * .51f)
            lineTo(size.width * .62f, size.height * .12f)
            lineTo(size.width * .72f, size.height * .55f)
            lineTo(size.width * .92f, size.height * .30f)
            lineTo(size.width * .82f, size.height * .86f)
            lineTo(size.width * .18f, size.height * .86f)
            close()
        }
        val gold = Brush.verticalGradient(
            listOf(Color(0xFFFFF0A8), Color(0xFFF4C44F), Color(0xFFD69424)),
        )
        drawPath(crown, brush = gold)
        drawPath(crown, color = Color(0xFF7A5218), style = Stroke(width = 1.05.dp.toPx()))
        drawLine(
            color = Color(0xFFFFF4C4),
            start = Offset(size.width * .22f, size.height * .70f),
            end = Offset(size.width * .78f, size.height * .70f),
            strokeWidth = 1.2.dp.toPx(),
        )
        drawCircle(Color(0xFFE85D5D), 1.35.dp.toPx(), Offset(size.width * .39f, size.height * .61f))
        drawCircle(Color(0xFF4E8FD4), 1.35.dp.toPx(), Offset(size.width * .62f, size.height * .59f))
    }
}
''',
    'premium crown',
)
write(path, text)


# Board topology: retain 15x15 and 33 strategic squares, but spread multipliers more deliberately. ----
path = GAME / 'WordSiegeBoardSpec.kt'
text = read(path)
text = replace_once(
    text,
    '    const val CellCount = Size * Size\n',
    '    const val CellCount = Size * Size\n    const val RackSize = 7\n',
    'rack size contract',
)
text = replace_once(
    text,
    '''    /** Watch points placed on an outer tactical ring. */
    private val SiegeWatchZones = setOf(
        2 to 4, 2 to 10, 4 to 2, 4 to 12,
        10 to 2, 10 to 12, 12 to 4, 12 to 10,
    )

    /** Fort positions surrounding the central crown zone. */
    private val SiegeFortZones = setOf(
        3 to 6, 3 to 8, 6 to 3, 6 to 11,
        8 to 3, 8 to 11, 11 to 6, 11 to 8,
    )

    /** Tactical positions that create multiple attack routes rather than diagonals. */
    private val SiegeTacticalZones = setOf(
        1 to 3, 1 to 11, 3 to 1, 3 to 13,
        11 to 1, 11 to 13, 13 to 3, 13 to 11,
        5 to 5, 5 to 9, 9 to 5, 9 to 9,
    )
''',
    '''    /** Watch points on a wider outer ring so ×3 letters create contested approach lanes. */
    private val SiegeWatchZones = setOf(
        2 to 3, 2 to 11, 3 to 2, 3 to 12,
        11 to 2, 11 to 12, 12 to 3, 12 to 11,
    )

    /** Fort positions split between the crown ring and four diagonal staging points. */
    private val SiegeFortZones = setOf(
        4 to 4, 4 to 10, 10 to 4, 10 to 10,
        6 to 6, 6 to 8, 8 to 6, 8 to 8,
    )

    /** Tactical ×2 letters spread across edge approaches and central attack lanes. */
    private val SiegeTacticalZones = setOf(
        1 to 4, 1 to 10, 4 to 1, 4 to 13,
        10 to 1, 10 to 13, 13 to 4, 13 to 10,
        3 to 7, 7 to 3, 7 to 11, 11 to 7,
    )
''',
    'strategic bonus topology',
)
write(path, text)


# Stronger ownership and bonus hierarchy in both practice and live boards. ------------------
path = GAME / 'WordSiegePracticeBoard.kt'
text = read(path)
color_pairs = [
    ('PracticeSiegeMine = Color(0xFFA8D5B5)', 'PracticeSiegeMine = Color(0xFF8BD8AA)'),
    ('PracticeSiegeRival = Color(0xFFE4AEAA)', 'PracticeSiegeRival = Color(0xFFEDA09B)'),
    ('PracticeSiegeMineBorder = Color(0xFF3F7C53)', 'PracticeSiegeMineBorder = Color(0xFF2D7B4C)'),
    ('PracticeSiegeRivalBorder = Color(0xFF9B4D4A)', 'PracticeSiegeRivalBorder = Color(0xFFA33F3B)'),
    ('PracticeZoneWatch = Color(0xFFDCEAF2)', 'PracticeZoneWatch = Color(0xFFC8E7F4)'),
    ('PracticeZoneCritical = Color(0xFFDCEAF2)', 'PracticeZoneCritical = Color(0xFF9CCFEA)'),
    ('PracticeZoneFort = Color(0xFFEAE2F0)', 'PracticeZoneFort = Color(0xFFDEC8EF)'),
    ('PracticeZoneSiege = Color(0xFFEAE2F0)', 'PracticeZoneSiege = Color(0xFFC5A3E2)'),
    ('PracticeZoneCrown = Color(0xFFE7DDBB)', 'PracticeZoneCrown = Color(0xFFF1B95E)'),
    ('PracticeZoneReward = Color(0xFFEAD59B)', 'PracticeZoneReward = Color(0xFFF4C95F)'),
    ('PracticeSiegeBonusLabel = Color(0xFF68716D)', 'PracticeSiegeBonusLabel = Color(0xFF33445A)'),
]
for old, new in color_pairs:
    text = replace_once(text, old, new, old)
write(path, text)

path = GAME / 'WordSiegePanMatch.kt'
text = read(path)
color_pairs = [
    ('PanSiegeMine = Color(0xFFA8D5B5)', 'PanSiegeMine = Color(0xFF8BD8AA)'),
    ('PanSiegeRival = Color(0xFFE4AEAA)', 'PanSiegeRival = Color(0xFFEDA09B)'),
    ('PanSiegeMineBorder = Color(0xFF3F7C53)', 'PanSiegeMineBorder = Color(0xFF2D7B4C)'),
    ('PanSiegeRivalBorder = Color(0xFF9B4D4A)', 'PanSiegeRivalBorder = Color(0xFFA33F3B)'),
    ('PanSiegeBonus2H = Color(0xFFDCEAF2)', 'PanSiegeBonus2H = Color(0xFFC8E7F4)'),
    ('PanSiegeBonus3H = Color(0xFFDCEAF2)', 'PanSiegeBonus3H = Color(0xFF9CCFEA)'),
    ('PanSiegeBonus2K = Color(0xFFEAE2F0)', 'PanSiegeBonus2K = Color(0xFFDEC8EF)'),
    ('PanSiegeBonus3K = Color(0xFFEAE2F0)', 'PanSiegeBonus3K = Color(0xFFC5A3E2)'),
    ('PanSiegeBonus4K = Color(0xFFE7DDBB)', 'PanSiegeBonus4K = Color(0xFFF1B95E)'),
    ('PanSiegeBonusStar = Color(0xFFEAD59B)', 'PanSiegeBonusStar = Color(0xFFF4C95F)'),
    ('PanSiegeBonusLabel = Color(0xFF68716D)', 'PanSiegeBonusLabel = Color(0xFF33445A)'),
]
for old, new in color_pairs:
    text = replace_once(text, old, new, old)
write(path, text)


# Practice screen accents, real chat button, result animation/rematch prompt. ----------------
path = GAME / 'WordSiegePracticeScreen.kt'
text = read(path)
text = replace_once(
    text,
    'import androidx.compose.animation.core.animateIntAsState\nimport androidx.compose.animation.core.tween',
    'import androidx.compose.animation.core.animateFloatAsState\nimport androidx.compose.animation.core.animateIntAsState\nimport androidx.compose.animation.core.tween',
    'result animation import',
)
text = replace_once(
    text,
    'import androidx.compose.ui.graphics.Color\n',
    'import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.graphics.graphicsLayer\n',
    'graphics layer import',
)
for old, new in [
    ('PracticePlayerAccent = Color(0xFF567A64)', 'PracticePlayerAccent = Color(0xFF3C8E62)'),
    ('PracticePlayerFill = Color(0xFFA8C7B1)', 'PracticePlayerFill = Color(0xFF8FD6AA)'),
    ('PracticeRivalAccent = Color(0xFF9B4D4A)', 'PracticeRivalAccent = Color(0xFFA84642)'),
    ('PracticeRivalFill = Color(0xFFE4AEAA)', 'PracticeRivalFill = Color(0xFFEDA39E)'),
]:
    text = replace_once(text, old, new, old)
text = replace_once(
    text,
    '    var tutorialStep by remember { mutableIntStateOf(-1) }\n',
    '''    var tutorialStep by remember { mutableIntStateOf(-1) }
    var showChat by remember { mutableStateOf(false) }
    var chatDraft by remember { mutableStateOf("") }
    var chatMessages by remember { mutableStateOf<List<Pair<Boolean, String>>>(emptyList()) }
''',
    'practice chat state',
)
# Preserve opponent for true rematches, randomize only for explicit new game/restart.
text = replace_once(
    text,
    '''    fun startAgain() {
        state = WordSiegePracticeEngine.newGame(state.language)
        botProfile = WordSiegePracticeBots.random()
        botDecisionSalt = kotlin.random.Random.nextLong()
''',
    '''    fun resetMatch(changeOpponent: Boolean) {
        state = WordSiegePracticeEngine.newGame(state.language)
        if (changeOpponent) botProfile = WordSiegePracticeBots.random()
        botDecisionSalt = kotlin.random.Random.nextLong()
''',
    'reset match function',
)
text = replace_once(
    text,
    '''        clearSelection()
    }

    fun applyPlayerMove() {
''',
    '''        showChat = false
        chatDraft = ""
        chatMessages = emptyList()
        clearSelection()
    }

    fun startAgain() = resetMatch(changeOpponent = true)
    fun startRematch() = resetMatch(changeOpponent = false)

    fun applyPlayerMove() {
''',
    'reset match tail',
)
text = replace_once(
    text,
    '''                        WordSiegeSideAction(
                            sh("YARDIM", "HELP"),
                            Icons.Rounded.HelpOutline,
                            modifier = Modifier.width(74.dp),
                        ) { tutorialStep = 0 }
''',
    '''                        WordSiegeSideAction(
                            sh("SOHBET", "CHAT"),
                            Icons.Rounded.Chat,
                            modifier = Modifier.width(74.dp),
                        ) { showChat = true }
''',
    'bottom chat action',
)
text = replace_once(
    text,
    'sh("İki oyuncu art arda pas verirse maç biter.", "Two consecutive passes end the match."),',
    'sh("Torbada 20’den az harf varken art arda 4 pas ve/veya değişim maçı bitirir.", "When fewer than 20 tiles remain, 4 consecutive passes and/or exchanges end the match."),',
    'pass rule copy',
)
# Add result modal and practice chat before the zone info dialog.
insert_marker = '''    zoneInfoCode?.let { code ->
        WordSiegePracticeZoneInfoDialog(
'''
insert = '''    if (showChat) {
        AlertDialog(
            onDismissRequest = { showChat = false },
            title = { Text(sh("SOHBET • ${botProfile.name}", "CHAT • ${botProfile.name}"), fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (chatMessages.isEmpty()) {
                        Text(
                            sh("Botla kısa mesajlaşabilirsin.", "You can exchange short messages with the bot."),
                            color = WordSiegeGameUi.Muted,
                            fontSize = 12.sp,
                        )
                    } else {
                        chatMessages.takeLast(5).forEach { (mine, message) ->
                            Surface(
                                modifier = Modifier.align(if (mine) Alignment.End else Alignment.Start),
                                shape = RoundedCornerShape(10.dp),
                                color = if (mine) PracticePlayerAccent.copy(alpha = .13f) else PracticeRivalAccent.copy(alpha = .10f),
                            ) {
                                Text(
                                    (if (mine) sh("Sen: ", "You: ") else "${botProfile.name}: ") + message,
                                    Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                    color = WordSiegeGameUi.Text,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = chatDraft,
                        onValueChange = { chatDraft = it.take(80) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(sh("Mesaj", "Message")) },
                        trailingIcon = {
                            IconButton(
                                enabled = chatDraft.isNotBlank(),
                                onClick = {
                                    val message = chatDraft.trim()
                                    if (message.isNotEmpty()) {
                                        chatMessages = (chatMessages + (true to message) +
                                            (false to sh("İyi oyunlar!", "Good game!"))).takeLast(8)
                                        chatDraft = ""
                                    }
                                },
                            ) { Icon(Icons.Rounded.Send, sh("Gönder", "Send")) }
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showChat = false }) {
                    Text(sh("KAPAT", "CLOSE"), fontWeight = FontWeight.Black)
                }
            },
        )
    }

    if (state.status == "finished") {
        WordSiegePracticeResultDialog(
            winnerOwner = state.winnerOwner,
            opponentName = botProfile.name,
            playerScore = WordSiegePracticeEngine.totalScore(state, 1),
            botScore = WordSiegePracticeEngine.totalScore(state, 2),
            onRematch = ::startRematch,
            onExit = onExit,
        )
    }

'''
if insert_marker not in text:
    raise SystemExit('result/chat insertion marker not found')
text = text.replace(insert_marker, insert + insert_marker, 1)

# Append the animated result component before the existing score-card wrapper.
score_marker = '''@Composable
private fun WordSiegePracticeScoreCard(
'''
result_component = '''@Composable
private fun WordSiegePracticeResultDialog(
    winnerOwner: Int?,
    opponentName: String,
    playerScore: Int,
    botScore: Int,
    onRematch: () -> Unit,
    onExit: () -> Unit,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val scale by animateFloatAsState(
        targetValue = if (entered) 1f else .84f,
        animationSpec = tween(420),
        label = "practiceResultScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(300),
        label = "practiceResultAlpha",
    )
    val won = winnerOwner == 1
    val draw = winnerOwner == null
    val accent = when {
        won -> PracticePlayerAccent
        draw -> WordSiegeGameUi.Gold
        else -> PracticeRivalAccent
    }
    val title = when {
        won -> sh("KAZANDIN", "YOU WON")
        draw -> sh("BERABERE", "DRAW")
        else -> sh("KAYBETTİN", "YOU LOST")
    }

    AlertDialog(
        onDismissRequest = {},
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        },
        icon = {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = accent.copy(alpha = .14f),
                border = BorderStroke(1.dp, accent.copy(alpha = .35f)),
            ) {
                Icon(
                    Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.padding(10.dp).size(32.dp),
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = accent, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(
                    "$playerScore  —  $botScore",
                    color = WordSiegeGameUi.Text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    if (won) sh("$opponentName karşısında tahtı aldın.", "You took the throne against $opponentName.")
                    else if (draw) sh("Puanlar eşitlendi.", "The scores are tied.")
                    else sh("$opponentName bu maçı aldı.", "$opponentName won this match."),
                    color = WordSiegeGameUi.Muted,
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(sh("RÖVANŞ?", "REMATCH?"), color = WordSiegeGameUi.Text, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Text(sh("Aynı rakiple hemen tekrar oyna.", "Play the same opponent again now."), color = WordSiegeGameUi.Muted, fontSize = 11.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = onRematch,
                colors = ButtonDefaults.buttonColors(containerColor = PracticePlayerAccent, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
            ) { Text(sh("EVET", "YES"), fontWeight = FontWeight.Black) }
        },
        dismissButton = {
            OutlinedButton(onClick = onExit, shape = RoundedCornerShape(10.dp)) {
                Text(sh("HAYIR", "NO"), fontWeight = FontWeight.Black)
            }
        },
    )
}

'''
if score_marker not in text:
    raise SystemExit('score wrapper marker not found')
text = text.replace(score_marker, result_component + score_marker, 1)
write(path, text)


# Practice rules: seven-tile rack, Kelimelik-style 4 pass/exchange rule only below 20 bag tiles. --
path = GAME / 'WordSiegePracticeEngine.kt'
text = read(path)
text = replace_once(
    text,
    'internal object WordSiegePracticeEngine {\n',
    '''internal object WordSiegePracticeEngine {
    private const val PracticeInactivityLimit = 4
    private const val PracticeInactivityBagThreshold = 20
''',
    'practice inactivity constants',
)
text = replace_once(text, 'playerRack = shuffled.take(7),', 'playerRack = shuffled.take(WordSiegeBoardSpec.RackSize),', 'player rack size')
text = replace_once(text, 'botRack = shuffled.drop(7).take(7),', 'botRack = shuffled.drop(WordSiegeBoardSpec.RackSize).take(WordSiegeBoardSpec.RackSize),', 'bot rack size')
text = replace_once(text, 'bag = shuffled.drop(14),', 'bag = shuffled.drop(WordSiegeBoardSpec.RackSize * 2),', 'initial bag offset')
text = replace_once(text, 'if (placements.size !in 1..7)', 'if (placements.size !in 1..WordSiegeBoardSpec.RackSize)', 'placement rack limit')
text = replace_once(text, 'val drawCount = (7 - remainingRack.length).coerceAtLeast(0)', 'val drawCount = (WordSiegeBoardSpec.RackSize - remainingRack.length).coerceAtLeast(0)', 'rack refill count')
text = replace_once(
    text,
    '''        val next = state.copy(
            currentOwner = other(owner),
            consecutivePasses = (state.consecutivePasses + 1).coerceAtMost(2),
            moveCount = state.moveCount + 1,
            lastAction = "pass",
        )
        return if (next.consecutivePasses >= 2) finish(next, "consecutive_passes") else next
''',
    '''        val next = state.copy(
            currentOwner = other(owner),
            consecutivePasses = (state.consecutivePasses + 1).coerceAtMost(PracticeInactivityLimit),
            moveCount = state.moveCount + 1,
            lastAction = "pass",
        )
        return finishForInactivityIfNeeded(next)
''',
    'practice pass finish rule',
)
text = replace_once(
    text,
    'if (rackIndices.isEmpty() || rackIndices.size > 7 || rackIndices.any',
    'if (rackIndices.isEmpty() || rackIndices.size > WordSiegeBoardSpec.RackSize || rackIndices.any',
    'exchange rack limit',
)
text = replace_once(
    text,
    '''        return state.copy(
            bag = nextBag,
            playerRack = if (owner == 1) remain + draw else state.playerRack,
            botRack = if (owner == 2) remain + draw else state.botRack,
            currentOwner = other(owner),
            consecutivePasses = 0,
            moveCount = state.moveCount + 1,
            lastAction = "exchange",
        )
''',
    '''        val next = state.copy(
            bag = nextBag,
            playerRack = if (owner == 1) remain + draw else state.playerRack,
            botRack = if (owner == 2) remain + draw else state.botRack,
            currentOwner = other(owner),
            consecutivePasses = (state.consecutivePasses + 1).coerceAtMost(PracticeInactivityLimit),
            moveCount = state.moveCount + 1,
            lastAction = "exchange",
        )
        return finishForInactivityIfNeeded(next)
''',
    'exchange participates in inactivity finish',
)
text = replace_once(
    text,
    '''    private fun finish(state: WordSiegePracticeState, reason: String, forcedWinner: Int? = null): WordSiegePracticeState {
        val winner = forcedWinner ?: when {
            state.playerArea > state.botArea -> 1
            state.botArea > state.playerArea -> 2
            else -> null
        }
        return state.copy(status = "finished", winnerOwner = winner, lastAction = reason)
    }
''',
    '''    private fun finishForInactivityIfNeeded(state: WordSiegePracticeState): WordSiegePracticeState {
        val eligible = state.bag.length < PracticeInactivityBagThreshold
        return if (eligible && state.consecutivePasses >= PracticeInactivityLimit) {
            finish(state, "consecutive_passes")
        } else state
    }

    private fun finish(state: WordSiegePracticeState, reason: String, forcedWinner: Int? = null): WordSiegePracticeState {
        val playerScore = totalScore(state, 1)
        val botScore = totalScore(state, 2)
        val winner = forcedWinner ?: when {
            playerScore > botScore -> 1
            botScore > playerScore -> 2
            else -> null
        }
        return state.copy(status = "finished", winnerOwner = winner, lastAction = reason)
    }
''',
    'practice finish scoring',
)
write(path, text)


# Tests: update intentional visual/rule contracts rather than weakening them. ----------------
path = TEST / 'SimpleKelimeTahtiRestoreContractTest.kt'
text = read(path)
for old, new in [
    ('PanSiegeMine = Color(0xFFA8D5B5)', 'PanSiegeMine = Color(0xFF8BD8AA)'),
    ('PanSiegeRival = Color(0xFFE4AEAA)', 'PanSiegeRival = Color(0xFFEDA09B)'),
    ('PracticeSiegeMine = Color(0xFFA8D5B5)', 'PracticeSiegeMine = Color(0xFF8BD8AA)'),
    ('PracticeSiegeRival = Color(0xFFE4AEAA)', 'PracticeSiegeRival = Color(0xFFEDA09B)'),
]:
    text = replace_once(text, old, new, f'test {old}')
write(path, text)

path = TEST / 'WordSiegeFinalRulesTest.kt'
text = read(path)
text = replace_once(text, 'assertTrue(pan.contains("0xFFA8D5B5"))', 'assertTrue(pan.contains("0xFF8BD8AA"))', 'final rules vivid green')
text = replace_once(text, 'assertTrue(pan.contains("0xFFE4AEAA"))', 'assertTrue(pan.contains("0xFFEDA09B"))', 'final rules vivid red')
write(path, text)

path = TEST / 'WordSiegeBoardSpecTest.kt'
text = read(path)
text = replace_once(text, 'assertEquals(225, WordSiegeBoardSpec.CellCount)', 'assertEquals(225, WordSiegeBoardSpec.CellCount)\n        assertEquals(7, WordSiegeBoardSpec.RackSize)', 'board rack size test')
text = replace_once(text, 'assertEquals("3H", WordSiegeBoardSpec.bonusAt(WordSiegeBoardSpec.index(2, 4)))', 'assertEquals("3H", WordSiegeBoardSpec.bonusAt(WordSiegeBoardSpec.index(2, 3)))', '3H strategic coordinate')
text = replace_once(text, 'assertEquals("2K", WordSiegeBoardSpec.bonusAt(WordSiegeBoardSpec.index(3, 6)))', 'assertEquals("2K", WordSiegeBoardSpec.bonusAt(WordSiegeBoardSpec.index(4, 4)))', '2K strategic coordinate')
text = replace_once(text, 'assertEquals("2H", WordSiegeBoardSpec.bonusAt(WordSiegeBoardSpec.index(5, 5)))', 'assertEquals("2H", WordSiegeBoardSpec.bonusAt(WordSiegeBoardSpec.index(3, 7)))', '2H strategic coordinate')
write(path, text)

path = TEST / 'WordSiegePracticeEngineTest.kt'
text = read(path)
text = replace_once(
    text,
    '''    @Test
    fun consecutivePassesFinishPractice() {
        val first = WordSiegePracticeEngine.pass(WordSiegePracticeEngine.newGame(random = Random(1)), 1)
        val finished = WordSiegePracticeEngine.pass(first, 2)

        assertEquals("finished", finished.status)
        assertEquals("consecutive_passes", finished.lastAction)
    }
''',
    '''    @Test
    fun fourPassOrExchangeActionsOnlyFinishWhenBagIsBelowTwenty() {
        val fresh = WordSiegePracticeEngine.newGame(random = Random(1))
        var state = fresh.copy(bag = "A".repeat(21))
        repeat(4) { turn ->
            state = WordSiegePracticeEngine.pass(state, if (turn % 2 == 0) 1 else 2)
        }
        assertEquals("playing", state.status)

        state = state.copy(bag = "A".repeat(19), consecutivePasses = 0, currentOwner = 1)
        state = WordSiegePracticeEngine.pass(state, 1)
        state = WordSiegePracticeEngine.exchange(state, 2, setOf(0))
        state = WordSiegePracticeEngine.pass(state, 1)
        state = WordSiegePracticeEngine.exchange(state, 2, setOf(0))

        assertEquals("finished", state.status)
        assertEquals("consecutive_passes", state.lastAction)
    }
''',
    'practice four-action finish test',
)
write(path, text)

path = TEST / 'WordSiegePracticeUxAndBotRegressionTest.kt'
text = read(path)
text = replace_once(
    text,
    '        assertTrue(screen.contains("Icons.Rounded.HelpOutline"))\n',
    '        assertTrue(screen.contains("Icons.Rounded.HelpOutline"))\n        assertTrue(screen.contains("sh(\\"SOHBET\\", \\"CHAT\\")"))\n        assertTrue(screen.contains("WordSiegePracticeResultDialog("))\n',
    'practice chat and result contracts',
)
write(path, text)

print('Siege match finish v34 polish applied.')
