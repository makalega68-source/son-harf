package com.sonharf.game.ui.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.FramedProfilePhotoAvatar
import com.sonharf.game.SonHarfTheme

/**
 * G5.5 — Her oyunun premium giriş ekranı için ortak header.
 *
 * Şu bölümleri sabitler:
 *   1) Üstte oyunun rengiyle boyalı AnimatedBackground.
 *   2) Ortada büyük başlık (title) + isteğe bağlı alt-başlık.
 *   3) Oyuncu şeridi: çerçeveli avatar, oyuncu adı (Pro rozetli),
 *      seviye + lig chip.
 *
 * Her oyun ekranı sadece [accent] rengini + logo başlığını değiştirerek
 * bunu paylaşabilir.
 */
@Composable
fun PremiumLobbyHeader(
    title: String,
    subtitle: String,
    accent: Color,
    avatarPath: String?,
    avatarGender: String?,
    playerName: String,
    isPro: Boolean,
    level: Int,
    leagueLabel: String,
    modifier: Modifier = Modifier,
    heightDp: Dp = 220.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp)
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        SonHarfTheme.PremiumBgStart,
                        accent.copy(alpha = 0.35f).compositeOver(SonHarfTheme.PremiumBgEnd),
                    ),
                ),
            ),
    ) {
        AnimatedBackground(
            accent = accent,
            modifier = Modifier.fillMaxWidth().height(heightDp),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    title,
                    style = TextStyle(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                    ),
                    color = SonHarfTheme.PremiumTextPrimary,
                )
                Text(
                    subtitle,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    color = SonHarfTheme.PremiumTextSecondary,
                )
            }
            Spacer(Modifier.height(12.dp))
            // Player strip.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                FramedProfilePhotoAvatar(
                    avatarPath = avatarPath,
                    gender = avatarGender,
                    name = playerName,
                    size = 56.dp,
                    frameId = null,
                    accent = accent,
                    visible = true,
                    showGenderBadge = false,
                    isPro = isPro,
                )
                Column(Modifier.weight(1f)) {
                    ProNameLabel(
                        name = playerName,
                        isPro = isPro,
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold),
                        color = SonHarfTheme.PremiumTextPrimary,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LevelChip(level = level, accent = accent)
                        LeagueChip(label = leagueLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelChip(level: Int, accent: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(accent.copy(alpha = 0.25f))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(
            text = "LVL $level",
            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.4.sp),
            color = SonHarfTheme.PremiumTextPrimary,
        )
    }
}

@Composable
private fun LeagueChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(SonHarfTheme.GoldBright.copy(alpha = 0.2f))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.4.sp),
            color = SonHarfTheme.GoldBright,
        )
    }
}

/** Compose analog to `Color#compositeOver` for shorthand use above. */
private fun Color.compositeOver(background: Color): Color {
    val a = this.alpha + background.alpha * (1 - this.alpha)
    if (a == 0f) return Color.Transparent
    val r = (this.red * this.alpha + background.red * background.alpha * (1 - this.alpha)) / a
    val g = (this.green * this.alpha + background.green * background.alpha * (1 - this.alpha)) / a
    val b = (this.blue * this.alpha + background.blue * background.alpha * (1 - this.alpha)) / a
    return Color(r, g, b, a)
}
