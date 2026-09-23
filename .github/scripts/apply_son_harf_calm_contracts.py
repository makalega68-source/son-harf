from pathlib import Path

ROOT = Path('.')


def replace_between(text: str, start: str, end: str, replacement: str) -> str:
    i = text.index(start)
    j = text.index(end, i)
    return text[:i] + replacement.rstrip() + "\n\n" + text[j:]

# 1) Centralize the mode-specific calm palette in the shared design system.
design_path = ROOT / 'app/src/main/java/com/sonharf/game/GameDesignSystem.kt'
design = design_path.read_text(encoding='utf-8')
marker = '    val Disabled = Color(0xFF2A3544)\n'
son_harf_tokens = '''    // Son Harf uses a quieter, warm arena palette while remaining inside the shared design system.\n    val SonHarfBackground = Color(0xFFF3EEE5)\n    val SonHarfSurface = Color(0xFFFFFBF4)\n    val SonHarfInk = Color(0xFF173247)\n    val SonHarfMuted = Color(0xFF6F7B7C)\n    val SonHarfOcean = Color(0xFF4F8F96)\n    val SonHarfOceanDeep = Color(0xFF2F6970)\n    val SonHarfSky = Color(0xFF8EB7B5)\n    val SonHarfIce = Color(0xFFE6EFEB)\n    val SonHarfBorder = Color(0xFFD8D0C4)\n    val SonHarfGreen = Color(0xFF789B73)\n    val SonHarfGreenSoft = Color(0xFFE5ECDD)\n    val SonHarfRed = Color(0xFFC86459)\n    val SonHarfRedSoft = Color(0xFFF4DDD7)\n    val SonHarfGold = Color(0xFFD1A13E)\n    val SonHarfGoldSoft = Color(0xFFF5E8BB)\n    val SonHarfRival = Color(0xFFD27869)\n    val SonHarfRivalSoft = Color(0xFFF6E2DC)\n\n'''
if 'val SonHarfBackground' not in design:
    design = design.replace(marker, son_harf_tokens + marker)
design_path.write_text(design, encoding='utf-8')

# 2) Make PremierUi consume shared GameColors tokens rather than owning a second palette source.
premier_path = ROOT / 'app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt'
premier = premier_path.read_text(encoding='utf-8')
palette_start = 'private object PremierUi {'
palette_end = 'private fun pt'
palette = '''private object PremierUi {\n    val Background = GameColors.SonHarfBackground\n    val Surface = GameColors.SonHarfSurface\n    val Ink = GameColors.SonHarfInk\n    val Muted = GameColors.SonHarfMuted\n    val Ocean = GameColors.SonHarfOcean\n    val OceanDeep = GameColors.SonHarfOceanDeep\n    val Sky = GameColors.SonHarfSky\n    val Ice = GameColors.SonHarfIce\n    val Border = GameColors.SonHarfBorder\n    val Green = GameColors.SonHarfGreen\n    val GreenSoft = GameColors.SonHarfGreenSoft\n    val Red = GameColors.SonHarfRed\n    val RedSoft = GameColors.SonHarfRedSoft\n    val Gold = GameColors.SonHarfGold\n    val GoldSoft = GameColors.SonHarfGoldSoft\n    val Rival = GameColors.SonHarfRival\n    val RivalSoft = GameColors.SonHarfRivalSoft\n}'''
premier = replace_between(premier, palette_start, palette_end, palette)
premier_path.write_text(premier, encoding='utf-8')

