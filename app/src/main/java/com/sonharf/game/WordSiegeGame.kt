package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.validateCoreWord
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private enum class SiegeTurn { PLAYER, BOT, FINISHED }
private enum class SiegeBonus { DOUBLE, TREASURE, BRIDGE, FOG, CASTLE }

private data class SiegeBoardTile(val letter: Char, val owner: Int)
private data class SiegePlacement(
    val row: Int,
    val col: Int,
    val horizontal: Boolean,
    val word: String,
    val newIndices: List<Int>,
    val score: Int,
)
private data class SiegeImpact(
    val damage: Int,
    val shield: Int,
    val critical: Boolean,
    val treasure: Int,
    val bridgeBoost: Int,
    val fog: Boolean,
)

private val siegeWords = listOf(
    "kalem","savaş","kule","masa","araba","kapı","roman","orman","kart",
    "serin","salon","tarla","kitap","taş","saray","kale","liman","metal","merak",
    "martı","simit","resim","insan","sanat","saat","tarih","ana","tan"
)

private val siegeFillers = "AEIİKLMNRSTOU".toList()

private val siegeBonuses = mapOf(
    12 to SiegeBonus.TREASURE,
    16 to SiegeBonus.DOUBLE,
    35 to SiegeBonus.FOG,
    40 to SiegeBonus.CASTLE,
    58 to SiegeBonus.BRIDGE,
    64 to SiegeBonus.DOUBLE,
    68 to SiegeBonus.TREASURE,
    72 to SiegeBonus.FOG,
)

private fun siegeNorm(value: String): String =
    value.trim().lowercase(Locale.forLanguageTag("tr-TR"))

private fun initialSiegeBoard(): List<SiegeBoardTile?> {
    val board = MutableList<SiegeBoardTile?>(81) { null }
    val word = "taht"
    val row = 2
    val startCol = 2
    word.forEachIndexed { i, ch -> board[row * 9 + startCol + i] = SiegeBoardTile(ch, 0) }
    return board
}

private fun initialSiegeTerritory(): List<Int> =
    List(81) { index ->
        when (index % 9) {
            0, 1, 2 -> 1
            6, 7, 8 -> 2
            else -> 0
        }
    }

private fun placementIndices(placement: SiegePlacement): List<Int> =
    placement.word.indices.map { i ->
        val row = placement.row + if (placement.horizontal) 0 else i
        val col = placement.col + if (placement.horizontal) i else 0
        row * 9 + col
    }

private fun findSiegePlacement(
    board: List<SiegeBoardTile?>,
    rawWord: String,
): SiegePlacement? {
    val word = siegeNorm(rawWord)
    if (word.length !in 3..7) return null
    var best: SiegePlacement? = null

    fun consider(row: Int, col: Int, horizontal: Boolean) {
        val indices = mutableListOf<Int>()
        var intersections = 0
        var score = 0
        for (i in word.indices) {
            val r = row + if (horizontal) 0 else i
            val c = col + if (horizontal) i else 0
            if (r !in 0..8 || c !in 0..8) return
            val index = r * 9 + c
            val existing = board[index]
            if (existing != null && existing.letter != word[i]) return
            if (existing != null) intersections += 1 else indices += index
        }
        if (intersections == 0 || indices.isEmpty()) return

        score += intersections * 6 + indices.size * 2
        indices.forEach { index ->
            score += when (siegeBonuses[index]) {
                SiegeBonus.DOUBLE -> 5
                SiegeBonus.TREASURE -> 4
                SiegeBonus.BRIDGE -> 4
                SiegeBonus.FOG -> 3
                SiegeBonus.CASTLE -> 6
                null -> 0
            }
        }

        val candidate = SiegePlacement(row, col, horizontal, word, indices, score)
        if (best == null || candidate.score > best!!.score) best = candidate
    }

    for (horizontal in listOf(true, false)) {
        val maxRow = if (horizontal) 8 else 9 - word.length
        val maxCol = if (horizontal) 9 - word.length else 8
        for (row in 0..maxRow) for (col in 0..maxCol) consider(row, col, horizontal)
    }
    return best
}

