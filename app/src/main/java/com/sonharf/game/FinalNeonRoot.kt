package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider

private val FBg = Color(0xFF090F1A)
private val FPanel = Color(0xFF10172B)
private val FPanel2 = Color(0xFF131D35)
private val FCyan = Color(0xFF00E5FF)
private val FPurple = Color(0xFF7B2FFF)
private val FPink = Color(0xFFFF4D6D)
private val FGold = Color(0xFFFFC107)
private val FBlue = Color(0xFF168CFF)
private val FText = Color(0xFFF5F7FF)
private val FMuted = Color(0xFF91A1BE)

private enum class FinalScreen { HOME, GAME, SHOP, PROFILE, HUB, RANKING }

@Composable
fun FinalNeonSonHarfApp() {
    var screen by remember { mutableStateOf(FinalScreen.HOME) }
    var checked by remember { mutableStateOf(false) }
    var authenticated by remember { mutableStateOf(false) }
    var vip by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { authenticated = SupabaseProvider.configured && hasVerifiedMembershipSession(); checked = true }
    if (!checked) { Box(Modifier.fillMaxSize().background(FBg), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = FCyan) }; return }
    if (!authenticated) { RequiredAuthGate { authenticated = true }; return }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF070C17), FBg, Color(0xFF050912))))) {
        when (screen) {
            FinalScreen.HOME -> FinalHome(
                onPlay = { screen = FinalScreen.GAME },
                onShop = { screen = FinalScreen.SHOP },
                onProfile = { screen = FinalScreen.PROFILE },
                onHub = { screen = FinalScreen.HUB },
                onRanking = { screen = FinalScreen.RANKING },
                onVip = { vip = true },
            )
            FinalScreen.GAME -> TargetNeonGameScreen()
            FinalScreen.SHOP -> FinalGeneric("MAĞAZA", "KOZMETİK & JETON", "Düellonu kişiselleştir", "🛍", FPurple, { screen = FinalScreen.HOME }) {
                FinalTile("♛", "VIP ÜYELİK", "Özel oda, özel görünüm ve bonuslar", FGold) { vip = true }
                FinalTile("◆", "JETON PAKETLERİ", "Oyun içi avantaj ve kozmetik", FCyan) { }
                FinalTile("✦", "NEON TEMALAR", "Arena efektlerini değiştir", FPink) { }
            }
            FinalScreen.PROFILE -> FinalProfile { screen = FinalScreen.HOME }
            FinalScreen.HUB -> FinalGeneric("OYUNCU MERKEZİ", "KARİYER MERKEZİ", "Hedeflerini büyüt, serini koru", "🚀", FCyan, { screen = FinalScreen.HOME }) {
                FinalTile("🏆", "SIRALAMA", "Haftalık ve genel lig", FGold) { screen = FinalScreen.RANKING }
                FinalTile("🎯", "GÖREVLER", "Günlük ve haftalık meydan okumalar", FPink) { }
                FinalTile("🛍", "MAĞAZA", "Jetonlar ve görsel öğeler", FCyan) { screen = FinalScreen.SHOP }
                FinalTile("♙", "PROFİL", "İstatistikler ve kimlik", FPurple) { screen = FinalScreen.PROFILE }
            }
            FinalScreen.RANKING -> FinalRanking { screen = FinalScreen.HOME }
        }
    }
    if (vip) VipPurchaseDialog { vip = false }
}

@Composable
private fun FinalHome(onPlay: () -> Unit, onShop: () -> Unit, onProfile: () -> Unit, onHub: () -> Unit, onRanking: () -> Unit, onVip: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    LaunchedEffect(Unit) { backend.currentUserId()?.let { profile = runCatching { backend.getProfile(it) }.getOrNull() } }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FinalAvatar(profile?.displayName ?: "Oyuncu", 48.dp)
                Spacer(Modifier.width(10.dp))
                Column { Text(profile?.displayName ?: "Oyuncu", color = FText, fontWeight = FontWeight.Black, fontSize = 16.sp); Text(if (profile?.isVip == true) "Usta • VIP" else "Usta", color = FGold, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FinalPill("🏆", "${profile?.wins ?: 0}", FGold); FinalPill("◆", "${profile?.diamonds ?: 0}", FCyan) }
        }

        Card(modifier = Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, FPurple.copy(alpha = .60f))) {
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(FPurple.copy(alpha = .32f), Color(0xFF0D1427), FBg))), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SON", color = FGold, fontWeight = FontWeight.Black, fontSize = 54.sp, letterSpacing = 2.sp)
                    Text("HARF", color = FCyan, fontWeight = FontWeight.Black, fontSize = 54.sp, letterSpacing = 2.sp)
                    Text("NEON KELİME DÜELLOSU", color = FText, fontSize = 11.sp, letterSpacing = 1.5.sp)
                }
            }
        }

        FinalAction("HEMEN OYNA", "Rastgele Rakip", FGold, Color(0xFF211500), onPlay)
        FinalAction("ARKADAŞINLA OYNA", "Davet Gönder", FPurple, Color.White, onPlay)
        FinalAction("ODA KUR", "Özel Eşleşme", FBlue, Color.White, onPlay)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            FinalQuick("✓", "GÖREVLER", FCyan, onHub)
            FinalQuick("♛", "SIRALAMA", FGold, onRanking)
            FinalQuick("🛒", "MAĞAZA", FCyan, onShop)
            FinalQuick("♙", "PROFİL", FCyan, onProfile)
        }
        TextButton(onClick = onVip, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("VIP AVANTAJLARI", color = FGold, fontWeight = FontWeight.Bold, fontSize = 10.sp) }
    }
}

