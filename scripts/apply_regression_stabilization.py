from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def patch(path: str, old: str, new: str) -> None:
    p = ROOT / path
    text = p.read_text(encoding="utf-8")
    if new in text:
        print(f"already patched {path}")
        return
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one old match in {path}, found {count}: {old[:80]!r}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")
    print(f"patched {path}")


# 1) Permanently mount the regression guard above the app shell.
patch(
    "app/src/main/java/com/sonharf/game/MainActivity.kt",
    "                    ClassicPremiumApp()\n                    WinnerFireworkOverlay()",
    "                    ClassicPremiumApp()\n                    RegressionGuardOverlay()\n                    WinnerFireworkOverlay()",
)

# 2) Profile must use the shared app theme, not an old hard-coded dark gradient.
patch(
    "app/src/main/java/com/sonharf/game/CompleteProfileScreen.kt",
    "Brush.verticalGradient(listOf(Color(0xFF040717), SonHarfBg, Color(0xFF060A18)))",
    "Brush.verticalGradient(listOf(SonHarfBg, SonHarfSurface2, SonHarfBg))",
)
patch(
    "app/src/main/java/com/sonharf/game/CompleteProfileScreen.kt",
    "ScrollableTabRow(selectedTabIndex = tab, edgePadding = 10.dp, containerColor = Color.Transparent, divider = {})",
    "ScrollableTabRow(selectedTabIndex = tab, edgePadding = 10.dp, containerColor = SonHarfBg, divider = {})",
)

# 3) League must use the same shared theme tokens.
patch(
    "app/src/main/java/com/sonharf/game/LeaderboardExperience.kt",
    "Brush.verticalGradient(listOf(Color(0xFF040717), SonHarfBg, Color(0xFF06091A)))",
    "Brush.verticalGradient(listOf(SonHarfBg, SonHarfSurface2, SonHarfBg))",
)
patch(
    "app/src/main/java/com/sonharf/game/LeaderboardExperience.kt",
    "Brush.radialGradient(listOf(SonHarfPurple.copy(alpha = .28f), SonHarfSurface, Color(0xFF050918)))",
    "Brush.radialGradient(listOf(SonHarfCyan.copy(alpha = .18f), SonHarfSurface2, SonHarfSurface))",
)
patch(
    "app/src/main/java/com/sonharf/game/LeaderboardExperience.kt",
    "trackColor = Color(0xFF1A2440),",
    "trackColor = SonHarfMuted.copy(alpha = .16f),",
)

# 4) Approved gender badge contract: female pink, male blue, beside the profile photo.
patch(
    "app/src/main/java/com/sonharf/game/ProfileExperienceV2.kt",
    '''                    ) { Box(contentAlignment = Alignment.Center) { Text("✎", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black) } }\n                }\n                Spacer(Modifier.height(10.dp))''',
    '''                    ) { Box(contentAlignment = Alignment.Center) { Text("✎", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black) } }\n                    p?.gender?.let { gender ->\n                        val female = gender.trim().lowercase() in setOf("kadın", "kadin", "female", "woman")\n                        val male = gender.trim().lowercase() in setOf("erkek", "male", "man")\n                        if (female || male) {\n                            Surface(\n                                modifier = Modifier.align(Alignment.BottomStart).size(42.dp),\n                                shape = CircleShape,\n                                color = if (female) Color(0xFFFF76A8) else Color(0xFF439EF2),\n                                border = BorderStroke(2.dp, Color.White),\n                                shadowElevation = 3.dp,\n                            ) {\n                                Box(contentAlignment = Alignment.Center) {\n                                    Text(if (female) "♀" else "♂", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)\n                                }\n                            }\n                        }\n                    }\n                }\n                Spacer(Modifier.height(10.dp))''',
)

# 5) Never leave the finished arena as an empty/blank screen.
patch(
    "app/src/main/java/com/sonharf/game/TargetNeonGameScreen.kt",
    '''    if (room.status == "finished") {\n        // The persistent ComboOverlayV9 is the single match-result owner.\n        // Keep the underlying arena neutral so two result UIs never overlap.\n        Box(Modifier.fillMaxSize().background(TGbg))\n        return\n    }''',
    '''    if (room.status == "finished") {\n        Box(Modifier.fillMaxSize().background(TGbg), contentAlignment = Alignment.Center) {\n            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {\n                Text(sh("MAÇ TAMAMLANDI", "MATCH FINISHED"), color = TGtext, fontWeight = FontWeight.Black, fontSize = 24.sp)\n                Text(sh("Sonuç özeti açılmazsa ana menüye güvenle dönebilirsin.", "If the result summary does not open, you can safely return home."), color = TGmuted, fontSize = 12.sp, textAlign = TextAlign.Center)\n                Button(onClick = onExit, colors = ButtonDefaults.buttonColors(containerColor = TGcyan)) {\n                    Text(sh("ANA MENÜ", "HOME"), color = Color.White, fontWeight = FontWeight.Black)\n                }\n            }\n        }\n        return\n    }''',
)

