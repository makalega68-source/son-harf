package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.getWordSiegeGames
import com.sonharf.game.data.inviteFriendToWordSiege
import kotlinx.coroutines.launch

/** What the PLAY tab knows about the player's Kelime Kuşatması games, read from the server. */
internal data class PlayHubSiegeSummary(
    val activeGames: Int = 0,
    val yourTurnGames: Int = 0,
    val lastRivalId: String? = null,
    val lastRivalName: String? = null,
)

/**
 * The PLAY tab: Kelime Kuşatması first (quick match, friend, private room, rematch, practice),
 * the other game modes below it with less visual weight. Every action reuses an existing flow.
 */
@Composable
internal fun PlayHubScreen(
    backend: OnlineGameBackend,
    onQuickMatch: () -> Unit,
    onPractice: () -> Unit,
    onMyGames: () -> Unit,
    onFriends: () -> Unit,
    onPrivateRoom: () -> Unit,
    onLastLetter: () -> Unit,
    onLetterPath: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var summary by remember { mutableStateOf(PlayHubSiegeSummary()) }
    var notice by remember { mutableStateOf<String?>(null) }
    var rematchBusy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val me = backend.currentUserId() ?: return@LaunchedEffect
        val games = runCatching { backend.getWordSiegeGames() }.getOrNull() ?: return@LaunchedEffect
        val active = games.filter { it.status == "playing" || it.status == "waiting" }
        val lastFinished = games
            .filter { it.status == "finished" && it.playerTwoId != null }
            .maxByOrNull { it.finishedAt ?: it.updatedAt }
        val rivalId = lastFinished?.let { if (it.playerOneId == me) it.playerTwoId else it.playerOneId }
        val rivalName = rivalId?.let { id -> runCatching { backend.getProfile(id).displayName }.getOrNull() }
        summary = PlayHubSiegeSummary(
            activeGames = active.size,
            yourTurnGames = active.count { it.status == "playing" && it.currentPlayerId == me },
            lastRivalId = rivalId,
            lastRivalName = rivalName,
        )
    }

    PlayHubContent(
        summary = summary,
        notice = notice,
        rematchBusy = rematchBusy,
        onQuickMatch = onQuickMatch,
        onPractice = onPractice,
        onMyGames = onMyGames,
        onFriends = onFriends,
        onPrivateRoom = onPrivateRoom,
        onRematch = {
            val rivalId = summary.lastRivalId
            if (rivalId == null || rematchBusy) return@PlayHubContent
            rematchBusy = true
            scope.launch {
                // The server only allows invites between friends; say so instead of failing silently.
                notice = runCatching {
                    backend.inviteFriendToWordSiege(rivalId, if (SonHarfUiState.isEnglish) "en" else "tr")
                }.fold(
                    onSuccess = { gameText("Rövanş daveti gönderildi.", "Rematch invite sent.") },
                    onFailure = { error ->
                        val raw = error.message.orEmpty()
                        when {
                            "not_friends" in raw -> gameText(
                                "Rövanş daveti için önce arkadaş olmalısınız. Sosyal sekmesinden istek gönderebilirsin.",
                                "You need to be friends to send a rematch. Send a request from the Social tab.",
                            )
                            "invite_pending" in raw -> gameText("Bu oyuncuya zaten bekleyen bir davetin var.", "You already have a pending invite to this player.")
                            else -> wordSiegeFriendlyError(raw)
                        }
                    },
                )
                rematchBusy = false
            }
        },
        onLastLetter = onLastLetter,
        onLetterPath = onLetterPath,
    )
}

