package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.EquippedCosmeticsDto
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.equipDefaultGameTheme
import com.sonharf.game.data.equipShopItem
import com.sonharf.game.data.getEquippedCosmetics
import com.sonharf.game.data.getInventory
import com.sonharf.game.data.getOwnedShopItems
import com.sonharf.game.data.ShopItemDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val ProfileThemeTimeoutMs = 10_000L
private const val DarkArenaThemeId = "theme_dark_arena"

/**
 * Profile-owned theme switcher. The blue/white theme is the built-in Son Harf visual system, so it
 * is always owned and never appears as a paid product. Purchased themes remain inventory-gated.
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

    suspend fun reloadThemes() {
        loading = true
        notice = null
        try {
            withTimeout(ProfileThemeTimeoutMs) {
                coroutineScope {
                    val selection = async { backend.getEquippedCosmetics() }
                    val nextInventory = backend.getInventory()
                    val nextCollection = backend.getOwnedShopItems(nextInventory)
                    val nextEquipped = selection.await()
                    // Publish one complete result. A failed fetch must never erase a cached theme.
                    owned = nextInventory
                    collection = nextCollection
                    equipped = nextEquipped
                    SonHarfCosmetics.applyAndPersist(context, nextEquipped)
                }
            }
        } catch (error: Exception) {
            if (error is CancellationException && error !is TimeoutCancellationException) throw error
            notice = sh("Koleksiyon yenilenemedi. Mevcut görünümün korundu; tekrar deneyebilirsin.", "Could not refresh your collection. Your current style is unchanged; you can retry.")
        } finally {
            loading = false
        }
    }

    fun selectTheme(themeId: String?) {
        if (busy || loading) return
        busy = true
        notice = null
        scope.launch {
            try {
                val next = withTimeout(ProfileThemeTimeoutMs) {
                    if (themeId == null) backend.equipDefaultGameTheme()
                    else backend.equipShopItem(themeId)
                    backend.getEquippedCosmetics()
                }
                equipped = next
                SonHarfCosmetics.applyAndPersist(context, next)
                notice = sh("Görünüm uygulandı.", "Style applied.")
            } catch (error: Exception) {
                if (error is CancellationException && error !is TimeoutCancellationException) throw error
                notice = sh("İşlem doğrulanamadı. Görünümünü yenileyip kontrol et.", "Could not confirm the change. Refresh to check your equipped style.")
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(backend) { reloadThemes() }

    val darkActive = SonHarfCosmetics.gameThemeId == DarkArenaThemeId

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Palette, null, tint = MainUi.Blue, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(7.dp))
            Text(sh("TEMALARIM", "MY THEMES"), color = MainUi.Text, fontSize = 13.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            if (loading || busy) CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp, color = MainUi.Blue)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileThemeCard(
                title = sh("Ana Mavi Beyaz", "Main Blue & White"),
                subtitle = sh("Son Harf ana teması • Ücretsiz", "Son Harf main theme • Free"),
                active = !darkActive,
                enabled = !busy && !loading,
                dark = false,
                modifier = Modifier.weight(1f),
                onClick = { selectTheme(null) },
            )
            if (DarkArenaThemeId in owned) {
                ProfileThemeCard(
                    title = sh("Gece Arenası", "Night Arena"),
                    subtitle = sh("Koleksiyonunda", "In your collection"),
                    active = darkActive,
                    enabled = !busy && !loading,
                    dark = true,
                    modifier = Modifier.weight(1f),
                    onClick = { selectTheme(DarkArenaThemeId) },
                )
            }
        }

        notice?.let {
            Text(it, color = MainUi.Muted, fontSize = 13.sp)
            TextButton(onClick = { scope.launch { reloadThemes() } }, enabled = !busy && !loading) {
                Text(sh("YENİLE", "REFRESH"))
            }
        }

        val styles = collection.filter { it.id != DarkArenaThemeId }
        Text(sh("STYLE KOLEKSİYONUM", "MY STYLE COLLECTION"), color = MainUi.Text, fontSize = 14.sp, fontWeight = FontWeight.Black)
        Text(sh("Satın aldıkların burada; vitrin değişse de seninle kalır.", "Your purchases stay here, even when the storefront changes."), color = MainUi.Muted, fontSize = 13.sp)
        if (!loading && notice == null && styles.isEmpty()) {
            Text(sh("Yeni Style ürünlerini mağazada keşfet.", "Discover new Style items in the store."), color = MainUi.Muted, fontSize = 13.sp)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(styles, key = { it.id }) { item ->
                OwnedStyleCard(item, equipped.isEquipped(item), !loading && !busy) { selectTheme(item.id) }
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
    dark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val border = if (active) MainUi.Green else MainUi.Border
    Surface(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MainUi.Surface,
        border = BorderStroke(if (active) 2.dp else 1.dp, border),
    ) {
        Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(
                Modifier.fillMaxWidth().height(68.dp).background(
                    brush = if (dark) {
                        Brush.linearGradient(listOf(Color(0xFF070A12), Color(0xFF1A2331), Color(0xFF5A431A)))
                    } else {
                        Brush.linearGradient(listOf(Color.White, Color(0xFFE8F1FF), Color(0xFF1769E0)))
                    },
                    shape = RoundedCornerShape(12.dp),
                ),
            ) {
                if (active) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        null,
                        tint = if (dark) Color(0xFFF0B84D) else Color(0xFF1769E0),
                        modifier = Modifier.align(Alignment.TopEnd).padding(7.dp).size(20.dp),
                    )
                }
            }
            Text(title, color = MainUi.Text, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = MainUi.Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun OwnedStyleCard(item: ShopItemDto, active: Boolean, enabled: Boolean, onEquip: () -> Unit) {
    val supported = item.isSupportedOwnedStyle()
    Card(
        modifier = Modifier.width(248.dp),
        colors = CardDefaults.cardColors(containerColor = MainUi.Surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(if (active) 2.dp else 1.dp, if (active) MainUi.Green else MainUi.Border),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.fillMaxWidth().height(76.dp), contentAlignment = Alignment.Center) {
                if (item.kind == "profile_frame" && supported) {
                    Icon(Icons.Rounded.Person, null, Modifier.size(36.dp), tint = MainUi.Blue)
                    PurchasedProfileFrameOverlay(frameId = item.id, modifier = Modifier.size(76.dp))
                } else {
                    Icon(Icons.Rounded.Palette, null, Modifier.size(38.dp), tint = MainUi.Blue)
                }
            }
            Text(sh(item.nameTr, item.nameEn), color = MainUi.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(sh(item.descriptionTr, item.descriptionEn), color = MainUi.Muted, fontSize = 13.sp)
            Text(if (item.active) sh("Koleksiyonunda", "In your collection") else sh("Arşiv ürünü • Koleksiyonunda", "Retired item • In your collection"), color = MainUi.Blue, fontSize = 12.sp)
            if (!supported) Text(sh("Bu sürümde kullanılamıyor. Sahipliğin korunuyor.", "Unavailable in this version. You still own this item."), color = MainUi.Muted, fontSize = 13.sp)
            Button(onClick = onEquip, enabled = enabled && supported && !active, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (active) sh("AKTİF", "EQUIPPED") else sh("KULLAN", "EQUIP"))
            }
        }
    }
}
