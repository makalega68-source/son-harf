package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.*
import kotlinx.coroutines.launch

private val StoreBg = Color(0xFFF5F8FC)
private val StoreSurface = Color.White
private val StoreAlt = Color(0xFFEEF5FF)
private val StoreBlue = Color(0xFF1677FF)
private val StoreBlue2 = Color(0xFF1687F8)
private val StoreText = Color(0xFF142033)
private val StoreMuted = Color(0xFF6F7C8D)
private val StoreBorder = Color(0xFFD5E0EA)
private val StoreGreen = Color(0xFF2FAE68)
private val StoreGold = Color(0xFFF6C453)

private data class StylePreview(
    val titleTr: String,
    val titleEn: String,
    val subtitleTr: String,
    val subtitleEn: String,
    val icon: ImageVector,
    val accent: Color,
)

@Composable
internal fun MonsterStyleStoreScreen() {
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
            val shop = b.getShopItems()
            val inventory = b.getInventory()
            val eq = b.getEquippedCosmetics()
            profile = p
            theme = shop.firstOrNull { it.id == "theme_monster_blue" && it.active }
            owned = "theme_monster_blue" in inventory
            equipped = eq?.gameThemeId == "theme_monster_blue"
            SonHarfCosmetics.apply(eq)
        }.onFailure {
            notice = sh("Style mağazası yüklenemedi.", "Style shop could not be loaded.")
        }
    }

    LaunchedEffect(Unit) { reload() }

    val profileItems = listOf(
        StylePreview("Arena Çerçevesi", "Arena Frame", "Profil ve eşleşme kartı çerçevesi", "Profile and matchup frame", Icons.Rounded.AccountCircle, StoreBlue),
        StylePreview("İsim Plakası", "Nameplate", "İsmini premium bir plakada göster", "Show your name on a premium plate", Icons.Rounded.Badge, Color(0xFF6C63D9)),
        StylePreview("Profil Arka Planı", "Profile Background", "Profil yüzeyini kişiselleştir", "Personalize your profile surface", Icons.Rounded.Wallpaper, Color(0xFF31A6A6)),
    )
    val matchItems = listOf(
        StylePreview("VS Giriş Efekti", "VS Intro", "Maç açılışında güç vermeyen görsel intro", "Visual match intro with no gameplay power", Icons.Rounded.Bolt, StoreBlue),
        StylePreview("Kelime Gönderme", "Word Send Effect", "Kelime kabulünde kısa premium efekt", "Short premium effect on accepted words", Icons.Rounded.AutoAwesome, Color(0xFF7A62D3)),
        StylePreview("Zafer Konfetisi", "Victory Confetti", "Galibiyet ekranı kutlama efekti", "Victory screen celebration effect", Icons.Rounded.Celebration, StoreGold),
    )
    val prestigeItems = listOf(
        StylePreview("Usta", "Master", "Güç vermeyen prestij ünvanı", "Prestige title with no power", Icons.Rounded.MilitaryTech, StoreGold),
        StylePreview("Kelime Avcısı", "Word Hunter", "Koleksiyonluk profil etiketi", "Collectible profile label", Icons.Rounded.EmojiEvents, StoreBlue),
        StylePreview("Sezon Rozeti", "Season Badge", "Geçmiş sezon başarısını sergile", "Show past season achievement", Icons.Rounded.WorkspacePremium, Color(0xFF6C63D9)),
    )

    Surface(Modifier.fillMaxSize(), color = StoreBg) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { StoreHeader(profile?.diamonds ?: 0) }
            item { StoreCategoryRail() }
            item {
                StoreSectionHeader(
                    sh("ÖNE ÇIKANLAR", "FEATURED"),
                    sh("Tema • yeni • sınırlı süreli slotlar", "Theme • new • limited-time slots"),
                )
            }
            item {
                FeaturedThemeCard(
                    theme = theme,
                    owned = owned,
                    equipped = equipped,
                    busy = busy,
                    onAction = {
                        val b = backend ?: return@FeaturedThemeCard
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
                                notice = if ("insufficient_diamonds" in raw) {
                                    sh("Yeterli Son Coin'in yok.", "Not enough Son Coin.")
                                } else {
                                    sh("Tema işlemi tamamlanamadı.", "Theme action could not be completed.")
                                }
                            }
                            busy = false
                        }
                    },
                )
            }
            notice?.let { message -> item { StoreNotice(message) } }
            item { FairPlayStoreNotice() }

            item { StoreSectionHeader(sh("TEMALAR", "THEMES"), sh("Uygulamanın tüm görsel sistemini değiştir", "Change the app-wide visual system")) }
            item { ThemeSlotRow(equipped) }

            item { StoreSectionHeader(sh("PROFİL STYLE", "PROFILE STYLE"), sh("Çerçeve, plaka, arka plan ve rozet", "Frames, nameplates, backgrounds and badges")) }
            item { PurchasedProfileFramesStoreRow() }

            item { StoreSectionHeader(sh("MAÇ STYLE", "MATCH STYLE"), sh("Sadece görsel efektler; rekabet avantajı yok", "Visual effects only; no competitive advantage")) }
            item { PreviewRow(matchItems) }

            item { StoreSectionHeader(sh("PRESTİJ", "PRESTIGE"), sh("Ünvan, sezon rozeti ve koleksiyon etiketleri", "Titles, season badges and collectible labels")) }
            item { PreviewRow(prestigeItems) }

            item { StoreSectionHeader(sh("PAKETLER", "BUNDLES"), sh("Birbiriyle uyumlu Style setleri", "Matching Style collections")) }
            item { BundleRow() }

            item { StoreSectionHeader("SON COIN", sh("Tek ve sade Style para birimi", "One simple Style currency")) }
            item { SonCoinReadyCard(profile?.diamonds ?: 0) }
        }
    }
}

