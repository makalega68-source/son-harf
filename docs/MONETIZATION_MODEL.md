# Son Harf Monetization Model

## Status

This document is the authoritative monetization policy for Son Harf from 2026-09-08 onward.

The previous absolute rule **"PAY-TO-WIN ASLA UYGULANMAYACAK"** is retired and must not be used as a product or test invariant. The active model is:

> **Kelimelik-style hybrid Freemium / Free-to-Play + advertising + Premium/PRO.**

This change does not authorize uncontrolled client-side advantages. Existing gameplay, server authority, balance, purchased inventory, progression, and stable user flows remain protected.

## Revenue pillars

1. **Free-to-Play core**
   - Core play remains accessible without a subscription.
   - Progression, leagues, social systems and matchmaking remain available to free users according to product rules.

2. **Advertising**
   - Thin adaptive banners may appear on non-gameplay surfaces such as lobby, profile, store and menus.
   - Mandatory banners/interstitials must not cover or interrupt an active match.
   - Rewarded video is opt-in and may grant server-validated rewards or mode-scoped helper resources.
   - Google UMP consent/privacy flow remains mandatory where required.

3. **Premium / PRO**
   - Ad-free non-gameplay experience.
   - Premium profile/status/Style surfaces.
   - Advanced statistics, post-match analysis and convenience/capacity features.
   - Private/social privileges supported by current server entitlements.
   - Daily helper resources such as **Hint** and **Letter Swap**, where the selected game mode explicitly supports them.
   - Future PRO benefits may affect gameplay only when the mode contract, server authority, telemetry and balance review explicitly support the effect.

4. **Son Coin and Style commerce**
   - Son Coin is the single user-facing soft currency.
   - The existing database column name `diamonds` is a legacy implementation detail and may remain internally until a dedicated safe migration is justified; it must not create a second user-facing currency.
   - Style, themes, frames, name styles and other personalization can be sold dynamically.
   - Purchased permanent Style inventory must survive catalog retirement.

5. **Season, event and rewarded monetization**
   - Time-limited catalog offers, season/event content, bundles and optional rewarded-ad rewards are allowed.
   - Catalog availability must be server controlled with explicit start/end windows.

## Gameplay-affecting monetization policy

Gameplay-affecting monetization is **allowed in principle**, but must be mode-scoped and server-authoritative.

A paid/rewarded helper must not be implemented as a client-only score, timer, rating or inventory mutation. The server must validate eligibility, consume the resource atomically and apply the effect according to that mode's rules.

### Initial rollout in Unified Pro

- **Hint:** re-enabled as a PRO daily helper resource.
- **Letter Swap:** re-enabled as a PRO daily helper resource.
- **Timer freeze / extra-time style effects:** remain dormant in core ranked duel until a dedicated server-side mode contract and balance review are completed.
- **Streak/rating protection:** remains dormant in core ranked duel until rating semantics and anti-abuse rules are explicitly implemented.
- **Direct 2x ranked match score:** not activated in this batch. It is no longer forbidden by an immutable policy, but the current ranked scoring protocol does not yet provide a reviewed server-authoritative contract for it.
- **Post-match reward multipliers:** preferred for future rewarded-ad/PRO expansion because they can increase Son Coin/XP/reward yield without changing the already-resolved winner.

These are product rollout decisions, not a return to the retired absolute pay-to-win prohibition.

## Server authority and entitlement rules

- Google Play purchases remain server verified and idempotent.
- PRO state is derived from verified entitlement/subscription state; the client must not self-authorize PRO.
- Consumable/helper grants and consumption must be atomic on the backend.
- Dynamic catalog sale windows are backend enforced, not merely hidden in the UI.
- Retired catalog items remain readable/equippable for legitimate owners.
- Purchased inventory rows must not be deleted merely because a catalog item is retired.
- Existing owner/admin test exceptions must be preserved when store functions evolve.

## Product safety gates

Every monetization change must pass the same release discipline as gameplay changes:

1. Inspect the latest verified working source and live schema.
2. Make the smallest backward-compatible change.
3. Keep gameplay calculations server-authoritative.
4. Run unit/contract/regression tests and Android build.
5. Run frame/asset provenance validation.
6. For database changes, run Supabase security and performance advisors after migration.
7. Merge only when the final PR head is green.

## Compatibility constraints

- Do not remove or downgrade approved Son Harf features to make room for monetization.
- Do not replace the canonical TR/EN dictionary authority with a local reduced dictionary.
- Do not break permanent purchased Style ownership.
- Do not add forced ads inside active gameplay.
- Do not create a second user-facing currency while Son Coin is the product currency.
- Do not bypass `RefinedDuelOverlay`, server timers, server scoring or authoritative match finalization for a monetized feature.
