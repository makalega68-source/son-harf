package com.sonharf.game

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One page of Obi's welcome: what it says, and how it moves and looks while saying it. */
private class WelcomeStep(
    val tr: String,
    val en: String,
    val action: WordSiegeMascotAction,
    val emotion: WordSiegeMascotEmotion,
)

/** "%s" becomes the player's first name; lines without a name read naturally too. */
private val welcomeSteps = listOf(
    WelcomeStep(
        "Hoş geldin %s! Ben Obi. Seni gördüğüme çok sevindim! 💫",
        "Welcome, %s! I'm Obi. I'm so happy to see you! 💫",
        WordSiegeMascotAction.WAVE, WordSiegeMascotEmotion.HAPPY,
    ),
    WelcomeStep(
        "Burası Kelime Tahtı: kelimelerin gücüyle tahtı fethettiğin yer. Sana kısaca anlatayım.",
        "This is Word Throne: where you conquer the throne with the power of words. Let me show you around.",
        WordSiegeMascotAction.NOD, WordSiegeMascotEmotion.CALM,
    ),
    WelcomeStep(
        "Ana oyunumuz Son Harf: Yeni Oyun'a bas, rakibinin kelimesinin son harfiyle 15 saniyede yeni bir kelime bul. Hızlı ol!",
        "Our main game is Last Letter: tap New Game and find a word from your rival's last letter in 15 seconds. Be quick!",
        WordSiegeMascotAction.HOP, WordSiegeMascotEmotion.EXCITED,
    ),
    WelcomeStep(
        "Diğer Oyunlar'da Kelime Kuşatması var: harflerini tahtaya diz, küpleri ele geçir, rakibini geç!",
        "More Games has Word Siege: place your letters, capture cubes and get ahead of your rival!",
        WordSiegeMascotAction.POINT, WordSiegeMascotEmotion.FOCUS,
    ),
    WelcomeStep(
        "Kelime Atölyesi'nde harflerden kelime kurar, görevleri bitirirsin. Turnuvada kürsüye çıkmak senin elinde!",
        "In the Word Workshop you build words from letters and finish tasks. The tournament podium is yours to take!",
        WordSiegeMascotAction.THINK, WordSiegeMascotEmotion.FOCUS,
    ),
    WelcomeStep(
        "Her hafta en çok puanı toplayan Taht'a oturur ve Altın Kral ona hizmet eder. 👑",
        "Each week the top scorer takes the Throne, and the Golden King serves them. 👑",
        WordSiegeMascotAction.SPARKLE, WordSiegeMascotEmotion.PROUD,
    ),
    WelcomeStep(
        "Hazırsan başlayalım! Bol şans %s, seninle gurur duyacağım. 🍀",
        "Ready? Let's begin! Good luck, %s, I know you'll make me proud. 🍀",
        WordSiegeMascotAction.CHEER, WordSiegeMascotEmotion.HAPPY,
    ),
)

/** An account counts as new for this long after it was created. */
private const val NEW_ACCOUNT_WINDOW_MS = 48L * 60 * 60 * 1000

/** The screenshot harness turns the tour off so its captures and checks see the home screen itself. */
internal object MascotWelcomeTourGate { var enabled = true }

private object MascotWelcomeTourState {
    private const val PREFS = "mascot_welcome_tour"
    private const val KEY_SEEN = "seen_v1"

    fun seen(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_SEEN, false)

    fun markSeen(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_SEEN, true).apply()
    }
}

/**
 * The first time a brand-new player reaches the home screen, Obi welcomes them, explains the game in a few
 * short pages and wishes them luck. Shown once per device; "Atla" ends it at any time.
 */
@Composable
internal fun MascotWelcomeTour(playerName: String?, accountCreatedAt: String?) {
    if (!MascotWelcomeTourGate.enabled) return
    // Only brand-new accounts: a returning player who reinstalls (and so lost the device flag)
    // never sees it again. Unknown creation time means no tour.
    val created = parseServerInstantMs(accountCreatedAt) ?: return
    if (System.currentTimeMillis() - created > NEW_ACCOUNT_WINDOW_MS) return
    val context = LocalContext.current
    var open by remember { mutableStateOf(!MascotWelcomeTourState.seen(context)) }
    if (!open) return
    var step by remember { mutableIntStateOf(0) }
    var actionKey by remember { mutableLongStateOf(0L) }
    val current = welcomeSteps[step]
    val firstName = playerName?.trim()?.split(' ')?.firstOrNull()?.takeIf { it.isNotBlank() && it.length <= 14 }
    val text = sh(current.tr, current.en).replace("%s", firstName.orEmpty()).replace(" !", "!").replace(" ,", ",")

    LaunchedEffect(step) {
        // A beat after the page appears, Obi acts it out.
        kotlinx.coroutines.delay(250L)
        actionKey += 1
        if (step == 0) SonHarfSoundFx.softNotify() else SonHarfSoundFx.tap()
        if (step == welcomeSteps.lastIndex) SonHarfSoundFx.bonus()
    }

    fun close() {
        MascotWelcomeTourState.markSeen(context)
        open = false
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = .62f))
            // The page underneath stays untouched until the tour ends.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(26.dp),
            color = LobbyBrand.Band,
            border = BorderStroke(1.5.dp, LobbyBrand.Gold.copy(alpha = .75f)),
        ) {
            Column(
                Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                WordSiegeMascot(
                    moveId = null, lastMoveMine = false, pendingCells = emptyList(), playerTurn = false,
                    requestedEmotion = current.emotion,
                    modifier = Modifier.size(132.dp),
                    actionKey = actionKey, action = current.action,
                    hat = WordSiegeMascotHat.NONE, skin = WordSiegeMascotSkin.ORB,
                    onTap = { actionKey += 1 },
                )
                // Obi's speech bubble.
                Surface(shape = RoundedCornerShape(18.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                    AnimatedContent(targetState = text, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "welcome-line") { line ->
                        Text(
                            line,
                            Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 16.dp, vertical = 14.dp),
                            color = LobbyBrand.NavBar, fontSize = 16.sp, lineHeight = 22.sp,
                            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                        )
                    }
                }
                // Progress dots.
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    welcomeSteps.indices.forEach { index ->
                        Box(
                            Modifier.size(if (index == step) 10.dp else 7.dp).background(
                                if (index <= step) LobbyBrand.Gold else Color.White.copy(alpha = .3f), CircleShape,
                            ),
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (step < welcomeSteps.lastIndex) {
                        TextButton(onClick = { close() }) {
                            Text(sh("Atla", "Skip"), color = Color.White.copy(alpha = .75f), fontSize = 15.sp)
                        }
                    } else Spacer(Modifier.width(1.dp))
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = { if (step < welcomeSteps.lastIndex) step += 1 else close() },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LobbyBrand.Gold, contentColor = LobbyBrand.NavBar),
                    ) {
                        Text(
                            if (step < welcomeSteps.lastIndex) sh("Devam", "Next") else sh("Başlayalım!", "Let's go!"),
                            fontSize = 16.sp, fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
    }
}
