# Son Harf dictionary and purchased frame provenance

## Canonical dictionaries

Son Harf uses `public.dictionary_words` as the authoritative word corpus. Mobile clients fetch a language-specific, game-allowed snapshot through `get_dictionary_snapshot_v3`. The game snapshot accepts words between 2 and 15 characters so the local canonical cache covers the full 15×15 Word Siege board; longer source entries remain in the database for server-side modes and future game surfaces.

Client and server normalization both use Unicode NFC canonicalization plus language-aware case folding. A persisted mobile snapshot is an offline continuity cache only: when network access is available the app refreshes it from the authoritative backend so existing installations receive dictionary corrections and expansions.

### Turkish (`tr`)

Live source (verified in `public.dictionary_words`, 2026-10-04): `source_id = tdk-gts-autocomplete`, version "TDK GTS official 2026-09-16", the headword list of the Türk Dil Kurumu Güncel Türkçe Sözlük (`https://eski.sozluk.gov.tr/autocomplete.json`). 61,742 active rows; 60,643 of them are 2–15 letters and game-allowed, and the bundled `app/src/main/assets/dictionary/tr.txt` matches that set exactly (circumflexed letters folded to plain letters). 44 foreign entries that TDK lists without a Turkish spelling (e.g. `marketing`, `deadline`) are kept but not game-allowed. Proper nouns are not part of the headword list; a name that is also a meaningful TDK word (e.g. `deniz`, `çiçek`, `umut`) is valid as that word. Redistribution rights for the TDK list are not asserted by this repository.

### English (`en`)

Live source: `source_id = scowl-esdb-en-us`, version "SCOWL/ESDB 2026.02.25", the American English list of SCOWL (Spell Checker Oriented Word Lists) and the English Speller Database (`en-wl/wordlist-diff`, commit `71d7dd07676edb60ade43552e10b41314b7e9287`), under the SCOWL / ESDB source licences. 79,063 game-allowed rows, matching the bundled `en.txt`. English has no single official language academy comparable to TDK, so this open spelling standard is used.

The project does not claim that any finite corpus contains every word that can exist in Turkish or English. The production goal is a broad, licensed, normalized canonical corpus with deterministic validation, offline snapshot support and explicit provenance.

## Purchased 2D Avatar Frame package

Source archive supplied by the project owner: `2D Avatar Frame (1).zip`. The original archive is treated as the authoritative source for these app assets. Integration uses the original PNG payloads, not older damaged staging copies.

Integrated permanent variants:

- `frame_asset_red` / Red
- `frame_asset_green` / Green
- `frame_asset_mint` / Mint
- `frame_asset_purple` / Purple
- `frame_asset_gold` / Gold

Non-retail variants retained by stable ID:

- `frame_asset_gold_crown` / Gold Crown — progression/league reward
- `frame_asset_christmas` / Christmas — seasonal/event
- `frame_asset_halloween` / Halloween — seasonal/event

Gold Crown and seasonal variants are not automatically activated for normal shop sale. Existing ownership/equipped records are preserved. Asset integrity is enforced in CI with exact SHA-256 checks against the project owner's source archive.

No marketplace license terms are invented in this repository document. The project owner is responsible for retaining the original purchase/license record for due diligence and future transfer.