private fun placeSiegeWord(
    board: List<SiegeBoardTile?>,
    placement: SiegePlacement,
    owner: Int,
): List<SiegeBoardTile?> {
    val next = board.toMutableList()
    placement.word.forEachIndexed { i, ch ->
        val row = placement.row + if (placement.horizontal) 0 else i
        val col = placement.col + if (placement.horizontal) i else 0
        val index = row * 9 + col
        if (next[index] == null) next[index] = SiegeBoardTile(ch, owner)
    }
    return next
}

private fun siegeImpact(placement: SiegePlacement): SiegeImpact {
    var multiplier = 1
    var shield = 0
    var treasure = 0
    var bridgeBoost = 0
    var siegePower = 0
    var fog = false

    placement.newIndices.forEach { index ->
        when (siegeBonuses[index]) {
            SiegeBonus.DOUBLE -> multiplier = 2
            SiegeBonus.TREASURE -> treasure += 7
            SiegeBonus.BRIDGE -> bridgeBoost += 2
            SiegeBonus.FOG -> fog = true
            SiegeBonus.CASTLE -> {
                shield += 5
                siegePower += 5
            }
            null -> Unit
        }
    }

    var damage = placement.word.length * 2 + siegePower + bridgeBoost * 2
    if (placement.word.length >= 6) damage += 4
    damage *= multiplier
    return SiegeImpact(
        damage = damage.coerceAtMost(32),
        shield = shield,
        critical = multiplier > 1 || damage >= 18,
        treasure = treasure,
        bridgeBoost = bridgeBoost,
        fog = fog,
    )
}

private fun claimSiegeTerritory(
    territory: List<Int>,
    placement: SiegePlacement,
    owner: Int,
    bridgeBoost: Int,
): Pair<List<Int>, List<Int>> {
    val next = territory.toMutableList()
    val captured = linkedSetOf<Int>()

    fun claim(index: Int) {
        if (index !in next.indices) return
        if (next[index] != owner) captured += index
        next[index] = owner
    }

    val wordCells = placementIndices(placement)
    wordCells.forEach(::claim)

    val direction = if (owner == 1) 1 else -1
    placement.newIndices.chunked(2).forEach { chunk ->
        val source = chunk.last()
        val row = source / 9
        val col = source % 9
        val nextCol = col + direction
        if (nextCol in 0..8) claim(row * 9 + nextCol)
    }

    if (bridgeBoost > 0) {
        val source = wordCells.lastOrNull() ?: return next to captured.toList()
        val row = source / 9
        var col = source % 9
        repeat(bridgeBoost) {
            col += direction
            if (col in 0..8) claim(row * 9 + col)
        }
    }

    return next to captured.toList()
}

private fun rackForBoard(board: List<SiegeBoardTile?>): List<Char> {
    val playable = siegeWords
        .mapNotNull { word -> findSiegePlacement(board, word)?.let { word } }
        .filter { it.length <= 7 }
    val seed = playable.randomOrNull() ?: "kalem"
    val chars = seed.uppercase(Locale.forLanguageTag("tr-TR")).toMutableList()
    while (chars.size < 7) chars += siegeFillers.random()
    return chars.take(7).shuffled()
}

private fun botWordForBoard(board: List<SiegeBoardTile?>): Pair<String, SiegePlacement>? =
    siegeWords
        .shuffled()
        .mapNotNull { word -> findSiegePlacement(board, word)?.let { word to it } }
        .maxByOrNull { it.second.score + it.first.length }

