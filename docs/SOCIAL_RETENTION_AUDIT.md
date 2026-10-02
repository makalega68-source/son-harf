# Social retention audit — 2 October 2026

Base: `9cc3160c327d478505cd0a1761caea365d819782`, existing mascot branch.

## Confirmed gaps and changes

- Home had no active-game library. `HomeSessions` now merges classic Siege and Quick Duel, deduplicates game IDs, verifies membership and sorts own turns by deadline. All / Your turn / Their turn filters launch the stored ID, mode and language through the existing game entry. Waiting games remain visible under All. Partial read failures retain that pool's last known list and show Retry.
- Home had only a league label. `HomeLeague` reads the current competitive season, RP, next threshold, end time and completed-season claimable rewards. Claiming uses the existing authoritative RPC, checks its success flag, refreshes the list and prevents duplicate taps. Future season rewards remain conditional on final ranking.
- Activity now groups existing friend and Last Letter requests, classic/Quick Duel challenges, own turns, online friends and completed Siege matches. Invitations are explicitly accepted/declined with server responses. Empty/expired and failed responses do not display a successful match launch. Existing invitation badges include Quick Duel and Last Letter.
- Online Siege result details now include Play again, friend rematch/add-friend actions, head-to-head history, league progress and completed Siege mission reward claims. Both classic and series parents create/reuse a new game through their existing RPC. A pending friend rematch is polled while the result is foregrounded; acceptance opens that game's ID. Existing PRO/friend requirements remain enforced; this does not introduce free non-friend rematches.
- Profile now displays Last Letter's existing personal-record RPC, plus Siege records computed from the returned finished-game library using the correct player side. Siege metrics are labelled **recorded finished games**, not guaranteed lifetime totals: existing PostgREST row limits still apply. Final cubes held/control are not labelled as total historical captures. Profile Rival history opens the history tab directly, including Siege results and rematch actions.
- Events groups upcoming Atelier slots and the weekly throne. The calendar uses Istanbul time, includes 19:00 and 22:00 triple XP slots, and anchors its clock to the existing tournament server time. Joining and viewing the throne use existing routes.
- Automatic companion speech/voice is disabled by default. Explicitly requested practice hints remain readable; they do not enable ambient voice. Home animation is confined to a clipped, dedicated section, rendered only when a mascot is owned. The Last Letter companion is confined to its reserved slot. Explicitly requested Last Letter hints use the existing inline hint strip even when a mascot is owned. Gesture and facial animation remain; no external AI was added.
- Product name remains **Kelime Tahtı**; primary mode is **Kelime Kuşatması / Word Siege**. The blanket localization replacement that conflated them was removed. Existing branding tests were updated to the requested hierarchy.

## Checks

- New behavior tests cover membership, active/finished filtering, deadline ordering, duplicate pools, both score sides, draw denominator, empty records, Istanbul event slots and TR/EN product/mode naming.
- Read-only live checks confirmed the referenced season, missions, tournament, throne and invitation RPCs exist and grant `authenticated` execute access. Their deployed return columns match the existing Android DTOs.
- The existing 15,000 ms fallback remains in `WordSiegePanMatch`; the parent game's refresh loop remains mounted during temporary practice. This is a code-path finding, not device timing evidence.

## Pending device QA

Not marked complete: exact elapsed-time fallback on a device, continued real matchmaking/handoff during AI, two-account rematch acceptance, real authenticated home/library/activity clicks, and visual review on small screens/large font settings. No live purchase success is claimed: the previously reported Play service-account configuration remains separate from this patch.

Club and 3–4 player modes remain deferred. No new branch or main merge.
