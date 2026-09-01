from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ONLINE = ROOT / "app/src/main/java/com/sonharf/game/OnlineGameScreenV6.kt"
UI = ROOT / "app/src/main/java/com/sonharf/game/LightDuelUi.kt"
TEST = ROOT / "app/src/test/java/com/sonharf/game/SonHarfTurnResolutionContractTest.kt"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


online = ONLINE.read_text()
online = replace_once(
    online,
    "import android.widget.Toast\n",
    "import android.util.Log\nimport android.widget.Toast\n",
    "Log import",
)
online = replace_once(
    online,
    '    var feedbackCorrect by remember { mutableStateOf<Boolean?>(null) }\n    var busy by remember { mutableStateOf(false) }',
    '    var feedbackCorrect by remember { mutableStateOf<Boolean?>(null) }\n    var feedbackScoreDelta by remember { mutableStateOf<Int?>(null) }\n    var isResolvingTurn by remember { mutableStateOf(false) }\n    var pendingRoomAfterResolution by remember { mutableStateOf<GameRoomDto?>(null) }\n    var busy by remember { mutableStateOf(false) }',
    "turn resolution state",
)
online = replace_once(
    online,
    '    fun failedEvent(e: String?) = e in setOf("word_already_used", "wrong_start_letter", "not_in_dictionary", "invalid_word", "ends_with_soft_g", "turn_expired")\n',
    '    fun failedEvent(e: String?) = e in setOf("word_already_used", "wrong_start_letter", "not_in_dictionary", "invalid_word", "ends_with_soft_g", "turn_expired")\n    fun playerScore(r: GameRoomDto, playerId: String?): Int = when (playerId) {\n        r.hostId -> r.hostScore\n        r.guestId -> r.guestScore\n        else -> 0\n    }\n',
    "score helper",
)
online = replace_once(
    online,
    '                .collect {\n                    room = it\n                    refreshQuiz(it)',
    '                .collect {\n                    if (isResolvingTurn) pendingRoomAfterResolution = it else room = it\n                    refreshQuiz(it)',
    "hold realtime room while resolving",
)
online = replace_once(
    online,
    '                    if (latest != null && latest.id != previousLastId) {\n                        feedbackWord = gameUppercase(\n                            latest.word.trim().ifBlank { latest.normalizedWord.trim() },\n                            room?.language ?: language,\n                        )\n                        feedbackCorrect = true\n                    }',
    '                    if (latest != null && latest.id != previousLastId && !isResolvingTurn && feedbackWord == null) {\n                        feedbackWord = gameUppercase(\n                            latest.word.trim().ifBlank { latest.normalizedWord.trim() },\n                            room?.language ?: language,\n                        )\n                        feedbackCorrect = true\n                        feedbackScoreDelta = null\n                    }',
    "word observer feedback guard",
)
online = replace_once(
    online,
    '    val active = room\n',
    '    LaunchedEffect(feedbackWord, feedbackCorrect, feedbackScoreDelta) {\n        if (feedbackWord != null && feedbackCorrect != null) {\n            delay(950L)\n            if (!isResolvingTurn) {\n                feedbackWord = null\n                feedbackCorrect = null\n                feedbackScoreDelta = null\n            }\n        }\n    }\n\n    val active = room\n',
    "feedback timeout",
)
online = replace_once(
    online,
    '        LaunchedEffect(active.id, active.status, active.botTurn, active.validWordCount, active.roundNo) {\n            if (active.isBot && active.botTurn && active.status in listOf("playing", "final", "sudden_death")) {',
    '        LaunchedEffect(active.id, active.status, active.botTurn, active.validWordCount, active.roundNo, isResolvingTurn) {\n            if (!isResolvingTurn && active.isBot && active.botTurn && active.status in listOf("playing", "final", "sudden_death")) {',
    "bot effect resolution guard",
)
online = replace_once(
    online,
    '                        latest.id != active.id ||\n                        !latest.isBot ||',
    '                        isResolvingTurn ||\n                        latest.id != active.id ||\n                        !latest.isBot ||',
    "bot loop resolution guard",
)
online = replace_once(
    online,
    '            feedbackCorrect = feedbackCorrect,\n            wordInput = wordInput,',
    '            feedbackCorrect = feedbackCorrect,\n            feedbackScoreDelta = feedbackScoreDelta,\n            wordInput = wordInput,',
    "score feedback parameter",
)
old_submit = '''            onSubmit = {\n                scope.launch {\n                    val submitted = wordInput.trim()\n                    if (submitted.isBlank()) return@launch\n                    val shownWord = gameUppercase(submitted, active.language)\n                    wordInput = ""\n                    busy = true\n                    SonHarfSoundFx.tap()\n                    runCatching { backend.submitWord(active.id, submitted) }\n                        .onSuccess { result ->\n                            room = result\n                            if (failedEvent(result.lastEvent) && result.lastEventPlayerId == me) {\n                                feedbackWord = shownWord\n                                feedbackCorrect = false\n                                notice = eventMessage(result.lastEvent)\n                                SonHarfSoundFx.warning()\n                            } else {\n                                feedbackWord = shownWord\n                                feedbackCorrect = true\n                                notice = sh("Kelime kabul edildi: $shownWord", "Word accepted: $shownWord")\n                                SonHarfSoundFx.wordAccepted()\n                            }\n                        }\n                        .onFailure {\n                            feedbackWord = shownWord\n                            feedbackCorrect = false\n                            notice = friendly(it.message.orEmpty())\n                            SonHarfSoundFx.warning()\n                        }\n                    busy = false\n                }\n            },'''
new_submit = '''            onSubmit = {\n                val submitted = wordInput.trim()\n                if (submitted.isNotBlank() && !isResolvingTurn && !busy) {\n                    val turnBefore = active.currentPlayerId\n                    val botTurnBefore = active.botTurn\n                    val scoreBefore = playerScore(active, me)\n                    val shownWord = gameUppercase(submitted, active.language)\n                    isResolvingTurn = true\n                    pendingRoomAfterResolution = null\n                    busy = true\n                    wordInput = ""\n                    feedbackWord = shownWord\n                    feedbackCorrect = null\n                    feedbackScoreDelta = null\n                    SonHarfSoundFx.tap()\n                    Log.d(\n                        "SonHarfMove",\n                        "submitted_word=$submitted normalized_word=$shownWord validation_started=true " +\n                            "turn_before=$turnBefore bot_turn_before=$botTurnBefore",\n                    )\n                    scope.launch {\n                        try {\n                            val result = backend.submitWord(active.id, submitted)\n                            val rejected = failedEvent(result.lastEvent) && result.lastEventPlayerId == me\n                            val scoreDelta = playerScore(result, me) - scoreBefore\n                            feedbackWord = shownWord\n                            feedbackCorrect = !rejected\n                            feedbackScoreDelta = scoreDelta.takeIf { it != 0 }\n                            if (rejected) {\n                                notice = eventMessage(result.lastEvent)\n                                SonHarfSoundFx.warning()\n                            } else {\n                                notice = sh("Kelime kabul edildi: $shownWord", "Word accepted: $shownWord")\n                                SonHarfSoundFx.wordAccepted()\n                            }\n                            val finalizedRoom = pendingRoomAfterResolution ?: result\n                            pendingRoomAfterResolution = null\n                            room = finalizedRoom\n                            Log.d(\n                                "SonHarfMove",\n                                "submitted_word=$submitted normalized_word=$shownWord validation_result=${if (rejected) "REJECTED" else "ACCEPTED"} " +\n                                    "score_delta=$scoreDelta turn_before=$turnBefore turn_after=${finalizedRoom.currentPlayerId} " +\n                                    "bot_triggered=false error_code=${result.lastEvent ?: "none"}",\n                            )\n                        } catch (t: Throwable) {\n                            val realtimeResult = pendingRoomAfterResolution\n                            pendingRoomAfterResolution = null\n                            if (realtimeResult != null &&\n                                (realtimeResult.validWordCount != active.validWordCount || realtimeResult.lastEventPlayerId == me)\n                            ) {\n                                val rejected = failedEvent(realtimeResult.lastEvent) && realtimeResult.lastEventPlayerId == me\n                                val scoreDelta = playerScore(realtimeResult, me) - scoreBefore\n                                feedbackCorrect = !rejected\n                                feedbackScoreDelta = scoreDelta.takeIf { it != 0 }\n                                notice = if (rejected) eventMessage(realtimeResult.lastEvent)\n                                else sh("Kelime kabul edildi: $shownWord", "Word accepted: $shownWord")\n                                room = realtimeResult\n                                if (rejected) SonHarfSoundFx.warning() else SonHarfSoundFx.wordAccepted()\n                                Log.w(\n                                    "SonHarfMove",\n                                    "submitted_word=$submitted normalized_word=$shownWord validation_result=${if (rejected) "REJECTED" else "ACCEPTED"} " +\n                                        "score_delta=$scoreDelta turn_before=$turnBefore turn_after=${realtimeResult.currentPlayerId} " +\n                                        "bot_triggered=false error_code=rpc_response_failed_realtime_recovered",\n                                    t,\n                                )\n                            } else {\n                                feedbackCorrect = false\n                                feedbackScoreDelta = null\n                                notice = friendly(t.message.orEmpty())\n                                SonHarfSoundFx.warning()\n                                Log.e(\n                                    "SonHarfMove",\n                                    "submitted_word=$submitted normalized_word=$shownWord validation_result=REJECTED " +\n                                        "score_delta=0 turn_before=$turnBefore turn_after=$turnBefore bot_triggered=false " +\n                                        "error_code=${t::class.simpleName ?: "unknown"}",\n                                    t,\n                                )\n                            }\n                        } finally {\n                            busy = false\n                            isResolvingTurn = false\n                        }\n                    }\n                }\n            },'''
online = replace_once(online, old_submit, new_submit, "atomic submit handler")
ONLINE.write_text(online)

