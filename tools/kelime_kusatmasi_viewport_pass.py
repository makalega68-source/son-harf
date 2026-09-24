from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def write(path: str, value: str) -> None:
    (ROOT / path).write_text(value, encoding="utf-8")


def replace_once(path: str, old: str, new: str) -> None:
    value = read(path)
    count = value.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected one match, got {count}: {old[:100]!r}")
    write(path, value.replace(old, new, 1))


def regex_once(path: str, pattern: str, replacement: str) -> None:
    value = read(path)
    updated, count = re.subn(pattern, replacement, value, count=1, flags=re.S)
    if count != 1:
        raise RuntimeError(f"{path}: expected one regex match, got {count}: {pattern[:100]!r}")
    write(path, updated)


pan = "app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt"
viewport = "app/src/main/java/com/sonharf/game/WordSiegeBoardViewport.kt"
ui = "app/src/main/java/com/sonharf/game/WordSiegeGameUi.kt"
contract = "app/src/test/java/com/sonharf/game/WordSiegePanAreaContractTest.kt"
viewport_test = "app/src/test/java/com/sonharf/game/WordSiegeBoardViewportTest.kt"

# Hoist viewport mode so CLOSE can reclaim vertical space while keeping player cards visible.
replace_once(
    pan,
    '    var shuffleSeed by remember(game.id) { mutableIntStateOf(0) }\n',
    '    var shuffleSeed by remember(game.id) { mutableIntStateOf(0) }\n'
    '    var boardViewportMode by remember(game.id) { mutableStateOf(WordSiegeBoardViewportMode.FIT) }\n',
)

regex_once(
    pan,
    r'''(    \) \{\n)(        Row\(verticalAlignment = Alignment\.CenterVertically\) \{.*?\n        \}\n\n)(        Row\(Modifier\.fillMaxWidth\(\), horizontalArrangement = Arrangement\.spacedBy\(7\.dp\)\) \{)''',
    r'''\1        if (boardViewportMode == WordSiegeBoardViewportMode.FIT) {\n\2        }\n\n\3''',
)

replace_once(
    pan,
    '        WordSiegeOwnershipLegend()\n',
    '        if (boardViewportMode == WordSiegeBoardViewportMode.FIT) WordSiegeOwnershipLegend()\n',
)

replace_once(
    pan,
    '''            lastMove = lastMove,\n            modifier = Modifier.fillMaxWidth().aspectRatio(1f),\n            onCell = onBoardCell,''',
    '''            lastMove = lastMove,\n            viewportMode = boardViewportMode,\n            onViewportModeChange = { boardViewportMode = it },\n            modifier = Modifier.fillMaxSize(),\n            onCell = onBoardCell,''',
)

replace_once(
    pan,
    '''            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {\n                if (placements.isNotEmpty()) {\n                    Text(readyFeedback.message, color = PanSiegeMineBorder, fontSize = 9.sp, fontWeight = FontWeight.Black)\n                } else Spacer(Modifier.weight(1f))\n                Spacer(Modifier.weight(1f))\n                Text(sh("Torba ${game.bag.length}", "Bag ${game.bag.length}"), color = WordSiegeGameUi.Muted, fontSize = 8.sp)\n            }\n\n            WordSiegePremiumPanel(\n                game = game,\n                placements = placements,\n                canAct = canAct,\n            )''',
    '''            if (boardViewportMode == WordSiegeBoardViewportMode.FIT) {\n                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {\n                    if (placements.isNotEmpty()) {\n                        Text(readyFeedback.message, color = PanSiegeMineBorder, fontSize = 9.sp, fontWeight = FontWeight.Black)\n                    } else Spacer(Modifier.weight(1f))\n                    Spacer(Modifier.weight(1f))\n                    Text(sh("Torba ${game.bag.length}", "Bag ${game.bag.length}"), color = WordSiegeGameUi.Muted, fontSize = 8.sp)\n                }\n\n                WordSiegePremiumPanel(\n                    game = game,\n                    placements = placements,\n                    canAct = canAct,\n                )\n            }''',
)

replace_once(
    pan,
    '        lastMove?.let { PanSiegeLastMoveInfo(it) }\n',
    '        if (boardViewportMode == WordSiegeBoardViewportMode.FIT) lastMove?.let { PanSiegeLastMoveInfo(it) }\n',
)

replace_once(
    pan,
    '''    lastMove: WordSiegeMoveDto?,\n    modifier: Modifier = Modifier,\n    onCell: (Int) -> Unit,''',
    '''    lastMove: WordSiegeMoveDto?,\n    viewportMode: WordSiegeBoardViewportMode,\n    onViewportModeChange: (WordSiegeBoardViewportMode) -> Unit,\n    modifier: Modifier = Modifier,\n    onCell: (Int) -> Unit,''',
)
replace_once(
    pan,
    '    var viewportMode by remember(gameId) { mutableStateOf(WordSiegeBoardViewportMode.FIT) }\n',
    '',
)
replace_once(
    pan,
    '        viewportMode = nextMode\n',
    '        onViewportModeChange(nextMode)\n',
)
replace_once(
    pan,
    '''                onClick = {\n                    viewportMode = WordSiegeBoardViewportMode.CLOSE\n                    closePan = centerCloseOn(WordSiegeBoardSpec.CenterIndex)\n                },''',
    '''                onClick = { toggleViewport(WordSiegeBoardSpec.CenterIndex) },''',
)

