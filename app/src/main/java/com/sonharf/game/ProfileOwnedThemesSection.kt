package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
private const val BlackThemeId = "theme_dark_arena"
private val retiredThemeIds = setOf("theme_black", "theme_monster_blue", "theme_aurora", "theme_neon", "theme_midnight")

private data class CollectionCategory(
    val titleTr: String,
    val titleEn: String,
    val subtitleTr: String,
    val subtitleEn: String,
    val icon: ImageVector,
    val accent: Color,
    val kinds: Set<String>,
)

private val collectionCategories = listOf(
    CollectionCategory("Temalar", "Themes", "Uygulamanın genel görünümü", "Overall application appearance", Icons.Rounded.Palette, Color(0xFF7C3AED), setOf("game_theme")),
    CollectionCategory("Profil Çerçeveleri", "Profile Frames", "Avatar çevreni kişiselleştir", "Customize your avatar frame", Icons.Rounded.AccountCircle, Color(0xFF2563EB), setOf("profile_frame")),
    CollectionCategory("Tuş Stilleri", "Keyboard Styles", "Kelime klavyesinin görünümü", "Appearance of the word keyboard", Icons.Rounded.Keyboard, Color(0xFF12B8A6), setOf("keyboard_theme")),
    CollectionCategory("İsim & Prestij", "Name & Prestige", "İsim rengi ve görsel prestij öğeleri", "Name color and visual prestige items", Icons.Rounded.AutoAwesome, Color(0xFFF97316), setOf("name_style", "victory_effect", "emoji_pack")),
)

