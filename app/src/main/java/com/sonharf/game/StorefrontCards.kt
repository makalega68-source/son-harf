package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sonharf.game.data.StoreBundleDto
import com.sonharf.game.data.StorefrontDto

@Composable
internal fun StoreDailyRewardCard(
    state: StorefrontDto?,
    busy: Boolean,
    onClaim: () -> Unit,
) {
    GameSurface(
        borderColor = GameColors.RewardAmber.copy(alpha = .30f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = GameShapes.Medium,
                color = GameColors.RewardAmber.copy(alpha = .11f),
            ) {
                Icon(
                    Icons.Rounded.CardGiftcard,
                    contentDescription = null,
                    modifier = Modifier.padding(9.dp).size(24.dp),
                    tint = GameColors.RewardAmber,
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    sh("Günlük hediyen", "Your daily gift"),
                    color = GameColors.TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    state?.let { "${it.dailyReward} Son Coin" }
                        ?: sh("Yükleniyor…", "Loading…"),
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(
                onClick = onClaim,
                enabled = state != null && !state.dailyClaimed && !busy,
            ) {
                Text(
                    if (state?.dailyClaimed == true) {
                        sh("Alındı", "Claimed")
                    } else {
                        sh("Ücretsiz al", "Claim free")
                    },
                    color = if (state?.dailyClaimed == true) {
                        GameColors.TextTertiary
                    } else {
                        GameColors.RewardAmber
                    },
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
internal fun StorePromoCard(
    title: String,
    subtitle: String,
    action: String,
    @androidx.annotation.DrawableRes artwork: Int? = null,
    onClick: () -> Unit,
) {
    GameSurface(borderColor = GameColors.Border) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (artwork != null) {
                androidx.compose.foundation.Image(
                    androidx.compose.ui.res.painterResource(artwork),
                    contentDescription = null,
                    modifier = Modifier.size(58.dp),
                )
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = GameColors.TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    color = GameColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onClick) {
                Text(
                    action,
                    color = GameColors.PrimaryBlue,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
internal fun StoreBundleCard(
    bundle: StoreBundleDto,
    ownedItems: Set<String>,
    busy: Boolean,
    onBuy: () -> Unit,
) {
    val complete = bundle.owned || bundle.items.all { it.id in ownedItems }

    GameSurface(
        elevated = true,
        borderColor = GameColors.Lavender.copy(alpha = .30f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = GameShapes.Medium,
                color = GameColors.Lavender.copy(alpha = .11f),
            ) {
                Icon(
                    Icons.Rounded.Inventory2,
                    contentDescription = null,
                    tint = GameColors.Lavender,
                    modifier = Modifier.padding(9.dp).size(22.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    sh(bundle.nameTr, bundle.nameEn),
                    color = GameColors.TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                bundle.availableUntil?.take(10)?.let { end ->
                    Text(
                        sh("$end tarihine kadar", "Available until $end"),
                        color = GameColors.TextTertiary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            CurrencyChip(bundle.diamondPrice)
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            bundle.items.forEach { product ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(90.dp),
                        shape = GameShapes.Medium,
                        color = GameColors.ElevatedBackground,
                        border = BorderStroke(1.dp, GameColors.Divider),
                    ) {
                        StoreProductPreview(
                            product,
                            Modifier.fillMaxSize().padding(4.dp),
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        sh(product.nameTr, product.nameEn),
                        color = GameColors.TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBuy,
            enabled = !busy && !complete,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = GameShapes.Medium,
            border = BorderStroke(
                1.dp,
                if (complete) GameColors.Border else GameColors.Lavender.copy(alpha = .55f),
            ),
        ) {
            if (complete) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = GameColors.PlayGreen,
                )
                Spacer(Modifier.width(7.dp))
            }
            Text(
                if (complete) {
                    sh("Koleksiyonunda", "In your collection")
                } else {
                    sh("Paketi incele", "View bundle")
                },
                color = if (complete) GameColors.TextTertiary else GameColors.Lavender,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
internal fun StoreProBenefits() {
    val uri = LocalUriHandler.current

    GameSurface(
        borderColor = GameColors.RewardAmber.copy(alpha = .28f),
    ) {
        Text(
            sh("PRO AYRICALIKLARI", "PRO BENEFITS"),
            color = GameColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.height(10.dp))

        listOf(
            sh("Zorunlu reklamsız kullanım", "No mandatory ads"),
            sh("PRO rozeti ve profil ayrıcalıkları", "PRO badge and profile benefits"),
            sh("Gelişmiş maç analizi", "Advanced match analysis"),
            sh("Özel odalar ve kayıtlı arkadaş listesi", "Private rooms and saved friends"),
        ).forEachIndexed { index, benefit ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = GameShapes.Pill,
                    color = GameColors.PlayGreen.copy(alpha = .11f),
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.padding(5.dp).size(14.dp),
                        tint = GameColors.PlayGreen,
                    )
                }
                Spacer(Modifier.width(9.dp))
                Text(
                    benefit,
                    color = GameColors.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            if (index < 3) {
                HorizontalDivider(color = GameColors.Divider)
            }
        }

        Spacer(Modifier.height(6.dp))

        TextButton(
            onClick = {
                uri.openUri(
                    "https://play.google.com/store/account/subscriptions?package=${BuildConfig.APPLICATION_ID}",
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                Icons.Rounded.OpenInNew,
                contentDescription = null,
                tint = GameColors.PrimaryBlue,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                sh("Aboneliği yönet", "Manage subscription"),
                color = GameColors.PrimaryBlue,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
