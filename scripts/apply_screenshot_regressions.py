from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace(path: str, old: str, new: str, count: int = 1):
    p = ROOT / path
    text = p.read_text(encoding="utf-8")
    actual = text.count(old)
    if actual != count:
        raise SystemExit(f"{path}: expected {count} occurrences, found {actual}: {old[:100]!r}")
    p.write_text(text.replace(old, new, count), encoding="utf-8")


def replace_all(path: str, old: str, new: str, minimum: int = 1):
    p = ROOT / path
    text = p.read_text(encoding="utf-8")
    actual = text.count(old)
    if actual < minimum:
        raise SystemExit(f"{path}: expected at least {minimum} occurrences, found {actual}: {old[:100]!r}")
    p.write_text(text.replace(old, new), encoding="utf-8")


# 1) Duel submission flow: local duplicate preflight, own-word-only success feedback,
#    7 second timeout, and a guaranteed busy/in-flight release.
online = "app/src/main/java/com/sonharf/game/OnlineGameScreenV6.kt"
replace(
    online,
    "import kotlinx.coroutines.launch\n",
    "import kotlinx.coroutines.launch\nimport kotlinx.coroutines.TimeoutCancellationException\nimport kotlinx.coroutines.withTimeout\n",
)
replace(
    online,
    "if (latest != null && latest.id != previousLastId) {",
    "if (latest != null && latest.id != previousLastId && latest.playerId == me) {",
)
old_submit = '''                            val submitted = wordInput.trim()
                            if (submitted.isNotEmpty()) {
                                val voiceToken = voiceInputToken?.takeIf { submitted == voiceInputWord && voiceActiveRequestId == voiceRequestId }
                                val submitKey = "${active.id}:${active.currentTurnIndex}:${gameUppercase(submitted, active.language)}"
                                if (submitInFlightKey != submitKey) {
                                    submitInFlightKey = submitKey
                                    val shownWord = gameUppercase(submitted, active.language)
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
                                                voiceInputToken = null
                                                voiceInputWord = null
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
                                }
                                submitInFlightKey = null
                            }
'''
new_submit = '''                            val submitted = wordInput.trim()
                            if (submitted.isNotEmpty()) {
                                val voiceToken = voiceInputToken?.takeIf { submitted == voiceInputWord && voiceActiveRequestId == voiceRequestId }
                                val shownWord = gameUppercase(submitted, active.language)
                                val submitKey = "${active.id}:${active.currentTurnIndex}:$shownWord"
                                val alreadyUsed = words.any { played ->
                                    val existing = played.normalizedWord.trim().ifBlank { played.word.trim() }
                                    gameUppercase(existing, active.language) == shownWord
                                }
                                if (alreadyUsed) {
                                    feedbackWord = shownWord
                                    feedbackCorrect = false
                                    notice = eventMessage("word_already_used")
                                    SonHarfSoundFx.warning()
                                } else if (submitInFlightKey != submitKey) {
                                    submitInFlightKey = submitKey
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
                                                    voiceInputToken = null
                                                    voiceInputWord = null
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
                            }
'''
replace(online, old_submit, new_submit)

# 2) Light duel surfaces must honor the selected Night Arena theme.
light = "app/src/main/java/com/sonharf/game/LightDuelUi.kt"
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
replace(light, old_palette, new_palette)
replace_all(light, "Brush.verticalGradient(listOf(Color.White, LBg))", "Brush.verticalGradient(listOf(LCard, LBg))")
replace_all(light, "CardDefaults.cardColors(containerColor = Color.White)", "CardDefaults.cardColors(containerColor = LCard)", minimum=3)
replace_all(light, "color = if (selected) LBlueSoft else Color.White", "color = if (selected) LBlueSoft else LCard")
replace_all(light, "color = Color.White, border = BorderStroke(1.dp, LBorder)", "color = LCard, border = BorderStroke(1.dp, LBorder)")
replace(
    light,
    "containerColor = if (matching) Color(0xFFFFEEF2) else LBlue,\n                        contentColor = if (matching) LRed else Color.White,",
    "containerColor = if (matching) LRed.copy(alpha = .12f) else LBlue,\n                        contentColor = if (matching) LRed else LOnPrimary,",
)