# Light satin-wood board bed: gaps stay warm and bright instead of reading as dark seams.
replace_once(
    pan,
    '                        listOf(Color(0xFF3B2518), Color(0xFF6A452B), Color(0xFF4A2F1E), Color(0xFF785238))\n',
    '                        listOf(Color(0xFFE8D8BF), Color(0xFFD8BE98), Color(0xFFF1E6D4), Color(0xFFCFAE7E))\n',
)
replace_once(pan, '    val regionGap = 1.25.dp\n', '    val regionGap = 0.9.dp\n')
replace_once(
    pan,
    '    val regionGap = 0.9.dp\n\n    Box(\n',
    '    val regionGap = 0.9.dp\n    val boardInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }\n\n    Box(\n',
)

old_modifier = '''        Modifier\n            .size(size)\n            .padding(regionGap)\n            .clip(RoundedCornerShape(7.dp))\n            .background(\n                Brush.linearGradient(\n                    listOf(\n                        androidx.compose.ui.graphics.lerp(displayBase, Color.White, .18f),\n                        displayBase,\n                        androidx.compose.ui.graphics.lerp(displayBase, Color.Black, .07f),\n                    )\n                )\n            )\n            .border(\n                width = if (lastMoveHighlight > 0f) 1.75.dp else 0.dp,\n                color = PanSiegeLastMove.copy(alpha = .45f + .45f * lastMoveHighlight),\n                shape = RoundedCornerShape(7.dp),\n            )\n            .combinedClickable(\n                onClick = {\n                    dispatchWordSiegeBoardTap(\n                        WordSiegeBoardTapAction.PLACE,\n                        canPlace,\n                        onClick,\n                        onDoubleClick,\n                    )\n                },\n                onDoubleClick = {\n                    dispatchWordSiegeBoardTap(\n                        WordSiegeBoardTapAction.TOGGLE_VIEWPORT,\n                        canPlace,\n                        onClick,\n                        onDoubleClick,\n                    )\n                },\n            ),'''
new_modifier = '''        Modifier\n            .size(size)\n            .combinedClickable(\n                interactionSource = boardInteraction,\n                indication = null,\n                onClick = {\n                    dispatchWordSiegeBoardTap(\n                        WordSiegeBoardTapAction.PLACE,\n                        canPlace,\n                        onClick,\n                        onDoubleClick,\n                    )\n                },\n                onDoubleClick = {\n                    dispatchWordSiegeBoardTap(\n                        WordSiegeBoardTapAction.TOGGLE_VIEWPORT,\n                        canPlace,\n                        onClick,\n                        onDoubleClick,\n                    )\n                },\n            )\n            .padding(regionGap)\n            .clip(RoundedCornerShape(7.dp))\n            .background(\n                Brush.linearGradient(\n                    listOf(\n                        androidx.compose.ui.graphics.lerp(displayBase, Color.White, .18f),\n                        displayBase,\n                        androidx.compose.ui.graphics.lerp(displayBase, Color.Black, .07f),\n                    )\n                )\n            )\n            .border(\n                width = if (lastMoveHighlight > 0f) 1.75.dp else 0.dp,\n                color = PanSiegeLastMove.copy(alpha = .45f + .45f * lastMoveHighlight),\n                shape = RoundedCornerShape(7.dp),\n            ),'''
replace_once(pan, old_modifier, new_modifier)

# Closer view now matches the reference density: roughly seven cells across on a 390dp phone.
replace_once(viewport, 'internal const val WORD_SIEGE_ONLINE_ZOOM_FACTOR = 1.85f\n', 'internal const val WORD_SIEGE_ONLINE_ZOOM_FACTOR = 2.20f\n')
replace_once(viewport, 'internal const val WORD_SIEGE_ONLINE_MAX_CLOSE_SCALE = 1f\n', 'internal const val WORD_SIEGE_ONLINE_MAX_CLOSE_SCALE = 1.12f\n')

# Player identity must remain readable while the board expands.
replace_once(ui, '                            size = 30.dp, accent = accent, visible = avatarVisible,\n', '                            size = 38.dp, accent = accent, visible = avatarVisible,\n')

# Update the source contract intentionally for the requested new viewport density.
replace_once(contract, 'assertTrue(viewport.contains("WORD_SIEGE_ONLINE_ZOOM_FACTOR = 1.85f"))', 'assertTrue(viewport.contains("WORD_SIEGE_ONLINE_ZOOM_FACTOR = 2.20f"))')
replace_once(
    viewport_test,
    '''    @Test\n    fun `fit scale uses exact minimum viewport ratio and centers the full board`() {''',
    '''    @Test\n    fun `online close scale shows about seven cells across on a 390dp phone`() {\n        val scale = wordSiegeOnlineCloseScale(390f, 500f, boardPx)\n        assertEquals(1.10f, scale, tolerance)\n        assertTrue(scale > wordSiegeFitScale(390f, 500f, boardPx) * 2f)\n    }\n\n    @Test\n    fun `fit scale uses exact minimum viewport ratio and centers the full board`() {''',
)

print("Kelime Kusatmasi viewport polish applied")
