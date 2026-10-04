package com.sonharf.game

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.VipEntitlementsDto
import com.sonharf.game.data.getVipEntitlements
import kotlinx.coroutines.launch

private val UProBg: Color get() = LobbyPalette.Ground
private val UProSurface: Color get() = LobbyPalette.Paper
private val UProBorder: Color get() = LobbyPalette.Line
private val UProText: Color get() = LobbyPalette.Ink
private val UProMuted: Color get() = LobbyPalette.Muted
private val UProBlue: Color get() = LobbyPalette.Green
private val UProGold: Color get() = LobbyPalette.Gold
private val UProGreen: Color get() = LobbyPalette.Accent

@Composable
internal fun UnifiedProVipScreen(
    backend: OnlineGameBackend,
    onBack: () -> Unit = {},
    onPrivateRoom: () -> Unit = {},
    onFriends: () -> Unit = {},
    onQuickDuel: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var entitlements by remember { mutableStateOf<VipEntitlementsDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var entitlementError by remember { mutableStateOf(false) }
    var showPurchase by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        loading = true
        entitlementError = false
        val id = backend.currentUserId()
        profile = id?.let { runCatching { backend.getProfile(it) }.getOrNull() }
        runCatching { backend.getVipEntitlements() }
            .onSuccess { entitlements = it }
            .onFailure {
                entitlementError = true
                notice = sh("PRO hakları şu anda doğrulanamadı. Satın alımın silinmedi; yeniden deneyebilirsin.", "PRO benefits could not be verified. Your purchase was not removed; you can retry.")
            }
        loading = false
    }

    LaunchedEffect(Unit) { reload() }
    val e = entitlements
    val active = e?.isPro == true || profile?.isVip == true

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            MainScreenHeader(
                title = "KELİME TAHTI PRO",
                subtitle = sh("Oyun keyfine daha fazlasını ekle", "More ways to enjoy your game"),
                onBack = onBack,
            )
        }

        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UProBlue, trackColor = UProBorder) }

        item {
            Surface(shape=RoundedCornerShape(28.dp),color=Color(0xFF173C30),border=BorderStroke(1.dp,Color(0xFFD8BA76))) {
                Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFF173C30),Color(0xFF0C251E))))
                    .padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.WorkspacePremium,null,tint=Color(0xFFE7C984),modifier=Modifier.size(52.dp))
                    Text("KELİME TAHTI",color=Color.White,fontSize=15.sp,letterSpacing=2.sp,fontWeight=FontWeight.Medium)
                    Text("PRO",color=Color(0xFFE7C984),fontSize=36.sp,letterSpacing=5.sp,fontWeight=FontWeight.Bold)
                    HorizontalDivider(Modifier.width(56.dp),color=Color(0xFFE7C984).copy(alpha=.5f))
                    Text(sh("Oyuna odaklan. Tarzını göster.","Focus on play. Show your style."),color=Color.White,fontSize=15.sp,textAlign=TextAlign.Center)
                    Surface(shape=RoundedCornerShape(50),color=Color.White.copy(alpha=.1f)) {
                        Text(if(active) sh("Üyeliğin aktif","Membership active") else sh("Deneyimini yükselt","Upgrade your experience"),
                            Modifier.padding(horizontal=16.dp,vertical=7.dp),color=Color(0xFFE7C984),fontSize=12.sp,fontWeight=FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    ProFeature(Icons.Rounded.Block,sh("Reklamsız","Ad-free"),sh("Menü, profil ve mağazada kesintisiz gezin.","Browse menus, profile and store without ads."),Modifier.weight(1f))
                    ProFeature(Icons.Rounded.WorkspacePremium,sh("Özel profil","Signature profile"),sh("Altın PRO çerçevesi ve üyelik rozeti.","Gold PRO frame and membership badge."),Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    ProFeature(Icons.Rounded.MeetingRoom,sh("Özel odalar","Private rooms"),sh("Davet koduyla kendi masanı kur.","Create your own table with an invite code."),Modifier.weight(1f))
                    ProFeature(Icons.Rounded.Insights,sh("Maç analizi","Match analysis"),sh("Biten karşılaşmalarını ayrıntılı incele.","Review completed matches in detail."),Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    ProFeature(Icons.Rounded.Groups,sh("Arkadaş listesi","Friend list"),sh("Arkadaşlarını gör, çevrimiçi olanı davet et.","See your friends and invite whoever is online."),Modifier.weight(1f))
                    ProFeature(Icons.Rounded.Bolt,sh("Hızlı Düello","Quick Duel"),sh("3, 5 veya 10 dakikalık hızlı maçlar.","Fast matches with 3, 5 or 10 minute turns."),Modifier.weight(1f))
                }
            }
        }

        if (active) {
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = UProSurface, border = BorderStroke(1.dp, UProBorder)) {
                    Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(sh("PRO ARAÇLARI", "PRO TOOLS"), color = UProText, fontWeight = FontWeight.Black)
                        if (e?.seriesGameAccess == true) OutlinedButton(onClick = onQuickDuel, modifier = Modifier.fillMaxWidth()) {
                            Text(sh("HIZLI DÜELLO", "QUICK DUEL"), color = UProGold)
                        }
                        if (e?.postMatchAnalysis == true) {
                            PremiumAnalysisCenterLauncher(Modifier.fillMaxWidth())
                        }
                        if (e?.privateRooms == true) {
                            Button(
                                onClick = onPrivateRoom,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = UProGold, contentColor = if (SonHarfTheme.IsDark) Color(0xFF343D30) else Color.White),
                            ) {
                                Icon(Icons.Rounded.MeetingRoom, null)
                                Spacer(Modifier.width(8.dp))
                                Text(sh("ÖZEL ODA AÇ / KATIL", "CREATE / JOIN PRIVATE ROOM"), fontWeight = FontWeight.Black)
                            }
                        }
                        if (entitlementError || e == null) {
                            OutlinedButton(
                                onClick = { scope.launch { reload() } },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, UProBlue),
                            ) {
                                Icon(Icons.Rounded.Refresh, null, tint = UProBlue)
                                Spacer(Modifier.width(7.dp))
                                Text(sh("PRO HAKLARINI YENİLE", "REFRESH PRO BENEFITS"), color = UProBlue, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }

        item {
            LobbyCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text(sh("Oyun içi konfor","In-game comfort"),color=UProText,fontWeight=FontWeight.Bold)
                    Text(sh("Hamle Önizleme ve Kalan Harfler, Kuşatma tahtasındaki araçlardan açılır. Son Harf kelime geçmişi maç ekranındadır.",
                        "Open Move Preview and Letters Left from the Siege board tools. Last Letter word history is in the match screen."),color=UProMuted,fontSize=13.sp,lineHeight=19.sp)
                }
            }
        }

        item {
            if (!active) {
                Button(
                    onClick = { showPurchase = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = UProBlue, contentColor = Color.White),
                ) { Text(sh("PRO PLANLARINI GÖR", "VIEW PRO PLANS"), fontWeight = FontWeight.Black) }
            }
        }

        notice?.let { message ->
            item {
                Surface(shape = RoundedCornerShape(14.dp), color = UProBlue.copy(alpha = .12f)) {
                    Text(message, Modifier.fillMaxWidth().padding(11.dp), color = UProText, fontSize = 13.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showPurchase) {
        VipPurchaseDialog(
            onVerified = {
                notice = sh("PRO üyeliğin doğrulandı.", "Your PRO membership was verified.")
                scope.launch { reload() }
            },
            onDismiss = { showPurchase = false },
        )
    }
}

@Composable
private fun ProFeature(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, detail: String, modifier: Modifier) {
    // Both cards of a row share the taller one's height, keeping the 2×2 grid symmetric.
    LobbyCard(modifier.fillMaxHeight().heightIn(min=166.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(9.dp)) {
            Icon(icon,null,tint=UProGold,modifier=Modifier.size(28.dp))
            Text(title,color=UProText,fontWeight=FontWeight.Bold,fontSize=15.sp,textAlign=TextAlign.Center)
            Text(detail,color=UProMuted,fontSize=12.sp,lineHeight=17.sp,textAlign=TextAlign.Center)
        }
    }
}
