from pathlib import Path

GAME = Path('app/src/main/java/com/sonharf/game/SketchGameOverlayV10.kt')
SOUND = Path('app/src/main/java/com/sonharf/game/SonHarfSoundFx.kt')


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly one match, found {count}')
    return text.replace(old, new, 1)


game = GAME.read_text(encoding='utf-8')

# Haptics are local UI feedback only; no permission or gameplay-state mutation.
game = replace_once(
    game,
    'import androidx.compose.ui.graphics.Color\n',
    'import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.hapticfeedback.HapticFeedbackType\nimport androidx.compose.ui.platform.LocalHapticFeedback\n',
    'haptic imports',
)

# Free, rate-limited-by-human-input quick reaction; full chat remains VIP.
game = replace_once(
    game,
    '            onRematch = { scope.launch { runCatching { if (active.isBot) backend.restartBotMatch(active.id) else backend.requestRematch(active.id) }.onSuccess { room = it; words = emptyList(); input = ""; feedback = null; loadProfiles(it) } } },\n',
    '''            onReaction = { message ->\n                if (!active.isBot) scope.launch {\n                    runCatching { backend.sendChat(active.id, message) }\n                        .onSuccess { notice = sh("Tepki gönderildi", "Reaction sent"); SonHarfSoundFx.softNotify() }\n                        .onFailure { notice = sh("Tepki gönderilemedi", "Reaction could not be sent") }\n                }\n            },\n            onRematch = { scope.launch { runCatching { if (active.isBot) backend.restartBotMatch(active.id) else backend.requestRematch(active.id) }.onSuccess { room = it; words = emptyList(); input = ""; feedback = null; loadProfiles(it) } } },\n''',
    'reaction callback wiring',
)

game = replace_once(
    game,
    '    onChat: () -> Unit,\n    onRematch: () -> Unit,\n',
    '    onChat: () -> Unit,\n    onReaction: (String) -> Unit,\n    onRematch: () -> Unit,\n',
    'reaction callback signature',
)

# Match tension is derived entirely from real score / turn / timer state.
game = replace_once(
    game,
    '    var seconds by remember(room.turnDeadline) { mutableIntStateOf(45) }\n',
    '''    val scoreDelta = myScore - oppScore\n    val tensionLabel = when {\n        room.status == "sudden_death" -> sh("SON DÜELLO", "FINAL DUEL")\n        scoreDelta >= 3 -> sh("ÖNDESİN +$scoreDelta", "LEADING +$scoreDelta")\n        scoreDelta <= -3 -> sh("RAKİP ÖNDE ${-scoreDelta}", "OPPONENT +${-scoreDelta}")\n        else -> sh("BAŞA BAŞ", "NECK & NECK")\n    }\n    val haptics = LocalHapticFeedback.current\n    var seconds by remember(room.turnDeadline) { mutableIntStateOf(45) }\n''',
    'tension state',
)

game = replace_once(
    game,
    '            if (seconds in 1..10 && seconds != lastTick) { lastTick = seconds; SonHarfSoundFx.heartbeat() }\n',
    '''            if (seconds in 1..10 && seconds != lastTick) {\n                lastTick = seconds\n                SonHarfSoundFx.heartbeat()\n                if (myTurn) haptics.performHapticFeedback(HapticFeedbackType.LongPress)\n            }\n''',
    'critical heartbeat feedback',
)

# Accepted/rejected turns now produce deterministic feedback from actual server result.
game = replace_once(
    game,
    '''                            feedback = if (rejected) {\n                                val f = messageForEvent(result.lastEvent, submitted)\n                                if (result.lastEvent == "word_already_used" && duplicate != null) f.copy(duplicateWord = duplicate.word.uppercase()) else f\n                            } else messageForEvent(null, submitted)\n''',
    '''                            feedback = if (rejected) {\n                                val f = messageForEvent(result.lastEvent, submitted)\n                                if (result.lastEvent == "word_already_used" && duplicate != null) f.copy(duplicateWord = duplicate.word.uppercase()) else f\n                            } else messageForEvent(null, submitted)\n                            if (rejected) SonHarfSoundFx.warning() else SonHarfSoundFx.wordAccepted()\n''',
    'server result sound feedback',
)

game = replace_once(
    game,
    '''                                val f = failureFeedback(e.message.orEmpty(), submitted)\n                                feedback = if (f.duplicateWord != null && duplicate != null) f.copy(duplicateWord = duplicate.word.uppercase()) else f\n''',
    '''                                val f = failureFeedback(e.message.orEmpty(), submitted)\n                                feedback = if (f.duplicateWord != null && duplicate != null) f.copy(duplicateWord = duplicate.word.uppercase()) else f\n                                SonHarfSoundFx.warning()\n''',
    'failure sound feedback',
)

# Keep the same footprint: add competition context inside the existing header row.
game = replace_once(
    game,
    '''                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {\n                        Text(if (room.status == "sudden_death") sh("ANİ ÖLÜM", "SUDDEN DEATH") else "ROUND ${room.roundNo}/3", fontWeight = FontWeight.Black, fontSize = 16.sp)\n                        Text("${room.roundWordCount}/10", color = SonHarfCyan, fontWeight = FontWeight.Black, fontSize = 16.sp)\n                    }\n''',
    '''                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {\n                        Text(if (room.status == "sudden_death") sh("ANİ ÖLÜM", "SUDDEN DEATH") else "ROUND ${room.roundNo}/3", fontWeight = FontWeight.Black, fontSize = 15.sp, maxLines = 1)\n                        Text(tensionLabel, color = if (scoreDelta < 0) SonHarfPink else if (scoreDelta > 0) SonHarfGreen else SonHarfGold, fontWeight = FontWeight.Black, fontSize = 11.sp, maxLines = 1)\n                        Text("${room.roundWordCount}/10", color = SonHarfCyan, fontWeight = FontWeight.Black, fontSize = 15.sp, maxLines = 1)\n                    }\n''',
    'compact competition header',
)

