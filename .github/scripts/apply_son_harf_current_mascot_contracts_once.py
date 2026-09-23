from pathlib import Path

premier_path = Path("app/src/test/java/com/sonharf/game/PremierDuelUxRegressionTest.kt")
premier = premier_path.read_text(encoding="utf-8")

replacements = {
    'assertTrue(screen.contains("Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-3).dp).size(10.dp).clip(CircleShape).background(PremierUi.Red)"))':
        'assertTrue(screen.contains("Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp)"))',
    'assertTrue(screen.contains("Alignment.CenterStart"))\n        assertTrue(screen.contains("Alignment.CenterEnd"))':
        'assertTrue(screen.contains("PremierSymmetricPlayerCard("))\n        assertTrue(screen.contains("Modifier.weight(1f).height(cardHeight)"))\n        assertTrue(screen.contains("Modifier.align(Alignment.CenterEnd).size(mascotSize)"))',
    'assertTrue(screen.contains("if (veryCompact) 78.dp"))\n        assertTrue(screen.contains("if (compact) 88.dp"))\n        assertTrue(screen.contains("if (tall) 118.dp"))\n        assertTrue(screen.contains("else 104.dp"))':
        'assertTrue(screen.contains("if (veryCompact) 74.dp"))\n        assertTrue(screen.contains("if (compact) 86.dp"))\n        assertTrue(screen.contains("if (tall) 112.dp"))\n        assertTrue(screen.contains("else 100.dp"))\n        assertTrue(screen.contains("val mascotSize = if (veryCompact) 64.dp"))',
}
for old, new in replacements.items():
    if old not in premier:
        raise RuntimeError(f"Premier contract marker not found: {old[:80]}")
    premier = premier.replace(old, new, 1)

anchor = '        assertFalse(screen.contains("MageCatCompanion("))\n        assertFalse(screen.contains("SyntheticBotPortrait("))'
addition = '''        assertFalse(screen.contains("MageCatCompanion("))
        assertFalse(screen.contains("SyntheticBotPortrait("))
        assertTrue(screen.contains("WordSiegeMascot("))
        assertTrue(screen.contains("WordSiegeMascotEmotion.FOCUS"))
        assertTrue(screen.contains("PremierKeyboard(language, input"))
        assertTrue(screen.contains("PremierArenaSky.BackgroundTop"))
        assertTrue(screen.contains("Color(0xFFEAF8FF)"))'''
if anchor not in premier:
    raise RuntimeError("Premier mascot contract insertion marker not found")
premier = premier.replace(anchor, addition, 1)
premier_path.write_text(premier, encoding="utf-8")

unified_path = Path("app/src/test/java/com/sonharf/game/UnifiedThemeSourceContractTest.kt")
unified = unified_path.read_text(encoding="utf-8")
unified_replacements = {
    'fun activeUnifiedShellUsesMonsterThemeAndPremierKeepsHighLegibilityArenaPalette()':
        'fun activeUnifiedShellKeepsMetaThemeAndPremierUsesSkyMascotArena() ',
    '// The approved meta shell stays light; Son Harf intentionally switches to a high-contrast\n        // competitive night arena while retaining readable cyan, red and gold action states.':
        '// The approved meta shell stays light; Son Harf now uses a scoped sky-blue arena\n        // with the shared in-game mascot while preserving the application theme elsewhere.',
    'assertTrue(premier.contains("val Background = Color(0xFF06101D)"))\n        assertTrue(premier.contains("val Surface = Color(0xFF0D1B2A)"))\n        assertTrue(premier.contains("val Ocean = Color(0xFF00D6C9)"))\n        assertTrue(premier.contains("val Sky = Color(0xFF8B5CF6)"))\n        assertTrue(premier.contains("Brush.verticalGradient("))\n        assertTrue(premier.contains("Color(0xFF050B14)"))':
        'assertTrue(premier.contains("private object PremierArenaSky"))\n        assertTrue(premier.contains("Color(0xFFEAF8FF)"))\n        assertTrue(premier.contains("Color(0xFFDDF3FC)"))\n        assertTrue(premier.contains("Color(0xFFCFEAF7)"))\n        assertTrue(premier.contains("Brush.verticalGradient("))\n        assertTrue(premier.contains("WordSiegeMascot("))\n        assertTrue(premier.contains("PremierKeyboard(language, input"))',
}
for old, new in unified_replacements.items():
    if old not in unified:
        raise RuntimeError(f"Unified contract marker not found: {old[:80]}")
    unified = unified.replace(old, new, 1)
unified_path.write_text(unified, encoding="utf-8")

print("Updated Son Harf sky/symmetry/mascot regression contracts")
