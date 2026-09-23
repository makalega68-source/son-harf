from pathlib import Path
import subprocess

BASE_SCREEN = Path("app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt")
REFERENCE_COMMIT = "a2400101f2e2263ed186b658e5ba66ee3d705fdf"
SCREEN_PATH = "app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt"


def git_show(path: str) -> str:
    return subprocess.check_output(
        ["git", "show", f"{REFERENCE_COMMIT}:{path}"],
        text=True,
        encoding="utf-8",
    )


def slice_between(source: str, start: str, end: str) -> str:
    i = source.index(start)
    j = source.index(end, i)
    return source[i:j]


def replace_between(source: str, start: str, end: str, replacement: str) -> str:
    i = source.index(start)
    j = source.index(end, i)
    return source[:i] + replacement.rstrip() + "\n\n" + source[j:]


screen = BASE_SCREEN.read_text(encoding="utf-8")
reference = git_show(SCREEN_PATH)

# Native Android IME support. Keep gameplay/business logic from v35 untouched.
focus_import = "import androidx.compose.ui.focus.focusRequester\n"
if focus_import not in screen:
    screen = screen.replace(
        "import androidx.compose.ui.draw.shadow\n",
        "import androidx.compose.ui.draw.shadow\n" + focus_import,
    )

# Calm Son Harf palette, scoped to this screen only.
palette = r'''private object PremierUi {
    val Background = Color(0xFFF3EEE5)
    val Surface = Color(0xFFFFFBF4)
    val Ink = Color(0xFF173247)
    val Muted = Color(0xFF6F7B7C)
    val Ocean = Color(0xFF4F8F96)
    val OceanDeep = Color(0xFF2F6970)
    val Sky = Color(0xFF8EB7B5)
    val Ice = Color(0xFFE6EFEB)
    val Border = Color(0xFFD8D0C4)
    val Green = Color(0xFF789B73)
    val GreenSoft = Color(0xFFE5ECDD)
    val Red = Color(0xFFC86459)
    val RedSoft = Color(0xFFF4DDD7)
    val Gold = Color(0xFFD1A13E)
    val GoldSoft = Color(0xFFF5E8BB)
    val Rival = Color(0xFFD27869)
    val RivalSoft = Color(0xFFF6E2DC)
}'''
screen = replace_between(screen, "private object PremierUi {", "private fun pt", palette)

# Transfer only the already compiled/validated calm arena UI blocks from the reference commit.
for start, end in [
    ("@Composable\nprivate fun PremierArena(", "@Composable\nprivate fun PremierArenaHeader("),
    ("@Composable\nprivate fun PremierArenaHeader(", "@Composable\nprivate fun PremierMiniPlayer("),
    ("@Composable\nprivate fun PremierTurnBadge(", "@Composable\nprivate fun PremierReconnectBanner("),
    ("@Composable\nprivate fun PremierTargetCard(", "@Composable\nprivate fun PremierInputBar("),
    ("@Composable\nprivate fun PremierInputBar(", "@OptIn(ExperimentalMaterial3Api::class)"),
]:
    block = slice_between(reference, start, end)
    screen = replace_between(screen, start, end, block)

# Required invariants: v35 gameplay stays server-authoritative; only arena presentation changes.
required_tokens = [
    "backend.submitPremierWord(active.id, candidate)",
    "backend.claimTurnTimeout(active.id)",
    "backend.botTakeTurn(active.id)",
    "backend.resumePremierBotMatch(found.id)",
    "fetchPremierTurnClock(active.id)",
    "room.roundWordCount.coerceIn(0, 10)",
    "PremierProWordPanel(",
    "PremierLastWordBar(language = language, word = lastWord, meId = meId)",
    "LocalSoftwareKeyboardController.current",
    "ImeAction.Done",
]
for token in required_tokens:
    if token not in screen:
        raise RuntimeError(f"Required Son Harf invariant missing after patch: {token}")

