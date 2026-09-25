package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.ShopItemDto

/** What the detail sheet's main button does for a product, from ownership and PRO state. */
internal enum class StoreProductAction { BUY, EQUIP, EQUIPPED, NEEDS_PRO }

internal fun storeProductAction(item: ShopItemDto, owned: Boolean, equipped: Boolean, proActive: Boolean): StoreProductAction = when {
    equipped -> StoreProductAction.EQUIPPED
    owned -> StoreProductAction.EQUIP
    item.vipOnly && !proActive -> StoreProductAction.NEEDS_PRO
    else -> StoreProductAction.BUY
}

/** Player-facing name of a cosmetic kind; unknown kinds fall back to a generic label. */
internal fun storeKindLabel(kind: String): String = when (kind) {
    "game_theme" -> gameText("Tahta teması", "Board theme")
    "keyboard_theme" -> gameText("Klavye / harf taşı", "Keyboard / tiles")
    "name_style" -> gameText("İsim stili", "Name style")
    "profile_frame" -> gameText("Profil çerçevesi", "Profile frame")
    "victory_effect" -> gameText("Zafer efekti", "Victory effect")
    "vs_intro" -> gameText("Maç başlangıç efekti", "Match intro")
    "word_effect" -> gameText("Kelime efekti", "Word effect")
    "emoji_pack" -> gameText("Emoji paketi", "Emoji pack")
    "badge" -> gameText("Rozet", "Badge")
    "title" -> gameText("Unvan", "Title")
    "nameplate" -> gameText("Profil arka planı", "Profile background")
    else -> gameText("Kozmetik", "Cosmetic")
}

/**
 * Product detail as a modal bottom sheet: large preview, description, type, duration,
 * "no gameplay advantage", price and one clear action. Purchase and equip stay server-side;
 * this sheet only calls them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StoreProductDetailSheet(
    item: ShopItemDto,
    owned: Boolean,
    equipped: Boolean,
    proActive: Boolean,
    balance: Int,
    busy: Boolean,
    onBuy: () -> Unit,
    onEquip: () -> Unit,
    onPro: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = GameColors.ElevatedBackground,
        contentColor = GameColors.TextPrimary,
    ) {
        StoreProductDetailContent(item, owned, equipped, proActive, balance, busy, onBuy, onEquip, onPro)
        Spacer(Modifier.navigationBarsPadding().height(12.dp))
    }
}

@Composable
internal fun StoreProductDetailContent(
    item: ShopItemDto,
    owned: Boolean,
    equipped: Boolean,
    proActive: Boolean,
    balance: Int,
    busy: Boolean,
    onBuy: () -> Unit,
    onEquip: () -> Unit,
    onPro: () -> Unit,
) {
    val name = if (SonHarfUiState.isEnglish) item.nameEn else item.nameTr
    val description = if (SonHarfUiState.isEnglish) item.descriptionEn else item.descriptionTr
    val action = storeProductAction(item, owned, equipped, proActive)
    val affordable = balance >= item.diamondPrice

    Column(
        Modifier.fillMaxWidth().padding(horizontal = GameSpacing.ScreenHorizontal),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                name,
                Modifier.weight(1f),
                color = GameColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            if (item.vipOnly) {
                Surface(shape = GameShapes.Pill, color = GameColors.PrestigeGold.copy(alpha = .16f)) {
                    Text("PRO", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = GameColors.PrestigeGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        StoreProductPreview(item, Modifier.fillMaxWidth().height(180.dp), expanded = true)
        if (description.isNotBlank()) {
            Text(description, color = GameColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        Surface(
            shape = GameShapes.Medium,
            color = GameColors.PrimarySurface,
            border = BorderStroke(1.dp, GameColors.Border),
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                DetailRow(gameText("Tür", "Type"), gameText("Kozmetik • ", "Cosmetic • ") + storeKindLabel(item.kind))
                DetailRow(
                    gameText("Süre", "Duration"),
                    if (item.trialMode != null) gameText("Süreli deneme mevcut", "Timed trial available") else gameText("Kalıcı", "Permanent"),
                )
                DetailRow(gameText("Oyun avantajı", "Gameplay advantage"), gameText("Yok", "None"), GameColors.PlayGreen)
                DetailRow(
                    gameText("Fiyat", "Price"),
                    if (owned) gameText("Sende var", "Owned") else "${item.diamondPrice} SC",
                    if (owned) GameColors.PlayGreen else GameColors.RewardAmber,
                )
            }
        }
        when (action) {
            StoreProductAction.EQUIPPED -> {
                Surface(shape = GameShapes.Medium, color = GameColors.PlayGreen.copy(alpha = .14f), border = BorderStroke(1.dp, GameColors.PlayGreen.copy(alpha = .5f))) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = GameColors.PlayGreen, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(gameText("KULLANILIYOR", "EQUIPPED"), color = GameColors.PlayGreen, fontWeight = FontWeight.Black)
                    }
                }
            }
            StoreProductAction.EQUIP -> GamePrimaryButton(
                text = gameText("KULLAN", "EQUIP"),
                onClick = onEquip,
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                icon = Icons.Rounded.Palette,
            )
            StoreProductAction.NEEDS_PRO -> GameSecondaryButton(
                text = gameText("PRO ÜYELERE ÖZEL • İNCELE", "PRO ONLY • EXPLORE"),
                onClick = onPro,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Rounded.WorkspacePremium,
            )
            StoreProductAction.BUY -> {
                GamePrimaryButton(
                    text = gameText("SATIN AL • ${item.diamondPrice} SC", "BUY • ${item.diamondPrice} SC"),
                    onClick = onBuy,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy && affordable,
                    icon = Icons.Rounded.ShoppingBag,
                )
                if (!affordable) {
                    Text(
                        gameText("Bakiyen: $balance SC • ${item.diamondPrice - balance} SC eksik", "Balance: $balance SC • ${item.diamondPrice - balance} SC short"),
                        color = GameColors.TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color = GameColors.TextPrimary) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = GameColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
        Text(value, color = valueColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}
