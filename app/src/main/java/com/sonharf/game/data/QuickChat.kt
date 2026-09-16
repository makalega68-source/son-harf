package com.sonharf.game.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * G4.6 istemci-tarafı RPC bağlayıcıları.
 * Migration: supabase/migrations/20260917040000_g46_quick_chat_v1.sql
 *
 * "Hazır mesaj" allowlist tek gerçek kaynak sunucuda. İstemcinin
 * hangi anahtarları göstereceği [QuickChatKey] enum'unda listelenmiş.
 */
enum class QuickChatKey(val serverKey: String, val tr: String, val en: String) {
    GoodLuck("good_luck", "İyi şanslar!", "Good luck!"),
    NiceWord("nice_word", "Güzel kelime!", "Nice word!"),
    Wow("wow",         "Vay be!",       "Wow!"),
    Close("close",     "Az kaldı!",     "Almost!"),
    GG("gg",           "İyi oyundu!",   "Good game!"),
    Again("again",     "Tekrar?",       "Rematch?"),
    EmojiClap("emoji:clap", "👏", "👏"),
    EmojiWow("emoji:wow",   "😮", "😮"),
    EmojiFire("emoji:fire", "🔥", "🔥");

    fun label(language: String): String = if (language == "en") en else tr

    companion object {
        val quickMessages = listOf(GoodLuck, NiceWord, Wow, Close, GG, Again)
        val emojis = listOf(EmojiClap, EmojiWow, EmojiFire)
        fun byServerKey(serverKey: String?): QuickChatKey? =
            entries.firstOrNull { it.serverKey == serverKey }
    }
}

/**
 * Returns 0 when the message was silently dropped (block on either side).
 * Throws PostgrestException with codes 'chat_rate_limit',
 * 'chat_room_limit', 'invalid_message_key', 'not_participant', etc.
 * Callers should map those into user-visible strings.
 */
suspend fun OnlineGameBackend.sendQuickChat(
    roomId: String,
    key: QuickChatKey,
): Long {
    val res = SupabaseProvider.client.postgrest.rpc(
        "send_quick_chat",
        buildJsonObject {
            put("p_room_id", roomId)
            put("p_message_key", key.serverKey)
        },
    )
    return res.decodeAs<Long?>() ?: 0L
}

suspend fun OnlineGameBackend.muteOpponentInMatch(
    roomId: String,
    targetId: String,
) {
    SupabaseProvider.client.postgrest.rpc(
        "mute_in_match",
        buildJsonObject {
            put("p_room_id", roomId)
            put("p_target_id", targetId)
        },
    )
}

suspend fun OnlineGameBackend.unmuteOpponentInMatch(
    roomId: String,
    targetId: String,
) {
    SupabaseProvider.client.postgrest.rpc(
        "unmute_in_match",
        buildJsonObject {
            put("p_room_id", roomId)
            put("p_target_id", targetId)
        },
    )
}

suspend fun OnlineGameBackend.isOpponentMutedInMatch(
    roomId: String,
    targetId: String,
): Boolean {
    val res = SupabaseProvider.client.postgrest.rpc(
        "is_muted_in_match",
        buildJsonObject {
            put("p_room_id", roomId)
            put("p_target_id", targetId)
        },
    )
    return runCatching { res.decodeAs<Boolean>() }.getOrDefault(false)
}
