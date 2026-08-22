package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.GameRoomDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import java.text.NumberFormat
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val BilBg = Color(0xFFF2EFE6)
private val BilCard = Color(0xFFFFFCF4)
private val BilInk = Color(0xFF263238)
private val BilMuted = Color(0xFF6B736E)
private val BilTeal = Color(0xFF1C8C8C)
private val BilGold = Color(0xFFF1B83B)
private val BilGreen = Color(0xFF20A45B)
private val BilRed = Color(0xFFD96B57)
private val BilPurple = Color(0xFF8066A8)

@Serializable
private data class BilRoundDto(
    val id: String,
    @SerialName("room_id") val roomId: String,
    val milestone: Int,
    @SerialName("bonus_points") val bonusPoints: Int = 10,
    @SerialName("question_id") val questionId: Long,
    @SerialName("answer_deadline") val answerDeadline: String? = null,
    @SerialName("host_answer") val hostAnswer: Long? = null,
    @SerialName("guest_answer") val guestAnswer: Long? = null,
    @SerialName("bot_answer") val botAnswer: Long? = null,
    @SerialName("correct_answer") val correctAnswer: Long? = null,
    @SerialName("winner_side") val winnerSide: String? = null,
    @SerialName("resolved_at") val resolvedAt: String? = null,
    @SerialName("result_until") val resultUntil: String? = null,
)

@Serializable
private data class BilQuestionDto(
    val id: Long,
    val question: String,
    @SerialName("answer_unit") val answerUnit: String? = null,
    @SerialName("question_kind") val questionKind: String? = null,
)

@Serializable private data class BilOwnAnswerDto(@SerialName("answer_index") val answerIndex: Long)
@Serializable private data class BilNameDto(@SerialName("display_name") val displayName: String)