arena = slice_between(screen, "@Composable\nprivate fun PremierArena(", "@Composable\nprivate fun PremierArenaHeader(")
if "PremierKeyboard(" in arena:
    raise RuntimeError("Custom arena keyboard call survived the native-IME redesign")

BASE_SCREEN.write_text(screen, encoding="utf-8")

# Update only the regression contracts that intentionally changed with this approved UI.
premier_test_path = Path("app/src/test/java/com/sonharf/game/PremierDuelUxRegressionTest.kt")
premier_test_path.write_text(
    git_show("app/src/test/java/com/sonharf/game/PremierDuelUxRegressionTest.kt"),
    encoding="utf-8",
)

cosmetic_path = Path("app/src/test/java/com/sonharf/game/PremiumCosmeticApplicationContractTest.kt")
cosmetic = cosmetic_path.read_text(encoding="utf-8")
cosmetic_ref = git_show("app/src/test/java/com/sonharf/game/PremiumCosmeticApplicationContractTest.kt")
start = "    @Test\n    fun premierMatch"
end = "    @Test\n    fun everySellableKeyboard"
cosmetic = replace_between(cosmetic, start, end, slice_between(cosmetic_ref, start, end))
cosmetic_path.write_text(cosmetic, encoding="utf-8")

premium_path = Path("app/src/test/java/com/sonharf/game/PremiumStoreProContractTest.kt")
premium = premium_path.read_text(encoding="utf-8")
premium_ref = git_show("app/src/test/java/com/sonharf/game/PremiumStoreProContractTest.kt")
start = "    @Test\n    fun `son harf history remains backend gated while latest word stays playable`()"
end = "    @Test\n    fun `series mode is separated from classic matchmaking and enforces missed turn defeat`()"
premium = replace_between(premium, start, end, slice_between(premium_ref, start, end))
premium_path.write_text(premium, encoding="utf-8")

unified_path = Path("app/src/test/java/com/sonharf/game/UnifiedThemeSourceContractTest.kt")
unified = unified_path.read_text(encoding="utf-8")
replacements = {
    "fun activeUnifiedShellUsesMonsterThemeAndPremierKeepsHighLegibilityArenaPalette()":
        "fun activeUnifiedShellUsesMonsterThemeAndPremierKeepsCalmArenaPalette()",
    "// The approved meta shell stays light; Son Harf intentionally switches to a high-contrast\n        // competitive night arena while retaining readable cyan, red and gold action states.":
        "// The approved meta shell stays light; Son Harf uses its scoped calm arena palette\n        // while keeping readable state colors and the existing gameplay shell.",
    "assertTrue(premier.contains(\"val Background = Color(0xFF06101D)\"))":
        "assertTrue(premier.contains(\"val Background = Color(0xFFF3EEE5)\"))",
    "assertTrue(premier.contains(\"val Surface = Color(0xFF0D1B2A)\"))":
        "assertTrue(premier.contains(\"val Surface = Color(0xFFFFFBF4)\"))",
    "assertTrue(premier.contains(\"val Ocean = Color(0xFF00D6C9)\"))":
        "assertTrue(premier.contains(\"val Ocean = Color(0xFF4F8F96)\"))",
    "assertTrue(premier.contains(\"val Sky = Color(0xFF8B5CF6)\"))":
        "assertTrue(premier.contains(\"val Sky = Color(0xFF8EB7B5)\"))",
    "assertTrue(premier.contains(\"Color(0xFF050B14)\"))":
        "assertTrue(premier.contains(\"Brush.verticalGradient(listOf(PremierUi.Surface, PremierUi.Background))\"))",
}
for old, new in replacements.items():
    if old not in unified:
        raise RuntimeError(f"Unified theme contract marker not found: {old}")
    unified = unified.replace(old, new)
unified_path.write_text(unified, encoding="utf-8")

print("Applied Son Harf calm arena on top of canonical v35 only")
