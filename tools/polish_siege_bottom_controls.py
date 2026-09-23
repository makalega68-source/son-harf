from pathlib import Path

ROOT = Path('.')
GAME = ROOT / 'app/src/main/java/com/sonharf/game'


def read(path):
    return Path(path).read_text()


def write(path, text):
    Path(path).write_text(text)


def require_replace(text, old, new, label, count=1):
    found = text.count(old)
    if found < count:
        raise SystemExit(f'{label}: expected at least {count} match(es), found {found}')
    return text.replace(old, new, count)

# 1) Shared score cards and compact actions.
path = GAME / 'WordSiegeGameUi.kt'
text = read(path)
text = require_replace(text, 'modifier = modifier.height(96.dp),', 'modifier = modifier.height(92.dp),', 'score card height')
old_avatar = '''                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = accent.copy(alpha = .075f),
                ) {
                    Box(Modifier.padding(2.dp)) {
                        ProfilePhotoAvatarWithGender(
                            avatarPath = avatarPath, gender = gender, name = name,
                            size = 50.dp, accent = accent, visible = avatarVisible,
                        )
                    }
                }
'''
new_avatar = '''                Box {
                    ProfilePhotoAvatarWithGender(
                        avatarPath = avatarPath, gender = gender, name = name,
                        size = 52.dp, accent = accent, visible = avatarVisible,
                    )
                    if (leading) {
                        Box(Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp)) {
                            WordSiegeLeaderCrown()
                        }
                    }
                }
'''
text = require_replace(text, old_avatar, new_avatar, 'avatar chrome')
old_crown = '''                if (leading) {
                    WordSiegeLeaderCrown()
                    Spacer(Modifier.width(4.dp))
                }
'''
text = require_replace(text, old_crown, '', 'inline crown')
text = require_replace(text, '.width(58.dp)\n                        .height(48.dp)', '.width(52.dp)\n                        .height(46.dp)', 'score box size')
text = require_replace(text, 'fontSize = 12.sp,\n                        lineHeight = 14.sp,', 'fontSize = 11.sp,\n                        lineHeight = 13.sp,', 'profile name size')
text = require_replace(text, 'modifier = modifier.height(48.dp).padding(horizontal = 2.dp),', 'modifier = modifier.height(40.dp).padding(horizontal = 1.dp),', 'compact action height')
text = require_replace(text, 'shape = RoundedCornerShape(12.dp),\n        contentPadding = PaddingValues(2.dp),', 'shape = RoundedCornerShape(10.dp),\n        contentPadding = PaddingValues(1.dp),', 'compact action shape')
text = require_replace(text, 'Icon(icon, null, Modifier.size(17.dp))\n            Text(label, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)', 'Icon(icon, null, Modifier.size(15.dp))\n            Text(label, fontSize = 9.sp, lineHeight = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)', 'compact action typography')
write(path, text)

# 2) Add compact PRO bag controls and shared bottom actions.
new_file = GAME / 'WordSiegeCompactBag.kt'
new_file.write_text(r'''package com.sonharf.game

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
            tint = if (proAccess == true) WordSiegeGameUi.Gold else WordSiegeGameUi.Navy,
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
                else -> Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    rows.chunked(6).forEach { group ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            group.forEach { (letter, count) ->
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = WordSiegeGameUi.SurfaceSoft,
                                    border = BorderStroke(1.dp, WordSiegeGameUi.Border),
                                ) {
                                    Text(
                                        "$letter $count",
                                        Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                                        color = WordSiegeGameUi.Text,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                    )
                                }
                            }
                            repeat(6 - group.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(sh("KAPAT", "CLOSE"), fontWeight = FontWeight.Black) } },
    )
}
''')

# 3) Slow score flights slightly, keeping the flow brisk.
path = GAME / 'WordSiegeCaptureMotion.kt'
text = read(path)
text = require_replace(text, 'WORD_SIEGE_CAPTURE_FLIGHT_MS = 540', 'WORD_SIEGE_CAPTURE_FLIGHT_MS = 720', 'flight duration')
text = require_replace(text, 'WORD_SIEGE_CAPTURE_STAGGER_MS = 85L', 'WORD_SIEGE_CAPTURE_STAGGER_MS = 95L', 'flight stagger')
# Keep the label bright a little longer.
text = text.replace('progress < .20f', 'progress < .26f')
write(path, text)

