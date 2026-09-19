# Profile Frames V2 — Production Rollout (2026-09-19)

## Scope

Profile Frames V2 introduces four permanent, cosmetic-only Google Play one-time products. Historical profile-frame products remain retired and isolated from this catalog.

| Product ID | TR title | Type | Target TR price |
|---|---|---|---:|
| `profile_frame_pink_blossom` | Pembe Çiçek Premium | One-time / non-consumable | 150 TL |
| `profile_frame_blue_royal` | Mavi Royal Premium | One-time / non-consumable | 150 TL |
| `profile_frame_amethyst` | Ametist Fantastik | One-time / non-consumable | 150 TL |
| `profile_frame_emerald` | Zümrüt Fantastik | One-time / non-consumable | 150 TL |

The Android client must display the real formatted Google Play price when ProductDetails is available. `150 TL` in `ProductCatalog.PROFILE_FRAME_FALLBACK_PRICE_TRY` is display fallback only and is not a billing authority.

## Production backend state

Applied to Supabase project `son-harf`:

- Migration `20260919191657_profile_frames_v2.sql`.
- Four active `shop_items` rows with zero diamond price. Zero diamond price is not a free-purchase path.
- Four `store_product_grants` rows grant the matching permanent style into `user_inventory` only after verified Play purchase processing.
- Four `store_catalog_config` rows are enabled.
- `purchase_shop_item(text)` rejects all four V2 product IDs with `google_play_required`, preventing Son Coin/diamond RPC bypass.
- `verify-play-purchase` Edge Function updated to version 8 with all four V2 IDs in the supported one-time product set.
- `profile_frame_state_v1` hardened with `security_invoker=true`; anonymous SELECT access revoked.

## Google Play Console — required before sales can open

Create each product using the exact product IDs above as a one-time in-app product / non-consumable entitlement. Configure the Turkish market price as **150 TL** for each item, complete the product listing, and activate/publish the product for the release track used by the app.

Do not change a product ID after publishing. The Android catalog, Supabase grants, verification Edge Function and Play Console must use the same exact ID.

## Verification checklist

1. Google Play returns ProductDetails for all four IDs.
2. Store cards show the Play-formatted live price rather than fallback text.
3. A license tester can complete purchase of one frame.
4. `verify-play-purchase` returns verified success and the matching item appears in `user_inventory` exactly once.
5. The purchased frame can be equipped and survives app restart / login refresh.
6. A direct `purchase_shop_item` attempt for a V2 frame fails with `google_play_required`.
7. A non-owner cannot equip the frame.
8. Restore purchases re-verifies ownership without granting a duplicate entitlement.

## Release gate

Do not merge the Profile Frames V2 feature branch into the canonical release branch until all four Play products are active and the license-tester purchase/equip/restore flow has passed.
