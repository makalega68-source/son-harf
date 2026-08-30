package com.sonharf.game

import androidx.activity.compose.BackHandler
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
private enum class SiegeBonus { DOUBLE, TRIPLE, SHIELD, SWORD }

private data class SiegeBoardTile(val letter: Char, val owner: Int)
private data class SiegePlacement(
    val row: Int,
    val col: Int,
    val horizontal: Boolean,
    val word: String,
    val newIndices: List<Int>,
    val score: Int,
)

private val siegeWords = listOf(
    "kalem","savaş","kule","masa","araba","kapı","roman","orman","kart",
    "serin","salon","tarla","kitap","taş","saray","kale","liman","metal","merak",
    "martı","simit","resim","insan","sanat","saat","tarih","ana","tan"
)

private val siegeFillers = "AEIİKLMNRSTOU".toList()

private val siegeBonuses = mapOf(
    0 to SiegeBonus.DOUBLE,
    8 to SiegeBonus.TRIPLE,
    20 to SiegeBonus.SHIELD,
    33 to SiegeBonus.SWORD,
    47 to SiegeBonus.DOUBLE,
    57 to SiegeBonus.SWORD,
    72 to SiegeBonus.TRIPLE,
)

private fun siegeNorm(value: String): String =
    value.trim().lowercase(Locale.forLanguageTag("tr-TR"))

