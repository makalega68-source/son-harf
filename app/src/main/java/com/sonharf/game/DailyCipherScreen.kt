package com.sonharf.game

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.DailyCipherStatusDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.getDailyCipherStatus
import com.sonharf.game.data.submitDailyCipherGuess
import kotlinx.coroutines.launch

private val CipherBg = Color(0xFFF7F9FC)
private val CipherPanel = Color.White
private val CipherPanel2 = Color(0xFFF0F4F8)
private val CipherCyan = Color(0xFF1769E0)
private val CipherGold = Color(0xFFF3A81A)
private val CipherGreen = Color(0xFF22B95F)
private val CipherText = Color(0xFF182235)
private val CipherMuted = Color(0xFF718096)

@Composable
fun DailyCipherScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<DailyCipherStatusDto?>(null) }
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var guess by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }

    suspend fun reload() {
        val b = backend
        if (b == null) {
            notice = sh("Kelime Avı çevrimiçi bağlantı gerektirir.", "Word Hunt requires an online connection.")
            return
        }
        status = runCatching { b.getDailyCipherStatus(SonHarfUiState.language) }
            .onFailure { notice = sh("Kelime Avı yüklenemedi.", "Word Hunt could not be loaded.") }
            .getOrNull()
    }

    LaunchedEffect(Unit) {
        reload()
        val b = backend
        val me = b?.currentUserId()
        if (b != null && me != null) profile = runCatching { b.getProfile(me) }.getOrNull()
        runCatching { backend?.logEvent("daily_cipher_open", SonHarfUiState.language) }
    }

    fun submit() {
        val b = backend ?: return
        val clean = guess.trim()
        if (clean.length != 5 || busy) {
            notice = sh("5 harfli bir kelime yaz.", "Enter a five-letter word.")
            return
        }
        busy = true
        notice = ""
        scope.launch {
            runCatching { b.submitDailyCipherGuess(SonHarfUiState.language, clean) }
                .onSuccess {
                    status = it
                    guess = ""
                    when {
                        it.won -> SonHarfSoundFx.victory()
                        it.finished -> SonHarfSoundFx.softNotify()
                        else -> SonHarfSoundFx.scoreTick()
                    }
                    notice = when {
                        it.won -> sh("Şifre çözüldü! +${it.rewardCoins} Son Coin", "Cipher solved! +${it.rewardCoins} Son Coin")
                        it.finished -> sh("Bugünkü hakların tamamlandı.", "Today's attempts are complete.")
                        else -> sh("İpucunu kullan ve tekrar dene.", "Use the clue and try again.")
                    }
                }
                .onFailure { error ->
                    SonHarfSoundFx.warning()
                    notice = when {
                        "guess_already_used" in error.message.orEmpty() -> sh("Bu kelimeyi zaten denedin.", "You already tried that word.")
                        "invalid_five_letter_word" in error.message.orEmpty() -> sh("Geçerli 5 harf gir.", "Enter five valid letters.")
                        else -> sh("Tahmin gönderilemedi.", "Guess could not be submitted.")
                    }
                }
            busy = false
        }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.White, CipherBg, Color(0xFFF1F6FC)))
        )
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back"), tint = CipherText)
                }
                Column(Modifier.weight(1f)) {
                    Text(sh("KELİME AVI", "WORD HUNT"), color = CipherText, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text(sh("Günün 5 harfli kelimesi", "Today's five-letter word"), color = CipherMuted, fontSize = 13.sp)
                }
                Text("1×", color = CipherGold, fontWeight = FontWeight.Black)
            }

            CipherHowToCard(compact = true)

            val s = status
            if (s == null && notice.isBlank()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CipherCyan)
                }
            } else {
                Column(
                    Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(6) { row ->
                        val word = s?.guesses?.getOrNull(row).orEmpty()
                        val feedback = s?.feedbacks?.getOrNull(row).orEmpty()
                        CipherGuessRow(word, feedback, compact = true)
                    }

                    if (s?.finished == true) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CipherPanel),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, if (s.won) CipherGreen else CipherGold),
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                Text(
                                    if (s.won) sh("ŞİFRE ÇÖZÜLDÜ", "CIPHER SOLVED") else sh("BUGÜNLÜK TAMAMLANDI", "DONE FOR TODAY"),
                                    color = if (s.won) CipherGreen else CipherGold,
                                    fontWeight = FontWeight.Black,
                                )
                                Text(s.answer.orEmpty(), color = CipherText, fontSize = 24.sp, fontWeight = FontWeight.Black)
                                if (s.won) Text("+${s.rewardCoins} SON COIN", color = CipherGold, fontWeight = FontWeight.Black)
                                OutlinedButton(
                                    onClick = {
                                        val result = cipherShareText(s)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, result)
                                        }
                                        context.startActivity(Intent.createChooser(intent, sh("Sonucu paylaş", "Share result")))
                                    },
                                    border = BorderStroke(1.dp, CipherCyan),
                                ) {
                                    Icon(Icons.Rounded.Share, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text(sh("SONUCU PAYLAŞ", "SHARE RESULT"), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = guess,
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            enabled = true,
                            readOnly = true,
                            singleLine = true,
                            label = { Text(sh("5 harfli tahmin", "Five-letter guess")) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = CipherText,
                                unfocusedTextColor = CipherText,
                                focusedBorderColor = CipherCyan,
                                unfocusedBorderColor = CipherMuted,
                                focusedLabelColor = CipherCyan,
                                unfocusedLabelColor = CipherMuted,
                                cursorColor = CipherCyan,
                            ),
                        )
                        EmbeddedWordKeyboard(
                            value = guess,
                            language = SonHarfUiState.language,
                            enabled = !busy && s != null,
                            maxLength = 5,
                            onValueChange = { guess = it.filter(Char::isLetter).take(5).uppercase() },
                            onSubmit = { if (guess.length == 5) submit() },
                        )
                    }

                    if (notice.isNotBlank()) {
                        Text(notice, Modifier.fillMaxWidth(), color = CipherGold, textAlign = TextAlign.Center, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CipherHowToCard(compact: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CipherPanel.copy(alpha = .92f)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CipherCyan.copy(alpha = .25f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(if (compact) 7.dp else 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            CipherLegend("A", CipherGreen, sh("Doğru yer", "Exact"), compact)
            CipherLegend("R", CipherGold, sh("Var, yeri farklı", "Wrong spot"), compact)
            CipherLegend("K", CipherPanel2, sh("Yok", "Absent"), compact)
        }
    }
}

@Composable
private fun CipherLegend(letter: String, color: Color, label: String, compact: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val stateShape = when (letter) {
            "A" -> CircleShape
            "R" -> CutCornerShape(9.dp)
            else -> RoundedCornerShape(8.dp)
        }
        Surface(shape = stateShape, color = color, border = BorderStroke(1.dp, CipherText.copy(alpha = .16f))) {
            Box(Modifier.size(if (compact) 34.dp else 40.dp), contentAlignment = Alignment.Center) {
                Text(letter, color = CipherText, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(if (compact) 2.dp else 4.dp))
        Text(label, color = CipherMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CipherGuessRow(word: String, feedback: String, compact: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        repeat(5) { index ->
            val marker = feedback.getOrNull(index)
            val color = when (marker) {
                'G' -> CipherGreen
                'Y' -> CipherGold
                'X' -> CipherPanel2
                else -> Color.Transparent
            }
            val cellModifier = if (compact) {
                Modifier.weight(1f).height(48.dp)
            } else {
                Modifier.weight(1f).aspectRatio(1f)
            }
            Surface(
                modifier = cellModifier,
                shape = when (marker) {
                    'G' -> CircleShape
                    'Y' -> CutCornerShape(12.dp)
                    else -> RoundedCornerShape(11.dp)
                },
                color = color,
                border = BorderStroke(1.dp, if (marker == null) CipherMuted.copy(alpha = .35f) else color),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(word.getOrNull(index)?.toString().orEmpty(), color = CipherText, fontSize = if (compact) 18.sp else 22.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

private fun cipherShareText(status: DailyCipherStatusDto): String = buildString {
    append(if (SonHarfUiState.isEnglish) "Son Harf • Daily Cipher " else "Son Harf • Günün Şifresi ")
    append(if (status.won) "${status.attempts}/${status.maxAttempts}" else "X/${status.maxAttempts}")
    append("\n")
    status.feedbacks.forEach { feedback ->
        feedback.forEach { marker ->
            append(when (marker) {
                'G' -> "🟩"
                'Y' -> "🟨"
                else -> "⬛"
            })
        }
        append("\n")
    }
    append("#SonHarf")
}
