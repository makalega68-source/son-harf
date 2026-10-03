package com.sonharf.game

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.sonharf.game.data.ShopItemDto
import com.sonharf.game.data.SocialActivityDto

/** Debug-only verification of production entry, navigation, product cards and adaptive icons. */
@Composable
internal fun EntryAndStoreReview(stage: String) {
    val scheme = if (SonHarfTheme.IsDark) darkColorScheme(
        primary = Hf.Green, background = Hf.Ground, surface = Hf.Surface, onSurface = Hf.Text,
        secondaryContainer = SonHarfTheme.PrimarySoft, onSecondaryContainer = Hf.Text,
    ) else lightColorScheme(primary = Hf.Green, background = Hf.Ground, surface = Hf.Surface, onSurface = Hf.Text)
    MaterialTheme(colorScheme = scheme) {
        when {
            stage == "mascot-drag" -> Box(Modifier.fillMaxSize().background(Hf.Ground).statusBarsPadding().navigationBarsPadding()) {
                Text("MASKOT · SÜRÜKLE", Modifier.align(Alignment.TopCenter).padding(16.dp), color = Hf.Text)
                WordSiegeMascotCompanion(anchors = listOf(androidx.compose.ui.geometry.Offset(.5f,.75f)),
                    mascotSize = 100.dp, moveId = null, lastMoveMine = false, playerTurn = false,
                    requireOwnership = false, greet = false, positionKey = "qa-drag", modifier = Modifier.matchParentSize())
            }
            stage == "mascot-face" -> Column(Modifier.fillMaxSize().background(Hf.Ground).statusBarsPadding().navigationBarsPadding().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(WordSiegeMascotEmotion.HAPPY, WordSiegeMascotEmotion.SURPRISED, WordSiegeMascotEmotion.TEARY,
                    WordSiegeMascotEmotion.LAUGH, WordSiegeMascotEmotion.STRESSED).forEach { mood ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf(-1f,0f,1f).forEach { gaze ->
                            WordSiegeMascot(moveId=null,lastMoveMine=false,pendingCells=emptyList(),playerTurn=false,
                                requestedEmotion=mood,idleGazeX=gaze, watching=true, modifier=Modifier.size(104.dp))
                        }
                    }
                    Text(mood.name,color=Hf.Text,fontSize=10.sp)
                }
            }
            stage.startsWith("welcome") -> IntroWelcomeScreen {}
            stage.startsWith("store") -> EconomyShopScreen(onCollection = {}, onPro = {})
            stage == "inbox-dark" -> Column(Modifier.fillMaxSize().background(Hf.Ground).statusBarsPadding().padding(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("AKTİVİTE",color=Hf.Text,fontSize=24.sp,fontWeight=FontWeight.Black)
                listOf("your_turn","rematch","friend_accepted").forEachIndexed { i,kind ->
                    SocialInboxRow(SocialActivityDto("preview-$i","preview-me",kind=kind,targetKind="activity",createdAt="2026-10-02T10:00:00Z",readAt=if(i==2) "2026-10-02T10:01:00Z" else null),"Selin") {}
                }
            }
            stage.startsWith("home-") -> PremiumUnifiedProApp(onSignedOut = {})
            else -> Column(Modifier.fillMaxSize().background(Hf.Ground).statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("KELİME TAHTI", color = Hf.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeSessionFilter("Tümü 3", false) {}
                    HomeSessionFilter("Sıra sende 2", true) {}
                    HomeSessionFilter("Rakip 1", false) {}
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VerifiedStoreProductCard(ShopItemDto("theme_black", "game_theme", "Kara Taht", "Black Throne", diamondPrice = 300),
                        false, false, false, true, 200, 20, modifier = Modifier.weight(1f).height(280.dp)) {}
                    VerifiedStoreProductCard(ShopItemDto("keyboard_premium_white", "keyboard_theme", "Beyaz Klavye", "White Keyboard", diamondPrice = 250),
                        true, true, false, true, modifier = Modifier.weight(1f).height(280.dp)) {}
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf(R.mipmap.ic_kelime_tahti, R.mipmap.ic_kelime_tahti_mascot_happy).forEach { res ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AndroidView(factory = { context -> ImageView(context).apply {
                                setImageDrawable(context.getDrawable(res)); scaleType = ImageView.ScaleType.FIT_CENTER
                            } }, modifier = Modifier.size(126.dp))
                            Text(if (res == R.mipmap.ic_kelime_tahti) "APK" else "ANA EKRAN", color = Hf.Text, fontSize = 12.sp)
                        }
                    }
                }
                // Original rig and artwork, never the reverted green redesign.
                WordSiegeMascot(moveId = null, lastMoveMine = false, pendingCells = emptyList(), playerTurn = false, requestedEmotion = WordSiegeMascotEmotion.HAPPY, modifier = Modifier.size(160.dp))
            }
        }
    }
}
