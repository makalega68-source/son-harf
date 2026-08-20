# Son Harf — Full Neon Integration Checklist

Goal: keep every working feature/fix accumulated through v0.8.6 while replacing the visible application with the approved dark neon mockup. No legacy light screens may appear in the normal navigation flow.

## Non-negotiable visual target
- Dark navy/black background throughout; no legacy pale-blue or white full-screen surfaces.
- Mockup structure: Home, Play/Arena, Private Room, Shop, Profile, League, Player Center.
- Responsive layouts using system-bar/window insets; no clipped headings, shifted cards, overlapping status bar, or bottom-nav collisions.
- Neon cyan / violet / magenta / gold accents; consistent rounded panels and borders.
- Home hero must visually match the approved SON HARF neon logo direction rather than plain text on an empty card.
- Arena must use the mockup HUD: two player panels, center countdown, round label, word prompt/card, word chain, neon input, utility actions, dark Turkish keyboard treatment.
- Private room must use mockup card layout: room name, mode, rounds, invite code, create/invite actions.
- Shop, profile and league must use the approved mockup layouts, not old production composables.

## Existing functionality that must be preserved and reconnected
- Verified membership/auth gate; remember-login preference and sign-out behavior.
- Registration identity persistence and one-time identity rules.
- Registration form keyboard/scroll fixes.
- Profile display name and avatar/photo behavior.
- Profile photo privacy controls and achievements.
- Social avatars on home, arena, rankings/podium and profile where data exists.
- Friend requests, friend list, game invites, private-room invites.
- Friend-only direct chat and quick friend chat access.
- Match chat, moderation controls, block/report/photo-access logic.
- Random matchmaking, cancel matchmaking, resume active room.
- Private room create/join by code.
- Easy/normal/hard bot functionality where available.
- Normal/expert game-mode state and overlays where applicable.
- Word validation errors, turn timeout, dictionary/duplicate/wrong-letter feedback.
- Trivia rounds and existing game-state transitions.
- Rematch / bot restart / forfeit / reconnect-heartbeat behavior.
- Single clean match-result summary with reliable dismiss X; dismissed old matches must not resurface.
- Match result sharing and challenge sharing.
- Winner fireworks, combo/streak celebration and quick reactions.
- Career / XP / levels / win streak / statistics.
- Daily reward and daily challenge.
- Achievements.
- Weekly goals and league/player-center hub.
- Analytics events already wired into gameplay/social flows.
- Economy / diamonds / cosmetics runtime.
- Google Play one-time products and production billing UI/logic.
- VIP purchase flow, server verification and duplicate-purchase warning.
- Account deletion.
- Accessibility typography/readability fixes without breaking target geometry.

## Integration rules
1. Do not delete backend/data/RPC behavior just to simplify the UI.
2. New neon screens call existing backend/service methods; avoid duplicate business logic.
3. Legacy composables may remain in source for fallback/reference but must not be reached in primary navigation.
4. Mount global overlays above the new UI only if they do not cause duplicate result dialogs or visual collisions.
5. Preserve state restoration/resume logic.
6. Test small and large Android aspect ratios using adaptive spacing/scrolling; avoid fixed heights that overflow.
7. Build only after source audit and route audit are complete.
8. Do not merge until Android CI is green.
9. Produce APK only after final CI and route/function checklist pass.
