package com.sonharf.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
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

    fun react(emotion: WordSiegeMascotEmotion, move: WordSiegeMascotAction, text: String, emoji: String) {
        mood = emotion
        action = move
        actionKey += 1
        lineSeed += 1
        line = MascotVoice.style(text, skin, lineSeed)
        repeat(5) { floats += RoomFloat(++floatId, emoji, Random.nextFloat()) }
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
            "love" -> react(WordSiegeMascotEmotion.HAPPY, WordSiegeMascotAction.CLAP, sh("Sen en iyi dostumsun!", "You're my best friend!"), "💖")
            "play" -> react(WordSiegeMascotEmotion.EXCITED, WordSiegeMascotAction.DANCE, sh("Yaşasın, oyun zamanı!", "Yay, playtime!"), "⚽")
            else -> react(WordSiegeMascotEmotion.PROUD, WordSiegeMascotAction.SPARKLE, sh("Pırıl pırıl oldum!", "All sparkly now!"), "✨")
        }
        scope.launch {
            runCatching { MascotRoomBackend.care(mascotId, kind) }.getOrNull()?.let {
                happiness = it.happiness; fullness = it.fullness; energy = it.energy
                friendLevel = it.friendshipLevel; friendXp = it.friendshipXp
                when (kind) { "love" -> loved = true; "play" -> played = true; else -> groomed = true }
                if (it.dailyBonusAwarded) {
                    delay(900)
                    react(WordSiegeMascotEmotion.LAUGH, WordSiegeMascotAction.CHEER, sh("Bugünkü bağımız tamam! Sana bir anı parçası!", "Today's bond is complete! A memory shard for you!"), "🌟")
                }
            }
            busy = false
        }
    }

    fun feed(fruit: RoomFruit) {
        if (busy) return
        if (fruit.price == 0 && applesLeft <= 0) {
            react(WordSiegeMascotEmotion.CALM, WordSiegeMascotAction.SHRUG, sh("Bugünlük elma bitti... yarın yine gel?", "No more apples today... come back tomorrow?"), "🍎")
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
                    WordSiegeMascotAction.HOP,
                    if (fruit.price > 0) sh("Sihirli lezzet! Güç doldum!", "Magic flavour! Power up!") else sh("Nam nam, çok lezzetli!", "Nom nom, so tasty!"),
                    fruit.emoji,
                )
            } else {
                val poor = ok.exceptionOrNull()?.message.orEmpty().contains("insufficient_diamonds")
                react(
                    WordSiegeMascotEmotion.SAD,
                    WordSiegeMascotAction.SHRUG,
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
                .background(Brush.verticalGradient(listOf(Color(0xFF2B2466), Color(0xFF5B3FA8), Color(0xFFE9A6C9)))),
        ) {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.size(48.dp))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(sh("MASKOT ODASI", "MASCOT ROOM"), color = Color(0xFFFFD36B), fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Text(name, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Close, sh("Kapat", "Close"), tint = Color.White)
                    }
                }
                RoomFriendship(friendLevel, friendXp)
                // Speech bubble in the mascot's own voice.
                Surface(shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 4.dp) {
                    Text(line, Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = Hf.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                }
                Box(Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(210.dp).background(Brush.radialGradient(listOf(Color.White.copy(alpha = .35f), Color.Transparent)), CircleShape))
                    WordSiegeMascot(
                        moveId = null,
                        lastMoveMine = false,
                        pendingCells = emptyList(),
                        playerTurn = true,
                        requestedEmotion = mood,
                        modifier = Modifier.size(220.dp),
                        actionKey = actionKey,
                        action = action,
                        skin = skin,
                        onTap = { if (!loved) care("love") else react(WordSiegeMascotEmotion.HAPPY, WordSiegeMascotAction.NOD, sh("Hihi, gıdıklanıyorum!", "Hehe, that tickles!"), "💕") },
                    )
                    floats.toList().forEach { f -> key(f.id) { RoomFloatingEmoji(f) { floats.remove(f) } } }
                }
                RoomStat("😊", sh("Mutluluk", "Happiness"), happiness, Color(0xFFFF7EB6))
                RoomStat("🍽", sh("Tokluk", "Fullness"), fullness, Color(0xFFFFB347))
                RoomStat("⚡", sh("Enerji", "Energy"), energy, Color(0xFF6FD3FF))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RoomCareButton("💖", sh("Sev", "Love"), loved, HfPanel.CoralSet, Modifier.weight(1f)) { care("love") }
                    RoomCareButton("⚽", sh("Oyna", "Play"), played, HfPanel.GreenSet, Modifier.weight(1f)) { care("play") }
                    RoomCareButton("✨", sh("Tımar", "Groom"), groomed, HfPanel.BlueSet, Modifier.weight(1f)) { care("groom") }
                }
                Text(
                    sh("Üçünü de yap: günlük bağ bonusu!", "Do all three: daily bond bonus!"),
                    color = Color.White.copy(alpha = .85f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(sh("BESLE", "FEED"), color = Color(0xFFFFD36B), fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoomFruits.forEach { fruit ->
                        HfGamePanel(if (fruit.price == 0) HfPanel.GreenSet else HfPanel.GoldSet, Modifier.weight(1f), onClick = { feed(fruit) }, corner = 16.dp, lip = 4.dp) {
                            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(fruit.emoji, fontSize = 28.sp)
                                Text(
                                    if (fruit.price == 0) sh("$applesLeft/3", "$applesLeft/3") else "${fruit.price}",
                                    color = if (fruit.price == 0) Color.White else Hf.Ink,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                )
                                Text(
                                    if (fruit.price == 0) sh("Bedava", "Free") else "SC",
                                    color = if (fruit.price == 0) Color.White.copy(alpha = .9f) else Hf.Ink.copy(alpha = .8f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun RoomFriendship(level: Int, xp: Int) {
    val inLevel = (xp % 40) / 40f
    val shown by animateFloatAsState(inLevel, tween(600), label = "room-friend")
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = .14f)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(Brush.verticalGradient(listOf(Color(0xFFFF9BC8), Color(0xFFE8436E))), CircleShape), contentAlignment = Alignment.Center) {
                Text("$level", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(sh("Dostluk seviyesi", "Friendship level"), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(10.dp).background(Color.White.copy(alpha = .2f), CircleShape)) {
                    Box(Modifier.fillMaxWidth(shown.coerceAtLeast(.04f)).fillMaxHeight().background(Color(0xFFFF7EB6), CircleShape))
                }
            }
            Spacer(Modifier.width(10.dp))
            Text("💞", fontSize = 26.sp)
        }
    }
}

@Composable
private fun RoomStat(emoji: String, label: String, value: Int, color: Color) {
    val shown by animateFloatAsState(value / 100f, tween(700), label = "room-stat")
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 20.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(92.dp))
        Box(Modifier.weight(1f).height(14.dp).background(Color.White.copy(alpha = .2f), CircleShape)) {
            Box(Modifier.fillMaxWidth(shown.coerceIn(.03f, 1f)).fillMaxHeight().background(color, CircleShape))
        }
        Text("$value", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun RoomCareButton(emoji: String, label: String, done: Boolean, colors: List<Color>, modifier: Modifier, onClick: () -> Unit) {
    HfGamePanel(colors, modifier, onClick = onClick, corner = 18.dp) {
        Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 30.sp)
            Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text(if (done) "✓" else sh("+dostluk", "+bond"), color = Color.White.copy(alpha = .9f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
