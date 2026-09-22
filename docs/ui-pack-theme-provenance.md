# Purchased UI Pack Theme Provenance

This branch uses selected, text-free source artwork from the two purchased UI packages as raw theme material. The original ZIP archives remain outside the repository; only optimized production derivatives used by the Android app are committed.

## Source packages

- `Casual Game UI for Mobile Games #02.zip`
  - ZIP SHA-256: `05979152cf38353c5580cf37ad6cedc740ef2f31596b925f2fca2f0f2e3729cf`
  - Source: `Panels/Panels/Sub panels/Subpanel_shop.png` → global dark game surfaces
- `Casual Game UI for Mobile Games #02 (Old).zip`
  - ZIP SHA-256: `c2d8734978809258fbbb7898ff3c8a11d55e33fe21bf1cb62ff6841e39761d49`
  - Source: `Game Buttons/Rectangle Buttons/Blue.png` → primary/secondary/tertiary/danger action-button shells

## Production derivatives

- `app/src/main/res/drawable-nodpi/theme_pack_new_surface.png`
- `app/src/main/res/drawable-nodpi/theme_pack_old_button_blue.png`
- `app/src/main/res/drawable-nodpi/theme_pack_old_button_green.png`
- `app/src/main/res/drawable-nodpi/theme_pack_old_button_red.png`
- `app/src/main/res/drawable-nodpi/theme_pack_old_button_dark.png`

The derivatives preserve the source geometry/highlight language while recoloring it into the production navy/blue/green/red palette. No preview, demo screenshot, or embedded-text asset is used.

## Scope

Theme layer only. No game rules, navigation routes, scoring, multiplayer, billing, authentication, economy, or other business logic is changed by this integration.
