package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.SharedDictionaryService
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object LetterLadderUi {
    val Background: Color get() = SonHarfTheme.Background
    val Surface: Color get() = SonHarfTheme.Surface
    val SurfaceRaised: Color get() = SonHarfTheme.Surface
    val SurfaceSoft: Color get() = SonHarfTheme.SurfaceSecondary
    val Text: Color get() = SonHarfTheme.TextPrimary
    val Muted: Color get() = SonHarfTheme.TextSecondary
    val Border: Color get() = SonHarfTheme.Border
    val Accent: Color get() = SonHarfTheme.PrimaryBlue
    val AccentText: Color get() = Color.White
    val Live: Color get() = SonHarfTheme.Error
    val Coral: Color get() = SonHarfTheme.Error
    val Orange: Color get() = SonHarfTheme.Warning
    val Green: Color get() = SonHarfTheme.Success
    val Gold: Color get() = SonHarfTheme.Warning
}

internal data class LetterLadderPuzzle(
    val id: String,
    val start: String,
    val target: String,
    val solution: List<String>,
)

internal enum class LetterLadderReject {
    LENGTH,
    NOT_DICTIONARY,
    NOT_ONE_CHANGE,
    POSITION_ALREADY_USED,
    WRONG_TARGET_LETTER,
}

internal data class LetterLadderMoveCheck(
    val accepted: Boolean,
    val changedIndex: Int? = null,
    val reject: LetterLadderReject? = null,
)

/**
 * HARF YOLU rule engine.
 *
 * In a five-move puzzle each of the five positions is changed exactly once.
 * A position that has changed is permanently locked. Because it cannot be
 * changed again, the new letter must already be the target letter for that
 * position; otherwise the target would become mathematically unreachable.
 */
internal object LetterLadderEngine {
    const val WORD_LENGTH = 5
    const val MOVE_COUNT = 5

    private val tr = Locale.forLanguageTag("tr-TR")

    fun changedIndex(from: String, to: String): Int? {
        if (from.length != WORD_LENGTH || to.length != WORD_LENGTH) return null
        var changed = -1
        for (index in 0 until WORD_LENGTH) {
            if (from[index] != to[index]) {
                if (changed >= 0) return null
                changed = index
            }
        }
        return changed.takeIf { it >= 0 }
    }

    fun validateMove(
        puzzle: LetterLadderPuzzle,
        current: String,
        candidate: String,
        usedPositions: Set<Int>,
        dictionary: Set<String>,
    ): LetterLadderMoveCheck {
        if (candidate.length != WORD_LENGTH) {
            return LetterLadderMoveCheck(false, reject = LetterLadderReject.LENGTH)
        }
        if (candidate !in dictionary) {
            return LetterLadderMoveCheck(false, reject = LetterLadderReject.NOT_DICTIONARY)
        }
        val changed = changedIndex(current, candidate)
            ?: return LetterLadderMoveCheck(false, reject = LetterLadderReject.NOT_ONE_CHANGE)
        if (changed in usedPositions) {
            return LetterLadderMoveCheck(false, changedIndex = changed, reject = LetterLadderReject.POSITION_ALREADY_USED)
        }
        if (candidate[changed] != puzzle.target[changed]) {
            return LetterLadderMoveCheck(false, changedIndex = changed, reject = LetterLadderReject.WRONG_TARGET_LETTER)
        }
        return LetterLadderMoveCheck(true, changedIndex = changed)
    }

    /**
     * Finds a complete route from the current state to the target using only
     * unused positions. This is deliberately tiny (maximum depth five), so it
     * can be used for hints and dead-end prevention without a background job.
     */
    fun completionPath(
        puzzle: LetterLadderPuzzle,
        current: String,
        usedPositions: Set<Int>,
        dictionary: Set<String>,
    ): List<String>? {
        if (current.length != WORD_LENGTH || puzzle.target.length != WORD_LENGTH) return null
        if (current == puzzle.target) return listOf(current)
        if (usedPositions.size >= MOVE_COUNT) return null

        val remaining = (0 until WORD_LENGTH).filterNot { it in usedPositions }
        for (index in remaining) {
            if (current[index] == puzzle.target[index]) continue
            val chars = current.toCharArray()
            chars[index] = puzzle.target[index]
            val next = String(chars)
            if (next !in dictionary) continue

            val tail = completionPath(
                puzzle = puzzle,
                current = next,
                usedPositions = usedPositions + index,
                dictionary = dictionary,
            )
            if (tail != null) return listOf(current) + tail
        }
        return null
    }