# 4) Practice screen: move turn strip to bottom and make CTA compact with help/bag.
path = GAME / 'WordSiegePracticeScreen.kt'
text = read(path)
turn_start_marker = '''                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (displayedOwner == 1) PracticePlayerAccent else PracticeRivalFill.copy(alpha = .42f),
'''
turn_start = text.find(turn_start_marker)
turn_end_marker = '\n\n                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {'
turn_end = text.find(turn_end_marker, turn_start)
if turn_start < 0 or turn_end < 0:
    raise SystemExit('practice top turn strip markers not found')
text = text[:turn_start] + text[turn_end:]

# Remove redundant bag text from the tiny preview row.
old_bag = '''                        Spacer(Modifier.weight(1f))
                        Text(
                            sh("Torba ${state.bag.length}", "Bag ${state.bag.length}"),
                            color = WordSiegeGameUi.Muted,
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            maxLines = 1,
                        )
'''
text = require_replace(text, old_bag, '                        Spacer(Modifier.weight(1f))\n', 'practice redundant bag count')

old_confirm = '''                    Row(Modifier.fillMaxWidth()) {
                        Button(
                            onClick = ::applyPlayerMove,
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                            enabled = canPlayerAct && placements.isNotEmpty(),
                            modifier = Modifier.weight(1f).height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PracticePlayerAccent,
                                contentColor = Color.White,
                                disabledContainerColor = WordSiegeGameUi.DisabledBackground,
                                disabledContentColor = WordSiegeGameUi.DisabledContent,
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                        ) {
                            Text(sh("HAMLEYİ ONAYLA", "CONFIRM MOVE"), fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    }
'''
new_confirm = '''                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        WordSiegeSideAction(
                            sh("YARDIM", "HELP"),
                            Icons.Rounded.HelpOutline,
                            modifier = Modifier.width(74.dp),
                        ) { tutorialStep = 0 }
                        Button(
                            onClick = ::applyPlayerMove,
                            shape = RoundedCornerShape(10.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                            enabled = canPlayerAct && placements.isNotEmpty(),
                            modifier = Modifier.weight(1f).height(40.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PracticePlayerAccent,
                                contentColor = Color.White,
                                disabledContainerColor = WordSiegeGameUi.DisabledBackground,
                                disabledContentColor = WordSiegeGameUi.DisabledContent,
                            ),
                            contentPadding = PaddingValues(horizontal = 3.dp),
                        ) {
                            Text(sh("HAMLEYİ ONAYLA", "CONFIRM MOVE"), fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        }
                        WordSiegePracticeBagButton(
                            bag = state.bag,
                            modifier = Modifier.width(82.dp),
                        )
                    }
'''
text = require_replace(text, old_confirm, new_confirm, 'practice confirm row')

# Put the turn indicator after the existing status bar so it is the lowest game strip.
old_status = '''                if (boardViewportMode == WordSiegeBoardViewportMode.FIT) WordSiegePracticeStatusBar(statusMessage, compact)
'''
new_status = '''                if (boardViewportMode == WordSiegeBoardViewportMode.FIT) WordSiegePracticeStatusBar(statusMessage, compact)
                WordSiegeTurnStrip(
                    text = when {
                        state.status == "finished" && matchmakingFallback -> sh("BOT MAÇI BİTTİ • RAKİP ARAMASI SÜRÜYOR", "BOT MATCH FINISHED • MATCHMAKING CONTINUES")
                        state.status == "finished" -> sh("ALIŞTIRMA BİTTİ", "PRACTICE FINISHED")
                        botThinking -> sh("${botProfile.name.uppercase()} HAMLESİNİ HAZIRLIYOR", "${botProfile.name.uppercase()} IS PREPARING A MOVE")
                        displayedOwner == 1 -> sh("SIRA SENDE • Kelimeni oluştur", "YOUR TURN • Build your word")
                        else -> sh("${botProfile.name.uppercase()} OYNUYOR", "${botProfile.name.uppercase()} IS PLAYING")
                    },
                    playerTurn = displayedOwner == 1 && !botThinking,
                    playerAccent = PracticePlayerAccent,
                    rivalAccent = PracticeRivalAccent,
                )
'''
text = require_replace(text, old_status, new_status, 'practice bottom turn strip')
write(path, text)

