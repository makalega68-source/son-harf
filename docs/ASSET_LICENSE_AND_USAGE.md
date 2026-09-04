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
