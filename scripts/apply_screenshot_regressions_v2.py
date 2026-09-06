from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def rep(path, old, new, expected=1):
    p = ROOT / path
    s = p.read_text(encoding='utf-8')
    n = s.count(old)
    if n != expected:
        raise SystemExit(f'{path}: expected {expected}, found {n}: {old[:120]!r}')
    p.write_text(s.replace(old, new, expected), encoding='utf-8')

def rep_all(path, old, new, minimum=1):
    p = ROOT / path
    s = p.read_text(encoding='utf-8')
    n = s.count(old)
    if n < minimum:
        raise SystemExit(f'{path}: expected >= {minimum}, found {n}: {old[:120]!r}')
    p.write_text(s.replace(old, new), encoding='utf-8')

online = 'app/src/main/java/com/sonharf/game/OnlineGameScreenV6.kt'
rep(online, 'import kotlinx.coroutines.launch\n', 'import kotlinx.coroutines.launch\nimport kotlinx.coroutines.TimeoutCancellationException\nimport kotlinx.coroutines.withTimeout\n')
rep(online, 'if (latest != null && latest.id != previousLastId) {', 'if (latest != null && latest.id != previousLastId && latest.playerId == me) {')

old = '''                scope.launch {
                    val submitted = wordInput.trim()
                    if (submitted.isBlank()) return@launch
                    val submitKey = "${active.id}|${active.roundNo}|${active.validWordCount}|${active.currentPlayerId}|${active.turnDeadline}|$submitted"
                    if (submitInFlightKey == submitKey || busy) return@launch
                    submitInFlightKey = submitKey
                    val shownWord = gameUppercase(submitted, active.language)
                    val voiceToken = voiceRequestId
                    if (voiceToken == null) wordInput = ""
                    busy = true
                    SonHarfSoundFx.tap()
                    runCatching {
                        if (voiceToken != null) backend.submitVoiceWord(active.id, submitted, voiceToken)
                        else backend.submitWord(active.id, submitted)
                    }
                        .onSuccess { result ->
                            acceptServerRoom(result)
                            if (voiceToken != null) {
                                wordInput = ""
                                voiceRequestId = null
                                voiceUses = runCatching { backend.getVoiceUses(active.id) }.getOrDefault(voiceUses + 1)
                            }
                            if (failedEvent(result.lastEvent) && result.lastEventPlayerId == me) {
                                feedbackWord = shownWord
                                feedbackCorrect = false
                                notice = eventMessage(result.lastEvent)
                                SonHarfSoundFx.warning()
                            } else {
                                feedbackWord = shownWord
                                feedbackCorrect = true
                                notice = sh("Kelime kabul edildi: $shownWord", "Word accepted: $shownWord")
                                SonHarfSoundFx.wordAccepted()
                            }
                        }
                        .onFailure {
                            feedbackWord = shownWord
                            feedbackCorrect = false
                            notice = friendly(it.message.orEmpty())
                            SonHarfSoundFx.warning()
                        }
                    busy = false
                    submitInFlightKey = null
                }
'''
new = '''                scope.launch {
                    val submitted = wordInput.trim()
                    if (submitted.isBlank()) return@launch
                    val shownWord = gameUppercase(submitted, active.language)
                    val alreadyUsed = words.any { played ->
                        val existing = played.normalizedWord.trim().ifBlank { played.word.trim() }
                        gameUppercase(existing, active.language) == shownWord
                    }
                    if (alreadyUsed) {
                        feedbackWord = shownWord
                        feedbackCorrect = false
                        notice = eventMessage("word_already_used")
                        SonHarfSoundFx.warning()
                        return@launch
                    }
                    val submitKey = "${active.id}|${active.roundNo}|${active.validWordCount}|${active.currentPlayerId}|${active.turnDeadline}|$shownWord"
                    if (submitInFlightKey == submitKey || busy) return@launch
                    submitInFlightKey = submitKey
                    val voiceToken = voiceRequestId
                    if (voiceToken == null) wordInput = ""
                    busy = true
                    SonHarfSoundFx.tap()
                    try {
                        runCatching {
                            withTimeout(7_000L) {
                                if (voiceToken != null) backend.submitVoiceWord(active.id, submitted, voiceToken)
                                else backend.submitWord(active.id, submitted)
                            }
                        }
                            .onSuccess { result ->
                                acceptServerRoom(result)
                                if (voiceToken != null) {
                                    wordInput = ""
                                    voiceRequestId = null
                                    voiceUses = runCatching { backend.getVoiceUses(active.id) }.getOrDefault(voiceUses + 1)
                                }
                                if (failedEvent(result.lastEvent) && result.lastEventPlayerId == me) {
                                    feedbackWord = shownWord
                                    feedbackCorrect = false
                                    notice = eventMessage(result.lastEvent)
                                    SonHarfSoundFx.warning()
                                } else {
                                    feedbackWord = shownWord
                                    feedbackCorrect = true
                                    notice = sh("Kelime kabul edildi: $shownWord", "Word accepted: $shownWord")
                                    SonHarfSoundFx.wordAccepted()
                                }
                            }
                            .onFailure { error ->
                                feedbackWord = shownWord
                                if (error is TimeoutCancellationException) {
                                    feedbackCorrect = null
                                    notice = sh(
                                        "Sunucu yanıtı gecikti. Oyun durumu eşitleniyor; tekrar göndermeden önce bekle.",
                                        "Server response is delayed. Game state is syncing; wait before sending again.",
                                    )
                                } else {
                                    feedbackCorrect = false
                                    notice = friendly(error.message.orEmpty())
                                    SonHarfSoundFx.warning()
                                }
                            }
                    } finally {
                        busy = false
                        submitInFlightKey = null
                    }
                }
'''
rep(online, old, new)

