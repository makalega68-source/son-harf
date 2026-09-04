from pathlib import Path


def replace_one(path: str, old: str, new: str, label: str):
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected 1 anchor, found {count}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")
    print("repaired", label)

light = "app/src/main/java/com/sonharf/game/LightDuelUi.kt"
siege = "app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt"

replace_one(
    light,
    'Text(sh("SON KABUL EDİLEN KELİME", "LAST ACCEPTED WORD"), color = LMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)',
    'Text(sh("SON KABUL EDİLEN KELİME", "LAST ACCEPTED WORD"), color = LMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)',
    "classic 12sp typography marker on last-word label",
)
replace_one(
    light,
    'Text(name, color = LText, fontSize = 13.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)',
    'Text(name, color = LText, fontSize = 15.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)',
    "classic 15sp player name",
)
replace_one(
    light,
    'Text(score.toString(), color = accent, fontSize = duelScoreFontSize(score).coerceAtMost(23).sp, lineHeight = 25.sp, fontWeight = FontWeight.Black, maxLines = 1)',
    'Text(score.toString(), color = accent, fontSize = duelScoreFontSize(score).sp, lineHeight = 25.sp, fontWeight = FontWeight.Black, maxLines = 1)',
    "preserve score sizing contract",
)
replace_one(
    siege,
    '''            val size = 50.dp
            if (isBot) SyntheticBotPortrait(name, gender ?: botGenderForName(name), size, if (compact) 54.dp else 60.dp, accent)
            else ProfilePhotoAvatarRectWithGender(avatarPath.takeIf { avatarVisible }, gender, name, size, if (compact) 54.dp else 60.dp, accent)
''',
    '''            val size = 50.dp
            if (isBot) {
                SyntheticBotPortrait(name, gender ?: botGenderForName(name), size, if (compact) 54.dp else 60.dp, accent)
            } else if (!avatarVisible) {
                ProfilePhotoAvatarWithGender(
                    avatarPath = avatarPath,
                    gender = gender,
                    name = name,
                    size = size,
                    accent = accent,
                    visible = false,
                )
            } else {
                ProfilePhotoAvatarRectWithGender(avatarPath, gender, name, size, if (compact) 54.dp else 60.dp, accent)
            }
''',
    "preserve hidden-avatar shared-renderer contract while visible photos stay rectangular",
)
