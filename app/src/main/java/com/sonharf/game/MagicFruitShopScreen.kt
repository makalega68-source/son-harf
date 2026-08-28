package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
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
internal fun MagicFruitShopScreen() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val scope = rememberCoroutineScope()
    var fruits by remember { mutableStateOf<List<MascotFruitDto>>(emptyList()) }
    var inventory by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var balance by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        val b = backend ?: return
        val id = b.currentUserId() ?: return
        fruits = runCatching { b.getMascotFruitCatalog() }.getOrDefault(emptyList()).filter { it.isMagic }
        inventory = runCatching { b.getMascotFruitInventory() }.getOrDefault(emptyList()).associate { it.fruitId to it.quantity }
        balance = runCatching { b.getProfile(id).diamonds }.getOrDefault(balance)
    }

    LaunchedEffect(Unit) { reload() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = LetharaPalette.Panel,
                border = BorderStroke(1.dp, LetharaPalette.Gold.copy(alpha = .45f)),
            ) {
                Column(Modifier.fillMaxWidth().padding(15.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = LetharaPalette.Gold)
                        Spacer(Modifier.width(8.dp))
                        Text(sh("BÜYÜLÜ MEYVELER", "MAGIC FRUIT"), color = LetharaPalette.Gold, fontWeight = FontWeight.Black, fontSize = 19.sp)
                        Spacer(Modifier.weight(1f))
                        Text(balance.toString() + " SC", color = LetharaPalette.Text, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        sh(
                            "Bu meyveler yalnızca maskot XP'sini, hafıza parçalarını ve kozmetik hikâye ilerlemesini hızlandırır; PvP avantajı vermez.",
                            "These fruits only accelerate mascot XP, memory fragments and cosmetic story progression; they never grant PvP power.",
                        ),
                        color = LetharaPalette.Muted,
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                    )
                }
            }
        }

        items(fruits, key = { it.id }) { fruit ->
            val count = inventory[fruit.id] ?: 0
            val icon = if (fruit.xpReward == 10) "🌙" else if (fruit.xpReward == 20) "⭐" else "🔮"
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = LetharaPalette.Panel,
                border = BorderStroke(1.dp, LetharaPalette.Gold.copy(alpha = .32f)),
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(icon, fontSize = 35.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (SonHarfUiState.isEnglish) fruit.nameEn else fruit.nameTr, color = LetharaPalette.Text, fontWeight = FontWeight.Black, fontSize = 15.sp)
                            Text(if (SonHarfUiState.isEnglish) fruit.descriptionEn else fruit.descriptionTr, color = LetharaPalette.Muted, fontSize = 9.sp)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("+" + fruit.xpReward + " XP", color = LetharaPalette.Cyan, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(12.dp))
                        Text(sh("Çantada", "In bag") + ": " + count, color = LetharaPalette.Muted, fontSize = 10.sp)
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = {
                                val b = backend
                                if (b != null) {
                                    scope.launch {
                                        busy = fruit.id
                                        runCatching { b.buyMascotFruit(fruit.id, 1) }
                                            .onSuccess {
                                                notice = sh("Büyülü meyve çantana eklendi.", "Magic fruit added to your bag.")
                                                reload()
                                            }
                                            .onFailure { error ->
                                                notice = if ("insufficient_diamonds" in error.message.orEmpty()) {
                                                    sh("Yeterli Son Coin yok.", "Not enough Son Coin.")
                                                } else {
                                                    sh("Satın alma tamamlanamadı.", "Purchase could not be completed.")
                                                }
                                            }
                                        busy = null
                                    }
                                }
                            },
                            enabled = busy == null && balance >= fruit.sonCoinPrice,
                            colors = ButtonDefaults.buttonColors(containerColor = LetharaPalette.Violet),
                        ) {
                            Text(if (busy == fruit.id) "…" else fruit.sonCoinPrice.toString() + " SC", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        notice?.let {
            item {
                Text(it, modifier = Modifier.fillMaxWidth(), color = LetharaPalette.Cyan, fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}
