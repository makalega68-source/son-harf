from pathlib import Path

path = Path("app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt")
text = path.read_text(encoding="utf-8")


def replace_between(source: str, start: str, end: str, replacement: str) -> str:
    i = source.index(start)
    j = source.index(end, i)
    return source[:i] + replacement.rstrip() + "\n\n" + source[j:]


# Native Android IME support. The custom in-game keyboard is intentionally removed from the arena.
focus_import = "import androidx.compose.ui.focus.focusRequester\n"
if focus_import not in text:
    text = text.replace("import androidx.compose.ui.draw.shadow\n", "import androidx.compose.ui.draw.shadow\n" + focus_import)

palette = r'''private object PremierUi {
    // Calm, warm production palette for Son Harf. High-saturation colors are reserved for state/feedback.
    val Background = Color(0xFFF3EEE5)
    val Surface = Color(0xFFFFFBF4)
    val Ink = Color(0xFF173247)
    val Muted = Color(0xFF6F7B7C)
    val Ocean = Color(0xFF4F8F96)
    val OceanDeep = Color(0xFF2F6970)
    val Sky = Color(0xFF8EB7B5)
    val Ice = Color(0xFFE6EFEB)
    val Border = Color(0xFFD8D0C4)
    val Green = Color(0xFF789B73)
    val GreenSoft = Color(0xFFE5ECDD)
    val Red = Color(0xFFC86459)
    val RedSoft = Color(0xFFF4DDD7)
    val Gold = Color(0xFFD1A13E)
    val GoldSoft = Color(0xFFF5E8BB)
    val Rival = Color(0xFFD27869)
    val RivalSoft = Color(0xFFF6E2DC)
}'''
text = replace_between(text, "private object PremierUi {", "private fun pt", palette)

