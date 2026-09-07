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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import java.time.LocalDate
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    private val curatedTurkish = listOf(
        listOf("kalın", "yalın", "yalan", "yalak", "yamak", "yumak"),
    )

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

    fun hintPositions(
        puzzle: LetterLadderPuzzle,
        current: String,
        usedPositions: Set<Int>,
        dictionary: Set<String>,
    ): List<Int> = (0 until WORD_LENGTH)
        .filterNot { it in usedPositions }
        .filter { index ->
            val chars = current.toCharArray()
            chars[index] = puzzle.target[index]
            String(chars) in dictionary
        }

    fun generate(
        sourceWords: Set<String>,
        language: String,
        seed: Long,
        preferCurated: Boolean = false,
    ): LetterLadderPuzzle? {
        val locale = if (language.lowercase(Locale.ROOT) == "en") Locale.ENGLISH else tr
        val words = sourceWords.asSequence()
            .map { it.trim().lowercase(locale) }
            .filter { word -> word.length == WORD_LENGTH && word.all(Char::isLetter) }
            .toSet()
        if (words.size < MOVE_COUNT + 1) return null

        if (preferCurated && language.lowercase(Locale.ROOT) != "en") {
            curatedTurkish.firstOrNull { chain -> chain.all { it in words } }?.let { chain ->
                return puzzleFromPath(chain, language, "curated")
            }
        }

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
            if (path.size == MOVE_COUNT + 1) return path.toList()
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
            return puzzleFromPath(path, language, seed.toString())
        }
        return null
    }

    private fun puzzleFromPath(path: List<String>, language: String, suffix: String): LetterLadderPuzzle? {
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
        return LetterLadderPuzzle(
            id = "${language.lowercase(Locale.ROOT)}-$suffix-$start-$target",
            start = start,
            target = target,
            solution = path,
        )
    }

    private fun pattern(word: String, index: Int): String = buildString(WORD_LENGTH) {
        word.forEachIndexed { i, char -> append(if (i == index) '*' else char) }
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

    LaunchedEffect(language, puzzleNonce) {
        loading = true
        loadError = false
        completed = false
        hintText = null
        input = ""
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
        val daySeed = LocalDate.now().toEpochDay() + puzzleNonce.toLong() * 104_729L
        val generated = withContext(Dispatchers.Default) {
            LetterLadderEngine.generate(
                sourceWords = loaded,
                language = language,
                seed = daySeed,
                preferCurated = puzzleNonce == 0,
            )
        }
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
        completed = false
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
            SonHarfSoundFx.warning()
            message = when (check.reject) {
                LetterLadderReject.LENGTH -> sh("Kelime tam 5 harf olmalı.", "The word must contain exactly 5 letters.")
                LetterLadderReject.NOT_DICTIONARY -> sh("Bu kelime Son Harf sözlüğünde yok.", "This word is not in the Son Harf dictionary.")
                LetterLadderReject.NOT_ONE_CHANGE -> sh("Yalnızca 1 harf değiştirebilirsin.", "You may change exactly one letter.")
                LetterLadderReject.POSITION_ALREADY_USED -> sh("Bu kutuyu daha önce değiştirdin; tekrar değişemez.", "That position was already changed and is locked.")
                LetterLadderReject.WRONG_TARGET_LETTER -> sh("Bu kutu bir daha değişmeyecek; hedefteki harfi bulmalısın.", "This position locks after the move; use its target letter.")
                null -> sh("Geçersiz hamle.", "Invalid move.")
            }
            return
        }

        val changed = check.changedIndex ?: return
        path = path + normalized
        usedPositions = usedPositions + changed
        input = ""
        hintText = null
        val solved = path.size == LetterLadderEngine.MOVE_COUNT + 1 && normalized == currentPuzzle.target
        if (solved) {
            completed = true
            message = sh("Hedefe ulaştın! 5 farklı harfi birer kez değiştirdin.", "Target reached! You changed all five positions exactly once.")
            SonHarfSoundFx.victory()
        } else {
            message = sh("Doğru hamle. ${usedPositions.size}/5 kutu kilitlendi.", "Valid move. ${usedPositions.size}/5 positions are locked.")
            SonHarfSoundFx.wordAccepted()
        }
    }

    Column(
        Modifier.fillMaxSize().background(MonsterUi.Background),
    ) {
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MonsterUi.Accent)
                    Spacer(Modifier.height(12.dp))
                    Text(sh("Bulmaca hazırlanıyor…", "Preparing puzzle…"), color = MonsterUi.Muted, fontSize = 12.sp)
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
                Icon(Icons.Rounded.TrackChanges, null, tint = MonsterUi.Orange, modifier = Modifier.size(42.dp))
                Spacer(Modifier.height(12.dp))
                Text(sh("Bulmaca açılamadı", "Puzzle unavailable"), color = MonsterUi.Text, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Spacer(Modifier.height(6.dp))
                Text(message, color = MonsterUi.Muted, textAlign = TextAlign.Center, fontSize = 12.sp)
                Spacer(Modifier.height(18.dp))
                Button(onClick = { puzzleNonce++ }) { Text(sh("TEKRAR DENE", "TRY AGAIN"), fontWeight = FontWeight.Black) }
                TextButton(onClick = onExit) { Text(sh("Geri dön", "Go back")) }
            }
            return@Column
        }

        val currentPuzzle = puzzle!!
        val scroll = rememberScrollState()
        Column(
            Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onExit) { Icon(Icons.Rounded.ArrowBack, sh("Geri", "Back"), tint = MonsterUi.Text) }
                Column(Modifier.weight(1f)) {
                    Text(sh("HARF YOLU", "LETTER PATH"), color = MonsterUi.Text, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(sh("5 hamle • Her kutu yalnızca 1 kez değişir", "5 moves • Each position changes only once"), color = MonsterUi.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Surface(shape = RoundedCornerShape(99.dp), color = MonsterUi.Gold.copy(alpha = .16f)) {
                    Text("${usedPositions.size}/5", Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = MonsterUi.Gold, fontWeight = FontWeight.Black)
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MonsterUi.SurfaceRaised,
                border = BorderStroke(1.dp, MonsterUi.Border),
            ) {
                Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(sh("BAŞLANGIÇ", "START"), color = MonsterUi.Muted, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(6.dp))
                    LadderWordTiles(currentPuzzle.start.uppercase(locale), locked = emptySet(), accent = MonsterUi.Accent)
                    Spacer(Modifier.height(10.dp))

                    for (move in 1..LetterLadderEngine.MOVE_COUNT) {
                        LadderMoveRow(
                            number = move,
                            word = path.getOrNull(move)?.uppercase(locale),
                            isActive = !completed && move == path.size,
                            activeInput = input.uppercase(locale),
                            usedPositions = usedPositions,
                        )
                        if (move < LetterLadderEngine.MOVE_COUNT) Spacer(Modifier.height(6.dp))
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(sh("HEDEF", "TARGET"), color = MonsterUi.Muted, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(6.dp))
                    LadderWordTiles(currentPuzzle.target.uppercase(locale), locked = (0 until 5).toSet(), accent = MonsterUi.Green)
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(17.dp),
                color = if (completed) MonsterUi.Green.copy(alpha = .10f) else MonsterUi.Accent.copy(alpha = .07f),
                border = BorderStroke(1.dp, (if (completed) MonsterUi.Green else MonsterUi.Accent).copy(alpha = .25f)),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(message, color = MonsterUi.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(sh("Değişen kutular", "Changed positions"), color = MonsterUi.Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
                        repeat(5) { index ->
                            Surface(
                                modifier = Modifier.size(28.dp),
                                shape = CircleShape,
                                color = if (index in usedPositions) MonsterUi.Green else MonsterUi.SurfaceSoft,
                                border = BorderStroke(1.dp, if (index in usedPositions) MonsterUi.Green else MonsterUi.Border),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (index in usedPositions) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(15.dp))
                                    else Text("${index + 1}", color = MonsterUi.Muted, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            if (index < 4) Spacer(Modifier.width(5.dp))
                        }
                    }
                }
            }

            hintText?.let { hint ->
                Surface(shape = RoundedCornerShape(15.dp), color = MonsterUi.Gold.copy(alpha = .12f), border = BorderStroke(1.dp, MonsterUi.Gold.copy(alpha = .28f))) {
                    Text(hint, Modifier.fillMaxWidth().padding(11.dp), color = MonsterUi.Text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        SonHarfSoundFx.tap()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) { Text(sh("GERİ AL", "UNDO"), fontWeight = FontWeight.Black, fontSize = 11.sp) }

                OutlinedButton(
                    enabled = !completed,
                    onClick = {
                        val positions = LetterLadderEngine.hintPositions(currentPuzzle, path.last(), usedPositions, dictionary)
                        hintText = if (positions.isEmpty()) {
                            sh("Bu yolda devam edecek geçerli hamle kalmadı. Bir adım geri al.", "No valid continuation remains. Undo one move.")
                        } else {
                            val readable = positions.joinToString(" / ") { (it + 1).toString() }
                            sh("İpucu: $readable. kutudaki harfi hedefe çevir.", "Hint: change position $readable to its target letter.")
                        }
                        SonHarfSoundFx.softNotify()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Rounded.Lightbulb, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(sh("İPUCU", "HINT"), fontWeight = FontWeight.Black, fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { resetCurrent(); SonHarfSoundFx.tap() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(sh("SIFIRLA", "RESET"), fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }

            if (completed) {
                Button(
                    onClick = { puzzleNonce++ },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MonsterUi.Green, contentColor = Color.White),
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
                onValueChange = { input = it },
                onSubmit = { submit() },
            )
        }
    }
}

@Composable
private fun LadderMoveRow(
    number: Int,
    word: String?,
    isActive: Boolean,
    activeInput: String,
    usedPositions: Set<Int>,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(30.dp),
            shape = CircleShape,
            color = if (word != null) MonsterUi.Green.copy(alpha = .16f) else if (isActive) MonsterUi.Accent.copy(alpha = .16f) else MonsterUi.SurfaceSoft,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("$number", color = if (isActive) MonsterUi.Accent else MonsterUi.Text, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.width(8.dp))
        val display = word ?: if (isActive) activeInput.padEnd(5, ' ') else "     "
        LadderWordTiles(
            word = display,
            locked = usedPositions,
            accent = if (word != null) MonsterUi.Green else MonsterUi.Accent,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LadderWordTiles(
    word: String,
    locked: Set<Int>,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(5) { index ->
            val char = word.getOrNull(index)?.takeUnless { it == ' ' }?.toString().orEmpty()
            Surface(
                modifier = Modifier.weight(1f).height(42.dp),
                shape = RoundedCornerShape(10.dp),
                color = when {
                    index in locked -> MonsterUi.Green.copy(alpha = .13f)
                    char.isNotEmpty() -> accent.copy(alpha = .09f)
                    else -> MonsterUi.SurfaceSoft
                },
                border = BorderStroke(1.dp, when {
                    index in locked -> MonsterUi.Green.copy(alpha = .55f)
                    char.isNotEmpty() -> accent.copy(alpha = .40f)
                    else -> MonsterUi.Border
                }),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(char, color = MonsterUi.Text, fontSize = 19.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
internal fun MonsterLetterLadderQuickCard(modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(126.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MonsterUi.SurfaceRaised,
        border = BorderStroke(1.dp, MonsterUi.Accent.copy(alpha = .24f)),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(86.dp),
                shape = RoundedCornerShape(18.dp),
                color = MonsterUi.Accent.copy(alpha = .09f),
                border = BorderStroke(1.dp, MonsterUi.Accent.copy(alpha = .20f)),
            ) {
                Column(
                    Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    MiniLadderWord("KALIN", setOf())
                    Text("↓", color = MonsterUi.Accent, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    MiniLadderWord("YUMAK", (0 until 5).toSet())
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(sh("3. OYUN", "GAME 3"), color = MonsterUi.Accent, fontSize = 9.sp, fontWeight = FontWeight.Black)
                Text(sh("HARF YOLU", "LETTER PATH"), color = MonsterUi.Text, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text(sh("5 hamlede hedef kelimeye ulaş", "Reach the target in 5 moves"), color = MonsterUi.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(sh("Değişen kutu kilitlenir", "Changed positions lock"), color = MonsterUi.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = MonsterUi.Muted, modifier = Modifier.size(22.dp))
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
                color = if (index in locked) MonsterUi.Green.copy(alpha = .20f) else MonsterUi.SurfaceRaised,
                border = BorderStroke(0.5.dp, if (index in locked) MonsterUi.Green else MonsterUi.Border),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(char.toString(), color = MonsterUi.Text, fontSize = 5.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
