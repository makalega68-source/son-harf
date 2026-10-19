package com.sonharf.game

import android.content.Intent
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

internal fun dailyLastLetterRemaining(endsAt: String?, serverNow: String, elapsedMillis: Long): Int {
    if (endsAt == null) return 60
    val end = runCatching { requireServerInstant(endsAt).toEpochMilli() }.getOrNull() ?: return 0
    val now = runCatching { requireServerInstant(serverNow).toEpochMilli() }.getOrNull() ?: return 0
    return ((end - now - elapsedMillis.coerceAtLeast(0) + 999) / 1_000).toInt().coerceIn(0, 60)
}

@Composable
internal fun DailyLastLetterScreen(language: String, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val foreground = rememberAppForeground()
    val locale = if (language == "en") Locale.ENGLISH else Locale.forLanguageTag("tr-TR")
    var run by remember(language) { mutableStateOf<LastLetterDailyRun?>(null) }
    var anchor by remember(language) { mutableLongStateOf(0L) }
    var elapsed by remember(language) { mutableLongStateOf(0L) }
    var busy by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    fun adopt(next: LastLetterDailyRun) {
        val old = run
        if (old?.id != null && old.id == next.id &&
            (next.words.size < old.words.size || (old.finished && !next.finished))) return
        run = next
        anchor = SystemClock.elapsedRealtime()
        elapsed = 0L
        failed = false
    }
    LaunchedEffect(language, foreground, refresh) {
        if (!foreground) return@LaunchedEffect
        gameRequestResult { lastLetterDaily("status", language) }.onSuccess(::adopt).onFailure { failed = true }
    }
    LaunchedEffect(foreground) {
        if (foreground) while (true) { elapsed = (SystemClock.elapsedRealtime() - anchor).coerceAtLeast(0); delay(200) }
    }
    val current = run
    val remaining = current?.let { dailyLastLetterRemaining(it.endsAt, it.serverNow, elapsed) } ?: 60
    val ended = current != null && (current.finished || (current.id != null && remaining == 0))
    val playing = current?.id != null && !ended
    fun act(action: String) {
        if (busy || current == null) return
        val submitted = input.trim()
        if (action == "word" && (!playing || submitted.isBlank())) return
        busy = true
        scope.launch {
            try {
                gameRequestResult { lastLetterDaily(action, language, current.id, submitted.takeIf { action == "word" }) }
                    .onSuccess { next -> adopt(next); if (action == "word") input = ""; notice = null }
                    .onFailure { error ->
                        val raw = error.message.orEmpty()
                        notice = when {
                            "wrong_start_letter" in raw -> sh("Kelime, son kelimenin son harfiyle başlamalı.", "Start with the last letter of the previous word.")
                            "word_already_used" in raw -> sh("Bu kelime zincirde zaten var.", "That word is already in the chain.")
                            "invalid_length" in raw -> sh("En az iki harf yaz.", "Use at least two letters.")
                            "not_in_dictionary" in raw -> sh("Bu kelime ana sözlükte bulunmuyor.", "This word is not in the main dictionary.")
                            "proper_noun" in raw -> sh("Özel isimler geçerli değil.", "Proper nouns are not allowed.")
                            "abbreviation_not_allowed" in raw -> sh("Kısaltmalar geçerli değil.", "Abbreviations are not allowed.")
                            "invalid_characters" in raw -> sh("Yalnız harflerden oluşan bir kelime yaz.", "Use a word containing only letters.")
                            "not_game_allowed" in raw -> sh("Bu sözcük oyun sözlüğünde geçerli sayılmıyor.", "This word is not eligible for play.")
                            "ends_with_soft_g" in raw -> sh("Ğ ile biten kelime kullanılamaz.", "Words ending in Ğ are not allowed.")
                            else -> sh("İşlem tamamlanamadı. Bağlantını kontrol edip tekrar dene.", "Could not complete the request. Check your connection and retry.")
                        }
                    }
            } finally { busy = false }
        }
    }
    Column(Modifier.fillMaxSize().background(LobbyPalette.Ground).statusBarsPadding().navigationBarsPadding().imePadding().padding(16.dp)
        .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MainScreenHeader(sh("Günün Zinciri", "Daily Chain"), sh("Son Harf · Günlük mücadele", "Last Letter · Daily challenge"), onBack = onBack)
        Text(sh("Herkese aynı başlangıç. 60 saniye, günde tek deneme ve ipucusuz oyun.",
            "The same start for everyone. 60 seconds, one attempt a day and no hints."), color = LobbyPalette.Muted)
        Text(sh("Bu alıştırmada RP veya XP kazanılmaz.", "This practice challenge does not award RP or XP."), color = LobbyPalette.Muted, fontSize = 12.sp)
        if (current == null && !failed) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (failed) TextButton(onClick = { refresh++ }) { Text(sh("Bağlantı kurulamadı · Yenile", "Connection unavailable · Retry")) }
        current?.let { value ->
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = LobbyPalette.Paper,
                border = BorderStroke(1.dp, LobbyPalette.Line)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(value.day, color = LobbyPalette.Muted, fontSize = 12.sp)
                    Text(value.startWord.uppercase(locale), color = LobbyPalette.Ink, fontWeight = FontWeight.Black, fontSize = 27.sp)
                    Text(sh("${value.words.size} kelime · ${if (ended) 0 else remaining} saniye", "${value.words.size} words · ${if (ended) 0 else remaining} seconds"),
                        color = LobbyPalette.Accent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    if (value.words.isNotEmpty()) Text(value.words.joinToString(" → ") { it.uppercase(locale) }, color = LobbyPalette.Ink, lineHeight = 23.sp)
                }
            }
            if (value.id == null) {
                Text(sh("Başladığında 60 saniye işler. Ekrandan ayrılsan da süre devam eder.",
                    "Your 60 seconds begin when you start and keep running if you leave this screen."), color = LobbyPalette.Muted, fontSize = 12.sp)
                Button(onClick = { act("start") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "…" else sh("BAŞLA", "START")) }
            } else if (playing) {
                val required = value.words.lastOrNull()?.lastOrNull() ?: value.startWord.last()
                val requiredLabel = required.toString().uppercase(locale)
                OutlinedTextField(value = input, onValueChange = { input = it.take(30) }, modifier = Modifier.fillMaxWidth(), enabled = !busy,
                    label = { Text(sh("$requiredLabel ile başlayan kelime", "A word starting with $requiredLabel")) },
                    singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { act("word") }))
                Button(onClick = { act("word") }, enabled = !busy && input.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text(if (busy) "…" else sh("ZİNCİRE EKLE", "ADD WORD"))
                }
            } else {
                Text(sh("Bugünkü mücadele tamamlandı. Yarın yeni bir başlangıç kelimesiyle buluşalım.",
                    "Today's challenge is complete. Come back tomorrow for a new starting word."), color = LobbyPalette.Ink)
                OutlinedButton(enabled = !busy, onClick = {
                    val text = if (language == "en") "Word Throne · Daily Chain\n${value.day}\nStart: ${value.startWord.uppercase(locale)}\nMy chain: ${value.words.size} words in 60 seconds. How many can you find?"
                    else "Kelime Tahtı · Günün Zinciri\n${value.day}\nBaşlangıç: ${value.startWord.uppercase(locale)}\n60 saniyede ${value.words.size} kelime. Sen kaç kelime bulabilirsin?"
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, sh("Sonucunu paylaş", "Share your result")))
                }, modifier = Modifier.fillMaxWidth()) { Text(sh("SONUCUMU PAYLAŞ", "SHARE MY RESULT")) }
            }
        }
        notice?.let { Text(it, color = SonHarfTheme.Error) }
        Spacer(Modifier.height(16.dp))
    }
}
