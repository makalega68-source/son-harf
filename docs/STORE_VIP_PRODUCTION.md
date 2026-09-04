# Son Harf Store + VIP Production Runbook

Last updated: 2026-09-04

## Product invariants

Son Harf monetization is strictly cosmetic/convenience-first. Real money, VIP, Season Pass, Son Coin and rewarded ads must never grant extra match time, score, score multipliers, rating/league points, additional moves, error forgiveness, correct-word or best-move suggestions, opponent penalties, favorable matchmaking, or any other ranked win power.

Ranked live information that can affect a decision must be symmetric for both players. VIP differentiation is limited to presentation, filtering, archive length, post-match analysis, social convenience, personalization, collection and prestige.

Son Coin is the only store currency. There is no second premium currency.

## Google Play product IDs

Subscriptions:
- `vip_monthly`
- `vip_yearly`
- `season_pass`
- `season_pass_monthly` — legacy restore compatibility only

One-time Son Coin:
- `coins_500` — MINI
- `coins_1500` — POPULAR candidate
- `coins_3500` — VALUE candidate
- `coins_8000` — MEGA

One-time packs:
- `starter_style_pack`
- `premium_style_pack`
- `season_pack`
- `vip_welcome_pack`

Legacy restore-only:
- `theme_neon`

Never hard-code real-money prices in Android. Display `ProductDetails` localized prices only. Products not present/approved in Google Play or disabled in `store_catalog_config` must not expose an enabled checkout action.

## Store information architecture

Production categories are exactly:
1. VIP
2. STYLE
3. SON COIN
4. SEZON PASS
5. PAKETLER
6. KASA
7. ETKİNLİK

Style supports profile frame, avatar background, nameplate, badge, title/name style, game theme, keyboard theme, VS intro, word-send effect, victory effect and emote/emoji pack. Rarities are `STANDARD`, `RARE`, `EPIC`, `LEGENDARY`, `SEASON`, `EVENT`, `VIP`; rarity is prestige/collection metadata only.

The primary Style state machine is `SATIN AL -> SAHİPSİN -> KULLAN -> AKTİF`. Direct trials use server time or completed-match count and are capped server-side.

## VIP lifecycle

VIP products are server-authoritative subscriptions. Android purchase state is never sufficient to unlock VIP. A purchase token must be verified by `verify-play-purchase`; Postgres entitlement state is authoritative for the app.

Expected states:
- active: VIP enabled
- grace: VIP remains enabled until Google Play says otherwise
- canceled before expiry: entitlement remains usable through verified expiry
- hold / paused: VIP disabled
- expired: VIP disabled
- revoked/refunded: VIP disabled immediately after server reconciliation
- pending: no grant until Google Play reports a purchased/active state

Expiry/revoke does not delete earned Son Coin or ordinary purchased Style. VIP-only Style remains owned where applicable but cannot be equipped/used while VIP is inactive.

## Purchase idempotency

`purchases.purchase_token` is unique. `apply_verified_play_purchase_v2` uses `INSERT ... ON CONFLICT DO NOTHING` plus the insert result to distinguish the first grant from restore/reconciliation. A one-time token may grant Son Coin/Style exactly once. Repeated restore, reconnect or Edge Function retry may update verification metadata but must not grant again.

`diamond_ledger` / Son Coin ledger is append-only for clients. Direct client writes to purchase/ledger/entitlement tables are denied. Payment tokens, order IDs, email, phone or message text must not be written to analytics or application logs.

## Google Play server flow

1. Android Billing returns a purchase token.
2. Android sends product ID + token to `verify-play-purchase` using the authenticated Supabase session.
3. Edge Function obtains Google service credentials from server secrets.
4. Edge Function re-fetches the purchase/subscription from Google Play Developer API.
5. Only a verified purchased/eligible state calls `apply_verified_play_purchase_v2` with service-role authority.
6. Database performs idempotent entitlement/inventory/SC grant.
7. Edge Function acknowledges/consumes only after the server grant succeeds.
8. Restore sends owned purchases through the same verification route.

RTDN does not grant from Pub/Sub payload alone. `google-play-rtdn` authenticates the webhook with `GOOGLE_PLAY_RTDN_SECRET`, extracts the token, re-fetches current Google Play state, then calls `reconcile_play_entitlement_v1`. Do not log the token.

## Backend rollout order

Do not partially roll out purchase v2. The safe release order is:

1. CI green for the exact Android/SQL/Edge Function commit.
2. Apply migrations in timestamp order:
   - `20260904090000_store_vip_production_hardening.sql`
   - `20260904090500_play_entitlement_reconciliation.sql`
   - `20260904091000_reward_center_v8.sql`
   - `20260904091500_style_trial_direct.sql`
   - `20260904092000_word_siege_area_score_authority.sql`
3. Verify RLS, grants, unique constraints and service-role-only purchase RPCs.
4. Deploy `verify-play-purchase` with JWT verification enabled.
5. Deploy `google-play-rtdn` with JWT verification disabled only because the function performs its own `X-Son-Harf-RTDN-Secret` authentication; configure the secret before enabling Pub/Sub delivery.
6. Execute non-mutating verification queries and Supabase security advisors.
7. Install the APK built from the same commit and run purchase/restore/offline/VIP-expiry smoke tests.
8. Only then mark the commit stable / merge according to the existing release process.

If any step fails, stop. Do not merge and do not present the APK as production-verified.

## Rewarded ads and Kasa

Rewarded ads are optional and never appear during a match. Ad privacy consent gates ad loading. Duplicate ad response IDs cannot grant twice; daily limits are server-side.

Kasa is deterministic and additive to normal match rewards. Fixed bonus levels are 200 / 400 / 600 / 800 SC. No random roll or VIP multiplier is allowed.

## Word Siege scoring authority

Each currently owned territory cube is worth exactly 2 area points. Word/letter score remains cumulative. When a cube changes owner, only territory ownership-derived area score changes; previously earned word/letter score is never clawed back.

Preview and committed move must use the same area delta engine. `20260904092000_word_siege_area_score_authority.sql` derives current area score from board ownership and reuses `private.word_siege_area_delta_v1` for preview.

## Analytics

Allowed store event names:
- `store_view`
- `product_view`
- `preview_start`
- `checkout_start`
- `purchase_success`
- `purchase_cancel`
- `purchase_failure`
- `purchase_pending`
- `restore`
- `equip`
- `vip_start`
- `vip_renew`
- `vip_cancel`
- `vip_expire`
- `season_upgrade`
- `rewarded_ad_complete`
- `piggy_open`

Analytics must remain non-PII and must strip payment tokens/order IDs and communication content.

## Release verification matrix

Required green checks before production delivery:
- Android compile and unit tests
- UI/contract regression tests
- Frame provenance gate
- final unified validation
- duplicate Son Coin grant test
- purchase restore test
- pending purchase does not grant
- offline purchase retry/reconnect
- multi-device entitlement reconciliation
- VIP expiry/grace/hold/revoke
- Season Pass boundary
- Style trial expiry and one-match consumption
- free/VIP authorization boundaries
- RLS allow/deny probes
- rewarded-ad duplicate proof and frequency cap
- Word Siege preview/actual 2-point territory equality
- classic Son Harf, Word Siege, matchmaking, bot fallback, rating, navigation, profile, sound/music and existing Style regressions

Final deliverable must be the directly installable APK from the exact green commit. Do not distribute a ZIP as the user-facing artifact.
