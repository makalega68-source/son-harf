package com.sonharf.game

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.RewardClaimDto
import com.sonharf.game.data.RewardKeys
import com.sonharf.game.data.RewardPassesDto
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.awaitVerifiedStoreReward
import com.sonharf.game.data.getRewardPasses
import com.sonharf.game.data.prepareStoreReward
import com.sonharf.game.data.useRewardHint
import java.time.OffsetDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The player's rewarded-video passes (banked hints, a day's keyboard or theme, Quick Games) as
 * the server reports them. A day's keyboard or theme is applied on this device until it expires.
 */
internal object RewardPassState {
    var passes by mutableStateOf<RewardPassesDto?>(null)
        private set

    suspend fun refresh() {
        if (!SupabaseProvider.configured) return
        val backend = OnlineGameBackend()
        if (backend.currentUserId() == null) return
        val next = runCatching { backend.getRewardPasses() }.getOrNull() ?: return
        passes = next
        SonHarfCosmetics.rewardKeyboardId = next.keyboard?.itemId
        SonHarfCosmetics.rewardKeyboardUntil = epochMillis(next.keyboard?.expiresAt)
        SonHarfCosmetics.rewardThemeId = next.theme?.itemId
        SonHarfCosmetics.rewardThemeUntil = epochMillis(next.theme?.expiresAt)
    }

    /** Banked hints for 'son_harf', 'siege' or 'kelime_atolyesi'. */
    fun hints(game: String): Int = passes?.let {
        when (game) {
            "son_harf" -> it.hintsSonHarf
            "siege" -> it.hintsSiege
            else -> it.hintsWorkshop
        }
    } ?: 0

    /** Spends one banked hint on the server; true when it was there to spend. */
    suspend fun useHint(game: String): Boolean {
        if (hints(game) <= 0 || !SupabaseProvider.configured) return false
        val left = runCatching { OnlineGameBackend().useRewardHint(game) }.getOrNull() ?: return false
        passes = passes?.let {
            when (game) {
                "son_harf" -> it.copy(hintsSonHarf = left)
                "siege" -> it.copy(hintsSiege = left)
                else -> it.copy(hintsWorkshop = left)
            }
        }
        return true
    }

    fun hintKey(game: String): String = when (game) {
        "son_harf" -> RewardKeys.HINTS_SON_HARF
        "siege" -> RewardKeys.HINTS_SIEGE
        else -> RewardKeys.HINTS_WORKSHOP
    }

    private fun epochMillis(value: String?): Long =
        value?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() } ?: 0L
}

private tailrec fun Context.rewardActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.rewardActivity()
    else -> null
}

/** Shows one rewarded video and claims its server-verified reward. */
internal class RewardedVideoLauncher(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val controller = RewardedAdController(context)
    private val backend = if (SupabaseProvider.configured) OnlineGameBackend() else null
    var ready by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set

    fun load() {
        if (!AdPrivacyManager.adsAllowed) {
            controller.clear()
            ready = false
            return
        }
        controller.load { ready = controller.ready }
    }

    fun clear() {
        controller.clear()
        ready = false
    }

    /**
     * Plays a video for [rewardType] and, once AdMob has verified it on the server, reports the
     * claim. [onMessage] gets a player-facing line for every outcome that is not a success.
     */
    fun watch(
        rewardType: String,
        itemId: String? = null,
        onMessage: (String) -> Unit,
        onRewarded: (RewardClaimDto) -> Unit,
    ) {
        val activity = context.rewardActivity()
        val b = backend
        if (busy) return
        if (activity == null || b == null) {
            onMessage(sh("Ödüllü video şu anda kullanılamıyor.", "Rewarded videos are unavailable right now."))
            return
        }
        busy = true
        scope.launch {
            val intentId = runCatching { b.prepareStoreReward(rewardType, itemId) }.getOrElse {
                onMessage(rewardErrorMessage(it.message.orEmpty()))
                busy = false
                return@launch
            }
            controller.show(
                activity,
                verificationUserId = b.currentUserId(),
                verificationData = intentId,
                onEarned = { responseId ->
                    scope.launch {
                        runCatching { b.awaitVerifiedStoreReward(rewardType, responseId, itemId) }
                            .onSuccess { claim ->
                                RewardPassState.refresh()
                                onRewarded(claim)
                            }
                            .onFailure { onMessage(rewardErrorMessage(it.message.orEmpty())) }
                        busy = false
                    }
                },
                onUnavailable = {
                    onMessage(sh("Video şu an hazır değil. Biraz sonra tekrar dene.", "The video isn't ready. Try again shortly."))
                    busy = false
                    ready = false
                },
                onClosed = {
                    busy = false
                    load()
                },
            )
        }
    }
}

internal fun rewardErrorMessage(raw: String): String = when {
    "daily_limit_reached" in raw -> sh("Bugünkü hakkın doldu. Yarın yine gel!", "Today's limit is used up. Come back tomorrow!")
    "weekly_limit_reached" in raw -> sh("Bu haftaki hakkın doldu.", "This week's limit is used up.")
    "mascot_owner" in raw -> sh("Maskotun zaten her maçta 3 ipucu veriyor.", "Your mascot already gives 3 hints every match.")
    "already_owned" in raw -> sh("Bu sende zaten var.", "You already have this.")
    "pass_active" in raw -> sh("Hızlı Oyun hakların hâlâ duruyor.", "You still have Quick Games left.")
    "daily_not_claimed" in raw -> sh("Önce bugünkü hediyeni al.", "Claim today's gift first.")
    "trial_item_unavailable" in raw -> sh("Bu ürün şu anda denenemiyor.", "This item can't be tried right now.")
    "rewarded_ads_unavailable" in raw -> sh("Ödüllü videolar yakında açılacak.", "Rewarded videos are coming soon.")
    "ad_verification_pending" in raw -> sh("Ödül doğrulanınca hesabına eklenecek.", "Your reward will arrive once it's verified.")
    else -> sh("Ödül işlenemedi. Tekrar dene.", "The reward could not be processed. Try again.")
}

/** A launcher tied to the screen; ads are only requested for signed-in, non-PRO players. */
@Composable
internal fun rememberRewardedVideo(enabled: Boolean = true): RewardedVideoLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = remember { RewardedVideoLauncher(context, scope) }
    LaunchedEffect(enabled) { if (enabled) launcher.load() else launcher.clear() }
    DisposableEffect(launcher) { onDispose { launcher.clear() } }
    return launcher
}
