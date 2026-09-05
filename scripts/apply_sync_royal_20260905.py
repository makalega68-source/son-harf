from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
AUTH = ROOT / "app/src/main/java/com/sonharf/game/ClassicServerAuthority.kt"
ONLINE = ROOT / "app/src/main/java/com/sonharf/game/OnlineGameScreenV6.kt"
DUEL = ROOT / "app/src/main/java/com/sonharf/game/LightDuelUi.kt"
STYLE = ROOT / "app/src/main/java/com/sonharf/game/PurchasedStyleUi.kt"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


def patch_authority() -> None:
    text = AUTH.read_text(encoding="utf-8")
    text = replace_once(
        text,
        '''        room.currentPlayerId.orEmpty(),
        room.turnDeadline.orEmpty(),
    ).joinToString("|")''',
        '''        room.currentPlayerId.orEmpty(),
        room.turnDeadline.orEmpty(),
        room.lastEvent.orEmpty(),
        room.lastEventPlayerId.orEmpty(),
        room.hostScore.toString(),
        room.guestScore.toString(),
    ).joinToString("|")''',
        "timeout event key authority fields",
    )
    AUTH.write_text(text, encoding="utf-8")


def patch_online() -> None:
    text = ONLINE.read_text(encoding="utf-8")
    text = replace_once(
        text,
        '''        LaunchedEffect(active.id) { while (true) { if (!active.isBot && active.status != "waiting") runCatching { backend.heartbeatRoom(active.id) }.onSuccess { room = it }; delay(5000) } }''',
        '''        LaunchedEffect(active.id) {
            while (true) {
                if (!active.isBot && active.status != "waiting") {
                    runCatching { backend.heartbeatRoom(active.id) }
                        .onSuccess { updated ->
                            acceptServerRoom(updated)
                            val claim = timeoutClaimKey
                            if (claim != null && classicDeadlineEventKey(updated) != claim) {
                                timeoutClaimKey = null
                                if (notice.contains("Senkronize ediliyor", true) || notice.contains("Synchronizing", true)) {
                                    notice = sh("Bağlantı aktif.", "Connection active.")
                                }
                            }
                        }
                }
                delay(5000)
            }
        }''',
        "heartbeat accepts server authority safely",
    )
    text = replace_once(
        text,
        '''            onTimeout = {
                scope.launch {
                    val claimKey = classicDeadlineEventKey(active)
                    if (timeoutClaimKey == claimKey) return@launch
                    timeoutClaimKey = claimKey
                    notice = sh("Senkronize ediliyor…", "Synchronizing…")
                    runCatching { backend.claimTurnTimeout(active.id) }
                        .onSuccess { acceptServerRoom(it) }
                        .onFailure { notice = friendly(it.message.orEmpty()) }
                }
            },''',
        '''            onTimeout = {
                scope.launch {
                    val claimKey = classicDeadlineEventKey(active)
                    if (timeoutClaimKey == claimKey) return@launch
                    timeoutClaimKey = claimKey
                    notice = sh("Senkronize ediliyor…", "Synchronizing…")

                    var lastFailure: Throwable? = null
                    repeat(12) { attempt ->
                        val latestBefore = room ?: return@launch
                        if (latestBefore.id != active.id || classicDeadlineEventKey(latestBefore) != claimKey) {
                            timeoutClaimKey = null
                            return@launch
                        }

                        val refresh = runCatching {
                            if (attempt == 0 || attempt == 4 || attempt == 8) {
                                backend.claimTurnTimeout(active.id)
                            } else {
                                backend.getRoom(active.id)
                            }
                        }
                        refresh.onSuccess { updated ->
                            acceptServerRoom(updated)
                            if (updated.status == "finished" || classicDeadlineEventKey(updated) != claimKey) {
                                timeoutClaimKey = null
                                if (notice.contains("Senkronize ediliyor", true) || notice.contains("Synchronizing", true)) {
                                    notice = sh("Bağlantı aktif.", "Connection active.")
                                }
                                return@launch
                            }
                        }.onFailure { lastFailure = it }
                        delay(650L)
                    }

                    timeoutClaimKey = null
                    val newest = runCatching { backend.getRoom(active.id) }.getOrNull()
                    if (newest != null) acceptServerRoom(newest)
                    if (newest == null || classicDeadlineEventKey(newest) == claimKey) {
                        notice = lastFailure?.let { friendly(it.message.orEmpty()) }
                            ?: sh("Bağlantı yenileniyor. Tekrar deneniyor…", "Reconnecting. Retrying…")
                    }
                }
            },''',
        "bounded timeout recovery",
    )
    ONLINE.write_text(text, encoding="utf-8")


def patch_duel() -> None:
    text = DUEL.read_text(encoding="utf-8")
    text = replace_once(
        text,
        '''    LaunchedEffect(room.turnDeadline, room.currentPlayerId, room.validWordCount, room.roundNo, room.status) {
        val currentKey = classicDeadlineEventKey(room)
        if (timeoutSignalKey != null && timeoutSignalKey != currentKey) {
            timerSynchronizing = false
        }
    }''',
        '''    LaunchedEffect(
        room.turnDeadline,
        room.currentPlayerId,
        room.validWordCount,
        room.roundNo,
        room.status,
        room.lastEvent,
        room.lastEventPlayerId,
        room.hostScore,
        room.guestScore,
    ) {
        val currentKey = classicDeadlineEventKey(room)
        if (timeoutSignalKey != null && timeoutSignalKey != currentKey) {
            timerSynchronizing = false
            timeoutSignalKey = null
        }
    }''',
        "timer synchronization reset on authoritative snapshot movement",
    )
    DUEL.write_text(text, encoding="utf-8")


