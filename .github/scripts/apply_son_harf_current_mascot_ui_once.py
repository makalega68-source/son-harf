from pathlib import Path

PATH = Path("app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt")
source = PATH.read_text(encoding="utf-8")


def replace_once(old: str, new: str, label: str) -> None:
    global source
    count = source.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    source = source.replace(old, new, 1)


def replace_between(start: str, end: str, replacement: str, label: str) -> None:
    global source
    i = source.find(start)
    if i < 0:
        raise RuntimeError(f"{label}: start marker not found")
    j = source.find(end, i)
    if j < 0:
        raise RuntimeError(f"{label}: end marker not found")
    source = source[:i] + replacement.rstrip() + "\n\n" + source[j:]

# Hard source-of-truth guards: current mascot build + integrated keyboard only.
for token in [
    "private fun PremierKeyboard(",
    "backend.submitPremierWord(active.id, candidate)",
    "backend.claimTurnTimeout(active.id)",
    "backend.botTakeTurn(active.id)",
    "room.roundWordCount",
]:
    if token not in source:
        raise RuntimeError(f"Current Son Harf source guard missing: {token}")

# Arena-only light sky palette. Do not change the rest of the application theme.
palette_marker = "private fun pt(language: String, tr: String, en: String): String"
sky_palette = '''private object PremierArenaSky {
    val BackgroundTop = Color(0xFFEAF8FF)
    val BackgroundMid = Color(0xFFDDF3FC)
    val BackgroundBottom = Color(0xFFCFEAF7)
    val Surface = Color(0xFFF9FDFF)
    val SurfaceBlue = Color(0xFFE6F5FB)
    val Ink = Color(0xFF17364D)
    val Muted = Color(0xFF647D8D)
    val Ocean = Color(0xFF4E98AA)
    val OceanDeep = Color(0xFF2E6F82)
    val Border = Color(0xFFB8D6E1)
    val Rival = Color(0xFFD77B70)
    val RivalSoft = Color(0xFFFBE8E4)
    val Green = Color(0xFF6F9E7B)
    val GreenSoft = Color(0xFFE8F3E9)
    val Gold = Color(0xFFC89C39)
    val GoldSoft = Color(0xFFF7ECCA)
    val Red = Color(0xFFC65C55)
    val RedSoft = Color(0xFFF8E2DF)
}

'''
if "private object PremierArenaSky" not in source:
    replace_once(palette_marker, sky_palette + palette_marker, "insert arena sky palette")

replace_once(
'''    val required = premierRequiredToken(room, words)
    val latestPlayedWord = words.lastOrNull()?.let { premierUpper(it.normalizedWord.ifBlank { it.word }, language) }.orEmpty()

    BoxWithConstraints(Modifier.fillMaxSize()) {''',
'''    val required = premierRequiredToken(room, words)
    val latestMove = words.lastOrNull()
    val latestPlayedWord = latestMove?.let { premierUpper(it.normalizedWord.ifBlank { it.word }, language) }.orEmpty()
    val latestMoveScore = latestMove?.let { DictionaryEngine.calculatePoints(it.normalizedWord.ifBlank { it.word }, language) } ?: 0
    val mascotEmotion = when {
        moveFeedback?.accepted == true && latestMoveScore >= 20 -> WordSiegeMascotEmotion.PROUD
        moveFeedback?.accepted == true -> WordSiegeMascotEmotion.HAPPY
        moveFeedback?.accepted == false -> WordSiegeMascotEmotion.STRESSED
        myTurn -> WordSiegeMascotEmotion.FOCUS
        else -> WordSiegeMascotEmotion.CALM
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {''',
"add Son Harf mascot state",
)