# Critical phase uses the existing card border; no extra layer or layout growth.
game = replace_once(
    game,
    '            border = BorderStroke(1.dp, if (SonHarfCosmetics.auroraTheme) SonHarfPurple.copy(alpha=.3f) else SonHarfMuted.copy(alpha=.16f)),\n',
    '            border = BorderStroke(if (myTurn && seconds <= 10) 1.8.dp else 1.dp, if (myTurn && seconds <= 10) SonHarfPink.copy(alpha=.72f) else if (SonHarfCosmetics.auroraTheme) SonHarfPurple.copy(alpha=.3f) else SonHarfMuted.copy(alpha=.16f)),\n',
    'critical arena border',
)

# Social action fits inside the existing 44dp action row, preserving responsive height.
game = replace_once(
    game,
    '''        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {\n            OutlinedButton(onClick=onForfeit, modifier=Modifier.weight(1f).height(44.dp), border=BorderStroke(1.dp, SonHarfPink.copy(alpha=.55f))) { Text(sh("⚑ PES ET", "⚑ FORFEIT"), color=SonHarfPink, fontWeight=FontWeight.Bold, fontSize=14.sp) }\n            OutlinedButton(onClick=onChat, modifier=Modifier.weight(1f).height(44.dp), border=BorderStroke(1.dp, SonHarfCyan.copy(alpha=.55f))) { Text(if (isVip) sh("● SOHBET", "● CHAT") else sh("🔒 SOHBET • VIP", "🔒 CHAT • VIP"), color=if (isVip) SonHarfCyan else SonHarfGold, fontWeight=FontWeight.Bold, fontSize=14.sp) }\n        }\n''',
    '''        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {\n            OutlinedButton(onClick=onForfeit, modifier=Modifier.weight(1f).height(44.dp), contentPadding=PaddingValues(horizontal=3.dp), border=BorderStroke(1.dp, SonHarfPink.copy(alpha=.55f))) { Text(sh("⚑ PES", "⚑ QUIT"), color=SonHarfPink, fontWeight=FontWeight.Bold, fontSize=12.sp, maxLines=1) }\n            OutlinedButton(onClick={ onReaction(if (room.language == "tr") "👏 İyi kelime!" else "👏 Nice word!") }, enabled=!room.isBot, modifier=Modifier.weight(1.15f).height(44.dp), contentPadding=PaddingValues(horizontal=3.dp), border=BorderStroke(1.dp, SonHarfGold.copy(alpha=.6f))) { Text(sh("👏 İYİ KELİME", "👏 NICE WORD"), color=if (room.isBot) SonHarfMuted else SonHarfGold, fontWeight=FontWeight.Bold, fontSize=10.sp, maxLines=1) }\n            OutlinedButton(onClick=onChat, modifier=Modifier.weight(1.15f).height(44.dp), contentPadding=PaddingValues(horizontal=3.dp), border=BorderStroke(1.dp, SonHarfCyan.copy(alpha=.55f))) { Text(if (isVip) sh("● SOHBET", "● CHAT") else sh("🔒 CHAT • VIP", "🔒 CHAT • VIP"), color=if (isVip) SonHarfCyan else SonHarfGold, fontWeight=FontWeight.Bold, fontSize=11.sp, maxLines=1) }\n        }\n''',
    'compact social action row',
)

GAME.write_text(game, encoding='utf-8')

sound = SOUND.read_text(encoding='utf-8')
sound = replace_once(
    sound,
    '    fun heartbeat() { click(58, 0.15, 0.07); delayedClick(118, 46, 0.11, 0.05) }\n',
    '''    fun heartbeat() {\n        if (!enabled) return\n        Thread {\n            // Two soft low-frequency body thumps (lub-dub), shaped like a heartbeat rather than a digital click.\n            val durationSec = 0.34\n            val count = (SAMPLE_RATE * durationSec).toInt()\n            val pcm = ShortArray(count)\n            for (i in pcm.indices) {\n                val t = i.toDouble() / SAMPLE_RATE\n                var sample = 0.0\n                for ((start, gain) in arrayOf(0.018 to 0.17, 0.145 to 0.105)) {\n                    val x = t - start\n                    if (x in 0.0..0.14) {\n                        val attack = (x / 0.012).coerceIn(0.0, 1.0)\n                        val body = kotlin.math.sin(2.0 * Math.PI * 68.0 * x) * exp(-x * 24.0)\n                        val sub = kotlin.math.sin(2.0 * Math.PI * 42.0 * x) * exp(-x * 18.0)\n                        sample += (body * 0.72 + sub * 0.28) * attack * gain\n                    }\n                }\n                pcm[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()\n            }\n            play(pcm)\n        }.start()\n    }\n''',
    'realistic heartbeat synthesis',
)
SOUND.write_text(sound, encoding='utf-8')

print('Gameplay experience upgrade applied successfully.')
