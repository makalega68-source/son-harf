package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
private data class BilBonusRoundDto(
    val id: String,
    @SerialName("room_id") val roomId: String,
    val milestone: Int,
    @SerialName("bonus_points") val bonusPoints: Int = 10,
    @SerialName("question_id") val questionId: Long,
    @SerialName("answer_deadline") val answerDeadline: String? = null,
    @SerialName("resolved_at") val resolvedAt: String? = null,
    @SerialName("result_until") val resultUntil: String? = null,
    @SerialName("host_answer") val hostAnswer: Long? = null,
    @SerialName("guest_answer") val guestAnswer: Long? = null,
    @SerialName("bot_answer") val botAnswer: Long? = null,
    @SerialName("correct_answer") val correctAnswer: Long? = null,
    @SerialName("winner_side") val winnerSide: String? = null,
)

@Serializable
private data class BilBonusQuestionDto(
    val id: Long,
    val language: String,
    val question: String,
    @SerialName("answer_unit") val answerUnit: String? = null,
    @SerialName("question_kind") val questionKind: String = "bil_bakalim",
)

@Serializable
private data class BilBonusAnswerDto(
    @SerialName("round_id") val roundId: String,
    @SerialName("player_id") val playerId: String,
    @SerialName("answer_index") val answerValue: Long,
)

private val BilBg = LetharaPalette.Night
private val BilPanel = Color.White
private val BilInk = LetharaPalette.Text
private val BilMuted = LetharaPalette.Muted
private val BilGold = LetharaPalette.Gold
private val BilGreen = LetharaPalette.Green
private val BilRed = LetharaPalette.Red
private val BilBlue = LetharaPalette.Cyan

/**
 * Server-synchronised closest-estimate bonus round.
 * The database pauses the word game at 10 valid words in Normal mode and 15 in Expert,
 * accepts one hidden estimate per player, resolves the closest answer, awards 10 points,
 * then resumes the exact game state that was paused.
 */
