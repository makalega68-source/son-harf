package com.sonharf.game

import com.sonharf.game.data.SharedDictionaryService
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

internal data class DictionaryDefinition(
    val headword: String,
    val meanings: List<String>,
    val sourceLabel: String,
)

/** Read-only official-TDK lookup for the last played Turkish word. */
internal object DictionaryDefinitionService {
    private const val SourceLabel = "TDK Güncel Türkçe Sözlük"
    private const val LookupAttempts = 3
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = ConcurrentHashMap<String, DictionaryDefinition>()

    suspend fun lookup(word: String, language: String): DictionaryDefinition? = withContext(Dispatchers.IO) {
        val lang = SharedDictionaryService.canonicalLanguage(language)
        if (lang != "tr") return@withContext null

        val normalized = SharedDictionaryService.normalize(word, lang)
        if (normalized.isBlank()) return@withContext null
        cache[normalized]?.let { return@withContext it }

        var lastResult: DictionaryDefinition? = null
        for (attempt in 0 until LookupAttempts) {
            lastResult = runCatching { lookupOnce(normalized, lang) }.getOrNull()
            if (lastResult != null) {
                cache[normalized] = lastResult
                return@withContext lastResult
            }
            if (attempt < LookupAttempts - 1) delay(350L * (attempt + 1))
        }
        null
    }

    private fun lookupOnce(normalized: String, language: String): DictionaryDefinition? {
        val encoded = URLEncoder.encode(normalized, Charsets.UTF_8.name())
        val connection = (URL("https://sozluk.gov.tr/gts?ara=$encoded").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Accept-Language", "tr-TR,tr;q=0.9")
            setRequestProperty("User-Agent", "SonHarf/Android")
        }

        try {
            if (connection.responseCode !in 200..299) return null
            val payload = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = runCatching { json.parseToJsonElement(payload) as? JsonArray }.getOrNull() ?: return null

            // Never display a definition for a merely similar search result. The shown headword must
            // normalize to the exact word that was played.
            val exact = root.mapNotNull { it as? JsonObject }.filter { entry ->
                val headword = entry["madde"]?.jsonPrimitive?.contentOrNull.orEmpty()
                SharedDictionaryService.normalize(headword, language) == normalized
            }
            if (exact.isEmpty()) return null

            val meanings = exact.asSequence()
                .flatMap { entry ->
                    val list = entry["anlamlarListe"] as? JsonArray ?: JsonArray(emptyList())
                    list.asSequence().mapNotNull { meaningEntry ->
                        (meaningEntry as? JsonObject)
                            ?.get("anlam")
                            ?.jsonPrimitive
                            ?.contentOrNull
                            ?.trim()
                            ?.takeIf(String::isNotBlank)
                    }
                }
                .distinct()
                .take(10)
                .toList()

            if (meanings.isEmpty()) return null
            val displayHeadword = exact.first()["madde"]
                ?.jsonPrimitive
                ?.contentOrNull
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?: normalized

            return DictionaryDefinition(displayHeadword, meanings, SourceLabel)
        } finally {
            connection.disconnect()
        }
    }
}