# 3) Update static regression contracts to the approved Son Harf production UX.
ux_path = ROOT / 'app/src/test/java/com/sonharf/game/PremierDuelUxRegressionTest.kt'
ux = ux_path.read_text(encoding='utf-8')
new_ux_test = r'''    @Test fun premierArenaKeepsProfilesVisibleAndServerAuthoritativeRecovery() {
        val screen = File("src/main/java/com/sonharf/game/PremierWordDuelScreen.kt").readText()
        val backend = File("src/main/java/com/sonharf/game/data/PremierDuelBackend.kt").readText()
        val onlineBackend = File("src/main/java/com/sonharf/game/data/OnlineGameBackend.kt").readText()
        val turnClock = File("src/main/java/com/sonharf/game/data/PremierTurnClock.kt").readText()

        // Real player profiles remain visible, but Son Harf has no level/XP badge.
        assertTrue(screen.contains("PremierCalmPlayerCard("))
        assertTrue(screen.contains("ProfilePhotoAvatarRectWithGender("))
        assertTrue(screen.contains("nameColor = SonHarfCosmetics.playerNameColor"))
        assertFalse(screen.contains("MageCatCompanion("))
        assertFalse(screen.contains("SyntheticBotPortrait("))
        assertFalse(screen.contains("Seviye"))

        // Chat remains typed/realtime and keeps the unread red indicator.
        assertTrue(screen.contains("Text(pt(language, \"Sohbet\", \"Chat\")"))
        assertFalse(screen.contains("enabled = !room.isBot"))
        assertTrue(screen.contains("var hasUnreadChat by remember { mutableStateOf(false) }"))
        assertTrue(screen.contains("if (latest != null && latest.id != previousId && latest.senderId != backend.currentUserId())"))
        assertTrue(screen.contains("hasUnreadChat = !showQuickChat"))
        assertTrue(screen.contains("unreadChat = hasUnreadChat"))
        assertTrue(screen.contains("if (unreadChat)"))
        assertTrue(screen.contains("background(PremierUi.Red)"))
        assertTrue(screen.contains("hasUnreadChat = false"))

        // Server-authoritative gameplay and recovery paths are unchanged.
        assertTrue(screen.contains("turnSeconds = 1"))
        assertTrue(screen.contains("backend.claimTurnTimeout(active.id)"))
        assertTrue(screen.contains("backend.botTakeTurn(active.id)"))
        assertTrue(screen.contains("backend.submitPremierWord(active.id, candidate)"))
        assertFalse(screen.contains("backend.validateCoreWordDetailed(candidate"))
        assertFalse(screen.contains("timeoutClaimKey"))
        assertTrue(backend.contains("submitWord(roomId, word)"))
        assertTrue(backend.contains("getRoom(roomId)"))
        assertTrue(backend.contains("botTakeTurn(roomId)"))
        assertFalse(backend.contains("submit_word_v4"))

        assertTrue(screen.contains("found?.isBot == true && found.isPremierLive()"))
        assertTrue(screen.contains("backend.resumePremierBotMatch(found.id)"))
        assertTrue(onlineBackend.contains("suspend fun resumePremierBotMatch(roomId: String): GameRoomDto"))
        assertTrue(onlineBackend.contains("\"resume_premier_bot_match_v1\""))
        assertTrue(onlineBackend.contains("put(\"p_room_id\", roomId)"))

        // Visible timer remains server-clock anchored and monotonic.
        assertTrue(screen.contains("private const val PREMIER_TURN_SECONDS = 15"))
        assertTrue(screen.contains("fetchPremierTurnClock(active.id)"))
        assertTrue(screen.contains("SystemClock.elapsedRealtime()"))
        assertTrue(screen.contains("premierRemainingTurnSecondsFromMillis(initialRemainingMs - elapsedMs)"))
        assertTrue(turnClock.contains("data class PremierTurnClockDto"))
        assertTrue(turnClock.contains("\"get_premier_turn_clock_v1\""))
        assertTrue(turnClock.contains("put(\"p_room_id\", roomId)"))

        // Approved best-of-three / 10-word round HUD is bound to live room state.
        assertTrue(screen.contains("Raund ${room.roundNo} / 3"))
        assertTrue(screen.contains("2 raund kazanan"))
        assertTrue(screen.contains("room.roundWordCount.coerceIn(0, 10)"))
        assertTrue(screen.contains("Raund Puanı"))
        assertTrue(screen.contains("Text(\"$myRounds - $rivalRounds\""))

        // Latest word is always playable context; history/found-word panels stay present but PRO-gated.
        assertTrue(screen.contains("val lastWord = words.lastOrNull()"))
        assertTrue(screen.contains("PremierLastWordBar(language = language, word = lastWord, meId = meId)"))
        assertTrue(screen.contains("private fun PremierProWordPanel("))
        assertTrue(screen.contains("Bulunan Kelimeler"))
        assertTrue(screen.contains("Kelime Geçmişi"))
        assertTrue(screen.contains("Sadece PRO üyeler görebilir"))

        // Target card has no previous/next arrow controls and remains compact on real devices.
        assertTrue(screen.contains("val targetSize = if (compact) 94.dp else 116.dp"))
        val targetStart = screen.indexOf("private fun PremierTargetCard(")
        val targetEnd = screen.indexOf("private fun PremierProWordPanel(", targetStart)
        assertTrue(targetStart >= 0 && targetEnd > targetStart)
        val target = screen.substring(targetStart, targetEnd)
        assertFalse(target.contains("ChevronLeft"))
        assertFalse(target.contains("ChevronRight"))
        assertFalse(target.contains("ArrowLeft"))
        assertFalse(target.contains("ArrowRight"))

        // Native Android IME owns the keyboard and Done action; there is no arena custom-keyboard call.
        assertTrue(screen.contains("LocalSoftwareKeyboardController.current"))
        assertTrue(screen.contains("KeyboardOptions("))
        assertTrue(screen.contains("ImeAction.Done"))
        assertTrue(screen.contains("onDone = { if (enabled && input.isNotBlank()) onSubmit() }"))
        val arenaStart = screen.indexOf("private fun PremierArena(")
        val headerStart = screen.indexOf("private fun PremierArenaHeader(", arenaStart)
        val arena = screen.substring(arenaStart, headerStart)
        assertFalse(arena.contains("PremierKeyboard("))

        // Purchased action VFX stays cosmetic on turn arrival and accepted moves.
        assertTrue(screen.contains("PurchasedVictoryVfx("))
        assertTrue(screen.contains("eventKey = \"turn:"))
        assertTrue(screen.contains("eventKey = \"accepted:"))

        // Send still clears the visible attempt before the authoritative server result arrives.
        val candidateIndex = screen.indexOf("val candidate = input")
        val clearIndex = screen.indexOf("input = \"\"", candidateIndex)
        val submitIndex = screen.indexOf("backend.submitPremierWord(active.id, candidate)", candidateIndex)
        assertTrue(candidateIndex >= 0)
        assertTrue(clearIndex > candidateIndex)
        assertTrue(submitIndex > clearIndex)
        assertTrue(screen.contains("val accepted = next.validWordCount > active.validWordCount"))
        assertTrue(screen.contains("pt(language, \"DOĞRU\", \"CORRECT\")"))
        assertTrue(screen.contains("pt(language, \"YANLIŞ\", \"WRONG\")"))
        assertTrue(screen.contains("PremierMoveFeedback("))
        assertFalse(screen.contains("PremierStatPill(pt(language, \"SUNUCU\", \"SERVER\")"))

        // Chat is a typed transcript for human and bot matches; canned quick-message UI stays gone.
        assertTrue(screen.contains("private fun PremierChatSheet("))
        assertTrue(screen.contains("messages = if (room?.isBot == true) botChat else chat"))
        assertTrue(screen.contains("OutlinedTextField("))
        assertTrue(screen.contains("\"Mesaj yaz…\""))
        assertTrue(screen.contains("backend.sendChat(active.id, message)"))
        assertTrue(screen.contains("backend.getChat(active.id)"))
        assertTrue(screen.contains("premierBotChatReply(language, message)"))
        assertTrue(screen.contains("Bot ile serbestçe yazış."))
        assertFalse(screen.contains("quickMessages"))
        assertFalse(screen.contains("Hızlı reaksiyonlar"))
        assertFalse(screen.contains("Bot maçında gerçek mesajlaşma kapalıdır."))
    }'''
