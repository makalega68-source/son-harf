#!/usr/bin/env python3
from __future__ import annotations

import base64
import binascii
import hashlib
from pathlib import Path
import struct

ROOT = Path(__file__).resolve().parents[1]
PARTS = ROOT / "tools" / "frame_assets_v3"
DRAW = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
UI = ROOT / "app" / "src" / "main" / "java" / "com" / "sonharf" / "game" / "PurchasedStyleUi.kt"
DICT = ROOT / "app" / "src" / "main" / "java" / "com" / "sonharf" / "game" / "data" / "SharedDictionaryService.kt"
DOC = ROOT / "docs" / "DICTIONARY_FRAME_SOURCES_20260903.md"

ASSETS = {
    "red": ("style_frame_red.png", "fa2c79145b5857707a2e099689d3e727978787bca346a163687d57b63548aa2c"),
    "gold_crown": ("style_frame_gold_crown.png", "1e955fe915d8b1b35045a21c1c4221521dfdfe4355c9321c591bd172b802ff05"),
    "christmas": ("style_frame_christmas.png", "48fa2d6afd6f95e25c0ac7ac8330a7e8d97a653fdb98f9c17e1a0b226e24082d"),
    "halloween": ("style_frame_halloween.png", "96598d5769d25f7520291e83248b1e9fa8f2984aa2822b41df3525bc63fbf62d"),
}


def png_check(data: bytes) -> None:
    assert data[:8] == b"\x89PNG\r\n\x1a\n", "bad PNG signature"
    p = 8
    saw_ihdr = saw_iend = False
    while p < len(data):
        if p + 12 > len(data):
            raise ValueError("truncated PNG chunk")
        n = struct.unpack(">I", data[p:p+4])[0]
        kind = data[p+4:p+8]
        payload = data[p+8:p+8+n]
        crc = struct.unpack(">I", data[p+8+n:p+12+n])[0]
        calc = binascii.crc32(kind)
        calc = binascii.crc32(payload, calc) & 0xffffffff
        if crc != calc:
            raise ValueError(f"CRC mismatch in {kind!r}")
        saw_ihdr |= kind == b"IHDR"
        saw_iend |= kind == b"IEND"
        p += 12 + n
    if not saw_ihdr or not saw_iend or p != len(data):
        raise ValueError("incomplete PNG")


def assemble(name: str) -> bytes:
    if name == "red" and (PARTS / "red.b64").exists():
        text = (PARTS / "red.b64").read_text().strip()
    else:
        files = sorted(PARTS.glob(f"{name}.*.part"))
        if not files:
            raise FileNotFoundError(name)
        text = "".join(p.read_text().strip() for p in files)
    return base64.b64decode(text, validate=True)


for key, (filename, expected) in ASSETS.items():
    data = assemble(key)
    got = hashlib.sha256(data).hexdigest()
    if got != expected:
        raise SystemExit(f"{key}: sha256 {got} != {expected}")
    png_check(data)
    (DRAW / filename).write_bytes(data)
    print(f"{key}: {len(data)} bytes sha256={got}")

