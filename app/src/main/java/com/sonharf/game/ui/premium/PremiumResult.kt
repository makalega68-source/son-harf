package com.sonharf.game.ui.premium

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.FramedProfilePhotoAvatar
import com.sonharf.game.SonHarfTheme
import com.sonharf.game.ui.vfx.LocalVfx
import com.sonharf.game.ui.vfx.VfxEvent
import kotlinx.coroutines.delay

/**
 * G5.6 — Premium sonuç ekranı için ortak yapı taşları.
 *
 * Kullanım (3 oyunda da aynı):
 *   PremiumResultLayout(state = MyResultState(...)) {
 *       ResultHero(state, ...)
 *       ResultMetrics(state, ...)
 *       ResultDetails(state, ...)          // opsiyonel
 *       ResultRewards(state, ...)          // opsiyonel
 *       ResultActions(onRematch, onHome, onShare)
 *   }
 *
 * Animasyon: giriş toplam ~2.5sn. Ekrana dokununca [skipped] true
 * yapılır ve içerik doğrudan görünür (spec: "ekrana dokununca
 * animasyon atlanır, sonuç hemen görünür").
 */

enum class ResultOutcome { WIN, LOSE, DRAW }

/**
 * Ekrana dokununca içeriğin hemen açılmasını yöneten kapsayıcı.
 * Alt bileşenler [PremiumResultScope] alarak `skipped` bayrağına
 * bakabilir ve gerekiyorsa animasyonlarını atlayabilir.
 */
@Composable
fun PremiumResultLayout(
    modifier: Modifier = Modifier,
    content: @Composable PremiumResultScope.() -> Unit,
) {
    var skipped by remember { mutableStateOf(false) }
    val scope = remember { object : PremiumResultScope {
        override val skipped: Boolean get() = skipped
    } }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(SonHarfTheme.PremiumBgStart, SonHarfTheme.PremiumBgEnd),
                ),
            )
            .clickable(
                interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
                indication = null,
                onClick = { skipped = true },
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            scope.content()
        }
    }
}

interface PremiumResultScope {
    val skipped: Boolean
}

/**
 * Başlık girişi (üstten düşen) + kazananın avatarı büyür, tacın altına
 * konur, RewardBurst (VfxEvent.Victory) tetiklenir.
 */
@Composable
fun PremiumResultScope.ResultHero(
    outcome: ResultOutcome,
    winnerName: String,
    winnerAvatarPath: String?,
    winnerGender: String?,
    winnerIsPro: Boolean,
    accent: Color,
    language: String = "tr",
) {
    val vfx = LocalVfx.current
    val avatarScale = remember { Animatable(0.6f) }
    val vfxKey = "hero:${outcome.name}:$winnerName"

    LaunchedEffect(vfxKey, skipped) {
        if (skipped) {
            avatarScale.snapTo(1f)
            return@LaunchedEffect
        }
        avatarScale.snapTo(0.6f)
        delay(120)
        avatarScale.animateTo(1.12f, tween(280))
        avatarScale.animateTo(1f, tween(180))
        if (outcome == ResultOutcome.WIN) {
            vfx.play(VfxEvent.Victory(Offset.Zero))
        }
    }

    val title = when (outcome) {
        ResultOutcome.WIN -> if (language == "en") "VICTORY!" else "ZAFER!"
        ResultOutcome.LOSE -> if (language == "en") "DEFEAT" else "YENİLGİ"
        ResultOutcome.DRAW -> if (language == "en") "DRAW" else "BERABERE"
    }
    val titleColor = when (outcome) {
        ResultOutcome.WIN -> SonHarfTheme.GoldBright
        ResultOutcome.LOSE -> SonHarfTheme.Lavender
        ResultOutcome.DRAW -> SonHarfTheme.PremiumTextPrimary
    }

    // Slide title in from above. AnimatedVisibility can't easily flip
    // to "already visible" at composition, so we key it off skipped.
    val titleVisible = skipped || true
    AnimatedVisibility(
        visible = titleVisible,
        enter = fadeIn(tween(320)) + slideInVertically(tween(320)) { -it },
        exit = fadeOut(),
    ) {
        Text(
            title,
            style = TextStyle(
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
            ),
            color = titleColor,
            textAlign = TextAlign.Center,
        )
    }
    Spacer(Modifier.size(18.dp))
    Box(
        modifier = Modifier.graphicsLayer(
            scaleX = avatarScale.value,
            scaleY = avatarScale.value,
        ),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (outcome == ResultOutcome.WIN) {
            Text(
                "👑",  // crown
                style = TextStyle(fontSize = 28.sp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 0.dp),
            )
        }
        Box(modifier = Modifier.padding(top = 22.dp)) {
            FramedProfilePhotoAvatar(
                avatarPath = winnerAvatarPath,
                gender = winnerGender,
                name = winnerName,
                size = 92.dp,
                frameId = null,
                accent = accent,
                visible = true,
                showGenderBadge = false,
                isPro = winnerIsPro,
            )
        }
    }
    Spacer(Modifier.size(6.dp))
    ProNameLabel(
        name = winnerName,
        isPro = winnerIsPro,
        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold),
        color = SonHarfTheme.PremiumTextPrimary,
    )
}

