# Wing & Blossom Store Frames — 2026-09-15

This change introduces six visual-only profile frames to the Son Coin Style store while preserving the existing PRO Golden entitlement and all existing frame behavior.

## Store catalog

- `frame_wing_silver` — Gümüş Kanat / Silver Wings — 180 SC
- `frame_wing_blue` — Mavi Kanat / Blue Wings — 240 SC
- `frame_flower_pink_blossom` — Pembe Çiçek Çelengi / Pink Blossom Wreath — 220 SC
- `frame_wing_pink` — Pembe Kanat / Pink Wings — 240 SC
- `frame_wing_gold` — Altın Kanat / Golden Wings — 260 SC
- `frame_wing_aurora` — Aurora Kanat / Aurora Wings — 320 SC

## Rendering contract

The new artwork is packaged as 512×512 Android vector drawables. This keeps the frame transparent and resolution-independent, avoids device-specific bitmap decode failures, and preserves a single standard canvas across profile, collection, and storefront rendering. Wing and blossom frames receive extra outer clearance in the shared `FramedProfilePhotoAvatar` component so decorative elements do not cover the avatar.

## Ownership and purchase contract

Discovery and prices remain server-authoritative through `shop_items`. Purchases use the existing `purchase_shop_item` RPC, which validates balance and creates ownership server-side. On success the client immediately calls `equip_shop_item`; later switching is available from Profile → Collection through the existing inventory/equip flow. Cosmetics are visual only and provide no gameplay power.
