from pathlib import Path
import re

path = Path("app/src/main/java/com/sonharf/game/LetterLadderGame.kt")
text = path.read_text()

# Harf Yolu is intentionally isolated from the global theme/cosmetics palette.
palette_pattern = re.compile(
    r"private object LetterLadderUi \{\n.*?\n\}\n\ninternal data class LetterLadderPuzzle",
    re.S,
)
palette_replacement = '''private object LetterLadderUi {
    val Background = Color(0xFFF5FCFF)
    val Surface = Color.White
    val SurfaceRaised = Color(0xFFFCFEFF)
    val SurfaceSoft = Color(0xFFEAF8FC)
    val Text = Color(0xFF123A4A)
    val Muted = Color(0xFF5D7C88)
    val Border = Color(0xFFA9DCE7)
    val Accent = Color(0xFF278DC3)
    val AccentStrong = Color(0xFF1579B4)
    val Turquoise = Color(0xFF22BFC4)
    val TurquoiseStrong = Color(0xFF10AEB5)
    val TurquoiseSoft = Color(0xFFDDF9FA)
    val AccentText = Color.White
    val Live = Turquoise
    val Coral = Accent
    val Orange = Accent
    val Green = Turquoise
    val Gold = Turquoise
}

internal data class LetterLadderPuzzle'''
text, count = palette_pattern.subn(palette_replacement, text, count=1)
assert count == 1, "LetterLadderUi palette block not found"

# Reset is removed from both behavior and UI.
reset_pattern = re.compile(r"\n    fun resetCurrent\(\) \{.*?\n    \}\n\n    fun submit\(\)", re.S)
text, count = reset_pattern.subn("\n    fun submit()", text, count=1)
assert count == 1, "resetCurrent block not found"

text = text.replace("FirstRunLanguageBackdrop(Modifier.fillMaxSize())", "HarfYoluBackdrop(Modifier.fillMaxSize())")
text = text.replace(
    "İpucu kullanıldı: sarı işaretli harfi değiştir. Harfi kendin bul.",
    "İpucu kullanıldı: turkuaz işaretli kutudaki harfi değiştir. Harfi kendin bul.",
)
text = text.replace(
    "Hint used: change the yellow-marked letter. Find the letter yourself.",
    "Hint used: change the turquoise-marked tile. Find the letter yourself.",
)
text = text.replace("import androidx.compose.foundation.background\n", "")
text = text.replace("import androidx.compose.material.icons.rounded.Refresh\n", "")

start_marker = '''                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        enabled = path.size > 1 && !completed,'''
end_marker = '''            }

            if (!completed) {
                EmbeddedWordKeyboard('''
start = text.find(start_marker)
assert start >= 0, "action row start not found"
end = text.find(end_marker, start)
assert end >= 0, "action row end not found"

actions = '''                if (!completed) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            enabled = path.size > 1,
                            onClick = {
                                if (path.size <= 1) return@OutlinedButton
                                val removed = path.last()
                                val previous = path[path.lastIndex - 1]
                                val index = LetterLadderEngine.changedIndex(previous, removed)
                                path = path.dropLast(1)
                                if (index != null) usedPositions = usedPositions - index
                                input = ""
                                hintText = null
                                hintedIndex = null
                                message = sh("Son hamle geri alındı.", "Last move undone.")
                                SonHarfSoundFx.puzzleTap()
                            },
                            modifier = Modifier.weight(1f).height(40.dp).sonHarfPressScale(pressedScale = 0.94f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(13.dp),
                            border = BorderStroke(1.dp, LetterLadderUi.Border),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = LetterLadderUi.AccentStrong,
                                disabledContentColor = LetterLadderUi.Muted.copy(alpha = .34f),
                            ),
                        ) {
                            Text(sh("GERİ AL", "UNDO"), fontWeight = FontWeight.Black, fontSize = 10.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            enabled = !hintUsed,
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
                                        "İpucu kullanıldı: turkuaz işaretli kutudaki harfi değiştir. Harfi kendin bul.",
                                        "Hint used: change the turquoise-marked tile. Find the letter yourself.",
                                    )
                                }
                                SonHarfSoundFx.puzzleHint()
                            },
                            modifier = Modifier.weight(1f).height(40.dp).sonHarfPressScale(pressedScale = 0.94f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(13.dp),
                            border = BorderStroke(1.dp, LetterLadderUi.Turquoise.copy(alpha = .72f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = LetterLadderUi.TurquoiseStrong,
                                disabledContentColor = LetterLadderUi.Muted.copy(alpha = .34f),
                            ),
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
                    }
                } else {
                    Button(
                        onClick = { puzzleNonce++ },
                        modifier = Modifier.fillMaxWidth().height(46.dp).sonHarfPressScale(pressedScale = 0.94f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LetterLadderUi.AccentStrong,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(sh("YENİ OYUN", "NEW GAME"), fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.ChevronRight, null)
                    }
                }
'''
text = text[:start] + actions + text[end:]

# Product contracts for this hotfix.
assert "SIFIRLA" not in text
assert "resetCurrent()" not in text
assert "YENİ OYUN" in text
assert "HarfYoluBackdrop(Modifier.fillMaxSize())" in text
assert "FirstRunLanguageBackdrop(Modifier.fillMaxSize())" not in text
assert "turkuaz işaretli kutudaki harfi değiştir" in text
assert "sarı işaretli" not in text
assert "activeInput = input.uppercase(locale).takeIf { isActiveEntry }" in text
assert "İPUCU 1/1" in text

path.write_text(text)
print("Harf Yolu blue/turquoise dynamic UI patch applied")
