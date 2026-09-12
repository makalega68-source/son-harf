package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.ProfileDto

/**
 * Calm premium dashboard for the flagship game. The hierarchy is deliberately simple:
 * player identity -> one dominant play surface -> four useful shortcuts -> competition.
 */
@Composable
internal fun PremiumHomeCommandDeck(
    profile: ProfileDto?,
    onProfile: () -> Unit,
    onSiege: () -> Unit,
    onTasks: () -> Unit,
    onCompetition: () -> Unit,
    onLeague: () -> Unit,
    onSocial: () -> Unit,
    onShop: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PremiumWelcomeBar(profile = profile, onProfile = onProfile)
        PremiumFlagshipCard(onClick = onSiege)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumHomeShortcut(
                icon = Icons.Rounded.CheckCircle,
                title = sh("GÜNLÜK", "DAILY"),
                subtitle = sh("Görevlerini tamamla", "Complete your missions"),
                accent = SonHarfTheme.Success,
                modifier = Modifier.weight(1f),
                onClick = onTasks,
            )
            PremiumHomeShortcut(
                icon = Icons.Rounded.EmojiEvents,
                title = sh("LİG", "LEAGUE"),
                subtitle = sh("Sıralamada yüksel", "Climb the standings"),
                accent = SonHarfTheme.PremiumGold,
                modifier = Modifier.weight(1f),
                onClick = onLeague,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumHomeShortcut(
                icon = Icons.Rounded.Groups,
                title = sh("SOSYAL", "SOCIAL"),
                subtitle = sh("Arkadaş ve rakipler", "Friends and rivals"),
                accent = SonHarfTheme.Turquoise,
                modifier = Modifier.weight(1f),
                onClick = onSocial,
            )
            PremiumHomeShortcut(
                icon = Icons.Rounded.Storefront,
                title = sh("MAĞAZA", "SHOP"),
                subtitle = sh("Profilini kişiselleştir", "Personalize your profile"),
                accent = SonHarfTheme.Primary,
                modifier = Modifier.weight(1f),
                onClick = onShop,
            )
        }
        PremiumArenaCard(onClick = onCompetition)
    }
}

@Composable
private fun PremiumWelcomeBar(profile: ProfileDto?, onProfile: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, shape)
            .clickable(onClick = onProfile),
        shape = shape,
        color = SonHarfTheme.Surface.copy(alpha = .98f),
        border = BorderStroke(1.dp, SonHarfTheme.Border),
    ) {
        Row(
            Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(SonHarfTheme.Primary, SonHarfTheme.Turquoise)
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    (profile?.displayName ?: "K").take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 19.sp,
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    profile?.displayName ?: sh("OYUNCU", "PLAYER"),
                    color = SonHarfTheme.TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    sh("Tahtın seni bekliyor", "Your throne is waiting"),
                    color = SonHarfTheme.TextSecondary,
                    fontSize = 10.sp,
                )
            }
            Surface(shape = CircleShape, color = SonHarfTheme.Primary.copy(alpha = .10f)) {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = null,
                    tint = SonHarfTheme.Primary,
                    modifier = Modifier.padding(9.dp).size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun PremiumFlagshipCard(onClick: () -> Unit) {
    val shape = RoundedCornerShape(30.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(10.dp, shape)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        SonHarfTheme.Surface,
                        SonHarfTheme.Primary.copy(alpha = .10f),
                        SonHarfTheme.Turquoise.copy(alpha = .09f),
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = SonHarfTheme.Primary.copy(alpha = .11f),
                ) {
                    Text(
                        sh("ANA OYUN", "MAIN GAME"),
                        Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = SonHarfTheme.Primary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    sh("TAKTİK • REKABET", "TACTICS • RIVALRY"),
                    color = SonHarfTheme.TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(4.dp))
            SonHarfOfficialLogo(
                modifier = Modifier
                    .fillMaxWidth(.72f)
                    .height(104.dp)
            )
            Text(
                sh(
                    "Kelimeyi kur, alanı ele geçir, tahtayı koru.",
                    "Build words, capture territory, defend the throne."
                ),
                color = SonHarfTheme.TextPrimary,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(13.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                PremiumFeatureChip(Icons.Rounded.GridView, sh("Alan Savaşı", "Territory"))
                Spacer(Modifier.width(7.dp))
                PremiumFeatureChip(Icons.Rounded.Groups, sh("Gerçek Rakip", "Real Rivals"))
                Spacer(Modifier.width(7.dp))
                PremiumFeatureChip(Icons.Rounded.EmojiEvents, sh("Lig", "League"))
            }
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SonHarfTheme.Primary,
                    contentColor = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(23.dp))
                Spacer(Modifier.width(6.dp))
                Text(sh("OYNA", "PLAY"), fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun PremiumFeatureChip(icon: ImageVector, label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = SonHarfTheme.Surface.copy(alpha = .82f),
        border = BorderStroke(1.dp, SonHarfTheme.Border.copy(alpha = .75f)),
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = SonHarfTheme.Primary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                label,
                color = SonHarfTheme.TextSecondary,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun PremiumHomeShortcut(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(21.dp)
    Surface(
        modifier = modifier
            .height(92.dp)
            .shadow(2.dp, shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = SonHarfTheme.Surface.copy(alpha = .98f),
        border = BorderStroke(1.dp, SonHarfTheme.Border),
    ) {
        Row(
            Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = RoundedCornerShape(15.dp), color = accent.copy(alpha = .12f)) {
                Icon(
                    icon,
                    null,
                    tint = accent,
                    modifier = Modifier.padding(9.dp).size(20.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = SonHarfTheme.TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    subtitle,
                    color = SonHarfTheme.TextSecondary,
                    fontSize = 8.5.sp,
                    lineHeight = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun PremiumArenaCard(onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = SonHarfTheme.Surface.copy(alpha = .98f),
        border = BorderStroke(1.dp, SonHarfTheme.PremiumGold.copy(alpha = .34f)),
    ) {
        Row(
            Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SonHarfTheme.PremiumGold.copy(alpha = .14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Bolt,
                    null,
                    tint = SonHarfTheme.PremiumGold,
                    modifier = Modifier.size(23.dp),
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    sh("REKABET ARENASI", "COMPETITION ARENA"),
                    color = SonHarfTheme.TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    sh("Turnuvalar, rövanşlar ve haftalık hedefler", "Tournaments, rematches and weekly goals"),
                    color = SonHarfTheme.TextSecondary,
                    fontSize = 9.sp,
                )
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.Primary)
        }
    }
}
