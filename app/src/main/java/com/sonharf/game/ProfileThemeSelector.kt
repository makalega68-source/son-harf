package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ShopItemDto
import com.sonharf.game.data.getEquippedCosmetics
import com.sonharf.game.data.getInventory
import com.sonharf.game.data.getShopItems
import com.sonharf.game.data.equipShopItem
import kotlinx.coroutines.launch

/**
 * Profile-only theme switcher. It never purchases a product.
 * A theme can be equipped only when the backend inventory says the user owns it.
 */
@Composable
internal fun ProfileThemeSelector(backend: OnlineGameBackend) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<ShopItemDto>>(emptyList()) }
    var owned by remember { mutableStateOf<Set<String>>(emptySet()) }
    var activeId by remember { mutableStateOf<String?>(null) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        loading = true
        runCatching {
            items = backend.getShopItems()
                .filter { it.kind == "game_theme" && SonHarfThemeCatalog.known(it.id) }
                .sortedBy { it.sortOrder }
            owned = backend.getInventory()
            val equipped = backend.getEquippedCosmetics()
            activeId = equipped?.gameThemeId
            SonHarfCosmetics.apply(equipped)
        }.onFailure {
            notice = sh("Tema bilgileri yüklenemedi.", "Theme data could not be loaded.")
        }
        loading = false
    }

    LaunchedEffect(Unit) { reload() }
    val shell = SonHarfCosmetics.currentThemePalette

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Palette, null, tint = shell.accent, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(7.dp))
            Column(Modifier.weight(1f)) {
                Text(sh("TEMA", "THEME"), color = shell.text, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Text(sh("Satın aldığın temalar arasında geçiş yap", "Switch between themes you own"), color = shell.muted, fontSize = 8.5.sp)
            }
        }

        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = shell.accent, trackColor = shell.surfaceSoft)
        }

        items.forEach { item ->
            val palette = SonHarfThemeCatalog.forId(item.id)
            val mine = item.id in owned
            val active = activeId == item.id
            Surface(
                shape = RoundedCornerShape(17.dp),
                color = shell.surface,
                border = BorderStroke(if (active) 2.dp else 1.dp, if (active) shell.accent else shell.border),
            ) {
                Column(Modifier.fillMaxWidth().padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MiniThemeSwatch(palette)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (SonHarfUiState.isEnglish) item.nameEn else item.nameTr, color = shell.text, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            Text(
                                when {
                                    active -> sh("Şu anda kullanılıyor", "Currently equipped")
                                    mine -> sh("Satın alındı", "Owned")
                                    else -> sh("Mağazadan satın alınmalı", "Purchase from the store")
                                },
                                color = if (mine || active) shell.green else shell.muted,
                                fontSize = 8.5.sp,
                            )
                        }
                        when {
                            active -> Icon(Icons.Rounded.CheckCircle, null, tint = shell.green, modifier = Modifier.size(22.dp))
                            !mine -> Icon(Icons.Rounded.Lock, null, tint = shell.muted, modifier = Modifier.size(20.dp))
                            else -> Button(
                                onClick = {
                                    scope.launch {
                                        busyId = item.id
                                        runCatching { backend.equipShopItem(item.id) }
                                            .onSuccess {
                                                notice = sh("Tema değiştirildi.", "Theme changed.")
                                                reload()
                                            }
                                            .onFailure { notice = sh("Tema değiştirilemedi.", "Theme could not be changed.") }
                                        busyId = null
                                    }
                                },
                                enabled = busyId == null,
                                shape = RoundedCornerShape(11.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = shell.accent, contentColor = shell.accentText),
                                contentPadding = PaddingValues(horizontal = 11.dp, vertical = 7.dp),
                            ) { Text(if (busyId == item.id) "…" else sh("KULLAN", "EQUIP"), fontSize = 8.sp, fontWeight = FontWeight.Black) }
                        }
                    }
                }
            }
        }

        notice?.let { Text(it, color = shell.muted, fontSize = 8.5.sp) }
    }
}

@Composable
private fun MiniThemeSwatch(palette: SonHarfThemePalette) {
    Surface(
        modifier = Modifier.size(width = 68.dp, height = 42.dp),
        shape = RoundedCornerShape(12.dp),
        color = palette.background,
        border = BorderStroke(1.dp, palette.border),
    ) {
        Row(Modifier.fillMaxSize().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.weight(1f).fillMaxHeight(), shape = RoundedCornerShape(7.dp), color = palette.surface) {}
            Surface(Modifier.width(15.dp).fillMaxHeight(), shape = RoundedCornerShape(7.dp), color = palette.accent) {}
        }
    }
}
