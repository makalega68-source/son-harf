from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if new in text:
        return
    if old not in text:
        raise SystemExit(f"Expected integration anchor not found in {path}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


store = ROOT / "app/src/main/java/com/sonharf/game/MonsterStyleStoreScreen.kt"
replace_once(
    store,
    '            item { PreviewRow(profileItems) }',
    '            item { PurchasedProfileFramesStoreRow() }',
)

profile = ROOT / "app/src/main/java/com/sonharf/game/MainPlayerProfileScreen.kt"
replace_once(
    profile,
    '        val friendsTask = async { runCatching { backend.getFriends() }.getOrDefault(emptyList()) }\n',
    '        val friendsTask = async { runCatching { backend.getFriends() }.getOrDefault(emptyList()) }\n'
    '        val cosmeticsTask = async { runCatching { backend.getEquippedCosmetics() }.getOrNull() }\n',
)
replace_once(
    profile,
    '        friends = friendsTask.await()\n        loading = false',
    '        friends = friendsTask.await()\n        SonHarfCosmetics.apply(cosmeticsTask.await())\n        loading = false',
)
replace_once(
    profile,
    '''                        ProfilePhotoAvatarWithGender(\n                            avatarPath = p?.avatarPath,\n                            gender = p?.gender,\n                            name = p?.displayName ?: sh("Oyuncu", "Player"),\n                            size = 106.dp,\n                            accent = if (p?.isVip == true) MainUi.Gold else MainUi.Blue,\n                        )''',
    '''                        Box(contentAlignment = Alignment.Center) {\n                            ProfilePhotoAvatarWithGender(\n                                avatarPath = p?.avatarPath,\n                                gender = p?.gender,\n                                name = p?.displayName ?: sh("Oyuncu", "Player"),\n                                size = 106.dp,\n                                accent = if (p?.isVip == true) MainUi.Gold else MainUi.Blue,\n                            )\n                            PurchasedProfileFrameOverlay(\n                                frameId = SonHarfCosmetics.profileFrameId,\n                                modifier = Modifier.size(126.dp),\n                            )\n                        }''',
)

print("Purchased Style assets integrated into store and profile.")
