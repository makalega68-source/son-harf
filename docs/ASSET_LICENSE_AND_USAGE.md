# Son Harf Asset Register

## LAYERLAB – GUI - Avatar Frame
- Source package: `2D Avatar Frame.zip`, purchased 2026-09-02.
- Integrated permanent/runtime variants: Red, Green, Mint, Purple, Gold, Gold Crown.
- Christmas and Halloween remain reserved in the purchased source package for future seasonal/event releases; they are deliberately not bundled into the permanent store build.
- Usage: cosmetic profile frames only; no gameplay advantage.
- Important restriction from vendor page: asset must not be used as input/training material for generative-AI programs. Integration here is deterministic file extraction and Android runtime use; no generative image processing was used.

## Nieobie – Game Icon Pack v1.4
- Integrated existing selected PNGs: user, palette, trophy, coin.
- Usage: Style/profile/reward/economy semantics, preserving one icon language.
- Project record: CC0 1.0 / commercial use permitted.

## Eric Wang VFX – Game VFX: UI & Interaction Effects Bundle
- Source package: `game_vfx_ui_interaction_effects (2).unitypackage`.
- Integrated texture subset: `twink_01.png` only (SHA-256 `4ed0e0f0c12df51c56f2145720031a55ca9db59a20d851d6fe47c1d632397b28`). Other Unity-prefab/shader resources remain outside the Android build.
- Unity prefabs/shaders are not embedded. The texture is adapted to bounded native Jetpack Compose victory and board-action overlays. Board rings are native Compose strokes; no additional package texture, prefab, shader or Unity runtime is bundled for them.
- Board effects are one-shot screen-space overlays: placement 650 ms with four cyan stars, resolved move 800 ms with five gold stars. They are clipped at the board viewport rather than individual cells and contain no input handlers or infinite animation.
- Usage is cosmetic only and isolated from scoring, rating, turn, timer and matchmaking state.

## Mobile Game UI FREE version
- Integrated restrained subset only: Market icon in Style shop/bundle/economy surfaces.
- The pack does not replace Son Harf's blue-white theme, typography, spacing or CTA hierarchy.

## Product constraints
- Pay-to-win is prohibited.
- OYNA remains primary CTA.
- Warm Beginnings remains the sole background music.
- Third-party provenance must remain documented for future due diligence/transfer.

## Originality audit (2026-09-29)
Items the repository can vouch for, and items only the owner can confirm. No license terms are invented here.

| Asset | Where | Provenance in the repo |
|---|---|---|
| Sound effects `sfx_*.wav` | `res/raw` | Rendered for this project (commits `1c31bed5`, `8d22f433`); no third-party samples recorded. |
| Background music `warm_beginnings.mp3` | `res/raw` | **Not recorded.** Owner must keep the purchase/license record. |
| Mascot clips `mascot_victory.mp4`, `mascot_defeat.mp4`, `intro_welcome.mp4` | `res/raw` | Produced for this project (commits `412e9c54`, `b5f66869`); source tool/licence of the base art **not recorded**. |
| Mascot (Obi) art, launcher icons `obi_launcher_*` | `drawable-nodpi`, `mipmap-*` | Project art; source tool/licence **not recorded**. |
| Profile frames `style_frame_*`, `profile_frame_*` | `drawable-nodpi` | LAYERLAB Avatar Frame (see above), pinned by `scripts/verify_frame_provenance.py`. |
| Icons | `drawable*` | Nieobie Game Icon Pack (CC0) and Mobile Game UI FREE subset (see above). |
| Fonts | — | No bundled font files; the system font (`FontFamily.SansSerif`) is used. |
| Dictionaries `assets/dictionary/*.txt` | `assets` | See `DICTIONARY_AND_FRAME_SOURCES.md`. They contain ordinary words such as "kelimelik" and "scrabble"; these are dictionary entries, not brand use. |

No asset file name or content refers to Kelimelik, He2 Apps or Scrabble. Board bonus cells, the Starting Seal, tiles and the HUD are drawn natively in Compose (no bitmap copied from another game).

## Kuşatma board skins (2026-09-29)
- The board slab (rim, bevel, inlay line, studs, recessed field) is drawn in code (`WordSiegeBoardSkin.kt`);
  no frame picture is used.
- `board_plate_*` (JPEG, 192 px, `drawable-nodpi`): stone grain crops from four board images the owner
  generated and supplied using the prompts in `docs/store-art/`; the generation tool/plan licence stays
  with the owner's records.
- `store_art_board_*` (JPEG, 384 px): rendered in-repo from the same slab design, the plate grain and the
  bonus icons below, so the store shows the board exactly as it is drawn in play.

## Kuşatma 3D bonus icons (2026-09-29)
- `bonus_letter_boost`, `bonus_letter_boost_plus`, `bonus_word_surge`, `bonus_word_surge_plus`,
  `bonus_starting_seal`, `bonus_premium_star` (PNG, `drawable-nodpi`): generated with Canva AI image
  generation in the owner's connected Canva account on the owner's instruction (media ids MAHWmMPo7v4,
  MAHWmIvt1Bw, MAHWmKiM4ZA, MAHWmN97_Yg, MAHWmDL1ydk, MAHWm1TyFHg); white background removed in-repo.
  Commercial use follows the owner's Canva plan terms.
- `store_art_board_classic` (JPEG, 384 px): rendered in-repo from the classic board colours and the bonus icons.