@Composable
internal fun WordSiegeGameScreen(onExit: () -> Unit) {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    val tr = remember { Locale.forLanguageTag("tr-TR") }

    var gameId by remember { mutableIntStateOf(1) }
    var board by remember(gameId) { mutableStateOf(initialSiegeBoard()) }
    var territory by remember(gameId) { mutableStateOf(initialSiegeTerritory()) }
    var lastCaptured by remember(gameId) { mutableStateOf<List<Int>>(emptyList()) }
    var myHp by remember(gameId) { mutableIntStateOf(100) }
    var botHp by remember(gameId) { mutableIntStateOf(100) }
    var myShield by remember(gameId) { mutableIntStateOf(0) }
    var botShield by remember(gameId) { mutableIntStateOf(0) }
    var myLoot by remember(gameId) { mutableIntStateOf(0) }
    var botLoot by remember(gameId) { mutableIntStateOf(0) }
    var myAttackFogged by remember(gameId) { mutableStateOf(false) }
    var botAttackFogged by remember(gameId) { mutableStateOf(false) }
    var myCastleHit by remember(gameId) { mutableIntStateOf(0) }
    var botCastleHit by remember(gameId) { mutableIntStateOf(0) }
    var turn by remember(gameId) { mutableStateOf(SiegeTurn.PLAYER) }
    var round by remember(gameId) { mutableIntStateOf(1) }
    var rack by remember(gameId) { mutableStateOf(rackForBoard(initialSiegeBoard())) }
    var selected by remember(gameId) { mutableStateOf<List<Int>>(emptyList()) }
    var notice by remember(gameId) { mutableStateOf("Harfleri seç ve bölgeyi kuşat.") }
    var attackBanner by remember(gameId) { mutableStateOf<String?>(null) }
    var attackIsMine by remember(gameId) { mutableStateOf(true) }
    var crit by remember(gameId) { mutableStateOf(false) }
    var busy by remember(gameId) { mutableStateOf(false) }
    var myProfile by remember { mutableStateOf<ProfileDto?>(null) }

    val word = selected.joinToString("") { rack[it].toString() }
    val gameOver = turn == SiegeTurn.FINISHED
    val myTerritory = territory.count { it == 1 }
    val botTerritory = territory.count { it == 2 }

    BackHandler { onExit() }

    LaunchedEffect(backend) {
        val id = backend?.currentUserId() ?: return@LaunchedEffect
        myProfile = runCatching { backend.getProfile(id) }.getOrNull()
    }

    fun finishIfNeeded() {
        if (botHp <= 0 || myHp <= 0 || round > 15) {
            turn = SiegeTurn.FINISHED
            notice = when {
                botHp <= 0 -> "RAKİP KALE YIKILDI!"
                myHp <= 0 -> "KALEN YIKILDI!"
                myHp > botHp -> "KUŞATMAYI KAZANDIN!"
                myHp < botHp -> "BOT KAZANDI."
                myTerritory > botTerritory -> "ALAN ÜSTÜNLÜĞÜYLE KAZANDIN!"
                myTerritory < botTerritory -> "BOT ALAN ÜSTÜNLÜĞÜYLE KAZANDI."
                else -> "BERABERE."
            }
        }
    }

    fun submitPlayerWord() {
        if (turn != SiegeTurn.PLAYER || busy) return
        val normalized = siegeNorm(word)
        if (normalized.length < 3) {
            notice = "En az 3 harf seç."
            SonHarfSoundFx.warning()
            return
        }
        val placement = findSiegePlacement(board, normalized)
        if (placement == null) {
            notice = "Kelime tahtadaki bir harfle kesişmeli."
            SonHarfSoundFx.warning()
            return
        }

        scope.launch {
            busy = true
            val valid = runCatching {
                (backend?.validateCoreWord(normalized, SonHarfUiState.language) ?: false) ||
                    normalized in siegeWords
            }.getOrDefault(normalized in siegeWords)

            if (!valid) {
                notice = "Sözlükte yok."
                SonHarfSoundFx.warning()
                busy = false
                return@launch
            }

            val impact = siegeImpact(placement)
            var rawDamage = impact.damage
            if (myAttackFogged) {
                rawDamage = (rawDamage * 0.7f).toInt().coerceAtLeast(1)
                myAttackFogged = false
            }
            val absorbed = minOf(botShield, rawDamage)
            val damage = (rawDamage - absorbed).coerceAtLeast(0)
            botShield = (botShield - absorbed).coerceAtLeast(0)
            botHp = (botHp - damage).coerceAtLeast(0)
            myShield += impact.shield
            myLoot += impact.treasure
            if (impact.fog) botAttackFogged = true

            val territoryResult = claimSiegeTerritory(territory, placement, 1, impact.bridgeBoost)
            territory = territoryResult.first
            lastCaptured = territoryResult.second
            board = placeSiegeWord(board, placement, 1)

            botCastleHit += 1
            attackBanner = if (impact.critical) "KRİTİK!  -$damage HP" else "-$damage HP"
            attackIsMine = true
            crit = impact.critical
            notice = "${normalized.uppercase(tr)} oynandı!  +${lastCaptured.size} kare ele geçirildi"
            selected = emptyList()
            SonHarfSoundFx.wordAccepted()
            if (impact.critical || impact.treasure > 0) SonHarfSoundFx.bonus()

            if (botHp <= 0) {
                turn = SiegeTurn.FINISHED
                finishIfNeeded()
            } else {
                turn = SiegeTurn.BOT
            }
            busy = false
        }
    }

    LaunchedEffect(gameId, turn) {
        if (turn != SiegeTurn.BOT) return@LaunchedEffect
        delay(900)
        val choice = botWordForBoard(board)
        if (choice == null) {
            round += 1
            rack = rackForBoard(board)
            turn = SiegeTurn.PLAYER
            return@LaunchedEffect
        }

        val (botWord, placement) = choice
        val impact = siegeImpact(placement)
        var rawDamage = impact.damage
        if (botAttackFogged) {
            rawDamage = (rawDamage * 0.7f).toInt().coerceAtLeast(1)
            botAttackFogged = false
        }
        val absorbed = minOf(myShield, rawDamage)
        val damage = (rawDamage - absorbed).coerceAtLeast(0)
        myShield = (myShield - absorbed).coerceAtLeast(0)
        myHp = (myHp - damage).coerceAtLeast(0)
        botShield += impact.shield
        botLoot += impact.treasure
        if (impact.fog) myAttackFogged = true

        val territoryResult = claimSiegeTerritory(territory, placement, 2, impact.bridgeBoost)
        territory = territoryResult.first
        lastCaptured = territoryResult.second
        board = placeSiegeWord(board, placement, 2)

        myCastleHit += 1
        attackBanner = if (impact.critical) "KRİTİK!  -$damage HP" else "-$damage HP"
        attackIsMine = false
        crit = impact.critical
        notice = "BOT: ${botWord.uppercase(tr)}  •  +${lastCaptured.size} kare"
        SonHarfSoundFx.scoreTick()

        round += 1
        if (myHp <= 0 || round > 15) {
            turn = SiegeTurn.FINISHED
            finishIfNeeded()
        } else {
            rack = rackForBoard(board)
            selected = emptyList()
            turn = SiegeTurn.PLAYER
        }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(
                    Color(0xFF08192D),
                    Color(0xFF11365A),
                    Color(0xFFF3F7FB),
                    Color(0xFFF7F9FC),
                )
            )
        )
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onExit) {
                    Icon(Icons.Rounded.ArrowBack, "Geri", tint = Color.White)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("KELİME KUŞATMASI", color = Color(0xFFFFD45B), fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("BOT ANTRENMANI • Kelimeyi kur. Alanı ele geçir.", color = Color.White.copy(alpha = .78f), fontSize = 9.sp)
                }
                IconButton(onClick = { gameId += 1 }) {
                    Icon(Icons.Rounded.Refresh, "Yenile", tint = Color.White)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                SiegePlayerCard(
                    label = "SEN",
                    hp = myHp,
                    shield = myShield,
                    avatarPath = myProfile?.avatarPath,
                    name = myProfile?.displayName ?: "Oyuncu",
                    castleRes = R.drawable.castle_blue,
                    accent = SonHarfBlue,
                    modifier = Modifier.weight(1f),
                    hitCounter = myCastleHit,
                )
                Surface(shape = CircleShape, color = Color(0xFFFFC84B), modifier = Modifier.size(38.dp), shadowElevation = 5.dp) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("VS", color = Color(0xFF10243B), fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
                SiegePlayerCard(
                    label = "BOT",
                    hp = botHp,
                    shield = botShield,
                    avatarPath = null,
                    name = "BOT",
                    castleRes = R.drawable.castle_red,
                    accent = SonHarfPink,
                    modifier = Modifier.weight(1f),
                    bot = true,
                    hitCounter = botCastleHit,
                )
            }

            SiegeTerritoryBar(
                myTerritory = myTerritory,
                botTerritory = botTerritory,
                round = round.coerceAtMost(15),
                turn = turn,
            )

            attackBanner?.let { banner ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = if (attackIsMine) Color(0xFF102B49) else Color(0xFF4B1E2C),
                    border = BorderStroke(1.dp, if (attackIsMine) SonHarfBlue.copy(alpha = .7f) else SonHarfPink.copy(alpha = .7f)),
                    shadowElevation = 5.dp,
                ) {
                    Text(
                        banner,
                        Modifier.fillMaxWidth().padding(vertical = 7.dp),
                        textAlign = TextAlign.Center,
                        color = if (crit) Color(0xFFFFD45B) else Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                    )
                }
            }

            SiegeBoard(
                board = board,
                territory = territory,
                bonuses = siegeBonuses,
                lastCaptured = lastCaptured,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            )

            SiegeLegend()

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(13.dp),
                color = Color.White.copy(alpha = .96f),
                border = BorderStroke(1.dp, Color(0xFFDCE4EE)),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (word.isBlank()) "7 harften kelimeni kur" else word,
                            color = if (word.isBlank()) SonHarfMuted else SonHarfText,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                        )
                        Text(notice, color = SonHarfMuted, fontSize = 8.sp, maxLines = 1)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("🏆 $myLoot", color = SonHarfGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        if (myAttackFogged) Text("SİS ETKİSİ", color = Color(0xFF718096), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            SiegeRack(
                rack = rack,
                selected = selected,
                enabled = turn == SiegeTurn.PLAYER && !busy,
                onTile = { index ->
                    if (index !in selected && selected.size < 7) {
                        selected = selected + index
                        SonHarfSoundFx.typingClick()
                    }
                },
            )

            if (gameOver) {
                Button(
                    onClick = { gameId += 1 },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74B63)),
                ) { Text("⚔  RÖVANŞ AL", fontWeight = FontWeight.Black) }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedButton(
                        onClick = { selected = selected.dropLast(1) },
                        enabled = selected.isNotEmpty() && turn == SiegeTurn.PLAYER,
                        modifier = Modifier.size(46.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(13.dp),
                    ) { Icon(Icons.Rounded.Backspace, "Sil") }

                    Button(
                        onClick = ::submitPlayerWord,
                        enabled = selected.size >= 3 && turn == SiegeTurn.PLAYER && !busy,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SonHarfBlue),
                    ) {
                        Text(
                            if (turn == SiegeTurn.BOT) "BOT OYNUYOR…" else "⚔  KELİMEYİ GÖNDER",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                        )
                    }

                    OutlinedButton(
                        onClick = { if (selected.isEmpty()) rack = rack.shuffled() },
                        enabled = selected.isEmpty() && turn == SiegeTurn.PLAYER,
                        modifier = Modifier.size(46.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(13.dp),
                    ) { Icon(Icons.Rounded.Shuffle, "Karıştır") }
                }
            }
        }
    }
}