arena = r'''@Composable
private fun PremierArena(
    language: String,
    room: GameRoomDto,
    me: ProfileDto?,
    opponent: ProfileDto?,
    meId: String?,
    words: List<GameWordDto>,
    input: String,
    notice: String,
    busy: Boolean,
    turnSeconds: Int,
    unreadChat: Boolean,
    floatingMessage: ChatMessageDto?,
    moveFeedback: PremierMoveFeedback?,
    onInput: (String) -> Unit,
    onForfeit: () -> Unit,
    onQuickChat: () -> Unit,
    onSubmit: () -> Unit,
) {
    val isPro = me?.isVip == true
    val amHost = meId == room.hostId
    val myScore = if (amHost) room.hostScore else room.guestScore
    val rivalScore = if (amHost) room.guestScore else room.hostScore
    val myRounds = if (amHost) room.hostRounds else room.guestRounds
    val rivalRounds = if (amHost) room.guestRounds else room.hostRounds
    val myStreak = if (amHost) room.hostStreak else room.guestStreak
    val rivalStreak = if (amHost) room.guestStreak else room.hostStreak
    val reconnectGraceActive = !room.isBot &&
        room.disconnectedPlayerId != null &&
        room.disconnectedPlayerId == room.currentPlayerId &&
        room.reconnectDeadline != null
    val reconnectingMe = reconnectGraceActive && room.disconnectedPlayerId == meId
    val myTurn = room.currentPlayerId == meId && !room.botTurn && !reconnectingMe &&
        room.status in setOf("playing", "final", "sudden_death")
    val rivalName = if (room.isBot) room.botName ?: pt(language, "KelimeBot", "WordBot") else opponent?.displayName ?: pt(language, "Rakip", "Rival")
    val required = premierRequiredToken(room, words)
    val lastWord = words.lastOrNull()
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    LaunchedEffect(myTurn) {
        if (myTurn) {
            delay(140)
            runCatching { focusRequester.requestFocus() }
            keyboardController?.show()
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PremierUi.Surface, PremierUi.Background)))
            .imePadding()
    ) {
        val compact = maxHeight < 650.dp
        val targetSize = if (compact) 94.dp else 116.dp

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            PremierArenaHeader(
                language = language,
                room = room,
                me = me,
                opponent = opponent,
                rivalName = rivalName,
                myScore = myScore,
                rivalScore = rivalScore,
                myRounds = myRounds,
                rivalRounds = rivalRounds,
                myStreak = myStreak,
                rivalStreak = rivalStreak,
                seconds = turnSeconds,
                unreadChat = unreadChat,
                onForfeit = onForfeit,
                onQuickChat = onQuickChat,
            )

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (reconnectGraceActive) PremierReconnectBanner(language, reconnectingMe, turnSeconds)
                        else PremierTurnBadge(language, myTurn, room.status)
                    }
                }
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PremierProWordPanel(
                            language = language,
                            title = pt(language, "Bulunan Kelimeler", "Found Words"),
                            icon = Icons.Rounded.MenuBook,
                            isPro = isPro,
                            entries = words.filter { it.playerId == meId }.takeLast(if (compact) 3 else 5).reversed(),
                            modifier = Modifier.weight(1f),
                        )
                        Column(
                            modifier = Modifier.width(targetSize + 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text(pt(language, "Son Harf", "Last Letter"), color = PremierUi.Ink, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            PremierTargetCard(language, required, room.gameMode, room.roundNo, targetSize)
                        }
                        PremierProWordPanel(
                            language = language,
                            title = pt(language, "Kelime Geçmişi", "Word History"),
                            icon = Icons.Rounded.History,
                            isPro = isPro,
                            entries = words.takeLast(if (compact) 3 else 5).reversed(),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    PremierLastWordBar(language = language, word = lastWord, meId = meId)
                }
                if (notice.isNotBlank()) {
                    item {
                        Text(
                            notice,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            color = PremierUi.OceanDeep,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            PremierInputBar(
                language = language,
                input = input,
                required = required,
                myTurn = myTurn,
                busy = busy,
                onInput = onInput,
                onSubmit = onSubmit,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 5.dp)
                    .focusRequester(focusRequester),
            )

            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onForfeit,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PremierUi.RedSoft, contentColor = PremierUi.Red),
                    border = BorderStroke(1.dp, PremierUi.Red.copy(alpha = .32f)),
                ) {
                    Icon(Icons.Rounded.Flag, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(pt(language, "Pes Et", "Surrender"), fontWeight = FontWeight.Black)
                }
                Box(Modifier.weight(1f)) {
                    Button(
                        onClick = onQuickChat,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PremierUi.Ocean, contentColor = Color.White),
                    ) {
                        Icon(Icons.Rounded.ChatBubbleOutline, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(pt(language, "Sohbet", "Chat"), fontWeight = FontWeight.Black)
                    }
                    if (unreadChat) {
                        Box(Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp).size(10.dp).clip(CircleShape).background(PremierUi.Red))
                    }
                }
            }
        }

        if (myTurn && room.validWordCount > 0) {
            PurchasedVictoryVfx(
                eventKey = "turn:${room.id}:${room.validWordCount}",
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (moveFeedback?.accepted == true) {
            PurchasedVictoryVfx(
                eventKey = "accepted:${room.id}:${room.validWordCount}:${moveFeedback.message}",
                modifier = Modifier.fillMaxSize(),
            )
        }

        AnimatedVisibility(
            visible = floatingMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 154.dp, start = 22.dp, end = 22.dp),
        ) {
            Surface(shape = RoundedCornerShape(16.dp), color = PremierUi.Surface, border = BorderStroke(1.dp, PremierUi.Ocean.copy(alpha = .32f)), shadowElevation = 7.dp) {
                Text(floatingMessage?.body.orEmpty(), Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = PremierUi.Ink, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
        }

        AnimatedVisibility(
            visible = moveFeedback != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 26.dp),
        ) {
            val feedback = moveFeedback
            if (feedback != null) {
                val accent = if (feedback.accepted) PremierUi.Green else PremierUi.Red
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (feedback.accepted) PremierUi.GreenSoft else PremierUi.RedSoft,
                    border = BorderStroke(2.dp, accent),
                    shadowElevation = 12.dp,
                ) {
                    Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (feedback.accepted) Icons.Rounded.CheckCircle else Icons.Rounded.Close, null, tint = accent, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(9.dp))
                        Text(feedback.message, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}'''
text = replace_between(text, "@Composable\nprivate fun PremierArena(", "@Composable\nprivate fun PremierArenaHeader(", arena)