ui = UI.read_text()
ui = replace_once(
    ui,
    '    feedbackCorrect: Boolean?,\n    wordInput: String,',
    '    feedbackCorrect: Boolean?,\n    feedbackScoreDelta: Int?,\n    wordInput: String,',
    "UI score feedback parameter",
)
ui = replace_once(
    ui,
    '    val shownLastWordColor = when {\n        feedbackWord != null && feedbackCorrect == false -> LRed\n        shownLastWord.isNotBlank() -> LGreen\n        else -> LBlue\n    }',
    '    val shownLastWordColor = when {\n        feedbackWord != null && feedbackCorrect == false -> LRed\n        feedbackWord != null && feedbackCorrect == null -> LBlue\n        shownLastWord.isNotBlank() -> LGreen\n        else -> LBlue\n    }',
    "validating word color",
)
ui = replace_once(
    ui,
    '            Surface(\n                modifier = Modifier.size(78.dp),\n                shape = CircleShape,\n                color = Color.White,\n                border = BorderStroke(3.dp, if (seconds <= 3 && !quizActive) LRed else LBlue),',
    '            val timerWarning = !quizActive && seconds in 1..10\n            val timerCritical = !quizActive && seconds in 1..3\n            Surface(\n                modifier = Modifier.size(if (timerWarning) 82.dp else 78.dp),\n                shape = CircleShape,\n                color = Color.White,\n                border = BorderStroke(3.dp, when { timerCritical -> LRed; timerWarning -> LGold; else -> LBlue }),',
    "last ten seconds timer emphasis",
)
ui = replace_once(
    ui,
    '                    Text(seconds.toString(), color = LText, fontSize = 30.sp, fontWeight = FontWeight.Black)',
    '                    Text(seconds.toString(), color = if (timerCritical) LRed else LText, fontSize = if (timerWarning) 34.sp else 30.sp, fontWeight = FontWeight.Black)',
    "timer font emphasis",
)
old_word = '''                    Text(\n                        shownLastWord.ifBlank { sh("İLK KELİMEYİ YAZ", "ENTER FIRST WORD") },\n                        color = shownLastWordColor,\n                        fontSize = 14.sp,\n                        fontWeight = FontWeight.Black,\n                        maxLines = 1,\n                    )\n                    Spacer(Modifier.height(3.dp))'''
new_word = '''                    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {\n                        val visibleWord = shownLastWord.ifBlank { sh("İLK KELİMEYİ YAZ", "ENTER FIRST WORD") }\n                        val wordSize = when {\n                            maxWidth < 330.dp && visibleWord.length >= 14 -> 20.sp\n                            visibleWord.length >= 20 -> 20.sp\n                            visibleWord.length >= 16 -> 23.sp\n                            visibleWord.length >= 12 -> 27.sp\n                            maxWidth < 330.dp -> 30.sp\n                            else -> 34.sp\n                        }\n                        Text(\n                            visibleWord,\n                            color = shownLastWordColor,\n                            fontSize = wordSize,\n                            lineHeight = (wordSize.value + 3).sp,\n                            fontWeight = FontWeight.Black,\n                            maxLines = 1,\n                            softWrap = false,\n                            overflow = TextOverflow.Ellipsis,\n                            textAlign = TextAlign.Center,\n                        )\n                    }\n                    if (feedbackScoreDelta != null && feedbackScoreDelta != 0) {\n                        Spacer(Modifier.height(3.dp))\n                        Text(\n                            "${if (feedbackScoreDelta > 0) "+" else ""}$feedbackScoreDelta ${if (room.language == "en") "PTS" else "PUAN"}",\n                            color = if (feedbackScoreDelta > 0) LGreen else LRed,\n                            fontSize = 14.sp,\n                            fontWeight = FontWeight.Black,\n                        )\n                    }\n                    Spacer(Modifier.height(3.dp))'''
ui = replace_once(ui, old_word, new_word, "responsive central word and score feedback")
ui = replace_once(
    ui,
    '                Spacer(Modifier.weight(.20f))\n',
    '                if (room.isBot) {\n                    Text(sh("ANTRENMAN • RATING ETKİLENMEZ", "PRACTICE • RATING UNAFFECTED"), color = LMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)\n                } else {\n                    val goal = ratingLeagueProgress(playerRating)\n                    if (goal.nextAt != null && goal.pointsToNext > 0) {\n                        Text(sh("${goal.nextLeagueName} ligine ${goal.pointsToNext} rating kaldı", "${goal.pointsToNext} rating to ${goal.nextLeagueName}"), color = LGold, fontSize = 8.sp, fontWeight = FontWeight.Bold)\n                    }\n                }\n\n                Spacer(Modifier.weight(.20f))\n',
    "practice and rating goal",
)
ui = replace_once(
    ui,
    '                Text(\n                    "🏆 $rating",\n                    color = LGold,\n                    fontSize = 8.sp,\n                    fontWeight = FontWeight.Bold,\n                    maxLines = 1,\n                )',
    '                val league = ratingLeagueProgress(rating)\n                Text(\n                    "${league.leagueName} • $rating",\n                    color = LGold,\n                    fontSize = 8.sp,\n                    fontWeight = FontWeight.Bold,\n                    maxLines = 1,\n                )',
    "league rating visibility",
)
ui = replace_once(
    ui,
    '                fontSize = if (value.isBlank()) 14.sp else 18.sp,\n',
    '                fontSize = if (value.isBlank()) 14.sp else if (value.length >= 18) 18.sp else if (value.length >= 12) 20.sp else 22.sp,\n',
    "typed word size",
)
UI.write_text(ui)

