from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / 'app/src/main/java/com/sonharf/game'

mascot = r'''package com.sonharf.game

import android.content.Context
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

internal enum class MascotEvent { GREETING, IDLE, GOOD_WORD, THINKING, CRITICAL, VICTORY, DEFEAT, ENCOURAGE }
internal enum class MascotMotion { IDLE, GREETING, THINKING, CRITICAL, VICTORY, DEFEAT }

internal data class MascotAnimationDef(
    val id: String,
    val motion: MascotMotion,
    val unlockLevel: Int,
    val trigger: MascotEvent,
)

internal object MascotAnimationRegistry {
    val core = listOf(
        MascotAnimationDef("idle", MascotMotion.IDLE, 1, MascotEvent.IDLE),
        MascotAnimationDef("greeting", MascotMotion.GREETING, 1, MascotEvent.GREETING),
        MascotAnimationDef("thinking", MascotMotion.THINKING, 1, MascotEvent.THINKING),
        MascotAnimationDef("critical", MascotMotion.CRITICAL, 1, MascotEvent.CRITICAL),
        MascotAnimationDef("victory", MascotMotion.VICTORY, 1, MascotEvent.VICTORY),
        MascotAnimationDef("defeat", MascotMotion.DEFEAT, 1, MascotEvent.DEFEAT),
    )
    // Future animation slots are intentionally data-driven: every 10 levels a new motion can be added
    // without changing game rules or creating pay-to-win power.
    fun nextUnlockLevel(level: Int): Int = ((level.coerceAtLeast(1) / 10) + 1) * 10
}

internal object MascotRuntime {
    var motion by mutableStateOf(MascotMotion.IDLE)
        private set
    var message by mutableStateOf("Hazırım.")
        private set
    var playerLevel by mutableIntStateOf(1)
        private set
    var playerXp by mutableIntStateOf(0)
        private set

    fun syncPlayerProgress(xp: Int, level: Int) {
        playerXp = xp.coerceAtLeast(0)
        playerLevel = level.coerceAtLeast(1)
    }

    fun react(event: MascotEvent, language: String = SonHarfUiState.language) {
        val tr = language != "en"
        motion = when (event) {
            MascotEvent.GREETING -> MascotMotion.GREETING
            MascotEvent.GOOD_WORD -> MascotMotion.VICTORY
            MascotEvent.THINKING -> MascotMotion.THINKING
            MascotEvent.CRITICAL -> MascotMotion.CRITICAL
            MascotEvent.VICTORY -> MascotMotion.VICTORY
            MascotEvent.DEFEAT -> MascotMotion.DEFEAT
            MascotEvent.ENCOURAGE -> MascotMotion.DEFEAT
            MascotEvent.IDLE -> MascotMotion.IDLE
        }
        message = MascotAiEngine.reply(event, tr, playerLevel, playerXp)
    }
}

/**
 * Lightweight on-device AI behavior layer. It observes game events, level and XP and selects
 * context-aware reactions. No remote API key is embedded in the APK; cloud LLM can later plug
 * into the same event interface without changing the UI or animation registry.
 */
internal object MascotAiEngine {
    fun reply(event: MascotEvent, tr: Boolean, level: Int, xp: Int): String {
        val next = MascotAnimationRegistry.nextUnlockLevel(level)
        return if (tr) when (event) {
            MascotEvent.GREETING -> "Buradayım. Hadi başlayalım!"
            MascotEvent.GOOD_WORD -> listOf("Güçlü kelime!", "Harika hamle!", "Devam, ritim sende!")[(xp + level) % 3]
            MascotEvent.THINKING -> "Bir saniye… en iyi devamı düşünüyorum."
            MascotEvent.CRITICAL -> "Süre daralıyor; son harfe odaklan!"
            MascotEvent.VICTORY -> "Kazandık! Müthiş oynadın."
            MascotEvent.DEFEAT -> "Yakındı. Bir sonraki maçta daha güçlüyüz."
            MascotEvent.ENCOURAGE -> "Sorun değil; yeni kelimeyi dene."
            MascotEvent.IDLE -> "Seviye $level • Yeni hareket hedefi: $next"
        } else when (event) {
            MascotEvent.GREETING -> "I'm here. Let's play!"
            MascotEvent.GOOD_WORD -> listOf("Strong word!", "Great move!", "Keep the rhythm!")[(xp + level) % 3]
            MascotEvent.THINKING -> "One moment… I'm thinking of the best continuation."
            MascotEvent.CRITICAL -> "Time is tight; focus on the last letter!"
            MascotEvent.VICTORY -> "We won! Great game."
            MascotEvent.DEFEAT -> "That was close. We'll be stronger next match."
            MascotEvent.ENCOURAGE -> "No problem; try another word."
            MascotEvent.IDLE -> "Level $level • Next motion target: $next"
        }
    }
}

private fun rawFor(motion: MascotMotion): Int = when (motion) {
    MascotMotion.IDLE -> R.raw.mascot_idle
    MascotMotion.GREETING -> R.raw.mascot_greeting
    MascotMotion.THINKING -> R.raw.mascot_thinking
    MascotMotion.CRITICAL -> R.raw.mascot_critical
    MascotMotion.VICTORY -> R.raw.mascot_victory
    MascotMotion.DEFEAT -> R.raw.mascot_defeat
}

private fun mascotName(context: Context): String =
    context.getSharedPreferences("son_harf_mascot", Context.MODE_PRIVATE).getString("name", "Dostum") ?: "Dostum"

private fun setMascotName(context: Context, value: String) {
    context.getSharedPreferences("son_harf_mascot", Context.MODE_PRIVATE).edit().putString("name", value.take(18)).apply()
}

@Composable
internal fun MascotIntegratedGameScreen() {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val railWidth = if (maxWidth < 420.dp) 86.dp else 104.dp
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxHeight()) {
                TargetNeonGameScreen()
                if (SonHarfGameModeState.mode == "expert") ExpertArenaOverlay() else SketchGameOverlayV9()
                ComboOverlayV9()
                BilBakalimBonusOverlay()
            }
            if (MascotPolicy.ENABLED) MascotRail(Modifier.width(railWidth).fillMaxHeight())
        }
    }
}

@Composable
private fun MascotRail(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(mascotName(context)) }
    var renameOpen by remember { mutableStateOf(false) }
    var renameValue by remember { mutableStateOf(name) }
    val motion = MascotRuntime.motion
    val message = MascotRuntime.message

    LaunchedEffect(Unit) { MascotRuntime.react(MascotEvent.GREETING) }

    Surface(
        modifier = modifier,
        color = Color(0xFFF7FCFF),
        border = BorderStroke(1.dp, Color(0xFFB9E8F8)),
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = SonHarfBlue, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(2.dp))
                Text("AI", color = SonHarfBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(5.dp))
            AndroidView(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setOnPreparedListener { mp -> mp.isLooping = true; start() }
                        setVideoURI(Uri.parse("android.resource://${ctx.packageName}/${rawFor(motion)}"))
                    }
                },
                update = { view ->
                    val tag = motion.name
                    if (view.tag != tag) {
                        view.tag = tag
                        view.setVideoURI(Uri.parse("android.resource://${context.packageName}/${rawFor(motion)}"))
                        view.setOnPreparedListener { mp -> mp.isLooping = motion == MascotMotion.IDLE; view.start() }
                    }
                }
            )
            Row(
                Modifier.fillMaxWidth().clickable { renameValue = name; renameOpen = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(name, color = SonHarfText, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(Modifier.width(2.dp))
                Icon(Icons.Rounded.Edit, null, tint = SonHarfMuted, modifier = Modifier.size(10.dp))
            }
            Text("Lv ${MascotRuntime.playerLevel}", color = SonHarfMuted, fontSize = 8.sp)
            Spacer(Modifier.height(5.dp))
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFD9F0F8)),
            ) {
                Text(
                    message,
                    modifier = Modifier.padding(5.dp),
                    color = SonHarfText,
                    fontSize = 8.sp,
                    lineHeight = 10.sp,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.weight(1f))
            Text("${MascotRuntime.playerXp} XP", color = SonHarfBlue, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (renameOpen) {
        AlertDialog(
            onDismissRequest = { renameOpen = false },
            title = { Text(sh("Maskotunun adı", "Mascot name")) },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it.take(18) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val clean = renameValue.trim().ifBlank { if (SonHarfUiState.language == "en") "Buddy" else "Dostum" }
                    setMascotName(context, clean)
                    name = clean
                    renameOpen = false
                }) { Text(sh("Kaydet", "Save")) }
            },
            dismissButton = { TextButton(onClick = { renameOpen = false }) { Text(sh("Vazgeç", "Cancel")) } },
        )
    }
}
'''
(APP / 'MascotSystem.kt').write_text(mascot, encoding='utf-8')

