package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.SharedDictionaryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Light natural-wood table palette for Kelime Atölyesi. */
private object AtelierUi {
    val WoodTop = Color(0xFFF3E8D3)
    val WoodBottom = Color(0xFFE6D3B0)
    val Cream = Color(0xFFFCF8F0)
    val TileEdge = Color(0xFFCDB387)
    val TileUsed = Color(0xFFE9DCC3)
    val Ink = Color(0xFF2B2720)
    val InkMuted = Color(0xFF6E665A)
    val Green = Color(0xFF2E7A53)
    val GreenSoft = Color(0xFFDDEDE1)
    val Gold = Color(0xFFB08D45)
    val GoldSoft = Color(0xFFF1E4C3)
    val Danger = Color(0xFFA9453D)
}

private data class AtelierFeedback(val text: String, val positive: Boolean, val nonce: Int)

private object AtelierRecords {
    private const val PREFS = "kelime_atolyesi_records"
    fun best(context: android.content.Context, language: String): Int =
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE).getInt("best_$language", 0)

    fun save(context: android.content.Context, language: String, score: Int): Int {
        val prefs = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        val best = maxOf(prefs.getInt("best_$language", 0), score)
        prefs.edit().putInt("best_$language", best).apply()
        return best
    }
}

/** The mascot the player picked, if owned, else their first owned character; visual companion only. */
private fun atelierMascotSkin(context: android.content.Context): WordSiegeMascotSkin {
    val owned = WordSiegeMascotOwnership.owned
    val picked = WordSiegeMascotBond(context).skinChoice
    return picked?.takeIf { it in owned } ?: owned.minByOrNull { it.ordinal } ?: WordSiegeMascotSkin.ORB
}

private fun atelierTaskText(task: AtelierTask, language: String): String = when (task.kind) {
    AtelierTaskKind.LENGTH -> sh("${task.length} harfli kelime kur", "Build a ${task.length}-letter word")
    AtelierTaskKind.LETTER -> {
        val letter = KelimeAtolyesiEngine.display(task.letter, language)
        sh("“$letter” harfini kullan", "Use the letter “$letter”")
    }
}