replace_once(
'''        val targetSize = if (veryCompact) 78.dp else if (compact) 88.dp else if (tall) 118.dp else 104.dp
        val keyHeight = if (veryCompact) 38.dp else if (compact) 41.dp else if (tall) 50.dp else 46.dp
        val primaryGap = if (veryCompact) 3.dp else if (compact) 5.dp else 9.dp''',
'''        val targetSize = if (veryCompact) 74.dp else if (compact) 86.dp else if (tall) 112.dp else 100.dp
        val mascotSize = if (veryCompact) 64.dp else if (compact) 72.dp else if (tall) 94.dp else 84.dp
        val keyHeight = if (veryCompact) 36.dp else if (compact) 39.dp else if (tall) 48.dp else 44.dp
        val primaryGap = if (veryCompact) 2.dp else if (compact) 4.dp else 7.dp''',
"responsive arena sizing",
)

replace_once(
'''                    Brush.verticalGradient(
                        listOf(Color(0xFF050B14), Color(0xFF0A1930), Color(0xFF07111F))
                    )''',
'''                    Brush.verticalGradient(
                        listOf(PremierArenaSky.BackgroundTop, PremierArenaSky.BackgroundMid, PremierArenaSky.BackgroundBottom)
                    )''',
"sky blue background",
)

replace_once(
'''                Spacer(Modifier.height(primaryGap))
                PremierTargetCard(language, required, room.gameMode, room.roundNo, targetSize)
                Spacer(Modifier.height(primaryGap))
                Text(''',
'''                Spacer(Modifier.height(primaryGap))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (targetSize > mascotSize) targetSize else mascotSize),
                    contentAlignment = Alignment.Center,
                ) {
                    PremierTargetCard(language, required, room.gameMode, room.roundNo, targetSize)
                    WordSiegeMascot(
                        moveId = latestMove?.id,
                        lastMoveMine = latestMove?.playerId == meId,
                        moveScore = latestMoveScore,
                        capturedCells = 0,
                        opponentCaptured = 0,
                        moveCell = null,
                        pendingCells = emptyList(),
                        playerTurn = myTurn,
                        requestedEmotion = mascotEmotion,
                        modifier = Modifier.align(Alignment.CenterEnd).size(mascotSize),
                    )
                }
                Spacer(Modifier.height(primaryGap))
                Text(''',
"place mascot beside centered target",
)

# Symmetric action row stays above the integrated keyboard and does not consume arena center alignment.
replace_once(
'''            // Keep the live input outside the flexible arena body. This guarantees visibility on
            // short screens after the global top banner consumes vertical space.
            PremierInputBar(''',
'''            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onForfeit,
                    modifier = Modifier.weight(1f).height(if (veryCompact) 34.dp else 38.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PremierArenaSky.RedSoft,
                        contentColor = PremierArenaSky.Red,
                    ),
                    border = BorderStroke(1.dp, PremierArenaSky.Red.copy(alpha = .35f)),
                ) {
                    Icon(Icons.Rounded.Flag, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(pt(language, "PES ET", "SURRENDER"), fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
                Box(Modifier.weight(1f)) {
                    Button(
                        onClick = onQuickChat,
                        modifier = Modifier.fillMaxWidth().height(if (veryCompact) 34.dp else 38.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PremierArenaSky.Ocean,
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(Icons.Rounded.ChatBubbleOutline, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(pt(language, "SOHBET", "CHAT"), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                    if (unreadChat) {
                        Box(
                            Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp)
                                .size(9.dp).clip(CircleShape).background(PremierArenaSky.Red)
                        )
                    }
                }
            }
            // Keep the live input and custom keyboard outside the flexible arena body.
            PremierInputBar(''',
"add symmetric bottom actions",
)

