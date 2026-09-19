# Premium Canva Asset Integration — 2026-09-19

## Canva source

- Design: `Kelime Kuşatması - Uygulanabilir Mağaza Ürün Kütüphanesi`
- Canva design id: `DAHVltf0-Pc`
- Source format: 33 square pages, 1024×1024.
- Supporting design: `Kelime Kuşatması Premium – Seri Oyun Artwork` (`DAHVonvAGGo`).
- Text audit: Canva decorative artwork contains no required runtime product copy. Product names, descriptions, prices, purchase states and ownership states remain Android Compose text.

## Runtime asset mapping

The Android runtime uses lightweight text-free vector equivalents aligned to the approved Canva visual language so density scaling stays sharp, transparent artwork remains clean and APK weight stays low.

### Permanent Google Play products

- `series_game` → `drawable/premium_series_game.xml`
- `letter_table` → `drawable/premium_letter_table.xml`
- `score_calculator` → `drawable/premium_score_calculator.xml`
- `pro_lifetime` → `drawable/premium_pro.xml`

### Son Coin cosmetics

- `theme_black` / legacy `theme_dark_arena` → `drawable/store_art_theme_black.xml`
- `keyboard_crystal` → `drawable/store_art_keyboard_crystal.xml`
- `keyboard_obsidian` → `drawable/store_art_keyboard_obsidian.xml`
- `keyboard_midnight` → `drawable/store_art_keyboard_midnight.xml`
- `keyboard_black_gold` → `drawable/store_art_keyboard_black_gold.xml`
- `keyboard_premium_white` → `drawable/store_art_keyboard_premium_white.xml`
- `name_cyan` → `drawable/store_art_name_cyan.xml`
- `name_sapphire` → `drawable/store_art_name_sapphire.xml`
- `name_amethyst` → `drawable/store_art_name_amethyst.xml`
- `name_aurelia` → `drawable/store_art_name_aurelia.xml`
- `victory_crown` → `drawable/store_art_victory_crown.xml`
- `emoji_vip` → `drawable/store_art_emoji_vip.xml`

Profile-frame products continue to render their actual packaged frame assets through `PurchasedProfileFrameOverlay`; no decorative substitute is used.

## Runtime readiness

- `theme_black` is the live Black Theme catalog id. `theme_dark_arena` remains accepted only as a compatibility alias for older ownership/cache data.
- Crystal, Obsidian, Midnight, Black Gold and Premium White keyboards each have their own runtime palette and store artwork.
- Name styles keep their real runtime text colors and now use matching text-free signature artwork in the store.
- Crown Victory and VIP Emoji artwork is prepared and mapped, but those catalog items remain hidden from purchase until their in-match behavior is connected. A decorative store image alone must never make an unfinished cosmetic purchasable.

## Production rules

- Decorative product assets must keep transparent outer backgrounds and contain no baked product names, prices, `PRO`, `SATIN ALINDI`, lock/open states or descriptions.
- Runtime wording remains real Compose UI text and must not overlap image-baked wording.
- Do not introduce unverified external icon packs or duplicate purchased assets.
- Keep the existing verified `frame_round_golden_avatar` as the PRO profile frame; no new purchased frame package is required.
- Product CTAs remain server/billing authoritative. Artwork changes must not modify purchase, entitlement, score, matchmaking or competitive gameplay logic.
