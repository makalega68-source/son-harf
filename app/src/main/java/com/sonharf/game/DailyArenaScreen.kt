package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.EmojiEvents
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.math.ceil

@Composable
fun DailyArenaScreen(onBack: () -> Unit) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    val language = SonHarfUiState.language

    var status by remember { mutableStateOf<DailyArenaStatusDto?>(null) }
    var words by remember { mutableStateOf<List<DailyArenaWordDto>>(emptyList()) }
    var leaderboard by remember { mutableStateOf<List<DailyArenaLeaderboardDto>>(emptyList()) }
    var leaderboardProfiles by remember { mutableStateOf<Map<String, ProfileDto?>>(emptyMap()) }
    var input by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val inputFocusRequester = remember { FocusRequester() }
    val softwareKeyboard = LocalSoftwareKeyboardController.current

    suspend fun reload() {
        val b = backend ?: return
        runCatching {
            val next = b.getDailyArenaStatus(language)
            status = next
            words = next.runId?.let { b.getDailyArenaWords(it) }.orEmpty()
            val nextLeaderboard = b.getDailyArenaLeaderboard(language, 50)
            leaderboard = nextLeaderboard
            val nextProfiles = mutableMapOf<String, ProfileDto?>()
            for (row in nextLeaderboard) {
                nextProfiles[row.userId] = runCatching { b.getProfile(row.userId) }.getOrNull()
            }
            leaderboardProfiles = nextProfiles
        }.onFailure {
            notice = sh("Günlük Arena yüklenemedi.", "Daily Arena could not be loaded.")
        }
        loading = false
    }

    LaunchedEffect(language) { reload() }

    LaunchedEffect(status?.status, busy, status?.runId) {
        if (status?.status == "playing") {
            delay(120)
            runCatching { inputFocusRequester.requestFocus() }
            softwareKeyboard?.show()
        } else if (status?.status == "finished") {
            softwareKeyboard?.hide()
        }
    }

    LaunchedEffect(status?.runId, status?.status, status?.endsAt) {
        if (status?.status == "playing") {
            while (status?.status == "playing") {
                nowMs = System.currentTimeMillis()
                val end = status?.endsAt?.let(::dailyArenaEpochMs) ?: break
                if (nowMs >= end) {
                    reload()
                    break
                }
                delay(200)
            }
        }
    }

    DisposableEffect(status?.status) {
        if (status?.status == "playing") SonHarfUiState.inMatch = true
        onDispose {
            if (status?.status == "playing") SonHarfUiState.inMatch = false
        }
    }

    BackHandler {
        if (status?.status == "playing") {
            notice = sh(
                "Resmî koşu sürüyor; süre arka planda devam eder.",
                "Official run is active; the timer keeps running.",
            )
        } else onBack()
    }

    val s = status
    val startMs = s?.startsAt?.let(::dailyArenaEpochMs) ?: 0L
    val endMs = s?.endsAt?.let(::dailyArenaEpochMs) ?: 0L
    val prepSeconds = if (s?.status == "playing" && nowMs < startMs)
        ceil((startMs - nowMs) / 1000.0).toInt().coerceAtLeast(1) else 0
    val remainingSeconds = if (s?.status == "playing" && nowMs >= startMs)
        ceil((endMs - nowMs).coerceAtLeast(0L) / 1000.0).toInt() else 60

    fun submit() {
        val b = backend ?: return
        val runId = status?.runId ?: return
        val clean = input.trim()
        if (clean.length !in 3..10 || busy || prepSeconds > 0 || remainingSeconds <= 0) return

        busy = true
        notice = ""
        scope.launch {
            runCatching { b.submitDailyArenaWord(runId, clean) }
                .onSuccess { result ->
                    if (result.accepted) {
                        input = ""
                        words = runCatching { b.getDailyArenaWords(runId) }.getOrDefault(words)
                        status = status?.copy(
                            score = result.score,
                            wordCount = result.wordCount,
                            bestCombo = maxOf(status?.bestCombo ?: 0, result.combo),
                        )
                        if (result.combo >= 2) {
                            notice = sh(
                                "🔥 ${result.combo}× hızlı seri",
                                "🔥 ${result.combo}× fast combo",
                            )
                        }
                    } else if (result.status == "finished") {
                        reload()
                    }
                }
                .onFailure { e ->
                    val raw = e.message.orEmpty()
                    notice = when {
                        "daily_arena_duplicate_word" in raw ->
                            sh("Bu kelimeyi zaten kullandın.", "You already used this word.")
                        "daily_arena_letters_mismatch" in raw ->
                            sh("Kelime yalnız verilen harflerden oluşmalı.", "Use only the given letters.")
                        "daily_arena_invalid_word" in raw ->
                            sh("Sözlükte geçerli bir kelime değil.", "This is not a valid dictionary word.")
                        "daily_arena_word_length" in raw ->
                            sh("Kelime 3–10 harf olmalı.", "Word must be 3–10 letters.")
                        else -> sh("Kelime kabul edilmedi.", "Word was not accepted.")
                    }
                }
            busy = false
        }
    }

    Column(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color.White, Color(0xFFF7F9FC), Color(0xFFF1F6FC))
            )
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    if (status?.status == "playing") {
                        notice = sh(
                            "Resmî koşu sürüyor; süre devam ediyor.",
                            "Official run is active; timer continues.",
                        )
                    } else onBack()
                }
            ) {
                Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back"))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    sh("GÜNLÜK ARENA", "DAILY ARENA"),
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    sh(
                        "Aynı harfler • Tek resmî deneme • 60 saniye",
                        "Same letters • One official run • 60 seconds",
                    ),
                    color = SonHarfMuted,
                    fontSize = 9.sp,
                )
            }
            Text("⚡", fontSize = 26.sp)
        }

        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = SonHarfBlue)
        }

        LazyColumn(
            Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            if (s != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SonHarfSurface),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, SonHarfBlue.copy(alpha = .25f)),
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(15.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("⚡", fontSize = 38.sp)
                            Text(
                                when (s.status) {
                                    "not_started" -> sh(
                                        "BUGÜNÜN RESMÎ KOŞUSU",
                                        "TODAY'S OFFICIAL RUN",
                                    )
                                    "playing" -> if (prepSeconds > 0)
                                        sh("$prepSeconds SANİYE SONRA", "STARTS IN $prepSeconds")
                                    else
                                        sh("$remainingSeconds SANİYE", "$remainingSeconds SECONDS")
                                    else -> sh(
                                        "BUGÜNLÜK TAMAMLANDI",
                                        "DONE FOR TODAY",
                                    )
                                },
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                            )

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                DailyArenaMetric(
                                    "${s.currentStreak} 🔥",
                                    sh("SERİ", "STREAK"),
                                    Modifier.weight(1f),
                                )
                                DailyArenaMetric(
                                    "${s.bestStreak}",
                                    sh("EN İYİ", "BEST"),
                                    Modifier.weight(1f),
                                )
                                DailyArenaMetric(
                                    if (s.myRank > 0) "#${s.myRank}" else "—",
                                    sh("SIRA", "RANK"),
                                    Modifier.weight(1f),
                                )
                            }

                            if (s.status == "not_started") {
                                Text(
                                    sh(
                                        "Harfler BAŞLA'ya basmadan görünmez. Uzun kelime daha çok puan; 8 saniye içinde yeni geçerli kelime combo kazandırır.",
                                        "Letters stay hidden until START. Longer words score more; another valid word within 8 seconds builds combo.",
                                    ),
                                    color = SonHarfMuted,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    sh(
                                        "En az 1 geçerli kelime: +8 Son Coin • günlük yalnız 1 kez",
                                        "At least 1 valid word: +8 Son Coin • once per day",
                                    ),
                                    color = SonHarfGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                )
                                Button(
                                    onClick = {
                                        scope.launch {
                                            busy = true
                                            runCatching { backend?.startDailyArena(language) }
                                                .onSuccess {
                                                    nowMs = System.currentTimeMillis()
                                                    reload()
                                                }
                                                .onFailure { e ->
                                                    notice = when {
                                                        "player_already_in_game" in e.message.orEmpty() ->
                                                            sh(
                                                                "Önce aktif maçını bitir.",
                                                                "Finish your active match first.",
                                                            )
                                                        "daily_arena_active" in e.message.orEmpty() ->
                                                            sh(
                                                                "Başka dildeki Günlük Arena koşun sürüyor.",
                                                                "Your Daily Arena run in another language is active.",
                                                            )
                                                        "team_arena_active" in e.message.orEmpty() ->
                                                            sh(
                                                                "Açık 2v2 lobin var. Takım Arenası'na dönüp lobiyi kapat.",
                                                                "A 2v2 lobby is still open. Return to Team Arena and close it.",
                                                            )
                                                        else -> sh(
                                                            "Koşu başlatılamadı. Tekrar dene.",
                                                            "Run could not be started. Try again.",
                                                        )
                                                    }
                                                }
                                            busy = false
                                        }
                                    },
                                    enabled = !busy,
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SonHarfBlue
                                    ),
                                ) {
                                    Text(
                                        sh(
                                            "RESMÎ KOŞUYU BAŞLAT",
                                            "START OFFICIAL RUN",
                                        ),
                                        fontWeight = FontWeight.Black,
                                    )
                                }
                            }

                            if (s.status == "finished") {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                                ) {
                                    DailyArenaMetric(
                                        "${s.score}",
                                        sh("PUAN", "SCORE"),
                                        Modifier.weight(1f),
                                    )
                                    DailyArenaMetric(
                                        "${s.wordCount}",
                                        sh("KELİME", "WORDS"),
                                        Modifier.weight(1f),
                                    )
                                    DailyArenaMetric(
                                        "${s.bestCombo}×",
                                        "COMBO",
                                        Modifier.weight(1f),
                                    )
                                }
                                if (s.rewardCoins > 0) {
                                    Text(
                                        sh(
                                            "✓ Bugünün +${s.rewardCoins} Son Coin ödülü alındı.",
                                            "✓ Today's +${s.rewardCoins} Son Coin reward claimed.",
                                        ),
                                        color = SonHarfGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                    )
                                }
                                if (s.longestWord.isNotBlank()) {
                                    Text(
                                        sh(
                                            "En uzun: ${s.longestWord.uppercase()}",
                                            "Longest: ${s.longestWord.uppercase()}",
                                        ),
                                        color = SonHarfMuted,
                                        fontSize = 10.sp,
                                    )
                                }
                            }
                        }
                    }
                }

                if (s.status == "playing") {
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(
                                5.dp,
                                Alignment.CenterHorizontally,
                            ),
                        ) {
                            s.letters.orEmpty().forEach { ch ->
                                Surface(
                                    modifier = Modifier.size(32.dp),
                                    shape = RoundedCornerShape(9.dp),
                                    color = SonHarfBlue.copy(alpha = .10f),
                                    border = BorderStroke(
                                        1.dp,
                                        SonHarfBlue.copy(alpha = .25f),
                                    ),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            ch.uppercase(),
                                            color = SonHarfText,
                                            fontWeight = FontWeight.Black,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { value ->
                                if (!busy && prepSeconds == 0 && remainingSeconds > 0) {
                                    input = value.filter(Char::isLetter).take(10).uppercase()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().focusRequester(inputFocusRequester),
                            enabled = true,
                            singleLine = true,
                            label = {
                                Text(sh("3–10 harfli kelime", "3–10 letter word"))
                            },
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Send,
                                showKeyboardOnFocus = true,
                            ),
                            keyboardActions = KeyboardActions(onSend = { submit() }),
                            trailingIcon = {
                                TextButton(
                                    onClick = ::submit,
                                    enabled = input.length in 3..10 &&
                                        !busy &&
                                        prepSeconds == 0 &&
                                        remainingSeconds > 0,
                                ) {
                                    Text(
                                        sh("GÖNDER", "SEND"),
                                        fontWeight = FontWeight.Black,
                                    )
                                }
                            },
                        )
                    }

                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            DailyArenaMetric(
                                "${s.score}",
                                sh("PUAN", "SCORE"),
                                Modifier.weight(1f),
                            )
                            DailyArenaMetric(
                                "${words.size}",
                                sh("KELİME", "WORDS"),
                                Modifier.weight(1f),
                            )
                            DailyArenaMetric(
                                "${s.bestCombo}×",
                                "COMBO",
                                Modifier.weight(1f),
                            )
                        }
                    }

                    if (words.isNotEmpty()) {
                        item {
                            Text(
                                sh("KELİMELERİN", "YOUR WORDS"),
                                color = SonHarfGold,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                            )
                        }
                        items(
                            words.reversed(),
                            key = { "${it.normalizedWord}-${it.createdAt}" },
                        ) { w ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SonHarfSurface,
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        w.word.uppercase(),
                                        Modifier.weight(1f),
                                        color = SonHarfText,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    if (w.combo > 1) {
                                        Text(
                                            "${w.combo}× ",
                                            color = SonHarfGold,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                        )
                                    }
                                    Text(
                                        "+${w.basePoints}",
                                        color = SonHarfBlue,
                                        fontWeight = FontWeight.Black,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (notice.isNotBlank()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SonHarfGold.copy(alpha = .09f),
                    ) {
                        Text(
                            notice,
                            Modifier.fillMaxWidth().padding(9.dp),
                            color = SonHarfText,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.EmojiEvents,
                        null,
                        tint = SonHarfGold,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                    Column {
                        Text(
                            sh("BUGÜNÜN SIRALAMASI", "TODAY'S RANKING"),
                            color = SonHarfGold,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                        )
                        Text(
                            "${s?.playerCount ?: 0} ${sh("tamamlayan oyuncu", "finishers")}",
                            color = SonHarfMuted,
                            fontSize = 8.sp,
                        )
                    }
                }
            }

            if (leaderboard.isEmpty()) {
                item {
                    Text(
                        sh(
                            "Bugün henüz skor yazan yok.",
                            "No scores posted today yet.",
                        ),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        color = SonHarfMuted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                items(leaderboard, key = { it.userId }) { row ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (row.isMe)
                            SonHarfBlue.copy(alpha = .08f)
                        else
                            SonHarfSurface,
                        border = BorderStroke(
                            1.dp,
                            if (row.isMe)
                                SonHarfBlue.copy(alpha = .28f)
                            else
                                SonHarfMuted.copy(alpha = .12f),
                        ),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                when (row.rank) {
                                    1L -> "🥇"
                                    2L -> "🥈"
                                    3L -> "🥉"
                                    else -> "#${row.rank}"
                                },
                                Modifier.width(42.dp),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Black,
                            )
                            ProfilePhotoAvatar(
                                avatarPath = leaderboardProfiles[row.userId]?.avatarPath,
                                name = row.displayName,
                                size = 34.dp,
                                accent = SonHarfBlue,
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    row.displayName,
                                    color = SonHarfText,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                )
                                Text(
                                    "${row.wordCount} ${sh("kelime", "words")} • ${row.bestCombo}× combo",
                                    color = SonHarfMuted,
                                    fontSize = 8.sp,
                                )
                            }
                            Text(
                                "${row.score}",
                                color = SonHarfBlue,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(10.dp)) }
        }
    }
}

@Composable
private fun DailyArenaMetric(
    value: String,
    label: String,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(13.dp),
        color = SonHarfSurface2,
    ) {
        Column(
            Modifier.padding(horizontal = 5.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                value,
                color = SonHarfText,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                maxLines = 1,
            )
            Text(
                label,
                color = SonHarfMuted,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

private fun dailyArenaEpochMs(value: String): Long =
    runCatching { Instant.parse(value).toEpochMilli() }.getOrDefault(0L)
