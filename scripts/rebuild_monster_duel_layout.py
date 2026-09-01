from pathlib import Path
import re

path = Path('app/src/main/java/com/sonharf/game/LightDuelUi.kt')
text = path.read_text(encoding='utf-8')

new_lobby = r'''@Composable
internal fun LightDuelLobby(
    playerName: String,
    playerAvatarPath: String?,
    playerGender: String?,
    language: String,
    matching: Boolean,
    notice: String,
    showPrivate: Boolean,
    showFriends: Boolean,
    privateCode: String,
    friends: List<Pair<FriendshipDto, ProfileDto>>,
    invites: List<GameInviteDto>,
    onLanguage: (String) -> Unit,
    onPrivateCode: (String) -> Unit,
    onRandom: () -> Unit,
    onCancel: () -> Unit,
    onPrivate: () -> Unit,
    onFriends: () -> Unit,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    onInvite: (String) -> Unit,
    onInviteResponse: (String, Boolean) -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D0E11), LBg, Color(0xFF15171C))))
            .statusBarsPadding()
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("SON HARF", color = LText, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(LRed))
                            Spacer(Modifier.width(6.dp))
                            Text(sh("CANLI DÜELLO", "LIVE DUEL"), color = LMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = LCard2, border = BorderStroke(1.dp, LBorder)) {
                        Text(if (language == "tr") "TR" else "EN", Modifier.padding(horizontal = 11.dp, vertical = 8.dp), color = LBlue, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            item {
                Surface(shape = RoundedCornerShape(16.dp), color = LCard, border = BorderStroke(1.dp, LBorder)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ProfilePhotoAvatarWithGender(
                            avatarPath = playerAvatarPath,
                            gender = playerGender,
                            name = playerName,
                            size = 46.dp,
                            accent = LBlue,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(playerName, color = LText, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1)
                            Text(sh("Düello lobisi", "Duel lobby"), color = LMuted, fontSize = 9.sp)
                        }
                        Surface(shape = RoundedCornerShape(9.dp), color = LBlueSoft) {
                            Text(sh("HAZIR", "READY"), Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = LBlue, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(Color(0xFFFF5B4D), Color(0xFFFF315E))))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(8.dp), color = Color.Black.copy(alpha = .18f)) {
                                    Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(6.dp).clip(CircleShape).background(LBlue))
                                        Spacer(Modifier.width(5.dp))
                                        Text(if (matching) sh("EŞLEŞME ARANIYOR", "MATCH SEARCH") else sh("CANLI EŞLEŞME", "LIVE MATCH"), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                                Text("1v1", color = Color.White.copy(alpha = .82f), fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }

                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(shape = CircleShape, color = Color.White.copy(alpha = .16f)) {
                                        Text("S", Modifier.padding(horizontal = 16.dp, vertical = 12.dp), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(playerName, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("VS", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
                                    Text(sh("ANLIK", "LIVE"), color = Color.White.copy(alpha = .72f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(shape = CircleShape, color = Color.White.copy(alpha = .16f)) {
                                        if (matching) {
                                            CircularProgressIndicator(Modifier.padding(12.dp).size(24.dp), color = LBlue, strokeWidth = 3.dp)
                                        } else {
                                            Text("?", Modifier.padding(horizontal = 16.dp, vertical = 12.dp), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(if (matching) sh("Rakip aranıyor", "Searching") else sh("Rakip Bul", "Find Rival"), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Text(
                                if (matching) sh("Önce gerçek oyuncu aranır, gerekirse BOT devreye girer.", "A real player is searched first; BOT joins if needed.")
                                else sh("Kelimeyi sürdür, rakibini geç.", "Keep the word going, beat your rival."),
                                color = Color.White.copy(alpha = .78f),
                                fontSize = 9.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LightChoicePill(selected = language == "tr", text = "🇹🇷 TÜRKÇE", modifier = Modifier.weight(1f)) { onLanguage("tr") }
                    LightChoicePill(selected = language == "en", text = "🇬🇧 ENGLISH", modifier = Modifier.weight(1f)) { onLanguage("en") }
                }
            }

            item {
                Button(
                    onClick = if (matching) onCancel else onRandom,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (matching) LCard2 else LBlue,
                        contentColor = if (matching) LRed else Color(0xFF101114),
                    ),
                    border = if (matching) BorderStroke(1.dp, LRed.copy(alpha = .45f)) else null,
                ) {
                    Text(
                        if (matching) sh("EŞLEŞMEYİ İPTAL ET", "CANCEL MATCHMAKING") else sh("OYNA  ⚡", "PLAY  ⚡"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LightLobbyAction(sh("ARKADAŞ", "FRIENDS"), sh("Davet et", "Invite"), "👥", Modifier.weight(1f), onFriends)
                    LightLobbyAction(sh("ÖZEL ODA", "PRIVATE ROOM"), sh("Kodla gir", "Join by code"), "♛", Modifier.weight(1f), onPrivate)
                }
            }

            if (notice.isNotBlank()) item { LightNotice(notice) }

            if (showPrivate) item {
                Surface(shape = RoundedCornerShape(16.dp), color = LCard, border = BorderStroke(1.dp, LBorder)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(sh("ÖZEL ODA", "PRIVATE ROOM"), color = LText, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Button(onClick = onCreate, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = LBlue, contentColor = Color(0xFF101114))) {
                            Text(sh("VIP ODA OLUŞTUR", "CREATE VIP ROOM"), fontWeight = FontWeight.Black)
                        }
                        OutlinedTextField(
                            value = privateCode,
                            onValueChange = onPrivateCode,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text(sh("6 haneli oda kodu", "6-character room code"), color = LMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = LText,
                                unfocusedTextColor = LText,
                                focusedBorderColor = LBlue,
                                unfocusedBorderColor = LBorder,
                                focusedContainerColor = LCard2,
                                unfocusedContainerColor = LCard2,
                            ),
                        )
                        OutlinedButton(onClick = onJoin, enabled = privateCode.length == 6, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, LBlue.copy(alpha = .5f))) {
                            Text(sh("ODA KODUYLA KATIL", "JOIN WITH ROOM CODE"), color = LBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (showFriends) item {
                Surface(shape = RoundedCornerShape(16.dp), color = LCard, border = BorderStroke(1.dp, LBorder)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text(sh("ARKADAŞLAR", "FRIENDS"), color = LText, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        invites.forEach { i ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(sh("Maç daveti", "Game invite"), color = LText)
                                Row {
                                    TextButton(onClick = { onInviteResponse(i.id, true) }) { Text(sh("Kabul", "Accept"), color = LBlue) }
                                    TextButton(onClick = { onInviteResponse(i.id, false) }) { Text(sh("Reddet", "Decline"), color = LRed) }
                                }
                            }
                        }
                        friends.forEach { (_, p) ->
                            Surface(shape = RoundedCornerShape(12.dp), color = LCard2) {
                                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(p.displayName, color = LText, fontWeight = FontWeight.Bold)
                                        Text(if (p.presenceStatus == "online") sh("Çevrimiçi", "Online") else sh("Çevrimdışı", "Offline"), color = if (p.presenceStatus == "online") LGreen else LMuted, fontSize = 9.sp)
                                    }
                                    Button(onClick = { onInvite(p.id) }, enabled = p.presenceStatus == "online", colors = ButtonDefaults.buttonColors(containerColor = LBlue, contentColor = Color(0xFF101114))) {
                                        Text(sh("Davet", "Invite"), fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
        }
    }
}
'''

