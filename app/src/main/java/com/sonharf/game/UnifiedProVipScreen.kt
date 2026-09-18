package com.sonharf.game

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.OpenInNew
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

private val UProBg: Color get() = SonHarfTheme.Background
private val UProSurface: Color get() = SonHarfTheme.Surface
private val UProBorder: Color get() = SonHarfTheme.Border
private val UProText: Color get() = SonHarfTheme.TextPrimary
private val UProMuted: Color get() = SonHarfTheme.TextSecondary
private val UProBlue: Color get() = SonHarfTheme.Primary
private val UProGold: Color get() = SonHarfTheme.Purple
private val UProGreen: Color get() = SonHarfTheme.Turquoise

@Composable
internal fun UnifiedProVipScreen(backend: OnlineGameBackend, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    var entitlements by remember { mutableStateOf<VipEntitlementsDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showPurchase by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        loading = true
        val id = backend.currentUserId()
        profile = id?.let { runCatching { backend.getProfile(it) }.getOrNull() }
        entitlements = if (profile?.isVip == true) runCatching { backend.getVipEntitlements() }.getOrNull() else null
        loading = false
    }

    LaunchedEffect(Unit) { reload() }
    val active = profile?.isVip == true
    val e = entitlements

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null, tint = UProText) }
                Column(Modifier.weight(1f)) {
                    Text("SON HARF PRO", color = UProText, fontSize = 23.sp, fontWeight = FontWeight.Black)
                    Text(sh("Premier üyelik ve fair-play ayrıcalıkları", "Premier membership and fair-play benefits"), color = UProBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Rounded.WorkspacePremium, null, tint = UProGold, modifier = Modifier.size(30.dp))
            }
        }

        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UProBlue, trackColor = SonHarfTheme.SurfaceSecondary) }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = UProSurface,
                border = BorderStroke(1.dp, if (active) UProGold.copy(alpha = .55f) else UProBorder),
                shadowElevation = 4.dp,
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(if (active) sh("PRO AKTİF", "PRO ACTIVE") else sh("FREE PLAN", "FREE PLAN"), color = if (active) UProGold else UProMuted, fontWeight = FontWeight.Black)
                    Text(profile?.displayName ?: sh("Oyuncu", "Player"), color = UProText, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (active) sh("Reklamsız deneyim + Style + özel oda + gelişmiş analiz", "Ad-free experience + Style + private rooms + advanced analysis")
                        else sh("PRO ile kozmetik, sosyal ve analiz özelliklerini aç.", "Unlock cosmetic, social, and analysis features with PRO."),
                        color = UProMuted,
                        fontSize = 10.sp,
                    )
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProAccessCard("🚫", sh("REKLAMSIZ", "AD-FREE"), active && e?.rewardedAdBypass == true, UProBlue, Modifier.weight(1f))
                ProAccessCard("📊", sh("ANALİZ", "ANALYSIS"), active && e?.postMatchAnalysis == true, UProGreen, Modifier.weight(1f))
                ProAccessCard("♛", sh("ÖZEL ODA", "PRIVATE ROOM"), active && e?.privateRooms == true, UProGold, Modifier.weight(1f))
            }
        }

        item {
            Surface(shape = RoundedCornerShape(20.dp), color = UProSurface, border = BorderStroke(1.dp, UProBorder), shadowElevation = 3.dp) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProLine("🚫", sh("Reklamsız menü, profil ve mağaza", "Ad-free menus, profile and shop"))
                    ProLine("🎨", sh("PRO Style ve profil ayrıcalıkları", "PRO Style and profile benefits"))
                    ProLine("📊", sh("Gelişmiş istatistik ve maç analizi", "Advanced stats and match analysis"))
                    ProLine("♛", sh("Özel oda ayrıcalıkları", "Private room benefits"))
                    ProLine("👥", sh("Kaydedilmiş arkadaş listesi", "Saved friend list"))
                    Surface(shape = RoundedCornerShape(12.dp), color = UProGreen.copy(alpha = .10f), border = BorderStroke(1.dp, UProGreen.copy(alpha = .35f))) {
                        Text(
                            sh(
                                "ADİL REKABET: PRO, dereceli Premier maçlarda skor, hedef harf, kelime ipucu veya rating avantajı vermez.",
                                "FAIR PLAY: PRO gives no score, target-letter, word-hint, or rating advantage in ranked Premier matches.",
                            ),
                            Modifier.fillMaxWidth().padding(10.dp), color = UProGreen, fontSize = 9.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        item {
            if (active) {
                OutlinedButton(
                    onClick = {
                        val url = "https://play.google.com/store/account/subscriptions?package=${BuildConfig.APPLICATION_ID}"
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, UProBlue),
                ) {
                    Icon(Icons.Rounded.OpenInNew, null, tint = UProBlue)
                    Spacer(Modifier.width(7.dp))
                    Text(sh("GOOGLE PLAY'DE YÖNET", "MANAGE ON GOOGLE PLAY"), color = UProBlue, fontWeight = FontWeight.Black)
                }
            } else {
                Button(
                    onClick = { showPurchase = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = UProGold),
                ) { Text(sh("PRO PLANLARINI GÖR", "VIEW PRO PLANS"), fontWeight = FontWeight.Black) }
            }
        }

        notice?.let { message ->
            item {
                Surface(shape = RoundedCornerShape(14.dp), color = UProBlue.copy(alpha = .10f)) {
                    Text(message, Modifier.fillMaxWidth().padding(11.dp), color = UProText, fontSize = 10.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
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
private fun ProAccessCard(icon: String, label: String, enabled: Boolean, accent: Color, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = UProSurface, border = BorderStroke(1.dp, accent.copy(alpha = .35f)), shadowElevation = 2.dp) {
        Column(Modifier.padding(11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(if (enabled) "✓" else "—", color = if (enabled) UProGreen else UProMuted, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(label, color = UProMuted, fontSize = 7.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

@Composable
private fun ProLine(icon: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 16.sp)
        Spacer(Modifier.width(9.dp))
        Text(text, color = UProText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