@Composable
fun BilBakalimBonusOverlay() {
    if (!SupabaseProvider.configured) return

    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var round by remember { mutableStateOf<BilBonusRoundDto?>(null) }
    var question by remember { mutableStateOf<BilBonusQuestionDto?>(null) }
    var myAnswer by remember { mutableStateOf<Long?>(null) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(20) }
    var hostProfile by remember { mutableStateOf<ProfileDto?>(null) }
    var guestProfile by remember { mutableStateOf<ProfileDto?>(null) }
    val answerFocusRequester = remember { FocusRequester() }
    val softwareKeyboard = LocalSoftwareKeyboardController.current

    suspend fun discoverRoom(): GameRoomDto? {
        val me = backend.currentUserId() ?: return null
        return SupabaseProvider.client.from("game_rooms").select().decodeList<GameRoomDto>()
            .filter { (it.hostId == me || it.guestId == me) && it.status == "quiz" }
            .maxByOrNull { it.validWordCount }
    }

    suspend fun refresh() {
        val r = room?.let { runCatching { backend.getRoom(it.id) }.getOrNull() } ?: runCatching { discoverRoom() }.getOrNull()
        if (r == null || r.status != "quiz") {
            room = null
            round = null
            question = null
            myAnswer = null
            input = ""
            return
        }
        room = r
        val qRound = SupabaseProvider.client.from("trivia_rounds").select {
            filter { eq("room_id", r.id) }
        }.decodeList<BilBonusRoundDto>().maxByOrNull { it.milestone }
        if (qRound == null) return
        round = qRound

        if (question?.id != qRound.questionId) {
            question = runCatching {
                SupabaseProvider.client.from("trivia_questions").select {
                    filter { eq("id", qRound.questionId) }
                }.decodeSingle<BilBonusQuestionDto>()
            }.getOrNull()
        }

        val me = backend.currentUserId()
        if (me != null && myAnswer == null) {
            myAnswer = runCatching {
                SupabaseProvider.client.from("trivia_answers").select {
                    filter { eq("round_id", qRound.id); eq("player_id", me) }
                }.decodeList<BilBonusAnswerDto>().firstOrNull()?.answerValue
            }.getOrNull()
        }

        if (hostProfile == null) hostProfile = runCatching { backend.getProfile(r.hostId) }.getOrNull()
        if (!r.isBot && guestProfile == null && r.guestId != null) guestProfile = runCatching { backend.getProfile(r.guestId!!) }.getOrNull()
    }

    suspend fun claimTimeout(roundId: String) {
        runCatching {
            SupabaseProvider.client.postgrest.rpc(
                "claim_estimate_timeout_v1",
                buildJsonObject { put("p_round_id", roundId) },
            ).decodeSingle<GameRoomDto>()
        }
        refresh()
    }

    suspend fun finishResult(roundId: String) {
        runCatching {
            SupabaseProvider.client.postgrest.rpc(
                "finish_bilbakalim_result_v1",
                buildJsonObject { put("p_round_id", roundId) },
            ).decodeSingle<GameRoomDto>()
        }
        refresh()
    }

    LaunchedEffect(Unit) {
        while (true) {
            runCatching { refresh() }
            delay(450)
        }
    }

    val activeRound = round ?: return
    val activeRoom = room ?: return
    val activeQuestion = question ?: return
    if (activeQuestion.questionKind != "bil_bakalim") return

    LaunchedEffect(activeRound.id, activeRound.answerDeadline, activeRound.resolvedAt, activeRound.resultUntil) {
        if (activeRound.resolvedAt == null) {
            while (true) {
                val deadline = activeRound.answerDeadline?.let { runCatching { Instant.parse(it) }.getOrNull() }
                seconds = if (deadline == null) 20 else (deadline.epochSecond - Instant.now().epochSecond).toInt().coerceAtLeast(0)
                if (seconds <= 0) {
                    claimTimeout(activeRound.id)
                    break
                }
                delay(250)
            }
        } else {
            val until = activeRound.resultUntil?.let { runCatching { Instant.parse(it) }.getOrNull() }
            if (until != null) {
                val waitMs = ((until.toEpochMilli() - Instant.now().toEpochMilli()).coerceAtLeast(0L))
                delay(waitMs)
            } else delay(4200)
            finishResult(activeRound.id)
        }
    }

    val me = backend.currentUserId()
    val host = me == activeRoom.hostId
    val myName = if (host) hostProfile?.displayName ?: "Sen" else guestProfile?.displayName ?: "Sen"
    val opponentName = if (activeRoom.isBot) "${activeRoom.botName ?: "KelimeBot"} BOT" else if (host) guestProfile?.displayName ?: "Rakip" else hostProfile?.displayName ?: "Rakip"
    val resolved = activeRound.resolvedAt != null
    val myResolvedAnswer = if (host) activeRound.hostAnswer else activeRound.guestAnswer
    val opponentResolvedAnswer = if (activeRoom.isBot) activeRound.botAnswer else if (host) activeRound.guestAnswer else activeRound.hostAnswer
    val mySide = if (host) "host" else "guest"
    val iWon = activeRound.winnerSide == mySide || activeRound.winnerSide == "tie"
    val opponentWon = activeRound.winnerSide == (if (activeRoom.isBot) "bot" else if (host) "guest" else "host") || activeRound.winnerSide == "tie"

    LaunchedEffect(activeRound.id, resolved, myAnswer, busy) {
        if (!resolved && myAnswer == null && !busy) {
            delay(140)
            runCatching { answerFocusRequester.requestFocus() }
            softwareKeyboard?.show()
        } else {
            softwareKeyboard?.hide()
        }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.White, SonHarfBg, SonHarfSurface2))
        ).statusBarsPadding().navigationBarsPadding().padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BilPanel),
            border = BorderStroke(2.dp, BilGold.copy(alpha = .62f)),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                Text(sh("BİL BAKALIM • BONUS TURU", "BIL BAKALIM • BONUS ROUND"), color = BilGold, fontWeight = FontWeight.Black, fontSize = 24.sp, textAlign = TextAlign.Center)
                Text("Doğru cevaba en yakın cevap kazanır.", color = BilMuted, fontSize = 12.sp, textAlign = TextAlign.Center)

                if (!resolved) {
                    Surface(shape = RoundedCornerShape(100.dp), color = if (seconds <= 5) BilRed else BilGold) {
                        Text("$seconds", Modifier.padding(horizontal = 20.dp, vertical = 7.dp), color = if (seconds <= 5) Color.White else Color(0xFF211830), fontWeight = FontWeight.Black, fontSize = 26.sp)
                    }
                }

                HorizontalDivider(color = BilMuted.copy(alpha = .18f))
                Text(activeQuestion.question, color = BilInk, fontWeight = FontWeight.Black, fontSize = 24.sp, lineHeight = 31.sp, textAlign = TextAlign.Center)

                if (!resolved && myAnswer == null) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { raw -> input = raw.filter(Char::isDigit).take(12) },
                        modifier = Modifier.fillMaxWidth().focusRequester(answerFocusRequester),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 32.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center),
                        placeholder = { Text("Tahminin", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            val value = input.toLongOrNull()
                            if (value != null && value <= Int.MAX_VALUE && !busy) scope.launch {
                                busy = true
                                runCatching { backend.answerTrivia(activeRound.id, value.toInt()) }.onSuccess { myAnswer = value }
                                refresh()
                                busy = false
                            }
                        }),
                        shape = RoundedCornerShape(18.dp),
                    )
                    Button(
                        onClick = {
                            val value = input.toLongOrNull() ?: return@Button
                            if (value > Int.MAX_VALUE || busy) return@Button
                            scope.launch {
                                busy = true
                                runCatching { backend.answerTrivia(activeRound.id, value.toInt()) }.onSuccess { myAnswer = value }
                                refresh()
                                busy = false
                            }
                        },
                        enabled = input.toLongOrNull()?.let { it <= Int.MAX_VALUE } == true && !busy,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BilBlue),
                        shape = RoundedCornerShape(16.dp),
                    ) { Text("CEVABI KİLİTLE", fontWeight = FontWeight.Black, fontSize = 16.sp) }
                } else if (!resolved) {
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = SonHarfSurface2, border = BorderStroke(1.dp, BilBlue.copy(alpha = .4f))) {
                        Column(Modifier.fillMaxWidth().padding(17.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("CEVABIN KİLİTLENDİ", color = BilBlue, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text(myAnswer?.toString() ?: "—", color = BilInk, fontWeight = FontWeight.Black, fontSize = 34.sp)
                            Text("Rakibin cevabı bekleniyor…", color = BilMuted, fontSize = 12.sp)
                        }
                    }
                } else {
                    Text("DOĞRU CEVAP", color = BilMuted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        buildString {
                            append(activeRound.correctAnswer ?: 0)
                            if (!activeQuestion.answerUnit.isNullOrBlank()) append(" ${activeQuestion.answerUnit}")
                        },
                        color = BilInk,
                        fontWeight = FontWeight.Black,
                        fontSize = 38.sp,
                        textAlign = TextAlign.Center,
                    )

                    ResultAnswerCard(myName, myResolvedAnswer, winner = iWon)
                    ResultAnswerCard(opponentName, opponentResolvedAnswer, winner = opponentWon)

                    Spacer(Modifier.height(2.dp))
                    if (iWon) {
                        Text(if (activeRound.winnerSide == "tie") "BERABERE" else "KAZANDIN!", color = BilGreen, fontWeight = FontWeight.Black, fontSize = 29.sp, textAlign = TextAlign.Center)
                        Text("Doğru cevap • +${activeRound.bonusPoints} puan", color = BilGreen, fontWeight = FontWeight.Black, fontSize = 17.sp, textAlign = TextAlign.Center)
                    } else {
                        Text("YANLIŞ CEVAP", color = BilRed, fontWeight = FontWeight.Black, fontSize = 28.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultAnswerCard(name: String, answer: Long?, winner: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (winner) BilGreen.copy(alpha = .10f) else Color(0xFF15284A),
        border = BorderStroke(if (winner) 2.dp else 1.dp, if (winner) BilGreen else BilMuted.copy(alpha = .18f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(name, color = BilMuted, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
            Text(
                answer?.toString() ?: "Cevap yok",
                color = if (winner) BilGreen else BilInk,
                fontWeight = FontWeight.Black,
                fontSize = if (winner) 36.sp else 30.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
