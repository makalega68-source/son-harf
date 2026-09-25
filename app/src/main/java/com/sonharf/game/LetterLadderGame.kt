package com.sonharf.game

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.setValue
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
    val Background = Color(0xFF171C1B)
    val Surface = Color(0xFF222827)
    val SurfaceRaised = Color(0xFF1D2322)
    val SurfaceSoft = Color(0xFF2A302F)
    val Text = Color(0xFFF0EDE3)
    val Muted = Color(0xFFA9AFAB)
    val Border = Color(0xFF5C5239)
    val Accent = Color(0xFF32845E)
    val AccentStrong = Color(0xFF286B4D)
    val Turquoise = Color(0xFF40A878)
    val TurquoiseStrong = Color(0xFF32845E)
    val TurquoiseSoft = Color(0xFF173B30)
    val Orange = Color(0xFFC5AA73)
    val OrangeSoft = Color(0xFF3A3326)
    val Purple = Color(0xFFC85A54)
    val PurpleSoft = Color(0xFF442624)
    val AccentText = Color(0xFFF0EDE3)
    val Live = Accent
    val Coral = Orange
    val Green = Accent
    val Gold = Orange
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
 * In a four-move puzzle four of the five positions are changed exactly once.
 * A position that has changed is permanently locked. Because it cannot be
 * changed again, the new letter must already be the target letter for that
 * position; otherwise the target would become mathematically unreachable.
 */
internal object LetterLadderEngine {
    const val WORD_LENGTH = 5
    const val MOVE_COUNT = 4

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
     * unused positions. This is deliberately tiny (maximum depth four), so it
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

    /**
     * Returns only positions that can be changed next while preserving at least one full route.
     * The UI may reveal a position as a one-use hint, but never receives the replacement letter
     * or the next solution word from this helper.
     */
    fun viableNextMoveIndices(
        puzzle: LetterLadderPuzzle,
        current: String,
        usedPositions: Set<Int>,
        dictionary: Set<String>,
    ): List<Int> {
        if (current.length != WORD_LENGTH || puzzle.target.length != WORD_LENGTH) return emptyList()
        if (current == puzzle.target) return emptyList()

        return (0 until WORD_LENGTH).filter { index ->
            if (index in usedPositions || current[index] == puzzle.target[index]) return@filter false
            val chars = current.toCharArray()
            chars[index] = puzzle.target[index]
            val next = String(chars)
            if (next !in dictionary) return@filter false

            next == puzzle.target || completionPath(
                puzzle = puzzle,
                current = next,
                usedPositions = usedPositions + index,
                dictionary = dictionary,
            ) != null
        }
    }

    fun viableNextMoveCount(
        puzzle: LetterLadderPuzzle,
        current: String,
        usedPositions: Set<Int>,
        dictionary: Set<String>,
    ): Int = viableNextMoveIndices(puzzle, current, usedPositions, dictionary).size

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
        if (used.size != MOVE_COUNT) return null
        val start = path.first()
        val target = path.last()
        if ((0 until WORD_LENGTH).count { start[it] == target[it] } != WORD_LENGTH - MOVE_COUNT) return null
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
    var hintUsed by remember { mutableStateOf(false) }
    var hintedIndex by remember { mutableStateOf<Int?>(null) }
    var successVfxNonce by remember { mutableIntStateOf(0) }
    var showRules by remember { mutableStateOf(false) }
    var stats by remember { mutableStateOf(LetterLadderStats.read(context)) }

    LaunchedEffect(language, puzzleNonce) {
        loading = true
        loadError = false
        completed = false
        hintText = null
        hintUsed = false
        hintedIndex = null
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
        hintedIndex = null
        successVfxNonce += 1
        val solved = path.size == LetterLadderEngine.MOVE_COUNT + 1 && normalized == currentPuzzle.target
        if (solved) {
            completed = true
            stats = LetterLadderStats.recordSolve(context)
            message = sh("Hedefe ulaştın! Dört değişim tamamlandı.", "Target reached! All four changes are complete.")
            SonHarfSoundFx.victory()
        } else {
            message = sh("Doğru hamle. Devam et.", "Valid move. Keep going.")
            SonHarfSoundFx.puzzleSuccess()
        }
    }