private fun initialSiegeBoard(): List<SiegeBoardTile?> {
    val board = MutableList<SiegeBoardTile?>(81) { null }
    val word = "taht"
    val row = 4
    val startCol = 2
    word.forEachIndexed { i, ch -> board[row * 9 + startCol + i] = SiegeBoardTile(ch, 0) }
    return board
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
                SiegeBonus.DOUBLE -> 3
                SiegeBonus.TRIPLE -> 5
                SiegeBonus.SHIELD -> 4
                SiegeBonus.SWORD -> 4
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

private fun siegeDamage(placement: SiegePlacement): Triple<Int, Int, Boolean> {
    var multiplier = 1
    var shield = 0
    var sword = 0
    placement.newIndices.forEach { index ->
        when (siegeBonuses[index]) {
            SiegeBonus.DOUBLE -> multiplier = maxOf(multiplier, 2)
            SiegeBonus.TRIPLE -> multiplier = maxOf(multiplier, 3)
            SiegeBonus.SHIELD -> shield += 8
            SiegeBonus.SWORD -> sword += 5
            null -> Unit
        }
    }
    var damage = placement.word.length * 2 + sword
    if (placement.word.length >= 6) damage += 4
    damage *= multiplier
    return Triple(damage.coerceAtMost(30), shield, multiplier >= 3 || damage >= 18)
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
    var myHp by remember(gameId) { mutableIntStateOf(100) }
    var botHp by remember(gameId) { mutableIntStateOf(100) }
    var myShield by remember(gameId) { mutableIntStateOf(0) }
    var botShield by remember(gameId) { mutableIntStateOf(0) }
    var turn by remember(gameId) { mutableStateOf(SiegeTurn.PLAYER) }
    var round by remember(gameId) { mutableIntStateOf(1) }
    var rack by remember(gameId) { mutableStateOf(rackForBoard(initialSiegeBoard())) }
    var selected by remember(gameId) { mutableStateOf<List<Int>>(emptyList()) }
    var notice by remember(gameId) { mutableStateOf("Harfleri seç ve saldır.") }
    var attackBanner by remember(gameId) { mutableStateOf<String?>(null) }
    var attackIsMine by remember(gameId) { mutableStateOf(true) }
    var crit by remember(gameId) { mutableStateOf(false) }
    var busy by remember(gameId) { mutableStateOf(false) }
    var myProfile by remember { mutableStateOf<ProfileDto?>(null) }

    val word = selected.joinToString("") { rack[it].toString() }
    val gameOver = turn == SiegeTurn.FINISHED

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
                else -> "BERABERE."
            }
        }
    }

    fun submitPlayerWord() {
        if (turn != SiegeTurn.PLAYER || busy) return
        val raw = word
        val normalized = siegeNorm(raw)
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

            val (rawDamage, gainedShield, isCrit) = siegeDamage(placement)
            val absorbed = minOf(botShield, rawDamage)
            val damage = (rawDamage - absorbed).coerceAtLeast(0)
            botShield = (botShield - absorbed).coerceAtLeast(0)
            botHp = (botHp - damage).coerceAtLeast(0)
            myShield += gainedShield
            board = placeSiegeWord(board, placement, 1)
            attackBanner = if (isCrit) "KRİTİK! -$damage HP" else "-$damage HP"
            attackIsMine = true
            crit = isCrit
            notice = normalized.uppercase(tr) + " • Kaleye $damage hasar"
            selected = emptyList()
            SonHarfSoundFx.wordAccepted()
            if (isCrit) SonHarfSoundFx.bonus()

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
        val (rawDamage, gainedShield, isCrit) = siegeDamage(placement)
        val absorbed = minOf(myShield, rawDamage)
        val damage = (rawDamage - absorbed).coerceAtLeast(0)
        myShield = (myShield - absorbed).coerceAtLeast(0)
        myHp = (myHp - damage).coerceAtLeast(0)
        botShield += gainedShield
        board = placeSiegeWord(board, placement, 2)
        attackBanner = if (isCrit) "KRİTİK! -$damage HP" else "-$damage HP"
        attackIsMine = false
        crit = isCrit
        notice = "BOT: ${botWord.uppercase(tr)} • $damage hasar"
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
            Brush.verticalGradient(listOf(Color.White, SonHarfBg, Color(0xFFF5F7FB)))
        )
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onExit) { Icon(Icons.Rounded.ArrowBack, "Geri") }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("KELİME KUŞATMASI", color = SonHarfText, fontSize = 19.sp, fontWeight = FontWeight.Black)
                    Text("BOT ANTRENMANI • Kelimeni kur • kaleye saldır", color = SonHarfMuted, fontSize = 8.sp)
                }
                IconButton(onClick = { gameId += 1 }) { Icon(Icons.Rounded.Refresh, "Yenile") }
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
                )
                Surface(shape = CircleShape, color = SonHarfText, modifier = Modifier.size(38.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("⚔", color = Color.White, fontSize = 18.sp)
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
                )
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("SENİN SALDIRIN", color = SonHarfBlue, fontSize = 8.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Text("BOT SALDIRISI", color = SonHarfPink, fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
            Row(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape)) {
                Box(Modifier.weight(myHp.coerceAtLeast(1).toFloat()).fillMaxHeight().background(SonHarfBlue))
                Box(Modifier.weight(botHp.coerceAtLeast(1).toFloat()).fillMaxHeight().background(SonHarfPink))
            }

            attackBanner?.let { banner ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    color = if (attackIsMine) Color(0xFF15243A) else Color(0xFF44202B),
                    shadowElevation = 5.dp,
                ) {
                    Text(
                        banner,
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        color = if (crit) Color(0xFFFFC34D) else Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                    )
                }
            }

            SiegeBoard(
                board = board,
                bonuses = siegeBonuses,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF8FAFD),
                border = BorderStroke(1.dp, Color(0xFFDCE4EE)),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (word.isBlank()) "Harflerini seç" else word,
                        modifier = Modifier.weight(1f),
                        color = if (word.isBlank()) SonHarfMuted else SonHarfText,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                    )
                    Text("TUR ${round.coerceAtMost(15)}/15", color = SonHarfMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(15.dp),
                ) { Text("RÖVANŞ", fontWeight = FontWeight.Black) }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedButton(
                        onClick = { selected = selected.dropLast(1) },
                        enabled = selected.isNotEmpty() && turn == SiegeTurn.PLAYER,
                        modifier = Modifier.size(48.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) { Icon(Icons.Rounded.Backspace, "Sil") }

                    Button(
                        onClick = ::submitPlayerWord,
                        enabled = selected.size >= 3 && turn == SiegeTurn.PLAYER && !busy,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SonHarfBlue),
                    ) {
                        Text(
                            if (turn == SiegeTurn.BOT) "BOT OYNUYOR…" else "⚔  KELİMEYİ GÖNDER",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            if (selected.isEmpty()) rack = rack.shuffled()
                        },
                        enabled = selected.isEmpty() && turn == SiegeTurn.PLAYER,
                        modifier = Modifier.size(48.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) { Icon(Icons.Rounded.Shuffle, "Karıştır") }
                }
            }

            Text(
                notice,
                Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = if (gameOver) SonHarfGold else SonHarfMuted,
                fontSize = 9.sp,
                fontWeight = if (gameOver) FontWeight.Black else FontWeight.Medium,
            )
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
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        color = accent.copy(alpha = .07f),
        border = BorderStroke(1.5.dp, accent.copy(alpha = .35f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (bot) {
                Surface(Modifier.size(34.dp), shape = CircleShape, color = Color.White, border = BorderStroke(2.dp, accent)) {
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
                    Text(hp.toString(), color = accent, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(" HP", color = SonHarfText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                if (shield > 0) Text("🛡 $shield", color = SonHarfBlue, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            Image(
                painter = painterResource(castleRes),
                contentDescription = null,
                modifier = Modifier.size(50.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun SiegeBoard(
    board: List<SiegeBoardTile?>,
    bonuses: Map<Int, SiegeBonus>,
    modifier: Modifier = Modifier,
) {
    val tr = remember { Locale.forLanguageTag("tr-TR") }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFD5DFEB)),
        shadowElevation = 3.dp,
    ) {
        Column(Modifier.fillMaxSize().padding(4.dp)) {
            repeat(9) { row ->
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    repeat(9) { col ->
                        val index = row * 9 + col
                        val tile = board[index]
                        val bonus = bonuses[index]
                        val bg = when {
                            tile?.owner == 1 -> Color(0xFFFFF6DF)
                            tile?.owner == 2 -> Color(0xFFFFEFF2)
                            tile?.owner == 0 -> Color(0xFFFFF7E5)
                            bonus == SiegeBonus.DOUBLE -> Color(0xFFE6F1FF)
                            bonus == SiegeBonus.TRIPLE -> Color(0xFFFFE7ED)
                            bonus == SiegeBonus.SHIELD -> Color(0xFFEAF3FF)
                            bonus == SiegeBonus.SWORD -> Color(0xFFFFEEF2)
                            else -> Color(0xFFF8FAFD)
                        }
                        val border = when {
                            tile?.owner == 1 -> SonHarfBlue.copy(alpha = .32f)
                            tile?.owner == 2 -> SonHarfPink.copy(alpha = .32f)
                            else -> Color(0xFFDDE5EE)
                        }
                        Surface(
                            modifier = Modifier.weight(1f).fillMaxHeight().padding(1.2.dp),
                            shape = RoundedCornerShape(6.dp),
                            color = bg,
                            border = BorderStroke(0.7.dp, border),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                when {
                                    tile != null -> Text(
                                        tile.letter.toString().uppercase(tr),
                                        color = SonHarfText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                    )
                                    bonus == SiegeBonus.DOUBLE -> Text("2×", color = SonHarfBlue, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                    bonus == SiegeBonus.TRIPLE -> Text("3×", color = SonHarfPink, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                    bonus == SiegeBonus.SHIELD -> Text("🛡", fontSize = 10.sp)
                                    bonus == SiegeBonus.SWORD -> Text("⚔", fontSize = 10.sp)
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
private fun SiegeRack(
    rack: List<Char>,
    selected: List<Int>,
    enabled: Boolean,
    onTile: (Int) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        rack.forEachIndexed { index, char ->
            val used = index in selected
            Surface(
                modifier = Modifier.weight(1f).height(49.dp).clickable(enabled = enabled && !used) { onTile(index) },
                shape = RoundedCornerShape(10.dp),
                color = if (used) Color(0xFFE3E8EF) else Color(0xFFFFF8E8),
                border = BorderStroke(1.dp, if (used) Color(0xFFCBD5E1) else Color(0xFFE3D4AE)),
                shadowElevation = if (used) 0.dp else 2.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        char.toString(),
                        color = if (used) SonHarfMuted else SonHarfText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}