lobby_pattern = re.compile(r'@Composable\ninternal fun LightDuelLobby\([\s\S]*?\n\}\n\n(?=@Composable\ninternal fun LightDuelArena\()')
text, count = lobby_pattern.subn(new_lobby + '\n', text, count=1)
if count != 1:
    raise SystemExit(f'Lobby structural replacement failed: {count}')

arena_ui = r'''    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0D0E11), LBg, Color(0xFF15171C))))
            .statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(LRed))
                    Spacer(Modifier.width(6.dp))
                    Text(sh("CANLI DÜELLO", "LIVE DUEL"), color = LMuted, fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
                Text(
                    if (room.status == "sudden_death") sh("ANİ ÖLÜM", "SUDDEN DEATH") else "ROUND ${room.roundNo}/3",
                    color = LText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (seconds <= 3 && !quizActive) LRed.copy(alpha = .16f) else LCard2,
                border = BorderStroke(1.dp, if (seconds <= 3 && !quizActive) LRed else LBorder),
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(seconds.toString(), color = if (seconds <= 3 && !quizActive) LRed else LBlue, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(4.dp))
                    Text(if (quizActive) "BONUS" else sh("SN", "SEC"), color = LMuted, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            shape = RoundedCornerShape(18.dp),
            color = LCard,
            border = BorderStroke(1.dp, LBorder),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                        Text(playerName, color = if (myTurn) LBlue else LText, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        Text("🏆 $playerRating", color = LGold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(myScore.toString(), color = LText, fontSize = duelScoreFontSize(myScore).sp, fontWeight = FontWeight.Black)
                    Text("  :  ", color = LMuted, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text(oppScore.toString(), color = LText, fontSize = duelScoreFontSize(oppScore).sp, fontWeight = FontWeight.Black)
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(opponentName.removeSuffix(" BOT"), color = if (!myTurn && liveWordPhase) LRed else LText, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        Text("🏆 $opponentRating", color = LGold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
                val shownCount = room.roundWordCount.coerceIn(0, 10)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(10) { i ->
                        Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(if (i < shownCount) LBlue else LCard2))
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color.Transparent,
        ) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        if (myTurn) listOf(Color(0xFF272A1D), LCard) else listOf(Color(0xFF21191B), LCard)
                    )
                ).padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(
                        when {
                            quizActive -> sh("BONUS DÜELLOSU", "BONUS DUEL")
                            myTurn -> sh("SIRA SENDE", "YOUR TURN")
                            room.isBot && room.botTurn -> sh("BOT DÜŞÜNÜYOR", "BOT IS THINKING")
                            else -> sh("RAKİBİN HAMLESİ", "OPPONENT'S MOVE")
                        },
                        color = when { quizActive -> LGold; myTurn -> LBlue; else -> LMuted },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(sh("SON HARF", "LAST LETTER"), color = LMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(required, color = LText, fontSize = 66.sp, lineHeight = 68.sp, fontWeight = FontWeight.Black)
                    Text(
                        shownLastWord.ifBlank { sh("İLK KELİMEYİ YAZ", "ENTER FIRST WORD") },
                        color = shownLastWordColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        if (last.isBlank()) sh("İlk kelimeyi sen başlat.", "Start with the first word.") else sh("$required ile başlayan bir kelime yaz", "Enter a word starting with $required"),
                        color = LMuted,
                        fontSize = 9.sp,
                    )
                }
            }
        }

        LightVipWordHistory(isVip = isVip, words = words, language = room.language, modifier = Modifier.padding(horizontal = 12.dp))

        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            LightActionButton(sh("⚑ PES ET", "⚑ FORFEIT"), LRed, Modifier.weight(1f), onForfeit)
            LightActionButton(sh("● SOHBET", "● CHAT"), LBlue, Modifier.weight(1f), onChat)
            LightActionButton("★ BONUS", LGold, Modifier.weight(1f)) { }
        }

        if (quizActive) {
            val activeTrivia = requireNotNull(triviaRound)
            LightBonusCard(
                round = activeTrivia,
                question = requireNotNull(triviaQuestion),
                myAnswer = (if (host) activeTrivia.hostAnswer else activeTrivia.guestAnswer) ?: triviaSelection,
                onTrivia = onTrivia,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }

        LightInputBar(value = wordInput, myTurn = myTurn, busy = busy, quiz = quizActive, onSubmit = onSubmit, modifier = Modifier.padding(horizontal = 12.dp))

        LightGameKeyboard(
            value = wordInput,
            language = room.language,
            enabled = !busy && !quizActive,
            submitEnabled = myTurn && wordInput.isNotBlank() && !busy && !quizActive,
            onValueChange = onWordInput,
            onSubmit = onSubmit,
            modifier = Modifier.navigationBarsPadding(),
        )
    }
'''

arena_pattern = re.compile(r'    Column\(\n        Modifier\n            \.fillMaxSize\(\)[\s\S]*?\n    \}\n(?=\}\n\n@Composable\nprivate fun LightPlayerCard\()')
text, count = arena_pattern.subn(arena_ui, text, count=1)
if count != 1:
    raise SystemExit(f'Arena structural replacement failed: {count}')

path.write_text(text, encoding='utf-8')
print('Monster duel lobby and arena layout rebuilt')
