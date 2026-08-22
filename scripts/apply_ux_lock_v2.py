from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(path: str, old: str, new: str) -> None:
    p = ROOT / path
    text = p.read_text(encoding="utf-8")
    if new in text:
        print(f"already patched {path}")
        return
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one old match in {path}, found {count}: {old[:120]!r}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")
    print(f"patched {path}")


def remove_once(path: str, target: str) -> None:
    p = ROOT / path
    text = p.read_text(encoding="utf-8")
    if target not in text:
        print(f"already removed {path}: {target.strip()}")
        return
    p.write_text(text.replace(target, "", 1), encoding="utf-8")
    print(f"removed legacy block from {path}")


# 1) New contract: use Android's system keyboard, not the custom regression keyboard overlay.
remove_once(
    "app/src/main/java/com/sonharf/game/MainActivity.kt",
    "                    RegressionGuardOverlay()\n",
)

# 2) Profile DTO carries gender everywhere game UI needs it.
replace_once(
    "app/src/main/java/com/sonharf/game/data/OnlineGameBackend.kt",
    '    @SerialName("avatar_visibility") val avatarVisibility: String = "visible",\n    @SerialName("allow_match_chat") val allowMatchChat: Boolean = true,',
    '    @SerialName("avatar_visibility") val avatarVisibility: String = "visible",\n    val gender: String? = null,\n    @SerialName("allow_match_chat") val allowMatchChat: Boolean = true,',
)

# 3) Profile photos are public UI identity and should render wherever a path exists.
replace_once(
    "app/src/main/java/com/sonharf/game/ProfilePhotoRuntime.kt",
    '''    LaunchedEffect(avatarPath, visible) {\n        bytes = if (visible && !avatarPath.isNullOrBlank()) ProfilePhotoRuntime.load(avatarPath) else null\n    }''',
    '''    LaunchedEffect(avatarPath) {\n        bytes = if (!avatarPath.isNullOrBlank()) ProfilePhotoRuntime.load(avatarPath) else null\n    }''',
)

# 4) Son Harf: always show opponent photo, carry gender to both player cards.
replace_once(
    "app/src/main/java/com/sonharf/game/TargetNeonGameScreen.kt",
    '''                playerAvatarPath = profile?.avatarPath,\n                opponentAvatarPath = opponentProfile?.avatarPath,\n                opponentAvatarVisible = active.isBot || opponentProfile?.avatarVisibility != "hidden",''',
    '''                playerAvatarPath = profile?.avatarPath,\n                playerGender = profile?.gender,\n                opponentAvatarPath = opponentProfile?.avatarPath,\n                opponentGender = opponentProfile?.gender,\n                opponentAvatarVisible = true,''',
)
replace_once(
    "app/src/main/java/com/sonharf/game/TargetNeonGameScreen.kt",
    '''    playerAvatarPath: String?,\n    opponentAvatarPath: String?,\n    opponentAvatarVisible: Boolean,''',
    '''    playerAvatarPath: String?,\n    playerGender: String?,\n    opponentAvatarPath: String?,\n    opponentGender: String?,\n    opponentAvatarVisible: Boolean,''',
)
replace_once(
    "app/src/main/java/com/sonharf/game/TargetNeonGameScreen.kt",
    '''            TargetArenaPlayer(playerName, playerAvatarPath, true, myScore, myRounds, myTurn, TGcyan, Modifier.weight(1f))''',
    '''            TargetArenaPlayer(playerName, playerAvatarPath, playerGender, true, myScore, myRounds, myTurn, TGcyan, Modifier.weight(1f))''',
)
replace_once(
    "app/src/main/java/com/sonharf/game/TargetNeonGameScreen.kt",
    '''            TargetArenaPlayer(opponentName, opponentAvatarPath, opponentAvatarVisible, oppScore, oppRounds, !myTurn, TGpink, Modifier.weight(1f))''',
    '''            TargetArenaPlayer(opponentName, opponentAvatarPath, opponentGender, opponentAvatarVisible, oppScore, oppRounds, !myTurn, TGpink, Modifier.weight(1f))''',
)

