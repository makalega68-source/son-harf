from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one match in {path}, found {count}: {old[:80]!r}")
    p.write_text(text.replace(old, new, 1))


p = Path("app/src/main/java/com/sonharf/game/ProfilePhotoRuntime.kt")
text = p.read_text()
start = text.index("internal fun ProfilePhotoAvatarWithGender(")
end = text.index("@Composable\ninternal fun ProfilePhotoAvatarRectWithGender(", start)
section = text[start:end]
section = section.replace(
    "    visible: Boolean = true,\n) {",
    "    visible: Boolean = true,\n    showGenderBadge: Boolean = true,\n) {",
    1,
)
section = section.replace(
    "        Box(Modifier.align(Alignment.BottomEnd)) {\n            FramelessGenderSymbol(gender, size)\n        }",
    "        if (showGenderBadge) {\n            Box(Modifier.align(Alignment.BottomEnd)) {\n                FramelessGenderSymbol(gender, size)\n            }\n        }",
    1,
)
text = text[:start] + section + text[end:]
start = text.index("internal fun ProfilePhotoAvatarRectWithGender(")
section = text[start:]
section = section.replace(
    "    accent: Color = SonHarfCyan,\n) {",
    "    accent: Color = SonHarfCyan,\n    showGenderBadge: Boolean = true,\n) {",
    1,
)
section = section.replace(
    "        Box(Modifier.align(Alignment.BottomEnd)) {\n            FramelessGenderSymbol(gender, height)\n        }",
    "        if (showGenderBadge) {\n            Box(Modifier.align(Alignment.BottomEnd)) {\n                FramelessGenderSymbol(gender, height)\n            }\n        }",
    1,
)
p.write_text(text[:start] + section)

replace_once(
    "app/src/main/java/com/sonharf/game/LightDuelUi.kt",
    "    playerGender: String?,\n    language: String,",
    "    playerGender: String?,\n    playerFrameId: String?,\n    language: String,",
)
replace_once(
    "app/src/main/java/com/sonharf/game/LightDuelUi.kt",
    "                    ProfilePhotoAvatarWithGender(playerAvatarPath, playerGender, playerName, 48.dp, LBlue)",
    """                    FramedProfilePhotoAvatar(
                        avatarPath = playerAvatarPath,
                        gender = playerGender,
                        name = playerName,
                        size = 48.dp,
                        frameId = playerFrameId,
                        accent = LBlue,
                        showGenderBadge = false,
                    )""",
)
replace_once(
    "app/src/main/java/com/sonharf/game/LightDuelUi.kt",
    "    playerGender: String?,\n    playerRating: Int,",
    "    playerGender: String?,\n    playerFrameId: String?,\n    playerRating: Int,",
)
replace_once(
    "app/src/main/java/com/sonharf/game/LightDuelUi.kt",
    "                    myTurn, LBlue, false, Modifier.weight(1f),\n                )",
    "                    myTurn, LBlue, false, Modifier.weight(1f), frameId = playerFrameId,\n                )",
)
replace_once(
    "app/src/main/java/com/sonharf/game/LightDuelUi.kt",
    "    bot: Boolean,\n    modifier: Modifier,\n) {",
    "    bot: Boolean,\n    modifier: Modifier,\n    frameId: String? = null,\n) {",
)
replace_once(
    "app/src/main/java/com/sonharf/game/LightDuelUi.kt",
    "            if (bot) SyntheticBotPortrait(name, gender ?: botGenderForName(name), 46.dp, 62.dp, accent)\n            else ProfilePhotoAvatarRectWithGender(avatarPath, gender, name, 46.dp, 62.dp, accent)",
    """            if (bot) {
                SyntheticBotPortrait(name, gender ?: botGenderForName(name), 46.dp, 62.dp, accent)
            } else if (!frameId.isNullOrBlank()) {
                FramedProfilePhotoAvatar(
                    avatarPath = avatarPath,
                    gender = gender,
                    name = name,
                    size = 46.dp,
                    frameId = frameId,
                    accent = accent,
                    showGenderBadge = false,
                )
            } else {
                ProfilePhotoAvatarRectWithGender(avatarPath, gender, name, 46.dp, 62.dp, accent, showGenderBadge = false)
            }""",
)

replace_once(
    "app/src/main/java/com/sonharf/game/OnlineGameScreenV6.kt",
    "playerGender = profile?.gender, language = language, matching = matching, notice = notice,",
    "playerGender = profile?.gender, playerFrameId = SonHarfCosmetics.profileFrameId, language = language, matching = matching, notice = notice,",
)
replace_once(
    "app/src/main/java/com/sonharf/game/OnlineGameScreenV6.kt",
    "playerGender = profile?.gender, playerRating = profile?.rating ?: 1000,",
    "playerGender = profile?.gender, playerFrameId = SonHarfCosmetics.profileFrameId, playerRating = profile?.rating ?: 1000,",
)

p = Path("app/src/main/java/com/sonharf/game/MainPlayerProfileScreen.kt")
text = p.read_text()
outer = text.index("                    Box(contentAlignment = Alignment.BottomEnd) {")
start = text.index("                        Box(contentAlignment = Alignment.Center) {", outer)
end = text.index("                        Surface(", start)
new = """                        FramedProfilePhotoAvatar(
                            avatarPath = p?.avatarPath,
                            gender = p?.gender,
                            name = p?.displayName ?: sh(\"Oyuncu\", \"Player\"),
                            size = 106.dp,
                            frameId = SonHarfCosmetics.profileFrameId,
                            accent = if (p?.isVip == true) MainUi.Gold else MainUi.Blue,
                            visible = p?.avatarVisibility != \"hidden\",
                            showGenderBadge = false,
                        )
"""
p.write_text(text[:start] + new + text[end:])

replace_once(
    "app/src/main/java/com/sonharf/game/MonsterExperienceApp.kt",
    """                Surface(modifier = Modifier.clickable(onClick = onProfile), shape = CircleShape, color = Color.White.copy(alpha = .17f)) {
                    Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.padding(10.dp).size(25.dp))
                }""",
    """                Box(modifier = Modifier.clickable(onClick = onProfile), contentAlignment = Alignment.Center) {
                    FramedProfilePhotoAvatar(
                        avatarPath = profile?.avatarPath,
                        gender = profile?.gender,
                        name = profile?.displayName ?: sh(\"OYUNCU\", \"PLAYER\"),
                        size = 44.dp,
                        frameId = SonHarfCosmetics.profileFrameId,
                        accent = Color.White,
                        visible = profile?.avatarVisibility != \"hidden\",
                        showGenderBadge = false,
                    )
                }""",
)

# Regression guards must follow the new stronger contract: real photos remain mandatory,
# and the live duel must also carry the equipped profile frame through lobby + arena.
replace_once(
    "app/src/test/java/com/sonharf/game/FinalRestorationRegressionTest.kt",
    '        assertTrue(duel.contains("ProfilePhotoAvatarWithGender"))',
    '        assertTrue(duel.contains("FramedProfilePhotoAvatar"))\n        assertTrue(duel.contains("playerFrameId"))',
)
replace_once(
    "app/src/test/java/com/sonharf/game/ProfileImageCoverageRegressionTest.kt",
    '        assertTrue(duel.contains("ProfilePhotoAvatarWithGender"))',
    '        assertTrue(duel.contains("FramedProfilePhotoAvatar"))\n        assertTrue(duel.contains("playerFrameId"))\n        assertTrue(duel.contains("ProfilePhotoAvatarRectWithGender"))',
)