@Composable
fun BilBakalimOverlay() {
    val backend = remember { OnlineGameBackend() }
    val me = backend.currentUserId() ?: return
    var room by remember { mutableStateOf<GameRoomDto?>(null) }
    var round by remember { mutableStateOf<BilRoundDto?>(null) }
    var question by remember { mutableStateOf<BilQuestionDto?>(null) }
    var myAnswer by remember { mutableStateOf<Long?>(null) }
    var hostName by remember { mutableStateOf("1. Oyuncu") }
    var guestName by remember { mutableStateOf("2. Oyuncu") }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var nowEpoch by remember { mutableLongStateOf(Instant.now().epochSecond) }

    LaunchedEffect(Unit) {
        while (isActive) {
            nowEpoch = Instant.now().epochSecond
            val activeRoom = runCatching {
                SupabaseProvider.client.from("game_rooms")
                    .select { filter { eq("status", "quiz") } }
                    .decodeList<GameRoomDto>()
                    .filter { it.hostId == me || it.guestId == me }
                    .maxByOrNull { it.validWordCount }
            }.getOrNull()

            if (activeRoom == null) {
                room = null; round = null; question = null; myAnswer = null
                delay(350)
                continue
            }

            val latestRound = runCatching {
                SupabaseProvider.client.from("trivia_rounds")
                    .select { filter { eq("room_id", activeRoom.id) } }
                    .decodeList<BilRoundDto>()
                    .maxByOrNull { it.milestone }
            }.getOrNull()
            val q = latestRound?.let { r ->
                runCatching {
                    SupabaseProvider.client.from("trivia_questions")
                        .select { filter { eq("id", r.questionId) } }
                        .decodeSingle<BilQuestionDto>()
                }.getOrNull()
            }

            if (latestRound == null || q?.questionKind != "bil_bakalim") {
                room = null; round = null; question = null; myAnswer = null
                delay(350)
                continue
            }

            room = activeRoom
            round = latestRound
            question = q
            myAnswer = runCatching {
                SupabaseProvider.client.from("trivia_answers")
                    .select { filter { eq("round_id", latestRound.id); eq("player_id", me) } }
                    .decodeList<BilOwnAnswerDto>()
                    .firstOrNull()?.answerIndex
            }.getOrNull()

            if (activeRoom.hostId == me) {
                hostName = runCatching { backend.getProfile(activeRoom.hostId).displayName }.getOrDefault("Sen")
            } else {
                hostName = runCatching { backend.getProfile(activeRoom.hostId).displayName }.getOrDefault("1. Oyuncu")
            }
            guestName = if (activeRoom.isBot) {
                activeRoom.botName ?: "KelimeBot"
            } else {
                activeRoom.guestId?.let { runCatching { backend.getProfile(it).displayName }.getOrNull() } ?: "2. Oyuncu"
            }

            val deadline = latestRound.answerDeadline?.let { runCatching { Instant.parse(it).epochSecond }.getOrNull() }
            if (latestRound.resolvedAt == null && deadline != null && nowEpoch >= deadline) {
                runCatching {
                    SupabaseProvider.client.postgrest.rpc(
                        "claim_estimate_timeout_v1",
                        buildJsonObject { put("p_round_id", latestRound.id) },
                    ).decodeSingle<GameRoomDto>()
                }
            }

            val resultUntil = latestRound.resultUntil?.let { runCatching { Instant.parse(it).epochSecond }.getOrNull() }
            if (latestRound.resolvedAt != null && resultUntil != null && nowEpoch >= resultUntil) {
                runCatching {
                    SupabaseProvider.client.postgrest.rpc(
                        "finish_bilbakalim_result_v1",
                        buildJsonObject { put("p_round_id", latestRound.id) },
                    ).decodeSingle<GameRoomDto>()
                }
            }
            delay(350)
        }
    }

    LaunchedEffect(round?.id) { input = ""; sending = false }

    val r = round ?: return
    val g = room ?: return
    val q = question ?: return
    val deadlineEpoch = r.answerDeadline?.let { runCatching { Instant.parse(it).epochSecond }.getOrNull() }
    val seconds = ((deadlineEpoch ?: nowEpoch) - nowEpoch).toInt().coerceIn(0, 20)
    val resolved = r.resolvedAt != null
    val submitted = myAnswer != null
    val meIsHost = g.hostId == me
    val mySide = if (meIsHost) "host" else "guest"
    val opponentSide = if (g.isBot) "bot" else if (meIsHost) "guest" else "host"
    val secondAnswer = if (g.isBot) r.botAnswer else r.guestAnswer

    Surface(Modifier.fillMaxSize(), color = BilBg) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("BİL BAKALIM", color = BilTeal, fontWeight = FontWeight.Black, fontSize = 30.sp, textAlign = TextAlign.Center)
            Text("Doğru cevaba en yakın cevap kazanır.", color = BilMuted, fontSize = 13.sp, textAlign = TextAlign.Center)

            Surface(shape = CircleShape, color = if (seconds <= 5 && !resolved) BilRed else BilGold) {
                Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                    Text(if (resolved) "✓" else seconds.toString(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 30.sp)
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = BilCard,
                border = BorderStroke(2.dp, BilGold.copy(alpha = 0.75f)),
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(q.question, color = BilInk, fontWeight = FontWeight.Black, fontSize = 22.sp, lineHeight = 28.sp, textAlign = TextAlign.Center)
                }
            }

            if (!resolved) {
                Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFFE8F4F1), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (submitted) "CEVABIN" else "TAHMİNİN", color = BilMuted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            when {
                                submitted -> formatBilValue(myAnswer, q.answerUnit)
                                input.isBlank() -> "—"
                                else -> formatBilValue(input.toLongOrNull(), q.answerUnit)
                            },
                            color = BilTeal,
                            fontWeight = FontWeight.Black,
                            fontSize = 34.sp,
                            textAlign = TextAlign.Center,
                        )
                        if (submitted) Text("Rakibin cevabı bekleniyor…", color = BilMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }

                Spacer(Modifier.weight(1f))
                BilNumberPad(
                    enabled = !submitted && !sending && seconds > 0,
                    submitEnabled = input.isNotBlank() && !submitted && !sending && seconds > 0,
                    onDigit = { if (input.length < 9) input += it },
                    onDelete = { if (input.isNotEmpty()) input = input.dropLast(1) },
                    onSubmit = {
                        val value = input.toIntOrNull() ?: return@BilNumberPad
                        sending = true
                        kotlinx.coroutines.GlobalScope
                        val localBackend = backend
                        @Suppress("UNUSED_VARIABLE") val ignored = localBackend
                    },
                )
                Button(
                    onClick = {
                        val value = input.toIntOrNull() ?: return@Button
                        sending = true
                    },
                    enabled = false,
                    modifier = Modifier.size(0.dp),
                ) { }
            } else {
                BilResultCard(
                    hostName = hostName,
                    guestName = guestName,
                    hostAnswer = r.hostAnswer,
                    guestAnswer = secondAnswer,
                    winnerSide = r.winnerSide,
                    correctAnswer = r.correctAnswer,
                    unit = q.answerUnit,
                    meSide = mySide,
                    opponentSide = opponentSide,
                    myAnswered = myAnswer != null,
                )
                Spacer(Modifier.weight(1f))
                Text("Kelime düellosu birkaç saniye içinde devam edecek…", color = BilMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    }

    if (!resolved && !submitted && sending) {
        LaunchedEffect(sending) {
            val value = input.toIntOrNull()
            if (value != null) runCatching { backend.answerTrivia(r.id, value) }
            sending = false
        }
    }
}

