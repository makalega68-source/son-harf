package com.sonharf.game.ui.premium

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.FramedProfilePhotoAvatar
import com.sonharf.game.SonHarfTheme
import com.sonharf.game.ui.vfx.LocalVfx
import com.sonharf.game.ui.vfx.VfxEvent
import kotlinx.coroutines.delay

/**
 * G5.5 — Rakip aranırken gösterilen VS ekranı.
 *
 * Sol: benim avatar, sağ: rakip yeri (henüz null olabilir),
 * ortada büyük "VS" + dönen harfler bandı. İptal butonu her
 * zaman görünür. [opponent] null iken "aranıyor…" göstergesi
 * dönüyor; opponent değeri geldiğinde 800ms çarpışma efekti
 * çalıştırılır (LocalVfx.play(VfxEvent.WordAccepted)) ve
 * [onMatched] tetiklenir.
 *
 * Reduced-motion açıksa dönen harf ve VS parlaması durur; sadece
 * statik VS metni + iptal görünür.
 */
@Composable
fun VsSearchingOverlay(
    myName: String,
    myAvatarPath: String?,
    myGender: String?,
    myIsPro: Boolean,
    opponentName: String?,
    opponentAvatarPath: String?,
    opponentGender: String?,
    opponentIsPro: Boolean,
    accent: Color,
    onCancel: () -> Unit,
    onMatched: () -> Unit,
    modifier: Modifier = Modifier,
    language: String = "tr",
) {
    val reducedMotion = rememberReducedMotion()
    val vfx = LocalVfx.current
    val transition = rememberInfiniteTransition(label = "vs-spin")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reducedMotion) 0f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "vs-t",
    )

    androidx.compose.runtime.LaunchedEffect(opponentName) {
        if (opponentName != null) {
            vfx.play(VfxEvent.WordAccepted(
                score = 0,
                anchor = androidx.compose.ui.geometry.Offset.Zero,
                tint = accent,
            ))
            delay(800)
            onMatched()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SonHarfTheme.PremiumBgStart.copy(alpha = 0.95f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (language == "en") "FINDING OPPONENT" else "RAKİP ARANIYOR",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp,
                ),
                color = SonHarfTheme.PremiumTextSecondary,
            )
            Spacer(Modifier.size(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VsFace(
                    name = myName,
                    avatarPath = myAvatarPath,
                    gender = myGender,
                    isPro = myIsPro,
                    accent = accent,
                )
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(88.dp)) {
                    Text(
                        "VS",
                        style = TextStyle(
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                        ),
                        color = accent,
                        modifier = Modifier.graphicsLayer(
                            scaleX = 1f + (if (reducedMotion) 0f else 0.08f * kotlin.math.sin(spin * 6.28f)),
                            scaleY = 1f + (if (reducedMotion) 0f else 0.08f * kotlin.math.sin(spin * 6.28f)),
                        ),
                    )
                }
                VsFace(
                    name = opponentName ?: "?",
                    avatarPath = opponentAvatarPath,
                    gender = opponentGender,
                    isPro = opponentIsPro,
                    accent = accent,
                    unknown = opponentName == null,
                )
            }
            Spacer(Modifier.size(20.dp))
            if (opponentName == null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val letters = "AEIKLMNORST"
                    val visible = ((spin * letters.length).toInt() % letters.length)
                    repeat(5) { i ->
                        Text(
                            text = letters[(visible + i) % letters.length].toString(),
                            style = TextStyle(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                            ),
                            color = SonHarfTheme.PremiumTextPrimary.copy(
                                alpha = 0.3f + 0.15f * (i.toFloat()),
                            ),
                        )
                    }
                }
                Spacer(Modifier.size(32.dp))
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .border(1.dp, SonHarfTheme.PremiumPanelBorder, RoundedCornerShape(99.dp))
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 22.dp, vertical = 10.dp),
            ) {
                Text(
                    if (language == "en") "CANCEL" else "İPTAL",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                    ),
                    color = SonHarfTheme.PremiumTextPrimary,
                )
            }
        }
    }
}

@Composable
private fun VsFace(
    name: String,
    avatarPath: String?,
    gender: String?,
    isPro: Boolean,
    accent: Color,
    unknown: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (unknown) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(SonHarfTheme.PremiumPanel),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "?",
                    style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Black),
                    color = SonHarfTheme.PremiumTextSecondary,
                )
            }
        } else {
            FramedProfilePhotoAvatar(
                avatarPath = avatarPath,
                gender = gender,
                name = name,
                size = 72.dp,
                frameId = null,
                accent = accent,
                visible = true,
                showGenderBadge = false,
                isPro = isPro,
            )
        }
        Text(
            name,
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.ExtraBold),
            color = SonHarfTheme.PremiumTextPrimary,
        )
    }
}