TEST.parent.mkdir(parents=True, exist_ok=True)
TEST.write_text(r'''package com.sonharf.game

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SonHarfTurnResolutionContractTest {
    private fun source(path: String): String = Files.readString(Path.of(path))

    @Test
    fun submitIsLockedBeforeAsyncValidationAndBotWaitsForFinalization() {
        val online = source("src/main/java/com/sonharf/game/OnlineGameScreenV6.kt")
        assertTrue(online.contains("if (submitted.isNotBlank() && !isResolvingTurn && !busy)"))
        assertTrue(online.contains("isResolvingTurn = true"))
        assertTrue(online.contains("LaunchedEffect(active.id, active.status, active.botTurn, active.validWordCount, active.roundNo, isResolvingTurn)"))
        assertTrue(online.contains("if (!isResolvingTurn && active.isBot && active.botTurn"))
        assertTrue(online.contains("if (isResolvingTurn) pendingRoomAfterResolution = it else room = it"))
        val submitStart = online.indexOf("val result = backend.submitWord(active.id, submitted)")
        val classify = online.indexOf("val rejected = failedEvent(result.lastEvent)", submitStart)
        val publishRoom = online.indexOf("room = finalizedRoom", classify)
        val unlock = online.indexOf("isResolvingTurn = false", publishRoom)
        assertTrue(submitStart >= 0 && classify > submitStart && publishRoom > classify && unlock > publishRoom)
    }

    @Test
    fun everySubmitPathProducesVisibleResolutionAndStructuredDevelopmentLog() {
        val online = source("src/main/java/com/sonharf/game/OnlineGameScreenV6.kt")
        assertTrue(online.contains("validation_result=${if (rejected) \"REJECTED\" else \"ACCEPTED\"}"))
        assertTrue(online.contains("validation_result=REJECTED"))
        listOf("submitted_word=", "normalized_word=", "validation_started=", "validation_result=", "score_delta=", "turn_before=", "turn_after=", "bot_triggered=", "error_code=").forEach {
            assertTrue(online.contains(it), "missing log field $it")
        }
    }

    @Test
    fun scoreFeedbackUsesRealRoomDeltaAndWordRenderIsResponsive() {
        val online = source("src/main/java/com/sonharf/game/OnlineGameScreenV6.kt")
        val ui = source("src/main/java/com/sonharf/game/LightDuelUi.kt")
        assertTrue(online.contains("val scoreDelta = playerScore(result, me) - scoreBefore"))
        assertTrue(online.contains("feedbackScoreDelta = scoreDelta.takeIf { it != 0 }"))
        assertTrue(ui.contains("BoxWithConstraints(Modifier.fillMaxWidth()"))
        assertTrue(ui.contains("overflow = TextOverflow.Ellipsis"))
        assertTrue(ui.contains("feedbackScoreDelta > 0"))
        assertTrue(ui.contains("PUAN"))
        assertFalse(ui.contains("shownLastWord.ifBlank { sh(\"İLK KELİMEYİ YAZ\", \"ENTER FIRST WORD\") },\n                        color = shownLastWordColor,\n                        fontSize = 14.sp"))
    }

    @Test
    fun botPracticeAndExistingRatingProgressStayExplicit() {
        val ui = source("src/main/java/com/sonharf/game/LightDuelUi.kt")
        assertTrue(ui.contains("ANTRENMAN • RATING ETKİLENMEZ"))
        assertTrue(ui.contains("ratingLeagueProgress(playerRating)"))
        assertTrue(ui.contains("${league.leagueName} • $rating"))
        assertTrue(ui.contains("seconds in 1..10"))
    }
}
''')

print("Son Harf scoped patch applied successfully")
