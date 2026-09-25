package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.WordSiegeGameDto
import com.sonharf.game.data.WordSiegeLetterCountDto
import com.sonharf.game.data.getPremiumWordSiegeLetterTable
import com.sonharf.game.data.getVipEntitlements

@Composable
internal fun WordSiegeSideAction(
    label: String,
    icon: ImageVector,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        border = BorderStroke(1.dp, WordSiegeGameUi.Border),
    ) {
        Icon(icon, null, Modifier.size(15.dp))
        Spacer(Modifier.width(3.dp))
        Text(label, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
internal fun WordSiegeTurnStrip(
    text: String,
    playerTurn: Boolean,
    playerAccent: Color,
    rivalAccent: Color,
    modifier: Modifier = Modifier,
) {
    val accent = if (playerTurn) playerAccent else rivalAccent
    Surface(
        modifier = modifier.fillMaxWidth().height(28.dp),
        shape = RoundedCornerShape(9.dp),
        color = accent.copy(alpha = if (playerTurn) .95f else .11f),
        border = BorderStroke(1.dp, accent.copy(alpha = .42f)),
    ) {
        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 10.dp)) {
            Text(
                text,
                color = if (playerTurn) Color.White else WordSiegeGameUi.Text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun WordSiegeOnlineBagButton(
    game: WordSiegeGameDto,
    modifier: Modifier = Modifier,
) {
    val backend = remember { OnlineGameBackend() }
    var access by remember(game.id) { mutableStateOf<Boolean?>(null) }
    var table by remember(game.id) { mutableStateOf<List<WordSiegeLetterCountDto>>(emptyList()) }
    var show by remember(game.id) { mutableStateOf(false) }
    var failed by remember(game.id) { mutableStateOf(false) }

    LaunchedEffect(game.id, game.moveCount) {
        failed = false
        runCatching { backend.getVipEntitlements() }
            .onSuccess { entitlement ->
                access = entitlement.letterTableAccess
                if (entitlement.letterTableAccess) {
                    runCatching { backend.getPremiumWordSiegeLetterTable(game.id) }
                        .onSuccess { table = it }
                        .onFailure { failed = true; table = emptyList() }
                } else table = emptyList()
            }
            .onFailure { failed = true }
    }

    WordSiegeBagButton(
        count = game.bag.length,
        proAccess = access,
        modifier = modifier,
        onClick = { show = true },
    )

    if (show) {
        WordSiegeBagDialog(
            proAccess = access,
            failed = failed,
            rows = table.map { it.letter to it.remaining },
            onDismiss = { show = false },
        )
    }
}

@Composable
internal fun WordSiegePracticeBagButton(
    bag: String,
    modifier: Modifier = Modifier,
) {
    val backend = remember { runCatching { OnlineGameBackend() }.getOrNull() }
    var access by remember { mutableStateOf<Boolean?>(null) }
    var failed by remember { mutableStateOf(false) }
    var show by remember { mutableStateOf(false) }

    LaunchedEffect(backend) {
        val b = backend ?: run { failed = true; return@LaunchedEffect }
        runCatching { b.getVipEntitlements() }
            .onSuccess { access = it.letterTableAccess }
            .onFailure { failed = true }
    }

    val rows = remember(bag) {
        bag.groupingBy { it }.eachCount().entries
            .sortedBy { it.key.toString() }
            .map { it.key.toString() to it.value }
    }
    WordSiegeBagButton(
        count = bag.length,
        proAccess = access,
        modifier = modifier,
        onClick = { show = true },
    )
    if (show) {
        WordSiegeBagDialog(
            proAccess = access,
            failed = failed,
            rows = rows,
            onDismiss = { show = false },
        )
    }
}

@Composable
private fun WordSiegeBagButton(
    count: Int,
    proAccess: Boolean?,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        border = BorderStroke(1.dp, if (proAccess == true) WordSiegeGameUi.PremiumBorder else WordSiegeGameUi.Border),
    ) {
        Icon(
            if (proAccess == false) Icons.Rounded.Lock else Icons.Rounded.GridView,
            null,
            Modifier.size(14.dp),
            tint = if (proAccess == true) WordSiegeGameUi.Gold else WordSiegeGameUi.Muted,
        )
        Spacer(Modifier.width(3.dp))
        Column(horizontalAlignment = Alignment.Start) {
            Text(sh("TORBA $count", "BAG $count"), fontSize = 8.sp, lineHeight = 9.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text("PRO", fontSize = 6.sp, lineHeight = 7.sp, color = WordSiegeGameUi.Gold, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun WordSiegeBagDialog(
    proAccess: Boolean?,
    failed: Boolean,
    rows: List<Pair<String, Int>>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(if (proAccess == false) Icons.Rounded.Lock else Icons.Rounded.GridView, null, tint = WordSiegeGameUi.Gold) },
        title = { Text(sh("Torbada Kalan Harfler", "Letters Remaining"), fontWeight = FontWeight.Black) },
        text = {
            when {
                failed && proAccess == null -> Text(sh("PRO erişimi şu anda doğrulanamadı.", "PRO access could not be verified right now."), color = WordSiegeGameUi.Muted)
                proAccess != true -> Text(sh("Hangi harflerin kaldığını görmek PRO üyeliğe özeldir.", "Viewing the remaining letters is a PRO feature."), color = WordSiegeGameUi.Muted)
                rows.isEmpty() -> Text(sh("Torbada harf kalmadı.", "The bag is empty."), color = WordSiegeGameUi.Muted)
                else -> Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    val total = rows.sumOf { it.second }
                    Text(
                        sh("Torbada $total harf", "$total tiles in bag"),
                        color = WordSiegeGameUi.Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    rows.chunked(5).forEach { group ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            group.forEach { (letter, count) ->
                                Surface(
                                    modifier = Modifier.weight(1f).height(58.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF0EDE3),
                                    border = BorderStroke(1.dp, WordSiegeGameUi.Border.copy(alpha = .72f)),
                                    shadowElevation = 1.dp,
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Text(
                                            letter,
                                            color = WordSiegeGameUi.Navy,
                                            fontSize = 18.sp,
                                            lineHeight = 19.sp,
                                            fontWeight = FontWeight.Black,
                                            maxLines = 1,
                                        )
                                        Text(
                                            "×$count",
                                            color = WordSiegeGameUi.Muted,
                                            fontSize = 10.sp,
                                            lineHeight = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                            repeat(5 - group.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(sh("KAPAT", "CLOSE"), fontWeight = FontWeight.Black) } },
    )
}
