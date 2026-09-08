# Son Harf Monetization Model

## Product decision

Son Harf uses **Premium = ad-free experience + presentation + non-competitive convenience**. The product owner's 8 September 2026 instruction forbids pay-to-win, including paid real-time strategic information unavailable to free opponents. See `PREMIUM_PRODUCT_REVIEW_20260908.md` for the researched Kelimelik mapping.

The free player must retain full access to the core competitive game. Payment may improve convenience, visibility, progression comfort and collection/status expression, but must not directly buy a match result.

## Allowed Premium advantages

Premium / PRO may include:

- ad-free experience outside mandatory legal/consent surfaces,
- expanded statistics and match-history analysis,
- richer rival/opponent analysis,
- additional social capacity such as larger friend/rival tracking lists,
- additional non-ranked daily challenge or task capacity,
- faster access to non-competitive reward presentation/claim flows where this does not alter ranked match outcomes,
- Premium-only Style items, profile frames, titles, badges and other status cosmetics,
- post-match word/letter analysis; any real-time score preview or remaining-letter information must be equally available to free and paid competitors,
- Premium tournament or event access only when the event is separated from standard ranked fairness and its rewards do not create gameplay power,
- expanded customization and convenience surfaces.

## Son Coin

Son Coin remains the single primary soft/premium currency. It may be earned and/or purchased.

Son Coin may be spent on:

- Style items,
- profile frames,
- titles and badges,
- presentation effects,
- non-power progression convenience,
- eligible event access,
- other non-match-power personalization/value items.

## Forbidden direct pay-to-win

The following must not be sold for real money, Premium or Son Coin in ranked/standard competitive play:

- extra match time,
- direct score or score multipliers,
- rating/league points,
- stronger matchmaking position,
- automatic valid-word acceptance,
- invalid-word forgiveness,
- forced opponent penalties,
- paid turn extensions,
- paid retry of a competitive turn,
- paid mechanics that materially increase win probability by overriding the rules.

## Information-assist boundary

Paid information must not give a real-time competitive advantage. Historical analysis and non-competitive convenience are distinct from live help during a match.

Permitted examples include:

- expanded personal performance statistics,
- historical letter/word usage analysis,
- post-match missed-opportunity analysis,
- opponent tendency summaries based on already available historical data,
- non-competitive personal practice analysis.

Do not sell live playable-word suggestions, hidden answers, exclusive remaining-letter counters, or other turn-solving assistance. Core friend invitations and rematches are free product goals; existing paid entitlements are not silently revoked by this policy update.

## Store design rule

The store must answer three questions for every paid item:

1. What is the player buying?
2. Why should the player want it?
3. What visible or practical value does the player receive immediately?

The store should prioritize clear value categories instead of an undifferentiated catalog:

- **Premium / PRO** — convenience, analysis, capacity and ad-free value,
- **Style** — identity, prestige and collection,
- **Son Coin** — a simple single-currency purchase path,
- **Events / Passes** — time-bounded progression or access that does not create ranked power.

## Competitive fairness invariant

Standard/ranked Son Harf must remain understandable as a fair contest. A paying player can have a better experience around the match, more information about their own performance, more customization and more convenience, but cannot purchase the decisive competitive variables of score, time, rules or rating.

## Implementation order

1. Stabilize the two core game modes.
2. Redesign the store around player value and the four categories above.
3. Add Premium entitlement state and UI.
4. Connect ad-free behavior to Premium.
5. Add statistics/analysis and capacity benefits.
6. Connect Son Coin purchase/spend flows to approved non-power items.
7. Add billing only after entitlement, restore-purchase, analytics and failure handling are tested.
8. Validate monetization through a current APK before declaring the feature complete.

## Analytics required

Track at minimum:

- store_open,
- premium_offer_view,
- premium_purchase_start,
- premium_purchase_success,
- premium_purchase_restore,
- son_coin_offer_view,
- son_coin_purchase_start,
- son_coin_purchase_success,
- style_item_view,
- style_item_purchase,
- premium_feature_used,
- ad_removed_by_premium.

Do not log sensitive payment details.
