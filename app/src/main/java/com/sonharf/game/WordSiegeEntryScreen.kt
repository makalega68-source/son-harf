package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.VipEntitlementsDto
import com.sonharf.game.data.WordSiegeLaunchConfig
import com.sonharf.game.data.getVipEntitlements

private enum class WordSiegeEntryMode { STANDARD, SERIES }

/** A shortcut from the PLAY tab straight into a standard-siege flow. */
internal enum class WordSiegeEntryAction { QUICK_MATCH, PRACTICE }

/**
 * Real gameplay entry for Kelime Kuşatması. Paid Series Game is reachable from the same
 * place as the standard arena instead of being hidden behind the store after purchase.
 */
@Composable
internal fun WordSiegeEntryScreen(
    onExit: () -> Unit,
    onOpenStore: () -> Unit,
    initialAction: WordSiegeEntryAction? = null,
    initialGameId: String? = null,
) {
    val shortcut = initialAction != null || initialGameId != null
    var mode by remember(initialAction, initialGameId) {
        mutableStateOf(if (shortcut) WordSiegeEntryMode.STANDARD else null)
    }

    // Turn length for new standard games; the server validates it (12 or 24 hours).
    var selectedClassicHours by remember { mutableIntStateOf(12) }

    when (mode) {
        WordSiegeEntryMode.STANDARD -> {
            WordSiegeLaunchConfig.classicTurnHours = if (shortcut) 12 else selectedClassicHours
            // Opened from a PLAY shortcut: leaving returns there instead of to this chooser.
            ProfessionalWordSiegeExperienceScreen(initialAction = initialAction, initialGameId = initialGameId) {
                WordSiegeLaunchConfig.classicTurnHours = 12
                if (shortcut) onExit() else mode = null
            }
            return
        }
        WordSiegeEntryMode.SERIES -> {
            WordSiegeSeriesScreen(verifiedAccess = true) { mode = null }
            return
        }
        null -> Unit
    }

    val backend = remember { OnlineGameBackend() }
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

    Column(Modifier.fillMaxSize()) {
        GameTopBar(
            title = sh("Kelime Kuşatması", "Word Siege"),
            subtitle = sh(
                "Ana rekabet modunu veya seri formatı seç",
                "Choose the main competitive mode or Series format",
            ),
            onBack = onExit,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = GameSpacing.ScreenHorizontal, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WordSiegeEntryCard(
                iconLocked = false,
                badge = sh("ANA REKABET MODU", "MAIN COMPETITIVE MODE"),
                featured = true,
                title = sh("STANDART KUŞATMA", "STANDARD SIEGE"),
                subtitle = sh(
                    "Klasik Kelime Kuşatması • çevrimiçi 1v1 veya alıştırma botu",
                    "Classic Word Siege • online 1v1 or practice bot",
                ),
                action = sh("KUŞATMAYA GİR", "ENTER SIEGE"),
                accent = GameColors.PlayGreen,
                onClick = { mode = WordSiegeEntryMode.STANDARD },
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(12 to sh("12 SAAT", "12 HOURS"), 24 to sh("24 SAAT", "24 HOURS")).forEach { (hours, label) ->
                    FilterChip(
                        selected = selectedClassicHours == hours,
                        onClick = {
                            selectedClassicHours = hours
                            if (hours == 24) WordSiegeLaunchConfig.classicTurnHours = 24
                        },
                        label = { Text(sh("$label hamle süresi", "$label per turn"), fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = GameColors.PrimarySurface,
                            labelColor = GameColors.TextSecondary,
                            selectedContainerColor = GameColors.PlayGreen.copy(alpha = .18f),
                            selectedLabelColor = GameColors.TextPrimary,
                        ),
                    )
                }
            }

            val access = entitlements
            val seriesOwned = access?.seriesGameAccess == true
            WordSiegeEntryCard(
                iconLocked = access != null && !seriesOwned,
                badge = sh("SERİ FORMAT", "SERIES FORMAT"),
                featured = false,
                title = sh("SERİ / HIZLI OYUN", "SERIES / QUICK GAME"),
                subtitle = when {
                    entitlementError -> sh(
                        "Satın alma erişimi şu anda doğrulanamadı. Yeniden deneyebilirsin.",
                        "Purchase access could not be verified. You can retry.",
                    )
                    access == null -> sh(
                        "Satın alma durumu kontrol ediliyor…",
                        "Checking purchase access…",
                    )
                    seriesOwned -> sh(
                        "3 / 5 / 10 dakikalık hamleler • otomatik pas • 3 kaçırılan turda mağlubiyet",
                        "3 / 5 / 10 minute turns • auto-pass • 3 missed turns lose",
                    )
                    else -> sh(
                        "Seri Oyun ürününü mağazadan açtıktan sonra burada oynayabilirsin.",
                        "Unlock Series Game in the store, then play it here.",
                    )
                },
                action = when {
                    entitlementError -> sh("YENİLE", "RETRY")
                    access == null -> sh("KONTROL", "CHECKING")
                    seriesOwned -> sh("SERİ OYNA", "PLAY SERIES")
                    else -> sh("MAĞAZAYA GİT", "OPEN STORE")
                },
                loading = access == null && !entitlementError,
                accent = GameColors.TacticalTurquoise,
                onClick = {
                    when {
                        entitlementError -> retry++
                        access == null -> Unit
                        seriesOwned -> mode = WordSiegeEntryMode.SERIES
                        else -> onOpenStore()
                    }
                },
            )

            GameSurface(
                borderColor = GameColors.PrimaryBlue.copy(alpha = .28f),
            ) {
                Text(
                    sh("PUAN MANTIĞI", "SCORING"),
                    color = GameColors.PrimaryBlue,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    sh(
                        "Kelime puanın kalıcıdır. Bölge puanı sahip olduğun küplere bağlıdır; her küp 2 puandır.",
                        "Word points stay earned. Territory points depend on cubes you currently own; each cube is worth 2 points.",
                    ),
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            GameSurface(
                borderColor = GameColors.PlayGreen.copy(alpha = .30f),
            ) {
                Text(
                    sh("ADİL REKABET", "FAIR PLAY"),
                    color = GameColors.PlayGreen,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    sh(
                        "Satın alınan seçenekler maç gücü, skor veya rating avantajı sağlamaz.",
                        "Purchased options never provide match power, score, or rating advantages.",
                    ),
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun WordSiegeEntryCard(
    iconLocked: Boolean,
    badge: String,
    featured: Boolean,
    title: String,
    subtitle: String,
    action: String,
    loading: Boolean = false,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    GameSurface(
        elevated = true,
        borderColor = accent.copy(alpha = if (featured) .62f else .32f),
    ) {
        Surface(
            shape = GameShapes.Pill,
            color = accent.copy(alpha = if (featured) .18f else .11f),
        ) {
            Text(
                badge,
                Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                color = accent,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = GameShapes.Medium,
                color = accent.copy(alpha = .12f),
            ) {
                Icon(
                    if (iconLocked) {
                        Icons.Rounded.Lock
                    } else if (title.contains("SERİ") || title.contains("SERIES")) {
                        Icons.Rounded.Bolt
                    } else {
                        Icons.Rounded.GridView
                    },
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.padding(11.dp).size(if (featured) 27.dp else 24.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = GameColors.TextPrimary,
                    style = if (featured) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Spacer(Modifier.height(13.dp))

        if (loading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = accent,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    action,
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        } else if (iconLocked) {
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = GameShapes.Medium,
                border = BorderStroke(1.dp, accent.copy(alpha = .55f)),
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = accent)
                Spacer(Modifier.width(7.dp))
                Text(action, color = accent, fontWeight = FontWeight.Black)
            }
        } else {
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().height(if (featured) 52.dp else 48.dp),
                shape = GameShapes.Medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = GameColors.TextPrimary,
                ),
            ) {
                if (action.contains("YENİLE") || action.contains("RETRY")) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                }
                Text(action, fontWeight = FontWeight.Black)
            }
        }
    }
}
