package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.ProfileDto

/** Premium home with one flagship action and two secondary destinations. */
@Composable
internal fun PremiumHomeCommandDeck(
    profile: ProfileDto?,
    onProfile: () -> Unit,
    onSiege: () -> Unit,
    onGames: () -> Unit,
    onCompete: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
        PremiumIdentityCard(profile = profile, onProfile = onProfile)
        PremiumFlagshipHero(onClick = onSiege)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PremiumSimpleDestination(
                icon = Icons.Rounded.SportsEsports,
                title = sh("OYUNLAR", "GAMES"),
                subtitle = sh("Tüm oyunlar tek yerde", "All games in one place"),
                modifier = Modifier.weight(1f),
                onClick = onGames,
            )
            PremiumSimpleDestination(
                icon = Icons.Rounded.EmojiEvents,
                title = sh("REKABET", "COMPETE"),
                subtitle = sh("Lig ve sıralama tek yerde", "League and ranking together"),
                modifier = Modifier.weight(1f),
                onClick = onCompete,
            )
        }
    }
}

@Composable
private fun PremiumIdentityCard(profile: ProfileDto?, onProfile: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, shape)
            .clickable(onClick = onProfile),
        shape = shape,
        color = SonHarfTheme.Surface.copy(alpha = .98f),
        border = BorderStroke(1.dp, SonHarfTheme.Border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
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
                    fontSize = 20.sp,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    profile?.displayName ?: sh("OYUNCU", "PLAYER"),
                    color = SonHarfTheme.TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    sh("Puan ${profile?.rating ?: 1000}", "Rating ${profile?.rating ?: 1000}"),
                    color = SonHarfTheme.TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Surface(
                shape = CircleShape,
                color = SonHarfTheme.Primary.copy(alpha = .10f),
            ) {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = null,
                    tint = SonHarfTheme.Primary,
                    modifier = Modifier.padding(10.dp).size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun PremiumFlagshipHero(onClick: () -> Unit) {
    val shape = RoundedCornerShape(32.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = SonHarfTheme.Surface,
        border = BorderStroke(1.dp, SonHarfTheme.Primary.copy(alpha = .24f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            SonHarfTheme.Surface,
                            SonHarfTheme.Primary.copy(alpha = .105f),
                            SonHarfTheme.Turquoise.copy(alpha = .075f),
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = SonHarfTheme.PremiumGold.copy(alpha = .14f),
                    ) {
                        Text(
                            sh("ANA OYUN", "MAIN GAME"),
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            color = SonHarfTheme.PremiumGold,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(
                        Icons.Rounded.GridView,
                        contentDescription = null,
                        tint = SonHarfTheme.Primary,
                        modifier = Modifier.size(25.dp),
                    )
                }

                SonHarfOfficialLogo(
                    modifier = Modifier
                        .fillMaxWidth(.78f)
                        .height(112.dp)
                )
                Text(
                    sh(
                        "Kelime kur. Alan kazan. Tahtayı ele geçir.",
                        "Build words. Gain territory. Take the throne."
                    ),
                    color = SonHarfTheme.TextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(17.dp))
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SonHarfTheme.Primary,
                        contentColor = Color.White,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(sh("OYNA", "PLAY"), fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun PremiumSimpleDestination(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(23.dp)
    Surface(
        modifier = modifier
            .height(112.dp)
            .shadow(2.dp, shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = SonHarfTheme.Surface.copy(alpha = .98f),
        border = BorderStroke(1.dp, SonHarfTheme.Border),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SonHarfTheme.Primary.copy(alpha = .11f),
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = SonHarfTheme.Primary,
                        modifier = Modifier.padding(8.dp).size(20.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.TextSecondary)
            }
            Text(
                title,
                color = SonHarfTheme.TextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
            )
            Text(
                subtitle,
                color = SonHarfTheme.TextSecondary,
                fontSize = 9.sp,
                lineHeight = 11.sp,
            )
        }
    }
}
