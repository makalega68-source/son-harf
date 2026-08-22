from pathlib import Path


def required_replace(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f"Patch target not found: {label}")
    return text.replace(old, new, 1)


def optional_replace(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    if old not in text:
        print(f"Optional patch skipped: {label}")
        return text
    return text.replace(old, new, 1)


# Main navigation: expose Bil Bakalim as the second game on the home screen.
classic_path = Path("app/src/main/java/com/sonharf/game/ClassicPremiumApp.kt")
classic = classic_path.read_text()
classic = required_replace(
    classic,
    "HOME, PLAY, GAME, PROFILE, SHOP, HUB, LEAGUE, PROFILE_FULL, SHOP_FULL",
    "HOME, PLAY, GAME, BIL_BAKALIM, PROFILE, SHOP, HUB, LEAGUE, PROFILE_FULL, SHOP_FULL",
    "classic enum",
)
classic = required_replace(
    classic,
    "onQuickGame = { gameKey += 1; screen = ClassicScreen.GAME },\n                        onHub = { screen = ClassicScreen.HUB },",
    "onQuickGame = { gameKey += 1; screen = ClassicScreen.GAME },\n                        onBilBakalim = { screen = ClassicScreen.BIL_BAKALIM },\n                        onHub = { screen = ClassicScreen.HUB },",
    "home Bil Bakalim callback",
)
classic = required_replace(
    classic,
    "ClassicScreen.GAME -> key(gameKey) { TargetNeonGameScreen() }\n                    ClassicScreen.HUB -> MetaHubScreen()",
    "ClassicScreen.GAME -> key(gameKey) { TargetNeonGameScreen() }\n                    ClassicScreen.BIL_BAKALIM -> BilBakalimStandaloneScreen { screen = ClassicScreen.HOME }\n                    ClassicScreen.HUB -> MetaHubScreen()",
    "Bil Bakalim route",
)
classic = required_replace(
    classic,
    "onQuickGame: () -> Unit,\n    onHub: () -> Unit,",
    "onQuickGame: () -> Unit,\n    onBilBakalim: () -> Unit,\n    onHub: () -> Unit,",
    "ClassicHome signature",
)
classic = required_replace(
    classic,
    "item { ClassicHero(onQuickGame) }\n        item {\n            ClassicModeSelector(mode)",
    "item { ClassicHero(onQuickGame) }\n        item { BilBakalimHomeCard(onBilBakalim) }\n        item {\n            ClassicModeSelector(mode)",
    "home Bil Bakalim card",
)
classic_path.write_text(classic)


# Normal arena: VIP gate chat + word chain. The arena already has a Turkish/English
# language-aware on-screen keyboard; add Android IME language hints to the chat field.
v10_path = Path("app/src/main/java/com/sonharf/game/SketchGameOverlayV10.kt")
v10 = v10_path.read_text()
v10 = required_replace(
    v10,
    "import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.text.SpanStyle",
    "import androidx.compose.ui.graphics.Color\nimport androidx.compose.foundation.text.KeyboardActions\nimport androidx.compose.foundation.text.KeyboardOptions\nimport androidx.compose.ui.text.SpanStyle\nimport androidx.compose.ui.text.input.ImeAction\nimport androidx.compose.ui.text.input.KeyboardType\nimport androidx.compose.ui.text.intl.Locale\nimport androidx.compose.ui.text.intl.LocaleList",
    "V10 keyboard imports",
)
v10 = required_replace(
    v10,
    "onChat = {\n                if (active.isBot) notice = sh(\"Bot maçında sohbet kapalı.\", \"Chat is disabled in bot matches.\")\n                else scope.launch { chat = runCatching { backend.getChat(active.id) }.getOrDefault(emptyList()); showChat = true }\n            },",
    "onChat = {\n                if (!isVip) notice = sh(\"Sohbet VIP üyelerine özeldir.\", \"Chat is for VIP members.\")\n                else if (active.isBot) notice = sh(\"Bot maçında sohbet kapalı.\", \"Chat is disabled in bot matches.\")\n                else scope.launch { chat = runCatching { backend.getChat(active.id) }.getOrDefault(emptyList()); showChat = true }\n            },",
    "V10 VIP chat gate",
)
v10 = required_replace(
    v10,
    '''        Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.CenterStart) {
            if (words.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    items(words.takeLast(if (isVip) 16 else 7)) { w ->
                        val duplicate = feedback?.duplicateWord?.equals(w.word, ignoreCase = true) == true
                        Surface(shape = RoundedCornerShape(12.dp), color = when { duplicate -> SonHarfPink.copy(alpha=.22f); isVip -> SonHarfGold.copy(alpha=.12f); else -> SonHarfSurface2 }, border = BorderStroke(if (duplicate) 2.dp else 1.dp, when { duplicate -> SonHarfPink; isVip -> SonHarfGold.copy(alpha=.5f); else -> SonHarfMuted.copy(alpha=.14f) })) {
                            Text(w.word.uppercase(), Modifier.padding(horizontal=12.dp, vertical=8.dp), color = if (duplicate) SonHarfPink else SonHarfText, fontSize = if (duplicate) 17.sp else 14.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            } else Text(sh("Kelime zinciri burada görünecek", "Word chain will appear here"), color = SonHarfMuted, fontSize = 13.sp)
        }''',
    '''        Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.CenterStart) {
            if (!isVip) {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = SonHarfSurface2, border = BorderStroke(1.dp, SonHarfGold.copy(alpha=.35f))) {
                    Text(sh("🔒 KELİME ZİNCİRİ • VIP", "🔒 WORD CHAIN • VIP"), Modifier.fillMaxWidth().padding(vertical=10.dp), color=SonHarfGold, fontWeight=FontWeight.Black, fontSize=13.sp, textAlign=TextAlign.Center)
                }
            } else if (words.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    items(words.takeLast(30)) { w ->
                        val duplicate = feedback?.duplicateWord?.equals(w.word, ignoreCase = true) == true
                        Surface(shape = RoundedCornerShape(12.dp), color = if (duplicate) SonHarfPink.copy(alpha=.22f) else SonHarfGold.copy(alpha=.12f), border = BorderStroke(if (duplicate) 2.dp else 1.dp, if (duplicate) SonHarfPink else SonHarfGold.copy(alpha=.5f))) {
                            Text(w.word.uppercase(), Modifier.padding(horizontal=12.dp, vertical=8.dp), color = if (duplicate) SonHarfPink else SonHarfText, fontSize = if (duplicate) 17.sp else 14.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            } else Text(sh("Kelime zinciri burada görünecek", "Word chain will appear here"), color = SonHarfMuted, fontSize = 13.sp)
        }''',
    "V10 VIP word chain",
)
v10 = required_replace(
    v10,
    'OutlinedTextField(chatInput, { chatInput = it.take(300) }, singleLine = true, modifier = Modifier.fillMaxWidth(), placeholder = { Text(sh("Mesaj yaz…", "Type a message…")) })',
    '''OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it.take(300) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(sh("Mesaj yaz…", "Type a message…")) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Send,
                            showKeyboardOnFocus = true,
                            hintLocales = LocaleList(Locale(if (active.language == "tr") "tr-TR" else "en-US")),
                        ),
                        keyboardActions = KeyboardActions(onSend = {
                            val message = chatInput.trim()
                            if (message.isNotEmpty()) scope.launch {
                                runCatching { backend.sendChat(active.id, message) }.onSuccess {
                                    chatInput = ""
                                    chat = runCatching { backend.getChat(active.id) }.getOrDefault(chat)
                                }
                            }
                        }),
                    )''',
    "V10 chat Android keyboard",
)
v10 = required_replace(
    v10,
    'Text(sh("● SOHBET", "● CHAT"), color=SonHarfCyan, fontWeight=FontWeight.Bold, fontSize=14.sp)',
    'Text(if (isVip) sh("● SOHBET", "● CHAT") else sh("🔒 SOHBET • VIP", "🔒 CHAT • VIP"), color=if (isVip) SonHarfCyan else SonHarfGold, fontWeight=FontWeight.Bold, fontSize=14.sp)',
    "V10 VIP chat label",
)
v10_path.write_text(v10)


# The standalone game initially used optional analytics helpers from development branches.
# Keep it build-safe on main; product analytics will be backed by the server migration.
bil_path = Path("app/src/main/java/com/sonharf/game/BilBakalimFeature.kt")
bil = bil_path.read_text()
bil = bil.replace("    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }\n", "")
bil = bil.replace('            runCatching { backend?.logEvent(if (playerWon == true) "bil_bakalim_round_win" else "bil_bakalim_round_loss") }\n', "")
bil = bil.replace('    LaunchedEffect(Unit) { runCatching { backend?.logEvent("bil_bakalim_open") } }\n', "")
bil = bil.replace("import com.sonharf.game.data.OnlineGameBackend\n", "")
bil = bil.replace("import com.sonharf.game.data.SupabaseProvider\n", "")
bil_path.write_text(bil)

print("Bil Bakalim integration applied")