# 5) Live match: compact action/CTA row, bottom status, no duplicate board chat FAB/bag count.
path = GAME / 'WordSiegePanMatch.kt'
text = read(path)
old_bag_live = '''                    Spacer(Modifier.weight(1f))
                    Text(sh("Torba ${game.bag.length}", "Bag ${game.bag.length}"), color = WordSiegeGameUi.Muted, fontSize = 8.sp)
'''
text = require_replace(text, old_bag_live, '                    Spacer(Modifier.weight(1f))\n', 'live redundant bag count')
old_live_confirm = '''            Row(Modifier.fillMaxWidth()) {
                Button(
                    onClick = onSubmit,
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                    enabled = canAct && placements.isNotEmpty(),
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PanSiegeMineBorder,
                        contentColor = Color.White,
                        disabledContainerColor = WordSiegeGameUi.DisabledBackground,
                        disabledContentColor = WordSiegeGameUi.DisabledContent,
                    ),
                    contentPadding = PaddingValues(horizontal = 5.dp),
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text(sh("HAMLEYİ ONAYLA", "CONFIRM MOVE"), fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
            }
'''
new_live_confirm = '''            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WordSiegeSideAction(
                    sh("SOHBET", "CHAT"),
                    Icons.Rounded.Chat,
                    modifier = Modifier.width(74.dp),
                    onClick = onChat,
                )
                Button(
                    onClick = onSubmit,
                    shape = RoundedCornerShape(10.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                    enabled = canAct && placements.isNotEmpty(),
                    modifier = Modifier.weight(1f).height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PanSiegeMineBorder,
                        contentColor = Color.White,
                        disabledContainerColor = WordSiegeGameUi.DisabledBackground,
                        disabledContentColor = WordSiegeGameUi.DisabledContent,
                    ),
                    contentPadding = PaddingValues(horizontal = 3.dp),
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text(sh("HAMLEYİ ONAYLA", "CONFIRM MOVE"), fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1)
                }
                WordSiegeOnlineBagButton(
                    game = game,
                    modifier = Modifier.width(82.dp),
                )
            }
'''
text = require_replace(text, old_live_confirm, new_live_confirm, 'live confirm row')

# Remove floating chat from the board; chat now has a fixed bottom control.
chat_start_marker = '''        SmallFloatingActionButton(
            onClick = onChat,
'''
chat_start = text.find(chat_start_marker)
if chat_start >= 0:
    # Find the full block by counting parentheses from SmallFloatingActionButton(
    i = chat_start
    depth = 0
    started = False
    while i < len(text):
        ch = text[i]
        if ch == '(':
            depth += 1
            started = True
        elif ch == ')':
            depth -= 1
            if started and depth == 0:
                end = i + 1
                # consume a trailing newline
                if end < len(text) and text[end] == '\n': end += 1
                text = text[:chat_start] + text[end:]
                break
        i += 1

# Add compact bottom turn status after last-move info.
old_tail = '''        notice?.let { PanSiegeNotice(it) }
        if (boardViewportMode == WordSiegeBoardViewportMode.FIT) lastMove?.let { PanSiegeLastMoveInfo(it) }
'''
new_tail = '''        notice?.let { PanSiegeNotice(it) }
        if (boardViewportMode == WordSiegeBoardViewportMode.FIT) lastMove?.let { PanSiegeLastMoveInfo(it) }
        if (game.status == "playing") {
            WordSiegeTurnStrip(
                text = if (visualMyTurn) sh("SIRA SENDE • Kelimeni oluştur", "YOUR TURN • Build your word") else sh("RAKİP OYNUYOR", "RIVAL IS PLAYING"),
                playerTurn = visualMyTurn,
                playerAccent = PanSiegeMineBorder,
                rivalAccent = PanSiegeRivalBorder,
            )
        }
'''
text = require_replace(text, old_tail, new_tail, 'live bottom turn strip')
write(path, text)

# 6) Keep the existing PRO score calculator, but only show its full panel while building a word.
path = GAME / 'WordSiegePremiumPanel.kt'
text = read(path)
marker = '''    val resolvedAccess = access ?: return
    val hasAnyPremiumTool = resolvedAccess.scoreCalculatorAccess || resolvedAccess.letterTableAccess
'''
replacement = '''    val resolvedAccess = access ?: return
    if (placements.isEmpty()) return
    val hasAnyPremiumTool = resolvedAccess.scoreCalculatorAccess || resolvedAccess.letterTableAccess
'''
text = require_replace(text, marker, replacement, 'premium panel collapse')
write(path, text)

# 7) Version bump so phone package manager clearly installs the new build.
path = ROOT / 'app/build.gradle.kts'
text = read(path)
text = require_replace(text, 'versionCode = 32', 'versionCode = 33', 'version code')
text = require_replace(text, 'versionName = "0.9.16"', 'versionName = "0.9.17"', 'version name')
write(path, text)

print('Siege bottom controls polish applied.')
