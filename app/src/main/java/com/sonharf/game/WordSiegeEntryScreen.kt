package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.sonharf.game.data.findOrCreateWordSiegeGame
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.VipEntitlementsDto
import com.sonharf.game.data.WordSiegeGameDto
import com.sonharf.game.data.WordSiegeLaunchConfig
import com.sonharf.game.data.getVipEntitlements
import com.sonharf.game.data.getWordSiegeGames
import com.sonharf.game.data.getWordSiegeSeriesGames

private enum class WordSiegeEntryMode { STANDARD, SERIES, AI }
private enum class WordSiegeLibraryTab { ACTIVE, FINISHED }

private val SiegeEntryCardShape = RoundedCornerShape(18.dp)
private val SiegeEntryControlShape = RoundedCornerShape(13.dp)

/**
 * Active Kelime Tahtı entrance. Brand/logo artwork is intentionally not embedded here:
 * the layout leaves branding independent so final logo assets can be swapped later without
 * rebuilding navigation or gameplay surfaces.
 */
@Composable
internal fun WordSiegeEntryScreen(
    onExit: () -> Unit,
    onOpenStore: () -> Unit,
    startQuickDuel: Boolean = false,
) {
    var quickLaunchConsumed by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf<WordSiegeEntryMode?>(
        if (WordSiegeLaunchConfig.pendingGameId == null) null
        else if (WordSiegeLaunchConfig.pendingGameMode == "series") WordSiegeEntryMode.SERIES
        else WordSiegeEntryMode.STANDARD,
    ) }
    var selectedClassicHours by remember { mutableIntStateOf(WordSiegeLaunchConfig.classicTurnHours) }

    when (mode) {
        WordSiegeEntryMode.STANDARD -> {
            WordSiegeLaunchConfig.classicTurnHours = selectedClassicHours
            WordSiegeExperienceScreen(directEntry = true) {
                WordSiegeLaunchConfig.classicTurnHours = 12
                onExit()
            }
            return
        }
        WordSiegeEntryMode.SERIES -> {
            WordSiegeSeriesScreen(verifiedAccess = true, directEntry = true) { onExit() }
            return
        }
        WordSiegeEntryMode.AI -> {
            WordSiegePracticeScreen(onExit = { mode = null })
            return
        }
        null -> Unit
    }

    val backend = remember { OnlineGameBackend() }
    val scope = rememberCoroutineScope()
    var starting by remember { mutableStateOf(false) }
    var launchError by remember { mutableStateOf<String?>(null) }
    fun startClassic(hours: Int) {
        if (starting) return
        starting = true
        launchError = null
        selectedClassicHours = hours
        WordSiegeLaunchConfig.classicTurnHours = hours
        scope.launch {
            gameRequestResult { backend.findOrCreateWordSiegeGame(SonHarfUiState.language, hours) }
                .onSuccess { WordSiegeLaunchConfig.open(it); mode = WordSiegeEntryMode.STANDARD }
                .onFailure { launchError = wordSiegeFriendlyError(it.message.orEmpty()) }
            starting = false
        }
    }
    var entitlements by remember { mutableStateOf<VipEntitlementsDto?>(null) }
    var entitlementError by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }

    BackHandler(onBack = onExit)

    LaunchedEffect(retry) {
        entitlementError = false

        runCatching { backend.getVipEntitlements() }
            .onSuccess { entitlements = it }
            .onFailure { entitlementError = true }

    }

    val access = entitlements
    val seriesOwned = access?.seriesGameAccess == true
    LaunchedEffect(startQuickDuel, seriesOwned) {
        if (startQuickDuel && seriesOwned && mode == null && !quickLaunchConsumed) {
            quickLaunchConsumed = true
            mode = WordSiegeEntryMode.SERIES
        }
    }
    // Quick Duel is a paid mode; one rewarded video opens five games to try it (non-PRO only).
    var showQuickOffer by remember { mutableStateOf(false) }
    var quickNotice by remember { mutableStateOf<String?>(null) }
    val quickVideo = rememberRewardedVideo(enabled = access != null && !access.isPro && !seriesOwned)
    if (showQuickOffer) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { if (!quickVideo.busy) showQuickOffer = false },
            title = { Text(sh("⚡ HIZLI DÜELLO", "⚡ QUICK DUEL"), fontWeight = FontWeight.Black) },
            text = {
                Column {
                    Text(sh(
                        "3, 5 veya 10 dakikalık hamlelerle hızlı Kuşatma. Kısa bir video izle, 5 maçı ücretsiz oyna (günde 1 kez, 3 gün geçerli).",
                        "Fast Siege with 3, 5 or 10 minute turns. Watch a short video and play 5 matches free (once a day, valid 3 days).",
                    ))
                    quickNotice?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = SonHarfTheme.ActionOrange, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = quickVideo.ready && !quickVideo.busy,
                    onClick = {
                        quickVideo.watch(
                            com.sonharf.game.data.RewardKeys.QUICK_GAMES,
                            onMessage = { quickNotice = it },
                            onRewarded = {
                                showQuickOffer = false
                                retry++
                            },
                        )
                    },
                ) { Text(if (quickVideo.busy) "…" else sh("▶ VİDEO İZLE • 5 OYUN", "▶ WATCH • 5 GAMES"), fontWeight = FontWeight.Black) }
            },
            dismissButton = {
                TextButton(onClick = { showQuickOffer = false; onOpenStore() }) { Text(sh("SATIN AL", "BUY")) }
            },
        )
    }

    Box(
        modifier = Modifier.fillMaxSize().background(LobbyPalette.Ground),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") {
                MainScreenHeader(
                    title = sh("Yeni Kuşatma", "New Siege"),
                    subtitle = sh("Bir kez seç, doğrudan maça gir", "Choose once and go straight to your match"),
                    onBack = onExit,
                )
            }

            if (starting) item { androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth()) }
            launchError?.let { item { Text(it, color = SonHarfTheme.Error) } }

            item(key = "new_game_title") {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        sh("YENİ OYUN", "NEW GAME"),
                        color = LobbyPalette.Ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = .5.sp,
                    )
                    Text(
                        sh("Hamle süresini veya oyun tipini seç.", "Choose a turn timer or game type."),
                        color = LobbyPalette.Muted,
                        fontSize = 10.sp,
                    )
                }
            }

            item(key = "classic_modes") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SiegeModeCard(
                        modifier = Modifier.weight(1f),
                        icon = { Icon(Icons.Rounded.Schedule, null, tint = LobbyPalette.Accent) },
                        title = sh("12 SAAT", "12 HOURS"),
                        subtitle = sh("Her hamle için 12 saat", "12 hours for each turn"),
                        accent = LobbyPalette.Accent,
                        enabled = !starting,
                        onClick = {
                            startClassic(12)
                        },
                    )
                    SiegeModeCard(
                        modifier = Modifier.weight(1f),
                        icon = { Icon(Icons.Rounded.History, null, tint = LobbyPalette.Gold) },
                        title = sh("24 SAAT", "24 HOURS"),
                        subtitle = sh("Her hamle için 24 saat", "24 hours for each turn"),
                        accent = LobbyPalette.Gold,
                        enabled = !starting,
                        onClick = {
                            startClassic(24)
                        },
                    )
                }
            }

            item(key = "special_modes") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SiegeModeCard(
                        modifier = Modifier.weight(1f),
                        icon = {
                            Icon(
                                if (access != null && !seriesOwned) Icons.Rounded.Lock else Icons.Rounded.Bolt,
                                null,
                                tint = SonHarfTheme.ActionOrange,
                            )
                        },
                        title = sh("HIZLI DÜELLO", "QUICK DUEL"),
                        subtitle = when {
                            entitlementError -> sh("PRO erişimi doğrulanamadı", "PRO access could not be verified")
                            access == null -> sh("HIZLI DÜELLO • kontrol ediliyor", "QUICK DUEL • checking")
                            seriesOwned -> sh("HIZLI DÜELLO • 3 / 5 / 10 dk", "QUICK DUEL • 3 / 5 / 10 min")
                            else -> sh("HIZLI DÜELLO • 🎬 5 oyun ücretsiz", "QUICK DUEL • 🎬 5 free games")
                        },
                        accent = SonHarfTheme.ActionOrange,
                        enabled = !starting && (access != null || entitlementError),
                        onClick = {
                            when {
                                entitlementError -> retry++
                                seriesOwned -> mode = WordSiegeEntryMode.SERIES
                                access?.isPro == false -> { quickNotice = null; showQuickOffer = true }
                                else -> onOpenStore()
                            }
                        },
                    )
                    SiegeModeCard(
                        modifier = Modifier.weight(1f),
                        icon = { Icon(Icons.Rounded.GridView, null, tint = LobbyPalette.Accent) },
                        title = sh("AI İLE OYNA", "PLAY AI"),
                        subtitle = sh("Anında antrenman maçı", "Instant practice match"),
                        accent = LobbyPalette.Accent,
                        enabled = !starting,
                        onClick = { mode = WordSiegeEntryMode.AI },
                    )
                }
            }

            item(key = "pro_note") {
                Surface(
                    shape = SiegeEntryControlShape,
                    color = LobbyPalette.Soft,
                    border = BorderStroke(1.dp, LobbyPalette.Accent.copy(alpha = .25f)),
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lock, null, tint = LobbyPalette.Accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            sh(
                                "PRO: Hamle Önizleme ve Kalan Harfler maç içinde konfor aracı olarak açılır.",
                                "PRO: Move Preview and Letters Left open in the match as comfort tools.",
                            ),
                            color = LobbyPalette.Muted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SiegeLibraryTabButton(
    selected: Boolean,
    label: String,
    count: Int,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = SiegeEntryControlShape,
        colors = ButtonDefaults.buttonColors(
            // Solid brand green when selected so the white label always reads.
            containerColor = if (selected) Color(0xFF2E8B45) else LobbyPalette.Paper,
            contentColor = if (selected) Color.White else LobbyPalette.Ink,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 10.dp),
    ) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.weight(1f))
        Surface(
            shape = RoundedCornerShape(99.dp),
            color = if (selected) Color.White.copy(alpha = .16f) else LobbyPalette.Paper,
        ) {
            Text(
                "$count",
                Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                color = if (selected) Color.White else LobbyPalette.Muted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun SiegeGameLibraryRow(game: WordSiegeGameDto, onClick: () -> Unit) {
    val isSeries = game.gameMode == "series"
    val modeLabel = if (isSeries) {
        sh("Hızlı • ${game.turnDurationMinutes ?: 5} dk", "Quick • ${game.turnDurationMinutes ?: 5} min")
    } else {
        sh("${game.turnDurationHours} saat", "${game.turnDurationHours} hours")
    }
    val statusLabel = when (game.status) {
        "waiting" -> sh("Rakip aranıyor", "Finding rival")
        "playing" -> sh("Devam ediyor", "In progress")
        "finished" -> sh("Tamamlandı", "Finished")
        else -> game.status
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = SiegeEntryControlShape,
        color = LobbyPalette.Paper.copy(alpha = .72f),
        border = BorderStroke(1.dp, LobbyPalette.Line.copy(alpha = .72f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (isSeries) SonHarfTheme.ActionOrange.copy(alpha = .12f) else LobbyPalette.Accent.copy(alpha = .10f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isSeries) Icons.Rounded.Bolt else Icons.Rounded.GridView,
                        null,
                        tint = if (isSeries) SonHarfTheme.ActionOrange else LobbyPalette.Accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (isSeries) sh("Hızlı Düello", "Quick Duel") else sh("Klasik Oyun", "Classic Game"),
                    color = LobbyPalette.Ink,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "$modeLabel • $statusLabel",
                    color = LobbyPalette.Muted,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = LobbyPalette.Muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SiegeModeCard(
    modifier: Modifier,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    accent: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 142.dp),
        shape = SiegeEntryCardShape,
        color = LobbyPalette.Paper,
        border = BorderStroke(1.dp, if (enabled) LobbyPalette.Line else LobbyPalette.Line.copy(alpha = .45f)),
        shadowElevation = if (enabled) 2.dp else 0.dp,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(4.dp).background(accent.copy(alpha = if (enabled) 1f else .35f)))
            Column(
                Modifier.fillMaxWidth().padding(13.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = RoundedCornerShape(11.dp),
                    color = accent.copy(alpha = .10f),
                ) {
                    Box(contentAlignment = Alignment.Center) { icon() }
                }
                Text(
                    title,
                    color = LobbyPalette.Ink.copy(alpha = if (enabled) 1f else .55f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )
                Text(
                    subtitle,
                    color = LobbyPalette.Muted.copy(alpha = if (enabled) 1f else .55f),
                    fontSize = 9.sp,
                    lineHeight = 12.sp,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(1.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        sh("SEÇ", "SELECT"),
                        color = accent.copy(alpha = if (enabled) 1f else .45f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Rounded.ChevronRight, null, tint = accent.copy(alpha = if (enabled) 1f else .45f), modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}