def patch_style() -> None:
    text = STYLE.read_text(encoding="utf-8")
    replacements = {
        'PurchasedFrameSpec(PurchasedFrameCatalog.RED, "Kırmızı Hat", "Red Line", "Sade başlangıç ve günlük kullanım çerçevesi", "Clean starter and everyday frame", R.drawable.style_frame_red, Color(0xFFD84C4C), "SIRADAN", "STANDARD", R.drawable.style_icon_user)':
        'PurchasedFrameSpec(PurchasedFrameCatalog.RED, "Royal Ruby", "Royal Ruby", "Yakut tonlu Royal Collection profil çerçevesi", "Ruby Royal Collection profile frame", R.drawable.style_frame_red, Color(0xFFD84C4C), "SIRADAN", "STANDARD", R.drawable.style_icon_user)',
        'PurchasedFrameSpec(PurchasedFrameCatalog.GREEN, "Zümrüt Hat", "Emerald Line", "Dengeli zümrüt profil çerçevesi", "Balanced emerald profile frame", R.drawable.style_frame_green, Color(0xFF2FAE68), "MAĞAZA", "SHOP", R.drawable.style_icon_coin)':
        'PurchasedFrameSpec(PurchasedFrameCatalog.GREEN, "Royal Emerald", "Royal Emerald", "Zümrüt tonlu Royal Collection profil çerçevesi", "Emerald Royal Collection profile frame", R.drawable.style_frame_green, Color(0xFF2FAE68), "MAĞAZA", "SHOP", R.drawable.style_icon_coin)',
        'PurchasedFrameSpec(PurchasedFrameCatalog.MINT, "Buz Mint", "Ice Mint", "Temiz ve modern mint çerçeve", "Clean modern mint frame", R.drawable.style_frame_mint, Color(0xFF32BFB3), "MAĞAZA", "SHOP", R.drawable.style_icon_coin)':
        'PurchasedFrameSpec(PurchasedFrameCatalog.MINT, "Royal Ice", "Royal Ice", "Buz tonlu Royal Collection profil çerçevesi", "Ice Royal Collection profile frame", R.drawable.style_frame_mint, Color(0xFF32BFB3), "MAĞAZA", "SHOP", R.drawable.style_icon_coin)',
        'PurchasedFrameSpec(PurchasedFrameCatalog.PURPLE, "Mor Spektrum", "Violet Spectrum", "Premium mor profil vurgusu", "Premium violet profile accent", R.drawable.style_frame_purple, Color(0xFF7257D8), "MAĞAZA", "SHOP", R.drawable.style_icon_coin)':
        'PurchasedFrameSpec(PurchasedFrameCatalog.PURPLE, "Royal Violet", "Royal Violet", "Mor tonlu Royal Collection profil çerçevesi", "Violet Royal Collection profile frame", R.drawable.style_frame_purple, Color(0xFF7257D8), "MAĞAZA", "SHOP", R.drawable.style_icon_coin)',
        'PurchasedFrameSpec(PurchasedFrameCatalog.GOLD, "Altın Hat", "Gold Line", "VIP ve prestij koleksiyonuna uygun metalik çerçeve", "Metallic frame for VIP and prestige collection", R.drawable.style_frame_gold, Color(0xFFD7A72E), "VIP / PREMIUM", "VIP / PREMIUM", R.drawable.style_icon_trophy)':
        'PurchasedFrameSpec(PurchasedFrameCatalog.GOLD, "Royal Gold", "Royal Gold", "Royal Collection premium altın profil çerçevesi", "Royal Collection premium gold profile frame", R.drawable.style_frame_gold, Color(0xFFD7A72E), "VIP / PREMIUM", "VIP / PREMIUM", R.drawable.style_icon_trophy)',
    }
    for old, new in replacements.items():
        text = replace_once(text, old, new, "Royal Collection catalog item")
    STYLE.write_text(text, encoding="utf-8")


def verify() -> None:
    auth = AUTH.read_text(encoding="utf-8")
    online = ONLINE.read_text(encoding="utf-8")
    duel = DUEL.read_text(encoding="utf-8")
    style = STYLE.read_text(encoding="utf-8")
    checks = {
        "event key includes timeout side effects": "room.lastEvent.orEmpty()" in auth and "room.hostScore.toString()" in auth,
        "timeout recovery bounded": "repeat(12) { attempt ->" in online,
        "timeout recovery reclaims": "attempt == 0 || attempt == 4 || attempt == 8" in online,
        "heartbeat uses authority filter": "acceptServerRoom(updated)" in online,
        "sync state reset fields": "room.lastEventPlayerId" in duel and "timeoutSignalKey = null" in duel,
        "royal ruby": "Royal Ruby" in style,
        "royal emerald": "Royal Emerald" in style,
        "royal ice": "Royal Ice" in style,
        "royal violet": "Royal Violet" in style,
        "royal gold": "Royal Gold" in style,
    }
    failed = [name for name, ok in checks.items() if not ok]
    if failed:
        raise RuntimeError("SYNC_ROYAL_VERIFY_FAILED: " + ", ".join(failed))
    print("SYNC_ROYAL_PATCH_VERIFY_PASS")


if __name__ == "__main__":
    patch_authority()
    patch_online()
    patch_duel()
    patch_style()
    verify()