policy = APP / 'MascotPolicy.kt'
policy.write_text('''package com.sonharf.game\n\n/** Approved product policy: the AI mascot is enabled in its reserved UI rail. */\ninternal object MascotPolicy {\n    const val ENABLED = true\n}\n''', encoding='utf-8')

classic = APP / 'ClassicPremiumApp.kt'
s = classic.read_text(encoding='utf-8')
s = s.replace('ClassicScreen.GAME -> key(gameKey) { TargetNeonGameScreen() }', 'ClassicScreen.GAME -> key(gameKey) { MascotIntegratedGameScreen() }')
old_overlay = '''\n        if (screen == ClassicScreen.GAME) {\n            if (SonHarfGameModeState.mode == "expert") ExpertArenaOverlay() else SketchGameOverlayV9()\n            ComboOverlayV9()\n            BilBakalimBonusOverlay()\n        }\n'''
s = s.replace(old_overlay, '\n')
needle = 'growth = runCatching { backend?.getGrowthDashboard() }.getOrNull()'
if needle in s and 'MascotRuntime.syncPlayerProgress' not in s:
    s = s.replace(needle, needle + '\n        growth?.let { MascotRuntime.syncPlayerProgress(it.xp, it.level) }', 1)
classic.write_text(s, encoding='utf-8')

