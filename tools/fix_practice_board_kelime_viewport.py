from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path):
    return (ROOT / path).read_text(encoding="utf-8")


def write(path, value):
    (ROOT / path).write_text(value, encoding="utf-8")


def replace_once(path, old, new):
    value = read(path)
    count = value.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected 1 match, got {count}: {old[:120]!r}")
    write(path, value.replace(old, new, 1))


board = "app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt"
screen = "app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt"
viewport = "app/src/main/java/com/sonharf/game/WordSiegeBoardViewport.kt"
ui = "app/src/main/java/com/sonharf/game/WordSiegeGameUi.kt"

# One double tap should create a genuinely close Kelimelik-like mobile viewport.
replace_once(
    viewport,
    "internal const val WORD_SIEGE_PRACTICE_CLOSE_SCALE = 0.68f\n",
    "internal const val WORD_SIEGE_PRACTICE_CLOSE_SCALE = 0.68f\ninternal const val WORD_SIEGE_PRACTICE_DOUBLE_TAP_SCALE = 1.0f\n",
)

replace_once(
    board,
    "    onCell: (Int) -> Unit,\n) {",
    "    onViewportModeChange: (WordSiegeBoardViewportMode) -> Unit = {},\n    onCell: (Int) -> Unit,\n) {",
)
replace_once(
    board,
    "    var closeScale by remember { mutableFloatStateOf(WORD_SIEGE_PRACTICE_CLOSE_SCALE) }\n",
    "    var closeScale by remember { mutableFloatStateOf(WORD_SIEGE_PRACTICE_DOUBLE_TAP_SCALE) }\n",
)
replace_once(
    board,
    "    fun toggleMode() {\n        val nextMode = mode.toggle()\n        if (nextMode == WordSiegeBoardViewportMode.CLOSE) closePan = centerClose()\n        mode = nextMode\n    }",
    "    fun toggleMode() {\n        val nextMode = mode.toggle()\n        if (nextMode == WordSiegeBoardViewportMode.CLOSE) {\n            closeScale = WORD_SIEGE_PRACTICE_DOUBLE_TAP_SCALE\n            closePan = centerClose()\n        }\n        mode = nextMode\n        onViewportModeChange(nextMode)\n    }",
)
replace_once(
    board,
    ".background(Brush.linearGradient(listOf(Color(0xFF3B2518), Color(0xFF6A452B), Color(0xFF4A2F1E), Color(0xFF785238))))",
    ".background(Brush.linearGradient(listOf(Color(0xFFF0E6D7), Color(0xFFE5D2B7), Color(0xFFF7F1E8), Color(0xFFD9BE98))))",
)
replace_once(
    board,
    "    val regionGap = 1.25.dp\n\n    Box(\n",
    "    val regionGap = 1.25.dp\n    val cellInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }\n\n    Box(\n",
)
old_clickable = '''        Modifier
            .size(PracticeSiegeCellSize)
            .padding(regionGap)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        androidx.compose.ui.graphics.lerp(displayCellColor, Color.White, .18f),
                        displayCellColor,
                        androidx.compose.ui.graphics.lerp(displayCellColor, Color.Black, .07f),
                    )
                )
            )
            .border(
                width = when {
                    lastMoveHighlight > 0f -> 1.7.dp
                    threatened && owner != 0 -> 1.5.dp
                    else -> .55.dp
                },
                color = if (lastMoveHighlight > 0f) {
                    PracticeLastMove.copy(alpha = 0.45f + .45f * lastMoveHighlight)
                } else borderColor,
                shape = RoundedCornerShape(8.dp),
            )
            .combinedClickable(
                onClick = {
                    dispatchWordSiegeBoardTap(
                        WordSiegeBoardTapAction.PLACE,
                        canPlace,
                        onClick,
                        onDoubleClick,
                    )
                },
                onDoubleClick = {
                    dispatchWordSiegeBoardTap(
                        WordSiegeBoardTapAction.TOGGLE_VIEWPORT,
                        canPlace,
                        onClick,
                        onDoubleClick,
                    )
                },
            ),'''
