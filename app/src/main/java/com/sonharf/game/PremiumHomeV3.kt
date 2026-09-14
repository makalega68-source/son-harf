package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.LeaderboardV2Row
import com.sonharf.game.data.ProfileDto

internal data class HomePodiumEntry(val row: LeaderboardV2Row, val profile: ProfileDto?)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PremiumHomeCommandDeck(
    profile: ProfileDto?,
    onProfile: () -> Unit,
    onSiege: () -> Unit,
    onShop: () -> Unit,
    onSocial: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onSocial, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Groups, sh("Sosyal merkez", "Social hub"), tint = SonHarfTheme.Primary)
                }
            }
            Surface(onClick = onProfile, color = Color.Transparent, shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FramedProfilePhotoAvatar(
                        avatarPath = profile?.avatarPath, gender = profile?.gender,
                        name = profile?.displayName ?: sh("Oyuncu", "Player"),
                        size = 44.dp, frameId = SonHarfCosmetics.profileFrameId,
                        accent = SonHarfTheme.Primary, visible = profile?.avatarVisibility != "hidden",
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(profile?.displayName ?: sh("Profilin", "Your profile"),
                            color = SonHarfCosmetics.playerNameColor, fontWeight = FontWeight.Bold,
                            fontSize = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(profile?.let { "${homeLeagueName(ratingLeagueProgress(it.rating).leagueName)} · ${it.rating} RP" } ?: "— RP",
                            color = SonHarfTheme.TextSecondary, fontSize = 13.sp)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.TextSecondary)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onShop, modifier = Modifier.heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Icon(Icons.Rounded.Toll, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${profile?.diamonds?.toString() ?: "—"} Son Coin", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onShop, modifier = Modifier.heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Icon(Icons.Rounded.WorkspacePremium, null, modifier = Modifier.size(18.dp),
                        tint = if (profile?.isVip == true) SonHarfTheme.Warning else SonHarfTheme.TextSecondary)
                    Spacer(Modifier.width(6.dp))
                    Text(when (profile?.isVip) {
                        true -> sh("PRO ÜYE", "PRO MEMBER")
                        false -> sh("Standart üyelik", "Standard plan")
                        null -> "—"
                    }, fontSize = 13.sp, color = SonHarfTheme.TextSecondary)
                }
            }
        }
        Surface(shape = RoundedCornerShape(28.dp), color = SonHarfTheme.ForestDeep,
            border = BorderStroke(1.dp, Color.White.copy(alpha = .08f)), shadowElevation = 3.dp) {
            Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Shield, null, tint = SonHarfTheme.PremiumGoldLight, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(sh("KELİMELERLE ALAN SAVAŞI", "A BATTLE OF WORDS & TERRITORY"),
                                color = Color.White.copy(alpha = .8f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text("KELİME TAHTI", color = Color.White, fontSize = 27.sp,
                            lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.5).sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Image(
                        painter = painterResource(R.drawable.kelime_tahti_logo),
                        contentDescription = null,
                        modifier = Modifier.size(width = 132.dp, height = 88.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                Text(sh("Kelimeyi kur. Alanı ele geçir. Rakibini geç.",
                    "Build your word. Claim territory. Outplay your rival."),
                    color = Color.White.copy(alpha = .85f), fontSize = 14.sp, lineHeight = 20.sp)
                Button(onClick = onSiege, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SonHarfTheme.PremiumGoldLight, contentColor = SonHarfTheme.ForestDeep),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(27.dp))
                        Spacer(Modifier.width(9.dp))
                        Text(sh("OYNA", "PLAY"), fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun PremiumWeeklyPodium(
    players: List<HomePodiumEntry>, loading: Boolean, failed: Boolean,
    onOpenLeague: () -> Unit, onRetry: () -> Unit,
) {
    Surface(onClick = onOpenLeague, shape = RoundedCornerShape(24.dp), color = Color.Transparent, shadowElevation = 3.dp) {
        Box(Modifier.fillMaxWidth().height(164.dp)) {
            Image(
                painter = painterResource(R.drawable.weekly_elite_gold_panel),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.FillBounds,
            )
            Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(42.dp))
                Text(sh("HAFTANIN ZİRVESİ", "WEEKLY ELITE"), Modifier.weight(1f), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp, textAlign = TextAlign.Center)
                Row(Modifier.width(42.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(sh("LİG", "LEAGUE"), color = SonHarfTheme.PremiumGoldLight, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    Icon(Icons.Rounded.ChevronRight, null, Modifier.size(14.dp), tint = SonHarfTheme.PremiumGoldLight)
                }
            }
            when {
                loading -> Box(Modifier.fillMaxWidth().height(58.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = SonHarfTheme.PremiumGoldLight, strokeWidth = 2.dp)
                }
                failed -> Row(Modifier.fillMaxWidth().height(58.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(sh("Sıralama yenilenemedi", "Ranking could not refresh"), Modifier.weight(1f), color = Color.White.copy(alpha = .78f), fontSize = 11.sp)
                    TextButton(onClick = onRetry, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) { Text(sh("YENİLE", "RETRY"), color = SonHarfTheme.PremiumGoldLight, fontSize = 10.sp) }
                }
                else -> Row(Modifier.fillMaxWidth().weight(1f).semantics { isTraversalGroup = true }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumPodiumPlace(1, players.getOrNull(0), Modifier.weight(1f))
                    PremiumPodiumPlace(2, players.getOrNull(1), Modifier.weight(1f))
                    PremiumPodiumPlace(3, players.getOrNull(2), Modifier.weight(1f))
                }
            }
        }
        }
    }
}

@Composable
private fun PremiumPodiumPlace(place: Int, player: HomePodiumEntry?, modifier: Modifier) {
    val frame = when (place) {
        1 -> listOf(Color(0xFFFFE7A1), Color(0xFFD2A23D), Color(0xFF7A5313))
        2 -> listOf(Color(0xFFF5FAFF), Color(0xFFB9C6D0), Color(0xFF62707B))
        else -> listOf(Color(0xFFE9F5FF), Color(0xFF8AB4C7), Color(0xFF345B70))
    }
    val accent = frame[1]
    val name = player?.row?.displayName?.ifBlank { sh("Oyuncu", "Player") } ?: "—"
    val spoken = player?.let { sh("$place. sıra, $name, ${it.row.rating} haftalık RP",
        "Rank $place, $name, ${it.row.rating} weekly RP") } ?: sh("$place. sıra henüz boş", "Rank $place is not filled yet")
    Column(modifier.clearAndSetSemantics { contentDescription = spoken; traversalIndex = place.toFloat() },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("$place.", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Black)
        Box(Modifier.size(if (place == 1) 54.dp else 46.dp).background(Brush.linearGradient(frame), CircleShape).padding(2.dp), contentAlignment = Alignment.Center) {
            if (player != null) {
                ProfilePhotoAvatarWithGender(
                    avatarPath = if (player.profile?.avatarVisibility == "hidden") null else player.profile?.avatarPath,
                    gender = player.profile?.gender, name = name, size = if (place == 1) 50.dp else 42.dp, accent = accent,
                    visible = player.profile?.avatarVisibility != "hidden", showGenderBadge = false,
                )
            } else Surface(Modifier.fillMaxSize(), shape = CircleShape, color = Color(0xFF31453B)) {
                Box(contentAlignment = Alignment.Center) { Text("$place", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black) }
            }
        }
        Spacer(Modifier.height(3.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Text(player?.let { "${it.row.rating} RP" } ?: "— RP", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

@Composable
internal fun PremiumLeagueProgress(profile: ProfileDto?, onLeague: () -> Unit) {
    val progress = profile?.let { ratingLeagueProgress(it.rating) }
    Surface(onClick = onLeague, shape = RoundedCornerShape(22.dp), color = SonHarfTheme.SurfaceSecondary,
        border = BorderStroke(1.dp, SonHarfTheme.Border.copy(alpha = .65f))) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.EmojiEvents, null, tint = SonHarfTheme.Primary, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(progress?.let { homeLeagueName(it.leagueName) } ?: sh("Lig durumun", "Your league"),
                        color = SonHarfTheme.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(profile?.let { "${it.rating} RP" } ?: "— RP", color = SonHarfTheme.TextSecondary, fontSize = 13.sp)
                }
                Icon(Icons.Rounded.ChevronRight, sh("Lige Git", "Go to League"), tint = SonHarfTheme.Primary)
            }
            if (progress != null) {
                val detail = if (progress.nextAt == null) sh("En üst ligdesin. Yerini koru.", "You're in the top league. Hold your place.")
                else sh("${homeLeagueName(progress.nextLeagueName)} ligine ${progress.pointsToNext} RP kaldı",
                    "${progress.pointsToNext} RP to ${homeLeagueName(progress.nextLeagueName)}")
                LinearProgressIndicator(progress = { progress.progress }, modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = SonHarfTheme.Primary, trackColor = SonHarfTheme.Border)
                Text(detail, color = SonHarfTheme.TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
            } else Text(sh("Lig bilgini sıralama ekranında kontrol et.", "Check your league on the standings screen."),
                color = SonHarfTheme.TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
internal fun PremiumOtherGames(onLastLetter: () -> Unit, onLetterPath: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(sh("Diğer Oyunlar", "More Games"), color = SonHarfTheme.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Surface(shape = RoundedCornerShape(22.dp), color = SonHarfTheme.Surface,
            border = BorderStroke(1.dp, SonHarfTheme.Border.copy(alpha = .65f))) {
            Column {
                PremiumHomeMode(R.drawable.son_harf_app_icon_master, sh("Son Harf", "Last Letter"),
                    sh("Hızlı kelime düellosu", "A quick word duel"), onLastLetter)
                HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = SonHarfTheme.Border.copy(alpha = .5f))
                PremiumHomeMode(R.drawable.harf_yolu_logo, sh("Harf Yolu", "Letter Path"),
                    sh("Kelime rotanı tamamla", "Complete your word path"), onLetterPath)
            }
        }
    }
}

@Composable
private fun PremiumHomeMode(logo: Int, title: String, subtitle: String, onPlay: () -> Unit) {
    Surface(onClick = onPlay, color = Color.Transparent) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(logo), contentDescription = null, modifier = Modifier.size(48.dp), contentScale = ContentScale.Fit)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = SonHarfTheme.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = SonHarfTheme.TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                Text(sh("Oyna", "Play"), modifier = Modifier.padding(top = 6.dp), color = SonHarfTheme.Primary,
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Icon(Icons.Rounded.PlayArrow, null, tint = SonHarfTheme.Primary)
        }
    }
}

@Composable
internal fun PremiumHomeExtras(profile: ProfileDto?, onShop: () -> Unit, onSocial: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(onClick = onShop, shape = RoundedCornerShape(20.dp), color = SonHarfTheme.PrimarySoft.copy(alpha = .65f)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CardGiftcard, null, tint = SonHarfTheme.Primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(sh("Günlük ücretsiz hediye", "Daily free gift"), fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold, color = SonHarfTheme.TextPrimary)
                    Text(sh("Bugünkü ödülünü mağazada kontrol et.", "Check today's reward in the shop."),
                        fontSize = 13.sp, color = SonHarfTheme.TextSecondary)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.Primary)
            }
        }
        Surface(onClick = onShop, shape = RoundedCornerShape(20.dp), color = SonHarfTheme.Surface,
            border = BorderStroke(1.dp, SonHarfTheme.Border.copy(alpha = .6f))) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WorkspacePremium, null, tint = SonHarfTheme.Warning)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (profile?.isVip == true) sh("PRO koleksiyonun", "Your PRO collection") else sh("PRO'yu keşfet", "Explore PRO"),
                        color = SonHarfTheme.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(sh("Reklamsız kullanım · Kozmetik · Konfor", "Ad-free · Cosmetics · Comfort"),
                        color = SonHarfTheme.TextSecondary, fontSize = 13.sp)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.Primary)
            }
        }
        TextButton(onClick = onSocial, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Icon(Icons.Rounded.Groups, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(sh("Arkadaşların ve rakiplerin", "Friends and rivals"), fontSize = 14.sp)
        }
    }
}

private fun homeLeagueName(value: String): String = when (value) {
    "BRONZ" -> sh("Bronz", "Bronze")
    "GÜMÜŞ" -> sh("Gümüş", "Silver")
    "ALTIN" -> sh("Altın", "Gold")
    "PLATİN" -> sh("Platin", "Platinum")
    "ELMAS" -> sh("Elmas", "Diamond")
    "EFSANE" -> sh("Efsane", "Legend")
    else -> value
}