# Input card is a common surface, but keep physical keyboard key colors under its existing neon policy.
replace(
    light,
    "shape = RoundedCornerShape(18.dp),\n        color = Color.White,\n        border = BorderStroke(2.dp, if (myTurn && !quiz) statusColor.copy(alpha = .65f) else LBorder),",
    "shape = RoundedCornerShape(18.dp),\n        color = LCard,\n        border = BorderStroke(2.dp, if (myTurn && !quiz) statusColor.copy(alpha = .65f) else LBorder),",
)

# 3) Bil Bakalim result: make tie/winner state explicit and show both answers.
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
replace(light, old_result, new_result)

# 4) Siege: default to readable close view; increase cell/bonus/rack point legibility.
practice_board = "app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt"
replace(
    practice_board,
    "var mode by remember { mutableStateOf(WordSiegeBoardViewportMode.FIT) }",
    "var mode by remember { mutableStateOf(WordSiegeBoardViewportMode.CLOSE) }",
)
replace(
    practice_board,
    "color = MainUi.Muted,\n                fontSize = WordSiegeBoardAccessibility.RackPoint,",
    "color = if (used) MainUi.Muted.copy(alpha = .55f) else Color(0xFF5D4B20),\n                fontSize = WordSiegeBoardAccessibility.RackPoint,",
)

accessibility = "app/src/main/java/com/sonharf/game/WordSiegeBoardAccessibility.kt"
replace(accessibility, "val BoardLetterPoint = 8.sp", "val BoardLetterPoint = 10.sp")
replace(accessibility, "val BoardBonus = 10.sp", "val BoardBonus = 12.sp")
replace(accessibility, "val RackPoint = 9.sp", "val RackPoint = 11.sp")

practice = "app/src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt"
replace(practice, "WordSiegeNotice(message)", "WordSiegePracticeNotice(message)")

notice_path = ROOT / "app/src/main/java/com/sonharf/game/WordSiegePracticeNotice.kt"
notice_path.write_text('''package com.sonharf.game

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
''', encoding="utf-8")

# Contract test for the bugs reported from device screenshots.
test_path = ROOT / "app/src/test/java/com/sonharf/game/ScreenshotRegressionFixContractTest.kt"
test_path.write_text('''package com.sonharf.game

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotRegressionFixContractTest {
    private fun read(path: String): String {
        val direct = File(path.removePrefix("app/"))
        val fromRoot = File(path)
        return when {
            direct.exists() -> direct.readText()
            fromRoot.exists() -> fromRoot.readText()
            else -> error("Missing $path")
        }
    }

    @Test fun duelSubmissionCannotFalselyAcceptOpponentOrDuplicateWord() {
        val src = read("app/src/main/java/com/sonharf/game/OnlineGameScreenV6.kt")
        assertTrue(src.contains("latest.playerId == me"))
        assertTrue(src.contains("val alreadyUsed = words.any"))
        assertTrue(src.contains("withTimeout(7_000L)"))
        assertTrue(src.contains("finally {"))
        assertTrue(src.contains("error is TimeoutCancellationException"))
    }

    @Test fun nightArenaAndQuizResultAreExplicit() {
        val src = read("app/src/main/java/com/sonharf/game/LightDuelUi.kt")
        assertTrue(src.contains("SonHarfCosmetics.darkArenaTheme -> SonHarfTheme.Background"))
        assertTrue(src.contains("private val LCard: Color get()"))
        assertTrue(src.contains("BERABERE • SEN"))
        assertTrue(src.contains("RAKİP ${'$'}{opponentAnswer"))
    }

    @Test fun siegeStartsReadableAndUsesAccessibleNotice() {
        val board = read("app/src/main/java/com/sonharf/game/WordSiegePracticeBoard.kt")
        val notice = read("app/src/main/java/com/sonharf/game/WordSiegePracticeNotice.kt")
        assertTrue(board.contains("mutableStateOf(WordSiegeBoardViewportMode.CLOSE)"))
        assertTrue(notice.contains("SonHarfTheme.TextPrimary"))
        assertTrue(notice.contains("fontSize = 11.sp"))
    }
}
''', encoding="utf-8")

print("Screenshot regression patch applied successfully")