ui = UI.read_text()
if 'const val CHRISTMAS = "frame_asset_christmas"' not in ui:
    ui = ui.replace(
        '    const val GOLD_CROWN = "frame_asset_gold_crown"\n',
        '    const val GOLD_CROWN = "frame_asset_gold_crown"\n'
        '    const val CHRISTMAS = "frame_asset_christmas"\n'
        '    const val HALLOWEEN = "frame_asset_halloween"\n',
    )
    ui = ui.replace(
        '    val ids = setOf(GOLD, MINT, PURPLE, GREEN, RED, GOLD_CROWN)',
        '    val ids = setOf(GOLD, MINT, PURPLE, GREEN, RED, GOLD_CROWN, CHRISTMAS, HALLOWEEN)',
    )
    ui = ui.replace(
        '        GOLD_CROWN -> R.drawable.style_frame_gold_crown\n',
        '        GOLD_CROWN -> R.drawable.style_frame_gold_crown\n'
        '        CHRISTMAS -> R.drawable.style_frame_christmas\n'
        '        HALLOWEEN -> R.drawable.style_frame_halloween\n',
    )
    ui = ui.replace(
        '        GOLD_CROWN -> Color(0xFFE0A51C)\n',
        '        GOLD_CROWN -> Color(0xFFE0A51C)\n'
        '        CHRISTMAS -> Color(0xFF2FAE68)\n'
        '        HALLOWEEN -> Color(0xFFF07A24)\n',
    )
    ui = ui.replace(
        '    PurchasedFrameSpec(PurchasedFrameCatalog.GOLD_CROWN, "Altın Taç", "Gold Crown", "Yüksek lig ve prestij ödülü", "High-league prestige reward", R.drawable.style_frame_gold_crown, Color(0xFFE0A51C), "LİG ÖDÜLÜ", "LEAGUE REWARD", R.drawable.style_icon_trophy),\n',
        '    PurchasedFrameSpec(PurchasedFrameCatalog.GOLD_CROWN, "Altın Taç", "Gold Crown", "Yüksek lig ve prestij ödülü", "High-league prestige reward", R.drawable.style_frame_gold_crown, Color(0xFFE0A51C), "LİG ÖDÜLÜ", "LEAGUE REWARD", R.drawable.style_icon_trophy),\n'
        '    PurchasedFrameSpec(PurchasedFrameCatalog.CHRISTMAS, "Yılbaşı", "Christmas", "Sezonluk yılbaşı etkinlik çerçevesi", "Seasonal Christmas event frame", R.drawable.style_frame_christmas, Color(0xFF2FAE68), "ETKİNLİK", "EVENT", R.drawable.style_icon_trophy),\n'
        '    PurchasedFrameSpec(PurchasedFrameCatalog.HALLOWEEN, "Halloween", "Halloween", "Sezonluk Halloween etkinlik çerçevesi", "Seasonal Halloween event frame", R.drawable.style_frame_halloween, Color(0xFFF07A24), "ETKİNLİK", "EVENT", R.drawable.style_icon_trophy),\n',
    )
ui = ui.replace('contentScale = ContentScale.FillBounds,', 'contentScale = ContentScale.Fit,')
UI.write_text(ui)

dict_text = DICT.read_text().replace('private const val MAX_CANONICAL_LENGTH = 32', 'private const val MAX_CANONICAL_LENGTH = 12')
DICT.write_text(dict_text)

DOC.write_text("""# Dictionary and purchased frame sources — 2026-09-03

## Purchased 2D Avatar Frame pack

Source: user-provided purchased archive `2D Avatar Frame (1).zip` (LAYERLAB artwork naming in the project catalog). The integration keeps original ownership/inventory IDs stable. Permanent retail variants are Red, Green, Mint, Purple and Gold; Gold Crown is reserved for league/prestige reward; Christmas and Halloween are seasonal/event items and remain inactive for ordinary shop discovery unless already owned/equipped.

Original Large PNGs were integrity-checked before integration. Expected SHA-256 values:

- Red: `fa2c79145b5857707a2e099689d3e727978787bca346a163687d57b63548aa2c`
- Gold Crown: `1e955fe915d8b1b35045a21c1c4221521dfdfe4355c9321c591bd172b802ff05`
- Christmas: `48fa2d6afd6f95e25c0ac7ac8330a7e8d97a653fdb98f9c17e1a0b226e24082d`
- Halloween: `96598d5769d25f7520291e83248b1e9fa8f2984aa2822b41df3525bc63fbf62d`

Gold and Purple were previously restored directly from the same archive; Green and Mint already matched the healthy originals.

## Canonical dictionaries

Mobile validation is backend-authoritative through `public.dictionary_words` and `get_dictionary_snapshot_v3(language)`. The game snapshot is restricted to active, game-allowed, non-abbreviation, non-proper-noun words of 2–12 letters. Turkish and English use separate versioned offline caches and never fall back across languages.

Imported expansion sources recorded in `dictionary_words.source_id/source_version`:

- Turkish: `wooorm/dictionaries` Turkish Hunspell dataset, repository revision `8cfea406b505e4d7df52d5a19bce525df98c54ab`. The dictionaries project publishes generated Hunspell dictionaries and carries the upstream package licensing/attribution files; imported data is provenance-tagged in the database.
- English: `wooorm/dictionaries` English US/GB Hunspell datasets, same repository revision. Existing legacy production entries are retained rather than destructively replaced.

The import is additive/upsert-based. Existing inventory, equipped cosmetics, player progression and historical dictionary rows are not deleted.
""")
print("production v3 source finalization complete")
