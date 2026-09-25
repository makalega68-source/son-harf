from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def patch(path: str, old: str, new: str) -> None:
    target = ROOT / path
    value = target.read_text(encoding="utf-8")
    count = value.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected one match, got {count}: {old!r}")
    target.write_text(value.replace(old, new, 1), encoding="utf-8")


pan = "app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt"
viewport = "app/src/main/java/com/sonharf/game/WordSiegeBoardViewport.kt"
contract = "app/src/test/java/com/sonharf/game/WordSiegePanAreaContractTest.kt"
viewport_test = "app/src/test/java/com/sonharf/game/WordSiegeBoardViewportTest.kt"

# Preserve the proven physical spacing; the lighter board bed fixes the dark seam complaint.
patch(pan, "    val regionGap = 0.9.dp\n", "    val regionGap = 1.25.dp\n")

# A 2x close scale gives 7.5 visible columns on a 390dp phone, close to the supplied reference.
patch(viewport, "internal const val WORD_SIEGE_ONLINE_ZOOM_FACTOR = 2.20f\n", "internal const val WORD_SIEGE_ONLINE_ZOOM_FACTOR = 2.00f\n")
patch(contract, 'assertTrue(viewport.contains("WORD_SIEGE_ONLINE_ZOOM_FACTOR = 2.20f"))', 'assertTrue(viewport.contains("WORD_SIEGE_ONLINE_ZOOM_FACTOR = 2.00f"))')
patch(viewport_test, "fun `online close scale shows about seven cells across on a 390dp phone`()", "fun `online close scale shows about seven and a half cells across on a 390dp phone`()")
patch(viewport_test, "        assertEquals(1.10f, scale, tolerance)\n", "        assertEquals(1.00f, scale, tolerance)\n")
patch(viewport_test, "        assertTrue(scale > wordSiegeFitScale(390f, 500f, boardPx) * 2f)\n", "        assertEquals(wordSiegeFitScale(390f, 500f, boardPx) * 2f, scale, tolerance)\n")

print("Kelime Kusatmasi viewport contract alignment applied")
