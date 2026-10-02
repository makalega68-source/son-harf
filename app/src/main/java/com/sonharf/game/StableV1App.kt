package com.sonharf.game

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.ComebackGiftDto
import com.sonharf.game.data.PresenceBackend
import com.sonharf.game.data.SupabaseProvider

/** Unified Pro startup shell: language -> auth -> premium product. */
@Composable
fun StableV1App() {
    val context = LocalContext.current
    remember(context) {
        SonHarfCosmetics.restore(context)
        WordSiegeBoardSkins.restore(context)
        WordSiegeBonusIcons.init(context)
        WordSiegeMascotOwnership.restore(context)
        true
    }
    var authChecked by remember { mutableStateOf(false) }
    var authenticated by remember { mutableStateOf(false) }
    // The welcome clip and language choice come before sign-in. A signed-in player never sees them
    // again: opening the app goes straight to the home page until they sign out in the profile.
    var introDone by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // The stored session loads asynchronously: wait for it (and silently re-login with the
        // remembered credentials if needed) so a signed-in player never sees the entry screens again.
        authenticated = SupabaseProvider.configured && (hasVerifiedMembershipSession() || restoreMembershipSession(context))
        authChecked = true
    }

    if (!authChecked) {
        LaunchSplashFrame()
        return
    }

    if (!authenticated) {
        if (!introDone) {
            IntroWelcomeScreen { language ->
                FirstRunLanguagePreferences.complete(context, language)
                SonHarfUiState.language = language
                introDone = true
            }
            return
        }
        CompactAuthGate { authenticated = true }
        return
    }

    // Players who skipped the language screen (e.g. an update over an older install) still get
    // the classic mascot's welcome once, on their first entry.
    var mascotWelcomePending by remember { mutableStateOf(false) }
    var comebackGift by remember { mutableStateOf<ComebackGiftDto?>(null) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        // Every entry counts as a visit; a player back after days away gets Obi's gift.
        FirstRunLanguagePreferences.markMascotWelcomeSeen(context)
        comebackGift = runCatching { PresenceBackend.touch() }.getOrNull()?.takeIf { it.gift > 0 }
    }
    LaunchedEffect(mascotWelcomePending) {
        // Ask for reminder notifications once, after the welcome, never over it.
        if (!mascotWelcomePending && ReminderNotifications.shouldAskPermission(context)) {
            ReminderNotifications.markPermissionAsked(context)
            runCatching { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }
    }
    Box(Modifier.fillMaxSize()) {
        PremiumUnifiedProApp(onSignedOut = {
            // Signing out brings back the welcome, the language choice and sign-in.
            introDone = false
            authenticated = false
        })
        val gift = comebackGift
        if (gift != null && !mascotWelcomePending) {
            ComebackGiftDialog(gift) { comebackGift = null }
        }
    }
}

/** Obi welcomes a player who has been away and hands over the Son Coin the server just credited. */
@Composable
private fun ComebackGiftDialog(gift: ComebackGiftDto, onDismiss: () -> Unit) {
    LaunchedEffect(gift) { SonHarfSoundFx.bonus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) { Text(sh("Teşekkürler Obi!", "Thanks, Obi!")) }
        },
        title = { Text(sh("Obi seni özledi! 🎁", "Obi missed you! 🎁"), fontWeight = FontWeight.Black) },
        text = {
            Text(
                sh(
                    "${gift.daysAway} gündür yoktun. Obi sana ${gift.gift} Son Coin biriktirdi. Hoş geldin!",
                    "You were away for ${gift.daysAway} days. Obi saved ${gift.gift} Son Coins for you. Welcome back!",
                ),
            )
        },
    )
}

/** A one-time greeting from the classic mascot over the home screen; tap anywhere to continue. */
/**
 * Keeps the existing authentication flow intact while making the oversized entry controls
 * slightly more compact on phones. The language selector is intentionally unaffected.
 */
@Composable
private fun CompactAuthGate(onAuthenticated: () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density * 0.94f,
            fontScale = density.fontScale,
        )
    ) {
        RequiredAuthGate(onAuthenticated)
    }
}

