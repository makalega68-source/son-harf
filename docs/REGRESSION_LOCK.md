# Son Harf Regression Lock

These behaviors are protected and must not be removed or silently redesigned by later feature work.

- Gameplay uses an always-visible in-app keyboard. The Android system keyboard must not cover the arena.
- Turkish gameplay keyboard rows include Ğ, Ü, Ş, İ, Ö, Ç; English gameplay uses the English key set.
- Match chat uses the in-app keyboard as well. Chat remains VIP-gated according to product rules.
- A failed word submission preserves the typed word and shows a retry/error message.
- Forfeit must end cleanly and return the player out of the arena; no blank/white/frozen finished screen.
- Room language is authoritative for gameplay and bot word selection. Turkish and English word pools must never cross.
- League and profile surfaces use shared SonHarf theme tokens; no hard-coded legacy dark gradients.
- Profile photo gender badge: female = pink ♀, male = blue ♂.
- Existing game/chat layouts may only be changed with an explicit regression check for all rules above.

Development rule: inspect -> surgical change -> compile/test -> regression check -> commit. Do not replace large working screens to implement an unrelated feature.
