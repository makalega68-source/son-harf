package com.sonharf.game.data

import android.content.Context
import io.github.jan.supabase.postgrest.postgrest
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
internal data class DictionaryReleaseMetaDto(
    val language: String,
    @SerialName("release_id") val releaseId: String,
    @SerialName("release_key") val releaseKey: String,
    @SerialName("checksum_sha256") val checksumSha256: String,
    @SerialName("word_count") val wordCount: Int,
    @SerialName("created_at") val createdAt: String,
)

/**
 * Small control-plane companion for [SharedDictionaryService].
 *
 * The gameplay service remains the only word-validation/cache implementation. This coordinator only
 * compares the active immutable release identity/checksum with the existing persisted V4 snapshot.
 * If either identity or content checksum changed, it forces the normal authoritative V4 refresh.
 * With no active release (pre-rollout / rollback of the feature itself), legacy V4 behavior remains.
 */
object DictionaryReleaseCacheCoordinator {
    private const val RELEASE_PREFS = "son_harf_dictionary_release_v1"
    private const val LEGACY_SNAPSHOT_PREFS = "son_harf_dictionary_snapshot_v4"
    private const val LEGACY_WORDS_PREFIX = "words_"

    suspend fun refreshIfNeeded(context: Context, language: String): Boolean {
        val lang = SharedDictionaryService.canonicalLanguage(language)
        if (!SupabaseProvider.configured) return SharedDictionaryService.restorePersisted(context, lang)

        val meta = runCatching {
            SupabaseProvider.client.postgrest.rpc(
                "get_dictionary_release_meta_v1",
                buildJsonObject { put("p_language", lang) },
            ).decodeSingle<DictionaryReleaseMetaDto>()
        }.getOrNull() ?: return SharedDictionaryService.restorePersisted(context, lang)

        require(meta.language == lang) { "dictionary_release_language_mismatch" }
        require(meta.wordCount > 0) { "dictionary_release_empty" }
        require(meta.checksumSha256.matches(Regex("^[0-9a-f]{64}$"))) { "dictionary_release_checksum_invalid" }

        val releasePrefs = context.getSharedPreferences(RELEASE_PREFS, Context.MODE_PRIVATE)
        val storedRelease = releasePrefs.getString("release_id_$lang", null)
        val storedChecksum = releasePrefs.getString("checksum_$lang", null)
        val localChecksum = persistedCanonicalChecksum(context, lang)

        if (
            !needsRefresh(
                storedReleaseId = storedRelease,
                storedChecksum = storedChecksum,
                localChecksum = localChecksum,
                activeReleaseId = meta.releaseId,
                activeChecksum = meta.checksumSha256,
            ) && SharedDictionaryService.restorePersisted(context, lang)
        ) {
            return true
        }

        val refreshed = SharedDictionaryService.preloadCanonical(context, lang)
        val refreshedChecksum = deterministicChecksum(refreshed)
        require(refreshed.size == meta.wordCount) { "dictionary_release_word_count_mismatch" }
        require(refreshedChecksum == meta.checksumSha256) { "dictionary_release_checksum_mismatch" }

        releasePrefs.edit()
            .putString("release_id_$lang", meta.releaseId)
            .putString("checksum_$lang", meta.checksumSha256)
            .apply()
        return true
    }

    internal fun needsRefresh(
        storedReleaseId: String?,
        storedChecksum: String?,
        localChecksum: String?,
        activeReleaseId: String,
        activeChecksum: String,
    ): Boolean =
        storedReleaseId != activeReleaseId ||
            storedChecksum != activeChecksum ||
            localChecksum != activeChecksum

    internal fun deterministicChecksum(words: Collection<String>): String {
        val payload = words.asSequence()
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .joinToString("\n") { word ->
                val utf8 = word.toByteArray(StandardCharsets.UTF_8)
                "${utf8.size}:$word"
            }
        return MessageDigest.getInstance("SHA-256")
            .digest(payload.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private fun persistedCanonicalChecksum(context: Context, language: String): String? {
        val raw = context.getSharedPreferences(LEGACY_SNAPSHOT_PREFS, Context.MODE_PRIVATE)
            .getString(LEGACY_WORDS_PREFIX + language, null)
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return deterministicChecksum(raw.lineSequence().filter { it.isNotBlank() }.toList())
    }
}
