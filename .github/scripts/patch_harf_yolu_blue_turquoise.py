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
    val Orange = Color(0xFFFF9F43)
    val OrangeSoft = Color(0xFFFFF0DE)
    val Purple = Color(0xFF8B5CF6)
    val PurpleSoft = Color(0xFFF2ECFF)
    val AccentText = Color.White
    val Live = Purple
    val Coral = Purple
    val Green = Turquoise
    val Gold = Orange
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
    "İpucu kullanıldı: turuncu işaretli kutudaki harfi değiştir. Harfi kendin bul.",
)
text = text.replace(
    "Hint used: change the yellow-marked letter. Find the letter yourself.",
    "Hint used: change the orange-marked tile. Find the letter yourself.",
)
text = text.replace("import androidx.compose.foundation.background\n", "")
text = text.replace("import androidx.compose.material.icons.rounded.Refresh\n", "")

# Start and target labels use different palette roles.
text = text.replace(
    'Text(sh("BAŞLANGIÇ", "START"), color = LetterLadderUi.Muted, fontSize = 8.sp, fontWeight = FontWeight.Black)',
    'Text(sh("BAŞLANGIÇ", "START"), color = LetterLadderUi.AccentStrong, fontSize = 8.sp, fontWeight = FontWeight.Black)',
)
text = text.replace(
    'Text(sh("HEDEF", "TARGET"), color = LetterLadderUi.Muted, fontSize = 8.sp, fontWeight = FontWeight.Black)',
    'Text(sh("HEDEF", "TARGET"), color = LetterLadderUi.Purple, fontSize = 8.sp, fontWeight = FontWeight.Black)',
)
# The target row is eflatun; committed progress remains turquoise via Green alias.
text = text.replace(
    'accent = LetterLadderUi.Green,\n                            modifier = Modifier.fillMaxWidth().weight(1f),',
    'accent = LetterLadderUi.Purple,\n                            modifier = Modifier.fillMaxWidth().weight(1f),',
    1,
)

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
                            border = BorderStroke(1.dp, LetterLadderUi.Accent.copy(alpha = .72f)),
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
                                        "İpucu kullanıldı: turuncu işaretli kutudaki harfi değiştir. Harfi kendin bul.",
                                        "Hint used: change the orange-marked tile. Find the letter yourself.",
                                    )
                                }
                                SonHarfSoundFx.puzzleHint()
                            },
                            modifier = Modifier.weight(1f).height(40.dp).sonHarfPressScale(pressedScale = 0.94f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(13.dp),
                            border = BorderStroke(1.dp, LetterLadderUi.Orange.copy(alpha = .78f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = LetterLadderUi.Orange,
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
                            containerColor = LetterLadderUi.Purple,
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

# Product contracts for this refresh.
assert "SIFIRLA" not in text
assert "RESET" not in text
assert "resetCurrent()" not in text
assert "YENİ OYUN" in text
assert "NEW GAME" in text
assert "HarfYoluBackdrop(Modifier.fillMaxSize())" in text
assert "FirstRunLanguageBackdrop(Modifier.fillMaxSize())" not in text
assert "turuncu işaretli kutudaki harfi değiştir" in text
assert "sarı işaretli" not in text
assert "activeInput = input.uppercase(locale).takeIf { isActiveEntry }" in text
assert "İPUCU 1/1" in text
assert "val Orange = Color(0xFFFF9F43)" in text
assert "val Purple = Color(0xFF8B5CF6)" in text
assert "accent = LetterLadderUi.Purple" in text

path.write_text(text)
print("Harf Yolu five-color dynamic UI patch applied")