@Composable
internal fun PlayHubContent(
    summary: PlayHubSiegeSummary,
    notice: String?,
    rematchBusy: Boolean,
    onQuickMatch: () -> Unit,
    onPractice: () -> Unit,
    onMyGames: () -> Unit,
    onFriends: () -> Unit,
    onPrivateRoom: () -> Unit,
    onRematch: () -> Unit,
    onLastLetter: () -> Unit,
    onLetterPath: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = GameSpacing.ScreenHorizontal, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "play-hero") { PlayHubSiegeHero(summary, onQuickMatch) }

        if (summary.activeGames > 0) {
            item(key = "play-active") { PlayHubActiveGames(summary, onMyGames) }
        }

        item(key = "play-options") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PlayHubOption(
                        icon = Icons.Rounded.Groups,
                        title = gameText("Arkadaşla Oyna", "Play a Friend"),
                        detail = gameText("Arkadaşına kuşatma daveti", "Send a siege invite"),
                        accent = GameColors.TacticalTurquoise,
                        onClick = onFriends,
                        modifier = Modifier.weight(1f),
                    )
                    PlayHubOption(
                        icon = Icons.Rounded.Replay,
                        title = gameText("Rövanş", "Rematch"),
                        detail = summary.lastRivalName?.let { gameText("Son rakip: $it", "Last rival: $it") }
                            ?: gameText("Henüz biten maç yok", "No finished match yet"),
                        accent = GameColors.RewardAmber,
                        onClick = onRematch,
                        enabled = summary.lastRivalId != null && !rematchBusy,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PlayHubOption(
                        icon = Icons.Rounded.SmartToy,
                        title = gameText("Antrenman", "Practice"),
                        detail = gameText("Bota karşı • rating etkilenmez", "Versus bot • unrated"),
                        accent = GameColors.PrimaryBlue,
                        onClick = onPractice,
                        modifier = Modifier.weight(1f),
                    )
                    PlayHubOption(
                        icon = Icons.Rounded.VpnKey,
                        title = gameText("Özel Oda", "Private Room"),
                        detail = gameText("Kodla oda • Son Harf", "Room code • Last Letter"),
                        accent = GameColors.Lavender,
                        onClick = onPrivateRoom,
                        badge = "PRO",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        notice?.let { message ->
            item(key = "play-notice") {
                Surface(
                    shape = GameShapes.Medium,
                    color = GameColors.PrimarySurface,
                    border = BorderStroke(1.dp, GameColors.Border),
                ) {
                    Text(
                        message,
                        Modifier.fillMaxWidth().padding(12.dp),
                        color = GameColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        item(key = "play-other-title") {
            Text(
                gameText("DİĞER OYUN MODLARI", "OTHER GAME MODES"),
                Modifier.padding(top = 6.dp),
                color = GameColors.TextTertiary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
        }
        item(key = "play-last-letter") {
            PlayHubModeRow(
                icon = Icons.Rounded.Bolt,
                title = gameText("Son Harf", "Last Letter"),
                detail = gameText("Hızlı rekabetçi kelime düellosu", "Fast competitive word duel"),
                accent = GameColors.PrimaryBlue,
                onClick = onLastLetter,
            )
        }
        item(key = "play-letter-path") {
            PlayHubModeRow(
                icon = Icons.Rounded.Route,
                title = gameText("Harf Yolu", "Letter Path"),
                detail = gameText("Harf harf kelime rotası", "A word route, letter by letter"),
                accent = GameColors.TacticalTurquoise,
                onClick = onLetterPath,
            )
        }
        item { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
private fun PlayHubSiegeHero(summary: PlayHubSiegeSummary, onQuickMatch: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = GameShapes.Hero,
        color = Color.Transparent,
        border = BorderStroke(1.dp, GameColors.TacticalTurquoise.copy(alpha = .45f)),
        shadowElevation = GameElevation.Medium,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(GameColors.HeroStart, GameColors.HeroMiddle, GameColors.HeroEnd)))
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        gameText("ANA OYUN", "MAIN GAME"),
                        color = GameColors.TacticalTurquoise,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "KELİME KUŞATMASI",
                        color = GameColors.TextPrimary,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        gameText("Kelime kur, bölge al, haritayı kontrol et.", "Build words, take territory, control the map."),
                        color = GameColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Surface(shape = CircleShape, color = GameColors.TacticalTurquoise.copy(alpha = .16f)) {
                    Icon(
                        Icons.Rounded.GridView,
                        contentDescription = null,
                        tint = GameColors.TacticalTurquoise,
                        modifier = Modifier.padding(12.dp).size(30.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            GamePrimaryButton(
                text = gameText("HIZLI EŞLEŞME", "QUICK MATCH"),
                onClick = onQuickMatch,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                icon = Icons.Rounded.PlayArrow,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                gameText(
                    "15 sn içinde rakip bulunamazsa seviyene uygun bir botla başlarsın; bot maçı rating'i etkilemez.",
                    "If no rival is found within 15 s, you start against a bot matched to you; bot games are unrated.",
                ),
                color = GameColors.TextTertiary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun PlayHubActiveGames(summary: PlayHubSiegeSummary, onMyGames: () -> Unit) {
    val yourTurn = summary.yourTurnGames > 0
    Surface(
        onClick = onMyGames,
        modifier = Modifier.fillMaxWidth(),
        shape = GameShapes.Large,
        color = GameColors.PrimarySurface,
        border = BorderStroke(1.dp, if (yourTurn) GameColors.PlayGreen.copy(alpha = .55f) else GameColors.Border),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = (if (yourTurn) GameColors.PlayGreen else GameColors.PrimaryBlue).copy(alpha = .15f)) {
                Icon(
                    if (yourTurn) Icons.Rounded.NotificationsActive else Icons.Rounded.HourglassTop,
                    contentDescription = null,
                    tint = if (yourTurn) GameColors.PlayGreen else GameColors.PrimaryBlue,
                    modifier = Modifier.padding(9.dp).size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    gameText("Maçlarım", "My Games"),
                    color = GameColors.TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    if (yourTurn) {
                        gameText(
                            "${summary.activeGames} aktif maç • ${summary.yourTurnGames} maçta sıra sende",
                            "${summary.activeGames} active • your turn in ${summary.yourTurnGames}",
                        )
                    } else {
                        gameText("${summary.activeGames} aktif maç • rakip bekleniyor", "${summary.activeGames} active • waiting on rivals")
                    },
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                gameText("DEVAM ET", "CONTINUE"),
                color = if (yourTurn) GameColors.PlayGreen else GameColors.PrimaryBlue,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun PlayHubOption(
    icon: ImageVector,
    title: String,
    detail: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    badge: String? = null,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 112.dp),
        shape = GameShapes.Large,
        color = GameColors.PrimarySurface,
        border = BorderStroke(1.dp, accent.copy(alpha = if (enabled) .38f else .14f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = GameShapes.Small, color = accent.copy(alpha = if (enabled) .15f else .07f)) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (enabled) accent else GameColors.DisabledContent,
                        modifier = Modifier.padding(8.dp).size(22.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                if (badge != null) {
                    Surface(shape = GameShapes.Pill, color = GameColors.PrestigeGold.copy(alpha = .16f)) {
                        Text(
                            badge,
                            Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            color = GameColors.PrestigeGold,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                title,
                color = if (enabled) GameColors.TextPrimary else GameColors.DisabledContent,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                detail,
                color = GameColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PlayHubModeRow(
    icon: ImageVector,
    title: String,
    detail: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = GameShapes.Medium,
        color = GameColors.ElevatedBackground,
        border = BorderStroke(1.dp, GameColors.Divider),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = GameColors.TextPrimary, style = MaterialTheme.typography.labelLarge)
                Text(detail, color = GameColors.TextSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = GameColors.TextTertiary)
        }
    }
}
