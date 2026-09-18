from pathlib import Path
import re

path = Path('app/src/main/java/com/sonharf/game/LetterLadderGame.kt')
text = path.read_text()

pattern = re.compile(
    r'''    /\*\*\n \* Counts only next moves.*?\nfun generate\(''',
    re.S,
)
replacement = '''    /**
     * Returns only positions that can be changed next while preserving at least one full route.
     * The UI may reveal a position as a one-use hint, but never receives the replacement letter
     * or the next solution word from this helper.
     */
    fun viableNextMoveIndices(
        puzzle: LetterLadderPuzzle,
        current: String,
        usedPositions: Set<Int>,
        dictionary: Set<String>,
    ): List<Int> {
        if (current.length != WORD_LENGTH || puzzle.target.length != WORD_LENGTH) return emptyList()
        if (current == puzzle.target) return emptyList()

        return (0 until WORD_LENGTH).filter { index ->
            if (index in usedPositions || current[index] == puzzle.target[index]) return@filter false
            val chars = current.toCharArray()
            chars[index] = puzzle.target[index]
            val next = String(chars)
            if (next !in dictionary) return@filter false

            next == puzzle.target || completionPath(
                puzzle = puzzle,
                current = next,
                usedPositions = usedPositions + index,
                dictionary = dictionary,
            ) != null
        }
    }

    fun viableNextMoveCount(
        puzzle: LetterLadderPuzzle,
        current: String,
        usedPositions: Set<Int>,
        dictionary: Set<String>,
    ): Int = viableNextMoveIndices(puzzle, current, usedPositions, dictionary).size

    fun generate('''
text, count = pattern.subn(replacement, text, count=1)
if count != 1:
    raise SystemExit(f'Hint engine block replacement count={count}')

old = '''    var hintText by remember { mutableStateOf<String?>(null) }
    var successVfxNonce by remember { mutableIntStateOf(0) }
'''
new = '''    var hintText by remember { mutableStateOf<String?>(null) }
    var hintUsed by remember { mutableStateOf(false) }
    var hintedIndex by remember { mutableStateOf<Int?>(null) }
    var successVfxNonce by remember { mutableIntStateOf(0) }
'''
if old not in text:
    raise SystemExit('Hint state anchor not found')
text = text.replace(old, new, 1)

old = '''        completed = false
        hintText = null
        input = ""
        successVfxNonce = 0
'''
new = '''        completed = false
        hintText = null
        hintUsed = false
        hintedIndex = null
        input = ""
        successVfxNonce = 0
'''
if old not in text:
    raise SystemExit('New puzzle reset anchor not found')
text = text.replace(old, new, 1)

old = '''        input = ""
        hintText = null
        completed = false
        successVfxNonce = 0
'''
new = '''        input = ""
        hintText = null
        hintedIndex = null
        completed = false
        successVfxNonce = 0
'''
if old not in text:
    raise SystemExit('Manual reset anchor not found')
text = text.replace(old, new, 1)

old = '''        path = path + normalized
        usedPositions = nextUsed
        input = ""
        hintText = null
        successVfxNonce += 1
'''
new = '''        path = path + normalized
        usedPositions = nextUsed
        input = ""
        hintText = null
        hintedIndex = null
        successVfxNonce += 1
'''
if old not in text:
    raise SystemExit('Successful move anchor not found')
text = text.replace(old, new, 1)

old = '''                            input = ""
                            hintText = null
                            message = sh("Son hamle geri alındı.", "Last move undone.")
'''
new = '''                            input = ""
                            hintText = null
                            hintedIndex = null
                            message = sh("Son hamle geri alındı.", "Last move undone.")
'''
if old not in text:
    raise SystemExit('Undo anchor not found')
text = text.replace(old, new, 1)

old = '''    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().background(LetterLadderUi.Background),
        ) {
'''
new = '''    Box(Modifier.fillMaxSize()) {
        FirstRunLanguageBackdrop(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize(),
        ) {
'''
if old not in text:
    raise SystemExit('Page background anchor not found')
