package com.sonharf.game

import android.content.Context
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Game moments the companion may respond to. The mascot decides itself whether and how. */
internal enum class WordSiegeMascotEvent {
    PRAISE, BIG_PRAISE, COMFORT, CRITICAL, BEHIND, RIVAL_STRONG, AHEAD,
}

internal enum class WordSiegeMascotOutcome { WIN, LOSS, DRAW }

/** One occurrence of a game moment; a new [key] means a new occurrence. */
internal data class WordSiegeMascotSignal(val key: String, val event: WordSiegeMascotEvent)

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
        val today = System.currentTimeMillis() / 86_400_000L
        val lastDay = prefs.getLong(KEY_LAST_DAY, -1L)
        val meetings = prefs.getInt(KEY_MEETINGS, 0) + 1
        prefs.edit()
            .putInt(KEY_MEETINGS, meetings)
            .putLong(KEY_LAST_DAY, today)
            .putInt(KEY_BOND, min(MAX_BOND, points + 2))
            .apply()
        return meetings to if (lastDay < 0L) 0L else today - lastDay
    }

    fun add(amount: Int) {
        prefs.edit().putInt(KEY_BOND, (points + amount).coerceIn(0, MAX_BOND)).apply()
    }

    fun recordWin() {
        prefs.edit()
            .putInt(KEY_WINS, prefs.getInt(KEY_WINS, 0) + 1)
            .putInt(KEY_BOND, min(MAX_BOND, points + 6))
            .apply()
    }

    private companion object {
        const val FILE = "word_siege_mascot_bond"
        const val KEY_BOND = "bond"
        const val KEY_MEETINGS = "meetings"
        const val KEY_LAST_DAY = "last_day"
        const val KEY_WINS = "wins"
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
    val draw = listOf(
        "Berabere! Ne çekişmeydi!" to "A draw! What a battle!",
        "Kıl payı… rövanşa ne dersin?" to "So close… rematch?",
    )
}

