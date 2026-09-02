package com.sonharf.game

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ShopItemDto
import com.sonharf.game.data.SupabaseProvider
import kotlinx.coroutines.launch

internal object PurchasedFrameCatalog {
    const val GOLD = "frame_asset_gold"
    const val MINT = "frame_asset_mint"
    const val PURPLE = "frame_asset_purple"
    const val GREEN = "frame_asset_green"

    val ids = setOf(GOLD, MINT, PURPLE, GREEN)

    fun drawable(id: String?): Int? = when (id) {
        GOLD -> R.drawable.style_frame_gold
        MINT -> R.drawable.style_frame_mint
        PURPLE -> R.drawable.style_frame_purple
        GREEN -> R.drawable.style_frame_green
        else -> null
    }
}

private data class PurchasedFrameSpec(
    val id: String,
    val titleTr: String,
    val titleEn: String,
    val subtitleTr: String,
    val subtitleEn: String,
    @DrawableRes val drawable: Int,
    val accent: Color,
)

private val purchasedFrameSpecs = listOf(
    PurchasedFrameSpec(PurchasedFrameCatalog.GOLD, "Altın Hat", "Gold Line", "Sıcak metalik profil çerçevesi", "Warm metallic profile frame", R.drawable.style_frame_gold, Color(0xFFD7A72E)),
    PurchasedFrameSpec(PurchasedFrameCatalog.MINT, "Buz Mint", "Ice Mint", "Temiz ve modern mint çerçeve", "Clean modern mint frame", R.drawable.style_frame_mint, Color(0xFF32BFB3)),
    PurchasedFrameSpec(PurchasedFrameCatalog.PURPLE, "Mor Spektrum", "Violet Spectrum", "Premium mor profil vurgusu", "Premium violet profile accent", R.drawable.style_frame_purple, Color(0xFF7257D8)),
    PurchasedFrameSpec(PurchasedFrameCatalog.GREEN, "Zümrüt Hat", "Emerald Line", "Dengeli zümrüt profil çerçevesi", "Balanced emerald profile frame", R.drawable.style_frame_green, Color(0xFF2FAE68)),
)

@Composable
internal fun PurchasedProfileFrameOverlay(frameId: String?, modifier: Modifier = Modifier) {
    val drawable = PurchasedFrameCatalog.drawable(frameId) ?: return
    Image(
        painter = painterResource(drawable),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.FillBounds,
    )
}

@Composable
internal fun PurchasedProfileFramesStoreRow() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var shopItems by remember { mutableStateOf<Map<String, ShopItemDto>>(emptyMap()) }
    var inventory by remember { mutableStateOf<Set<String>>(emptySet()) }
    var equippedId by remember { mutableStateOf<String?>(SonHarfCosmetics.profileFrameId) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        val b = backend ?: return
        runCatching {
            val shop = b.getShopItems().filter { it.kind == "profile_frame" && it.id in PurchasedFrameCatalog.ids }
            val owned = b.getInventory().toSet()
            val equipped = b.getEquippedCosmetics()
            shopItems = shop.associateBy { it.id }
            inventory = owned
            equippedId = equipped?.profileFrameId
            SonHarfCosmetics.apply(equipped)
        }.onFailure {
            notice = sh("Profil Style ürünleri yüklenemedi.", "Profile Style items could not be loaded.")
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(purchasedFrameSpecs, key = { it.id }) { spec ->
                val item = shopItems[spec.id]
                val owned = spec.id in inventory
                val equipped = equippedId == spec.id
                Surface(
                    modifier = Modifier.width(184.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(if (equipped) 2.dp else 1.dp, if (equipped) spec.accent else Color(0xFFD5E0EA)),
                ) {
                    Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            Modifier.fillMaxWidth().height(102.dp).clip(RoundedCornerShape(13.dp)).background(spec.accent.copy(alpha = .08f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Surface(modifier = Modifier.size(62.dp), shape = CircleShape, color = Color(0xFFF5F8FC)) {
                                    Image(
                                        painter = painterResource(R.drawable.style_icon_user),
                                        contentDescription = null,
                                        modifier = Modifier.padding(15.dp),
                                        colorFilter = ColorFilter.tint(Color(0xFF142033)),
                                    )
                                }
                                Image(
                                    painter = painterResource(spec.drawable),
                                    contentDescription = null,
                                    modifier = Modifier.size(86.dp),
                                    contentScale = ContentScale.FillBounds,
                                )
                            }
                            if (equipped) {
                                Surface(
                                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                                    shape = RoundedCornerShape(99.dp),
                                    color = Color(0xFF2FAE68),
                                ) {
                                    Text(sh("AKTİF", "ACTIVE"), Modifier.padding(horizontal = 7.dp, vertical = 4.dp), color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                        Text(sh(spec.titleTr, spec.titleEn), color = Color(0xFF142033), fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Text(sh(spec.subtitleTr, spec.subtitleEn), color = Color(0xFF6F7C8D), fontSize = 8.sp, minLines = 2)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            if (!owned && !equipped) {
                                Image(
                                    painter = painterResource(R.drawable.style_icon_coin),
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    colorFilter = ColorFilter.tint(spec.accent),
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                when {
                                    equipped -> sh("AKTİF", "ACTIVE")
                                    owned -> sh("SAHİPSİN", "OWNED")
                                    else -> "${item?.diamondPrice ?: 0} SC"
                                },
                                modifier = Modifier.weight(1f),
                                color = if (owned || equipped) Color(0xFF2FAE68) else spec.accent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                            )
                            Button(
                                onClick = {
                                    val b = backend ?: return@Button
                                    scope.launch {
                                        busyId = spec.id
                                        runCatching {
                                            if (!owned) b.purchaseShopItem(spec.id)
                                            b.equipShopItem(spec.id)
                                        }.onSuccess {
                                            notice = sh("${spec.titleTr} kullanılıyor.", "${spec.titleEn} equipped.")
                                            reload()
                                        }.onFailure { error ->
                                            notice = if ("insufficient_diamonds" in error.message.orEmpty()) {
                                                sh("Yeterli Son Coin'in yok.", "Not enough Son Coin.")
                                            } else {
                                                sh("Style işlemi tamamlanamadı.", "Style action could not be completed.")
                                            }
                                        }
                                        busyId = null
                                    }
                                },
                                enabled = backend != null && item != null && !equipped && busyId == null,
                                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 3.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = spec.accent, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Text(if (busyId == spec.id) "…" else if (owned) sh("KULLAN", "EQUIP") else sh("AL", "BUY"), fontSize = 8.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
        notice?.let { Text(it, color = Color(0xFF6F7C8D), fontSize = 9.sp) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Image(
                painter = painterResource(R.drawable.style_icon_palette),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                colorFilter = ColorFilter.tint(Color(0xFF1677FF)),
            )
            Text(
                sh("Satın alınan çerçeveler yalnızca görünümü değiştirir; oyun gücü vermez.", "Purchased frames only change appearance; they grant no gameplay power."),
                color = Color(0xFF6F7C8D),
                fontSize = 8.sp,
            )
        }
    }
}