/** Two-column count-up scoreboard. */
@Composable
fun PremiumResultScope.ResultMetrics(
    myScore: Int,
    rivalScore: Int,
    myLabel: String = "SEN",
    rivalLabel: String = "RAKİP",
) {
    Spacer(Modifier.size(22.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SonHarfTheme.PremiumPanel.copy(alpha = 0.85f))
            .border(1.dp, SonHarfTheme.PremiumPanelBorder, RoundedCornerShape(20.dp))
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetricColumn(label = myLabel, value = myScore)
        Box(Modifier.width(1.dp).size(width = 1.dp, height = 44.dp).background(SonHarfTheme.PremiumPanelBorder))
        MetricColumn(label = rivalLabel, value = rivalScore)
    }
}

@Composable
private fun MetricColumn(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
            ),
            color = SonHarfTheme.PremiumTextSecondary,
        )
        CountUpText(
            value = value,
            style = TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
            ),
            color = SonHarfTheme.PremiumTextPrimary,
        )
    }
}

/** Detail rows (en iyi kelime, en uzun kelime, seri, harita %). */
@Composable
fun PremiumResultScope.ResultDetails(
    entries: List<Pair<String, String>>,
) {
    if (entries.isEmpty()) return
    Spacer(Modifier.size(12.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SonHarfTheme.PremiumPanel.copy(alpha = 0.7f))
            .border(1.dp, SonHarfTheme.PremiumPanelBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        entries.forEach { (k, v) ->
            Row(Modifier.fillMaxWidth()) {
                Text(
                    k,
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                    color = SonHarfTheme.PremiumTextSecondary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    v,
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Black),
                    color = SonHarfTheme.PremiumTextPrimary,
                )
            }
        }
    }
}

/** Reward count-ups (elmas +18 gibi). Each entry: (label, amount). */
@Composable
fun PremiumResultScope.ResultRewards(
    entries: List<Pair<String, Int>>,
) {
    if (entries.isEmpty()) return
    Spacer(Modifier.size(10.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        entries.forEach { (label, amount) ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(SonHarfTheme.GoldBright.copy(alpha = 0.14f))
                    .border(1.dp, SonHarfTheme.GoldBright.copy(alpha = 0.4f), RoundedCornerShape(99.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    label,
                    style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = SonHarfTheme.PremiumTextSecondary,
                )
                Spacer(Modifier.width(4.dp))
                CountUpText(
                    value = amount,
                    prefix = "+",
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Black),
                    color = SonHarfTheme.GoldBright,
                )
            }
        }
    }
}

/**
 * Butonlar: REVANŞ (büyük GameButton), ANA SAYFA, PAYLAŞ.
 * Revanş kilidini (G4.2 10sn timeout) caller yönetir.
 */
@Composable
fun PremiumResultScope.ResultActions(
    rematchEnabled: Boolean,
    rematchLabel: String,
    onRematch: () -> Unit,
    onHome: () -> Unit,
    onShare: (() -> Unit)? = null,
    accent: Color = SonHarfTheme.SonHarfOrange,
    language: String = "tr",
) {
    Spacer(Modifier.size(18.dp))
    GameButton(
        text = rematchLabel,
        onClick = onRematch,
        primary = true,
        enabled = rematchEnabled,
        accent = accent,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.size(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GameButton(
            text = if (language == "en") "HOME" else "ANA SAYFA",
            onClick = onHome,
            modifier = Modifier.weight(1f),
            accent = SonHarfTheme.Lavender,
        )
        if (onShare != null) {
            GameButton(
                text = if (language == "en") "SHARE" else "PAYLAŞ",
                onClick = onShare,
                modifier = Modifier.weight(1f),
                accent = SonHarfTheme.DiamondBlue,
            )
        }
    }
}

/** Loss-side moral text (spec: "Az kaldı! Tekrar dene."). */
@Composable
fun PremiumResultScope.LossMoraleText(language: String = "tr") {
    Spacer(Modifier.size(8.dp))
    Text(
        text = if (language == "en") "So close! Try again." else "Az kaldı! Tekrar dene.",
        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
        color = SonHarfTheme.PremiumTextSecondary,
        textAlign = TextAlign.Center,
    )
}
