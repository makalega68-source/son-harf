from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def read(path):
    return (ROOT / path).read_text(encoding="utf-8")

def write(path, value):
    (ROOT / path).write_text(value, encoding="utf-8")

def replace_once(path, old, new):
    s = read(path)
    count = s.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected one match, got {count}: {old[:100]!r}")
    write(path, s.replace(old, new, 1))

def replace_all(path, old, new, minimum=1):
    s = read(path)
    count = s.count(old)
    if count < minimum:
        raise RuntimeError(f"{path}: expected >= {minimum}, got {count}: {old[:100]!r}")
    write(path, s.replace(old, new))

# Keep paid keyboard cosmetics functional inside the new dark duel deck.
p = "app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt"
replace_once(p, "        color = Color(0xFF050C15),\n        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),", "        color = palette.background,\n        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),")

# 4-move Letter Path tests: one original letter remains correct; four discoveries advance the four lights.
p = "app/src/test/java/com/sonharf/game/LetterLadderEngineTest.kt"
s = read(p)
s = s.replace('private val chain = listOf("kalın", "yalın", "yalan", "yalak", "yamak", "yumak")', 'private val chain = listOf("kalın", "yalın", "yalan", "yalak", "yamak")')
s = s.replace('fun knownFiveMoveChainChangesEveryPositionExactlyOnce()', 'fun knownFourMoveChainChangesFourPositionsExactlyOnce()')
s = s.replace('assertEquals(setOf(0, 1, 2, 3, 4), used)', 'assertEquals(setOf(0, 2, 3, 4), used)')
s = s.replace('assertEquals("yumak", chain.last())', 'assertEquals("yamak", chain.last())')
s = s.replace('fun validSequenceReachesTargetInExactlyFiveMoves()', 'fun validSequenceReachesTargetInExactlyFourMoves()')
s = s.replace('assertEquals(5, used.size)', 'assertEquals(4, used.size)')
s = s.replace('fun generatorReturnsALegalFiveMovePuzzleFromCanonicalCandidates()', 'fun generatorReturnsALegalFourMovePuzzleFromCanonicalCandidates()')
s = s.replace('assertEquals(6, generated.solution.size)', 'assertEquals(5, generated.solution.size)')
s = s.replace('assertTrue((0 until 5).all { generated.start[it] != generated.target[it] })', 'assertEquals(1, (0 until 5).count { generated.start[it] == generated.target[it] })')
s = s.replace('val firstRoute = listOf("abcde", "fbcde", "fgcde", "fghde", "fghie", "fghij")', 'val firstRoute = listOf("abcde", "fbcde", "fgcde", "fghde", "fghie")')
s = s.replace('val secondRoute = listOf("klmno", "plmno", "pqmno", "pqrno", "pqrso", "pqrst")', 'val secondRoute = listOf("klmno", "plmno", "pqmno", "pqrno", "pqrso")')
write(p, s)

# Son Harf regression: preserve runtime/recovery contracts while locking the new high-adrenaline visuals.
p = "app/src/test/java/com/sonharf/game/PremierDuelUxRegressionTest.kt"
replace_once(p, 'assertTrue(screen.contains("if (required.length > 1) .32f else .42f"))', 'assertTrue(screen.contains("if (required.length > 1) .28f else .41f"))\n        assertTrue(screen.contains("PremierPressureStrip("))\n        assertTrue(screen.contains("KRİTİK 5 SANİYE"))\n        assertTrue(screen.contains("HAMLE SENDE • SALDIR"))')

# Theme contract: meta shell stays light/approved; Son Harf is intentionally a dark competitive arena.
p = "app/src/test/java/com/sonharf/game/UnifiedThemeSourceContractTest.kt"
old = '''        // Premier remains a deliberately fixed high-legibility gameplay surface. The application
        // shell and meta screens use Monster; competitive text entry retains its proven palette.
        assertTrue(premier.contains("val Background = Color(0xFFEAF6F8)"))
        assertTrue(premier.contains("val Surface = Color(0xFFFFFFFF)"))
        assertTrue(premier.contains("val Ocean = Color(0xFF14B8B0)"))
        assertTrue(premier.contains("val Sky = Color(0xFF8B6CF0)"))
        assertTrue(premier.contains("Brush.verticalGradient(listOf(PremierUi.Surface, PremierUi.Background))"))
        assertFalse(premier.contains("val Ocean = Color(0xFF2563EB)"))'''
new = '''        // The approved meta shell stays light; Son Harf intentionally switches to a high-contrast
        // competitive night arena while retaining readable cyan, red and gold action states.
        assertTrue(premier.contains("val Background = Color(0xFF06101D)"))
        assertTrue(premier.contains("val Surface = Color(0xFF0D1B2A)"))
        assertTrue(premier.contains("val Ocean = Color(0xFF00D6C9)"))
        assertTrue(premier.contains("val Sky = Color(0xFF8B5CF6)"))
        assertTrue(premier.contains("Brush.verticalGradient("))
        assertTrue(premier.contains("Color(0xFF050B14)"))'''
replace_once(p, old, new)

# Kuşatma premium board contracts: preserve ownership colors and input behavior; lock wood/brass presentation.
p = "app/src/test/java/com/sonharf/game/WordSiegeEntryModesContractTest.kt"
old = '''        assertTrue(board.contains("PanSiegeFrameNavy"))
        assertTrue(board.contains("shadowElevation = 8.dp"))
        assertTrue(board.contains("border = BorderStroke(2.dp, PanSiegeFrameEdge)"))'''
new = '''        assertTrue(board.contains("color = Color(0xFF2F1D13)"))
        assertTrue(board.contains("shadowElevation = 14.dp"))
        assertTrue(board.contains("border = BorderStroke(2.dp, Color(0xFFC6A56B))"))
        assertTrue(board.contains("Color(0xFFF2DFC0)"))'''
replace_once(p, old, new)

p = "app/src/test/java/com/sonharf/game/WordSiegePanAreaContractTest.kt"
replace_once(p, 'assertTrue(pan.contains("fontSize = 21.sp"))', 'assertTrue(pan.contains("fontSize = 22.sp"))')
replace_once(p, 'assertTrue(pan.contains("Color(0xFF17372C)"))', 'assertTrue(pan.contains("Color(0xFF2A1B13)"))\n        assertTrue(pan.contains("FontFamily.Serif"))\n        assertTrue(pan.contains("Color(0xFFF2DFC0)"))')

print("Gameplay contract alignment applied")