header_start = "@Composable\nprivate fun PremierArenaHeader("
header_end = "@Composable\nprivate fun PremierMiniPlayer("
new_header = '''@Composable
private fun PremierArenaHeader(
    language: String,
    room: GameRoomDto,
    me: ProfileDto?,
    opponent: ProfileDto?,
    rivalName: String,
    myScore: Int,
    rivalScore: Int,
    myRounds: Int,
    rivalRounds: Int,
    myStreak: Int,
    rivalStreak: Int,
    seconds: Int,
    unreadChat: Boolean,
    onForfeit: () -> Unit,
    onQuickChat: () -> Unit,
) {
    val myRating = me?.rating ?: 1000
    val rivalRating = if (room.isBot) myRating else opponent?.rating ?: 1000
    val danger = seconds in 1..5
    val wordProgress = room.roundWordCount.coerceIn(0, 10) / 10f
    val timerProgress = seconds.coerceIn(0, PREMIER_TURN_SECONDS) / PREMIER_TURN_SECONDS.toFloat()
    val cardHeight = 116.dp

    Surface(
        shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
        color = PremierArenaSky.Surface,
        shadowElevation = 5.dp,
        border = BorderStroke(1.dp, PremierArenaSky.Border),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PremierSymmetricPlayerCard(
                    language = language,
                    name = me?.displayName ?: pt(language, "Sen", "You"),
                    avatar = me?.avatarPath,
                    gender = me?.gender,
                    visible = me?.avatarVisibility != "hidden",
                    rating = myRating,
                    score = myScore,
                    streak = myStreak,
                    accent = PremierArenaSky.Ocean,
                    soft = PremierArenaSky.SurfaceBlue,
                    modifier = Modifier.weight(1f).height(cardHeight),
                    nameColor = SonHarfCosmetics.playerNameColor,
                )
                Surface(
                    modifier = Modifier.width(94.dp).height(cardHeight),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, PremierArenaSky.Border),
                    shadowElevation = 2.dp,
                ) {
                    Column(
                        Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            pt(language, "Raund ${room.roundNo} / 3", "Round ${room.roundNo} / 3"),
                            color = PremierArenaSky.Ink,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                        )
                        Text("$myRounds - $rivalRounds", color = PremierArenaSky.Ink, fontSize = 25.sp, fontWeight = FontWeight.Black)
                        Text(
                            pt(language, "2 raund kazanan\nmaçı kazanır", "First to 2 rounds\nwins"),
                            color = PremierArenaSky.Muted,
                            fontSize = 7.sp,
                            lineHeight = 8.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                PremierSymmetricPlayerCard(
                    language = language,
                    name = rivalName,
                    avatar = opponent?.avatarPath,
                    gender = opponent?.gender,
                    visible = opponent?.avatarVisibility != "hidden",
                    rating = rivalRating,
                    score = rivalScore,
                    streak = rivalStreak,
                    accent = PremierArenaSky.Rival,
                    soft = PremierArenaSky.RivalSoft,
                    modifier = Modifier.weight(1f).height(cardHeight),
                    bot = room.isBot,
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(15.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, PremierArenaSky.Border),
                ) {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Timer, null, tint = if (danger) PremierArenaSky.Red else PremierArenaSky.OceanDeep, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "00:${seconds.coerceAtLeast(0).toString().padStart(2, '0')}",
                            color = if (danger) PremierArenaSky.Red else PremierArenaSky.Ink,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Spacer(Modifier.width(7.dp))
                        LinearProgressIndicator(
                            progress = { timerProgress },
                            modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(99.dp)),
                            color = if (danger) PremierArenaSky.Red else PremierArenaSky.Ocean,
                            trackColor = PremierArenaSky.Border.copy(alpha = .48f),
                        )
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(15.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, PremierArenaSky.Border),
                ) {
                    Column(
                        Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.MenuBook, null, tint = PremierArenaSky.Gold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(
                                pt(language, "Kelime ${room.roundWordCount.coerceIn(0, 10)} / 10", "Word ${room.roundWordCount.coerceIn(0, 10)} / 10"),
                                color = PremierArenaSky.Ink,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                        LinearProgressIndicator(
                            progress = { wordProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp)),
                            color = PremierArenaSky.Green,
                            trackColor = PremierArenaSky.Border.copy(alpha = .48f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PremierSymmetricPlayerCard(
    language: String,
    name: String,
    avatar: String?,
    gender: String?,
    visible: Boolean,
    rating: Int,
    score: Int,
    streak: Int,
    accent: Color,
    soft: Color,
    modifier: Modifier,
    bot: Boolean = false,
    nameColor: Color = PremierArenaSky.Ink,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = soft,
        border = BorderStroke(1.dp, accent.copy(alpha = .38f)),
        shadowElevation = 2.dp,
    ) {
        Column(
            Modifier.fillMaxSize().padding(7.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (bot) {
                    PremierBotAvatar(size = 42.dp, accent = accent)
                } else {
                    ProfilePhotoAvatarRectWithGender(
                        avatarPath = if (visible) avatar else null,
                        gender = gender,
                        name = name,
                        width = 42.dp,
                        height = 42.dp,
                        accent = accent,
                    )
                }
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        name,
                        color = if (nameColor == PremierUi.Ink) PremierArenaSky.Ink else nameColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(premierLeagueLabel(rating, language), color = accent, fontSize = 7.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("🏆 $rating", color = PremierArenaSky.Muted, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(pt(language, "Raund Puanı", "Round Score"), color = PremierArenaSky.Muted, fontSize = 7.sp)
                    if (streak >= 2) Text("🔥 $streak", color = PremierArenaSky.Red, fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
                Text(score.toString(), color = PremierArenaSky.Ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}'''