    Box(Modifier.fillMaxSize()) {
        HarfYoluBackdrop(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize(),
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
            val currentWord = path.lastOrNull().orEmpty()
            // The final move produces the target itself: its letters are typed on the target row.
            val finalMove = !completed && path.size == LetterLadderEngine.MOVE_COUNT
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
                        Text(
                            sh("HARF YOLU", "LETTER PATH"),
                            color = LetterLadderUi.Text,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = .6.sp,
                        )
                        Text(
                            sh("4 hamle • Her değişim hedefe yaklaşır", "4 moves • Every change closes the gap"),
                            color = LetterLadderUi.Muted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                    LadderStatChip("🏆 ${stats.solved}", LetterLadderUi.Accent)
                    Spacer(Modifier.width(4.dp))
                    LadderStatChip("🔥 ${stats.streak}", LetterLadderUi.Purple)
                    Spacer(Modifier.width(4.dp))
                    LadderStatChip("${usedPositions.size}/${LetterLadderEngine.MOVE_COUNT}", LetterLadderUi.Gold)
                }

                Box(Modifier.fillMaxWidth().weight(1f)) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(20.dp),
                        color = LetterLadderUi.Surface.copy(alpha = .92f),
                        border = BorderStroke(1.dp, LetterLadderUi.Border),
                        shadowElevation = 4.dp,
                    ) {
                        Column(
                            Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            LadderPathRow(
                                marker = "▶",
                                label = sh("BAŞLANGIÇ", "START"),
                                markerColor = LetterLadderUi.AccentStrong,
                                reached = true,
                                modifier = Modifier.fillMaxWidth().weight(1f),
                            ) {
                                LadderWordTiles(
                                    word = currentPuzzle.start.uppercase(locale),
                                    locked = emptySet(),
                                    accent = LetterLadderUi.Accent,
                                    hintedIndex = hintedIndex.takeIf { path.size == 1 },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }

                            for (move in 1 until LetterLadderEngine.MOVE_COUNT) {
                                val committedWord = path.getOrNull(move)?.uppercase(locale)
                                val isCurrentWord = committedWord != null && move == path.lastIndex
                                val isActiveEntry = committedWord == null && move == path.size
                                val changed = path.getOrNull(move)?.let { LetterLadderEngine.changedIndex(path[move - 1], it) }
                                LadderPathRow(
                                    marker = if (committedWord != null) "✓" else "$move",
                                    label = null,
                                    markerColor = when {
                                        committedWord != null -> LetterLadderUi.Green
                                        isActiveEntry -> LetterLadderUi.Accent
                                        else -> LetterLadderUi.Muted.copy(alpha = .5f)
                                    },
                                    reached = committedWord != null,
                                    active = isActiveEntry,
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                ) {
                                    LadderMoveRow(
                                        word = committedWord,
                                        activeInput = input.uppercase(locale).takeIf { isActiveEntry },
                                        usedPositions = usedPositions,
                                        hintedIndex = hintedIndex.takeIf { isCurrentWord || isActiveEntry },
                                        changedIndex = changed,
                                        active = isActiveEntry,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }

                            LadderPathRow(
                                marker = "★",
                                label = sh("HEDEF", "TARGET"),
                                markerColor = LetterLadderUi.Purple,
                                reached = completed,
                                active = finalMove,
                                modifier = Modifier.fillMaxWidth().weight(1f),
                            ) {
                                LadderTargetTiles(
                                    target = currentPuzzle.target.uppercase(locale),
                                    current = currentWord.uppercase(locale),
                                    typed = if (finalMove) input.uppercase(locale) else "",
                                    hintedIndex = hintedIndex.takeIf { finalMove },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                    if (completed) {
                        LadderCompletionCard(
                            words = path.map { it.uppercase(locale) },
                            hintUsed = hintUsed,
                            stats = stats,
                            modifier = Modifier.align(Alignment.Center).padding(horizontal = 18.dp),
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
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            val progressCount = usedPositions.size.coerceIn(0, LetterLadderEngine.MOVE_COUNT)
                            repeat(LetterLadderEngine.MOVE_COUNT) { index ->
                                val reached = index < progressCount
                                Surface(
                                    modifier = Modifier.size(20.dp),
                                    shape = CircleShape,
                                    color = if (reached) LetterLadderUi.Green else LetterLadderUi.SurfaceSoft,
                                    border = BorderStroke(1.dp, if (reached) LetterLadderUi.Green else LetterLadderUi.Border),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (reached) {
                                            Icon(Icons.Rounded.Check, null, tint = LetterLadderUi.AccentText, modifier = Modifier.size(11.dp))
                                        } else {
                                            Surface(modifier = Modifier.size(4.dp), shape = CircleShape, color = LetterLadderUi.Muted.copy(alpha = .45f)) {}
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (!completed) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showRules = true },
                            modifier = Modifier.weight(1f).height(40.dp).sonHarfPressScale(pressedScale = 0.94f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(13.dp),
                            border = BorderStroke(1.dp, LetterLadderUi.Accent.copy(alpha = .72f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LetterLadderUi.AccentStrong),
                        ) {
                            Text(sh("NASIL OYNANIR?", "HOW TO PLAY?"), fontWeight = FontWeight.Black, fontSize = 10.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            enabled = !hintUsed,
                            onClick = {
                                val current = path.last()
                                val hintIndex = LetterLadderEngine.viableNextMoveIndices(
                                    puzzle = currentPuzzle,
                                    current = current,
                                    usedPositions = usedPositions,
                                    dictionary = dictionary,
                                ).firstOrNull()
                                hintUsed = true
                                hintedIndex = hintIndex
                                hintText = if (hintIndex == null) {
                                    sh("Bu rota kapanmış görünüyor. Oyunu tamamlayıp yeni oyuna geç.", "This route is closed. Finish the puzzle, then start a new game.")
                                } else {
                                    sh(
                                        "İpucu kullanıldı: turuncu işaretli kutudaki harfi değiştir. Harfi kendin bul.",
                                        "Hint used: change the orange-marked tile. Find the letter yourself.",
                                    )
                                }
                                SonHarfSoundFx.puzzleHint()
                            },
                            modifier = Modifier.weight(1f).height(40.dp).sonHarfPressScale(pressedScale = 0.94f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(13.dp),
                            border = BorderStroke(1.dp, LetterLadderUi.Orange.copy(alpha = .78f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = LetterLadderUi.Orange,
                                disabledContentColor = LetterLadderUi.Muted.copy(alpha = .34f),
                            ),
                        ) {
                            Icon(Icons.Rounded.Lightbulb, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(
                                if (hintUsed) sh("İPUCU 0/1", "HINT 0/1") else sh("İPUCU 1/1", "HINT 1/1"),
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                maxLines = 1,
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { puzzleNonce++ },
                        modifier = Modifier.fillMaxWidth().height(50.dp).sonHarfPressScale(pressedScale = 0.94f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LetterLadderUi.Accent,
                            contentColor = LetterLadderUi.AccentText,
                        ),
                    ) {
                        Text(sh("YENİ OYUN", "NEW GAME"), fontWeight = FontWeight.Black, fontSize = 16.sp)
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

    if (showRules) {
        AlertDialog(
            onDismissRequest = { showRules = false },
            confirmButton = { TextButton(onClick = { showRules = false }) { Text(sh("ANLADIM", "GOT IT"), fontWeight = FontWeight.Black) } },
            title = { Text(sh("Harf Yolu nasıl oynanır?", "How to play Letter Path"), fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(sh("1. Her hamlede kelimenin yalnızca 1 harfini değiştir.", "1. Change exactly one letter on each move."))
                    Text(sh("2. Yeni harf, hedef kelimenin aynı kutusundaki harf olmalı.", "2. The new letter must be the target's letter in that position."))
                    Text(sh("3. Değişen kutu kilitlenir; her hamle yeni bir kutu açar.", "3. A changed tile locks; each move opens a new one."))
                    Text(sh("4. Her ara adım gerçek bir kelime olmalı. 4 hamlede hedefe ulaş!", "4. Every step must be a real word. Reach the target in 4 moves!"))
                }
            },
        )
    }
}

/** A ladder row: a step marker on a vertical rail, then the word tiles. */
@Composable
private fun LadderPathRow(
    marker: String,
    label: String?,
    markerColor: Color,
    reached: Boolean,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    content: @Composable () -> Unit,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.width(38.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(if (active) 26.dp else 22.dp),
                shape = CircleShape,
                color = if (reached || active) markerColor else LetterLadderUi.SurfaceSoft,
                border = BorderStroke(if (active) 2.dp else 1.dp, markerColor),
                shadowElevation = if (active) 4.dp else 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(marker, color = if (reached || active) Color.White else markerColor, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
            if (label != null) {
                Text(label, color = markerColor, fontSize = 7.sp, fontWeight = FontWeight.Black, maxLines = 1)
            }
        }
        Spacer(Modifier.width(4.dp))
        Box(Modifier.weight(1f).fillMaxHeight().padding(vertical = 1.dp)) { content() }
    }
}

@Composable
private fun LadderStatChip(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(99.dp), color = color.copy(alpha = .14f)) {
        Text(text, Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = color, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

/** The target row: letters already reached glow green; on the final move the typed letters appear here. */
@Composable
private fun LadderTargetTiles(
    target: String,
    current: String,
    typed: String,
    hintedIndex: Int?,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(LetterLadderEngine.WORD_LENGTH) { index ->
            val reached = current.getOrNull(index) == target.getOrNull(index)
            val typedChar = typed.getOrNull(index)
            val hinted = index == hintedIndex
            Surface(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(9.dp),
                color = when {
                    hinted -> LetterLadderUi.Gold.copy(alpha = .20f)
                    typedChar != null -> LetterLadderUi.Accent.copy(alpha = .12f)
                    reached -> LetterLadderUi.Green.copy(alpha = .16f)
                    else -> LetterLadderUi.Purple.copy(alpha = .10f)
                },
                border = BorderStroke(
                    if (hinted || typedChar != null) 2.dp else 1.dp,
                    when {
                        hinted -> LetterLadderUi.Gold
                        typedChar != null -> LetterLadderUi.Accent
                        reached -> LetterLadderUi.Green.copy(alpha = .6f)
                        else -> LetterLadderUi.Purple.copy(alpha = .45f)
                    },
                ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        (typedChar ?: target.getOrNull(index) ?: ' ').toString(),
                        color = when {
                            typedChar != null -> LetterLadderUi.Text
                            reached -> LetterLadderUi.Text
                            else -> LetterLadderUi.Purple
                        }.copy(alpha = if (typed.isNotEmpty() && typedChar == null) .35f else 1f),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                    )
                    if (reached && typedChar == null) {
                        Text("✓", color = LetterLadderUi.Green, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.TopEnd).padding(3.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LadderCompletionCard(words: List<String>, hintUsed: Boolean, stats: LetterLadderStats.Snapshot, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = LetterLadderUi.Surface,
        border = BorderStroke(2.dp, LetterLadderUi.Accent),
        shadowElevation = 14.dp,
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🎉", fontSize = 34.sp)
            Text(sh("YOL TAMAMLANDI!", "PATH COMPLETE!"), color = LetterLadderUi.Text, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(
                if (hintUsed) sh("İpucuyla çözdün", "Solved with a hint") else sh("İpucusuz, kusursuz! ⭐", "No hints, flawless! ⭐"),
                color = if (hintUsed) LetterLadderUi.Muted else LetterLadderUi.AccentStrong,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(words.joinToString("  →  "), color = LetterLadderUi.Text, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LadderStatChip(sh("🏆 ${stats.solved} çözüm", "🏆 ${stats.solved} solved"), LetterLadderUi.Accent)
                LadderStatChip(sh("🔥 ${stats.streak} gün seri", "🔥 ${stats.streak}-day streak"), LetterLadderUi.Purple)
            }
        }
    }
}

/** Solved puzzles and the daily streak, kept on the device. */
internal object LetterLadderStats {
    private const val PREFS = "son_harf_letter_ladder_stats_v1"

    data class Snapshot(val solved: Int, val streak: Int, val best: Int)

    fun read(context: android.content.Context): Snapshot {
        val prefs = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        val today = System.currentTimeMillis() / 86_400_000L
        val last = prefs.getLong("last_day", -10L)
        // A streak survives only if a puzzle was solved today or yesterday.
        val streak = if (today - last <= 1L) prefs.getInt("streak", 0) else 0
        return Snapshot(prefs.getInt("solved", 0), streak, prefs.getInt("best", 0))
    }

    fun recordSolve(context: android.content.Context): Snapshot {
        val prefs = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        val today = System.currentTimeMillis() / 86_400_000L
        val last = prefs.getLong("last_day", -10L)
        val streak = when (today - last) {
            0L -> prefs.getInt("streak", 1).coerceAtLeast(1)
            1L -> prefs.getInt("streak", 0) + 1
            else -> 1
        }
        val solved = prefs.getInt("solved", 0) + 1
        val best = maxOf(prefs.getInt("best", 0), streak)
        prefs.edit().putInt("solved", solved).putInt("streak", streak).putInt("best", best).putLong("last_day", today).apply()
        return Snapshot(solved, streak, best)
    }
}

@Composable
private fun LadderMoveRow(
    word: String?,
    activeInput: String? = null,
    usedPositions: Set<Int>,
    hintedIndex: Int? = null,
    changedIndex: Int? = null,
    active: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val display = word ?: activeInput?.padEnd(LetterLadderEngine.WORD_LENGTH, ' ') ?: "     "
    LadderWordTiles(
        word = display,
        locked = usedPositions,
        accent = if (word != null) LetterLadderUi.Green else LetterLadderUi.Accent,
        hintedIndex = hintedIndex,
        changedIndex = changedIndex,
        active = active,
        modifier = modifier,
    )
}

@Composable
private fun LadderWordTiles(
    word: String,
    locked: Set<Int>,
    accent: Color,
    hintedIndex: Int? = null,
    changedIndex: Int? = null,
    active: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(5) { index ->
            val char = word.getOrNull(index)?.takeUnless { it == ' ' }?.toString().orEmpty()
            val hinted = index == hintedIndex
            val changed = index == changedIndex
            Surface(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(9.dp),
                color = when {
                    hinted -> LetterLadderUi.Gold.copy(alpha = .20f)
                    changed -> LetterLadderUi.Gold.copy(alpha = .16f)
                    index in locked -> LetterLadderUi.Green.copy(alpha = .13f)
                    char.isNotEmpty() -> accent.copy(alpha = .09f)
                    active -> LetterLadderUi.Surface
                    else -> LetterLadderUi.SurfaceSoft
                },
                border = BorderStroke(
                    if (hinted || changed || active) 2.dp else 1.dp,
                    when {
                        hinted -> LetterLadderUi.Gold
                        changed -> LetterLadderUi.Gold.copy(alpha = .8f)
                        active -> LetterLadderUi.Accent.copy(alpha = .75f)
                        index in locked -> LetterLadderUi.Green.copy(alpha = .55f)
                        char.isNotEmpty() -> accent.copy(alpha = .40f)
                        else -> LetterLadderUi.Border
                    },
                ),
                shadowElevation = if (active) 2.dp else 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        char,
                        color = if (hinted) LetterLadderUi.Gold else LetterLadderUi.Text,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                    )
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
                Text(sh("4 hamlede hedef kelimeye ulaş", "Reach the target in 4 moves"), color = LetterLadderUi.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
