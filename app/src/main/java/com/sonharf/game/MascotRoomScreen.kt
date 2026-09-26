package com.sonharf.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sonharf.game.data.MascotRoomBackend
import com.sonharf.game.data.SupabaseProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/** A fruit the mascot can eat: the apple is free (3 a day), the magic ones cost Son Coin. */
private data class RoomFruit(val id: String, val emoji: String, val tr: String, val en: String, val price: Int)

private val RoomFruits = listOf(
    RoomFruit("lethara_apple", "🍎", "Elma", "Apple", 0),
    RoomFruit("moon_fruit", "🌙", "Ay meyvesi", "Moon fruit", 20),
    RoomFruit("star_fruit", "⭐", "Yıldız meyvesi", "Star fruit", 45),
    RoomFruit("seal_fruit", "💎", "Mühür meyvesi", "Seal fruit", 70),
)

private data class RoomFloat(val id: Long, val emoji: String, val x: Float)

/**
 * Mascot room: the player loves, plays with, grooms and feeds their mascot. Care fills the
 * friendship bar on the server (daily bond bonus when all three are done); magic fruits are a
 * Son Coin sink. The mascot reacts to every touch with a move, a face and a line in its voice.
 */
@Composable
internal fun MascotRoomDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val scope = rememberCoroutineScope()
    val owned = WordSiegeMascotOwnership.owned
    val skin = remember { WordSiegeMascotBond(context).skinChoice?.takeIf { it in owned } ?: owned.minByOrNull { it.ordinal } }
    if (skin == null) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }
    val mascotId = skin.productId
    val name = sh(skin.titleTr, skin.titleEn)

    var happiness by remember { mutableIntStateOf(50) }
    var fullness by remember { mutableIntStateOf(50) }
    var energy by remember { mutableIntStateOf(50) }
    var friendLevel by remember { mutableIntStateOf(1) }
    var friendXp by remember { mutableIntStateOf(0) }
    var loved by remember { mutableStateOf(false) }
    var played by remember { mutableStateOf(false) }
    var groomed by remember { mutableStateOf(false) }
    var applesLeft by remember { mutableIntStateOf(3) }
    var busy by remember { mutableStateOf(false) }
    var mood by remember { mutableStateOf<WordSiegeMascotEmotion?>(WordSiegeMascotEmotion.HAPPY) }
    var action by remember { mutableStateOf<WordSiegeMascotAction?>(WordSiegeMascotAction.WAVE) }
    var actionKey by remember { mutableLongStateOf(1L) }
    var line by remember { mutableStateOf(MascotVoice.style(sh("Hoş geldin! Beni ziyarete geldin!", "Welcome! You came to visit me!"), skin, 1)) }
    var lineSeed by remember { mutableIntStateOf(2) }
    val floats = remember { mutableStateListOf<RoomFloat>() }
    var floatId by remember { mutableLongStateOf(0L) }

    // Moves are chained into short scenes (e.g. clap, then a happy hop) so the mascot acts, not twitches.
    var scene by remember { mutableStateOf<List<WordSiegeMascotAction>>(emptyList()) }
    var sceneKey by remember { mutableLongStateOf(0L) }
    var lastTouch by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    var sceneRunning by remember { mutableStateOf(false) }
    var sceneEmoji by remember { mutableStateOf("") }
    var lastIdle by remember { mutableStateOf<WordSiegeMascotAction?>(null) }
    val fruitTravel = remember { Animatable(0f) }
    val spaceTravel = remember { Animatable(0f) }
    val kissApproach = remember { Animatable(0f) }
    var kissMark by remember { mutableStateOf(false) }
    var specialTap by remember { mutableIntStateOf(0) }

    fun react(emotion: WordSiegeMascotEmotion, moves: List<WordSiegeMascotAction>, text: String, emoji: String) {
        mood = emotion
        sceneRunning = true
        scene = moves
        sceneEmoji = emoji
        sceneKey += 1
        lastTouch = SystemClock.uptimeMillis()
        lineSeed += 1
        line = MascotVoice.style(text, skin, lineSeed)

    }

    LaunchedEffect(sceneKey) {
        if (scene.isEmpty()) return@LaunchedEffect
        sceneRunning = true
        try {
            for ((index, move) in scene.withIndex()) {
                action = move
                actionKey += 1
                // Stage effects after anticipation, not before the mascot responds.
                val effectDelay = when (move) {
                    WordSiegeMascotAction.NUZZLE -> 1_000L
                    WordSiegeMascotAction.SPARKLE -> 400L
                    WordSiegeMascotAction.HOP -> 500L
                    else -> 0L
                }
                delay(effectDelay)
                if (sceneEmoji.isNotEmpty() && (move == WordSiegeMascotAction.NUZZLE ||
                        move == WordSiegeMascotAction.SPARKLE || move == WordSiegeMascotAction.HOP ||
                        (index == 0 && scene.size == 1))) {
                    repeat(4) { floats += RoomFloat(++floatId, sceneEmoji, Random.nextFloat()) }
                }
                delay(roomMoveMillis(move) - effectDelay)
            }
        } finally {
            sceneRunning = false
        }
    }

    LaunchedEffect(actionKey) {
        spaceTravel.snapTo(0f)
        kissApproach.snapTo(0f)
        kissMark = false
        if (action == WordSiegeMascotAction.FOOD_LOOK) fruitTravel.snapTo(0f)
        if (action == WordSiegeMascotAction.EAT) {
            fruitTravel.snapTo(0f)
            fruitTravel.animateTo(1f, tween(1_150, easing = FastOutSlowInEasing))
        }
        if (action == WordSiegeMascotAction.SPACE_FLIGHT) {
            spaceTravel.animateTo(1f, tween(1_450, easing = FastOutSlowInEasing))
            delay(400L)
            spaceTravel.animateTo(0f, tween(1_750, easing = FastOutSlowInEasing))
        }
        if (action == WordSiegeMascotAction.KISS) {
            kissApproach.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
            kissMark = true
            delay(500L)
            kissApproach.animateTo(0f, tween(850, easing = FastOutSlowInEasing))
            delay(350L)
            kissMark = false
        }
    }

    // Long quiet gestures (2–6 s), layered over breathing, blinks and stable eye fixation.
    // A scene owns the rig until its last move completes; backend latency cannot release that lock.
    LaunchedEffect(Unit) {
        val awakeMoves = listOf(WordSiegeMascotAction.LOOK_AROUND, WordSiegeMascotAction.STRETCH,
            WordSiegeMascotAction.PEEK, WordSiegeMascotAction.SWAY, WordSiegeMascotAction.THINK)
        while (true) {
            delay(Random.nextLong(2_000L, 4_000L))
            if (busy || sceneRunning || SystemClock.uptimeMillis() - lastTouch < 2_000L) continue
            mood = WordSiegeMascotEmotion.CALM
            sceneEmoji = ""
            if (energy < 30) {
                scene = listOf(WordSiegeMascotAction.YAWN, WordSiegeMascotAction.DOZE)
            } else {
                val next = awakeMoves.filter { it != lastIdle }.random()
                lastIdle = next
                scene = listOf(next)
            }
            sceneRunning = true
            sceneKey += 1
        }
    }

    LaunchedEffect(mascotId) {
        if (!SupabaseProvider.configured) return@LaunchedEffect
        runCatching { MascotRoomBackend.progress(mascotId) }.getOrNull()?.let {
            happiness = it.happiness; fullness = it.fullness; energy = it.energy
            applesLeft = (it.normalFruitDailyLimit - it.normalFruitUsedToday).coerceAtLeast(0)
        }
        runCatching { MascotRoomBackend.room(mascotId) }.getOrNull()?.let {
            friendLevel = it.friendshipLevel; friendXp = it.friendshipXp
            loved = it.lovedToday; played = it.playedToday; groomed = it.groomedToday
        }
    }

    fun care(kind: String) {
        if (busy) return
        busy = true
        when (kind) {
            "love" -> react(WordSiegeMascotEmotion.HAPPY, listOf(WordSiegeMascotAction.NUZZLE, WordSiegeMascotAction.KISS, WordSiegeMascotAction.SWAY), sh("Sen en iyi dostumsun!", "You're my best friend!"), "💖")
            "play" -> react(WordSiegeMascotEmotion.EXCITED,
                if (happiness >= 80) listOf(WordSiegeMascotAction.HOP, WordSiegeMascotAction.SPACE_FLIGHT, WordSiegeMascotAction.CHEER, WordSiegeMascotAction.SWAY)
                else listOf(WordSiegeMascotAction.HOP, WordSiegeMascotAction.CLAP, WordSiegeMascotAction.SWAY),
                sh("Yaşasın, oyun zamanı!", "Yay, playtime!"), "⚽")
            else -> react(WordSiegeMascotEmotion.PROUD, listOf(WordSiegeMascotAction.GROOM, WordSiegeMascotAction.SPARKLE, WordSiegeMascotAction.SWAY), sh("Pırıl pırıl oldum!", "All sparkly now!"), "✨")
        }
        scope.launch {
            val cared = runCatching { MascotRoomBackend.care(mascotId, kind) }.getOrNull()
            if (cared == null) {
                react(WordSiegeMascotEmotion.SAD, listOf(WordSiegeMascotAction.SHRUG), sh("Bağlantı koptu... birazdan yine dene?", "Lost the connection... try again soon?"), "💧")
            }
            cared?.let {
                happiness = it.happiness; fullness = it.fullness; energy = it.energy
                friendLevel = it.friendshipLevel; friendXp = it.friendshipXp
                when (kind) { "love" -> loved = true; "play" -> played = true; else -> groomed = true }

                if (it.dailyBonusAwarded) {
                    while (sceneRunning) delay(100L)
                    react(WordSiegeMascotEmotion.LAUGH, listOf(WordSiegeMascotAction.CHEER, WordSiegeMascotAction.DANCE), sh("Bugünkü bağımız tamam! Sana bir anı parçası!", "Today's bond is complete! A memory shard for you!"), "🌟")
                }
            }
            busy = false
        }
    }

    fun feed(fruit: RoomFruit) {
        if (busy) return
        if (fruit.price == 0 && applesLeft <= 0) {
            // The daily reward is exhausted, but the mascot can still enjoy a purely visual
            // apple snack. No server call, fullness gain or extra reward is granted.
            react(WordSiegeMascotEmotion.HAPPY,
                listOf(WordSiegeMascotAction.FOOD_LOOK, WordSiegeMascotAction.EAT, WordSiegeMascotAction.HOP),
                sh("Elmayı yedim! Bugünkü üç tokluk ödülünü zaten aldım.", "Yummy apple! Today's three fullness rewards are already used."), "🍎")
            return
        }
        busy = true
        scope.launch {
            val ok = runCatching {
                if (fruit.price > 0) MascotRoomBackend.buyFruit(fruit.id)
                MascotRoomBackend.feed(mascotId, fruit.id)
            }
            val result = ok.getOrNull()
            if (result != null) {
                happiness = result.happiness; fullness = result.fullness; energy = result.energy
                if (fruit.price == 0) applesLeft = (3 - result.normalFruitUsedToday).coerceAtLeast(0)
                react(
                    WordSiegeMascotEmotion.LAUGH,
                    listOf(WordSiegeMascotAction.FOOD_LOOK, WordSiegeMascotAction.EAT, WordSiegeMascotAction.HOP, WordSiegeMascotAction.SWAY),
                    if (fruit.price > 0) sh("Sihirli lezzet! Güç doldum!", "Magic flavour! Power up!") else sh("Nam nam, çok lezzetli!", "Nom nom, so tasty!"),
                    fruit.emoji,
                )
            } else {
                val poor = ok.exceptionOrNull()?.message.orEmpty().contains("insufficient_diamonds")
                react(
                    WordSiegeMascotEmotion.SAD,
                    listOf(WordSiegeMascotAction.SHRUG),
                    if (poor) sh("Jeton yetmedi... maç kazanıp geri gelelim mi?", "Not enough coins... win a match and come back?")
                    else sh("Bağlantı kayboldu, sonra tekrar dene?", "Lost the connection, try again later?"),
                    "💧",
                )
            }
            busy = false
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0B0D1F), Color(0xFF1A1740), Color(0xFF2A2160)))),
        ) {
            RoomNightSky(Modifier.matchParentSize())
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.size(44.dp))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(name, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = .5.sp)
                        Text(sh("Dostluk seviyesi $friendLevel", "Friendship level $friendLevel"), color = Color(0xFFB9B3E8), fontSize = 13.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Rounded.Close, sh("Kapat", "Close"), tint = Color.White.copy(alpha = .8f))
                    }
                }
                RoomFriendship(friendLevel, friendXp)
                Box(Modifier.fillMaxWidth().height(330.dp), contentAlignment = Alignment.BottomCenter) {
                    // Soft moonlight pool the mascot floats in.
                    Box(Modifier.size(260.dp).align(Alignment.BottomCenter).background(Brush.radialGradient(listOf(Color(0x557C6CFF), Color.Transparent)), CircleShape))
                    WordSiegeMascot(
                        moveId = null,
                        lastMoveMine = false,
                        pendingCells = emptyList(),
                        playerTurn = true,
                        requestedEmotion = mood,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp).size(230.dp)
                            .graphicsLayer {
                                val flight = spaceTravel.value
                                val near = kissApproach.value
                                val kissScale = (screenWidthDp * .5f / (230f * .64f)).coerceIn(1f, 1.9f)
                                translationY = -flight * (screenHeightDp * .65f).dp.toPx() - near * 32.dp.toPx()
                                val scale = (1f - flight * .60f) * (1f + (kissScale - 1f) * near)
                                scaleX = scale
                                scaleY = scale
                            },
                        actionKey = actionKey,
                        action = action,
                        flying = sceneRunning && action == WordSiegeMascotAction.SPACE_FLIGHT,
                        skin = skin,
                        onTap = {
                            if (!busy && !sceneRunning) {
                                if (!loved) care("love") else {
                                    specialTap++
                                    if (specialTap % 2 == 0 && happiness >= 80) {
                                        react(WordSiegeMascotEmotion.EXCITED,
                                            listOf(WordSiegeMascotAction.HOP, WordSiegeMascotAction.SPACE_FLIGHT, WordSiegeMascotAction.CHEER),
                                            sh("Sevinçten uçuyorum!", "I'm flying with joy!"), "✨")
                                    } else {
                                        react(WordSiegeMascotEmotion.HAPPY,
                                            listOf(WordSiegeMascotAction.NUZZLE, WordSiegeMascotAction.KISS),
                                            sh("Bu öpücük sana!", "This kiss is for you!"), "💕")
                                    }
                                }
                            }
                        },
                    )
                    if (kissMark && action == WordSiegeMascotAction.KISS) {
                        Text("💋", fontSize = 48.sp,
                            modifier = Modifier.align(Alignment.Center).offset(y = 18.dp)
                                .graphicsLayer {
                                    alpha = (.35f + .65f * kissApproach.value).coerceIn(0f, 1f)
                                    scaleX = .7f + .3f * kissApproach.value
                                    scaleY = scaleX
                                })
                    }
                    if (sceneRunning && (action == WordSiegeMascotAction.FOOD_LOOK || action == WordSiegeMascotAction.EAT)) {
                        Text(sceneEmoji, fontSize = 28.sp,
                            modifier = Modifier.align(Alignment.BottomCenter).offset(x = 48.dp, y = (-60).dp)
                                .graphicsLayer {
                                    val travel = fruitTravel.value
                                    translationX = -travel * 24.dp.toPx()
                                    translationY = -travel * 16.dp.toPx()
                                    scaleX = 1f - travel * .65f
                                    scaleY = scaleX
                                    alpha = 1f - travel
                                })
                    }
                    Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                        floats.toList().forEach { f -> key(f.id) { RoomFloatingEmoji(f) { floats.remove(f) } } }
                    }
                    // The line floats above the mascot and fades in with each new sentence.
                    androidx.compose.animation.AnimatedContent(
                        targetState = line,
                        modifier = Modifier.align(Alignment.TopCenter),
                        transitionSpec = { androidx.compose.animation.fadeIn(tween(400)) togetherWith androidx.compose.animation.fadeOut(tween(250)) },
                        label = "room-line",
                    ) { text ->
                        Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = .92f)) {
                            Text(text, Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = Color(0xFF1A1740), fontSize = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                        }
                    }
                }
                RoomGlass {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        RoomStat(sh("Mutluluk", "Happiness"), happiness, Color(0xFFFF8FC7))
                        RoomStat(sh("Tokluk", "Fullness"), fullness, Color(0xFFFFC46B))
                        RoomStat(sh("Enerji", "Energy"), energy, Color(0xFF7FD8FF))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RoomCareButton(Icons.Rounded.Favorite, sh("Sev", "Love"), loved, Color(0xFFFF8FC7), Modifier.weight(1f)) { care("love") }
                    RoomCareButton(Icons.Rounded.SportsEsports, sh("Oyna", "Play"), played, Color(0xFF8BE39A), Modifier.weight(1f)) { care("play") }
                    RoomCareButton(Icons.Rounded.AutoAwesome, sh("Tımar", "Groom"), groomed, Color(0xFF7FD8FF), Modifier.weight(1f)) { care("groom") }
                }
                Text(
                    sh("Üçünü de yaparsan günlük bağ bonusu kazanırsın.", "Do all three for the daily bond bonus."),
                    color = Color(0xFFB9B3E8),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
                RoomGlass {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(sh("Besle", "Feed"), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RoomFruits.forEach { fruit ->
                                Surface(
                                    onClick = { feed(fruit) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = .08f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .14f)),
                                ) {
                                    Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(fruit.emoji, fontSize = 24.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            if (fruit.price == 0) "$applesLeft/3" else "${fruit.price} SC",
                                            color = if (fruit.price == 0) Color(0xFF8BE39A) else Color(0xFFFFD36B),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

/** How long a room move plays before the next one in a scene starts. */
private fun roomMoveMillis(move: WordSiegeMascotAction): Long = mascotActionMillis(move)

@Composable
private fun RoomGlass(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = .06f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .12f)),
        content = content,
    )
}

/** Slowly twinkling stars behind the room. */
@Composable
private fun RoomNightSky(modifier: Modifier) {
    val stars = remember { List(46) { Triple(Random.nextFloat(), Random.nextFloat() * .7f, Random.nextFloat()) } }
    val twinkle = rememberInfiniteTransition(label = "room-sky")
    val t by twinkle.animateFloat(0f, 6.2832f, infiniteRepeatable(tween(6_000, easing = LinearEasing)), label = "room-sky-t")
    Canvas(modifier) {
        stars.forEach { (x, y, phase) ->
            val a = .25f + .45f * (.5f + .5f * kotlin.math.sin(t + phase * 6.28f))
            drawCircle(Color.White.copy(alpha = a), radius = 1.2.dp.toPx() + phase * 1.1.dp.toPx(), center = Offset(x * size.width, y * size.height))
        }
    }
}

@Composable
private fun RoomFriendship(level: Int, xp: Int) {
    val inLevel = (xp % 40) / 40f
    val shown by animateFloatAsState(inLevel, tween(900), label = "room-friend")
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Favorite, null, tint = Color(0xFFFF8FC7), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(6.dp).background(Color.White.copy(alpha = .12f), CircleShape)) {
            Box(
                Modifier.fillMaxWidth(shown.coerceAtLeast(.03f)).fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(Color(0xFFFF8FC7), Color(0xFFB08CFF))), CircleShape),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text("${xp % 40}/40", color = Color(0xFFB9B3E8), fontSize = 12.sp)
    }
}