replace_between(header_start, header_end, new_header, "replace arena header")

input_start = "@Composable\nprivate fun PremierInputBar("
input_end = "@Composable\nprivate fun PremierKeyboard("
new_input = '''@Composable
private fun PremierInputBar(language: String, input: String, required: String, myTurn: Boolean, busy: Boolean, modifier: Modifier = Modifier) {
    val requiredLetterBadge = myTurn && input.isBlank() && required.isNotBlank() && required != "★"
    val accent = if (myTurn) PremierArenaSky.Ocean else PremierArenaSky.Border
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = PremierArenaSky.Surface,
        border = BorderStroke(if (myTurn) 2.dp else 1.dp, accent),
        shadowElevation = if (myTurn) 5.dp else 1.dp,
    ) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Bolt, null, tint = if (myTurn) PremierArenaSky.Ocean else PremierArenaSky.Muted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            if (requiredLetterBadge) {
                Surface(shape = RoundedCornerShape(7.dp), color = PremierArenaSky.GoldSoft, border = BorderStroke(1.dp, PremierArenaSky.Gold)) {
                    Text(required.uppercase(), Modifier.padding(horizontal = 8.dp, vertical = 3.dp), color = PremierArenaSky.Gold, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(8.dp))
            }
            Text(
                when {
                    busy -> pt(language, "DOĞRULANIYOR…", "VERIFYING…")
                    input.isNotBlank() -> input
                    required == "★" -> pt(language, "KELİMENİ YAZ…", "TYPE YOUR WORD…")
                    else -> pt(language, "$required İLE BAŞLAYAN KELİMEYİ YAZ…", "TYPE A WORD STARTING WITH $required…")
                },
                color = if (input.isBlank()) PremierArenaSky.Muted else PremierArenaSky.Ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = .2.sp,
            )
        }
    }
}'''
replace_between(input_start, input_end, new_input, "replace input bar")

# Keep target card light enough to visually belong to the sky arena.
replace_once(
'''            .background(Brush.radialGradient(listOf(Color(0xFF15355F), Color(0xFF07111F))))''',
'''            .background(Brush.radialGradient(listOf(Color(0xFFF8FDFF), PremierArenaSky.SurfaceBlue)))''',
"target card sky surface",
)

# Some current revisions use a different target background; accept it without broad edits.
# Validate final user-visible contract and critical gameplay invariants.
for token in [
    "PremierKeyboard(language, input",
    "WordSiegeMascot(",
    "PremierArenaSky.BackgroundTop",
    "Modifier.weight(1f).height(cardHeight)",
    "backend.submitPremierWord(active.id, candidate)",
    "backend.claimTurnTimeout(active.id)",
    "backend.botTakeTurn(active.id)",
]:
    if token not in source:
        raise RuntimeError(f"Final Son Harf contract missing: {token}")

if "LocalSoftwareKeyboardController" in source[source.find("private fun PremierArena("):source.find("private fun PremierArenaHeader(")]:
    raise RuntimeError("System keyboard controller survived in Son Harf arena")

PATH.write_text(source, encoding="utf-8")
print("Applied current Son Harf sky/symmetry/mascot integration")