    fun generate(
        sourceWords: Set<String>,
        language: String,
        seed: Long,
        excludedPuzzleIds: Set<String> = emptySet(),
    ): LetterLadderPuzzle? {
        val locale = if (language.lowercase(Locale.ROOT) == "en") Locale.ENGLISH else tr
        val words = sourceWords.asSequence()
            .map { it.trim().lowercase(locale) }
            .filter { word -> word.length == WORD_LENGTH && word.all(Char::isLetter) }
            .toSet()
        if (words.size < MOVE_COUNT + 1) return null

        val wildcard = Array(WORD_LENGTH) { mutableMapOf<String, MutableList<String>>() }
        words.forEach { word ->
            for (index in 0 until WORD_LENGTH) {
                wildcard[index].getOrPut(pattern(word, index)) { mutableListOf() }.add(word)
            }
        }

        val random = Random(seed)
        val starts = words.shuffled(random).take(900)
        var visitedNodes = 0
        val nodeBudget = 45_000

        fun search(path: MutableList<String>, used: MutableSet<Int>): List<String>? {
            if (path.size == MOVE_COUNT + 1) {
                val candidate = puzzleFromPath(path, language) ?: return null
                return path.toList().takeIf { candidate.id !in excludedPuzzleIds }
            }
            if (visitedNodes++ >= nodeBudget) return null

            val current = path.last()
            val positions = (0 until WORD_LENGTH).filterNot { it in used }.shuffled(random)
            for (position in positions) {
                val neighbors = wildcard[position][pattern(current, position)]
                    .orEmpty()
                    .asSequence()
                    .filter { it != current && it !in path }
                    .toList()
                    .shuffled(random)
                    .take(36)
                for (next in neighbors) {
                    path += next
                    used += position
                    val result = search(path, used)
                    if (result != null) return result
                    used -= position
                    path.removeAt(path.lastIndex)
                }
            }
            return null
        }

        for (start in starts) {
            val path = search(mutableListOf(start), mutableSetOf()) ?: continue
            return puzzleFromPath(path, language)
        }
        return null
    }

    private fun puzzleFromPath(path: List<String>, language: String): LetterLadderPuzzle? {
        if (path.size != MOVE_COUNT + 1) return null
        val used = mutableSetOf<Int>()
        for (index in 1..MOVE_COUNT) {
            val changed = changedIndex(path[index - 1], path[index]) ?: return null
            if (!used.add(changed)) return null
        }
        if (used.size != WORD_LENGTH) return null
        val start = path.first()
        val target = path.last()
        if ((0 until WORD_LENGTH).any { start[it] == target[it] }) return null
        val routeEndpoints = listOf(start, target).sorted().joinToString("-")
        return LetterLadderPuzzle(
            // The id represents puzzle content, not the random attempt. This makes recent-history
            // exclusion reliable across app restarts and across different random seeds.
            id = "${language.lowercase(Locale.ROOT)}-$routeEndpoints",
            start = start,
            target = target,
            solution = path,
        )
    }

    private fun pattern(word: String, index: Int): String = buildString(WORD_LENGTH) {
        word.forEachIndexed { i, char -> append(if (i == index) '*' else char) }
    }
}

private object LetterLadderPuzzleHistory {
    private const val PREFS = "son_harf_letter_ladder_history_v2"
    private const val IDS_PREFIX = "recent_ids_"
    private const val SEED_PREFIX = "next_seed_"
    private const val HISTORY_LIMIT = 24

    fun recentIds(context: android.content.Context, language: String): Set<String> =
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .getString(IDS_PREFIX + SharedDictionaryService.canonicalLanguage(language), "")
            .orEmpty()
            .lineSequence()
            .filter(String::isNotBlank)
            .toSet()

