from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
SCREEN = ROOT / "app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt"
BOARD = ROOT / "app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f"Missing expected pattern: {label}")
    return text.replace(old, new, 1)


def patch_screen() -> None:
    text = SCREEN.read_text()

    text = replace_once(
        text,
        "        modifier = modifier,\n        color = if (active) accent.copy(alpha = .09f) else MainUi.Surface,",
        "        modifier = modifier.height(if (compact) 70.dp else 78.dp),\n        color = if (active) accent.copy(alpha = .09f) else MainUi.Surface,",
        "fixed score-card height",
    )

    text = replace_once(text, "                size = 50.dp,", "                size = 46.dp,", "avatar size")
    text = replace_once(
        text,
        "                    Text(name, color = MainUi.Text, fontWeight = FontWeight.Black, fontSize = 15.sp, lineHeight = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))",
        "                    Text(name, color = MainUi.Text, fontWeight = FontWeight.Black, fontSize = 14.sp, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))",
        "profile name typography",
    )
    text = replace_once(
        text,
        "                    Text(\"$score\", color = accent, fontWeight = FontWeight.Black, fontSize = 30.sp, lineHeight = 32.sp, maxLines = 1)",
        "                    Text(\"$score\", color = accent, fontWeight = FontWeight.Black, fontSize = 24.sp, lineHeight = 27.sp, maxLines = 1)",
        "score typography",
    )
    text = replace_once(
        text,
        "                    Text(sh(\"Alan $area\", \"Area $area\"), color = MainUi.Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)",
        "                    Text(sh(\"Alan $area\", \"Area $area\"), color = MainUi.Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)",
        "area typography",
    )

    old_notice = '''                notice?.let { message ->
                    WordSiegeNotice(message)
                } ?: lastMove?.let { move ->
                    Text(
                        sh("Son: ${move.formedWords.joinToString(" + ")} • +${move.wordScore}", "Last: ${move.formedWords.joinToString(" + ")} • +${move.wordScore}"),
                        color = MainUi.Muted,
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                    )
                }
'''
    new_notice = '''                Box(
                    modifier = Modifier.fillMaxWidth().height(if (compact) 34.dp else 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    notice?.let { message ->
                        WordSiegeNotice(message)
                    } ?: lastMove?.let { move ->
                        Text(
                            sh("Son: ${move.formedWords.joinToString(" + ")} • +${move.wordScore}", "Last: ${move.formedWords.joinToString(" + ")} • +${move.wordScore}"),
                            color = MainUi.Muted,
                            fontSize = 8.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                        )
                    }
                }
'''
    text = replace_once(text, old_notice, new_notice, "reserved notice slot")

    SCREEN.write_text(text)


def patch_board() -> None:
    text = BOARD.read_text()

    text = replace_once(
        text,
        "import androidx.compose.foundation.shape.CircleShape\n",
        "",
        "remove circle import",
    )
    text = replace_once(
        text,
        "import androidx.compose.material.icons.rounded.CenterFocusStrong\n",
        "",
        "remove focus icon import",
    )
    text = replace_once(
        text,
        "import androidx.compose.material3.Icon\n",
        "",
        "remove icon import",
    )
    text = replace_once(
        text,
        "import androidx.compose.material3.SmallFloatingActionButton\n",
        "",
        "remove FAB import",
    )

    text = replace_once(
        text,
        "internal val PracticeSiegeNeutral = Color(0xFFF8FAF9)\n",
        "internal val PracticeSiegeNeutral = Color(0xFFF8FAF9)\nprivate val PracticeSiegeEmpty = Color(0xFFFFF7E6)\n",
        "warm empty cube color",
    )

    text = replace_once(
        text,
        "    var mode by remember { mutableStateOf(WordSiegeBoardViewportMode.CLOSE) }",
        "    var mode by remember { mutableStateOf(WordSiegeBoardViewportMode.FIT) }",
        "stable fit mode on open",
    )

    vfx_pattern = re.compile(
        r'''    val actionVfxEvents = remember\(placements, moveEventKey, resolvedIndices\) \{\n        buildList \{.*?\n        \}\n    \}\n''',
        re.S,
    )
    text, count = vfx_pattern.subn(
        "    val actionVfxEvents = emptyList<PurchasedBoardVfxEvent>()\n",
        text,
        count=1,
    )
    if count != 1:
        raise SystemExit("Missing expected pattern: board action VFX events")

    fab_pattern = re.compile(
        r'''\n            SmallFloatingActionButton\(\n                onClick = \{.*?\n            \}\n''',
        re.S,
    )
    text, count = fab_pattern.subn("\n", text, count=1)
    if count != 1:
        raise SystemExit("Missing expected pattern: board top-right FAB")

    text = replace_once(
        text,
        "        else -> PracticeSiegeNeutral\n    }",
        "        else -> PracticeSiegeEmpty\n    }",
        "empty cell background",
    )

    BOARD.write_text(text)


if __name__ == "__main__":
    patch_screen()
    patch_board()
    print("Word Siege UI stabilization patch applied successfully.")
