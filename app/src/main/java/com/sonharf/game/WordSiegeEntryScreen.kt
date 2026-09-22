package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import com.sonharf.game.data.getVipEntitlements

private enum class WordSiegeEntryMode { STANDARD, SERIES }

/**
 * Real gameplay entry for Kelime Kuşatması. Paid Series Game is reachable from the same
 * place as the standard arena instead of being hidden behind the store after purchase.
 */
@Composable
internal fun WordSiegeEntryScreen(
    onExit: () -> Unit,
    onOpenStore: () -> Unit,
) {
    var mode by remember { mutableStateOf<WordSiegeEntryMode?>(null) }

    when (mode) {
        WordSiegeEntryMode.STANDARD -> {
            WordSiegeExperienceScreen { mode = null }
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
                "Oyun türünü seç ve arenaya gir",
                "Choose a battle type and enter the arena",
            ),
            onBack = onExit,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GameSpacing.ScreenHorizontal, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WordSiegeEntryCard(
                iconLocked = false,
                title = sh("STANDART KUŞATMA", "STANDARD SIEGE"),
                subtitle = sh(
                    "Klasik Kelime Kuşatması • normal hamle süresi • rakip veya bot",
                    "Classic Word Siege • standard turns • rival or practice bot",
                ),
                action = sh("OYNA", "PLAY"),
                accent = GameColors.PrimaryBlue,
                onClick = { mode = WordSiegeEntryMode.STANDARD },
            )

            val access = entitlements
            val seriesOwned = access?.seriesGameAccess == true
            WordSiegeEntryCard(
                iconLocked = access != null && !seriesOwned,
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
        }
    }
}

@Composable
private fun WordSiegeEntryCard(
    iconLocked: Boolean,
    title: String,
    subtitle: String,
    action: String,
    loading: Boolean = false,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    GameSurface(
        elevated = true,
        borderColor = accent.copy(alpha = .32f),
    ) {
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
                    modifier = Modifier.padding(11.dp).size(24.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = GameColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
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
                modifier = Modifier.fillMaxWidth().height(48.dp),
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