ux = replace_between(ux, '    @Test fun premierArenaKeepsProfilesVisibleAndServerAuthoritativeRecovery() {', '    @Test fun serverAnchoredCountdownRoundsUpWithoutSkippingSeconds()', new_ux_test)
ux_path.write_text(ux, encoding='utf-8')

cos_path = ROOT / 'app/src/test/java/com/sonharf/game/PremiumCosmeticApplicationContractTest.kt'
cos = cos_path.read_text(encoding='utf-8')
old_cos = '''    @Test\n    fun premierMatchUsesTheEquippedKeyboardAndNameStyle() {\n        val premier = source("PremierWordDuelScreen.kt")\n\n        assertTrue(premier.contains("val palette = SonHarfCosmetics.keyboardPalette"))\n        assertTrue(premier.contains("color = palette.background"))\n        assertTrue(premier.contains("nameColor = SonHarfCosmetics.playerNameColor"))\n    }'''
new_cos = '''    @Test\n    fun premierMatchUsesNativeAndroidImeAndEquippedNameStyle() {\n        val premier = source("PremierWordDuelScreen.kt")\n\n        assertTrue(premier.contains("LocalSoftwareKeyboardController.current"))\n        assertTrue(premier.contains("ImeAction.Done"))\n        assertTrue(premier.contains("keyboardController?.show()"))\n        assertTrue(premier.contains("nameColor = SonHarfCosmetics.playerNameColor"))\n    }'''
if old_cos not in cos:
    raise SystemExit('Premium cosmetic test marker not found')
