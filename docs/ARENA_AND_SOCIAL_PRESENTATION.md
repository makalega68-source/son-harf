# Arena and social presentation — 2 October 2026

References reviewed: Google Play listings for Kelimelik (`com.he2apps.kelimelik`), Words With Friends (`com.zynga.words3`), and Wordfeud (`com.hbwares.wordfeud.free`). The listings emphasize friends, game chat, custom profile frames and multiplayer rivalry. Reference store screenshots were opened for comparison. No reference artwork, rules or assets were copied.

Implemented:
- Kuşatma online/practice: the shared player card now uses the equipped circular frame, leader crown, gold edging, player green / rival red, real profile league/rating and PRO status. Scores and word/territory points remain separate; board control is derived from actual owned cells. The card is 126dp high to make the larger avatar, score row and control bar fit. Existing source assertions for card height were updated for this intentional layout change.
- Kuşatma online/practice: newly confirmed moves trigger a 1.15-second edge burst and a short move/capture ribbon. Initial historical moves are not replayed. Existing purchased board effects and score flights remain intact.
- Son Harf: newly accepted words have the same short impact/ribbon; the last five active seconds have an edge pulse, suppressed during preparation and reconnection grace.
- Atölye: accepted words/combos trigger edge feedback; the last five seconds have an edge pulse. Existing combo, confetti, scoring, tasks and tournament submission are preserved.
- Home: a prominent friends entry shows real loaded online friends and incoming request/invitation count, opens the existing social screen, and never sends an invitation automatically.
- Social: share-invite entry, explicit refresh, equipped friend frames, clear PRO and online/playing status, and a clear invite button. In-game friends cannot be invited into a second match through that button. Existing backend PRO/friend/block/invitation restrictions remain.
- Kuşatma chat: explicit-tap good-luck / nice-move / congratulations reactions through the existing chat sender; player green and rival red bubble tints. Existing purchased VIP emoji row remains.

No score/rule/XP/turn-time/backend schema changes, new AI connection, new branch or main merge. Visual/emulator validation remains deferred at the user's instruction; report compilation/tests only from an actual CI result.