# 5) Clear the input immediately on send and only display an error for actual server rejection events.
replace_once(
    "app/src/main/java/com/sonharf/game/TargetNeonGameScreen.kt",
    '''                        val submitted = wordInput.trim(); if (submitted.isBlank()) return@launch\n                        busy = true\n                        runCatching { backend.submitWord(active.id, submitted) }\n                            .onSuccess {\n                                room = it\n                                wordInput = ""\n                                notice = if (it.lastEventPlayerId == me && it.lastEvent != null && it.lastEvent != "word_accepted") friendly(it.lastEvent ?: "") else "${submitted.uppercase()} kabul edildi"\n                            }\n                            .onFailure { notice = friendly(it.message.orEmpty()) }\n                        busy = false''',
    '''                        val submitted = wordInput.trim(); if (submitted.isBlank()) return@launch\n                        wordInput = ""\n                        busy = true\n                        runCatching { backend.submitWord(active.id, submitted) }\n                            .onSuccess { updated ->\n                                room = updated\n                                val rejected = updated.lastEventPlayerId == me && updated.lastEvent in setOf(\n                                    "word_already_used", "wrong_start_letter", "not_in_dictionary", "invalid_word", "turn_expired"\n                                )\n                                notice = if (rejected) friendly(updated.lastEvent.orEmpty()) else "${submitted.uppercase()} kabul edildi"\n                            }\n                            .onFailure { notice = friendly(it.message.orEmpty()) }\n                        busy = false''',
)

# 6) Modern, light-theme forfeit dialog.
replace_once(
    "app/src/main/java/com/sonharf/game/TargetNeonGameScreen.kt",
    '''            AlertDialog(\n                onDismissRequest = { confirmForfeit = false },\n                title = { Text(sh("PES ETMEK İSTEDİĞİNE EMİN MİSİN?", "ARE YOU SURE YOU WANT TO FORFEIT?"), fontWeight = FontWeight.Black) },\n                text = { Text(sh("Maç devam ederken çıkış yapılamaz. Çıkmak için maçı pes ederek bitirmen gerekir.", "You cannot leave during a live match. Forfeit the match to exit.")) },\n                confirmButton = { Button(onClick = { confirmForfeit = false; onForfeit() }, colors = ButtonDefaults.buttonColors(containerColor = TGpink)) { Text(sh("EVET, PES ET", "YES, FORFEIT")) } },\n                dismissButton = { TextButton(onClick = { confirmForfeit = false }) { Text(sh("OYUNA DÖN", "RETURN TO GAME")) } },\n            )''',
    '''            AlertDialog(\n                onDismissRequest = { confirmForfeit = false },\n                icon = {\n                    Surface(shape = CircleShape, color = TGpink.copy(alpha = .12f)) {\n                        Text("⚑", modifier = Modifier.padding(14.dp), color = TGpink, fontSize = 28.sp, fontWeight = FontWeight.Black)\n                    }\n                },\n                title = { Text(sh("Maçtan çıkılsın mı?", "Leave the match?"), fontWeight = FontWeight.Black, textAlign = TextAlign.Center) },\n                text = { Text(sh("Pes edersen maç rakibin lehine tamamlanır. Bu işlem geri alınamaz.", "If you forfeit, the match ends in your opponent's favor. This cannot be undone."), textAlign = TextAlign.Center) },\n                confirmButton = {\n                    Button(\n                        onClick = { confirmForfeit = false; onForfeit() },\n                        colors = ButtonDefaults.buttonColors(containerColor = TGpink),\n                        shape = RoundedCornerShape(14.dp),\n                    ) { Text(sh("PES ET VE ÇIK", "FORFEIT & LEAVE"), color = Color.White, fontWeight = FontWeight.Black) }\n                },\n                dismissButton = {\n                    OutlinedButton(onClick = { confirmForfeit = false }, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, TGcyan.copy(alpha = .55f))) {\n                        Text(sh("OYUNA DÖN", "RETURN TO GAME"), color = TGblue, fontWeight = FontWeight.Bold)\n                    }\n                },\n                containerColor = TGpanel,\n                titleContentColor = TGtext,\n                textContentColor = TGmuted,\n                shape = RoundedCornerShape(28.dp),\n            )''',
)