text = text.replace(old, new, 1)

old_surface = '''                    color = LetterLadderUi.SurfaceRaised,
                    border = BorderStroke(1.dp, LetterLadderUi.Border),
'''
new_surface = '''                    color = LetterLadderUi.SurfaceRaised.copy(alpha = .94f),
                    border = BorderStroke(1.dp, LetterLadderUi.Border),
'''
if old_surface not in text:
    raise SystemExit('Main surface anchor not found')
text = text.replace(old_surface, new_surface, 1)

old = '''                        LadderWordTiles(
                            word = currentPuzzle.start.uppercase(locale),
                            locked = emptySet(),
                            accent = LetterLadderUi.Accent,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        )
'''
new = '''                        LadderWordTiles(
                            word = currentPuzzle.start.uppercase(locale),
                            locked = emptySet(),
                            accent = LetterLadderUi.Accent,
                            hintedIndex = hintedIndex.takeIf { path.size == 1 },
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        )
'''
if old not in text:
    raise SystemExit('Start row anchor not found')
text = text.replace(old, new, 1)

old = '''                        for (move in 1 until LetterLadderEngine.MOVE_COUNT) {
                            LadderMoveRow(
                                word = path.getOrNull(move)?.uppercase(locale),
                                usedPositions = usedPositions,
                                modifier = Modifier.fillMaxWidth().weight(1f),
                            )
                            if (move < LetterLadderEngine.MOVE_COUNT - 1) Spacer(Modifier.height(3.dp))
                        }
'''
new = '''                        for (move in 1 until LetterLadderEngine.MOVE_COUNT) {
                            val committedWord = path.getOrNull(move)?.uppercase(locale)
                            val isCurrentWord = committedWord != null && move == path.lastIndex
                            val isActiveEntry = committedWord == null && move == path.size
                            LadderMoveRow(
                                word = committedWord,
                                activeInput = input.uppercase(locale).takeIf { isActiveEntry },
                                usedPositions = usedPositions,
                                hintedIndex = hintedIndex.takeIf { isCurrentWord || isActiveEntry },
                                modifier = Modifier.fillMaxWidth().weight(1f),
                            )
                            if (move < LetterLadderEngine.MOVE_COUNT - 1) Spacer(Modifier.height(3.dp))
                        }
'''
if old not in text:
    raise SystemExit('Intermediate rows anchor not found')
text = text.replace(old, new, 1)

old = '''                    OutlinedButton(
                        enabled = !completed,
                        onClick = {
                            val current = path.last()
                            val safeOptions = LetterLadderEngine.viableNextMoveCount(
                                puzzle = currentPuzzle,
                                current = current,
                                usedPositions = usedPositions,
                                dictionary = dictionary,
                            )
                            val remainingMoves = LetterLadderEngine.MOVE_COUNT - usedPositions.size
                            hintText = if (safeOptions == 0) {
                                sh("Son hamleyi geri al ve farklı bir yol dene.", "Undo the last move and try a different route.")
                            } else {
                                sh(
                                    "İpucu: $remainingMoves hamle kaldı • $safeOptions güvenli hamle seçeneği var.",
                                    "Hint: $remainingMoves moves left • $safeOptions safe move option(s) remain.",
                                )
                            }
                            SonHarfSoundFx.puzzleHint()
                        },
                        modifier = Modifier.weight(1f).height(38.dp).sonHarfPressScale(pressedScale = 0.94f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Rounded.Lightbulb, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(sh("İPUCU", "HINT"), fontWeight = FontWeight.Black, fontSize = 10.sp, maxLines = 1)
                    }
'''
new = '''                    OutlinedButton(
                        enabled = !completed && !hintUsed,
                        onClick = {
                            val current = path.last()
                            val hintIndex = LetterLadderEngine.viableNextMoveIndices(
                                puzzle = currentPuzzle,
                                current = current,
                                usedPositions = usedPositions,
                                dictionary = dictionary,
                            ).firstOrNull()
                            hintUsed = true
                            hintedIndex = hintIndex
                            hintText = if (hintIndex == null) {
                                sh("Son hamleyi geri al ve farklı bir yol dene.", "Undo the last move and try a different route.")
                            } else {
                                sh(
                                    "İpucu kullanıldı: sarı işaretli harfi değiştir. Harfi kendin bul.",
                                    "Hint used: change the yellow-marked letter. Find the letter yourself.",
                                )
                            }
                            SonHarfSoundFx.puzzleHint()
                        },
                        modifier = Modifier.weight(1f).height(38.dp).sonHarfPressScale(pressedScale = 0.94f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Rounded.Lightbulb, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(
                            if (hintUsed) sh("İPUCU 0/1", "HINT 0/1") else sh("İPUCU 1/1", "HINT 1/1"),
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            maxLines = 1,
                        )
                    }
'''
if old not in text:
    raise SystemExit('Hint button block not found')