@Composable
internal fun KelimeAtolyesiScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val language = SharedDictionaryService.canonicalLanguage(SonHarfUiState.language)
    val scope = rememberCoroutineScope()
    val mascotSkin = remember { atelierMascotSkin(context) }

    var engine by remember(language) { mutableStateOf<KelimeAtolyesiEngine?>(null) }
    var state by remember(language) { mutableStateOf<AtelierState?>(null) }
    var loadFailed by remember(language) { mutableStateOf(false) }
    var loadNonce by remember { mutableIntStateOf(0) }
    var roundKey by remember { mutableIntStateOf(0) }
    var secondsLeft by remember { mutableIntStateOf(KelimeAtolyesiEngine.ROUND_SECONDS) }
    var busy by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<AtelierFeedback?>(null) }
    var gain by remember { mutableIntStateOf(0) }
    var gainNonce by remember { mutableIntStateOf(0) }
    var happyUntil by remember { mutableStateOf(0L) }
    var best by remember(language) { mutableIntStateOf(AtelierRecords.best(context, language)) }
    var newBest by remember { mutableStateOf(false) }
    // Mascot hints: three per round, only when the player taps the hint button.
    var hintsLeft by remember { mutableIntStateOf(MascotHints.HINTS_PER_MATCH) }
    var hintText by remember { mutableStateOf<String?>(null) }
    var mascotAction by remember { mutableStateOf<WordSiegeMascotAction?>(null) }
    var mascotActionKey by remember { mutableStateOf(0L) }

    BackHandler { onExit() }

    fun endRound(finished: AtelierState) {
        state = finished
        newBest = finished.score > best
        best = AtelierRecords.save(context, language, finished.score)
        if (finished.allTasksDone) SonHarfSoundFx.victory() else SonHarfSoundFx.softNotify()
    }

    fun startRound() {
        val e = engine ?: return
        scope.launch {
            val fresh = withContext(Dispatchers.Default) { runCatching { e.newRound() }.getOrNull() }
            if (fresh == null) {
                loadFailed = true
                return@launch
            }
            state = fresh
            feedback = null
            newBest = false
            hintsLeft = MascotHints.HINTS_PER_MATCH
            hintText = null
            secondsLeft = KelimeAtolyesiEngine.ROUND_SECONDS
            roundKey += 1
        }
    }

    LaunchedEffect(language, loadNonce) {
        loadFailed = false
        engine = null
        state = null
        val dictionary = runCatching { SharedDictionaryService.preloadCanonical(context, language) }.getOrNull()
        val built = dictionary?.let { words ->
            withContext(Dispatchers.Default) { KelimeAtolyesiEngine(words, language).takeIf { it.playable } }
        }
        if (built == null) {
            loadFailed = true
            return@LaunchedEffect
        }
        engine = built
        startRound()
    }

    // The 60-second round clock.
    LaunchedEffect(roundKey) {
        if (roundKey == 0) return@LaunchedEffect
        while (true) {
            delay(1_000)
            val current = state ?: break
            if (current.over) break
            secondsLeft = (secondsLeft - 1).coerceAtLeast(0)
            if (secondsLeft == 0) {
                engine?.let { endRound(it.finish(current, 0)) }
                break
            }
        }
    }

    fun submit() {
        val e = engine ?: return
        val before = state ?: return
        if (busy || before.over || before.picked.isEmpty()) return
        busy = true
        scope.launch {
            val result = withContext(Dispatchers.Default) { e.submit(before) }
            busy = false
            if (state !== before) return@launch // the round ended while the word was being checked
            val shown = KelimeAtolyesiEngine.display(result.word, language)
            val nonce = (feedback?.nonce ?: 0) + 1
            when (result.reject) {
                AtelierReject.TOO_SHORT -> feedback = AtelierFeedback(sh("En az 3 harf kullan.", "Use at least 3 letters."), false, nonce)
                AtelierReject.ALREADY_USED -> feedback = AtelierFeedback(sh("“$shown” bu turda kullanıldı.", "“$shown” was already used this round."), false, nonce)
                AtelierReject.NOT_IN_DICTIONARY -> feedback = AtelierFeedback(sh("“$shown” sözlükte yok.", "“$shown” is not in the dictionary."), false, nonce)
                AtelierReject.ROUND_OVER -> Unit
                null -> {
                    state = result.state
                    hintText = null
                    mascotAction = if (result.completed.isNotEmpty()) WordSiegeMascotAction.CLAP else WordSiegeMascotAction.HOP
                    mascotActionKey += 1
                    gain = result.gained
                    gainNonce += 1
                    val taskNote = if (result.completed.isNotEmpty()) {
                        sh(" · Görev +${result.taskPoints}", " · Task +${result.taskPoints}")
                    } else ""
                    feedback = AtelierFeedback("$shown +${result.wordPoints}$taskNote", true, nonce)
                    if (result.completed.isNotEmpty()) {
                        happyUntil = System.currentTimeMillis() + 1_600
                        SonHarfSoundFx.missionComplete()
                    } else {
                        SonHarfSoundFx.puzzleSuccess()
                    }
                    if (result.state.allTasksDone) endRound(e.finish(result.state, secondsLeft))
                    return@launch
                }
            }
            SonHarfSoundFx.puzzleError()
        }
    }

    fun askHint() {
        val e = engine ?: return
        val current = state ?: return
        if (hintsLeft <= 0 || current.over) return
        val open = current.tasks.filter { !it.done }
        val options = e.formable(current.pool.map { it.letter }, current.words.toSet())
        val word = open.firstNotNullOfOrNull { task -> options.filter { task.matches(it) }.minByOrNull { it.length } }
        hintsLeft -= 1
        mascotAction = WordSiegeMascotAction.POINT
        mascotActionKey += 1
        hintText = if (word == null) {
            sh("Harflerini bir daha incele, bir kelime saklanıyor!", "Look at your letters again, a word is hiding!")
        } else {
            val shown = (word.length / 2).coerceAtLeast(1)
            sh("Şunu dene: ${MascotHints.pattern(word, shown, language)} (${word.length} harf)",
                "Try this: ${MascotHints.pattern(word, shown, language)} (${word.length} letters)")
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AtelierUi.WoodTop, AtelierUi.WoodBottom))),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val current = state
            AtelierTopBar(
                seconds = secondsLeft,
                score = current?.score ?: 0,
                onBack = onExit,
            )
            AtelierMascotRow(
                skin = mascotSkin,
                happy = happyUntil > System.currentTimeMillis() || current?.over == true && current.allTasksDone,
                actionKey = mascotActionKey,
                action = mascotAction,
                hint = when {
                    current?.over == true -> sh("Tur bitti.", "Round over.")
                    hintText != null -> hintText!!
                    else -> sh("Harflere dokun, kelimeni kur.", "Tap letters to build your word.")
                },
                hintsLeft = if (current != null && !current.over) hintsLeft else 0,
                onHint = { askHint() },
            )
            when {
                loadFailed -> AtelierLoadError { loadNonce += 1 }
                current == null -> Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(color = AtelierUi.Green)
                        Text(sh("Harfler hazırlanıyor…", "Preparing letters…"), color = AtelierUi.InkMuted, fontSize = 14.sp)
                    }
                }
                current.over -> AtelierResult(
                    state = current,
                    language = language,
                    best = best,
                    newBest = newBest,
                    onNewRound = { startRound() },
                    onExit = onExit,
                )
                else -> {
                    AtelierPool(current, language) { tileId ->
                        SonHarfSoundFx.puzzleTap()
                        state = current.pick(tileId)
                    }
                    AtelierTasks(current.tasks, language)
                    AtelierSlot(current, language, gain = gain, gainNonce = gainNonce) { index ->
                        SonHarfSoundFx.puzzleKey()
                        state = current.unpickAt(index)
                    }
                    AtelierFeedbackLine(feedback)
                    AtelierActions(
                        canClear = current.picked.isNotEmpty(),
                        canSubmit = current.picked.isNotEmpty() && !busy,
                        onClear = { state = current.clearPicks() },
                        onSubmit = { submit() },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AtelierTopBar(seconds: Int, score: Int, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back"), tint = AtelierUi.Ink)
        }
        Text(
            sh("Kelime Atölyesi", "Word Workshop"),
            modifier = Modifier.weight(1f),
            color = AtelierUi.Ink,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        AtelierChip(
            label = sh("Süre", "Time"),
            value = "0:${seconds.toString().padStart(2, '0')}",
            accent = if (seconds <= 10) AtelierUi.Danger else AtelierUi.Ink,
        )
        Spacer(Modifier.width(8.dp))
        AtelierChip(label = sh("Puan", "Score"), value = score.toString(), accent = AtelierUi.Green)
    }
}

@Composable
private fun AtelierChip(label: String, value: String, accent: Color) {
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AtelierUi.Cream)
            .border(1.dp, AtelierUi.TileEdge.copy(alpha = .6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = AtelierUi.InkMuted, fontSize = 10.sp)
        Text(value, color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AtelierMascotRow(
    skin: WordSiegeMascotSkin,
    happy: Boolean,
    actionKey: Long,
    action: WordSiegeMascotAction?,
    hint: String,
    hintsLeft: Int,
    onHint: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // Visual companion only: no speech bubble, no pop-ups.
        WordSiegeMascot(
            moveId = null,
            lastMoveMine = false,
            pendingCells = emptyList(),
            playerTurn = true,
            requestedEmotion = if (happy) WordSiegeMascotEmotion.HAPPY else WordSiegeMascotEmotion.CALM,
            modifier = Modifier.size(56.dp),
            actionKey = actionKey,
            action = action,
            skin = skin,
        )
        Spacer(Modifier.width(10.dp))
        Text(hint, color = AtelierUi.InkMuted, fontSize = 14.sp, modifier = Modifier.weight(1f))
        if (hintsLeft > 0) {
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = onHint,
                shape = RoundedCornerShape(99.dp),
                border = BorderStroke(1.dp, AtelierUi.Gold),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = AtelierUi.GoldSoft, contentColor = AtelierUi.Ink),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.heightIn(min = 36.dp),
            ) {
                Text(sh("💡 İpucu ($hintsLeft)", "💡 Hint ($hintsLeft)"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AtelierPool(state: AtelierState, language: String, onPick: (Long) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = 10.dp
        val tile = min(68.dp, (maxWidth - gap * 3) / 4)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(gap)) {
            listOf(state.pool.take(4), state.pool.drop(4)).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { t ->
                        AtelierPoolTile(
                            letter = KelimeAtolyesiEngine.display(t.letter, language),
                            tileId = t.id,
                            size = tile,
                            used = t.id in state.picked,
                            onClick = { onPick(t.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AtelierPoolTile(letter: String, tileId: Long, size: Dp, used: Boolean, onClick: () -> Unit) {
    // Each new tile (a fresh round or a refill) settles in with a short pop.
    val appear = remember(tileId) { Animatable(.55f) }
    LaunchedEffect(tileId) { appear.animateTo(1f, tween(260, easing = FastOutSlowInEasing)) }
    Box(
        Modifier
            .size(size)
            .graphicsLayer {
                scaleX = appear.value
                scaleY = appear.value
                alpha = if (used) .38f else appear.value
            }
            .shadow(if (used) 0.dp else 3.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(if (used) AtelierUi.TileUsed else AtelierUi.Cream)
            .border(1.dp, AtelierUi.TileEdge, RoundedCornerShape(12.dp))
            .clickable(enabled = !used, onClick = onClick)
            .semantics { contentDescription = letter },
        contentAlignment = Alignment.Center,
    ) {
        Text(letter, color = AtelierUi.Ink, fontSize = (size.value * .46f).sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AtelierTasks(tasks: List<AtelierTask>, language: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tasks.forEachIndexed { index, task -> AtelierTaskCard(index, task, language) }
        Text(
            sh("Bir kelime, uyduğu tüm görevleri aynı anda tamamlar.", "One word completes every task it fits at once."),
            color = AtelierUi.InkMuted,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun AtelierTaskCard(index: Int, task: AtelierTask, language: String) {
    val background by animateColorAsState(if (task.done) AtelierUi.GreenSoft else AtelierUi.Cream, tween(300), label = "task-bg")
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(task.done) {
        if (task.done) {
            pulse.snapTo(1.06f)
            pulse.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .border(1.dp, if (task.done) AtelierUi.Green.copy(alpha = .7f) else AtelierUi.TileEdge.copy(alpha = .7f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(if (task.done) AtelierUi.Green else AtelierUi.GoldSoft),
            contentAlignment = Alignment.Center,
        ) {
            if (task.done) {
                Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
            } else {
                Text("${index + 1}", color = AtelierUi.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            atelierTaskText(task, language),
            modifier = Modifier.weight(1f),
            color = if (task.done) AtelierUi.Green else AtelierUi.Ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "+${KelimeAtolyesiEngine.TASK_POINTS}",
            color = if (task.done) AtelierUi.Green else AtelierUi.Gold,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AtelierSlot(state: AtelierState, language: String, gain: Int, gainNonce: Int, onUnpick: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(sh("Kelimen", "Your word"), color = AtelierUi.InkMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val gap = 6.dp
            val cell = min(46.dp, (maxWidth - gap * 6) / 7)
            val letters = state.picked.mapNotNull { id -> state.pool.firstOrNull { it.id == id }?.letter }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally)) {
                for (i in 0 until KelimeAtolyesiEngine.POOL_SIZE) {
                    val letter = letters.getOrNull(i)
                    Box(
                        Modifier
                            .size(cell)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (letter != null) AtelierUi.Cream else AtelierUi.WoodTop.copy(alpha = .7f))
                            .border(
                                1.dp,
                                if (letter != null) AtelierUi.Green.copy(alpha = .75f) else AtelierUi.TileEdge.copy(alpha = .55f),
                                RoundedCornerShape(10.dp),
                            )
                            .then(if (letter != null) Modifier.clickable { onUnpick(i) } else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (letter != null) {
                            Text(
                                KelimeAtolyesiEngine.display(letter, language),
                                color = AtelierUi.Ink,
                                fontSize = (cell.value * .48f).sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            // The score float: drawn above the slot, never intercepts touches.
            if (gainNonce > 0) {
                val rise = remember(gainNonce) { Animatable(0f) }
                LaunchedEffect(gainNonce) { rise.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
                Text(
                    "+$gain",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            translationY = -rise.value * 46f
                            alpha = (1f - rise.value).coerceIn(0f, 1f)
                        },
                    color = AtelierUi.Green,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun AtelierFeedbackLine(feedback: AtelierFeedback?) {
    Box(Modifier.fillMaxWidth().height(22.dp), contentAlignment = Alignment.Center) {
        if (feedback != null) {
            Text(
                feedback.text,
                color = if (feedback.positive) AtelierUi.Green else AtelierUi.Danger,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AtelierActions(canClear: Boolean, canSubmit: Boolean, onClear: () -> Unit, onSubmit: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = onClear,
            enabled = canClear,
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, AtelierUi.TileEdge),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = AtelierUi.Cream, contentColor = AtelierUi.Ink),
        ) {
            Text(sh("Temizle", "Clear"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Button(
            onClick = onSubmit,
            enabled = canSubmit,
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AtelierUi.Green,
                contentColor = Color.White,
                disabledContainerColor = AtelierUi.Green.copy(alpha = .35f),
                disabledContentColor = Color.White.copy(alpha = .85f),
            ),
        ) {
            Text(sh("Gönder", "Submit"), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AtelierResult(
    state: AtelierState,
    language: String,
    best: Int,
    newBest: Boolean,
    onNewRound: () -> Unit,
    onExit: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AtelierUi.Cream)
            .border(1.dp, AtelierUi.Gold.copy(alpha = .55f), RoundedCornerShape(18.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            if (state.allTasksDone) sh("Tüm görevler tamam!", "All tasks done!") else sh("Süre doldu!", "Time's up!"),
            color = AtelierUi.Ink,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Text("${state.score}", color = AtelierUi.Green, fontSize = 44.sp, fontWeight = FontWeight.Bold)
        Text(
            if (newBest) sh("Yeni en iyi skor!", "New best score!") else sh("En iyi: $best", "Best: $best"),
            color = if (newBest) AtelierUi.Gold else AtelierUi.InkMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            sh("Tamamlanan görevler: ${state.completedTasks}/3", "Tasks completed: ${state.completedTasks}/3"),
            color = AtelierUi.Ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        state.tasks.forEach { task ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (task.done) "✓" else "–",
                    color = if (task.done) AtelierUi.Green else AtelierUi.InkMuted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(24.dp),
                )
                Text(atelierTaskText(task, language), color = if (task.done) AtelierUi.Ink else AtelierUi.InkMuted, fontSize = 14.sp)
            }
        }
        if (state.words.isNotEmpty()) {
            Text(
                state.words.joinToString(" · ") { KelimeAtolyesiEngine.display(it, language) },
                color = AtelierUi.InkMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onNewRound,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AtelierUi.Green, contentColor = Color.White),
        ) {
            Text(sh("Yeni Tur", "New Round"), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, AtelierUi.TileEdge),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AtelierUi.Ink),
        ) {
            Text(sh("Çık", "Exit"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AtelierLoadError(onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AtelierUi.Cream)
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            sh("Sözlük yüklenemedi. İnternet bağlantını kontrol et.", "The dictionary could not be loaded. Check your connection."),
            color = AtelierUi.Ink,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AtelierUi.Green, contentColor = Color.White),
        ) {
            Text(sh("Tekrar dene", "Retry"))
        }
    }
}
