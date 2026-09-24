package com.sonharf.game

import android.content.Context
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Game moments the companion may respond to. The mascot decides itself whether and how. */
internal enum class WordSiegeMascotEvent {
    PRAISE, BIG_PRAISE, RARE_WORD, COMFORT, CRITICAL, BEHIND, RIVAL_STRONG, AHEAD, STREAK, STREAK_LOST,
}

internal enum class WordSiegeMascotOutcome { WIN, LOSS, DRAW }

/** One occurrence of a game moment; a new [key] means a new occurrence. */
internal data class WordSiegeMascotSignal(
    val key: String,
    val event: WordSiegeMascotEvent,
    /** The player's word for praise events, remembered as a shared memory. */
    val word: String = "",
    /** Streak length for [WordSiegeMascotEvent.STREAK]. */
    val count: Int = 0,
)

internal enum class WordSiegeMascotVisitKind { CAPTURE, HINT }

/** A spot in the game area worth flying to: fresh territory, or (practice only) a bonus square. */
internal data class WordSiegeMascotVisit(
    val key: String,
    /** Fraction (0..1) of the companion area. */
    val point: Offset,
    val kind: WordSiegeMascotVisitKind,
)

/** Where the player last touched, observed without consuming the touch. */
internal class WordSiegeMascotTouchState {
    var position by mutableStateOf(Offset.Unspecified)
    var tick by mutableIntStateOf(0)
    var dragPosition by mutableStateOf(Offset.Unspecified)
    var dragTick by mutableIntStateOf(0)
    var lastDragAt = 0L
}

/**
 * Lets the mascot notice touches on the game area. Put it on the same parent that hosts the
 * companion. Events are only observed, never consumed, so game gestures work as before.
 */