internal enum class WordSiegeMascotIdle { WATCH, LOOK_AROUND, NOD, HOP, SPARKLE, TWIRL, FLIP, CHAT, WANDER }

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

    fun nextIdleDelay(): Long = 4_500L + random.nextLong(7_500L)

    fun pickIdle(now: Long, bondLevel: Int, stayCalm: Boolean): WordSiegeMascotIdle {
        val options = if (stayCalm) {
            // The player is thinking or under pressure: do not distract.
            listOf(WordSiegeMascotIdle.WATCH to 8f, WordSiegeMascotIdle.NOD to .5f)
        } else {
            listOf(
                WordSiegeMascotIdle.WATCH to 6f,
                WordSiegeMascotIdle.LOOK_AROUND to 1.5f,
                WordSiegeMascotIdle.NOD to .7f,
                WordSiegeMascotIdle.HOP to .8f,
                WordSiegeMascotIdle.SPARKLE to .8f,
                WordSiegeMascotIdle.TWIRL to .5f,
                WordSiegeMascotIdle.FLIP to .28f,
                WordSiegeMascotIdle.CHAT to .55f + .25f * bondLevel,
                WordSiegeMascotIdle.WANDER to .3f,
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
        WordSiegeMascotIdle.LOOK_AROUND -> 15_000L
        WordSiegeMascotIdle.NOD -> 20_000L
        WordSiegeMascotIdle.HOP -> 30_000L
        WordSiegeMascotIdle.SPARKLE -> 25_000L
        WordSiegeMascotIdle.TWIRL -> 75_000L
        WordSiegeMascotIdle.FLIP -> 140_000L
        WordSiegeMascotIdle.CHAT -> 50_000L
        WordSiegeMascotIdle.WANDER -> 120_000L
    }

    /** Picks a line it has not said recently. */
    fun line(options: List<Pair<String, String>>, name: String?): String {
        val usable = options.filter { name != null || !it.first.contains("%s") }.ifEmpty { options }
        val fresh = usable.filter { it.first !in recentLines }.ifEmpty { usable }
        val chosen = fresh[random.nextInt(fresh.size)]
        recentLines.addLast(chosen.first)
        while (recentLines.size > 12) recentLines.removeFirst()
        val text = sh(chosen.first, chosen.second)
        return text.replace("%s", name ?: "").replace(" !", "!").replace(" ,", ",").trim()
    }

    private companion object {
        const val NEVER = Long.MIN_VALUE / 2
    }
}

private const val STAGE = -1

/**
 * A living companion layer: place it over a game area with `Modifier.matchParentSize()`.
 * The mascot flies in, perches on one of [anchors] (fractions of the area, 0..1, of its centre),
 * flies to another perch when tapped, celebrates a win on centre stage, comforts after a loss and
 * sometimes speaks in a bubble. Touches outside the mascot pass through to the game.
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
) {
    if (anchors.isEmpty()) return
    val context = LocalContext.current
    val bond = remember { WordSiegeMascotBond(context) }
    val mind = remember { WordSiegeMascotMind() }
    val scope = rememberCoroutineScope()
    val firstName = playerName?.trim()?.split(' ')?.firstOrNull()?.takeIf { it.isNotBlank() && it.length <= 14 }

    var anchorIndex by remember { mutableIntStateOf(0) }
    var fromPosition by remember { mutableStateOf(Offset.Unspecified) }
    val flight = remember { Animatable(0f) }
    var flying by remember { mutableStateOf(false) }
    val scale = remember { Animatable(1f) }
    var speech by remember { mutableStateOf<String?>(null) }
    var speechId by remember { mutableIntStateOf(0) }
    var talking by remember { mutableStateOf(false) }
    var speechJob by remember { mutableStateOf<Job?>(null) }
    var actionKey by remember { mutableLongStateOf(0L) }
    var action by remember { mutableStateOf<WordSiegeMascotAction?>(null) }
    var watching by remember { mutableStateOf(true) }
    var stageEmotion by remember { mutableStateOf<WordSiegeMascotEmotion?>(null) }
    var busy by remember { mutableStateOf(false) }
    var lastTypingAt by remember { mutableLongStateOf(0L) }
    val tapTimes = remember { ArrayDeque<Long>() }
    var tapBond by remember { mutableIntStateOf(0) }
    val initialSignalKey = remember { signal?.key }
    val initialOutcome = remember { outcome }

    val currentUrgency by rememberUpdatedState(urgency)
    val currentPendingCount by rememberUpdatedState(pendingCells.size)
    val currentAnchors by rememberUpdatedState(anchors)
    val currentName by rememberUpdatedState(firstName)

    LaunchedEffect(typingKey) { lastTypingAt = SystemClock.uptimeMillis() }

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
            val raw = if (index == STAGE) {
                Offset(w / 2f, h * stageY)
            } else {
                val anchor = currentAnchors[index.coerceIn(0, currentAnchors.lastIndex)]
                Offset(anchor.x * w, anchor.y * h)
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
            val lift = max(abs(target.x - start.x) * .35f, baseSizePx * .9f)
            val control = Offset((start.x + target.x) / 2f, min(start.y, target.y) - lift)
            val u = 1f - t
            return Offset(
                u * u * start.x + 2f * u * t * control.x + t * t * target.x,
                u * u * start.y + 2f * u * t * control.y + t * t * target.y,
            )
        }

        fun perform(next: WordSiegeMascotAction) {
            action = next
            actionKey += 1L
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
            flying = true
            flight.snapTo(0f)
            flight.animateTo(1f, tween(durationMillis, easing = FastOutSlowInEasing))
            flying = false
            perform(WordSiegeMascotAction.LAND)
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

        // Life loop: arrive, greet, then mostly watch with an occasional, never-repeating gesture.
        LaunchedEffect(Unit) {
            delay(250L)
            if (initialOutcome == null) flyTo(0, 1_000)
            if (greet && initialOutcome == null) {
                val (meetings, daysAway) = bond.meet()
                delay(350L)
                val lines = when {
                    meetings <= 1 -> WordSiegeMascotLines.greetFirst
                    daysAway >= 3L -> WordSiegeMascotLines.missed
                    bond.level >= 2 -> WordSiegeMascotLines.greetBest
                    bond.level == 1 -> WordSiegeMascotLines.greetFriend
                    else -> WordSiegeMascotLines.greet
                }
                if (meetings <= 1 || mind.chance(.7f)) say(mind.line(lines, currentName))
            }
            while (true) {
                delay(mind.nextIdleDelay())
                if (busy || flying || speech != null) continue
                val now = SystemClock.uptimeMillis()
                val stayCalm = currentUrgency > .3f || currentPendingCount > 0 || now - lastTypingAt < 3_000L
                val choice = mind.pickIdle(now, bond.level, stayCalm)
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
                        val lines = if (bond.level >= 1 && mind.chance(.5f)) WordSiegeMascotLines.chatFriend else WordSiegeMascotLines.chat
                        say(mind.line(lines, currentName))
                    }
                    WordSiegeMascotIdle.WANDER -> {
                        watching = false
                        if (currentAnchors.size > 1) flyTo(otherAnchor(), 1_100)
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
            fun speakIf(topic: String, cooldown: Long, probability: Float, lines: List<Pair<String, String>>) {
                if (mind.ready("talk:$topic", cooldown, now) && mind.chance(probability)) {
                    mind.mark("talk:$topic", now)
                    say(mind.line(lines, currentName))
                }
            }
            when (current.event) {
                WordSiegeMascotEvent.PRAISE -> speakIf("praise", 18_000L, .35f, WordSiegeMascotLines.praise)
                WordSiegeMascotEvent.BIG_PRAISE -> {
                    delay(700L)
                    if (mind.ready("act:praise-twirl", 40_000L, now) && mind.chance(.4f)) {
                        mind.mark("act:praise-twirl", now)
                        perform(WordSiegeMascotAction.TWIRL)
                    } else {
                        perform(WordSiegeMascotAction.SPARKLE)
                    }
                    speakIf("praise", 10_000L, .75f, WordSiegeMascotLines.bigPraise)
                }
                WordSiegeMascotEvent.COMFORT -> speakIf("comfort", 14_000L, .6f, WordSiegeMascotLines.comfort)
                WordSiegeMascotEvent.CRITICAL -> {
                    watching = false
                    speakIf("critical", 25_000L, 1f, WordSiegeMascotLines.critical)
                }
                WordSiegeMascotEvent.BEHIND -> speakIf("behind", 70_000L, .55f, WordSiegeMascotLines.behind)
                WordSiegeMascotEvent.RIVAL_STRONG -> {
                    // Let the sad reaction play first, then lift the player's spirits.
                    delay(1_100L)
                    speakIf("rival", 30_000L, .6f, WordSiegeMascotLines.rivalStrong)
                }
                WordSiegeMascotEvent.AHEAD -> {
                    if (mind.ready("talk:ahead", 90_000L, now) && mind.chance(.35f)) perform(WordSiegeMascotAction.NOD)
                    speakIf("ahead", 90_000L, .35f, WordSiegeMascotLines.ahead)
                }
            }
        }

        // Match result: fly to centre stage, celebrate or grieve, then return to a perch.
        LaunchedEffect(outcome) {
            val result = outcome ?: return@LaunchedEffect
            busy = true
            watching = false
            speechJob?.cancel()
            speech = null
            when (result) {
                WordSiegeMascotOutcome.WIN -> {
                    bond.recordWin()
                    stageEmotion = WordSiegeMascotEmotion.EXCITED
                    launch { scale.animateTo(celebrationScale, tween(900)) }
                    flyTo(STAGE, 950)
                    perform(WordSiegeMascotAction.CHEER)
                    say(mind.line(WordSiegeMascotLines.win, currentName), holdExtraMillis = 1_200L)
                    delay(1_500L)
                    perform(WordSiegeMascotAction.TWIRL)
                    delay(1_300L)
                    perform(WordSiegeMascotAction.HOP)
                    delay(1_000L)
                    if (mind.chance(.5f)) {
                        perform(WordSiegeMascotAction.FLIP)
                        delay(2_300L)
                    } else {
                        perform(WordSiegeMascotAction.SPARKLE)
                        delay(1_400L)
                    }
                }
                WordSiegeMascotOutcome.LOSS -> {
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
            busy = false
            watching = true
        }

        val sizePx = baseSizePx * scale.value
        val center = currentCenter()
        val target = anchorCenter(anchorIndex, sizePx)
        val start = if (fromPosition.isSpecified) fromPosition else Offset(areaWidth + baseSizePx, -baseSizePx)
        val direction = if (flying) sign(target.x - start.x) else 0f

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
                momentum = momentum,
                idleGazeX = idleGazeX,
                idleGazeY = idleGazeY,
                typingKey = typingKey,
                actionKey = actionKey,
                action = action,
                flying = flying,
                flightDirection = direction,
                speaking = talking,
                watching = watching && !busy,
                onTap = {
                    val now = SystemClock.uptimeMillis()
                    tapTimes.addLast(now)
                    while (tapTimes.size > 4) tapTimes.removeFirst()
                    if (!busy && !flying) {
                        if (tapBond < 5) {
                            tapBond += 1
                            bond.add(1)
                        }
                        val poked = tapTimes.size >= 3 && now - tapTimes[tapTimes.size - 3] < 2_500L
                        scope.launch {
                            when {
                                poked && mind.chance(.8f) -> say(mind.line(WordSiegeMascotLines.poked, currentName))
                                mind.ready("talk:tap", 12_000L, now) && mind.chance(.35f) -> {
                                    mind.mark("talk:tap", now)
                                    say(mind.line(WordSiegeMascotLines.tap, currentName))
                                }
                            }
                            delay(260L)
                            flyTo(otherAnchor(), 850)
                            if (mind.ready("act:tap-twirl", 45_000L, now) && mind.chance(.15f)) {
                                mind.mark("act:tap-twirl", now)
                                delay(200L)
                                perform(WordSiegeMascotAction.TWIRL)
                            }
                        }
                    }
                },
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
