package com.sonharf.game

import android.content.Context
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL

@Serializable
data class RemoteExperienceConfig(
    val version: Int = 3,
    val primaryColor: String = "#FFD36A",
    val secondaryColor: String = "#56D6FF",
    val backgroundColor: String = "#071229",
    val surfaceColor: String = "#101D39",
    val surfaceVariantColor: String = "#15284A",
    val textColor: String = "#F4F0FF",
    val mutedColor: String = "#B8B5D4",
    val homeWordArenaBadgeTr: String = "SÖZ DOKUSU DÜELLOSU",
    val homeWordArenaBadgeEn: String = "WORD WEAVE DUEL",
    val homeWordArenaSubtitleTr: String = "Her son harf, yeni bir mührü açar.",
    val homeWordArenaSubtitleEn: String = "Every final letter opens a new seal.",
    val tournamentMinutes: Int = 18,
    val brandLogoBase64Url: String = "",
)

object RemoteExperience {
    private const val CONFIG_URL = "https://raw.githubusercontent.com/makalega68-source/son-harf/main/remote/experience.json"
    private const val PREFS = "son_harf_remote_experience"
    private const val KEY_CONFIG = "config_json"
    private const val KEY_LOGO = "brand_logo_b64"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    var config by mutableStateOf(RemoteExperienceConfig())
        private set

    var brandLogoBytes by mutableStateOf<ByteArray?>(null)
        private set

    fun loadCached(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY_CONFIG, null)?.let { cached ->
            runCatching { json.decodeFromString<RemoteExperienceConfig>(cached) }
                .onSuccess { cachedConfig ->
                    // Ignore the legacy light-theme cache after the Lethara migration.
                    if (cachedConfig.version >= 3) config = cachedConfig
                }
        }
        prefs.getString(KEY_LOGO, null)?.let { encoded ->
            runCatching { Base64.decode(encoded, Base64.DEFAULT) }
                .onSuccess { brandLogoBytes = it }
        }
    }

    suspend fun refresh(context: Context) {
        val fetched = withContext(Dispatchers.IO) {
            val stamp = System.currentTimeMillis()
            val configText = runCatching { URL("$CONFIG_URL?ts=$stamp").readText() }.getOrNull()
                ?: return@withContext null
            val remote = runCatching { json.decodeFromString<RemoteExperienceConfig>(configText) }.getOrNull()
                ?: return@withContext null
            if (remote.version < 3) return@withContext null
            val logoEncoded = if (remote.brandLogoBase64Url.isNotBlank()) {
                val separator = if (remote.brandLogoBase64Url.contains('?')) '&' else '?'
                runCatching { URL("${remote.brandLogoBase64Url}${separator}ts=$stamp").readText().trim() }.getOrNull()
            } else null
            Triple(configText, remote, logoEncoded)
        } ?: return

        val (configText, remote, logoEncoded) = fetched
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        config = remote
        prefs.edit().putString(KEY_CONFIG, configText).apply()

        if (!logoEncoded.isNullOrBlank()) {
            val bytes = runCatching { Base64.decode(logoEncoded, Base64.DEFAULT) }.getOrNull()
            if (bytes != null && bytes.isNotEmpty()) {
                brandLogoBytes = bytes
                prefs.edit().putString(KEY_LOGO, logoEncoded).apply()
            }
        }
    }
}