# 7) Small gender token sits on the bottom corner of every arena profile photo.
replace_once(
    "app/src/main/java/com/sonharf/game/TargetNeonGameScreen.kt",
    '''@Composable private fun TargetArenaPlayer(name: String, avatarPath: String?, avatarVisible: Boolean, score: Int, rounds: Int, active: Boolean, accent: Color, modifier: Modifier) {\n    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {\n        ProfilePhotoAvatar(avatarPath, name, 48.dp, visible = avatarVisible, accent = accent)\n        Spacer(Modifier.height(5.dp))\n        Text(name, color = TGtext, fontWeight = FontWeight.Black, fontSize = 10.sp, maxLines = 1)\n        Text("🏆 $score", color = TGgold, fontSize = 8.sp)\n        Text("$rounds round", color = if (active) accent else TGmuted, fontSize = 8.sp)\n    }\n}''',
    '''@Composable private fun TargetArenaPlayer(name: String, avatarPath: String?, gender: String?, avatarVisible: Boolean, score: Int, rounds: Int, active: Boolean, accent: Color, modifier: Modifier) {\n    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {\n        Box {\n            ProfilePhotoAvatar(avatarPath, name, 48.dp, visible = avatarVisible, accent = accent)\n            val normalizedGender = gender?.trim()?.lowercase()\n            val female = normalizedGender in setOf("kadın", "kadin", "female", "woman")\n            val male = normalizedGender in setOf("erkek", "male", "man")\n            if (female || male) {\n                Surface(\n                    modifier = Modifier.align(Alignment.BottomEnd).size(17.dp),\n                    shape = CircleShape,\n                    color = if (female) Color(0xFFFF76A8) else Color(0xFF439EF2),\n                    border = BorderStroke(1.dp, Color.White),\n                ) { Box(contentAlignment = Alignment.Center) { Text(if (female) "♀" else "♂", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black) } }\n            }\n        }\n        Spacer(Modifier.height(5.dp))\n        Text(name, color = TGtext, fontWeight = FontWeight.Black, fontSize = 10.sp, maxLines = 1)\n        Text("🏆 $score", color = TGgold, fontSize = 8.sp)\n        Text("$rounds round", color = if (active) accent else TGmuted, fontSize = 8.sp)\n    }\n}''',
)

# 8) Bil Bakalım uses a scroll-safe layout so the bottom blue action is never clipped.
replace_once(
    "app/src/main/java/com/sonharf/game/BilBakalimFeature.kt",
    '''import androidx.compose.foundation.background\nimport androidx.compose.foundation.layout.*''',
    '''import androidx.compose.foundation.background\nimport androidx.compose.foundation.rememberScrollState\nimport androidx.compose.foundation.verticalScroll\nimport androidx.compose.foundation.layout.*''',
)
replace_once(
    "app/src/main/java/com/sonharf/game/BilBakalimFeature.kt",
    '''import androidx.compose.ui.unit.sp\nimport kotlinx.coroutines.delay''',
    '''import androidx.compose.ui.unit.sp\nimport com.sonharf.game.data.OnlineGameBackend\nimport com.sonharf.game.data.ProfileDto\nimport com.sonharf.game.data.SupabaseProvider\nimport kotlinx.coroutines.delay''',
)
replace_once(
    "app/src/main/java/com/sonharf/game/BilBakalimFeature.kt",
    '''fun BilBakalimStandaloneScreen(onBack: () -> Unit) {\n    val scope = rememberCoroutineScope()''',
    '''fun BilBakalimStandaloneScreen(onBack: () -> Unit) {\n    val scope = rememberCoroutineScope()\n    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }\n    var playerProfile by remember { mutableStateOf<ProfileDto?>(null) }\n    LaunchedEffect(Unit) {\n        val b = backend ?: return@LaunchedEffect\n        val id = b.currentUserId() ?: return@LaunchedEffect\n        playerProfile = runCatching { b.getProfile(id) }.getOrNull()\n    }''',
)
replace_once(
    "app/src/main/java/com/sonharf/game/BilBakalimFeature.kt",
    '''        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {''',
    '''        Column(\n            Modifier.fillMaxSize().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),\n            horizontalAlignment = Alignment.CenterHorizontally,\n        ) {''',
)