light = 'app/src/main/java/com/sonharf/game/LightDuelUi.kt'
old_palette = '''private val LBg: Color get() = if (SonHarfCosmetics.monsterBlueTheme) Color(0xFFC9E3FF) else Color(0xFFF4F7FB)
private val LCard = Color.White
private val LCard2 = Color(0xFFF0F4F8)
private val LText = Color(0xFF182235)
private val LMuted = Color(0xFF718096)
private val LBlue = Color(0xFF1769E0)
private val LBlueSoft = Color(0xFFE8F2FF)
private val LBorder = Color(0xFFDDE5EE)
private val LRed = Color(0xFFE24D6B)
private val LOrange = Color(0xFFF47B20)
private val LGold = Color(0xFFF3A81A)
private val LPurple = Color(0xFF7658D6)
private val LGreen = Color(0xFF22A85A)
'''
new_palette = '''private val LBg: Color get() = when {
    SonHarfCosmetics.darkArenaTheme -> SonHarfTheme.Background
    SonHarfCosmetics.monsterBlueTheme -> Color(0xFFC9E3FF)
    else -> Color(0xFFF4F7FB)
}
private val LCard: Color get() = if (SonHarfCosmetics.darkArenaTheme) SonHarfTheme.Surface else Color.White
private val LCard2: Color get() = if (SonHarfCosmetics.darkArenaTheme) SonHarfTheme.SurfaceSecondary else Color(0xFFF0F4F8)
private val LText: Color get() = if (SonHarfCosmetics.darkArenaTheme) SonHarfTheme.TextPrimary else Color(0xFF182235)
private val LMuted: Color get() = if (SonHarfCosmetics.darkArenaTheme) SonHarfTheme.TextSecondary else Color(0xFF718096)
private val LBlue: Color get() = if (SonHarfCosmetics.darkArenaTheme) SonHarfTheme.PrimaryBlue else Color(0xFF1769E0)
private val LBlueSoft: Color get() = if (SonHarfCosmetics.darkArenaTheme) SonHarfTheme.PrimaryBlueSoft else Color(0xFFE8F2FF)
private val LBorder: Color get() = if (SonHarfCosmetics.darkArenaTheme) SonHarfTheme.Border else Color(0xFFDDE5EE)
private val LRed: Color get() = if (SonHarfCosmetics.darkArenaTheme) SonHarfTheme.Error else Color(0xFFE24D6B)
private val LOrange: Color get() = if (SonHarfCosmetics.darkArenaTheme) SonHarfTheme.Warning else Color(0xFFF47B20)
private val LGold: Color get() = if (SonHarfCosmetics.darkArenaTheme) SonHarfTheme.Warning else Color(0xFFF3A81A)
private val LPurple: Color get() = if (SonHarfCosmetics.darkArenaTheme) SonHarfTheme.Purple else Color(0xFF7658D6)
private val LGreen: Color get() = if (SonHarfCosmetics.darkArenaTheme) SonHarfTheme.Success else Color(0xFF22A85A)
private val LOnPrimary: Color get() = if (SonHarfCosmetics.darkArenaTheme) Color(0xFF17120A) else Color.White
'''
rep(light, old_palette, new_palette)
rep_all(light, 'Brush.verticalGradient(listOf(Color.White, LBg))', 'Brush.verticalGradient(listOf(LCard, LBg))')
rep_all(light, 'CardDefaults.cardColors(containerColor = Color.White)', 'CardDefaults.cardColors(containerColor = LCard)', minimum=3)
rep_all(light, 'color = if (selected) LBlueSoft else Color.White', 'color = if (selected) LBlueSoft else LCard')
rep_all(light, 'color = Color.White, border = BorderStroke(1.dp, LBorder)', 'color = LCard, border = BorderStroke(1.dp, LBorder)')
rep(light,
    'containerColor = if (matching) Color(0xFFFFEEF2) else LBlue,\n                        contentColor = if (matching) LRed else Color.White,',
    'containerColor = if (matching) LRed.copy(alpha = .12f) else LBlue,\n                        contentColor = if (matching) LRed else LOnPrimary,')
rep(light,
    'shape = RoundedCornerShape(18.dp),\n        color = Color.White,\n        border = BorderStroke(2.dp, if (myTurn && !quiz) statusColor.copy(alpha = .65f) else LBorder),',
    'shape = RoundedCornerShape(18.dp),\n        color = LCard,\n        border = BorderStroke(2.dp, if (myTurn && !quiz) statusColor.copy(alpha = .65f) else LBorder),')

