#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def replace(path: str, old: str, new: str, label: str):
    p = ROOT / path
    text = p.read_text(encoding="utf-8")
    if new in text:
        print(f"already applied: {label}")
        return
    if old not in text:
        raise RuntimeError(f"{label}: source pattern not found")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")
    print(f"patched: {label}")

replace(
    "app/src/main/java/com/sonharf/game/WordSiegeBoardViewport.kt",
    "    closeScale: Float = WORD_SIEGE_DEFAULT_CLOSE_SCALE,",
    "    closeScale: Float = 1f,",
    "generic transform keeps historical default",
)
replace(
    "app/src/main/java/com/sonharf/game/WordSiegeBoardViewport.kt",
    "    scale: Float = WORD_SIEGE_DEFAULT_CLOSE_SCALE,",
    "    scale: Float = 1f,",
    "generic center helper keeps historical default",
)
replace(
    "app/src/main/java/com/sonharf/game/WordSiegePanMatch.kt",
    "                border.copy(alpha = (.96f + .04f * lastMoveHighlightAlpha).coerceAtMost(1f)),",
    "                border.copy(alpha = .96f),",
    "ownership border palette stays unchanged during highlight",
)