@Composable
private fun FinalProfile(onBack: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    var p by remember { mutableStateOf<ProfileDto?>(null) }
    LaunchedEffect(Unit) { backend.currentUserId()?.let { p = runCatching { backend.getProfile(it) }.getOrNull() } }
    FinalGeneric("PROFİL", p?.displayName ?: "Oyuncu", "SON HARF OYUNCUSU", "♙", FPink, onBack) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FinalStat("${p?.wins ?: 0}", "Galibiyet", Modifier.weight(1f)); FinalStat("${p?.losses ?: 0}", "Mağlubiyet", Modifier.weight(1f)); val total = (p?.wins ?: 0) + (p?.losses ?: 0); FinalStat("%${if (total == 0) 0 else (p?.wins ?: 0) * 100 / total}", "Kazanma", Modifier.weight(1f))
        }
        FinalTile("🏆", "BAŞARILAR", "Seriler ve kilometre taşları", FGold) { }
        FinalTile("👥", "ARKADAŞLAR", "Çevrimiçi oyuncular ve davetler", FCyan) { }
    }
}

@Composable
private fun FinalRanking(onBack: () -> Unit) {
    FinalGeneric("SIRALAMA", "NEON LİG", "En iyi kelime düellocuları", "🏆", FGold, onBack) {
        listOf("KAĞAN", "ZEYNEP", "OYUNCU-4555", "ÜMİT", "KELİME USTASI").forEachIndexed { i, name ->
            Card(colors = CardDefaults.cardColors(containerColor = FPanel), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, if (i == 0) FGold.copy(alpha = .55f) else Color.White.copy(alpha = .08f))) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("${i + 1}  $name", color = FText, fontWeight = FontWeight.Black); Text("${1250 - i * 70}", color = if (i == 0) FGold else FCyan, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun FinalGeneric(title: String, hero: String, subtitle: String, icon: String, accent: Color, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), contentPadding = PaddingValues(top = 18.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { OutlinedButton(onClick = onBack, modifier = Modifier.size(42.dp), contentPadding = PaddingValues(0.dp), shape = CircleShape, border = BorderStroke(1.dp, FCyan.copy(alpha = .40f))) { Text("‹", color = FCyan, fontSize = 25.sp) }; Spacer(Modifier.width(14.dp)); Text(title, color = FText, fontWeight = FontWeight.Black, fontSize = 24.sp) } }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(26.dp), border = BorderStroke(1.dp, accent.copy(alpha = .42f))) {
                Row(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(accent.copy(alpha = .22f), FPanel))).padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Text(icon, fontSize = 42.sp); Spacer(Modifier.width(14.dp)); Column { Text(hero, color = FText, fontSize = 20.sp, fontWeight = FontWeight.Black); Text(subtitle, color = FMuted, fontSize = 10.sp) } }
            }
        }
        item { Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content) }
    }
}

@Composable private fun FinalTile(icon: String, title: String, subtitle: String, accent: Color, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = FPanel), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, accent.copy(alpha = .26f))) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = .14f)), contentAlignment = Alignment.Center) { Text(icon, fontSize = 23.sp) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, color = FText, fontWeight = FontWeight.Black); Text(subtitle, color = FMuted, fontSize = 9.sp) }; Text("›", color = accent, fontSize = 28.sp) }
    }
}
@Composable private fun FinalAction(title: String, subtitle: String, bg: Color, fg: Color, onClick: () -> Unit) { Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(62.dp), colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = fg), shape = RoundedCornerShape(17.dp)) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(title, fontWeight = FontWeight.Black, fontSize = 16.sp); Text(subtitle, fontSize = 8.sp) } } }
@Composable private fun FinalQuick(icon: String, label: String, accent: Color, onClick: () -> Unit) { TextButton(onClick = onClick, contentPadding = PaddingValues(2.dp)) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(icon, color = accent, fontSize = 20.sp); Text(label, color = FMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold) } } }
@Composable private fun FinalPill(icon: String, value: String, accent: Color) { Surface(color = FPanel, shape = RoundedCornerShape(99.dp), border = BorderStroke(1.dp, accent.copy(alpha = .25f))) { Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { Text(icon, fontSize = 11.sp); Spacer(Modifier.width(4.dp)); Text(value, color = FText, fontWeight = FontWeight.Black, fontSize = 10.sp) } } }
@Composable private fun FinalAvatar(name: String, size: androidx.compose.ui.unit.Dp) { Box(Modifier.size(size).clip(CircleShape).background(Brush.sweepGradient(listOf(FCyan, FPurple, FPink, FGold, FCyan))).padding(3.dp), contentAlignment = Alignment.Center) { Box(Modifier.fillMaxSize().clip(CircleShape).background(FPanel2), contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), color = FText, fontWeight = FontWeight.Black, fontSize = (size.value * .38f).sp) } } }
@Composable private fun FinalStat(value: String, label: String, modifier: Modifier) { Surface(modifier, color = FPanel2, shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, color = FText, fontWeight = FontWeight.Black, fontSize = 19.sp); Text(label, color = FMuted, fontSize = 8.sp) } } }