# 6) Submission errors must not erase the user's word; clear only after confirmed success.
patch(
    "app/src/main/java/com/sonharf/game/TargetNeonGameScreen.kt",
    '''                        val submitted = wordInput.trim(); if (submitted.isBlank()) return@launch\n                        wordInput = ""; busy = true\n                        runCatching { backend.submitWord(active.id, submitted) }\n                            .onSuccess { room = it; notice = if (it.lastEventPlayerId == me && it.lastEvent != null && it.lastEvent != "word_accepted") friendly(it.lastEvent ?: "") else "${submitted.uppercase()} kabul edildi" }\n                            .onFailure { notice = friendly(it.message.orEmpty()) }\n                        busy = false''',
    '''                        val submitted = wordInput.trim(); if (submitted.isBlank()) return@launch\n                        busy = true\n                        runCatching { backend.submitWord(active.id, submitted) }\n                            .onSuccess {\n                                room = it\n                                wordInput = ""\n                                notice = if (it.lastEventPlayerId == me && it.lastEvent != null && it.lastEvent != "word_accepted") friendly(it.lastEvent ?: "") else "${submitted.uppercase()} kabul edildi"\n                            }\n                            .onFailure { notice = friendly(it.message.orEmpty()) }\n                        busy = false''',
)

# 7) Explicit surrender must never strand the user on the finished arena.
patch(
    "app/src/main/java/com/sonharf/game/TargetNeonGameScreen.kt",
    '''                onForfeit = { scope.launch { runCatching { backend.forfeit(active.id) }.onSuccess { room = it } } },''',
    '''                onForfeit = {\n                    scope.launch {\n                        runCatching { backend.forfeit(active.id) }\n                            .onSuccess {\n                                room = it\n                                if (it.status == "finished") SonHarfUiState.homeRequest += 1\n                            }\n                            .onFailure { notice = friendly(it.message.orEmpty()) }\n                    }\n                },''',
)

# 8) Never resume a stale active room from the wrong language when a same-language room exists.
patch(
    "app/src/main/java/com/sonharf/game/TargetNeonGameScreen.kt",
    '''        return SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>()\n            .filter { (it.hostId == me || it.guestId == me) && it.status in listOf("waiting", "playing", "quiz", "final", "sudden_death", "paused") }\n            .maxByOrNull { it.validWordCount }''',
    '''        val candidates = SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>()\n            .filter { (it.hostId == me || it.guestId == me) && it.status in listOf("waiting", "playing", "quiz", "final", "sudden_death", "paused") }\n        val sameLanguage = candidates.filter { it.language == language }\n        return (sameLanguage.ifEmpty { candidates }).maxByOrNull { it.validWordCount }''',
)

# 9) Synchronize the global language whenever a room is resumed, matched or joined.
patch(
    "app/src/main/java/com/sonharf/game/TargetNeonGameScreen.kt",
    '''            if (old != null) { room = old; language = old.language; observe(old) }''',
    '''            if (old != null) { room = old; language = old.language; SonHarfUiState.language = old.language; observe(old) }''',
)
patch(
    "app/src/main/java/com/sonharf/game/TargetNeonGameScreen.kt",
    '''                                if (found != null) { room = found; language = found.language; observe(found); SonHarfSoundFx.softNotify(); break }''',
    '''                                if (found != null) { room = found; language = found.language; SonHarfUiState.language = found.language; observe(found); SonHarfSoundFx.softNotify(); break }''',
)
patch(
    "app/src/main/java/com/sonharf/game/TargetNeonGameScreen.kt",
    '''                onJoin = { scope.launch { busy = true; runCatching { backend.joinPrivateRoom(privateCode) }.onSuccess { room = it; language = it.language; observe(it) }.onFailure { notice = friendly(it.message.orEmpty()) }; busy = false } },''',
    '''                onJoin = { scope.launch { busy = true; runCatching { backend.joinPrivateRoom(privateCode) }.onSuccess { room = it; language = it.language; SonHarfUiState.language = it.language; observe(it) }.onFailure { notice = friendly(it.message.orEmpty()) }; busy = false } },''',
)

# 10) The keyboard/chat guard must follow the selected language and cannot attach to a stale other-language room.
patch(
    "app/src/main/java/com/sonharf/game/RegressionGuardOverlay.kt",
    '''                val active = runCatching {\n                    SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>()\n                        .filter {\n                            (it.hostId == me || it.guestId == me) &&\n                                it.status in listOf("playing", "final", "sudden_death")\n                        }\n                        .maxByOrNull { it.validWordCount }\n                }.getOrNull()''',
    '''                val active = runCatching {\n                    val candidates = SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>()\n                        .filter {\n                            (it.hostId == me || it.guestId == me) &&\n                                it.status in listOf("playing", "final", "sudden_death")\n                        }\n                    val sameLanguage = candidates.filter { it.language == SonHarfUiState.language }\n                    (sameLanguage.ifEmpty { candidates }).maxByOrNull { it.validWordCount }\n                }.getOrNull()''',
)

print("All regression-stabilization patches applied successfully.")
