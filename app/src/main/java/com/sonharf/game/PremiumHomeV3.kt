package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.ProfileDto

private val PremiumHeroShape = RoundedCornerShape(24.dp)
private val PremiumCardShape = RoundedCornerShape(18.dp)

/**
 * Code-only home presentation for the simplified test release.
 *
 * There are deliberately no raster logos with baked text in this file. Every title, subtitle,
 * score and CTA is real Compose text, so duplicate/overlapping text cannot be introduced by an
 * imported visual asset.
 */
@Composable
internal fun PremiumHomeCommandDeck(
    profile: ProfileDto?,
    onProfile: () -> Unit,
    onSiege: () -> Unit,
    onPremium: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            onClick = onProfile,
            shape = PremiumCardShape,
            color = SonHarfTheme.Surface,
            border = BorderStroke(1.dp, SonHarfTheme.Border),
            shadowElevation = 0.dp,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FramedProfilePhotoAvatar(
                    avatarPath = profile?.avatarPath,
                    gender = profile?.gender,
                    name = profile?.displayName ?: sh("Oyuncu", "Player"),
                    size = 52.dp,
                    frameId = SonHarfCosmetics.profileFrameId,
                    accent = if (profile?.isVip == true) SonHarfTheme.PremiumGold else SonHarfTheme.Primary,
                    visible = profile?.avatarVisibility != "hidden",
                    isPro = profile?.isVip == true,
                )
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        sh("Merhaba", "Hello"),
                        color = SonHarfTheme.TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        profile?.displayName ?: sh("Oyuncu", "Player"),
                        color = SonHarfTheme.TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = sh("Profili aç", "Open profile"),
                    tint = SonHarfTheme.TextSecondary,
                )
            }
        }

        Surface(
            shape = PremiumHeroShape,
            color = Color.Transparent,
            border = BorderStroke(1.dp, SonHarfTheme.Primary.copy(alpha = .28f)),
            shadowElevation = 0.dp,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(SonHarfTheme.HeroStart, SonHarfTheme.HeroMiddle, SonHarfTheme.HeroEnd),
                        ),
                        PremiumHeroShape,
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = SonHarfTheme.PrimarySoft) {
                        Icon(
                            Icons.Rounded.GridView,
                            contentDescription = null,
                            tint = SonHarfTheme.Primary,
                            modifier = Modifier.padding(10.dp).size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            sh("ANA OYUN", "MAIN GAME"),
                            color = SonHarfTheme.Primary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = .8.sp,
                        )
                        Text(
                            "KELİME KUŞATMASI",
                            color = SonHarfTheme.TextPrimary,
                            fontSize = 24.sp,
                            lineHeight = 27.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }

                Text(
                    sh(
                        "Kelimeyi kur, alanını genişlet ve rakibinin bölgesini kuşat.",
                        "Build words, expand your territory and surround your rival.",
                    ),
                    color = SonHarfTheme.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )

                Button(
                    onClick = onSiege,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SonHarfTheme.Primary,
                        contentColor = SonHarfTheme.OnPrimary,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(23.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(sh("OYNA", "PLAY"), fontSize = 15.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        Surface(
            onClick = onPremium,
            shape = PremiumCardShape,
            color = SonHarfTheme.Surface,
            border = BorderStroke(1.dp, SonHarfTheme.PremiumGold.copy(alpha = .55f)),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(shape = CircleShape, color = SonHarfTheme.PremiumGold.copy(alpha = .13f)) {
                    Icon(
                        Icons.Rounded.WorkspacePremium,
                        contentDescription = null,
                        tint = SonHarfTheme.PremiumGoldLight,
                        modifier = Modifier.padding(9.dp).size(22.dp),
                    )
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "PREMIUM",
                        color = SonHarfTheme.PremiumGoldLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        sh("Reklamsız ve daha sade deneyim", "Ad-free and more focused experience"),
                        color = SonHarfTheme.TextSecondary,
                        fontSize = 10.sp,
                    )
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.PremiumGold)
            }
        }
    }
}

@Composable
internal fun PremiumOtherGames(onLastLetter: () -> Unit, onLetterPath: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(
            sh("DİĞER OYUN MODLARI", "OTHER GAME MODES"),
            color = SonHarfTheme.TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = .7.sp,
        )

        PremiumModeRow(
            icon = Icons.Rounded.Link,
            title = sh("Son Harf", "Last Letter"),
            subtitle = sh("Son harfle devam et, kelime zincirini sürdür.", "Continue with the last letter and keep the chain alive."),
            accent = SonHarfTheme.Primary,
            onClick = onLastLetter,
        )
        PremiumModeRow(
            icon = Icons.Rounded.Route,
            title = sh("Harf Yolu", "Letter Path"),
            subtitle = sh("Her hamlede tek harf değiştirerek hedef kelimeye ulaş.", "Change one letter per move to reach the target word."),
            accent = SonHarfTheme.SoftBlue,
            onClick = onLetterPath,
        )
    }
}

@Composable
private fun PremiumModeRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = PremiumCardShape,
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, SonHarfTheme.Border),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = RoundedCornerShape(13.dp), color = accent.copy(alpha = .13f)) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.padding(10.dp).size(23.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = SonHarfTheme.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    color = SonHarfTheme.TextSecondary,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.ChevronRight, null, tint = accent)
        }
    }
}

/**
 * Compact daily-return cue for the test build. It is informational only; league/ranking UI is
 * deliberately not exposed while the reduced product shell is being validated.
 */
@Composable
internal fun PremiumDailyObjective() {
    Surface(
        shape = PremiumCardShape,
        color = SonHarfTheme.SurfaceSecondary,
        border = BorderStroke(1.dp, SonHarfTheme.Border),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = SonHarfTheme.PrimarySoft) {
                Icon(
                    Icons.Rounded.Today,
                    contentDescription = null,
                    tint = SonHarfTheme.Primary,
                    modifier = Modifier.padding(9.dp).size(20.dp),
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    sh("BUGÜNÜN HEDEFİ", "TODAY'S GOAL"),
                    color = SonHarfTheme.TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    sh("Bir Kelime Kuşatması maçı tamamla.", "Complete one Kelime Kuşatması match."),
                    color = SonHarfTheme.TextSecondary,
                    fontSize = 10.sp,
                )
            }
        }
    }
}