target = APP / 'TargetNeonGameScreen.kt'
t = target.read_text(encoding='utf-8')
needle2 = 'var matchJob by remember { mutableStateOf<Job?>(null) }'
if needle2 in t and 'MascotRuntime.react(MascotEvent.GREETING' not in t:
    t = t.replace(needle2, needle2 + '\n\n    LaunchedEffect(Unit) { MascotRuntime.react(MascotEvent.GREETING, language) }', 1)
needle3 = 'notice = if (rejected) friendly(updated.lastEvent.orEmpty()) else "${submitted.uppercase()} kabul edildi"'
if needle3 in t and 'MascotEvent.GOOD_WORD' not in t:
    repl = needle3 + '\n                                MascotRuntime.react(if (rejected) MascotEvent.ENCOURAGE else MascotEvent.GOOD_WORD, language)'
    t = t.replace(needle3, repl, 1)
# Observe match finish and critical/final phases without touching gameplay rules.
needle4 = 'LaunchedEffect(active.currentPlayerId, active.validWordCount, active.roundNo) { wordInput = "" }'
if needle4 in t and 'MascotEvent.CRITICAL' not in t:
    addon = needle4 + '''\n            LaunchedEffect(active.status, active.winnerId, active.finalMovesRemaining) {\n                when {\n                    active.status == "finished" && active.winnerId == me -> MascotRuntime.react(MascotEvent.VICTORY, language)\n                    active.status == "finished" && active.winnerId != null && active.winnerId != me -> MascotRuntime.react(MascotEvent.DEFEAT, language)\n                    active.status in listOf("final", "sudden_death") || active.finalMovesRemaining in 1..2 -> MascotRuntime.react(MascotEvent.CRITICAL, language)\n                    else -> Unit\n                }\n            }'''
    t = t.replace(needle4, addon, 1)
target.write_text(t, encoding='utf-8')

build = ROOT / 'app/build.gradle.kts'
b = build.read_text(encoding='utf-8')
b = b.replace('versionCode = 15', 'versionCode = 16')
b = b.replace('versionName = "0.8.6"', 'versionName = "0.8.7"')
build.write_text(b, encoding='utf-8')

print('Mascot AI integration applied')