@Composable
private fun RoomStat(label: String, value: Int, color: Color) {
    val shown by animateFloatAsState(value / 100f, tween(900), label = "room-stat")
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White.copy(alpha = .85f), fontSize = 14.sp, modifier = Modifier.width(90.dp))
        Box(Modifier.weight(1f).height(8.dp).background(Color.White.copy(alpha = .1f), CircleShape)) {
            Box(Modifier.fillMaxWidth(shown.coerceIn(.03f, 1f)).fillMaxHeight().background(color, CircleShape))
        }
        Text("$value", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun RoomCareButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    done: Boolean,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = if (done) .10f else .18f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = .45f)),
    ) {
        Column(Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(if (done) sh("Bugün yapıldı", "Done today") else sh("+ dostluk", "+ bond"), color = Color.White.copy(alpha = .6f), fontSize = 11.sp)
        }
    }
}

/** An emoji that pops out of the mascot and floats up while fading. */
@Composable
private fun RoomFloatingEmoji(item: RoomFloat, onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(item.id) {
        progress.animateTo(1f, tween(1400, easing = LinearEasing))
        onDone()
    }
    val p = progress.value
    Text(
        item.emoji,
        fontSize = 26.sp,
        modifier = Modifier
            .graphicsLayer {
                translationX = (item.x - .5f) * 360f + kotlin.math.sin(p * 9f + item.x * 6f) * 18f
                translationY = -p * 320f
                scaleX = .6f + p * .7f
                scaleY = .6f + p * .7f
            }
            .alpha(1f - p),
    )
}
