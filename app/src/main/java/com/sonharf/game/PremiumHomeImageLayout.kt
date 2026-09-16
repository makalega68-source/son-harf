package com.sonharf.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Toll
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SharedDictionaryService
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.getLeaderboardV2

/**
 * Restores the proven compact profile header from the latest working main shell.
 * The brand/logo hero is intentionally absent: profile identity is the first home element.
 */
@Composable
internal fun PremiumHomeProfileStrip(
    profile: ProfileDto?,
    onProfile: () -> Unit,
    onSocial: () -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSocial, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Rounded.Notifications,
                    sh("Bildirimler ve davetler", "Notifications and invites"),
                    tint = SonHarfTheme.Primary,
                )
            }
        }
        Surface(onClick = onProfile, color = Color.Transparent, shape = RoundedCornerShape(20.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FramedProfilePhotoAvatar(
                    avatarPath = profile?.avatarPath,
                    gender = profile?.gender,
                    name = profile?.displayName ?: sh("Oyuncu", "Player"),
                    size = 44.dp,
                    frameId = SonHarfCosmetics.profileFrameId,
                    accent = SonHarfTheme.Primary,
                    visible = profile?.avatarVisibility != "hidden",
                    isPro = profile?.isVip == true,
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        profile?.displayName ?: sh("Profilin", "Your profile"),
                        color = SonHarfCosmetics.playerNameColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        profile?.let { "${homeLeagueLabel(ratingLeagueProgress(it.rating).leagueName)} · ${it.rating} RP" } ?: "— RP",
                        color = SonHarfTheme.TextSecondary,
                        fontSize = 13.sp,
                    )
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.TextSecondary)
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Toll, null, modifier = Modifier.size(18.dp), tint = SonHarfTheme.Primary)
            Text(
                "${profile?.diamonds?.toString() ?: "—"} Son Coin",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                Icons.Rounded.WorkspacePremium,
                null,
                modifier = Modifier.size(18.dp),
                tint = if (profile?.isVip == true) SonHarfTheme.Warning else SonHarfTheme.TextSecondary,
            )
            Text(
                when (profile?.isVip) {
                    true -> sh("PRO ÜYE", "PRO MEMBER")
                    false -> sh("Standart üyelik", "Standard plan")
                    null -> "—"
                },
                fontSize = 13.sp,
                color = SonHarfTheme.TextSecondary,
            )
        }
    }
}

/** Uses the supplied WebP itself as the home CTA; no extra logo or duplicate labels are painted. */
@Composable
internal fun PremiumModeArtworkButton(
    drawable: Int,
    description: String,
    aspectRatio: Float,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        shadowElevation = 2.dp,
    ) {
        Image(
            painter = painterResource(drawable),
            contentDescription = description,
            modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio),
            contentScale = ContentScale.Fit,
        )
    }
}

/** Loads the authoritative weekly top three and reuses the existing proven podium component. */
@Composable
internal fun PremiumHomeWeeklyTop3(
    backend: OnlineGameBackend,
    onOpenLeague: () -> Unit,
) {
    var entries by remember { mutableStateOf<List<HomePodiumEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var retryKey by remember { mutableIntStateOf(0) }
    val language = SharedDictionaryService.canonicalLanguage(SonHarfUiState.language)

    LaunchedEffect(language, retryKey) {
        loading = true
        failed = false
        if (!SupabaseProvider.configured) {
            loading = false
            failed = true
            return@LaunchedEffect
        }
        runCatching {
            backend.getLeaderboardV2(language = language, period = "week", limit = 3).map { row ->
                val profile = runCatching { backend.getProfile(row.userId) }.getOrNull()
                HomePodiumEntry(row = row, profile = profile)
            }
        }.onSuccess {
            entries = it
            loading = false
        }.onFailure {
            entries = emptyList()
            loading = false
            failed = true
        }
    }

    PremiumWeeklyPodiumShowcase(
        players = entries,
        loading = loading,
        failed = failed,
        onOpenLeague = onOpenLeague,
        onRetry = { retryKey += 1 },
    )
}

@Composable
internal fun PremiumCompetitionShortcut(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(46.dp)) {
        Text(
            sh("Rekabet Merkezi", "Competition Hub"),
            color = SonHarfTheme.Primary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Rounded.ChevronRight, null, tint = SonHarfTheme.Primary, modifier = Modifier.size(18.dp))
    }
}

private fun homeLeagueLabel(value: String): String = when (value) {
    "BRONZ" -> sh("Bronz", "Bronze")
    "GÜMÜŞ" -> sh("Gümüş", "Silver")
    "ALTIN" -> sh("Altın", "Gold")
    "PLATİN" -> sh("Platin", "Platinum")
    "ELMAS" -> sh("Elmas", "Diamond")
    "EFSANE" -> sh("Efsane", "Legend")
    else -> value
}