internal fun Modifier.wordSiegeMascotTouchWatcher(state: WordSiegeMascotTouchState): Modifier =
    pointerInput(state) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull() ?: continue
                when (event.type) {
                    PointerEventType.Press -> {
                        state.position = change.position
                        state.tick += 1
                    }
                    PointerEventType.Move -> if (change.pressed) {
                        val now = SystemClock.uptimeMillis()
                        if (now - state.lastDragAt > 120L) {
                            state.lastDragAt = now
                            state.dragPosition = change.position
                            state.dragTick += 1
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

/** Friendship memory between the player and the mascot. Stored only on this device. */
internal class WordSiegeMascotBond(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    val points: Int get() = prefs.getInt(KEY_BOND, 0)
    val level: Int get() = when {
        points >= 150 -> 2
        points >= 40 -> 1
        else -> 0
    }

    /** Registers a meeting; returns the meeting count and the days since the previous one. */
    fun meet(): Pair<Int, Long> {
        val today = today()
        val lastDay = prefs.getLong(KEY_LAST_DAY, -1L)
        val meetings = prefs.getInt(KEY_MEETINGS, 0) + 1
        val editor = prefs.edit()
            .putInt(KEY_MEETINGS, meetings)
            .putLong(KEY_LAST_DAY, today)
            .putInt(KEY_BOND, min(MAX_BOND, points + 2))
            .putInt(KEY_MOOD, mood - mood.sign)
        if (prefs.getLong(KEY_FIRST_DAY, -1L) < 0L) editor.putLong(KEY_FIRST_DAY, today)
        editor.apply()
        return meetings to if (lastDay < 0L) 0L else today - lastDay
    }

    fun add(amount: Int) {
        prefs.edit().putInt(KEY_BOND, (points + amount).coerceIn(0, MAX_BOND)).apply()
    }

    val wins: Int get() = prefs.getInt(KEY_WINS, 0)
    val winStreak: Int get() = prefs.getInt(KEY_WIN_STREAK, 0)
    val bestWinStreak: Int get() = prefs.getInt(KEY_BEST_WIN_STREAK, 0)
    val longestWord: String get() = prefs.getString(KEY_LONGEST_WORD, "").orEmpty()

    /** -5 (down after losses) .. 5 (buoyant after wins); drifts back toward 0 every meeting. */
    val mood: Int get() = prefs.getInt(KEY_MOOD, 0)

    /** Whole days since the first meeting, or -1 before it. */
    fun daysTogether(): Long {
        val first = prefs.getLong(KEY_FIRST_DAY, -1L)
        return if (first < 0L) -1L else today() - first
    }

    fun recordWin() {
        val streak = winStreak + 1
        prefs.edit()
            .putInt(KEY_WINS, wins + 1)
            .putInt(KEY_WIN_STREAK, streak)
            .putInt(KEY_BEST_WIN_STREAK, max(bestWinStreak, streak))
            .putInt(KEY_MOOD, (mood + 2).coerceIn(-5, 5))
            .putInt(KEY_BOND, min(MAX_BOND, points + 6))
            .apply()
    }

    fun recordLoss() {
        prefs.edit()
            .putInt(KEY_WIN_STREAK, 0)
            .putInt(KEY_MOOD, (mood - 2).coerceIn(-5, 5))
            .putInt(KEY_BOND, min(MAX_BOND, points + 2))
            .apply()
    }

    /** Remembers the longest word the player has played in front of the mascot. */
    fun recordWord(word: String) {
        val clean = word.trim()
        if (clean.length > longestWord.length && clean.length <= 20) {
            prefs.edit().putString(KEY_LONGEST_WORD, clean).apply()
        }
    }

    /** The look the player picked with a long press, or null to follow the default. */
    var skinChoice: WordSiegeMascotSkin?
        get() = when (prefs.getString(KEY_SKIN, "")) {
            "pink" -> WordSiegeMascotSkin.PINK
            "orb" -> WordSiegeMascotSkin.ORB
            else -> null
        }
        set(value) {
            prefs.edit().putString(KEY_SKIN, when (value) {
                WordSiegeMascotSkin.PINK -> "pink"
                WordSiegeMascotSkin.ORB -> "orb"
                null -> ""
            }).apply()
        }

    private fun today(): Long = System.currentTimeMillis() / 86_400_000L

    private companion object {
        const val FILE = "word_siege_mascot_bond"
        const val KEY_BOND = "bond"
        const val KEY_MEETINGS = "meetings"
        const val KEY_LAST_DAY = "last_day"
        const val KEY_WINS = "wins"
        const val KEY_WIN_STREAK = "win_streak"
        const val KEY_BEST_WIN_STREAK = "best_win_streak"
        const val KEY_LONGEST_WORD = "longest_word"
        const val KEY_MOOD = "mood"
        const val KEY_FIRST_DAY = "first_day"
        const val KEY_SKIN = "skin"
        const val MAX_BOND = 1_000
    }
}

/** Turkish/English line pairs. "%s" is replaced with the player's first name when it is known. */
internal object WordSiegeMascotLines {
    val greetFirst = listOf(
        "Merhaba! Ben senin oyun arkadaşınım. Birlikte kazanalım!" to "Hi! I'm your game buddy. Let's win together!",
        "Selam %s! Seninle oynamak için sabırsızlanıyordum." to "Hi %s! I couldn't wait to play with you.",
    )
    val greet = listOf(
        "Yine geldin! Hazır mısın?" to "You're back! Ready?",
        "Hadi bakalım, güzel kelimeler bekliyorum." to "Let's go, I'm expecting great words.",
        "Tahtayı ısıttım, başlayabiliriz!" to "Board's warmed up, let's start!",
    )
    val greetFriend = listOf(
        "%s! Seni görmek güzel." to "%s! Good to see you.",
        "Ekip yine bir arada!" to "The team is back together!",
        "Bugün şanslı günümüz, hissediyorum." to "I feel it, today's our lucky day.",
    )
    val greetBest = listOf(
        "En iyi arkadaşım geldi! ✨" to "My best friend is here! ✨",
        "%s, seninle her maç bir macera." to "%s, every match with you is an adventure.",
    )
    val missed = listOf(
        "Seni özledim %s! Neredeydin?" to "I missed you, %s! Where have you been?",
        "Uzun zaman oldu! Kaldığımız yerden devam." to "It's been a while! Let's pick up where we left off.",
    )
    val praise = listOf(
        "Güzel hamle!" to "Nice move!",
        "Bunu sevdim!" to "I love that!",
        "Harika gidiyorsun!" to "You're doing great!",
        "İşte bu!" to "That's it!",
        "Tam isabet!" to "Spot on!",
    )
    val bigPraise = listOf(
        "Vay canına, muhteşem!" to "Wow, magnificent!",
        "Bu kelime efsane! ✨" to "That word is legendary! ✨",
        "Rakip şokta!" to "The rival is in shock!",
        "Sen bir dâhisin %s!" to "You're a genius, %s!",
    )
    val comfort = listOf(
        "Olsun, bir sonrakinde!" to "No worries, next one!",
        "Sorun yok, yeniden dene." to "It's okay, try again.",
        "Hata yapmak da oyunun parçası." to "Mistakes are part of the game.",
        "Derin bir nefes… sen yaparsın." to "Deep breath… you've got this.",
    )
    val critical = listOf(
        "Hızlı ol, yapabilirsin!" to "Quick, you can do it!",
        "Son saniyeler, odaklan!" to "Last seconds, focus!",
        "Kısa bir kelime de iş görür!" to "A short word works too!",
        "Sakin ol, aklına gelecek!" to "Stay calm, it'll come to you!",
    )
    val behind = listOf(
        "Daha bitmedi, geri döneriz!" to "It's not over, we'll come back!",
        "Pes etmek yok, az kaldı!" to "No giving up, we're close!",
        "Tek büyük hamle her şeyi değiştirir." to "One big move changes everything.",
    )
    val rivalStrong = listOf(
        "Rakip iyi oynadı ama biz daha iyisini yaparız." to "The rival played well, but we'll do better.",
        "Üzülme, sıradaki hamle bizim!" to "Don't worry, the next move is ours!",
        "Bu can yaktı… ama toparlarız." to "That hurt… but we'll recover.",
    )
    val ahead = listOf(
        "Öndeyiz, böyle devam!" to "We're ahead, keep it up!",
        "Rakip bize yetişemiyor!" to "The rival can't keep up!",
    )
    val chat = listOf(
        "Hmm… tahtayı izliyorum 👀" to "Hmm… I'm watching the board 👀",
        "Sence rakip ne yapacak?" to "What do you think the rival will do?",
        "Güzel harfler var gibi…" to "Looks like there are nice letters…",
        "Işıldamak yorucu iş, biliyor musun?" to "Glowing is hard work, you know?",
    )
    val chatFriend = listOf(
        "Seninle oynamayı seviyorum." to "I love playing with you.",
        "%s, sana güveniyorum." to "%s, I believe in you.",
        "Birlikte çok iyi takımız." to "We make a great team.",
    )
    val tap = listOf(
        "Hihi, gıdıklanıyorum!" to "Hehe, that tickles!",
        "Hop! Şuraya konayım." to "Whee! I'll perch over here.",
        "Beni yakalayamazsın!" to "You can't catch me!",
        "Wiii!" to "Wheee!",
    )
    val poked = listOf(
        "Tamam tamam, gidiyorum!" to "Okay okay, I'm going!",
        "Hey! Başım döndü 😵" to "Hey! I'm dizzy 😵",
    )
    val twirl = listOf(
        "Tadaa! ✨" to "Ta-da! ✨",
        "Bir tur daha mı? 💫" to "One more spin? 💫",
    )
    val flip = listOf(
        "Dünya baş aşağı daha güzel!" to "The world looks better upside down!",
        "Beni böyle de sevdin mi? 🙃" to "Do you like me this way too? 🙃",
    )
    val win = listOf(
        "KAZANDIK! 🎉" to "WE WON! 🎉",
        "Şampiyon sensin %s!" to "You're the champion, %s!",
        "Bu zafer ikimizin! ✨" to "This victory is ours! ✨",
        "Muhteşem bir maçtı!" to "What a match!",
    )
    val loss = listOf(
        "Üzülme… bir dahaki sefere biz kazanacağız." to "Don't be sad… we'll win next time.",
        "İyi mücadeleydi, seninle gurur duyuyorum." to "Good fight, I'm proud of you.",
        "Yanındayım, rövanşı alırız!" to "I'm with you, we'll get our revenge!",
    )
    val morning = listOf(
        "Günaydın %s! ☀️ Güne bir zaferle başlayalım." to "Good morning, %s! ☀️ Let's start the day with a win.",
        "Günaydın! Kahveni aldıysan başlayalım ☕" to "Good morning! Got your coffee? Let's go ☕",
    )
    val night = listOf(
        "Geç oldu… bir maç daha, sonra uyu 🌙" to "It's late… one more match, then sleep 🌙",
        "Gece kuşu %s! Ben de uykusuzum 🦉" to "Night owl %s! I can't sleep either 🦉",
    )
    val milestone = listOf(
        "Bu bizim %n. buluşmamız! 🎉" to "This is our meeting number %n! 🎉",
        "%n kez birlikte oynadık, harikasın! ✨" to "We've played together %n times, you're amazing! ✨",
    )
    val anniversary = listOf(
        "Bugün tanışmamızın yıldönümü! 🎂" to "Today is our anniversary! 🎂",
    )
    val memoryLongest = listOf(
        "En uzun kelimen hâlâ %w, rekoru kırar mısın?" to "Your longest word is still %w. Can you beat it?",
        "%w kelimesini hiç unutmayacağım!" to "I'll never forget the word %w!",
    )
    val memoryWins = listOf(
        "Birlikte %n maç kazandık!" to "We've won %n matches together!",
        "Tam %n galibiyetimiz var, devam!" to "We have %n wins, let's keep going!",
    )
    val memoryStreak = listOf(
        "Üst üste %n galibiyet! Seriyi bozmayalım 🔥" to "%n wins in a row! Let's keep the streak 🔥",
    )
    val sadMood = listOf(
        "Son maçlar zor geçti… ama birlikte toparlarız." to "The last matches were tough… but we'll bounce back together.",
    )
    val happyMood = listOf(
        "Bugün çok iyi hissediyorum! ✨" to "I feel great today! ✨",
    )
    val streak = listOf(
        "%n'lü seri! 🔥" to "%n in a row! 🔥",
        "Durdurulamıyorsun! 🔥" to "You're unstoppable! 🔥",
        "Seri devam ediyor, harikasın!" to "The streak goes on, amazing!",
    )
    val streakLost = listOf(
        "Seri bitti ama sorun yok, yenisini yaparız." to "The streak ended, no worries, we'll start a new one.",
        "Olsun! Yeni seri seni bekliyor." to "That's okay! A new streak awaits.",
    )
    val rareWord = listOf(
        "Bu kelimeyi nereden buldun?! 😲" to "Where did you find that word?! 😲",
        "Vay, %w! Sözlük gibisin." to "Wow, %w! You're a walking dictionary.",
        "Böyle uzun kelime görmemiştim!" to "I've never seen such a long word!",
    )
    val capture = listOf(
        "Burası artık bizim! 🏰" to "This is ours now! 🏰",
        "Bayrağı diktik! 🚩" to "Flag planted! 🚩",
    )
    val hint = listOf(
        "Şuradaki bonus kareye bir bak 👀" to "Take a look at this bonus square 👀",
        "Burası puanı katlayabilir…" to "This spot could multiply your points…",
    )
    val chaseCatch = listOf(
        "Yakaladım! ✨" to "Got it! ✨",
        "Hop! Bir harf daha bizim." to "Whee! Another letter for us.",
    )
    val skinPink = listOf(
        "Pembe hâlim nasıl? 💖" to "How do you like me in pink? 💖",
        "Kurdelemi taktım! 🎀" to "I put on my bow! 🎀",
    )
    val skinOrb = listOf(
        "Klasik hâlime döndüm ✨" to "Back to my classic look ✨",
    )
    val draw = listOf(
        "Berabere! Ne çekişmeydi!" to "A draw! What a battle!",
        "Kıl payı… rövanşa ne dersin?" to "So close… rematch?",
    )
}

internal enum class WordSiegeMascotIdle { WATCH, LOOK_AROUND, NOD, HOP, SPARKLE, TWIRL, FLIP, CHAT, WANDER, PEEK, CHASE }

/**
 * The mascot's decision making: weighted choices with cooldowns and a short memory of what it did
 * and said recently, so it rarely repeats itself and often just quietly watches the game.
 */
internal class WordSiegeMascotMind(private val random: Random = Random.Default) {
    private val lastUsed = HashMap<String, Long>()
    private val recentLines = ArrayDeque<String>()
    private val recentIdle = ArrayDeque<WordSiegeMascotIdle>()

    fun ready(key: String, cooldownMillis: Long, now: Long): Boolean =
        now - (lastUsed[key] ?: NEVER) >= cooldownMillis

    fun mark(key: String, now: Long) {
        lastUsed[key] = now
    }

    fun chance(probability: Float): Boolean = random.nextFloat() < probability

    fun nextIdleDelay(): Long = 8_000L + random.nextLong(10_000L)

    fun pickIdle(
        now: Long,
        bondLevel: Int,
        stayCalm: Boolean,
        rivalTurn: Boolean = false,
        playfulness: Float = 1f,
    ): WordSiegeMascotIdle {
        val options = if (stayCalm) {
            // The player is thinking or under pressure: do not distract.
            listOf(WordSiegeMascotIdle.WATCH to 8f, WordSiegeMascotIdle.NOD to .5f)
        } else {
            listOf(
                WordSiegeMascotIdle.WATCH to 6f,
                WordSiegeMascotIdle.LOOK_AROUND to 1.5f,
                WordSiegeMascotIdle.NOD to .7f,
                WordSiegeMascotIdle.HOP to .8f * playfulness,
                WordSiegeMascotIdle.SPARKLE to .8f,
                WordSiegeMascotIdle.TWIRL to .5f * playfulness,
                WordSiegeMascotIdle.FLIP to .28f * playfulness,
                WordSiegeMascotIdle.CHAT to (.55f + .25f * bondLevel) * playfulness,
                WordSiegeMascotIdle.WANDER to .3f,
                // Curious about the rival's move: stretches up to peek.
                WordSiegeMascotIdle.PEEK to (if (rivalTurn) 1.3f else 0f),
                WordSiegeMascotIdle.CHASE to .22f * playfulness,
            )
        }
        val weighted = options.map { (idle, weight) ->
            val w = when {
                !ready("idle:$idle", cooldown(idle), now) -> 0f
                idle != WordSiegeMascotIdle.WATCH && idle in recentIdle -> weight * .15f
                else -> weight
            }
            idle to w
        }
        val total = weighted.sumOf { it.second.toDouble() }.toFloat()
        var roll = random.nextFloat() * total
        var chosen = WordSiegeMascotIdle.WATCH
        for ((idle, weight) in weighted) {
            if (weight <= 0f) continue
            if (roll < weight) {
                chosen = idle
                break
            }
            roll -= weight
        }
        mark("idle:$chosen", now)
        recentIdle.addLast(chosen)
        while (recentIdle.size > 3) recentIdle.removeFirst()
        return chosen
    }

    private fun cooldown(idle: WordSiegeMascotIdle): Long = when (idle) {
        WordSiegeMascotIdle.WATCH -> 0L
        WordSiegeMascotIdle.LOOK_AROUND -> 25_000L
        WordSiegeMascotIdle.NOD -> 30_000L
        WordSiegeMascotIdle.HOP -> 50_000L
        WordSiegeMascotIdle.SPARKLE -> 40_000L
        WordSiegeMascotIdle.TWIRL -> 200_000L
        WordSiegeMascotIdle.FLIP -> 360_000L
        WordSiegeMascotIdle.CHAT -> 60_000L
        WordSiegeMascotIdle.WANDER -> 240_000L
        WordSiegeMascotIdle.PEEK -> 40_000L
        WordSiegeMascotIdle.CHASE -> 420_000L
    }

    /** Picks a line it has not said recently. */
    fun line(options: List<Pair<String, String>>, name: String?, number: Int = 0, word: String = ""): String {
        val usable = options.filter { name != null || !it.first.contains("%s") }.ifEmpty { options }
        val fresh = usable.filter { it.first !in recentLines }.ifEmpty { usable }
        val chosen = fresh[random.nextInt(fresh.size)]
        recentLines.addLast(chosen.first)
        while (recentLines.size > 12) recentLines.removeFirst()
        val text = sh(chosen.first, chosen.second)
        return text.replace("%s", name ?: "").replace("%n", number.toString()).replace("%w", word)
            .replace(" !", "!").replace(" ,", ",").trim()
    }

    private companion object {
        const val NEVER = Long.MIN_VALUE / 2
    }
}

private const val STAGE = -1
private const val VISIT = -2

/** A drifting letter the mascot sometimes chases for fun. Positions are area fractions. */
private data class WordSiegeMascotChase(val id: Int, val letter: String, val start: Offset, val end: Offset)

/**
 * A living companion layer: place it over a game area with `Modifier.matchParentSize()`.
 * The mascot flies in, perches on one of [anchors] (fractions of the area, 0..1, of its centre),
 * flies to another perch when tapped, celebrates a win on centre stage, comforts after a loss and
 * sometimes speaks in a bubble. Touches outside the mascot pass through to the game; with
 * [touches] (see [wordSiegeMascotTouchWatcher]) it glances wherever the player touches.
 */
@Composable
internal fun WordSiegeMascotCompanion(
    anchors: List<Offset>,
    mascotSize: Dp,
    moveId: Long?,
    lastMoveMine: Boolean,
    playerTurn: Boolean,
    modifier: Modifier = Modifier,
    moveScore: Int = 0,
    capturedCells: Int = 0,
    opponentCaptured: Int = 0,
    moveCell: Int? = null,
    pendingCells: Collection<Int> = emptyList(),
    requestedEmotion: WordSiegeMascotEmotion? = null,
    urgency: Float = 0f,
    momentum: Float = 0f,
    idleGazeX: Float = 0f,
    idleGazeY: Float = .2f,
    typingKey: Int = 0,
    signal: WordSiegeMascotSignal? = null,
    outcome: WordSiegeMascotOutcome? = null,
    playerName: String? = null,
    greet: Boolean = true,
    stageY: Float = .42f,
    celebrationScale: Float = 1.7f,
    touches: WordSiegeMascotTouchState? = null,
    visit: WordSiegeMascotVisit? = null,
    /** The player's profile gender; picks the pink girl look by default for female players. */
    playerGender: String? = null,
) {
    if (anchors.isEmpty()) return
    val context = LocalContext.current
    val bond = remember { WordSiegeMascotBond(context) }
    val mind = remember { WordSiegeMascotMind() }
    val scope = rememberCoroutineScope()
    val firstName = playerName?.trim()?.split(' ')?.firstOrNull()?.takeIf { it.isNotBlank() && it.length <= 14 }

    var anchorIndex by remember { mutableIntStateOf(0) }
    var homeIndex by remember { mutableIntStateOf(0) }
    var visitPoint by remember { mutableStateOf(Offset(.5f, .5f)) }
    var fromPosition by remember { mutableStateOf(Offset.Unspecified) }
    val flight = remember { Animatable(0f) }
    var flying by remember { mutableStateOf(false) }
    var flightId by remember { mutableIntStateOf(0) }
    val scale = remember { Animatable(1f) }
    var speech by remember { mutableStateOf<String?>(null) }
    var speechId by remember { mutableIntStateOf(0) }
    var talking by remember { mutableStateOf(false) }
    var speechJob by remember { mutableStateOf<Job?>(null) }
    // The current multi-step trip (visit, celebration, escape); a new one replaces the old.
    var tripJob by remember { mutableStateOf<Job?>(null) }
    var actionKey by remember { mutableLongStateOf(0L) }
    var action by remember { mutableStateOf<WordSiegeMascotAction?>(null) }
    var watching by remember { mutableStateOf(true) }
    var stageEmotion by remember { mutableStateOf<WordSiegeMascotEmotion?>(null) }
    var busy by remember { mutableStateOf(false) }
    var lastTypingAt by remember { mutableLongStateOf(0L) }
    var lastInteractionAt by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    var glanceKey by remember { mutableIntStateOf(0) }
    var glance by remember { mutableStateOf(Offset.Zero) }
    var lastEscapeAt by remember { mutableLongStateOf(0L) }
    var chase by remember { mutableStateOf<WordSiegeMascotChase?>(null) }
    val chaseProgress = remember { Animatable(0f) }
    val tapTimes = remember { ArrayDeque<Long>() }
    var tapBond by remember { mutableIntStateOf(0) }
    val initialSignalKey = remember { signal?.key }
    val initialVisitKey = remember { visit?.key }
    val initialOutcome = remember { outcome }
    // A win earns a party hat (or the purchased victory crown) for the rest of the screen.
    val hat = when {
        outcome != WordSiegeMascotOutcome.WIN -> WordSiegeMascotHat.NONE
        SonHarfCosmetics.crownVictory -> WordSiegeMascotHat.CROWN
        else -> WordSiegeMascotHat.PARTY
    }

    // Long-pressing the mascot switches between the blue orb and the pink girl; the choice is kept.
    var skinChoice by remember { mutableStateOf(bond.skinChoice) }
    val skin = skinChoice ?: if (playerGender?.trim()?.lowercase() in FEMALE_GENDERS) {
        WordSiegeMascotSkin.PINK
    } else {
        WordSiegeMascotSkin.ORB
    }

    val currentUrgency by rememberUpdatedState(urgency)
    val currentPendingCount by rememberUpdatedState(pendingCells.size)
    val currentAnchors by rememberUpdatedState(anchors)
    val currentName by rememberUpdatedState(firstName)
    val currentPlayerTurn by rememberUpdatedState(playerTurn)

    LaunchedEffect(typingKey) {
        lastTypingAt = SystemClock.uptimeMillis()
        lastInteractionAt = lastTypingAt
    }
    LaunchedEffect(moveId, pendingCells.size) { lastInteractionAt = SystemClock.uptimeMillis() }

    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val areaWidth = constraints.maxWidth.toFloat()
        val areaHeight = constraints.maxHeight.toFloat()
        val baseSizePx = with(density) { mascotSize.toPx() }
        val marginPx = with(density) { 4.dp.toPx() }
        val area by rememberUpdatedState(Size(areaWidth, areaHeight))

        fun anchorCenter(index: Int, sizePx: Float): Offset {
            val w = area.width
            val h = area.height
            val raw = when (index) {
                STAGE -> Offset(w / 2f, h * stageY)
                VISIT -> Offset(visitPoint.x * w, visitPoint.y * h)
                else -> {
                    val anchor = currentAnchors[index.coerceIn(0, currentAnchors.lastIndex)]
                    Offset(anchor.x * w, anchor.y * h)
                }
            }
            val half = sizePx / 2f + marginPx
            return Offset(
                raw.x.coerceIn(half, max(half, w - half)),
                raw.y.coerceIn(half, max(half, h - half)),
            )
        }

        fun currentCenter(): Offset {
            val sizePx = baseSizePx * scale.value
            val target = anchorCenter(anchorIndex, sizePx)
            val start = if (fromPosition.isSpecified) fromPosition else Offset(area.width + baseSizePx, -baseSizePx)
            val t = flight.value
            if (t >= 1f) return target
            // A curved flight path that rises above both ends, like a real hop through the air.
            val lift = max(abs(target.x - start.x) * .25f, baseSizePx * .6f)
            val control = Offset((start.x + target.x) / 2f, min(start.y, target.y) - lift)
            val u = 1f - t
            val half = baseSizePx * scale.value / 2f
            val x = u * u * start.x + 2f * u * t * control.x + t * t * target.x
            val y = u * u * start.y + 2f * u * t * control.y + t * t * target.y
            // Only the entrance may start outside; once inside, the flight stays within the area.
            return if (!fromPosition.isSpecified) Offset(x, y) else Offset(
                x.coerceIn(half, max(half, area.width - half)),
                y.coerceIn(half, max(half, area.height - half)),
            )
        }

        fun perform(next: WordSiegeMascotAction) {
            action = next
            actionKey += 1L
        }

        /** Makes the eyes dart toward a point of the area (in pixels). */
        fun lookAt(point: Offset) {
            val here = currentCenter()
            glance = Offset(
                ((point.x - here.x) / (area.width * .45f)).coerceIn(-1f, 1f),
                ((point.y - here.y) / (area.height * .45f)).coerceIn(-1f, 1f),
            )
            glanceKey += 1
        }

        fun say(text: String, holdExtraMillis: Long = 0L) {
            speechJob?.cancel()
            speechId += 1
            speech = text
            speechJob = scope.launch {
                talking = true
                delay(text.length * 34L + 250L)
                talking = false
                delay((1_500L + text.length * 30L + holdExtraMillis).coerceAtMost(5_500L))
                speech = null
            }
        }

        suspend fun flyTo(index: Int, durationMillis: Int) {
            fromPosition = currentCenter()
            anchorIndex = index
            if (index >= 0) homeIndex = index
            val id = ++flightId
            flying = true
            try {
                flight.snapTo(0f)
                flight.animateTo(1f, tween(durationMillis, easing = FastOutSlowInEasing))
                perform(WordSiegeMascotAction.LAND)
            } finally {
                // A newer flight may have taken over; otherwise never stay stuck "in the air".
                if (id == flightId) flying = false
            }
        }

        /**
         * Starts a multi-step trip in the companion scope, replacing any trip in progress. Game
         * events (a new move, the result) never cut a trip short halfway and strand the mascot.
         */
        fun startTrip(block: suspend CoroutineScope.() -> Unit): Job {
            tripJob?.cancel()
            return scope.launch(block = block).also { tripJob = it }
        }

        /** Flies beside [point] (area fraction) so it does not hide what it is showing. */
        suspend fun flyBeside(point: Offset, durationMillis: Int, onTop: Boolean = false) {
            val sideX = if (point.x > .5f) -.65f else .65f
            visitPoint = if (onTop) point else Offset(
                point.x + sideX * baseSizePx / max(1f, area.width),
                point.y - .35f * baseSizePx / max(1f, area.height),
            )
            flyTo(VISIT, durationMillis)
            lookAt(Offset(point.x * area.width, point.y * area.height))
        }

        fun otherAnchor(): Int {
            val all = currentAnchors.indices.filter { it != anchorIndex }
            if (all.isEmpty()) return 0
            val here = currentCenter()
            // Prefer somewhere noticeably different, but not always the very farthest spot.
            val sorted = all.sortedByDescending {
                val p = anchorCenter(it, baseSizePx)
                abs(p.x - here.x) + abs(p.y - here.y)
            }
            return sorted[Random.nextInt(min(2, sorted.size))]
        }

        /** Flies to another perch because a finger came close. */
        fun moveOutOfTheWay(now: Long) {
            if (busy || flying || chase != null || now - lastEscapeAt < 1_500L) return
            lastEscapeAt = now
            startTrip { flyTo(otherAnchor(), 750) }
        }

        /** The player touched the mascot: a little reaction, then it flies to another perch. */
        fun touchedMascot(now: Long) {
            lastInteractionAt = now
            // The press and the click of one tap arrive separately; count them once.
            if (tapTimes.lastOrNull()?.let { now - it < 350L } != true) tapTimes.addLast(now)
            while (tapTimes.size > 4) tapTimes.removeFirst()
            if (busy || flying || chase != null || now - lastEscapeAt < 400L) return
            lastEscapeAt = now
            if (tapBond < 5) {
                tapBond += 1
                bond.add(1)
            }
            val poked = tapTimes.size >= 3 && now - tapTimes[tapTimes.size - 3] < 2_500L
            startTrip {
                when {
                    poked && mind.chance(.8f) -> say(mind.line(WordSiegeMascotLines.poked, currentName))
                    mind.ready("talk:tap", 12_000L, now) && mind.chance(.35f) -> {
                        mind.mark("talk:tap", now)
                        say(mind.line(WordSiegeMascotLines.tap, currentName))
                    }
                }
                delay(150L)
                flyTo(otherAnchor(), 800)
            }
        }

        fun memoryLine(): String? {
            val options = buildList {
                if (bond.longestWord.length >= 5) add(mind.line(WordSiegeMascotLines.memoryLongest, currentName, word = bond.longestWord))
                if (bond.wins >= 3) add(mind.line(WordSiegeMascotLines.memoryWins, currentName, number = bond.wins))
                if (bond.winStreak >= 2) add(mind.line(WordSiegeMascotLines.memoryStreak, currentName, number = bond.winStreak))
                if (bond.mood <= -3) add(mind.line(WordSiegeMascotLines.sadMood, currentName))
                if (bond.mood >= 3) add(mind.line(WordSiegeMascotLines.happyMood, currentName))
            }
            return options.randomOrNull()
        }

        // Life loop: arrive, greet, then mostly watch with an occasional, never-repeating gesture.
        LaunchedEffect(Unit) {
            delay(250L)
            if (initialOutcome == null) startTrip { flyTo(0, 1_000) }.join()
            if (greet && initialOutcome == null) {
                val (meetings, daysAway) = bond.meet()
                delay(350L)
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                val together = bond.daysTogether()
                when {
                    meetings in MILESTONES -> {
                        perform(WordSiegeMascotAction.CHEER)
                        say(mind.line(WordSiegeMascotLines.milestone, currentName, number = meetings), holdExtraMillis = 800L)
                    }
                    together > 0L && together % 365L == 0L -> {
                        perform(WordSiegeMascotAction.SPARKLE)
                        say(mind.line(WordSiegeMascotLines.anniversary, currentName), holdExtraMillis = 800L)
                    }
                    meetings <= 1 -> say(mind.line(WordSiegeMascotLines.greetFirst, currentName))
                    daysAway >= 3L -> say(mind.line(WordSiegeMascotLines.missed, currentName))
                    mind.chance(.7f) -> {
                        val lines = when {
                            hour in 5..10 && mind.chance(.6f) -> WordSiegeMascotLines.morning
                            (hour >= 23 || hour < 5) && mind.chance(.7f) -> WordSiegeMascotLines.night
                            bond.level >= 2 -> WordSiegeMascotLines.greetBest
                            bond.level == 1 -> WordSiegeMascotLines.greetFriend
                            else -> WordSiegeMascotLines.greet
                        }
                        say(mind.line(lines, currentName))
                    }
                }
            }
            while (true) {
                // After losses it is a little quieter; after wins a little livelier.
                val moodFactor = 1f + bond.mood * .08f
                delay((mind.nextIdleDelay() / moodFactor.coerceIn(.6f, 1.4f)).toLong())
                if (busy || flying || speech != null || chase != null) continue
                val now = SystemClock.uptimeMillis()
                // Nobody around for a while: just keep watching (the rig dozes off by itself).
                if (now - lastInteractionAt > 40_000L) {
                    watching = true
                    continue
                }
                val stayCalm = currentUrgency > .3f || currentPendingCount > 0 || now - lastTypingAt < 3_000L
                val choice = mind.pickIdle(now, bond.level, stayCalm, rivalTurn = !currentPlayerTurn, playfulness = moodFactor)
                when (choice) {
                    WordSiegeMascotIdle.WATCH -> watching = true
                    WordSiegeMascotIdle.LOOK_AROUND -> {
                        watching = false
                        perform(WordSiegeMascotAction.LOOK_AROUND)
                    }
                    WordSiegeMascotIdle.NOD -> perform(WordSiegeMascotAction.NOD)
                    WordSiegeMascotIdle.HOP -> {
                        watching = false
                        perform(WordSiegeMascotAction.HOP)
                    }
                    WordSiegeMascotIdle.SPARKLE -> perform(WordSiegeMascotAction.SPARKLE)
                    WordSiegeMascotIdle.TWIRL -> {
                        watching = false
                        perform(WordSiegeMascotAction.TWIRL)
                        if (mind.chance(.3f)) {
                            delay(900L)
                            say(mind.line(WordSiegeMascotLines.twirl, currentName))
                        }
                    }
                    WordSiegeMascotIdle.FLIP -> {
                        watching = false
                        perform(WordSiegeMascotAction.FLIP)
                        if (mind.chance(.4f)) {
                            delay(700L)
                            say(mind.line(WordSiegeMascotLines.flip, currentName))
                        }
                    }
                    WordSiegeMascotIdle.CHAT -> {
                        val memory = if (mind.chance(.35f)) memoryLine() else null
                        val lines = if (bond.level >= 1 && mind.chance(.5f)) WordSiegeMascotLines.chatFriend else WordSiegeMascotLines.chat
                        say(memory ?: mind.line(lines, currentName))
                    }
                    WordSiegeMascotIdle.WANDER -> {
                        watching = false
                        if (currentAnchors.size > 1) startTrip { flyTo(otherAnchor(), 1_100) }.join()
                    }
                    WordSiegeMascotIdle.PEEK -> {
                        watching = false
                        perform(WordSiegeMascotAction.PEEK)
                    }
                    WordSiegeMascotIdle.CHASE -> {
                        // A glowing letter drifts by; it follows it with its eyes, then catches it.
                        watching = false
                        val fromLeft = Random.nextBoolean()
                        val letters = if (SonHarfUiState.language == "en") "ABCDEFGHIJKLMNOPRSTUVWYZ" else "ABCÇDEFGĞHIİKLMNOÖPRSŞTUÜVYZ"
                        val next = WordSiegeMascotChase(
                            id = Random.nextInt(),
                            letter = letters[Random.nextInt(letters.length)].toString(),
                            start = Offset(if (fromLeft) -.05f else 1.05f, .15f + Random.nextFloat() * .35f),
                            end = Offset(if (fromLeft) .7f else .3f, .2f + Random.nextFloat() * .4f),
                        )
                        startTrip {
                            try {
                                chase = next
                                chaseProgress.snapTo(0f)
                                launch { chaseProgress.animateTo(1f, tween(2_700, easing = LinearEasing)) }
                                repeat(9) {
                                    lookAt(chasePosition(next, chaseProgress.value, area))
                                    delay(180L)
                                }
                                val home = homeIndex
                                flyBeside(next.end, 1_000, onTop = true)
                                chase = null
                                perform(WordSiegeMascotAction.SPARKLE)
                                if (mind.chance(.6f)) say(mind.line(WordSiegeMascotLines.chaseCatch, currentName))
                                delay(1_100L)
                                flyTo(home, 1_000)
                            } finally {
                                chase = null
                            }
                        }.join()
                    }
                }
                if (choice != WordSiegeMascotIdle.WATCH) {
                    delay(2_500L)
                    watching = true
                }
            }
        }

        // Game moments: support, praise and comfort, chosen with restraint.
        LaunchedEffect(signal?.key) {
            val current = signal ?: return@LaunchedEffect
            if (current.key == initialSignalKey || busy) return@LaunchedEffect
            val now = SystemClock.uptimeMillis()
            lastInteractionAt = now
            if (current.word.isNotBlank()) bond.recordWord(current.word)
            fun speakIf(topic: String, cooldown: Long, probability: Float, lines: List<Pair<String, String>>) {
                // Apart from real emergencies it keeps long quiet stretches between remarks.
                val urgent = topic == "critical"
                if (!urgent && !mind.ready("talk:any", 40_000L, now)) return
                if (mind.ready("talk:$topic", cooldown, now) && mind.chance(probability)) {
                    mind.mark("talk:$topic", now)
                    mind.mark("talk:any", now)
                    say(mind.line(lines, currentName, number = current.count, word = current.word))
                }
            }
            when (current.event) {
                WordSiegeMascotEvent.PRAISE -> speakIf("praise", 60_000L, .15f, WordSiegeMascotLines.praise)
                WordSiegeMascotEvent.BIG_PRAISE -> {
                    delay(700L)
                    if (mind.ready("act:praise", 30_000L, now)) {
                        mind.mark("act:praise", now)
                        perform(WordSiegeMascotAction.SPARKLE)
                    }
                    speakIf("praise", 30_000L, .5f, WordSiegeMascotLines.bigPraise)
                }
                WordSiegeMascotEvent.RARE_WORD -> {
                    delay(500L)
                    perform(WordSiegeMascotAction.SPARKLE)
                    speakIf("rare", 20_000L, .85f, WordSiegeMascotLines.rareWord)
                }
                WordSiegeMascotEvent.COMFORT -> speakIf("comfort", 45_000L, .4f, WordSiegeMascotLines.comfort)
                WordSiegeMascotEvent.CRITICAL -> {
                    watching = false
                    speakIf("critical", 25_000L, 1f, WordSiegeMascotLines.critical)
                }
                WordSiegeMascotEvent.BEHIND -> speakIf("behind", 70_000L, .55f, WordSiegeMascotLines.behind)
                WordSiegeMascotEvent.RIVAL_STRONG -> {
                    // Let the sad reaction play first, then lift the player's spirits.
                    delay(1_100L)
                    speakIf("rival", 60_000L, .4f, WordSiegeMascotLines.rivalStrong)
                }
                WordSiegeMascotEvent.AHEAD -> {
                    if (mind.ready("talk:ahead", 90_000L, now) && mind.chance(.35f)) perform(WordSiegeMascotAction.NOD)
                    speakIf("ahead", 90_000L, .35f, WordSiegeMascotLines.ahead)
                }
                WordSiegeMascotEvent.STREAK -> {
                    // Each step of a streak is celebrated a little bigger.
                    delay(600L)
                    if (mind.ready("act:streak", 25_000L, now) || current.count >= 5) {
                        mind.mark("act:streak", now)
                        perform(if (current.count >= 5) WordSiegeMascotAction.CHEER else WordSiegeMascotAction.HOP)
                    }
                    speakIf("streak", 30_000L, if (current.count >= 5) .8f else .4f, WordSiegeMascotLines.streak)
                }
                WordSiegeMascotEvent.STREAK_LOST -> {
                    perform(WordSiegeMascotAction.SHRUG)
                    speakIf("streak-lost", 30_000L, .6f, WordSiegeMascotLines.streakLost)
                }
            }
        }

        // Places worth a visit: freshly won territory, or (practice) a bonus square as a hint.
        // The trip runs in the companion's own scope so a new move cannot cut it short midway.
        LaunchedEffect(visit?.key) {
            val current = visit ?: return@LaunchedEffect
            if (current.key == initialVisitKey) return@LaunchedEffect
            when (current.kind) {
                WordSiegeMascotVisitKind.CAPTURE -> {
                    delay(900L)
                    val now = SystemClock.uptimeMillis()
                    if (busy || flying || !mind.ready("visit:capture", 60_000L, now) || !mind.chance(.4f)) return@LaunchedEffect
                    mind.mark("visit:capture", now)
                    startTrip {
                        val home = homeIndex
                        flyBeside(current.point, 1_000)
                        perform(WordSiegeMascotAction.NOD)
                        if (mind.chance(.5f)) say(mind.line(WordSiegeMascotLines.capture, currentName))
                        delay(1_200L)
                        flyTo(home, 1_000)
                    }
                }
                WordSiegeMascotVisitKind.HINT -> {
                    // Only when the player seems stuck: their turn, nothing placed, no touches for a while.
                    delay(12_000L)
                    val now = SystemClock.uptimeMillis()
                    if (busy || flying || !currentPlayerTurn || currentPendingCount > 0) return@LaunchedEffect
                    if (now - lastInteractionAt < 10_000L || !mind.ready("visit:hint", 45_000L, now)) return@LaunchedEffect
                    mind.mark("visit:hint", now)
                    startTrip {
                        val home = homeIndex
                        flyBeside(current.point, 1_000)
                        perform(WordSiegeMascotAction.NOD)
                        say(mind.line(WordSiegeMascotLines.hint, currentName), holdExtraMillis = 1_000L)
                        delay(2_500L)
                        flyTo(home, 1_000)
                    }
                }
            }
        }

        // Touches on the game: glance at them. A finger on or near the mascot sends it flying
        // to another perch so it never stays in the player's way.
        LaunchedEffect(touches?.tick) {
            val touch = touches?.position ?: return@LaunchedEffect
            if (!touch.isSpecified) return@LaunchedEffect
            val now = SystemClock.uptimeMillis()
            lastInteractionAt = now
            if (busy) return@LaunchedEffect
            val here = currentCenter()
            val radius = baseSizePx * scale.value / 2f
            val distance = (touch - here).getDistance()
            when {
                distance <= radius -> Unit // Taps and long presses on the mascot are handled by the view.
                distance < radius * 1.9f -> moveOutOfTheWay(now)
                !flying -> lookAt(touch)
            }
        }
        LaunchedEffect(touches?.dragTick) {
            val drag = touches?.dragPosition ?: return@LaunchedEffect
            if (!drag.isSpecified || busy) return@LaunchedEffect
            val now = SystemClock.uptimeMillis()
            lastInteractionAt = now
            val here = currentCenter()
            val radius = baseSizePx * scale.value / 2f
            if ((drag - here).getDistance() < radius * 1.7f) moveOutOfTheWay(now)
        }

        // Match result: fly to centre stage, celebrate or grieve, then return to a perch.
        LaunchedEffect(outcome) {
            val result = outcome ?: return@LaunchedEffect
            // Runs in the companion scope so the celebration always finishes and lands on a perch.
            startTrip {
                busy = true
                try {
                    watching = false
                    chase = null
                    speechJob?.cancel()
                    speech = null
                    when (result) {
                        WordSiegeMascotOutcome.WIN -> {
                            bond.recordWin()
                            stageEmotion = WordSiegeMascotEmotion.EXCITED
                            launch { scale.animateTo(celebrationScale, tween(900)) }
                            flyTo(STAGE, 950)
                            perform(WordSiegeMascotAction.CHEER)
                            val line = if (bond.winStreak >= 2 && mind.chance(.6f)) {
                                mind.line(WordSiegeMascotLines.memoryStreak, currentName, number = bond.winStreak)
                            } else {
                                mind.line(WordSiegeMascotLines.win, currentName)
                            }
                            say(line, holdExtraMillis = 1_200L)
                            delay(1_700L)
                            perform(WordSiegeMascotAction.TWIRL)
                            delay(2_000L)
                            perform(WordSiegeMascotAction.HOP)
                            delay(1_000L)
                            if (mind.chance(.3f)) {
                                perform(WordSiegeMascotAction.FLIP)
                                delay(3_100L)
                            } else {
                                perform(WordSiegeMascotAction.SPARKLE)
                                delay(1_400L)
                            }
                        }
                        WordSiegeMascotOutcome.LOSS -> {
                            bond.recordLoss()
                            stageEmotion = WordSiegeMascotEmotion.TEARY
                            launch { scale.animateTo(1.3f, tween(900)) }
                            flyTo(STAGE, 1_100)
                            delay(700L)
                            say(mind.line(WordSiegeMascotLines.loss, currentName), holdExtraMillis = 1_500L)
                            delay(3_400L)
                            // After the tears, a brave little smile for the player.
                            stageEmotion = WordSiegeMascotEmotion.CALM
                            perform(WordSiegeMascotAction.NOD)
                            delay(1_300L)
                        }
                        WordSiegeMascotOutcome.DRAW -> {
                            stageEmotion = WordSiegeMascotEmotion.SURPRISED
                            launch { scale.animateTo(1.4f, tween(900)) }
                            flyTo(STAGE, 1_000)
                            say(mind.line(WordSiegeMascotLines.draw, currentName))
                            delay(1_200L)
                            stageEmotion = WordSiegeMascotEmotion.HAPPY
                            perform(WordSiegeMascotAction.HOP)
                            delay(2_000L)
                        }
                    }
                    stageEmotion = null
                    launch { scale.animateTo(1f, tween(800)) }
                    flyTo(0, 1_000)
                } finally {
                    stageEmotion = null
                    busy = false
                    watching = true
                    // If the celebration was cut short, still shrink back to normal size.
                    if (scale.targetValue != 1f) scope.launch { scale.animateTo(1f, tween(600)) }
                }
            }
        }

        val sizePx = baseSizePx * scale.value
        val center = currentCenter()
        val target = anchorCenter(anchorIndex, sizePx)
        val start = if (fromPosition.isSpecified) fromPosition else Offset(areaWidth + baseSizePx, -baseSizePx)
        val direction = if (flying) sign(target.x - start.x) else 0f

        chase?.let { current ->
            val p = chasePosition(current, chaseProgress.value, Size(areaWidth, areaHeight))
            val tilePx = with(density) { 30.dp.toPx() }
            Box(
                Modifier
                    .offset { IntOffset((p.x - tilePx / 2f).roundToInt(), (p.y - tilePx / 2f).roundToInt()) }
                    .requiredSize(30.dp)
                    .graphicsLayer {
                        rotationZ = kotlin.math.sin(chaseProgress.value * 12f) * 12f
                        alpha = min(1f, chaseProgress.value * 5f)
                    }
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(listOf(Color(0x99FFF4C2), Color.Transparent)),
                            radius = size.minDimension * .9f,
                        )
                        drawRoundRect(
                            brush = Brush.linearGradient(listOf(Color(0xFF8AF1FF), Color(0xFFB266F5))),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(current.letter, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
        }

        Box(
            Modifier
                .offset { IntOffset((center.x - sizePx / 2f).roundToInt(), (center.y - sizePx / 2f).roundToInt()) }
                .requiredSize(mascotSize * scale.value),
        ) {
            WordSiegeMascot(
                moveId = moveId,
                lastMoveMine = lastMoveMine,
                moveScore = moveScore,
                capturedCells = capturedCells,
                opponentCaptured = opponentCaptured,
                moveCell = moveCell,
                pendingCells = pendingCells,
                playerTurn = playerTurn,
                requestedEmotion = stageEmotion ?: requestedEmotion,
                modifier = Modifier.matchParentSize(),
                urgency = urgency,
                momentum = (momentum + bond.mood * .05f).coerceIn(-1f, 1f),
                idleGazeX = idleGazeX,
                idleGazeY = idleGazeY,
                typingKey = typingKey,
                actionKey = actionKey,
                action = action,
                flying = flying,
                flightDirection = direction,
                speaking = talking,
                watching = watching && !busy,
                glanceKey = glanceKey,
                glanceX = glance.x,
                glanceY = glance.y,
                hat = hat,
                skin = skin,
                onLongPress = {
                    val next = if (skin == WordSiegeMascotSkin.PINK) WordSiegeMascotSkin.ORB else WordSiegeMascotSkin.PINK
                    skinChoice = next
                    bond.skinChoice = next
                    perform(WordSiegeMascotAction.SPARKLE)
                    say(mind.line(if (next == WordSiegeMascotSkin.PINK) WordSiegeMascotLines.skinPink else WordSiegeMascotLines.skinOrb, currentName))
                },
                onTap = { touchedMascot(SystemClock.uptimeMillis()) },
            )
        }

        speech?.let { text ->
            WordSiegeMascotSpeechBubble(
                text = text,
                id = speechId,
                mascotCenter = center,
                mascotRadius = sizePx / 2f,
                areaWidth = areaWidth,
            )
        }
    }
}

private val MILESTONES = setOf(10, 25, 50, 100, 250, 500, 1_000)
private val FEMALE_GENDERS = setOf("kadın", "kadin", "female", "woman", "f")

/** Position (px) of a chased letter: a lazy drift with a gentle wave. */
private fun chasePosition(chase: WordSiegeMascotChase, t: Float, area: Size): Offset {
    val x = chase.start.x + (chase.end.x - chase.start.x) * t
    val y = chase.start.y + (chase.end.y - chase.start.y) * t + kotlin.math.sin(t * 3f * PI_F) * .05f
    return Offset(x * area.width, y * area.height)
}

private const val PI_F = 3.1415927f

/** Silent speech: a small comic bubble that types out what the mascot says. */
@Composable
private fun WordSiegeMascotSpeechBubble(
    text: String,
    id: Int,
    mascotCenter: Offset,
    mascotRadius: Float,
    areaWidth: Float,
) {
    val density = LocalDensity.current
    val appear = remember(id) { Animatable(0f) }
    var shown by remember(id) { mutableIntStateOf(0) }
    LaunchedEffect(id) {
        launch { appear.animateTo(1f, spring(dampingRatio = .55f, stiffness = 420f)) }
        for (i in 1..text.length) {
            shown = i
            delay(34L)
        }
    }
    val marginPx = with(density) { 6.dp.toPx() }
    val tailPx = with(density) { 9.dp.toPx() }
    val cornerPx = with(density) { 14.dp.toPx() }
    val maxWidthPx = min(areaWidth - marginPx * 2f, with(density) { 220.dp.toPx() }).roundToInt().coerceAtLeast(1)
    // Written during layout, read while drawing the tail in the same frame.
    val geometry = remember { FloatArray(2) }
    val border = Brush.linearGradient(listOf(Color(0xFF56E4F7), Color(0xFFB266F5)))

    Box(
        Modifier
            .layout { measurable, _ ->
                val placeable = measurable.measure(Constraints(maxWidth = maxWidthPx))
                val above = mascotCenter.y - mascotRadius - tailPx - placeable.height > marginPx
                val x = (mascotCenter.x - placeable.width / 2f)
                    .coerceIn(marginPx, max(marginPx, areaWidth - placeable.width - marginPx))
                val y = if (above) mascotCenter.y - mascotRadius * .85f - tailPx - placeable.height
                    else mascotCenter.y + mascotRadius * .85f + tailPx
                geometry[0] = (mascotCenter.x - x).coerceIn(cornerPx, max(cornerPx, placeable.width - cornerPx))
                geometry[1] = if (above) 1f else 0f
                layout(placeable.width, placeable.height) {
                    placeable.place(x.roundToInt(), y.roundToInt())
                }
            }
            .graphicsLayer {
                val progress = appear.value
                scaleX = .55f + .45f * progress
                scaleY = .55f + .45f * progress
                alpha = progress.coerceIn(0f, 1f)
                transformOrigin = TransformOrigin(
                    if (size.width > 0f) geometry[0] / size.width else .5f,
                    if (geometry[1] > .5f) 1f else 0f,
                )
            }
            .drawBehind {
                val tailX = geometry[0]
                val above = geometry[1] > .5f
                val tail = Path().apply {
                    if (above) {
                        moveTo(tailX - tailPx * .8f, size.height - 1f)
                        lineTo(tailX, size.height + tailPx)
                        lineTo(tailX + tailPx * .8f, size.height - 1f)
                    } else {
                        moveTo(tailX - tailPx * .8f, 1f)
                        lineTo(tailX, -tailPx)
                        lineTo(tailX + tailPx * .8f, 1f)
                    }
                    close()
                }
                drawPath(tail, Color.White)
                drawRoundRect(color = Color.White, cornerRadius = CornerRadius(cornerPx, cornerPx))
                drawRoundRect(
                    brush = border,
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
            .padding(horizontal = 11.dp, vertical = 7.dp),
    ) {
        // The full text is laid out from the start so the bubble does not grow while typing.
        Text(
            text = buildAnnotatedString {
                append(text.take(shown))
                withStyle(SpanStyle(color = Color.Transparent)) { append(text.drop(shown)) }
            },
            color = Color(0xFF1B2140),
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 3,
        )
    }
}
