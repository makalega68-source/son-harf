package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun ProfileStyleInventoryScreen(
    backend: OnlineGameBackend,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var items by remember { mutableStateOf<List<ShopItemDto>>(emptyList()) }
    var owned by remember { mutableStateOf<Set<String>>(emptySet()) }
    var equipped by remember { mutableStateOf<EquippedCosmeticsDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun reload() = coroutineScope {
        loading = true
        val id = backend.currentUserId()
        val profileTask = async { id?.let { runCatching { backend.getProfile(it) }.getOrNull() } }
        val itemTask = async { runCatching { backend.getShopItems() }.getOrDefault(emptyList()) }
        val ownedTask = async { runCatching { backend.getInventory() }.getOrDefault(emptySet()) }
        val equippedTask = async { runCatching { backend.getEquippedCosmetics() }.getOrNull() }
        profile = profileTask.await()
        items = itemTask.await()
        owned = ownedTask.await()
        equipped = equippedTask.await()
        SonHarfCosmetics.apply(equipped)
        loading = false
    }

    LaunchedEffect(Unit) { reload() }

    fun active(item: ShopItemDto): Boolean = when (item.kind) {
        "profile_frame" -> equipped?.profileFrameId == item.id
        "name_style" -> equipped?.nameStyleId == item.id
        "game_theme" -> equipped?.gameThemeId == item.id
        "keyboard_theme" -> equipped?.keyboardThemeId == item.id
        "victory_effect" -> equipped?.victoryEffectId == item.id
        "emoji_pack" -> equipped?.emojiPackId == item.id
        "mascot" -> equipped?.mascotId == item.id
        else -> false
    }

    val ownedItems = items.filter { it.id in owned }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            MainScreenHeader(
                title = sh("Görünümümü düzenle", "Edit appearance"),
                subtitle = sh("Sahip olduğun Style öğelerini profilinde uygula", "Apply owned Style items to your profile"),
                onBack = onBack,
            )
        }

        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = MainUi.Blue, trackColor = MainUi.BlueSoft) }

        item {
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = MainUi.Surface, border = BorderStroke(1.dp, MainUi.Border)) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MainUi.BlueSoft) { Icon(Icons.Rounded.Palette, null, tint = MainUi.Blue, modifier = Modifier.padding(10.dp).size(24.dp)) }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(profile?.displayName ?: sh("Profil", "Profile"), color = MainUi.Text, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Text(sh("${ownedItems.size} Style öğesine sahipsin", "You own ${ownedItems.size} Style items"), color = MainUi.Muted, fontSize = 9.sp)
                    }
                }
            }
        }

        if (!loading && ownedItems.isEmpty()) {
            item {
                Surface(color = MainUi.SurfaceSoft, shape = RoundedCornerShape(18.dp)) {
                    Text(sh("Henüz Style öğen yok. Style mağazasından keşfedip satın alabilirsin.", "You do not own Style items yet. Discover them in the Style shop."), Modifier.fillMaxWidth().padding(15.dp), color = MainUi.Muted, fontSize = 10.sp)
                }
            }
        }

        items(ownedItems, key = { it.id }) { item ->
            val selected = active(item)
            val name = if (SonHarfUiState.isEnglish) item.nameEn else item.nameTr
            val description = if (SonHarfUiState.isEnglish) item.descriptionEn else item.descriptionTr
            Surface(
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MainUi.Surface,
                border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) MainUi.Blue else MainUi.Border),
            ) {
                Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    InventoryStylePreview(item = item, selected = selected)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, color = MainUi.Text, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Text(description, color = MainUi.Muted, fontSize = 8.5.sp, maxLines = 2)
                        Text(styleKindLabel(item.kind), color = MainUi.Blue, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (busy != null || selected) return@Button
                            scope.launch {
                                busy = item.id
                                runCatching { backend.equipShopItem(item.id) }
                                    .onSuccess {
                                        notice = sh("$name profiline uygulandı.", "$name applied to your profile.")
                                        reload()
                                    }
                                    .onFailure { notice = sh("Style öğesi uygulanamadı.", "Style item could not be applied.") }
                                busy = null
                            }
                        },
                        enabled = busy == null && !selected,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MainUi.Blue,
                            disabledContainerColor = if (selected) MainUi.Green.copy(alpha = .14f) else MainUi.SurfaceSoft,
                            disabledContentColor = if (selected) MainUi.Green else MainUi.Muted,
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                    ) {
                        Text(when { busy == item.id -> "…"; selected -> sh("AKTİF", "ACTIVE"); else -> sh("UYGULA", "APPLY") }, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        notice?.let {
            item { Surface(color = MainUi.SurfaceSoft, shape = RoundedCornerShape(14.dp)) { Text(it, Modifier.fillMaxWidth().padding(12.dp), color = MainUi.Text, fontSize = 10.sp) } }
        }
        item { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
private fun InventoryStylePreview(item: ShopItemDto, selected: Boolean) {
    val frameAccent = SonHarfCosmetics.frameAccent(item.id)
    val accent = when (item.kind) {
        "profile_frame" -> frameAccent
        "keyboard_theme" -> SonHarfCosmetics.keyboardPaletteFor(item.id).accent
        "game_theme" -> SonHarfCosmetics.gamePaletteFor(item.id).accent
        "name_style" -> if ("gold" in item.id) MainUi.Gold else if ("cyan" in item.id) SonHarfCyan else MainUi.Text
        else -> MainUi.Purple
    }
    Surface(
        modifier = Modifier.size(58.dp),
        shape = RoundedCornerShape(15.dp),
        color = accent.copy(alpha = .08f),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accent else accent.copy(alpha = .22f)),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (item.kind) {
                "profile_frame" -> {
                    Box(Modifier.size(42.dp, 48.dp), contentAlignment = Alignment.Center) {
                        Surface(Modifier.fillMaxSize(), shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(3.dp, frameAccent)) {}
                        Icon(Icons.Rounded.Person, null, tint = MainUi.Text.copy(alpha = .70f), modifier = Modifier.size(24.dp))
                        if ("neon" in item.id) Text("✦", Modifier.align(Alignment.TopEnd), color = SonHarfCyan, fontSize = 12.sp)
                        if ("gold" in item.id || item.vipOnly) Text("VIP", Modifier.align(Alignment.BottomCenter), color = MainUi.Gold, fontSize = 6.sp, fontWeight = FontWeight.Black)
                    }
                }
                "keyboard_theme" -> {
                    val palette = SonHarfCosmetics.keyboardPaletteFor(item.id)
                    Surface(Modifier.size(46.dp, 34.dp), shape = RoundedCornerShape(7.dp), color = palette.background) {
                        Column(Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            repeat(3) { r -> Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) { repeat(4) { c -> Surface(Modifier.weight(1f).fillMaxHeight(), shape = RoundedCornerShape(2.dp), color = if (r == 2 && c == 3) palette.action else palette.key) {} } } }
                        }
                    }
                }
                "game_theme" -> {
                    val palette = SonHarfCosmetics.gamePaletteFor(item.id)
                    Surface(Modifier.size(46.dp, 34.dp), shape = RoundedCornerShape(7.dp), color = palette.background) {
                        Row(Modifier.fillMaxSize().padding(5.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Surface(Modifier.weight(1f).fillMaxHeight(), shape = RoundedCornerShape(3.dp), color = palette.accent) {}
                            Surface(Modifier.weight(1f).fillMaxHeight(), shape = RoundedCornerShape(3.dp), color = palette.secondary) {}
                        }
                    }
                }
                "name_style" -> Text("Aa", color = accent, fontSize = 20.sp, fontWeight = FontWeight.Black)
                "emoji_pack" -> Text("😎", fontSize = 22.sp)
                "victory_effect" -> Text("✦", color = accent, fontSize = 24.sp, fontWeight = FontWeight.Black)
                "mascot" -> Text("●", color = accent, fontSize = 24.sp)
                else -> Icon(Icons.Rounded.GridOn, null, tint = accent, modifier = Modifier.size(25.dp))
            }
            if (selected) {
                Surface(modifier = Modifier.align(Alignment.TopEnd).padding(2.dp), shape = CircleShape, color = MainUi.Green) {
                    Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.padding(2.dp).size(10.dp))
                }
            }
        }
    }
}

@Composable
private fun styleKindLabel(kind: String): String = when (kind) {
    "profile_frame" -> sh("Profil çerçevesi", "Profile frame")
    "name_style" -> sh("İsim stili", "Name style")
    "emoji_pack" -> sh("İfade / emoji", "Emote / emoji")
    "victory_effect" -> sh("Zafer efekti", "Victory effect")
    "keyboard_theme" -> sh("Klavye teması", "Keyboard theme")
    "game_theme" -> sh("Görsel tema", "Visual theme")
    "mascot" -> sh("Maskot", "Mascot")
    else -> "Style"
}
