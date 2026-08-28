# Lethara Release Audit — Stage 9

Status: CI PENDING

## Production path
- MainActivity renders GamePortalApp.
- GamePortalApp exposes ClassicPremiumApp as the active product shell.
- Active navigation does not expose legacy Eve screens.
- MascotCardPreview compatibility code now resolves through MascotLive3DStage and MascotSelectionRuntime.

## Android keyboard / chat
- MainActivity uses `windowSoftInputMode="adjustResize"`.
- Companion chat input uses Compose IME insets.
- Android IME action is Send and invokes the same validated chat send path as the on-screen button.
- Duel word input also uses Android IME Send.

## Mascot placement
- Active duel mascot is restricted to the dedicated 58×64 dp Seal-round header slot.
- It does not overlap the word history card, word text field, Send button, forfeit button or opponent/player controls.
- Result-screen mascot is isolated in the result summary.
- Companion and Seal Room use dedicated 3D presentation panels.

## Mascot ownership / roster
- Lyra / mascot_white is active at 0 SC and is granted to every profile by the profiles_grant_default_mascot trigger.
- Existing profiles have the default mascot inventory.
- Neris / mascot_chibi_wizard is active at 700 SC.
- Kael, Ryvan, Mivo and Selen shop rows remain inactive until distinct licensed 3D assets pass runtime validation.
- Server ensure_mascot_progress_v1 rejects unavailable mascots and rejects unowned paid mascots.
- Archive/Shop/Companion/Room resolve the active playable mascot through the canonical selected-Seal runtime.

## Fruit economy
Server catalog contract:
- Lethara Elması: +3 XP, free normal fruit.
- Ay Meyvesi: +10 XP, 20 SC.
- Yıldız Meyvesi: +20 XP, 45 SC.
- Mühür Meyvesi: +30 XP, 70 SC.

Database constraint `mascot_fruit_catalog_magic_xp_contract` enforces:
- normal fruit = +3 XP and 0 SC;
- magic fruit = +10/+20/+30 XP and positive SC price.

Server RPCs remain authoritative:
- buy_mascot_fruit_v1 validates authentication, catalog, magic-only purchase, balance and inventory mutation.
- feed_mascot_v1 validates authentication, ownership/daily limit and applies XP from the server catalog.
- normal fruit remains limited to 3 uses per day.
- fruit only changes mascot XP, memory/care state; it does not alter ranked-match power.

## Monetization fairness
Active shop kinds are cosmetic/presentation only: profile frame, name style, game theme, keyboard theme, victory effect, emoji pack and mascots.
Magic fruit accelerates companion/lore progression only.
No purchasable item changes duel score, timer, rating, word validity or ranked power.

## Final gates
- Unit tests: PENDING
- Android CI: PENDING
- Final APK: PENDING
- Post-merge Final Unified Validation: PENDING
- Post-merge Final Mascot Runtime (Lyra + Neris): PENDING
