package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onExit) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = sh("Geri", "Back"))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        sh("KELİME KUŞATMASI", "WORD SIEGE"),
                        color = SonHarfTheme.TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        sh("Oyun türünü seç ve arenaya gir.", "Choose a battle type and enter the arena."),
                        color = SonHarfTheme.TextSecondary,
                        fontSize = 11.sp,
                    )
                }
            }

            WordSiegeEntryCard(
                iconLocked = false,
                title = sh("STANDART KUŞATMA", "STANDARD SIEGE"),
                subtitle = sh(
                    "Klasik Kelime Kuşatması • normal hamle süresi • rakip veya bot",
                    "Classic Word Siege • standard turns • rival or practice bot",
                ),
                action = sh("OYNA", "PLAY"),
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
                    access == null -> sh("Satın alma durumu kontrol ediliyor…", "Checking purchase access…")
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
                onClick = {
                    when {
                        entitlementError -> retry++
                        access == null -> Unit
                        seriesOwned -> mode = WordSiegeEntryMode.SERIES
                        else -> onOpenStore()
                    }
                },
            )
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
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = WordSiegeGameUi.SurfaceSoft,
        border = BorderStroke(1.dp, WordSiegeGameUi.Border),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = WordSiegeGameUi.Blue.copy(alpha = .12f),
                ) {
                    Icon(
                        if (iconLocked) Icons.Rounded.Lock else if (title.contains("SERİ") || title.contains("SERIES")) Icons.Rounded.Bolt else Icons.Rounded.GridView,
                        contentDescription = null,
                        tint = WordSiegeGameUi.Blue,
                        modifier = Modifier.padding(11.dp).size(24.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = WordSiegeGameUi.Text, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Text(subtitle, color = WordSiegeGameUi.Muted, fontSize = 10.sp, lineHeight = 14.sp)
                }
            }
            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(action, color = WordSiegeGameUi.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            } else if (iconLocked) {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Rounded.Lock, null)
                    Spacer(Modifier.width(7.dp))
                    Text(action, fontWeight = FontWeight.Black)
                }
            } else {
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WordSiegeGameUi.Blue),
                ) {
                    if (action.contains("YENİLE") || action.contains("RETRY")) {
                        Icon(Icons.Rounded.Refresh, null)
                        Spacer(Modifier.width(7.dp))
                    }
                    Text(action, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
