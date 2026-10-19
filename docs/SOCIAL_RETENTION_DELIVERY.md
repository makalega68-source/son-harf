# Social and storefront completion — 2026-10-02

Current branch: `claude/mascot-animations-behavior-31rof9`. Game rules, original restored mascot rig/art, first-run welcome and adaptive icon are retained.

## Implemented

- Basic saved friends and classic rematches are free in both UI and server permissions. PRO retains its paid tools and 50 active-game allowance. Quick Duel still uses its existing mode entitlement.
- My Games combines ongoing Siege, Quick Duel and Son Harf rooms, prioritizes the player's turn and resumes the exact participant-validated room. Empty home states are compact and the brand artwork is shorter.
- Stable private friend codes, manual code entry and `kelimetahti://invite/CODE` links use existing block/self/request checks. App installation remains a Google Play link; automatic deferred linking after installation is not claimed.
- Persistent activity events: friend request/acceptance, challenge, turn, result, rematch and friend online. Owner-only history and read markers survive relaunch; event keys prevent duplicates. Retrieval is bounded to 100 events.
- Android notifications open the appropriate match or actionable Activity inbox. Recent unread events only, duplicate suppression, stale-turn checking, existing notification preferences, permission checks, and account validation apply.
- Native expanded shop previews, collection actions for owned products, shorter text, consistent brand and explicit unavailable Play offers.
- Champion gold frame is retained as the existing weekly server reward, with its number-one ornament and expiry/handoff.

## Verification

Executed against the live migrated project using rolled-back synthetic fixtures:

- `social_inbox_free_rematch_v1.sql`: ordinary non-PRO accounts, stable code, non-friend rematch and receiver acceptance, duplicate rejection, event creation, persistent read markers, owner RLS, outsider and anonymous-session rejection.
- `compact_store_and_verified_rewards.sql`: current supported fixture offers, coin debit, duplicate purchase/reward protection, disabled and expired offers, verified ad rewards, item ownership and actual equip.
- `permanent_style_ownership.sql`: owned supported styles survive store rotation; PRO reward frame is supported and not sold; retired mascot and anonymous purchase/equip restrictions remain.
- `shop_sale_window_purchase_enforcement.sql`: sale window enforcement.
- `throne_champion_and_pro_access.sql`: weekly handoff/expiry, championship frame hidden from sale, premium tool entitlements.

Historical store tests were updated to current supported products/reward configuration. Their temporary offers/tester slots and all fixture effects roll back; retired production bundles remain disabled.

Android unit tests, release-check build and native emulator captures are checked separately in CI. Debug runtime QA exercises the production fallback timer and replaces the seeded waiting room with a simulated real-rival room; it does not prove a physical-device two-account network session.

## External limits

- Notifications use authenticated WorkManager background checks (15-minute scheduling interval; Android can defer). There is no configured FCM sender/client project in this repository, so instant push delivery is not claimed.
- Google Play monetary purchase/restore needs an installed Play test-track build, configured products/server verification and a licensed tester. Side-loaded emulator captures do not validate a real charge.
- Physical-device reconnect and two-account live matchmaking/rematch need device testing. No physical-device or real-money result is reported as passed.
- Clubs and 3–4-player events remain postponed until the Google Play test as instructed. No Gemini/other AI API was added.