text = text.replace(old, new, 1)

pattern = re.compile(
    r'''@Composable\nprivate fun LadderMoveRow\(.*?\n}\n\n@Composable\nprivate fun LadderWordTiles\(.*?\n}\n\n@Composable\ninternal fun MonsterLetterLadderQuickCard''',
    re.S,
)
replacement = '''@Composable
private fun LadderMoveRow(
    word: String?,
    activeInput: String? = null,
    usedPositions: Set<Int>,
    hintedIndex: Int? = null,
    modifier: Modifier = Modifier,
) {
    val display = word ?: activeInput?.padEnd(LetterLadderEngine.WORD_LENGTH, ' ') ?: "     "
    LadderWordTiles(
        word = display,
        locked = usedPositions,
        accent = if (word != null) LetterLadderUi.Green else LetterLadderUi.Accent,
        hintedIndex = hintedIndex,
        modifier = modifier,
    )
}

@Composable
private fun LadderWordTiles(
    word: String,
    locked: Set<Int>,
    accent: Color,
    hintedIndex: Int? = null,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(5) { index ->
            val char = word.getOrNull(index)?.takeUnless { it == ' ' }?.toString().orEmpty()
            val hinted = index == hintedIndex
            Surface(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(8.dp),
                color = when {
                    hinted -> LetterLadderUi.Gold.copy(alpha = .20f)
                    index in locked -> LetterLadderUi.Green.copy(alpha = .13f)
                    char.isNotEmpty() -> accent.copy(alpha = .09f)
                    else -> LetterLadderUi.SurfaceSoft
                },
                border = BorderStroke(
                    if (hinted) 2.dp else 1.dp,
                    when {
                        hinted -> LetterLadderUi.Gold
                        index in locked -> LetterLadderUi.Green.copy(alpha = .55f)
                        char.isNotEmpty() -> accent.copy(alpha = .40f)
                        else -> LetterLadderUi.Border
                    },
                ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        char,
                        color = if (hinted) LetterLadderUi.Gold else LetterLadderUi.Text,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@Composable
internal fun MonsterLetterLadderQuickCard'''
text, count = pattern.subn(replacement, text, count=1)
if count != 1:
    raise SystemExit(f'Ladder row/tile replacement count={count}')

path.write_text(text)

test_path = Path('app/src/test/java/com/sonharf/game/LetterLadderUxRegressionTest.kt')
test = test_path.read_text()
old = '''        assertTrue(source.contains("viableNextMoveCount("))
        assertTrue(source.contains("Bu hamle çıkmaza götürüyor"))
        assertTrue(source.contains("güvenli hamle seçeneği"))
'''
new = '''        assertTrue(source.contains("viableNextMoveIndices("))
        assertTrue(source.contains("Bu hamle çıkmaza götürüyor"))
        assertTrue(source.contains("sarı işaretli harfi değiştir"))
'''
if old not in test:
    raise SystemExit('Reliable hint test anchors not found')
test = test.replace(old, new, 1)