header = r'''@Composable
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
    val progress = (room.roundWordCount.coerceIn(0, 10) / 10f)

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.Stretch,
        ) {
            PremierCalmPlayerCard(
                name = me?.displayName ?: pt(language, "Sen", "You"),
                avatar = me?.avatarPath,
                gender = me?.gender,
                visible = me?.avatarVisibility != "hidden",
                rating = myRating,
                score = myScore,
                streak = myStreak,
                accent = PremierUi.Ocean,
                soft = PremierUi.Ice,
                modifier = Modifier.weight(1f),
                language = language,
                nameColor = SonHarfCosmetics.playerNameColor,
            )
            Surface(
                modifier = Modifier.width(108.dp),
                shape = RoundedCornerShape(18.dp),
                color = PremierUi.Surface,
                border = BorderStroke(1.dp, PremierUi.Border),
                shadowElevation = 3.dp,
            ) {
                Column(
                    Modifier.padding(horizontal = 7.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(pt(language, "Raund ${room.roundNo} / 3", "Round ${room.roundNo} / 3"), color = PremierUi.Ink, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text("$myRounds - $rivalRounds", color = PremierUi.Ink, fontSize = 27.sp, fontWeight = FontWeight.Black)
                    Text(pt(language, "2 raund kazanan\nmaçı kazanır", "First to 2 rounds\nwins the match"), color = PremierUi.Muted, fontSize = 7.sp, lineHeight = 9.sp, textAlign = TextAlign.Center)
                }
            }
            PremierCalmPlayerCard(
                name = rivalName,
                avatar = opponent?.avatarPath,
                gender = opponent?.gender,
                visible = opponent?.avatarVisibility != "hidden",
                rating = rivalRating,
                score = rivalScore,
                streak = rivalStreak,
                accent = PremierUi.Rival,
                soft = PremierUi.RivalSoft,
                modifier = Modifier.weight(1f),
                language = language,
                bot = room.isBot,
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = PremierUi.Surface,
                border = BorderStroke(1.dp, PremierUi.Border),
            ) {
                Row(Modifier.padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Timer, null, tint = if (danger) PremierUi.Red else PremierUi.OceanDeep, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("00:${seconds.coerceAtLeast(0).toString().padStart(2, '0')}", color = if (danger) PremierUi.Red else PremierUi.Ink, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(8.dp))
                    LinearProgressIndicator(
                        progress = { (seconds.coerceIn(0, PREMIER_TURN_SECONDS) / PREMIER_TURN_SECONDS.toFloat()) },
                        modifier = Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(99.dp)),
                        color = if (danger) PremierUi.Red else PremierUi.Ocean,
                        trackColor = PremierUi.Border.copy(alpha = .45f),
                    )
                }
            }
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = PremierUi.Surface,
                border = BorderStroke(1.dp, PremierUi.Border),
            ) {
                Column(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.MenuBook, null, tint = PremierUi.Gold, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(pt(language, "Kelime ${room.roundWordCount.coerceIn(0, 10)} / 10", "Word ${room.roundWordCount.coerceIn(0, 10)} / 10"), color = PremierUi.Ink, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp)),
                        color = PremierUi.Green,
                        trackColor = PremierUi.Border.copy(alpha = .45f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PremierCalmPlayerCard(
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
    nameColor: Color = PremierUi.Ink,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = soft,
        border = BorderStroke(1.dp, accent.copy(alpha = .32f)),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (bot) PremierBotAvatar(size = 42.dp, accent = accent)
                else ProfilePhotoAvatarRectWithGender(
                    avatarPath = if (visible) avatar else null,
                    gender = gender,
                    name = name,
                    width = 46.dp,
                    height = 40.dp,
                    accent = accent,
                )
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, color = nameColor, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(premierLeagueLabel(rating, language), color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("🏆 $rating", color = PremierUi.Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(pt(language, "Raund Puanı", "Round Score"), color = PremierUi.Muted, fontSize = 7.sp)
                    if (streak >= 2) Text("🔥 $streak", color = PremierUi.Red, fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
                Text(score.toString(), color = PremierUi.Ink, fontSize = 21.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

private fun premierLeagueLabel(rating: Int, language: String): String {
    val name = ratingLeagueProgress(rating).leagueName
    val localized = when (name) {
        "BRONZ" -> pt(language, "Bronz", "Bronze")
        "GÜMÜŞ" -> pt(language, "Gümüş", "Silver")
        "ALTIN" -> pt(language, "Altın", "Gold")
        "PLATİN" -> pt(language, "Platin", "Platinum")
        "ELMAS" -> pt(language, "Elmas", "Diamond")
        else -> pt(language, "Efsane", "Legend")
    }
    return "$localized ${pt(language, "Lig", "League")}"
}'''
text = replace_between(text, "@Composable\nprivate fun PremierArenaHeader(", "@Composable\nprivate fun PremierMiniPlayer(", header)

target = r'''@Composable
private fun PremierTargetCard(language: String, required: String, gameMode: String, round: Int, size: Dp) {
    val transition = rememberInfiniteTransition(label = "letter")
    val glow by transition.animateFloat(.12f, .30f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "glow")
    Box(
        Modifier
            .size(size)
            .shadow(8.dp, RoundedCornerShape(25.dp))
            .clip(RoundedCornerShape(25.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFF1F4E7), PremierUi.GreenSoft)))
            .then(Modifier.background(PremierUi.Green.copy(alpha = glow * .10f))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            required,
            color = PremierUi.Ink,
            fontSize = (size.value * if (required.length > 1) .31f else .48f).sp,
            fontWeight = FontWeight.Black,
            letterSpacing = .6.sp,
        )
    }
}'''
text = replace_between(text, "@Composable\nprivate fun PremierTargetCard(", "@Composable\nprivate fun PremierWordTrail(", target)

