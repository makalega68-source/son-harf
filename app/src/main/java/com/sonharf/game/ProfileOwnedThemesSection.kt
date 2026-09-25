package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sonharf.game.data.EquippedCosmeticsDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ShopItemDto
import com.sonharf.game.data.equipDefaultGameTheme
import com.sonharf.game.data.equipShopItem
import com.sonharf.game.data.getEquippedCosmetics
import com.sonharf.game.data.getInventory
import com.sonharf.game.data.getOwnedShopItems
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val ProfileThemeTimeoutMs = 10_000L
private const val BlackThemeId = "theme_black"
private const val LegacyDarkArenaThemeId = "theme_dark_arena"
private val DarkThemeIds = setOf(BlackThemeId, LegacyDarkArenaThemeId)

/**
 * Owned Style collection. Store rotation may stop new sales, but supported purchased visuals remain
 * available to their owner. Network failures never publish a partial result over the cached look.
 */
@Composable
internal fun ProfileOwnedThemesSection(backend: OnlineGameBackend) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var owned by remember { mutableStateOf<Set<String>>(emptySet()) }
    var equipped by remember { mutableStateOf<EquippedCosmeticsDto?>(null) }
    var collection by remember { mutableStateOf<List<ShopItemDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun reloadCollection() {
        loading = true
        notice = null
        try {
            withTimeout(ProfileThemeTimeoutMs) {
                coroutineScope {
                    val equippedRequest = async { backend.getEquippedCosmetics() }
                    val nextOwned = backend.getInventory()
                    val nextCollection = backend.getOwnedShopItems(nextOwned)
                    val nextEquipped = equippedRequest.await()

                    owned = nextOwned
                    collection = nextCollection
                    equipped = nextEquipped
                    SonHarfCosmetics.applyAndPersist(context, nextEquipped)
                }
            }
        } catch (error: Exception) {
            if (error is CancellationException && error !is TimeoutCancellationException) throw error
            // A failed request must not erase cached UI.
            notice = sh(
                "Koleksiyon yenilenemedi. Mevcut görünümün korundu; tekrar deneyebilirsin.",
                "Could not refresh your collection. Your current style is unchanged; you can retry.",
            )
        } finally {
            loading = false
        }
    }

    fun equipStyle(itemId: String?) {
        if (busy || loading) return
        busy = true
        notice = null
        scope.launch {
            try {
                val nextEquipped = withTimeout(ProfileThemeTimeoutMs) {
                    if (itemId == null) backend.equipDefaultGameTheme()
                    else backend.equipShopItem(itemId)
                    backend.getEquippedCosmetics()
                }
                equipped = nextEquipped
                SonHarfCosmetics.applyAndPersist(context, nextEquipped)
                notice = sh("Görünüm uygulandı.", "Style applied.")
            } catch (error: Exception) {
                if (error is CancellationException && error !is TimeoutCancellationException) throw error
                notice = sh(
                    "İşlem doğrulanamadı. Görünümünü yenileyip kontrol et.",
                    "Could not confirm the change. Refresh to check your equipped style.",
                )
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(backend) { reloadCollection() }

    val activeDarkThemeId = SonHarfCosmetics.gameThemeId?.takeIf { it in DarkThemeIds }
    val ownedDarkThemeId = when {
        BlackThemeId in owned -> BlackThemeId
        LegacyDarkArenaThemeId in owned -> LegacyDarkArenaThemeId
        activeDarkThemeId != null -> activeDarkThemeId
        else -> null
    }
    val darkActive = activeDarkThemeId != null
    val showBlackTheme = ownedDarkThemeId != null

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = GameShapes.Small,
                color = GameColors.PrimaryBlue.copy(alpha = .11f),
            ) {
                Icon(
                    Icons.Rounded.Palette,
                    contentDescription = null,
                    tint = GameColors.PrimaryBlue,
                    modifier = Modifier.padding(7.dp).size(18.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                sh("KOLEKSİYONUM", "MY COLLECTION"),
                color = GameColors.TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.weight(1f))
            if (loading || busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = GameColors.PrimaryBlue,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProfileThemeCard(
                title = sh("Ana Tema", "Main Theme"),
                subtitle = sh("Varsayılan görünüm • Ücretsiz", "Default look • Free"),
                active = !darkActive,
                enabled = !busy && !loading,
                blackVariant = false,
                modifier = Modifier.weight(1f),
                onClick = { equipStyle(null) },
            )
            if (showBlackTheme) {
                ProfileThemeCard(
                    title = "Black Theme",
                    subtitle = sh("Koleksiyonunda", "In your collection"),
                    active = darkActive,
                    enabled = !busy && !loading,
                    blackVariant = true,
                    modifier = Modifier.weight(1f),
                    onClick = { equipStyle(ownedDarkThemeId ?: BlackThemeId) },
                )
            }
        }

        notice?.let {
            Surface(
                shape = GameShapes.Small,
                color = GameColors.SecondarySurface,
                border = BorderStroke(1.dp, GameColors.Border),
            ) {
                Column(Modifier.fillMaxWidth().padding(10.dp)) {
                    Text(
                        it,
                        color = GameColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = { scope.launch { reloadCollection() } },
                        enabled = !busy && !loading,
                    ) {
                        Text(
                            sh("YENİLE", "REFRESH"),
                            color = GameColors.PrimaryBlue,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        val styles = collection.filter { it.id !in DarkThemeIds && it.isSupportedOwnedStyle() }

        Text(
            sh("STYLE KOLEKSİYONUM", "MY STYLE COLLECTION"),
            color = GameColors.TextPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
        )
        Text(
            sh(
                "Satın aldıkların burada kalır; vitrin değişse de sahipliğin korunur.",
                "Your purchases stay here; ownership is preserved when the storefront changes.",
            ),
            color = GameColors.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )

        if (!loading && notice == null && styles.isEmpty()) {
            Text(
                sh(
                    "Yeni Style ürünlerini mağazada keşfet.",
                    "Discover new Style items in the store.",
                ),
                color = GameColors.TextTertiary,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(styles, key = { it.id }) { item ->
                OwnedStyleCard(
                    item = item,
                    active = equipped.isEquipped(item),
                    enabled = !loading && !busy,
                    onEquip = { equipStyle(item.id) },
                )
            }
        }
    }
}

@Composable
private fun ProfileThemeCard(
    title: String,
    subtitle: String,
    active: Boolean,
    enabled: Boolean,
    blackVariant: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val accent = if (blackVariant) GameColors.Lavender else GameColors.PrimaryBlue
    val border = if (active) GameColors.PlayGreen else GameColors.Border

    Surface(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        shape = GameShapes.Large,
        color = GameColors.PrimarySurface,
        border = BorderStroke(if (active) 2.dp else 1.dp, border),
    ) {
        Column(
            modifier = Modifier.padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .background(
                        brush = if (blackVariant) {
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF080B10),
                                    Color(0xFF151C27),
                                    Color(0xFF263041),
                                ),
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    GameColors.HeroStart,
                                    GameColors.HeroMiddle,
                                    GameColors.HeroEnd,
                                ),
                            )
                        },
                        shape = GameShapes.Medium,
                    ),
            ) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(7.dp),
                    shape = GameShapes.Pill,
                    color = accent.copy(alpha = .18f),
                ) {
                    Text(
                        if (blackVariant) "BLACK" else "KELİME KUŞATMASI",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = GameColors.TextPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                    )
                }
                if (active) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = GameColors.PlayGreen,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(7.dp)
                            .size(20.dp),
                    )
                }
            }
            Text(
                title,
                color = GameColors.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Black,
            )
            Text(
                subtitle,
                color = GameColors.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun OwnedStyleCard(
    item: ShopItemDto,
    active: Boolean,
    enabled: Boolean,
    onEquip: () -> Unit,
) {
    val supported = item.isSupportedOwnedStyle()

    Card(
        modifier = Modifier.width(248.dp),
        colors = CardDefaults.cardColors(containerColor = GameColors.PrimarySurface),
        shape = GameShapes.Large,
        border = BorderStroke(
            if (active) 2.dp else 1.dp,
            if (active) GameColors.PlayGreen else GameColors.Border,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(76.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = GameShapes.Medium,
                    color = GameColors.SecondarySurface,
                ) {
                    Box(
                        modifier = Modifier.size(76.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (item.kind == "profile_frame" && supported) {
                            Icon(
                                Icons.Rounded.Person,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = GameColors.PrimaryBlue,
                            )
                            PurchasedProfileFrameOverlay(frameId = item.id, modifier = Modifier.size(76.dp))
                        } else {
                            Icon(
                                Icons.Rounded.Palette,
                                contentDescription = null,
                                modifier = Modifier.size(38.dp),
                                tint = GameColors.Lavender,
                            )
                        }
                    }
                }
            }

            Text(
                sh(item.nameTr, item.nameEn),
                color = GameColors.TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                sh(item.descriptionTr, item.descriptionEn),
                color = GameColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                if (item.active) {
                    sh("Koleksiyonunda", "In your collection")
                } else {
                    sh("Arşiv ürünü • Koleksiyonunda", "Retired item • In your collection")
                },
                color = GameColors.PrimaryBlue,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )

            if (!supported) {
                Text(
                    sh(
                        "Bu sürümde kullanılamıyor. Sahipliğin korunuyor.",
                        "Unavailable in this version. You still own this item.",
                    ),
                    color = GameColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = onEquip,
                enabled = enabled && supported && !active,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = GameShapes.Medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GameColors.PrimaryBlue,
                    contentColor = GameColors.TextPrimary,
                    disabledContainerColor = GameColors.Disabled,
                    disabledContentColor = GameColors.DisabledContent,
                ),
            ) {
                Text(
                    if (active) sh("AKTİF", "EQUIPPED") else sh("KULLAN", "EQUIP"),
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}