@Composable
private fun StoreHeader(balance: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("STYLE", color = StoreText, fontSize = 29.sp, fontWeight = FontWeight.Black)
            Text(sh("Görünüm • koleksiyon • prestij", "Appearance • collection • prestige"), color = StoreMuted, fontSize = 10.sp)
        }
        Surface(shape = RoundedCornerShape(99.dp), color = StoreAlt, border = BorderStroke(1.dp, Color(0xFFBED5F5))) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Toll, null, Modifier.size(17.dp), tint = StoreBlue)
                Spacer(Modifier.width(6.dp))
                Text("$balance SC", color = StoreBlue, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun StoreCategoryRail() {
    val categories = listOf(
        sh("Öne Çıkanlar", "Featured"), sh("Temalar", "Themes"), sh("Profil", "Profile"),
        sh("Maç", "Match"), sh("Prestij", "Prestige"), sh("Paketler", "Bundles"), "Son Coin",
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        items(categories) { label ->
            Surface(shape = RoundedCornerShape(99.dp), color = StoreSurface, border = BorderStroke(1.dp, StoreBorder)) {
                Text(label, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = StoreText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StoreSectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = StoreText, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = .5.sp)
        Text(subtitle, color = StoreMuted, fontSize = 9.sp)
    }
}

@Composable
private fun FeaturedThemeCard(theme: ShopItemDto?, owned: Boolean, equipped: Boolean, busy: Boolean, onAction: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StoreSurface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(if (equipped) 2.dp else 1.dp, if (equipped) StoreBlue else StoreBorder),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box {
                MonsterBlueThemePreview()
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(9.dp),
                    color = StoreText.copy(alpha = .88f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(sh("ÖNE ÇIKAN TEMA", "FEATURED THEME"), Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(sh("Mavi Beyaz Arena", "Blue White Arena"), color = StoreText, fontSize = 19.sp, fontWeight = FontWeight.Black)
                    Text(sh("Beyaz yüzeyler, buz mavisi katmanlar ve güçlü modern mavi vurgular.", "White surfaces, ice-blue layers and a strong modern-blue accent."), color = StoreMuted, fontSize = 10.sp)
                }
                if (equipped) Icon(Icons.Rounded.CheckCircle, null, tint = StoreGreen, modifier = Modifier.size(25.dp))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val price = theme?.diamondPrice ?: 600
                Column(Modifier.weight(1f)) {
                    Text(
                        when { equipped -> sh("AKTİF", "ACTIVE"); owned -> sh("SAHİPSİN", "OWNED"); else -> "$price SC" },
                        color = if (owned || equipped) StoreGreen else StoreBlue,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                    )
                    Text(sh("Gerçek önizleme • kalıcı sahiplik", "Real preview • permanent ownership"), color = StoreMuted, fontSize = 8.sp)
                }
                Button(
                    enabled = !busy && !equipped && theme != null,
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = StoreBlue, contentColor = Color.White, disabledContainerColor = Color(0xFFDDE7F3), disabledContentColor = Color(0xFF66758A)),
                    shape = RoundedCornerShape(13.dp),
                ) {
                    Icon(if (owned) Icons.Rounded.Palette else Icons.Rounded.ShoppingBag, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (busy) "…" else if (owned) sh("KULLAN", "EQUIP") else if (equipped) sh("AKTİF", "ACTIVE") else sh("SATIN AL", "BUY"), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun ThemeSlotRow(equipped: Boolean) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SmallThemeCard(sh("Mavi Beyaz Arena", "Blue White Arena"), if (equipped) sh("Aktif", "Active") else sh("Mağazada", "In shop"), StoreBlue, true)
        }
        item { SmallThemeCard(sh("Yeni Tema Slotu", "New Theme Slot"), sh("Yakında", "Coming soon"), Color(0xFF6C63D9), false) }
        item { SmallThemeCard(sh("Sezon Teması", "Season Theme"), sh("Gelecek sezon", "Future season"), Color(0xFF31A6A6), false) }
    }
}

@Composable
private fun SmallThemeCard(title: String, state: String, accent: Color, live: Boolean) {
    Surface(modifier = Modifier.width(180.dp), shape = RoundedCornerShape(18.dp), color = StoreSurface, border = BorderStroke(1.dp, StoreBorder)) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.fillMaxWidth().height(70.dp).clip(RoundedCornerShape(13.dp)).background(Brush.linearGradient(listOf(accent.copy(alpha = .16f), StoreSurface)))) {
                Box(Modifier.align(Alignment.Center).size(38.dp).clip(RoundedCornerShape(11.dp)).background(accent))
            }
            Text(title, color = StoreText, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text(state, color = if (live) StoreGreen else StoreMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PreviewRow(previews: List<StylePreview>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(previews) { preview -> StylePreviewCard(preview) }
    }
}

@Composable
private fun StylePreviewCard(preview: StylePreview) {
    Surface(modifier = Modifier.width(178.dp), shape = RoundedCornerShape(18.dp), color = StoreSurface, border = BorderStroke(1.dp, StoreBorder)) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(13.dp)).background(preview.accent.copy(alpha = .09f)), contentAlignment = Alignment.Center) {
                Surface(shape = CircleShape, color = preview.accent.copy(alpha = .15f)) {
                    Icon(preview.icon, null, Modifier.padding(13.dp).size(28.dp), tint = preview.accent)
                }
            }
            Text(sh(preview.titleTr, preview.titleEn), color = StoreText, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text(sh(preview.subtitleTr, preview.subtitleEn), color = StoreMuted, fontSize = 8.sp, minLines = 2)
            Surface(shape = RoundedCornerShape(99.dp), color = StoreAlt) {
                Text(sh("YAKINDA", "COMING SOON"), Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = StoreBlue, fontSize = 7.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun BundleRow() {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        item { BundleCard(sh("Başlangıç Style Paketi", "Starter Style Bundle"), Icons.Rounded.RocketLaunch, StoreBlue) }
        item { BundleCard(sh("Premium Style Paketi", "Premium Style Bundle"), Icons.Rounded.Diamond, Color(0xFF6C63D9)) }
        item { BundleCard(sh("Sezon Style Paketi", "Season Style Bundle"), Icons.Rounded.CalendarMonth, StoreGold) }
    }
}

@Composable
private fun BundleCard(title: String, icon: ImageVector, accent: Color) {
    Surface(modifier = Modifier.width(190.dp), shape = RoundedCornerShape(18.dp), color = StoreSurface, border = BorderStroke(1.dp, StoreBorder)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(11.dp), color = accent.copy(alpha = .12f)) { Icon(icon, null, Modifier.padding(9.dp).size(23.dp), tint = accent) }
                Spacer(Modifier.width(8.dp))
                Text(title, Modifier.weight(1f), color = StoreText, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            Text(sh("Theme + frame + title için hazır bundle slotu", "Ready bundle slot for theme + frame + title"), color = StoreMuted, fontSize = 8.sp)
            Text(sh("YAKINDA", "COMING SOON"), color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun SonCoinReadyCard(balance: Int) {
    Surface(shape = RoundedCornerShape(20.dp), color = StoreSurface, border = BorderStroke(1.dp, StoreBorder)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = StoreAlt) { Icon(Icons.Rounded.Toll, null, Modifier.padding(10.dp).size(24.dp), tint = StoreBlue) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("$balance Son Coin", color = StoreText, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Text(sh("Tek para birimi • sade ekonomi", "Single currency • simple economy"), color = StoreMuted, fontSize = 9.sp)
                }
            }
            Text(sh("Google Play Billing coin paketleri için alan hazırdır. Gerçek para işlemi bağlanmadan sahte satın alma butonu gösterilmez.", "The area is ready for Google Play Billing coin packs. No fake purchase button is shown before real-money billing is connected."), color = StoreMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun FairPlayStoreNotice() {
    Surface(color = Color(0xFFEAF3FF), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFC7DCF7))) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.VerifiedUser, null, Modifier.size(19.dp), tint = StoreBlue)
            Spacer(Modifier.width(8.dp))
            Text(sh("Style ürünleri yalnız görünüm, koleksiyon ve prestij içindir. Rating, süre, harf, joker veya maç kazanma gücü vermez.", "Style items are appearance, collection and prestige only. They never grant rating, timer, tile, joker or match-winning power."), color = Color(0xFF31506F), fontSize = 9.sp, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StoreNotice(message: String) {
    Surface(color = StoreSurface, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, StoreBorder)) {
        Text(message, Modifier.fillMaxWidth().padding(11.dp), color = StoreText, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun MonsterBlueThemePreview() {
    Surface(modifier = Modifier.fillMaxWidth().height(154.dp), color = Color(0xFFF4F8FE), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, StoreBorder)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(34.dp), shape = RoundedCornerShape(10.dp), color = StoreBlue) {}
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Box(Modifier.fillMaxWidth(.55f).height(8.dp).background(StoreText, RoundedCornerShape(99.dp)))
                    Spacer(Modifier.height(5.dp))
                    Box(Modifier.fillMaxWidth(.35f).height(6.dp).background(Color(0xFF9AAAC0), RoundedCornerShape(99.dp)))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(Modifier.weight(1f).height(57.dp), color = Color.White, shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, StoreBorder)) {}
                Surface(Modifier.weight(1f).height(57.dp), color = StoreAlt, shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, Color(0xFFB9D4FA))) {}
            }
            Box(Modifier.fillMaxWidth().height(24.dp).background(Brush.horizontalGradient(listOf(StoreBlue, StoreBlue2)), RoundedCornerShape(10.dp)))
        }
    }
}
