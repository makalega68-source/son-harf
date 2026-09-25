package com.sonharf.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.SupabaseProvider
import com.sonharf.game.data.getPublicCosmetics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * The single rule for which frame a player's avatar wears:
 * 1. the frame the player selected (equipped on the server, so it is owned),
 * 2. otherwise the golden frame for PRO/VIP players,
 * 3. otherwise the white/neutral starter frame.
 * The golden frame is never shown for a player who is not PRO, even if it is still equipped.
 */
internal object PlayerFrameResolver {
    fun resolve(selectedFrameId: String?, isPro: Boolean): String {
        val selected = selectedFrameId?.takeIf { it.isNotBlank() && PurchasedFrameCatalog.drawable(it) != null }
        return when {
            selected != null && (selected != PurchasedFrameCatalog.GOLDEN_AVATAR || isPro) -> selected
            isPro -> PurchasedFrameCatalog.GOLDEN_AVATAR
            else -> PurchasedFrameCatalog.STARTER_NEUTRAL
        }
    }
}

/** Source-compatible entry point for profile avatars; the frame comes from [PlayerFrameResolver]. */
@Composable
internal fun FramedProfilePhotoAvatar(
    avatarPath: String?,
    gender: String?,
    name: String,
    size: Dp,
    frameId: String?,
    accent: Color = SonHarfCyan,
    visible: Boolean = true,
    showGenderBadge: Boolean = true,
    isPro: Boolean = false,
) {
    ProfilePhotoAvatarWithGender(
        avatarPath = avatarPath,
        gender = gender,
        name = name,
        size = size,
        accent = accent,
        visible = visible,
        showGenderBadge = showGenderBadge,
        frameId = frameId,
        isPro = isPro,
    )
}

/** Equipped frame ids of other players, read once per session from the server. */
private object PlayerFrameCache {
    private val frames = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val loaded = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    fun cached(userId: String): String? = frames[userId]

    suspend fun load(userId: String): String? {
        if (userId in loaded || !SupabaseProvider.configured) return frames[userId]
        val id = runCatching { OnlineGameBackend().getPublicCosmetics(userId)?.profileFrameId }.getOrNull()
        if (!id.isNullOrBlank()) frames[userId] = id
        loaded += userId
        return frames[userId]
    }
}

/** The frame a player has equipped (server-authoritative), or null while unknown. */
@Composable
internal fun rememberPlayerFrameId(userId: String?): String? {
    // Only real account ids (UUIDs) are looked up; bots and previews keep the default frame.
    if (userId.isNullOrBlank() || userId.length != 36) return null
    val frame by produceState(PlayerFrameCache.cached(userId), userId) { value = PlayerFrameCache.load(userId) }
    return frame
}
