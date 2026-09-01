package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.launch

@Composable
internal fun MonsterStyleStoreScreen() {
    var showFullCatalog by remember { mutableStateOf(false) }
    if (showFullCatalog) {
        EconomyShopScreen(onBack = { showFullCatalog = false })
        return
    }

    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var theme by remember { mutableStateOf<ShopItemDto?>(null) }
    var owned by remember { mutableStateOf(false) }
    var equipped by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        val b = backend ?: return
        val id = b.currentUserId() ?: return
        runCatching {
            val p = b.getProfile(id)
            val items = b.getShopItems()
            val inventory = b.getInventory()
            val eq = b.getEquippedCosmetics()
            profile = p
            theme = items.firstOrNull { it.id == "theme_monster_blue" }
            owned = "theme_monster_blue" in inventory
            equipped = eq?.gameThemeId == "theme_monster_blue"
            SonHarfCosmetics.apply(eq)
        }.onFailure {
            notice = sh("Tema mağazası yüklenemedi.", "Theme store could not be loaded.")
        }
    }

    LaunchedEffect(Unit) { reload() }

    Surface(Modifier.fillMaxSize(), color = Color(0xFFF7FAFF)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("STYLE", color = Color(0xFF10213A), fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text(sh("Temalar ve görünüm paketleri", "Themes and appearance packs"), color = Color(0xFF62758F), fontSize = 10.sp)
                    }
                    Surface(shape = RoundedCornerShape(99.dp), color = Color(0xFFE8F1FF), border = BorderStroke(1.dp, Color(0xFFBED5F5))) {
                        Text("◈ ${profile?.diamonds ?: 0} SC", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Color(0xFF0A66D8), fontWeight = FontWeight.Black)
                    }
                }
            }

            item {
                Text(sh("PREMIUM TEMA", "PREMIUM THEME"), color = Color(0xFF0A66D8), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(if (equipped) 2.dp else 1.dp, if (equipped) Color(0xFF1677FF) else Color(0xFFD5E2F0)),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                        MonsterBlueThemePreview()
                        Row(verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Text(sh("Mavi Beyaz Arena", "Blue White Arena"), color = Color(0xFF10213A), fontSize = 19.sp, fontWeight = FontWeight.Black)
                                Text(
                                    sh("Yeni Monster düzeninin premium beyaz-mavi renk paketi. Menü, kart ve oyun vurgularını mavi-beyaz stile geçirir.", "Premium blue-white color pack for the new Monster layout. Applies blue-white styling to menus, cards and game accents."),
                                    color = Color(0xFF62758F),
                                    fontSize = 10.sp,
                                )
                            }
                            if (equipped) Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF1677FF), modifier = Modifier.size(26.dp))
                        }

                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            val price = theme?.diamondPrice ?: 600
                            Text(
                                when {
                                    equipped -> sh("AKTİF", "EQUIPPED")
                                    owned -> sh("✓ SAHİPSİN", "✓ OWNED")
                                    else -> "◈ $price SC"
                                },
                                color = if (owned || equipped) Color(0xFF168A55) else Color(0xFF0A66D8),
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                            )
                            Button(
                                enabled = !busy && !equipped && theme != null,
                                onClick = {
                                    val b = backend ?: return@Button
                                    scope.launch {
                                        busy = true
                                        runCatching {
                                            if (!owned) b.purchaseShopItem("theme_monster_blue")
                                            b.equipShopItem("theme_monster_blue")
                                        }.onSuccess {
                                            notice = sh("Mavi Beyaz Arena etkinleştirildi.", "Blue White Arena equipped.")
                                            reload()
                                        }.onFailure {
                                            val raw = it.message.orEmpty()
                                            notice = if ("insufficient_diamonds" in raw) sh("Yeterli Son Coin'in yok.", "Not enough Son Coin.") else sh("Tema etkinleştirilemedi.", "Theme could not be equipped.")
                                        }
                                        busy = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1677FF), contentColor = Color.White),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(if (owned) Icons.Rounded.Palette else Icons.Rounded.ShoppingBag, null, Modifier.size(17.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (busy) "…" else if (owned) sh("KULLAN", "EQUIP") else sh("SATIN AL", "BUY"), fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }

            item {
                Surface(color = Color(0xFFEAF3FF), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFC7DCF7))) {
                    Text(
                        sh("Bu tema yalnız görünümü değiştirir. Puan, süre, joker gücü veya lig avantajı vermez.", "This theme only changes appearance. It gives no score, timer, power or league advantage."),
                        Modifier.fillMaxWidth().padding(12.dp),
                        color = Color(0xFF31506F),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            notice?.let { message ->
                item {
                    Surface(color = Color.White, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Color(0xFFD5E2F0))) {
                        Text(message, Modifier.fillMaxWidth().padding(11.dp), color = Color(0xFF10213A), fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { showFullCatalog = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    border = BorderStroke(1.dp, Color(0xFF1677FF)),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(sh("DİĞER STYLE ÜRÜNLERİ", "OTHER STYLE ITEMS"), color = Color(0xFF0A66D8), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun MonsterBlueThemePreview() {
    Surface(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        color = Color(0xFFF4F8FE),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFD5E2F0)),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(34.dp), shape = RoundedCornerShape(10.dp), color = Color(0xFF1677FF)) {}
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Box(Modifier.fillMaxWidth(.55f).height(8.dp).background(Color(0xFF10213A), RoundedCornerShape(99.dp)))
                    Spacer(Modifier.height(5.dp))
                    Box(Modifier.fillMaxWidth(.35f).height(6.dp).background(Color(0xFF9AAAC0), RoundedCornerShape(99.dp)))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(Modifier.weight(1f).height(56.dp), color = Color.White, shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, Color(0xFFD5E2F0))) {}
                Surface(Modifier.weight(1f).height(56.dp), color = Color(0xFFE8F1FF), shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, Color(0xFFB9D4FA))) {}
            }
            Box(Modifier.fillMaxWidth().height(24.dp).background(Color(0xFF1677FF), RoundedCornerShape(10.dp)))
        }
    }
}
