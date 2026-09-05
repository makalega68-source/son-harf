from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
DUEL = ROOT / "app/src/main/java/com/sonharf/game/LightDuelUi.kt"
SIEGE_SCREEN = ROOT / "app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt"
SIEGE_VIEWPORT = ROOT / "app/src/main/java/com/sonharf/game/WordSiegeBoardViewport.kt"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


def regex_once(text: str, pattern: str, replacement: str, label: str) -> str:
    out, count = re.subn(pattern, replacement, text, count=1, flags=re.S)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return out


def patch_duel() -> None:
    text = DUEL.read_text(encoding="utf-8")

    text = replace_once(
        text,
        '''                    ) {
                        Text(seconds.toString(), color = LText, fontSize = 31.sp, fontWeight = FontWeight.Black)
                        Text(if (quizActive) "BONUS" else sh("SN", "SEC"), color = timerColor, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }''',
        '''                    ) {
                        if (timerSynchronizing && !quizActive) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = timerColor,
                                strokeWidth = 3.dp,
                            )
                            Text(sh("SENKR.", "SYNC"), color = timerColor, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        } else {
                            Text(seconds.toString(), color = LText, fontSize = 31.sp, fontWeight = FontWeight.Black)
                            Text(if (quizActive) "BONUS" else sh("SN", "SEC"), color = timerColor, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }''',
        "timer loader",
    )

    text = replace_once(
        text,
        '''                    Text(
                        when {
                            quizActive -> sh("BONUS DÜELLOSU", "BONUS DUEL")
                            timerSynchronizing && !quizActive -> sh("Senkronize ediliyor…", "Synchronizing…")
                            myTurn -> sh("● SIRA SENDE", "● YOUR TURN")
                            room.isBot && room.botTurn -> sh("● BOT DÜŞÜNÜYOR", "● BOT THINKING")
                            else -> sh("● RAKİBİN SIRASI", "● OPPONENT TURN")
                        },
                        color = if (myTurn) LBlue else if (quizActive) LPurple else LRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                    )''',
        '''                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (timerSynchronizing && !quizActive) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = LBlue,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            when {
                                quizActive -> sh("BONUS DÜELLOSU", "BONUS DUEL")
                                timerSynchronizing && !quizActive -> sh("Senkronize ediliyor…", "Synchronizing…")
                                myTurn -> sh("● SIRA SENDE", "● YOUR TURN")
                                room.isBot && room.botTurn -> sh("● BOT DÜŞÜNÜYOR", "● BOT THINKING")
                                else -> sh("● RAKİBİN SIRASI", "● OPPONENT TURN")
                            },
                            color = if (timerSynchronizing && !quizActive) LBlue else if (myTurn) LBlue else if (quizActive) LPurple else LRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }''',
        "sync status loader",
    )

    text = replace_once(text, "Modifier.fillMaxSize().padding(horizontal = 15.dp, vertical = 10.dp)", "Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp)", "game-card padding")
    text = replace_once(text, "color = LMuted,\n                            fontSize = 10.sp,\n                            fontWeight = FontWeight.Bold,", "color = LText,\n                            fontSize = 15.sp,\n                            lineHeight = 18.sp,\n                            fontWeight = FontWeight.Black,", "last accepted word emphasis")
    spacer = "Spacer(Modifier.weight(.12f))"
    if text.count(spacer) != 2:
        raise RuntimeError(f"game-card spacers: expected exactly two matches, found {text.count(spacer)}")
    text = text.replace(spacer, "Spacer(Modifier.weight(.06f))", 2)
    text = replace_once(text, "fontSize = 78.sp,\n                        lineHeight = 80.sp,", "fontSize = 62.sp,\n                        lineHeight = 64.sp,", "mandatory letter size")
    text = replace_once(text, "fontSize = 13.sp,\n                        fontWeight = FontWeight.Black,", "fontSize = 17.sp,\n                        lineHeight = 20.sp,\n                        fontWeight = FontWeight.Black,\n                        maxLines = 1,", "mandatory-letter guidance")
    text = replace_once(
        text,
        '''                Text("$league • $rating", color = LMuted, fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)''',
        '''                Text("$league • $rating", color = LMuted, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)''',
        "league-rating line",
    )

    replacement_input = r'''@Composable
private fun CompetitiveInputBar(
    value: String,
    required: String,
    inputMatches: Boolean?,
    feedbackWord: String?,
    feedbackCorrect: Boolean?,
    notice: String,
    myTurn: Boolean,
    busy: Boolean,
    quiz: Boolean,
    voiceSupported: Boolean,
    voiceUses: Int,
    onVoice: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusColor = when {
        feedbackWord != null && feedbackCorrect == true -> LGreen
        inputMatches == false || feedbackCorrect == false -> LRed
        value.isNotBlank() -> LGreen
        else -> LMuted
    }
    val voiceEnabled = myTurn && !busy && !quiz && voiceSupported && voiceUses < 5
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(2.dp, if (myTurn && !quiz) statusColor.copy(alpha = .65f) else LBorder),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(48.dp).clickable(enabled = voiceEnabled, onClick = onVoice),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.size(30.dp),
                    shape = CircleShape,
                    color = if (voiceEnabled) LBlueSoft else LCard2,
                    border = BorderStroke(1.dp, if (voiceEnabled) LBlue.copy(alpha = .45f) else LBorder),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🎙", fontSize = 14.sp)
                        Text(
                            (5 - voiceUses).coerceAtLeast(0).toString(),
                            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 2.dp, bottom = 1.dp),
                            color = if (voiceEnabled) LBlue else LMuted,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
            Spacer(Modifier.width(4.dp))
            Text(
                value,
                color = LText,
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun GameKeyboard'''
    text = regex_once(
        text,
        r'@Composable\nprivate fun CompetitiveInputBar\(.*?\n\}\n\n@Composable\nprivate fun GameKeyboard',
        replacement_input,
        "competitive input bar",
    )

    text = replace_once(text, "modifier = modifier.height(37.dp),", "modifier = modifier.height(48.dp),", "action touch target")

    # Signature is intentionally retained for compatibility; submit is owned by the large keyboard button.
    if "onSubmit: () -> Unit," not in text:
        raise RuntimeError("input callback signature unexpectedly missing")

    DUEL.write_text(text, encoding="utf-8")