trail_input = r'''@Composable
private fun PremierProWordPanel(
    language: String,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPro: Boolean,
    entries: List<GameWordDto>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.heightIn(min = 150.dp),
        shape = RoundedCornerShape(18.dp),
        color = PremierUi.Surface,
        border = BorderStroke(1.dp, PremierUi.Border),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = PremierUi.OceanDeep, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(title, color = PremierUi.Ink, fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!isPro) {
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.Lock, null, tint = PremierUi.OceanDeep, modifier = Modifier.size(27.dp))
                Text(pt(language, "Sadece PRO üyeler görebilir", "Visible to PRO members"), color = PremierUi.Muted, fontSize = 7.sp, textAlign = TextAlign.Center, lineHeight = 9.sp)
                Surface(shape = RoundedCornerShape(99.dp), color = PremierUi.GoldSoft, border = BorderStroke(1.dp, PremierUi.Gold.copy(alpha = .5f))) {
                    Row(Modifier.padding(horizontal = 9.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.WorkspacePremium, null, tint = PremierUi.Gold, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("PRO", color = PremierUi.Ink, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.weight(1f))
            } else if (entries.isEmpty()) {
                Spacer(Modifier.weight(1f))
                Text(pt(language, "Henüz kelime yok", "No words yet"), color = PremierUi.Muted, fontSize = 8.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.weight(1f))
            } else {
                entries.forEach { entry ->
                    val shown = premierUpper(entry.normalizedWord.ifBlank { entry.word }, language)
                    val points = DictionaryEngine.calculatePoints(entry.normalizedWord.ifBlank { entry.word }, language)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(shown, modifier = Modifier.weight(1f), color = PremierUi.Ink, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("+$points", color = PremierUi.Green, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                    HorizontalDivider(color = PremierUi.Border.copy(alpha = .55f), thickness = .5.dp)
                }
            }
        }
    }
}

@Composable
private fun PremierLastWordBar(language: String, word: GameWordDto?, meId: String?) {
    val raw = word?.normalizedWord?.ifBlank { word.word }.orEmpty()
    val shown = if (raw.isBlank()) pt(language, "Henüz kelime yok", "No word yet") else premierUpper(raw, language)
    val points = if (raw.isBlank()) 0 else DictionaryEngine.calculatePoints(raw, language)
    val mine = word?.playerId != null && word.playerId == meId
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PremierUi.Surface,
        border = BorderStroke(1.dp, PremierUi.Border),
    ) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.History, null, tint = if (mine) PremierUi.Ocean else PremierUi.Rival, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text(pt(language, "Son Yazılan Kelime:", "Last Word:"), color = PremierUi.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Text(shown, modifier = Modifier.weight(1f), color = PremierUi.Ink, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (points > 0) Text("+$points", color = PremierUi.Green, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PremierInputBar(
    language: String,
    input: String,
    required: String,
    myTurn: Boolean,
    busy: Boolean,
    onInput: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = myTurn && !busy
    OutlinedTextField(
        value = input,
        onValueChange = { onInput(it.replace("\n", "")) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        shape = RoundedCornerShape(19.dp),
        textStyle = LocalTextStyle.current.copy(color = PremierUi.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold),
        placeholder = {
            Text(
                when {
                    busy -> pt(language, "Kontrol ediliyor…", "Checking…")
                    !myTurn -> pt(language, "Rakibin hamlesi bekleniyor…", "Waiting for rival…")
                    required == "★" -> pt(language, "Kelimeyi buraya yazın…", "Type your word here…")
                    else -> pt(language, "$required ile başlayan kelime yazın…", "Type a word starting with $required…")
                },
                color = PremierUi.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingIcon = {
            if (input.isNotEmpty()) {
                IconButton(onClick = { onInput("") }, enabled = enabled) {
                    Icon(Icons.Rounded.Close, pt(language, "Temizle", "Clear"), tint = PremierUi.Muted)
                }
            }
        },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters,
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
            imeAction = androidx.compose.ui.text.input.ImeAction.Done,
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onDone = { if (enabled && input.isNotBlank()) onSubmit() },
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = PremierUi.Surface,
            unfocusedContainerColor = PremierUi.Surface,
            disabledContainerColor = PremierUi.Ice.copy(alpha = .72f),
            focusedBorderColor = PremierUi.Ocean,
            unfocusedBorderColor = PremierUi.Border,
            disabledBorderColor = PremierUi.Border,
            cursorColor = PremierUi.Ocean,
        ),
    )
}'''
text = replace_between(
    text,
    "@Composable\nprivate fun PremierWordTrail(",
    "@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nprivate fun PremierChatSheet(",
    trail_input,
)

path.write_text(text, encoding="utf-8")
print("Son Harf calm redesign applied:", path)
