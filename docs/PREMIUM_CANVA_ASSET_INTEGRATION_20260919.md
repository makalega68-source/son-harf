# Premium Canva Asset Integration — 2026-09-19

## Canva source

- Design: `Kelime Kuşatması - Uygulanabilir Mağaza Ürün Kütüphanesi`
- Canva design id: `DAHVltf0-Pc`
- Source format: 33 square pages, 1024×1024.
- Text audit: the Canva design exposes no rich-text content. Runtime product names, descriptions, prices and purchase states remain Android Compose text.

## Runtime asset mapping

The Android runtime uses lightweight vector equivalents derived from the approved Canva visual language so density scaling stays sharp and APK weight stays low:

- `series_game` → `drawable/premium_series_game.xml`
- `letter_table` → `drawable/premium_letter_table.xml`
- `score_calculator` → `drawable/premium_score_calculator.xml`
- `pro_lifetime` → `drawable/premium_pro.xml`

The vectors use the current blue / turquoise / lilac / orange / white palette and contain no baked text, prices, labels or product names. All visible wording is real Compose UI text.

## Production rules

- Do not rasterize product names, prices, `PRO`, `SATIN ALINDI`, lock/open states or descriptions into decorative assets.
- Do not introduce unverified external icon packs.
- Keep the existing verified `frame_round_golden_avatar` as the PRO profile frame; no new purchased frame package is required.
- Product CTA remains disabled until Google Play returns a real `ProductDetails` offer.
