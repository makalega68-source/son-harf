from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(path: str, old: str, new: str) -> None:
    target = ROOT / path
    text = target.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected exactly one match in {path!r}, found {count}: {old!r}")
    target.write_text(text.replace(old, new, 1), encoding="utf-8")


# Black Theme must never be activated from equipped/cache state alone.
# Pass the server-authoritative ownership set whenever cosmetics are applied.
replace_once(
    "app/src/main/java/com/sonharf/game/ProfileOwnedThemesSection.kt",
    "SonHarfCosmetics.applyAndPersist(context, nextEquipped)",
    "SonHarfCosmetics.applyAndPersist(context, nextEquipped, nextOwned)",
)
replace_once(
    "app/src/main/java/com/sonharf/game/ProfileOwnedThemesSection.kt",
    "SonHarfCosmetics.applyAndPersist(context, next)",
    "SonHarfCosmetics.applyAndPersist(context, next, owned)",
)

profile_path = ROOT / "app/src/main/java/com/sonharf/game/ProfileExperienceV2.kt"
profile = profile_path.read_text(encoding="utf-8")

import_anchor = "import com.sonharf.game.data.getPersonalRecords\n"
import_line = "import com.sonharf.game.data.getVipEntitlements\n"
if import_line not in profile:
    if import_anchor not in profile:
        raise RuntimeError("ProfileExperienceV2 import anchor missing")
    profile = profile.replace(import_anchor, import_anchor + import_line, 1)

state_anchor = "    var profile by remember { mutableStateOf<ProfileV2Dto?>(null) }\n"
state_line = "    var proActive by remember { mutableStateOf(false) }\n"
if state_line not in profile:
    if state_anchor not in profile:
        raise RuntimeError("ProfileExperienceV2 state anchor missing")
    profile = profile.replace(state_anchor, state_anchor + state_line, 1)

refresh_anchor = "        profile = runCatching { loadProfileV2() }.getOrNull()\n"
refresh_line = "        proActive = runCatching { backend?.let { backend.getVipEntitlements().isPro } ?: false }.getOrDefault(false)\n"
if refresh_line not in profile:
    if refresh_anchor not in profile:
        raise RuntimeError("ProfileExperienceV2 refresh anchor missing")
    profile = profile.replace(refresh_anchor, refresh_anchor + refresh_line, 1)

profile = profile.replace(
    'if (p?.isVip == true) sh("SON HARF VIP", "SON HARF VIP") else sh("SON HARF OYUNCUSU", "SON HARF PLAYER")',
    'if (proActive) sh("SON HARF PRO", "SON HARF PRO") else sh("SON HARF OYUNCUSU", "SON HARF PLAYER")',
    1,
)
profile = profile.replace(
    'color = if (p?.isVip == true) SonHarfGold else SonHarfMuted,',
    'color = if (proActive) SonHarfGold else SonHarfMuted,',
    1,
)

required = [
    "backend.getVipEntitlements().isPro",
    "if (proActive) sh(\"SON HARF PRO\"",
    "color = if (proActive) SonHarfGold else SonHarfMuted",
]
for marker in required:
    if marker not in profile:
        raise RuntimeError(f"ProfileExperienceV2 marker missing after patch: {marker}")

profile_path.write_text(profile, encoding="utf-8")
print("V2 premium security follow-up patch applied")
