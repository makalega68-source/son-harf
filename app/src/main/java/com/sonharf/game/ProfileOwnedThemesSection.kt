package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Palette
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
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun reloadThemes() {
        val nextInventory = runCatching { withTimeout(ProfileThemeTimeoutMs) { backend.getInventory() } }
            .getOrDefault(owned)
        val nextEquipped = runCatching { withTimeout(ProfileThemeTimeoutMs) { backend.getEquippedCosmetics() } }
            .getOrNull()
        owned = nextInventory
        equipped = nextEquipped
        SonHarfCosmetics.applyAndPersist(context, nextEquipped)
    }

    fun selectTheme(themeId: String?) {
        if (busy) return
        busy = true
        notice = null
        scope.launch {
            runCatching {
                withTimeout(ProfileThemeTimeoutMs) {
                    if (themeId == null) backend.equipDefaultGameTheme()
                    else backend.equipShopItem(themeId)
                    backend.getEquippedCosmetics()
                }
            }.onSuccess { next ->
                equipped = next
                SonHarfCosmetics.applyAndPersist(context, next)
                notice = sh("Tema uygulandı.", "Theme applied.")
            }.onFailure {
                notice = sh("Tema uygulanamadı. Bağlantını kontrol edip tekrar dene.", "Theme could not be applied. Check your connection and try again.")
            }
            busy = false
        }
    }

    LaunchedEffect(Unit) {
        loading = true
        reloadThemes()
        loading = false
    }

    val darkActive = equipped?.gameThemeId == DarkArenaThemeId

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
                enabled = !busy,
                dark = false,
                modifier = Modifier.weight(1f),
                onClick = { selectTheme(null) },
            )
            if (DarkArenaThemeId in owned) {
                ProfileThemeCard(
                    title = sh("Gece Arenası", "Night Arena"),
                    subtitle = sh("Koleksiyonunda", "In your collection"),
                    active = darkActive,
                    enabled = !busy,
                    dark = true,
                    modifier = Modifier.weight(1f),
                    onClick = { selectTheme(DarkArenaThemeId) },
                )
            }
        }

        notice?.let { Text(it, color = MainUi.Muted, fontSize = 9.sp) }
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
            Text(title, color = MainUi.Text, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(subtitle, color = MainUi.Muted, fontSize = 8.sp, maxLines = 2)
        }
    }
}