new_clickable = '''        Modifier
            .size(PracticeSiegeCellSize)
            .combinedClickable(
                interactionSource = cellInteraction,
                indication = null,
                onClick = {
                    dispatchWordSiegeBoardTap(
                        WordSiegeBoardTapAction.PLACE,
                        canPlace,
                        onClick,
                        onDoubleClick,
                    )
                },
                onDoubleClick = {
                    dispatchWordSiegeBoardTap(
                        WordSiegeBoardTapAction.TOGGLE_VIEWPORT,
                        canPlace,
                        onClick,
                        onDoubleClick,
                    )
                },
            )
            .padding(regionGap)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        androidx.compose.ui.graphics.lerp(displayCellColor, Color.White, .18f),
                        displayCellColor,
                        androidx.compose.ui.graphics.lerp(displayCellColor, Color.Black, .07f),
                    )
                )
            )
            .border(
                width = when {
                    lastMoveHighlight > 0f -> 1.7.dp
                    threatened && owner != 0 -> 1.5.dp
                    else -> .55.dp
                },
                color = if (lastMoveHighlight > 0f) {
                    PracticeLastMove.copy(alpha = 0.45f + .45f * lastMoveHighlight)
                } else borderColor,
                shape = RoundedCornerShape(8.dp),
            ),'''
replace_once(board, old_clickable, new_clickable)

# Hoist only viewport visibility state to the practice screen; gameplay stays untouched.
replace_once(
    screen,
    "    var shuffleSeed by remember { mutableIntStateOf(0) }\n",
    "    var shuffleSeed by remember { mutableIntStateOf(0) }\n    var boardViewportMode by remember { mutableStateOf(WordSiegeBoardViewportMode.FIT) }\n",
)
replace_once(
    screen,
    "        shuffleSeed = 0\n        actionVfxEvent = 0\n",
    "        shuffleSeed = 0\n        boardViewportMode = WordSiegeBoardViewportMode.FIT\n        actionVfxEvent = 0\n",
)
header_start = '''                Row(
                    modifier = Modifier.fillMaxWidth().height(if (compact) 46.dp else 52.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {'''
replace_once(screen, header_start, '''                if (boardViewportMode == WordSiegeBoardViewportMode.FIT) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(if (compact) 46.dp else 52.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {''')
header_end = '''                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {'''
replace_once(screen, header_end, '''                    }
                }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {''')
replace_once(
    screen,
    "                WordSiegeOwnershipLegend()\n",
    "                if (boardViewportMode == WordSiegeBoardViewportMode.FIT) WordSiegeOwnershipLegend()\n",
)
replace_once(
    screen,
    '''                        resolvedIndices = lastMove?.placements?.keys ?: emptySet(),
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        onCell = { boardIndex ->''',
    '''                        resolvedIndices = lastMove?.placements?.keys ?: emptySet(),
                        modifier = if (boardViewportMode == WordSiegeBoardViewportMode.CLOSE) Modifier.fillMaxSize() else Modifier.fillMaxWidth().aspectRatio(1f),
                        onViewportModeChange = { boardViewportMode = it },
                        onCell = { boardIndex ->''',
)
replace_once(
    screen,
    '''                if (state.status == "playing") {
                    Row(
                        Modifier.fillMaxWidth().height(16.dp),''',
    '''                if (state.status == "playing") {
                    if (boardViewportMode == WordSiegeBoardViewportMode.FIT) Row(
                        Modifier.fillMaxWidth().height(16.dp),''',
)
replace_once(
    screen,
    "                WordSiegePracticeStatusBar(statusMessage, compact)\n",
    "                if (boardViewportMode == WordSiegeBoardViewportMode.FIT) WordSiegePracticeStatusBar(statusMessage, compact)\n",
)

# Keep identity visible and make avatars clearly larger in both normal and close views.
replace_once(ui, "                            size = 38.dp, accent = accent, visible = avatarVisible,\n", "                            size = 42.dp, accent = accent, visible = avatarVisible,\n")

print("Practice board Kelimelik-style viewport correction applied")
