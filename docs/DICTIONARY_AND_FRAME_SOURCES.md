# Son Harf dictionary and purchased frame provenance

## Canonical dictionaries

Son Harf uses `public.dictionary_words` as the authoritative word corpus. Mobile clients fetch a language-specific, game-allowed snapshot through `get_dictionary_snapshot_v3`. The game snapshot currently accepts words between 2 and 12 characters; the database may retain longer source entries for future game modes.

### Turkish (`tr`)

Primary production expansion source: `wooorm/dictionaries` normalized Turkish dictionary (`dictionary-tr`), source revision `8cfea406b505e4d7df52d5a19bce525df98c54ab`. The Turkish dictionary package declares the MIT license. Existing legacy Son Harf entries are retained for compatibility and tagged separately in `source_id`; no legacy rows are deleted by the V3 migration.

### English (`en`)

Primary production expansion source: normalized English dictionaries in `wooorm/dictionaries`, source revision `8cfea406b505e4d7df52d5a19bce525df98c54ab`, based on the English spelling/SCOWL ecosystem. The English dictionary package carries MIT and BSD licensing requirements. Existing legacy Son Harf entries are retained and provenance-tagged separately.

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