@Composable
internal fun ProfileOwnedThemesSection(backend: OnlineGameBackend) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var owned by remember { mutableStateOf<Set<String>>(emptySet()) }
    var equipped by remember { mutableStateOf<EquippedCosmeticsDto?>(null) }
    var collection by remember { mutableStateOf<List<ShopItemDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busyId by remember { mutableStateOf<String?>(null) }
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
                    collection = nextCollection.filterNot { it.id in retiredThemeIds }
                    equipped = nextEquipped
                    SonHarfCosmetics.applyAndPersist(context, nextEquipped)
                }
            }
        } catch (error: Exception) {
            if (error is CancellationException && error !is TimeoutCancellationException) throw error
            notice = sh("Koleksiyon yenilenemedi. Mevcut görünümün korundu.", "Could not refresh collection. Your current appearance is unchanged.")
        } finally {
            loading = false
        }
    }

    fun equip(itemId: String?) {
        if (loading || busyId != null) return
        busyId = itemId ?: "default"
        notice = null
        scope.launch {
            try {
                val next = withTimeout(ProfileThemeTimeoutMs) {
                    if (itemId == null) backend.equipDefaultGameTheme() else backend.equipShopItem(itemId)
                    backend.getEquippedCosmetics()
                }
                equipped = next
                SonHarfCosmetics.applyAndPersist(context, next)
                notice = sh("Görünüm uygulandı.", "Appearance applied.")
            } catch (error: Exception) {
                if (error is CancellationException && error !is TimeoutCancellationException) throw error
                notice = sh("Değişiklik doğrulanamadı. Tekrar deneyebilirsin.", "Could not verify the change. Please try again.")
            } finally {
                busyId = null
            }
        }
    }

    LaunchedEffect(backend) { reloadCollection() }

    val blackOwned = BlackThemeId in owned
    val blackActive = SonHarfCosmetics.blackThemeActive
    val visibleCollection = collection.filter(::collectionItemSupported)

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.GridView, null, tint = SonHarfTheme.Primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(sh("KOLEKSİYON", "COLLECTION"), color = SonHarfTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text(sh("Yalnızca sahip olduğun ürünleri burada yönet.", "Manage only the items you own here."), color = SonHarfTheme.TextSecondary, fontSize = 10.sp)
            }
            if (loading || busyId != null) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = SonHarfTheme.Primary)
        }

        ActiveStyleSummary(
            theme = if (blackActive) "Black Theme" else sh("Premium", "Premium"),
            frame = equipped?.profileFrameId?.let(::friendlyCollectionName) ?: sh("Standart", "Standard"),
            keyboard = equipped?.keyboardThemeId?.let(::friendlyCollectionName) ?: sh("Standart", "Standard"),
        )

        CollectionCategoryBlock(
            title = sh("Temalar", "Themes"),
            subtitle = sh("Satın aldığın uygulama temaları", "Application themes you own"),
            icon = Icons.Rounded.Palette,
            accent = SonHarfTheme.Purple,
        ) {
            if (blackOwned) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DefaultPremiumThemeTile(
                        active = !blackActive,
                        enabled = !loading && busyId == null,
                        modifier = Modifier.weight(1f),
                        onClick = { equip(null) },
                    )
                    val blackItem = visibleCollection.firstOrNull { it.id == BlackThemeId }
                    if (blackItem != null) {
                        CollectionProductTile(
                            item = blackItem,
                            active = blackActive,
                            enabled = !loading && busyId == null,
                            modifier = Modifier.weight(1f),
                            onClick = { equip(BlackThemeId) },
                        )
                    } else {
                        BlackOwnedFallbackTile(
                            active = blackActive,
                            enabled = !loading && busyId == null,
                            modifier = Modifier.weight(1f),
                            onClick = { equip(BlackThemeId) },
                        )
                    }
                }
            } else {
                DefaultPremiumThemeTile(
                    active = true,
                    enabled = !loading && busyId == null,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { equip(null) },
                )
            }
        }

        collectionCategories.drop(1).forEach { category ->
            val items = visibleCollection.filter { it.kind in category.kinds }
            if (items.isNotEmpty()) {
                CollectionCategoryBlock(
                    title = sh(category.titleTr, category.titleEn),
                    subtitle = sh(category.subtitleTr, category.subtitleEn),
                    icon = category.icon,
                    accent = category.accent,
                ) {
                    items.chunked(2).forEach { rowItems ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowItems.forEach { item ->
                                CollectionProductTile(
                                    item = item,
                                    active = equipped.isCollectionEquipped(item),
                                    enabled = !loading && busyId == null,
                                    modifier = Modifier.weight(1f),
                                    onClick = { equip(item.id) },
                                )
                            }
                            if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        if (!loading && visibleCollection.none { it.kind != "game_theme" }) {
            Surface(shape = MainUiShape.Control, color = SonHarfTheme.SurfaceSecondary) {
                Text(
                    sh("Mağazadan satın aldığın diğer ürünler burada kategorileri altında görünür.", "Other items you purchase from the shop will appear here under their categories."),
                    Modifier.fillMaxWidth().padding(14.dp),
                    color = SonHarfTheme.TextSecondary,
                    fontSize = 11.sp,
                )
            }
        }

        notice?.let { message ->
            Surface(shape = MainUiShape.Control, color = SonHarfTheme.PrimarySoft) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(message, Modifier.weight(1f), color = SonHarfTheme.TextPrimary, fontSize = 11.sp)
                    TextButton(onClick = { scope.launch { reloadCollection() } }, enabled = busyId == null) {
                        Text(sh("YENİLE", "REFRESH"), color = SonHarfTheme.Primary, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveStyleSummary(theme: String, frame: String, keyboard: String) {
    Surface(
        shape = MainUiShape.Card,
        color = SonHarfTheme.SurfaceSecondary,
        border = BorderStroke(1.dp, SonHarfTheme.Border),
    ) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(sh("AKTİF GÖRÜNÜM", "ACTIVE LOOK"), color = SonHarfTheme.TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                SummaryChip(Icons.Rounded.Palette, theme, SonHarfTheme.Purple, Modifier.weight(1f))
                SummaryChip(Icons.Rounded.AccountCircle, frame, SonHarfTheme.Primary, Modifier.weight(1f))
                SummaryChip(Icons.Rounded.Keyboard, keyboard, SonHarfTheme.Turquoise, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryChip(icon: ImageVector, text: String, accent: Color, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(12.dp), color = SonHarfTheme.Surface) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(17.dp))
            Spacer(Modifier.height(4.dp))
            Text(text, color = SonHarfTheme.TextPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CollectionCategoryBlock(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = MainUiShape.Card,
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, SonHarfTheme.Border),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = accent.copy(alpha = .10f)) {
                    Icon(icon, null, tint = accent, modifier = Modifier.padding(9.dp).size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title, color = SonHarfTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    Text(subtitle, color = SonHarfTheme.TextSecondary, fontSize = 9.sp)
                }
            }
            content()
        }
    }
}

@Composable
private fun DefaultPremiumThemeTile(active: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    CollectionTileShell(active, enabled, modifier, onClick) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1.35f).background(
                Brush.linearGradient(listOf(Color.White, Color(0xFFEAF1FF), Color(0xFF12B8A6), Color(0xFF7C3AED))),
                RoundedCornerShape(13.dp),
            ),
        ) { if (active) ActiveCheck(Color(0xFF2563EB)) }
        Text("Premium", color = SonHarfTheme.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Black)
        Text(sh("Varsayılan", "Default"), color = SonHarfTheme.TextSecondary, fontSize = 9.sp)
    }
}

@Composable
private fun BlackOwnedFallbackTile(active: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    CollectionTileShell(active, enabled, modifier, onClick) {
        BlackThemeSwatch(active)
        Text("Black Theme", color = SonHarfTheme.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Black)
        Text(sh("Sahipsin", "Owned"), color = SonHarfTheme.Turquoise, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BlackThemeSwatch(active: Boolean) {
    Box(
        Modifier.fillMaxWidth().aspectRatio(1.35f).background(
            Brush.linearGradient(listOf(Color(0xFF090B10), Color(0xFF19202D), Color(0xFF3B82F6), Color(0xFF9B6CFF))),
            RoundedCornerShape(13.dp),
        ),
    ) { if (active) ActiveCheck(Color(0xFF1FD1C2)) }
}

@Composable
private fun CollectionProductTile(item: ShopItemDto, active: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    CollectionTileShell(active, enabled && !active, modifier, onClick) {
        StoreProductPreview(item = item, modifier = Modifier.fillMaxWidth().aspectRatio(1.35f))
        Text(sh(item.nameTr, item.nameEn), color = SonHarfTheme.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            if (active) sh("AKTİF", "EQUIPPED") else sh("KULLAN", "EQUIP"),
            color = if (active) SonHarfTheme.Turquoise else SonHarfTheme.Primary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun CollectionTileShell(active: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(17.dp),
        color = SonHarfTheme.Surface,
        border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) SonHarfTheme.Turquoise else SonHarfTheme.Border),
    ) {
        Column(Modifier.fillMaxWidth().padding(9.dp), verticalArrangement = Arrangement.spacedBy(7.dp), content = content)
    }
}

@Composable
private fun BoxScope.ActiveCheck(accent: Color) {
    Surface(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp), shape = RoundedCornerShape(99.dp), color = accent) {
        Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.padding(4.dp).size(13.dp))
    }
}

private fun collectionItemSupported(item: ShopItemDto): Boolean = when (item.kind) {
    "game_theme" -> item.id == BlackThemeId
    "profile_frame" -> item.id in PurchasedFrameCatalog.ids
    "keyboard_theme" -> item.id in setOf("keyboard_crystal", "keyboard_obsidian")
    "name_style" -> item.id in setOf("name_cyan", "name_sapphire", "name_amethyst", "name_aurelia")
    "victory_effect" -> item.id == "victory_crown"
    "emoji_pack" -> true
    else -> false
}

private fun EquippedCosmeticsDto?.isCollectionEquipped(item: ShopItemDto): Boolean = when (item.kind) {
    "game_theme" -> this?.gameThemeId == item.id
    "profile_frame" -> this?.profileFrameId == item.id
    "keyboard_theme" -> this?.keyboardThemeId == item.id
    "name_style" -> this?.nameStyleId == item.id
    "victory_effect" -> this?.victoryEffectId == item.id
    "emoji_pack" -> this?.emojiPackId == item.id
    else -> false
}

private fun friendlyCollectionName(id: String): String = when (id) {
    "keyboard_crystal" -> "Kristal"
    "keyboard_obsidian" -> "Obsidyen"
    "frame_round_ocean" -> "Okyanus"
    "frame_round_botanic" -> "Botanik"
    "frame_round_lilac" -> "Lila"
    "frame_round_rose" -> "Gül"
    else -> id.removePrefix("frame_").removePrefix("keyboard_").replace('_', ' ').take(16)
}
