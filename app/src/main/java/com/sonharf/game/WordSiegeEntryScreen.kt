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
) {
    var mode by remember { mutableStateOf<WordSiegeEntryMode?>(null) }
    var selectedClassicHours by remember { mutableIntStateOf(12) }

    when (mode) {
        WordSiegeEntryMode.STANDARD -> {
            WordSiegeLaunchConfig.classicTurnHours = selectedClassicHours
            WordSiegeExperienceScreen {
                WordSiegeLaunchConfig.classicTurnHours = 12
                mode = null
            }
            return
        }
        WordSiegeEntryMode.SERIES -> {
            WordSiegeSeriesScreen(verifiedAccess = true) { mode = null }
            return
        }
        WordSiegeEntryMode.AI -> {
            WordSiegePracticeScreen(onExit = { mode = null })
            return
        }
        null -> Unit
    }

    val backend = remember { OnlineGameBackend() }
    var entitlements by remember { mutableStateOf<VipEntitlementsDto?>(null) }
    var entitlementError by remember { mutableStateOf(false) }
    var libraryLoading by remember { mutableStateOf(true) }
    var libraryError by remember { mutableStateOf(false) }
    var games by remember { mutableStateOf<List<WordSiegeGameDto>>(emptyList()) }
    var libraryTab by remember { mutableStateOf(WordSiegeLibraryTab.ACTIVE) }
    var retry by remember { mutableIntStateOf(0) }

    BackHandler(onBack = onExit)

    LaunchedEffect(retry) {
        entitlementError = false
        libraryError = false
        libraryLoading = true

        runCatching { backend.getVipEntitlements() }
            .onSuccess { entitlements = it }
            .onFailure { entitlementError = true }

        val classic = runCatching { backend.getWordSiegeGames() }
        val series = runCatching { backend.getWordSiegeSeriesGames() }
        if (classic.isFailure && series.isFailure) {
            libraryError = true
        } else {
            games = (classic.getOrDefault(emptyList()) + series.getOrDefault(emptyList()))
                .distinctBy { it.id }
                .sortedByDescending { it.updatedAt.ifBlank { it.createdAt } }
        }
        libraryLoading = false
    }

    val activeGames = games.filter { it.status == "waiting" || it.status == "playing" }
    val finishedGames = games.filter { it.status == "finished" }
    val shownGames = if (libraryTab == WordSiegeLibraryTab.ACTIVE) activeGames else finishedGames
    val access = entitlements
    val seriesOwned = access?.seriesGameAccess == true

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onExit) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = sh("Geri", "Back"), tint = SonHarfTheme.TextPrimary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            sh("KELİME TAHTI", "WORD THRONE"),
                            color = SonHarfTheme.TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            sh("Oyunlarını yönet veya yeni bir kuşatma başlat.", "Manage your games or start a new siege."),
                            color = SonHarfTheme.TextSecondary,
                            fontSize = 11.sp,
                        )
                    }
                }
            }

            item(key = "my_games") {
                Surface(
                    shape = SiegeEntryCardShape,
                    color = SonHarfTheme.Surface,
                    border = BorderStroke(1.dp, SonHarfTheme.Border),
                    shadowElevation = 2.dp,
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.SportsEsports, null, tint = SonHarfTheme.Primary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                sh("OYUNLARIM", "MY GAMES"),
                                modifier = Modifier.weight(1f),
                                color = SonHarfTheme.TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = .4.sp,
                            )
                            if (libraryLoading) CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp, color = SonHarfTheme.Primary)
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SiegeLibraryTabButton(
                                selected = libraryTab == WordSiegeLibraryTab.ACTIVE,
                                label = sh("DEVAM EDEN", "ACTIVE"),
                                count = activeGames.size,
                                modifier = Modifier.weight(1f),
                                onClick = { libraryTab = WordSiegeLibraryTab.ACTIVE },
                            )
                            SiegeLibraryTabButton(
                                selected = libraryTab == WordSiegeLibraryTab.FINISHED,
                                label = sh("BİTEN", "FINISHED"),
                                count = finishedGames.size,
                                modifier = Modifier.weight(1f),
                                onClick = { libraryTab = WordSiegeLibraryTab.FINISHED },
                            )
                        }

                        when {
                            libraryLoading && games.isEmpty() -> {
                                Text(
                                    sh("Oyunların yükleniyor…", "Loading your games…"),
                                    color = SonHarfTheme.TextSecondary,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                            libraryError -> {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        sh("Oyun listesi yenilenemedi.", "Game list could not refresh."),
                                        modifier = Modifier.weight(1f),
                                        color = SonHarfTheme.TextSecondary,
                                        fontSize = 10.sp,
                                    )
                                    TextButton(onClick = { retry++ }) {
                                        Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(sh("YENİLE", "RETRY"), fontWeight = FontWeight.Black, fontSize = 9.sp)
                                    }
                                }
                            }
                            shownGames.isEmpty() -> {
                                Text(
                                    if (libraryTab == WordSiegeLibraryTab.ACTIVE) {
                                        sh("Devam eden oyunun yok.", "You have no active games.")
                                    } else {
                                        sh("Henüz biten oyunun yok.", "You have no finished games yet.")
                                    },
                                    color = SonHarfTheme.TextSecondary,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                            else -> {
                                shownGames.take(4).forEach { game ->
                                    SiegeGameLibraryRow(
                                        game = game,
                                        onClick = {
                                            if (game.gameMode == "series") {
                                                if (seriesOwned) mode = WordSiegeEntryMode.SERIES else onOpenStore()
                                            } else {
                                                selectedClassicHours = if (game.turnDurationHours == 24) 24 else 12
                                                mode = WordSiegeEntryMode.STANDARD
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "new_game_title") {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        sh("YENİ OYUN", "NEW GAME"),
                        color = SonHarfTheme.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = .5.sp,
                    )
                    Text(
                        sh("Hamle süresini veya oyun tipini seç.", "Choose a turn timer or game type."),
                        color = SonHarfTheme.TextSecondary,
                        fontSize = 10.sp,
                    )
                }
            }

            item(key = "classic_modes") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SiegeModeCard(
                        modifier = Modifier.weight(1f),
                        icon = { Icon(Icons.Rounded.Schedule, null, tint = SonHarfTheme.Primary) },
                        title = sh("12 SAAT", "12 HOURS"),
                        subtitle = sh("Her hamle için 12 saat", "12 hours for each turn"),
                        accent = SonHarfTheme.Primary,
                        onClick = {
                            selectedClassicHours = 12
                            WordSiegeLaunchConfig.classicTurnHours = 12
                            mode = WordSiegeEntryMode.STANDARD
                        },
                    )
                    SiegeModeCard(
                        modifier = Modifier.weight(1f),
                        icon = { Icon(Icons.Rounded.History, null, tint = SonHarfTheme.SoftBlue) },
                        title = sh("24 SAAT", "24 HOURS"),
                        subtitle = sh("Her hamle için 24 saat", "24 hours for each turn"),
                        accent = SonHarfTheme.SoftBlue,
                        onClick = {
                            selectedClassicHours = 24
                            WordSiegeLaunchConfig.classicTurnHours = 24
                            mode = WordSiegeEntryMode.STANDARD
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
                        title = sh("HIZLI OYUN", "QUICK GAME"),
                        subtitle = when {
                            entitlementError -> sh("PRO erişimi doğrulanamadı", "PRO access could not be verified")
                            access == null -> sh("SERİ / HIZLI OYUN • kontrol ediliyor", "SERIES / QUICK GAME • checking")
                            seriesOwned -> sh("SERİ / HIZLI OYUN • 3 / 5 / 10 dk", "SERIES / QUICK GAME • 3 / 5 / 10 min")
                            else -> sh("SERİ / HIZLI OYUN • PRO", "SERIES / QUICK GAME • PRO")
                        },
                        accent = SonHarfTheme.ActionOrange,
                        enabled = access != null || entitlementError,
                        onClick = {
                            when {
                                entitlementError -> retry++
                                seriesOwned -> mode = WordSiegeEntryMode.SERIES
                                else -> onOpenStore()
                            }
                        },
                    )
                    SiegeModeCard(
                        modifier = Modifier.weight(1f),
                        icon = { Icon(Icons.Rounded.GridView, null, tint = SonHarfTheme.Lavender) },
                        title = sh("AI İLE OYNA", "PLAY AI"),
                        subtitle = sh("Anında antrenman maçı", "Instant practice match"),
                        accent = SonHarfTheme.Lavender,
                        onClick = { mode = WordSiegeEntryMode.AI },
                    )
                }
            }

            item(key = "pro_note") {
                Surface(
                    shape = SiegeEntryControlShape,
                    color = SonHarfTheme.SurfaceElevated,
                    border = BorderStroke(1.dp, SonHarfTheme.Lavender.copy(alpha = .25f)),
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lock, null, tint = SonHarfTheme.Lavender, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            sh(
                                "PRO: Puan hesaplama ve kalan harf tablosu oyun içinde açılır.",
                                "PRO: Score calculation and remaining-letter table unlock in the match.",
                            ),
                            color = SonHarfTheme.TextSecondary,
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
            containerColor = if (selected) SonHarfTheme.HeroStart else SonHarfTheme.SurfaceSecondary,
            contentColor = if (selected) Color.White else SonHarfTheme.TextPrimary,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 10.dp),
    ) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.weight(1f))
        Surface(
            shape = RoundedCornerShape(99.dp),
            color = if (selected) Color.White.copy(alpha = .16f) else SonHarfTheme.Surface,
        ) {
            Text(
                "$count",
                Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                color = if (selected) Color.White else SonHarfTheme.TextSecondary,
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
        color = SonHarfTheme.SurfaceSecondary.copy(alpha = .72f),
        border = BorderStroke(1.dp, SonHarfTheme.Border.copy(alpha = .72f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (isSeries) SonHarfTheme.ActionOrange.copy(alpha = .12f) else SonHarfTheme.Primary.copy(alpha = .10f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isSeries) Icons.Rounded.Bolt else Icons.Rounded.GridView,
                        null,
                        tint = if (isSeries) SonHarfTheme.ActionOrange else SonHarfTheme.Primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (isSeries) sh("Hızlı Oyun", "Quick Game") else sh("Klasik Oyun", "Classic Game"),
                    color = SonHarfTheme.TextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "$modeLabel • $statusLabel",
                    color = SonHarfTheme.TextSecondary,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.TextSecondary, modifier = Modifier.size(18.dp))
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
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, if (enabled) SonHarfTheme.Border else SonHarfTheme.Border.copy(alpha = .45f)),
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
                    color = SonHarfTheme.TextPrimary.copy(alpha = if (enabled) 1f else .55f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )
                Text(
                    subtitle,
                    color = SonHarfTheme.TextSecondary.copy(alpha = if (enabled) 1f else .55f),
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