cos_path.write_text(cos.replace(old_cos, new_cos, 1), encoding='utf-8')

pro_path = ROOT / 'app/src/test/java/com/sonharf/game/PremiumStoreProContractTest.kt'
pro = pro_path.read_text(encoding='utf-8')
old_pro = '''        val screen = repoFile("app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt").readText()\n        assertTrue(screen.contains("PRO • Tüm oynanan kelimeler"))\n        assertTrue(screen.contains("val ordered = if (isPro) words.reversed()"))'''
new_pro = '''        val screen = repoFile("app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt").readText()\n        assertTrue(screen.contains("val isPro = me?.isVip == true"))\n        assertTrue(screen.contains("private fun PremierProWordPanel("))\n        assertTrue(screen.contains("title = pt(language, \\"Bulunan Kelimeler\\", \\"Found Words\\")"))\n        assertTrue(screen.contains("title = pt(language, \\"Kelime Geçmişi\\", \\"Word History\\")"))\n        assertTrue(screen.contains("if (!isPro)"))\n        assertTrue(screen.contains("Sadece PRO üyeler görebilir"))\n        assertTrue(screen.contains("val lastWord = words.lastOrNull()"))\n        assertTrue(screen.contains("PremierLastWordBar(language = language, word = lastWord, meId = meId)"))'''
if old_pro not in pro:
    raise SystemExit('Premium PRO test marker not found')
pro_path.write_text(pro.replace(old_pro, new_pro, 1), encoding='utf-8')

theme_path = ROOT / 'app/src/test/java/com/sonharf/game/UnifiedThemeSourceContractTest.kt'
theme = theme_path.read_text(encoding='utf-8')
old_theme_block = '''        assertTrue(premier.contains("val Background = GameColors.AppBackground"))\n        assertTrue(premier.contains("val Surface = GameColors.PrimarySurface"))\n        assertTrue(premier.contains("val Ink = GameColors.TextPrimary"))\n        assertTrue(premier.contains("val Ocean = GameColors.PrimaryBlue"))\n        assertTrue(premier.contains("val Sky = GameColors.TacticalTurquoise"))\n        assertTrue(premier.contains("val Green = GameColors.PlayGreen"))\n        assertTrue(premier.contains("val Red = GameColors.Danger"))\n        assertTrue(premier.contains("Brush.verticalGradient(listOf(PremierUi.Surface, PremierUi.Background))"))\n        assertFalse(premier.contains("val Background = Color(0xFFF1F5F2)"))\n        assertFalse(premier.contains("val Surface = Color(0xFFFFFDF7)"))\n        assertFalse(premier.contains("MageCatCompanion("))'''
new_theme_block = '''        assertTrue(design.contains("val SonHarfBackground = Color(0xFFF3EEE5)"))\n        assertTrue(design.contains("val SonHarfSurface = Color(0xFFFFFBF4)"))\n        assertTrue(design.contains("val SonHarfInk = Color(0xFF173247)"))\n        assertTrue(design.contains("val SonHarfOcean = Color(0xFF4F8F96)"))\n        assertTrue(design.contains("val SonHarfGreen = Color(0xFF789B73)"))\n        assertTrue(design.contains("val SonHarfRival = Color(0xFFD27869)"))\n\n        assertTrue(premier.contains("val Background = GameColors.SonHarfBackground"))\n        assertTrue(premier.contains("val Surface = GameColors.SonHarfSurface"))\n        assertTrue(premier.contains("val Ink = GameColors.SonHarfInk"))\n        assertTrue(premier.contains("val Ocean = GameColors.SonHarfOcean"))\n        assertTrue(premier.contains("val Sky = GameColors.SonHarfSky"))\n        assertTrue(premier.contains("val Green = GameColors.SonHarfGreen"))\n        assertTrue(premier.contains("val Red = GameColors.SonHarfRed"))\n        assertTrue(premier.contains("Brush.verticalGradient(listOf(PremierUi.Surface, PremierUi.Background))"))\n        assertFalse(premier.contains("val Background = Color(0xFFF3EEE5)"))\n        assertFalse(premier.contains("val Surface = Color(0xFFFFFBF4)"))\n        assertFalse(premier.contains("MageCatCompanion("))'''
if old_theme_block not in theme:
    raise SystemExit('Unified theme test marker not found')
theme_path.write_text(theme.replace(old_theme_block, new_theme_block, 1), encoding='utf-8')

print('Son Harf design tokens and updated regression contracts applied')