old_test = '''    @Test
    fun harfYoluHintDoesNotRevealTheNextAnswer() {
        val source = projectFile("app/src/main/java/com/sonharf/game/LetterLadderGame.kt").readText()

        assertFalse(source.contains("next.uppercase(locale)"))
        assertFalse(source.contains("val from = current[changed]"))
        assertFalse(source.contains("val to = next[changed]"))
        assertTrue(source.contains("safeOptions"))
        assertTrue(source.contains("remainingMoves"))
    }
'''
new_test = '''    @Test
    fun harfYoluHintIsSingleUseAndRevealsOnlyThePosition() {
        val source = projectFile("app/src/main/java/com/sonharf/game/LetterLadderGame.kt").readText()

        assertFalse(source.contains("next.uppercase(locale)"))
        assertFalse(source.contains("val from = current[changed]"))
        assertFalse(source.contains("val to = next[changed]"))
        assertTrue(source.contains("var hintUsed by remember { mutableStateOf(false) }"))
        assertTrue(source.contains("enabled = !completed && !hintUsed"))
        assertTrue(source.contains("hintedIndex = hintIndex"))
        assertTrue(source.contains("İPUCU 1/1"))
        assertTrue(source.contains("sarı işaretli harfi değiştir"))
    }
'''
if old_test not in test:
    raise SystemExit('Old hint UX test not found')
test = test.replace(old_test, new_test, 1)

old_test = '''    @Test
    fun harfYoluDoesNotMirrorLiveInputIntoTheActiveRow() {
        val source = projectFile("app/src/main/java/com/sonharf/game/LetterLadderGame.kt").readText()

        assertFalse(source.contains("activeInput = input.uppercase(locale)"))
        assertFalse(source.contains("activeInput.padEnd"))
        assertTrue(source.contains("val display = word ?: \\"     \\""))
    }
'''
new_test = '''    @Test
    fun harfYoluShowsLiveKeyboardInputInTheNextPlayableRow() {
        val source = projectFile("app/src/main/java/com/sonharf/game/LetterLadderGame.kt").readText()
        val keyboard = projectFile("app/src/main/java/com/sonharf/game/HarfYoluKeyboard.kt").readText()

        assertTrue(source.contains("activeInput = input.uppercase(locale).takeIf { isActiveEntry }"))
        assertTrue(source.contains("activeInput?.padEnd(LetterLadderEngine.WORD_LENGTH, ' ')"))
        assertTrue(keyboard.contains("onValueChange((value + key).take(maxLength))"))
    }

    @Test
    fun harfYoluUsesTheSharedDecorativeBackdrop() {
        val source = projectFile("app/src/main/java/com/sonharf/game/LetterLadderGame.kt").readText()

        assertTrue(source.contains("FirstRunLanguageBackdrop(Modifier.fillMaxSize())"))
        assertFalse(source.contains("Modifier.fillMaxSize().background(LetterLadderUi.Background)"))
    }
'''
if old_test not in test:
    raise SystemExit('Old live-input UX test not found')
test = test.replace(old_test, new_test, 1)
test_path.write_text(test)

engine_test_path = Path('app/src/test/java/com/sonharf/game/LetterLadderEngineTest.kt')
engine_test = engine_test_path.read_text()
anchor = '''    @Test
    fun safeHintCountsChoicesWithoutExposingAWord() {
        assertEquals(
            1,
            LetterLadderEngine.viableNextMoveCount(
                puzzle = puzzle,
                current = puzzle.start,
                usedPositions = emptySet(),
                dictionary = dictionary,
            ),
        )
    }
'''
replacement = anchor + '''
    @Test
    fun safeHintReturnsOnlyThePositionThatCanContinue() {
        assertEquals(
            listOf(0),
            LetterLadderEngine.viableNextMoveIndices(
                puzzle = puzzle,
                current = puzzle.start,
                usedPositions = emptySet(),
                dictionary = dictionary,
            ),
        )
    }
'''
if anchor not in engine_test:
    raise SystemExit('Engine hint test anchor not found')
engine_test = engine_test.replace(anchor, replacement, 1)
engine_test_path.write_text(engine_test)
