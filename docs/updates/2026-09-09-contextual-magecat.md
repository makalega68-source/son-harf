# Contextual 2D Mage Cat — implementation record

Base: c2cbb9aec986cfcdafb1fea7d84aeabff830fcbe (main inspected before changes).

## Findings
- The previous renderer used a single WebP for every emotion and only moved/scaled it.
- The committed WebP failed an independent Pillow decode. Android BitmapFactory previously hid decode failures with a blank spacer; device-specific crash causality is not proven.
- The overlay polled an additional backend every 650 ms, conflated event names with event identity, and checked urgency without checking the local player's turn.

## Changes
- Nine original 2D derivative emotion poses, referenced from the owner-supplied Mage Cat prefab. Transparent PNG, cached downsampled frames; background decoding.
- Bounded celebration/wave/comfort motion. Android disabled-animation preference respected. No continuous bouncing during matches.
- Offline contextual behaviour AI: pure read-only snapshot policy, no LLM/API or extra account/game data transfer. Handles repeated words, streak, taking the lead, urgency on own turn, rejected words, territory loss, victory, defeat and draw. No game commands, score changes or economy privileges.
- Reserved inline dock in Premier duel and Word Siege; no absolute positioning over the keyboard or board. Greeting in main lobby, companion on loading and result screens.
- English/Turkish copy and theme-aware UI. Existing main modes, keyboard, store, profile and server logic retained.
- Updated source-contract gates to follow the inline shared-snapshot integration instead of requiring obsolete polling.

## Asset provenance
- Input prefab preview extracted statically; package scripts were never executed.
- Atlas is AI-redrawn artwork derived from that reference, not a claimed FBX render or skeletal animation export.
- Runtime atlas: app/src/main/res/drawable-nodpi/mage_cat_expressions.png.
- Original source binaries are not added to APK. Licensed-source ownership and derivative distribution rights remain governed by the purchased asset license.

## Release status
See the final verification record for build/tests and remaining device validation limits. This branch must not be represented as a device-verified release before those checks exist.
