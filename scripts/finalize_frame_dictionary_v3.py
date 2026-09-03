#!/usr/bin/env python3
from __future__ import annotations
import base64, hashlib, struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
STAGE = ROOT / "tools" / "frame_assets_v3"
DRAW = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
UI = ROOT / "app" / "src" / "main" / "java" / "com" / "sonharf" / "game" / "PurchasedStyleUi.kt"

ASSETS = {
    "red": ("style_frame_red.png", ["red.00.part", "red.01.part", "red.02.part"], "fa2c79145b5857707a2e099689d3e727978787bca346a163687d57b63548aa2c"),
    "gold_crown": ("style_frame_gold_crown.png", [f"gold_crown.{i:02d}.part" for i in range(6)], "1e955fe915d8b1b35045a21c1c4221521dfdfe4355c9321c591bd172b802ff05"),
    "christmas": ("style_frame_christmas.png", ["christmas.00.part", "christmas.01.part", "christmas.02a.part", "christmas.02b.part", "christmas.03.part", "christmas.04.part", "christmas.05.part"], "48fa2d6afd6f95e25c0ac7ac8330a7e8d97a653fdb98f9c17e1a0b226e24082d"),
    "halloween": ("style_frame_halloween.png", ["halloween.00.part", "halloween.01.part", "halloween.02.part", "halloween.03.part", "halloween.04a.part", "halloween.04b.part"], "96598d5769d25f7520291e83248b1e9fa8f2984aa2822b41df3525bc63fbf62d"),
}

def verify_png(data: bytes) -> None:
    assert data[:8] == b"\x89PNG\r\n\x1a\n", "bad PNG signature"
    pos, saw_ihdr, saw_iend = 8, False, False
    while pos + 12 <= len(data):
        length = struct.unpack(">I", data[pos:pos+4])[0]
        kind = data[pos+4:pos+8]
        end = pos + 12 + length
        assert end <= len(data), "truncated PNG chunk"
        if kind == b"IHDR": saw_ihdr = True
        if kind == b"IEND": saw_iend = True; break
        pos = end
    assert saw_ihdr and saw_iend, "incomplete PNG"

for _, (target, parts, expected) in ASSETS.items():
    encoded = "".join((STAGE / p).read_text(encoding="utf-8").strip() for p in parts)
    raw = base64.b64decode(encoded, validate=True)
    verify_png(raw)
    actual = hashlib.sha256(raw).hexdigest()
    assert actual == expected, f"{target}: sha mismatch {actual} != {expected}"
    (DRAW / target).write_bytes(raw)
    print(f"materialized {target} sha256={actual}")

text = UI.read_text(encoding="utf-8")
repls = [
("    const val GOLD_CROWN = \"frame_asset_gold_crown\"\n\n    val ids = setOf(GOLD, MINT, PURPLE, GREEN, RED, GOLD_CROWN)",
 "    const val GOLD_CROWN = \"frame_asset_gold_crown\"\n    const val CHRISTMAS = \"frame_asset_christmas\"\n    const val HALLOWEEN = \"frame_asset_halloween\"\n\n    val ids = setOf(GOLD, MINT, PURPLE, GREEN, RED, GOLD_CROWN, CHRISTMAS, HALLOWEEN)"),
("        GOLD_CROWN -> R.drawable.style_frame_gold_crown\n        else -> null",
 "        GOLD_CROWN -> R.drawable.style_frame_gold_crown\n        CHRISTMAS -> R.drawable.style_frame_christmas\n        HALLOWEEN -> R.drawable.style_frame_halloween\n        else -> null"),
("        GOLD_CROWN -> Color(0xFFE0A51C)\n        \"frame_neon\"",
 "        GOLD_CROWN -> Color(0xFFE0A51C)\n        CHRISTMAS -> Color(0xFFC73D3D)\n        HALLOWEEN -> Color(0xFFEF7D22)\n        \"frame_neon\""),
("    PurchasedFrameSpec(PurchasedFrameCatalog.GOLD_CROWN, \"Altın Taç\", \"Gold Crown\", \"Yüksek lig ve prestij ödülü\", \"High-league prestige reward\", R.drawable.style_frame_gold_crown, Color(0xFFE0A51C), \"LİG ÖDÜLÜ\", \"LEAGUE REWARD\", R.drawable.style_icon_trophy),\n)",
 "    PurchasedFrameSpec(PurchasedFrameCatalog.GOLD_CROWN, \"Altın Taç\", \"Gold Crown\", \"Yüksek lig ve prestij ödülü\", \"High-league prestige reward\", R.drawable.style_frame_gold_crown, Color(0xFFE0A51C), \"LİG ÖDÜLÜ\", \"LEAGUE REWARD\", R.drawable.style_icon_trophy),\n    PurchasedFrameSpec(PurchasedFrameCatalog.CHRISTMAS, \"Yılbaşı\", \"Christmas\", \"Sezonluk yılbaşı etkinlik çerçevesi\", \"Seasonal Christmas event frame\", R.drawable.style_frame_christmas, Color(0xFFC73D3D), \"ETKİNLİK\", \"EVENT\", R.drawable.style_icon_trophy),\n    PurchasedFrameSpec(PurchasedFrameCatalog.HALLOWEEN, \"Halloween\", \"Halloween\", \"Sezonluk Halloween etkinlik çerçevesi\", \"Seasonal Halloween event frame\", R.drawable.style_frame_halloween, Color(0xFFEF7D22), \"ETKİNLİK\", \"EVENT\", R.drawable.style_icon_trophy),\n)"),
("            contentScale = ContentScale.FillBounds,", "            contentScale = ContentScale.Fit,"),
("                                        contentScale = ContentScale.FillBounds,", "                                        contentScale = ContentScale.Fit,"),
]
for old, new in repls:
    if old in text:
        text = text.replace(old, new)
    elif new not in text:
        raise SystemExit(f"Expected UI anchor missing: {old[:80]!r}")
UI.write_text(text, encoding="utf-8")
print("PurchasedStyleUi finalized")