    fun nextSeed(context: android.content.Context, language: String): Long {
        val lang = SharedDictionaryService.canonicalLanguage(language)
        val prefs = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        val counter = prefs.getLong(SEED_PREFIX + lang, 0L) + 1L
        prefs.edit().putLong(SEED_PREFIX + lang, counter).apply()
        return System.currentTimeMillis() xor System.nanoTime() xor (counter * 104_729L)
    }

    fun remember(context: android.content.Context, language: String, puzzleId: String) {
        val lang = SharedDictionaryService.canonicalLanguage(language)
        val prefs = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        val key = IDS_PREFIX + lang
        val ordered = prefs.getString(key, "").orEmpty().lineSequence()
            .filter(String::isNotBlank)
            .filterNot { it == puzzleId }
            .toMutableList()
        ordered.add(0, puzzleId)
        prefs.edit().putString(key, ordered.take(HISTORY_LIMIT).joinToString("\n")).apply()
    }
}

@Composable
internal fun LetterLadderGameScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val language = SonHarfUiState.language
    val locale = remember(language) {
        if (language.lowercase(Locale.ROOT) == "en") Locale.ENGLISH else Locale.forLanguageTag("tr-TR")
    }
    var puzzleNonce by remember { mutableIntStateOf(0) }
    var dictionary by remember { mutableStateOf<Set<String>>(emptySet()) }
    var puzzle by remember { mutableStateOf<LetterLadderPuzzle?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    var path by remember { mutableStateOf<List<String>>(emptyList()) }
    var usedPositions by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var input by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var completed by remember { mutableStateOf(false) }
    var hintText by remember { mutableStateOf<String?>(null) }
    // G5.8 SORUN 2: progressive hint state.
    //   0 = no hint requested yet; 1..3 = which stage was last revealed for
    //   the current move. Reset whenever a new move is played (see submit()).
    var hintStage by remember { mutableIntStateOf(0) }
    var hintHighlight by remember { mutableStateOf<Int?>(null) }
    var hintDefinition by remember { mutableStateOf<String?>(null) }
    var successVfxNonce by remember { mutableIntStateOf(0) }
    val hintScope = rememberCoroutineScope()
    val vfx = com.sonharf.game.ui.vfx.LocalVfx.current
    val vfxAnchor = remember(context) {
        val metrics = context.resources.displayMetrics
        androidx.compose.ui.geometry.Offset(metrics.widthPixels * .5f, metrics.heightPixels * .5f)
    }

    LaunchedEffect(language, puzzleNonce) {
        loading = true
        loadError = false
        completed = false
        hintText = null
        hintStage = 0
        hintHighlight = null
        hintDefinition = null
        input = ""
        successVfxNonce = 0
        val loaded = runCatching { SharedDictionaryService.preloadCanonical(context, language) }.getOrNull()
        if (loaded.isNullOrEmpty()) {
            dictionary = emptySet()
            puzzle = null
            path = emptyList()
            usedPositions = emptySet()
            loadError = true
            loading = false
            return@LaunchedEffect
        }
        dictionary = loaded
        val recentPuzzleIds = LetterLadderPuzzleHistory.recentIds(context, language)
        val gameSeed = LetterLadderPuzzleHistory.nextSeed(context, language) + puzzleNonce.toLong() * 104_729L
        val generated = withContext(Dispatchers.Default) {
            LetterLadderEngine.generate(
                sourceWords = loaded,
                language = language,
                seed = gameSeed,
                excludedPuzzleIds = recentPuzzleIds,
            )
                // Very small custom dictionaries may contain only recently played routes. In that
                // exceptional case, keep the mode playable instead of showing a false load error.
                ?: LetterLadderEngine.generate(loaded, language, gameSeed xor Long.MIN_VALUE)
        }
        if (generated != null) LetterLadderPuzzleHistory.remember(context, language, generated.id)
        puzzle = generated
        path = generated?.let { listOf(it.start) }.orEmpty()
        usedPositions = emptySet()
        message = if (generated == null) {
            sh("Bu sözlükte uygun 5 adımlı bulmaca üretilemedi.", "No valid five-step puzzle could be generated.")
        } else {
            sh("Her hamlede yalnızca 1 harfi değiştir.", "Change exactly one letter on each move.")
        }
        loadError = generated == null
        loading = false
    }

    fun resetCurrent() {
        val currentPuzzle = puzzle ?: return
        path = listOf(currentPuzzle.start)
        usedPositions = emptySet()
        input = ""
        hintText = null
        hintStage = 0
        hintHighlight = null
        hintDefinition = null
        completed = false
        successVfxNonce = 0
        message = sh("Her hamlede yalnızca 1 harfi değiştir.", "Change exactly one letter on each move.")
    }

    fun submit() {
        val currentPuzzle = puzzle ?: return
        if (completed || path.isEmpty()) return
        val normalized = SharedDictionaryService.normalize(input, language)
        val current = path.last()
        val check = LetterLadderEngine.validateMove(
            puzzle = currentPuzzle,
            current = current,
            candidate = normalized,
            usedPositions = usedPositions,
            dictionary = dictionary,
        )
        if (!check.accepted) {
            hintText = null
            SonHarfSoundFx.puzzleError()
            message = when (check.reject) {
                LetterLadderReject.LENGTH -> sh("Kelime tam 5 harf olmalı.", "The word must contain exactly 5 letters.")
                LetterLadderReject.NOT_DICTIONARY -> sh("Bu kelime Son Harf sözlüğünde yok.", "This word is not in the Son Harf dictionary.")
                LetterLadderReject.NOT_ONE_CHANGE -> sh("Yalnızca 1 harf değiştirebilirsin.", "You may change exactly one letter.")
                LetterLadderReject.POSITION_ALREADY_USED -> sh("Bu kutuyu daha önce değiştirdin; tekrar değişemez.", "That position was already changed and is locked.")
                LetterLadderReject.WRONG_TARGET_LETTER -> sh("Değişen harf hedefteki harf olmalı.", "The changed letter must match the target letter.")
                null -> sh("Geçersiz hamle.", "Invalid move.")
            }
            return
        }

        val changed = check.changedIndex ?: return
        val nextUsed = usedPositions + changed
        val canFinish = normalized == currentPuzzle.target || LetterLadderEngine.completionPath(
            puzzle = currentPuzzle,
            current = normalized,
            usedPositions = nextUsed,
            dictionary = dictionary,
        ) != null
        if (!canFinish) {
            hintText = null
            input = ""
            SonHarfSoundFx.puzzleError()
            message = sh(
                "Bu hamle çıkmaza götürüyor. Başka bir harf değiştir.",
                "That move leads to a dead end. Change a different letter.",
            )
            return
        }

        path = path + normalized
        usedPositions = nextUsed
        input = ""
        hintText = null
        hintStage = 0
        hintHighlight = null
        hintDefinition = null
        successVfxNonce += 1
        if (nextUsed.size == 2) {
            vfx.play(com.sonharf.game.ui.vfx.VfxEvent.SpecialNode(vfxAnchor))
        }
        // G5.8 SORUN 1: player only writes 4 intermediate words. After the
        // 4th valid move (usedPositions.size == MOVE_COUNT - 1), the last
        // position is deterministic — its target letter — so we auto-fill
        // the target word instead of asking the player to retype it.
        val autoComplete =
            nextUsed.size == LetterLadderEngine.MOVE_COUNT - 1 &&
                normalized != currentPuzzle.target
        val solved: Boolean
        if (autoComplete) {
            path = path + currentPuzzle.target
            usedPositions = (0 until LetterLadderEngine.WORD_LENGTH).toSet()
            solved = true
        } else {
            solved = path.size == LetterLadderEngine.MOVE_COUNT + 1 && normalized == currentPuzzle.target
        }
        if (solved) {
            completed = true
            message = sh("Hedefe ulaştın! Yol tamamlandı.", "Target reached! Path complete.")
            SonHarfSoundFx.victory()
        } else {
            message = sh("Doğru hamle. Devam et.", "Valid move. Keep going.")
            SonHarfSoundFx.puzzleSuccess()
        }
    }

    fun requestHint() {
        val currentPuzzle = puzzle ?: return
        if (completed) return
        val current = path.last()
        val route = LetterLadderEngine.completionPath(currentPuzzle, current, usedPositions, dictionary)
        val next = route?.getOrNull(1)
        val changed = next?.let { LetterLadderEngine.changedIndex(current, it) }
        // Advance the hint stage in-place so repeated presses reveal more
        // (but the answer is NEVER spelled out).
        SonHarfSoundFx.puzzleHint()
        vfx.play(com.sonharf.game.ui.vfx.VfxEvent.HintReveal(vfxAnchor))
        if (next == null || changed == null) {
            hintStage = 3
            hintHighlight = null
            hintText = sh(
                "Bu yoldan hedefe ulaşılamıyor. Son hamleyi geri al.",
                "This route can't reach the target. Undo the last move.",
            )
            return
        }
        when (hintStage) {
            0 -> {
                hintStage = 1
                hintHighlight = changed
                hintText = sh(
                    "İpucu 1/3: Değişecek kutu vurgulandı.",
                    "Hint 1/3: The position that changes is highlighted.",
                )
            }
            1 -> {
                hintStage = 2
                hintText = sh("İpucu 2/3: Sözlük anlamı yükleniyor…", "Hint 2/3: loading definition…")
                hintScope.launch {
                    val def = runCatching {
                        DictionaryDefinitionService.lookup(next, language)
                    }.getOrNull()
                    val shortMeaning = def?.meanings?.firstOrNull()?.trim().orEmpty().take(120)
                    hintDefinition = shortMeaning.ifEmpty { null }
                    hintText = if (hintDefinition != null) {
                        sh("İpucu 2/3: $hintDefinition", "Hint 2/3: $hintDefinition")
                    } else {
                        sh(
                            "İpucu 2/3: Bu kelimenin sözlük tanımı bulunamadı.",
                            "Hint 2/3: no dictionary definition available.",
                        )
                    }
                }
            }
            else -> {
                hintStage = 3
                val correct = next[changed]
                // Two distractors from the same alphabet, distinct from correct.
                val alphabet = if (language.lowercase(Locale.ROOT) == "en") {
                    "ABCDEFGHIJKLMNOPRSTUVWY"
                } else {
                    "ABCÇDEFGHIİJKLMNOÖPRSŞTUÜVYZ"
                }
                val correctUpper = correct.uppercaseChar()
                val pool = alphabet.filter { it != correctUpper }.toList().shuffled()
                val distractors = pool.take(2)
                val choices = (distractors + correctUpper).shuffled().joinToString(" / ")
                hintText = sh(
                    "İpucu 3/3: Bu harflerden biri: $choices",
                    "Hint 3/3: one of these: $choices",
                )
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().background(LetterLadderUi.Background),
        ) {
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = LetterLadderUi.Accent)
                        Spacer(Modifier.height(12.dp))
                        Text(sh("Bulmaca hazırlanıyor…", "Preparing puzzle…"), color = LetterLadderUi.Muted, fontSize = 12.sp)
                    }
                }
                return@Column
            }

            if (loadError || puzzle == null) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Rounded.TrackChanges, null, tint = LetterLadderUi.Orange, modifier = Modifier.size(42.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(sh("Bulmaca açılamadı", "Puzzle unavailable"), color = LetterLadderUi.Text, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(message, color = LetterLadderUi.Muted, textAlign = TextAlign.Center, fontSize = 12.sp)
                    Spacer(Modifier.height(18.dp))
                    Button(onClick = { puzzleNonce++ }) { Text(sh("TEKRAR DENE", "TRY AGAIN"), fontWeight = FontWeight.Black) }
                    TextButton(onClick = onExit) { Text(sh("Geri dön", "Go back")) }
                }
                return@Column
            }

            val currentPuzzle = puzzle!!
            Column(
                Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onExit, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back"), tint = LetterLadderUi.Text)
                    }
                    Spacer(Modifier.width(2.dp))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.harf_yolu_logo),
                            contentDescription = sh("Harf Yolu logosu", "Letter Path logo"),
                            modifier = Modifier.width(116.dp).height(54.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        )
                        Text(
                            sh("5 hamle • Her kutu yalnızca 1 kez değişir", "5 moves • Each position changes only once"),
                            color = LetterLadderUi.Muted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                    Surface(shape = RoundedCornerShape(99.dp), color = LetterLadderUi.Gold.copy(alpha = .16f)) {
                        Text(
                            "${usedPositions.size}/5",
                            Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            color = LetterLadderUi.Gold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    color = LetterLadderUi.SurfaceRaised,
                    border = BorderStroke(1.dp, LetterLadderUi.Border),
                ) {
                    Column(
                        Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(sh("BAŞLANGIÇ", "START"), color = LetterLadderUi.Muted, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(3.dp))
                        LadderWordTiles(
                            word = currentPuzzle.start.uppercase(locale),
                            locked = emptySet(),
                            accent = LetterLadderUi.Accent,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        )
                        Spacer(Modifier.height(3.dp))

                        // G5.8 SORUN 1: only 4 player rows. The final step
                        // (target) is filled automatically and shown below.
                        val playerRows = LetterLadderEngine.MOVE_COUNT - 1
                        for (move in 1..playerRows) {
                            val isActive = !completed && move == path.size
                            LadderMoveRow(
                                word = path.getOrNull(move)?.uppercase(locale),
                                usedPositions = usedPositions,
                                hintPosition = if (isActive) hintHighlight else null,
                                modifier = Modifier.fillMaxWidth().weight(1f),
                            )
                            if (move < playerRows) Spacer(Modifier.height(3.dp))
                        }

                        Spacer(Modifier.height(3.dp))
                        Text(sh("HEDEF", "TARGET"), color = LetterLadderUi.Muted, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(3.dp))
                        LadderWordTiles(
                            word = currentPuzzle.target.uppercase(locale),
                            locked = (0 until 5).toSet(),
                            accent = LetterLadderUi.Green,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = when {
                        completed -> LetterLadderUi.Green.copy(alpha = .10f)
                        hintText != null -> LetterLadderUi.Gold.copy(alpha = .10f)
                        else -> LetterLadderUi.Accent.copy(alpha = .07f)
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            completed -> LetterLadderUi.Green.copy(alpha = .25f)
                            hintText != null -> LetterLadderUi.Gold.copy(alpha = .30f)
                            else -> LetterLadderUi.Accent.copy(alpha = .25f)
                        },
                    ),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            hintText ?: message,
                            color = LetterLadderUi.Text,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(5) { index ->
                                val locked = index in usedPositions
                                Surface(
                                    modifier = Modifier.size(19.dp),
                                    shape = CircleShape,
                                    color = if (locked) LetterLadderUi.Green else LetterLadderUi.SurfaceSoft,
                                    border = BorderStroke(1.dp, if (locked) LetterLadderUi.Green else LetterLadderUi.Border),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (locked) {
                                            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(11.dp))
                                        } else {
                                            Surface(
                                                modifier = Modifier.size(4.dp),
                                                shape = CircleShape,
                                                color = LetterLadderUi.Muted.copy(alpha = .45f),
                                            ) {}
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        enabled = path.size > 1 && !completed,
                        onClick = {
                            if (path.size <= 1) return@OutlinedButton
                            val removed = path.last()
                            val previous = path[path.lastIndex - 1]
                            val index = LetterLadderEngine.changedIndex(previous, removed)
                            path = path.dropLast(1)
                            if (index != null) usedPositions = usedPositions - index
                            input = ""
                            hintText = null
                            message = sh("Son hamle geri alındı.", "Last move undone.")
                            SonHarfSoundFx.puzzleTap()
                        },
                        modifier = Modifier.weight(1f).height(38.dp).sonHarfPressScale(pressedScale = 0.94f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(sh("GERİ AL", "UNDO"), fontWeight = FontWeight.Black, fontSize = 10.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        enabled = !completed,
                        onClick = { requestHint() },
                        modifier = Modifier.weight(1f).height(38.dp).sonHarfPressScale(pressedScale = 0.94f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Rounded.Lightbulb, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(sh("İPUCU", "HINT"), fontWeight = FontWeight.Black, fontSize = 10.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = { resetCurrent(); SonHarfSoundFx.puzzleTap() },
                        modifier = Modifier.weight(1f).height(38.dp).sonHarfPressScale(pressedScale = 0.94f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(sh("SIFIRLA", "RESET"), fontWeight = FontWeight.Black, fontSize = 10.sp, maxLines = 1)
                    }
                }

                if (completed) {
                    Button(
                        onClick = { puzzleNonce++ },
                        modifier = Modifier.fillMaxWidth().height(42.dp).sonHarfPressScale(pressedScale = 0.94f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LetterLadderUi.Green, contentColor = Color.White),
                    ) {
                        Text(sh("YENİ BULMACA", "NEW PUZZLE"), fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.ChevronRight, null)
                    }
                }
            }

            if (!completed) {
                EmbeddedWordKeyboard(
                    value = input,
                    language = language,
                    enabled = true,
                    submitEnabled = input.length == LetterLadderEngine.WORD_LENGTH,
                    maxLength = LetterLadderEngine.WORD_LENGTH,
                    onValueChange = {
                        input = it
                        hintText = null
                    },
                    onSubmit = { submit() },
                    compact = true,
                    keySound = { SonHarfSoundFx.puzzleKey() },
                    actionSound = { SonHarfSoundFx.puzzleTap() },
                )
            }
        }

        if (successVfxNonce > 0) {
            PurchasedVictoryVfx(
                eventKey = "letter:${puzzle?.id}:$successVfxNonce",
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun LadderMoveRow(
    word: String?,
    usedPositions: Set<Int>,
    modifier: Modifier = Modifier,
    hintPosition: Int? = null,
) {
    // The live input already exists in the keyboard/input area below. Do not mirror it in the
    // active ladder row; that duplicate made the bottom word appear twice.
    val display = word ?: "     "
    LadderWordTiles(
        word = display,
        locked = usedPositions,
        accent = if (word != null) LetterLadderUi.Green else LetterLadderUi.Accent,
        highlightIndex = hintPosition,
        modifier = modifier,
    )
}

@Composable
private fun LadderWordTiles(
    word: String,
    locked: Set<Int>,
    accent: Color,
    modifier: Modifier = Modifier,
    highlightIndex: Int? = null,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(5) { index ->
            val char = word.getOrNull(index)?.takeUnless { it == ' ' }?.toString().orEmpty()
            val isHint = index == highlightIndex && index !in locked
            Surface(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(8.dp),
                color = when {
                    index in locked -> LetterLadderUi.Green.copy(alpha = .13f)
                    isHint -> LetterLadderUi.Gold.copy(alpha = .18f)
                    char.isNotEmpty() -> accent.copy(alpha = .09f)
                    else -> LetterLadderUi.SurfaceSoft
                },
                border = BorderStroke(
                    if (isHint) 2.dp else 1.dp,
                    when {
                        index in locked -> LetterLadderUi.Green.copy(alpha = .55f)
                        isHint -> LetterLadderUi.Gold
                        char.isNotEmpty() -> accent.copy(alpha = .40f)
                        else -> LetterLadderUi.Border
                    },
                ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(char, color = LetterLadderUi.Text, fontSize = 17.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
internal fun MonsterLetterLadderQuickCard(modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(136.dp).sonHarfPressScale(pressedScale = 0.95f).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = LetterLadderUi.SurfaceRaised,
        border = BorderStroke(1.dp, LetterLadderUi.Accent.copy(alpha = .24f)),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(R.drawable.harf_yolu_logo),
                contentDescription = sh("Harf Yolu logosu", "Letter Path logo"),
                modifier = Modifier.size(106.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(sh("3. OYUN", "GAME 3"), color = LetterLadderUi.Accent, fontSize = 9.sp, fontWeight = FontWeight.Black)
                Text(sh("HARF YOLU", "LETTER PATH"), color = LetterLadderUi.Text, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text(sh("5 hamlede hedef kelimeye ulaş", "Reach the target in 5 moves"), color = LetterLadderUi.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(sh("Değişen kutu kilitlenir", "Changed positions lock"), color = LetterLadderUi.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = LetterLadderUi.Muted, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun MiniLadderWord(word: String, locked: Set<Int>) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        word.forEachIndexed { index, char ->
            Surface(
                modifier = Modifier.size(12.dp),
                shape = RoundedCornerShape(3.dp),
                color = if (index in locked) LetterLadderUi.Green.copy(alpha = .20f) else LetterLadderUi.SurfaceRaised,
                border = BorderStroke(0.5.dp, if (index in locked) LetterLadderUi.Green else LetterLadderUi.Border),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(char.toString(), color = LetterLadderUi.Text, fontSize = 5.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}