def patch_siege() -> None:
    viewport = SIEGE_VIEWPORT.read_text(encoding="utf-8")
    viewport = replace_once(viewport, "internal const val WORD_SIEGE_PRACTICE_CLOSE_SCALE = 0.86f", "internal const val WORD_SIEGE_PRACTICE_CLOSE_SCALE = 0.80f", "siege initial scale")
    SIEGE_VIEWPORT.write_text(viewport, encoding="utf-8")

    text = SIEGE_SCREEN.read_text(encoding="utf-8")
    text = replace_once(
        text,
        '''                    notice = sh("Ana sözlük hazır. Çevrimdışı alıştırmada da aynı sözlük kullanılacak.", "Main dictionary ready. The same dictionary will be used for offline practice.")''',
        '''                    notice = null''',
        "remove dictionary-ready info box",
    )
    text = replace_once(
        text,
        '''        modifier = modifier,
        color = if (active) accent.copy(alpha = .09f) else MainUi.Surface,''',
        '''        modifier = modifier.height(if (compact) 78.dp else 86.dp),
        color = if (active) accent.copy(alpha = .09f) else MainUi.Surface,''',
        "equal siege score-card height",
    )
    text = replace_once(
        text,
        '''            ProfilePhotoAvatarWithGender(
                avatarPath = avatarPath,
                gender = gender,
                name = name,
                size = 50.dp,
                accent = accent,
                visible = avatarVisible,
            )''',
        '''            if (isBot) {
                SyntheticBotPortrait(
                    name = name,
                    gender = gender ?: botGenderForName(name),
                    width = 42.dp,
                    height = 56.dp,
                    accent = accent,
                )
            } else if (avatarVisible) {
                ProfilePhotoAvatarRectWithGender(
                    avatarPath = avatarPath,
                    gender = gender,
                    name = name,
                    width = 42.dp,
                    height = 56.dp,
                    accent = accent,
                )
            } else {
                ProfilePhotoAvatarWithGender(
                    avatarPath = avatarPath,
                    gender = gender,
                    name = name,
                    size = 44.dp,
                    accent = accent,
                    visible = false,
                )
            }''',
        "rectangular siege avatar",
    )
    SIEGE_SCREEN.write_text(text, encoding="utf-8")


def verify() -> None:
    duel = DUEL.read_text(encoding="utf-8")
    siege = SIEGE_SCREEN.read_text(encoding="utf-8")
    viewport = SIEGE_VIEWPORT.read_text(encoding="utf-8")
    required = {
        "mandatory letter 62sp": "fontSize = 62.sp" in duel,
        "guidance 17sp": "fontSize = 17.sp" in duel,
        "last word 15sp": "fontSize = 15.sp" in duel,
        "sync loader": "Senkronize ediliyor…" in duel and "CircularProgressIndicator" in duel,
        "no small input send": "CompetitiveInputBar" in duel and 'Text("➤"' not in duel,
        "no input placeholder": "Kelimenizi yazın…" not in duel and "Type your word…" not in duel,
        "keyboard send preserved": 'sh("GÖNDER", "SEND")' in duel,
        "turkish keyboard preserved": 'listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "Ğ", "Ü")' in duel,
        "action 48dp": "modifier = modifier.height(48.dp)" in duel,
        "siege scale": "WORD_SIEGE_PRACTICE_CLOSE_SCALE = 0.80f" in viewport,
        "dictionary success notice removed": "Ana sözlük hazır. Çevrimdışı alıştırmada da aynı sözlük kullanılacak." not in siege,
        "siege rectangular avatar": "ProfilePhotoAvatarRectWithGender(" in siege,
    }
    failed = [name for name, ok in required.items() if not ok]
    if failed:
        raise RuntimeError("verification failed: " + ", ".join(failed))
    print("ACTIVE_SCREEN_UI_PATCH_VERIFY_PASS")


if __name__ == "__main__":
    patch_duel()
    patch_siege()
    verify()
