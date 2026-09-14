# Kelime Kuşatması store — implementation and release checks

Source: branch `codex/compact-store-monetization-20260913`, based on verified `8877b2e` and its latest-game ancestor `4d8df34`. Match logic, scores, ownership colours, authentication and the four main navigation destinations are preserved.

## Implemented
- Profile entry to a single store with Featured, Season, Styles, Mascots and PRO.
- Daily free gift reuses the existing daily-check-in ledger and daily uniqueness rule.
- One Son Coin balance. Google Play packs open separately from the featured catalogue.
- Existing purchased frame artwork and runtime previews are reused; no generated assets.
- Five active frames and the existing name/keyboard/arena styles. Unsupported mascot/effect products remain unavailable.
- Starter bundle: Red Line + Cyan Name, 240 SC. Limited Quiet Light collection: Ice Mint + Violet Spectrum, 400 SC, offered for 14 days from migration application. Existing item prices and ownership are unchanged.
- Server transaction owns bundle price, availability, debit, ledger and inventory delivery. Replays do not debit again.
- Existing PRO and monthly Season Pass verification are reused. Missing Play products cannot start billing. Restore actions retry completed purchases through the server verifier.
- No forced banner during gameplay in the active shell; verified PRO suppresses mandatory banners.
- Reward grants require a server intent and a signed AdMob SSV callback. Clients cannot mint rewards with arbitrary response IDs. Receipt, transaction uniqueness, account binding, ad-unit allowlist and quota checks run on the server.

## Verification
- SQL regression tests run with generated, isolated account fixtures inside BEGIN / ROLLBACK. No test account, currency or purchase is retained.
- Tests cover bundle debit/delivery/replay, insufficient balance, expired offers, daily reward replay, unverified ads, callback role, account/ad-unit binding, duplicate callbacks and privileged grants.
- Node tests verify DER ECDSA callbacks and reject tampering, unknown keys, duplicate parameters and unsigned fields.
- Android CI and existing unit/regression gates validate the client.
- Local workspace disconnected during implementation; authoritative changes are saved directly in GitHub. A physical phone and a Google Play licensed purchase were not tested.

## Required account configuration before paid release
1. Confirm the existing Play products `vip_monthly`, `vip_yearly`, `season_pass_monthly` and coin packs are available on the app's internal test track. Complete a licensed purchase, restore, renewal and cancellation test. The existing verification service must have its Google Play service account configured.
2. Configure the rewarded ad unit's server callback as:
   https://bzdtftzdjtjoqhtcqtxb.supabase.co/functions/v1/admob-ssv
3. Populate `store_monetization_config.allowed_rewarded_ad_units` with the exact ad-unit ID sent in verified callbacks and enable `rewarded_enabled` only after AdMob SSV configuration and a callback test. It defaults to false; daily free gifts remain available.
4. The server function `admob-ssv` uses its own ECDSA webhook authentication, so gateway JWT verification is disabled only for this callback.
5. Unsupported mascots, tile skins and effects need a real runtime delivery path before being offered. Do not sell previews without delivery.

References: [Play purchase security](https://developer.android.com/google/play/billing/security), [AdMob SSV](https://developers.google.com/admob/android/ssv).

## Applied state
Migration `20260913112201_compact_store_and_verified_rewards.sql` is applied to the active project. The SSV callback is deployed and active; rewarded ads remain off until account configuration. Post-migration SQL tests passed and all generated fixtures were rolled back. Android CI, Final Unified Validation and Frame Provenance Gate passed for `cc647ec`, including 223 unit tests and 6 signature tests.

Security advisors: the three service-only tables intentionally have RLS with no client policy. The bundle history SELECT policy is restricted to the current account, including authenticated guest accounts already supported by the app. It does not expose other players' purchases.