# 9) Bil Bakalım match end publishes the winner with avatar and name.
replace_once(
    "app/src/main/java/com/sonharf/game/BilBakalimFeature.kt",
    '''            if (phase == BilPhase.MATCH_END) {\n                Spacer(Modifier.weight(1f))\n                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(2.dp, Color(0xFF69C9EF)), shape = RoundedCornerShape(26.dp)) {\n                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {\n                        Icon(Icons.Rounded.EmojiEvents, null, tint = Color(0xFF45B8E5), modifier = Modifier.size(52.dp))\n                        Text(if (playerScore >= botScore) "KAZANDIN!" else "MAÇ BİTTİ", color = Color(0xFF17344A), fontWeight = FontWeight.Black, fontSize = 32.sp)\n                        Text("15 SORU TAMAMLANDI", color = Color(0xFF6C8293), fontWeight = FontWeight.Bold)\n                        Text("$playerScore  -  $botScore", color = Color(0xFF2CA9DC), fontSize = 42.sp, fontWeight = FontWeight.Black)\n                        Button(onClick = ::resetMatch, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4BBBE8))) { Text("BİR OYUN DAHA", fontWeight = FontWeight.Black) }\n                        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("ANA MENÜ") }\n                    }\n                }\n                Spacer(Modifier.weight(1f))\n                return@Column\n            }''',
    '''            if (phase == BilPhase.MATCH_END) {\n                val playerIsWinner = playerScore >= botScore\n                val winnerName = if (playerIsWinner) playerProfile?.displayName ?: "Sen" else "KelimeBot BOT"\n                Spacer(Modifier.height(24.dp))\n                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(2.dp, Color(0xFF69C9EF)), shape = RoundedCornerShape(26.dp)) {\n                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {\n                        Icon(Icons.Rounded.EmojiEvents, null, tint = Color(0xFF45B8E5), modifier = Modifier.size(48.dp))\n                        Text(if (playerIsWinner) "KAZANDIN!" else "MAÇ BİTTİ", color = Color(0xFF17344A), fontWeight = FontWeight.Black, fontSize = 32.sp)\n                        Text("15 SORU TAMAMLANDI", color = Color(0xFF6C8293), fontWeight = FontWeight.Bold)\n                        Spacer(Modifier.height(4.dp))\n                        if (playerIsWinner) {\n                            Box {\n                                ProfilePhotoAvatar(playerProfile?.avatarPath, winnerName, 82.dp, visible = true, accent = Color(0xFF2CA9DC))\n                                val g = playerProfile?.gender?.trim()?.lowercase()\n                                val female = g in setOf("kadın", "kadin", "female", "woman")\n                                val male = g in setOf("erkek", "male", "man")\n                                if (female || male) {\n                                    Surface(modifier = Modifier.align(Alignment.BottomEnd).size(22.dp), shape = RoundedCornerShape(100.dp), color = if (female) Color(0xFFFF76A8) else Color(0xFF439EF2), border = BorderStroke(1.dp, Color.White)) {\n                                        Box(contentAlignment = Alignment.Center) { Text(if (female) "♀" else "♂", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black) }\n                                    }\n                                }\n                            }\n                        } else {\n                            Surface(modifier = Modifier.size(82.dp), shape = RoundedCornerShape(100.dp), color = Color(0xFFFFEEF2), border = BorderStroke(2.dp, Color(0xFFEA7484))) {\n                                Box(contentAlignment = Alignment.Center) { Text("🤖", fontSize = 42.sp) }\n                            }\n                        }\n                        Text(winnerName, color = if (playerIsWinner) Color(0xFF2CA9DC) else Color(0xFFEA7484), fontSize = 19.sp, fontWeight = FontWeight.Black)\n                        Text("KAZANAN", color = Color(0xFF6C8293), fontSize = 11.sp, fontWeight = FontWeight.Bold)\n                        Text("$playerScore  -  $botScore", color = Color(0xFF2CA9DC), fontSize = 42.sp, fontWeight = FontWeight.Black)\n                        Button(onClick = ::resetMatch, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4BBBE8)), shape = RoundedCornerShape(18.dp)) { Text("BİR OYUN DAHA", fontWeight = FontWeight.Black) }\n                        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(18.dp)) { Text("ANA MENÜ") }\n                    }\n                }\n                Spacer(Modifier.height(24.dp))\n                return@Column\n            }''',
)
replace_once(
    "app/src/main/java/com/sonharf/game/BilBakalimFeature.kt",
    '''                        Button(onClick = ::advance, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4BBBE8))) { Text(if (questionNo == 15) "MAÇI BİTİR" else "SONRAKİ SORU", fontWeight = FontWeight.Black) }\n                    }\n                }\n            }''',
    '''                        Button(onClick = ::advance, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4BBBE8)), shape = RoundedCornerShape(16.dp)) { Text(if (questionNo == 15) "MAÇI BİTİR" else "SONRAKİ SORU", fontWeight = FontWeight.Black) }\n                    }\n                }\n                Spacer(Modifier.height(22.dp))\n            }''',
)

print("UX lock v2 repairs applied successfully.")