@Composable
private fun SiegePlayerCard(
    label: String,
    hp: Int,
    shield: Int,
    avatarPath: String?,
    name: String,
    castleRes: Int,
    accent: Color,
    modifier: Modifier,
    bot: Boolean = false,
    hitCounter: Int = 0,
) {
    var hit by remember { mutableStateOf(false) }
    LaunchedEffect(hitCounter) {
        if (hitCounter <= 0) return@LaunchedEffect
        hit = true
        delay(150)
        hit = false
    }
    val castleScale by animateFloatAsState(
        targetValue = if (hit) 1.12f else 1f,
        animationSpec = tween(140),
        label = "castle-scale",
    )
    val castleRotation by animateFloatAsState(
        targetValue = if (hit) if (bot) -7f else 7f else 0f,
        animationSpec = tween(140),
        label = "castle-rotation",
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        color = Color.White.copy(alpha = .96f),
        border = BorderStroke(1.5.dp, accent.copy(alpha = .55f)),
        shadowElevation = 4.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (bot) {
                Surface(Modifier.size(34.dp), shape = CircleShape, color = accent.copy(alpha = .09f), border = BorderStroke(2.dp, accent)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("BOT", color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }
            } else {
                ProfilePhotoAvatar(
                    avatarPath = avatarPath,
                    name = name,
                    size = 34.dp,
                    visible = true,
                    accent = accent,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(label, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(hp.toString(), color = accent, fontSize = 21.sp, fontWeight = FontWeight.Black)
                    Text(" HP", color = SonHarfText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
                if (shield > 0) Text("Kalkan +$shield", color = SonHarfBlue, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            }
            Image(
                painter = painterResource(castleRes),
                contentDescription = null,
                modifier = Modifier.size(48.dp).graphicsLayer {
                    scaleX = castleScale
                    scaleY = castleScale
                    rotationZ = castleRotation
                },
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun SiegeTerritoryBar(
    myTerritory: Int,
    botTerritory: Int,
    round: Int,
    turn: SiegeTurn,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        color = Color(0xFF0D223A).copy(alpha = .94f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .13f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("MAVİ $myTerritory", color = Color(0xFF70B8FF), fontSize = 9.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Text(
                    when (turn) {
                        SiegeTurn.PLAYER -> "SIRA SENDE"
                        SiegeTurn.BOT -> "BOT HAMLESİ"
                        SiegeTurn.FINISHED -> "MAÇ BİTTİ"
                    },
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.weight(1f))
                Text("KIRMIZI $botTerritory", color = Color(0xFFFF8BA0), fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape)) {
                Box(Modifier.weight(myTerritory.coerceAtLeast(1).toFloat()).fillMaxHeight().background(SonHarfBlue))
                Box(Modifier.weight(botTerritory.coerceAtLeast(1).toFloat()).fillMaxHeight().background(SonHarfPink))
            }
            Text("TUR $round/15", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Color.White.copy(alpha = .55f), fontSize = 7.sp)
        }
    }
}

@Composable
private fun SiegeBoard(
    board: List<SiegeBoardTile?>,
    territory: List<Int>,
    bonuses: Map<Int, SiegeBonus>,
    lastCaptured: List<Int>,
    modifier: Modifier = Modifier,
) {
    val tr = remember { Locale.forLanguageTag("tr-TR") }
    val transition = rememberInfiniteTransition(label = "siege-board")
    val capturePulse by transition.animateFloat(
        initialValue = .96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "capture-pulse",
    )
    val fogAlpha by transition.animateFloat(
        initialValue = .55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "fog-alpha",
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF10253D),
        border = BorderStroke(2.dp, Color(0xFF294B6D)),
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.fillMaxSize().padding(5.dp)) {
            repeat(9) { row ->
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    repeat(9) { col ->
                        val index = row * 9 + col
                        val tile = board[index]
                        val owner = territory[index]
                        val bonus = bonuses[index]
                        val isRiver = col == 4
                        val cellBrush = when (owner) {
                            1 -> Brush.linearGradient(listOf(Color(0xFF2A8BF2), Color(0xFF1769E0)))
                            2 -> Brush.linearGradient(listOf(Color(0xFFF06A7D), Color(0xFFD94761)))
                            else -> if (isRiver) {
                                Brush.linearGradient(listOf(Color(0xFF9FDBF1), Color(0xFF6FB8DB)))
                            } else {
                                Brush.linearGradient(listOf(Color(0xFFDCEFD2), Color(0xFFBFDCAE)))
                            }
                        }
                        val border = when (owner) {
                            1 -> Color(0xFF8DCBFF)
                            2 -> Color(0xFFFFA3B1)
                            else -> Color(0xFF9DB6A1)
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(1.dp)
                                .graphicsLayer {
                                    val scale = if (index in lastCaptured) capturePulse else 1f
                                    scaleX = scale
                                    scaleY = scale
                                },
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Transparent,
                            border = BorderStroke(if (index in lastCaptured) 1.5.dp else .7.dp, border),
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(cellBrush),
                                contentAlignment = Alignment.Center,
                            ) {
                                when {
                                    tile != null -> Surface(
                                        shape = RoundedCornerShape(5.dp),
                                        color = Color(0xFFFFF4D8),
                                        border = BorderStroke(1.dp, Color(0xFFD8B977)),
                                        shadowElevation = 2.dp,
                                    ) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                tile.letter.toString().uppercase(tr),
                                                color = Color(0xFF192A3B),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black,
                                            )
                                        }
                                    }
                                    bonus == SiegeBonus.CASTLE -> Image(
                                        painter = painterResource(R.drawable.siege_castle_neutral),
                                        contentDescription = "Kale",
                                        modifier = Modifier.fillMaxSize(.88f),
                                        contentScale = ContentScale.Fit,
                                    )
                                    bonus == SiegeBonus.TREASURE -> Image(
                                        painter = painterResource(R.drawable.siege_treasure),
                                        contentDescription = "Hazine",
                                        modifier = Modifier.fillMaxSize(.72f),
                                        contentScale = ContentScale.Fit,
                                    )
                                    bonus == SiegeBonus.BRIDGE -> Image(
                                        painter = painterResource(R.drawable.siege_bridge),
                                        contentDescription = "Köprü",
                                        modifier = Modifier.fillMaxSize(.9f),
                                        contentScale = ContentScale.Fit,
                                    )
                                    bonus == SiegeBonus.FOG -> Image(
                                        painter = painterResource(R.drawable.siege_fog),
                                        contentDescription = "Sisli Bölge",
                                        modifier = Modifier.fillMaxSize(.86f).graphicsLayer { alpha = fogAlpha },
                                        contentScale = ContentScale.Fit,
                                    )
                                    bonus == SiegeBonus.DOUBLE -> Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF0D5FBF),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = .65f)),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("2x", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                    isRiver -> Text("≈", color = Color.White.copy(alpha = .7f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }

                                if (index in lastCaptured) {
                                    Surface(
                                        modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(8.dp),
                                        shape = CircleShape,
                                        color = Color(0xFFFFD45B),
                                    ) {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SiegeLegend() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF10253D),
        border = BorderStroke(1.dp, Color(0xFF294B6D)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SiegeLegendItem(R.drawable.siege_castle_neutral, "Kale", "+5")
            SiegeLegendItem(R.drawable.siege_treasure, "Hazine", "+7")
            SiegeLegendItem(R.drawable.siege_bridge, "Köprü", "yol açar")
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(shape = CircleShape, color = SonHarfBlue, modifier = Modifier.size(22.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("2x", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }
                Text("2x Kelime", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                Text("çarpan", color = Color.White.copy(alpha = .55f), fontSize = 6.sp)
            }
            SiegeLegendItem(R.drawable.siege_fog, "Sis", "gizli")
        }
    }
}

@Composable
private fun SiegeLegendItem(iconRes: Int, label: String, detail: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(iconRes), null, Modifier.size(22.dp), contentScale = ContentScale.Fit)
        Text(label, color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
        Text(detail, color = Color.White.copy(alpha = .55f), fontSize = 6.sp)
    }
}

@Composable
private fun SiegeRack(
    rack: List<Char>,
    selected: List<Int>,
    enabled: Boolean,
    onTile: (Int) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        rack.forEachIndexed { index, char ->
            val used = index in selected
            val tileScale by animateFloatAsState(
                targetValue = if (used) .88f else 1f,
                animationSpec = tween(130),
                label = "rack-$index",
            )
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(47.dp)
                    .graphicsLayer { scaleX = tileScale; scaleY = tileScale }
                    .clickable(enabled = enabled && !used) { onTile(index) },
                shape = RoundedCornerShape(9.dp),
                color = if (used) Color(0xFFE3E8EF) else Color(0xFFFFF2D2),
                border = BorderStroke(1.dp, if (used) Color(0xFFCBD5E1) else Color(0xFFD4AC5D)),
                shadowElevation = if (used) 0.dp else 3.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        char.toString(),
                        color = if (used) SonHarfMuted else Color(0xFF192A3B),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}