@Composable
private fun BilNumberPad(enabled: Boolean, submitEnabled: Boolean, onDigit: (String) -> Unit, onDelete: () -> Unit, onSubmit: () -> Unit) {
    val rows = listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"))
    Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFDDEBE6), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { digit ->
                        Button(onClick = { onDigit(digit) }, enabled = enabled, modifier = Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = BilCard, contentColor = BilInk)) { Text(digit, fontWeight = FontWeight.Black, fontSize = 20.sp) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = onDelete, enabled = enabled, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = BilRed)) { Text("⌫", fontSize = 20.sp) }
                Button(onClick = { onDigit("0") }, enabled = enabled, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = BilCard, contentColor = BilInk)) { Text("0", fontWeight = FontWeight.Black, fontSize = 20.sp) }
                Button(onClick = onSubmit, enabled = submitEnabled, modifier = Modifier.weight(1.5f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = BilTeal)) { Text("CEVAPLA", fontWeight = FontWeight.Black, fontSize = 13.sp) }
            }
        }
    }
}

@Composable
private fun BilResultCard(hostName: String, guestName: String, hostAnswer: Long?, guestAnswer: Long?, winnerSide: String?, correctAnswer: Long?, unit: String?, meSide: String, opponentSide: String, myAnswered: Boolean) {
    val hostWinner = winnerSide == "host" || winnerSide == "tie"
    val guestWinner = winnerSide == opponentSide || winnerSide == "tie"
    val iWon = winnerSide == meSide || winnerSide == "tie"
    val winnerName = when (winnerSide) {
        "host" -> hostName
        "guest", "bot" -> guestName
        "tie" -> "Berabere"
        else -> "—"
    }

    Surface(shape = RoundedCornerShape(24.dp), color = BilCard, border = BorderStroke(2.dp, if (iWon) BilGreen else BilGold), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("CEVAPLAR", color = BilPurple, fontWeight = FontWeight.Black, fontSize = 15.sp)
            BilAnswerLine("1. $hostName", hostAnswer, unit, hostWinner)
            BilAnswerLine("2. $guestName", guestAnswer, unit, guestWinner)
            HorizontalDivider(color = BilGold.copy(alpha = .45f))
            Text("DOĞRU CEVAP", color = BilMuted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(formatBilValue(correctAnswer, unit), color = BilInk, fontWeight = FontWeight.Black, fontSize = 36.sp, textAlign = TextAlign.Center)
            Text("KAZANAN: $winnerName", color = if (winnerSide == "none") BilMuted else BilGreen, fontWeight = FontWeight.Black, fontSize = 20.sp, textAlign = TextAlign.Center)
            Surface(shape = RoundedCornerShape(14.dp), color = if (iWon) Color(0xFFDDF5E5) else Color(0xFFF9E4DF)) {
                Text(
                    when {
                        iWon -> "DOĞRU CEVAP • +10 PUAN"
                        !myAnswered -> "SÜRE DOLDU • YANLIŞ CEVAP"
                        else -> "YANLIŞ CEVAP"
                    },
                    Modifier.fillMaxWidth().padding(13.dp),
                    color = if (iWon) BilGreen else BilRed,
                    fontWeight = FontWeight.Black,
                    fontSize = if (iWon) 19.sp else 17.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun BilAnswerLine(label: String, value: Long?, unit: String?, winner: Boolean) {
    Surface(shape = RoundedCornerShape(14.dp), color = if (winner) Color(0xFFDDF5E5) else Color(0xFFF2F0E9), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = BilMuted, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center)
            Text(if (value == null) "SÜRE DOLDU" else formatBilValue(value, unit), color = if (winner) BilGreen else BilInk, fontWeight = FontWeight.Black, fontSize = if (winner) 29.sp else 24.sp, textAlign = TextAlign.Center)
        }
    }
}

private fun formatBilValue(value: Long?, unit: String?): String {
    if (value == null) return "—"
    val locale = if (SonHarfUiState.language == "en") Locale.US else Locale("tr", "TR")
    val number = NumberFormat.getIntegerInstance(locale).format(value)
    return if (unit.isNullOrBlank()) number else "$number $unit"
}
