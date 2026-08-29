package com.sonharf.game

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.DailyCipherStatusDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.getDailyCipherStatus
import com.sonharf.game.data.submitDailyCipherGuess
import kotlinx.coroutines.delay
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
    var guess by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    val guessFocusRequester = remember { FocusRequester() }
    val softwareKeyboard = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0

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
        runCatching { backend?.logEvent("daily_cipher_open", SonHarfUiState.language) }
    }

    LaunchedEffect(status?.finished, busy, guess) {
        if (status?.finished == false) {
            delay(120)
            runCatching { guessFocusRequester.requestFocus() }
            softwareKeyboard?.show()
        } else if (status?.finished == true) {
            softwareKeyboard?.hide()
        }
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
                    notice = when {
                        it.won -> sh("Şifre çözüldü! +${it.rewardCoins} Son Coin", "Cipher solved! +${it.rewardCoins} Son Coin")
                        it.finished -> sh("Bugünkü hakların tamamlandı.", "Today's attempts are complete.")
                        else -> sh("İpucunu kullan ve tekrar dene.", "Use the clue and try again.")
                    }
                }
                .onFailure { error ->
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
            Modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp, vertical = if (imeVisible) 5.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(if (imeVisible) 6.dp else 12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back"), tint = CipherText)
                }
                Column(Modifier.weight(1f)) {
                    Text(sh("KELİME AVI", "WORD HUNT"), color = CipherText, fontSize = if (imeVisible) 18.sp else 21.sp, fontWeight = FontWeight.Black)
                    if (!imeVisible) {
                        Text(sh("Günün 5 harfli kelimesini ipuçlarıyla bul.", "Find today's five-letter word using the clues."), color = CipherMuted, fontSize = 10.sp)
                    }
                }
                Text("1×", color = CipherGold, fontWeight = FontWeight.Black)
            }

            CipherHowToCard(compact = imeVisible)

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
                        CipherGuessRow(word, feedback, compact = imeVisible)
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
                            onValueChange = { value ->
                                guess = value.filter { it.isLetter() }.take(5).uppercase()
                            },
                            modifier = Modifier.fillMaxWidth().focusRequester(guessFocusRequester),
                            enabled = true,
                            readOnly = busy || s == null,
                            singleLine = true,
                            label = { Text(sh("5 harfli tahmin", "Five-letter guess")) },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = { submit() }),
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
                        Button(
                            onClick = ::submit,
                            enabled = guess.length == 5 && !busy && s != null,
                            modifier = Modifier.fillMaxWidth().height(if (imeVisible) 44.dp else 52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CipherCyan, contentColor = CipherBg),
                        ) {
                            Text(if (busy) "…" else sh("TAHMİN ET", "GUESS"), fontWeight = FontWeight.Black)
                        }
                    }

                    if (notice.isNotBlank()) {
                        Text(notice, Modifier.fillMaxWidth(), color = CipherGold, textAlign = TextAlign.Center, fontSize = 10.sp)
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
        Surface(shape = RoundedCornerShape(8.dp), color = color) {
            Box(Modifier.size(if (compact) 28.dp else 34.dp), contentAlignment = Alignment.Center) {
                Text(letter, color = CipherText, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(if (compact) 2.dp else 4.dp))
        Text(label, color = CipherMuted, fontSize = if (compact) 6.sp else 7.sp)
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
            Surface(
                modifier = Modifier.weight(1f).height(if (compact) 38.dp else 50.dp),
                shape = RoundedCornerShape(11.dp),
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