old_result = '''                        Text(sh("ASIL CEVAP ${round.correctAnswer ?: "—"}", "ACTUAL ANSWER ${round.correctAnswer ?: "—"}"), color = LText, fontWeight = FontWeight.Black)
                        Text(
                            if (tied) sh("BERABERE", "TIE")
                            else if (myWon) sh("DOĞRU • $myAnswer", "CORRECT • $myAnswer")
                            else sh("RAKİP DAHA YAKIN • $opponentAnswer", "OPPONENT CLOSER • $opponentAnswer"),
                            color = if (myWon) LGreen else LRed,
                            fontWeight = FontWeight.Bold,
                        )
'''
new_result = '''                        Text(sh("DOĞRU CEVAP: ${round.correctAnswer ?: "—"}", "CORRECT ANSWER: ${round.correctAnswer ?: "—"}"), color = LText, fontWeight = FontWeight.Black)
                        Text(
                            when {
                                tied -> sh("BERABERE • SEN ${myAnswer ?: "—"} • RAKİP ${opponentAnswer ?: "—"}", "TIE • YOU ${myAnswer ?: "—"} • OPPONENT ${opponentAnswer ?: "—"}")
                                myWon -> sh("SEN DAHA YAKINSIN • $myAnswer • +${round.bonusPoints}", "YOU ARE CLOSER • $myAnswer • +${round.bonusPoints}")
                                else -> sh("RAKİP DAHA YAKIN • $opponentAnswer • +${round.bonusPoints}", "OPPONENT CLOSER • $opponentAnswer • +${round.bonusPoints}")
                            },
                            color = when { tied -> LGold; myWon -> LGreen; else -> LRed },
                            fontWeight = FontWeight.Bold,
                        )
'''
rep(light, old_result, new_result)

board = 'app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt'
rep(board, 'var mode by remember { mutableStateOf(WordSiegeBoardViewportMode.FIT) }', 'var mode by remember { mutableStateOf(WordSiegeBoardViewportMode.CLOSE) }')
rep(board,
    'color = MainUi.Muted,\n                fontSize = WordSiegeBoardAccessibility.RackPoint,',
    'color = if (used) MainUi.Muted.copy(alpha = .55f) else Color(0xFF5D4B20),\n                fontSize = WordSiegeBoardAccessibility.RackPoint,')

acc = 'app/src/main/java/com/sonharf/game/WordSiegeBoardAccessibility.kt'
rep(acc, 'val BoardLetterPoint: TextUnit = 8.sp', 'val BoardLetterPoint: TextUnit = 10.sp')
rep(acc, 'val BoardBonus: TextUnit = 10.sp', 'val BoardBonus: TextUnit = 12.sp')
rep(acc, 'val RackPoint: TextUnit = 9.sp', 'val RackPoint: TextUnit = 11.sp')

practice = 'app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt'
rep(practice, 'WordSiegeNotice(message)', 'WordSiegePracticeNotice(message)')

(ROOT/'app/src/main/java/com/sonharf/game/WordSiegePracticeNotice.kt').write_text('''package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun WordSiegePracticeNotice(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SonHarfTheme.SurfaceSecondary,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SonHarfTheme.Border),
    ) {
        Text(
            text = message,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
            color = SonHarfTheme.TextPrimary,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
''', encoding='utf-8')

(ROOT/'app/src/test/java/com/sonharf/game/ScreenshotRegressionFixContractTest.kt').write_text('''package com.sonharf.game

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotRegressionFixContractTest {
    private fun read(path: String): String {
        val direct = File(path.removePrefix("app/"))
        val root = File(path)
        return when { direct.exists() -> direct.readText(); root.exists() -> root.readText(); else -> error("Missing $path") }
    }
    @Test fun duelSubmissionIsProtected() {
        val s = read("app/src/main/java/com/sonharf/game/OnlineGameScreenV6.kt")
        assertTrue(s.contains("latest.playerId == me"))
        assertTrue(s.contains("val alreadyUsed = words.any"))
        assertTrue(s.contains("withTimeout(7_000L)"))
        assertTrue(s.contains("finally {"))
    }
    @Test fun nightArenaAndQuizResultAreExplicit() {
        val s = read("app/src/main/java/com/sonharf/game/LightDuelUi.kt")
        assertTrue(s.contains("SonHarfCosmetics.darkArenaTheme -> SonHarfTheme.Background"))
        assertTrue(s.contains("BERABERE • SEN"))
        assertTrue(s.contains("RAKİP ${'$'}{opponentAnswer"))
    }
    @Test fun siegeStartsReadableAndUsesAccessibleNotice() {
        assertTrue(read("app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt").contains("mutableStateOf(WordSiegeBoardViewportMode.CLOSE)"))
        assertTrue(read("app/src/main/java/com/sonharf/game/WordSiegePracticeNotice.kt").contains("SonHarfTheme.TextPrimary"))
    }
}
''', encoding='utf-8')

print('v2 screenshot regression patch applied')
