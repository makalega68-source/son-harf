package com.sonharf.game

import com.sonharf.game.data.SharedDictionaryService
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
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

/** Lightweight read-only lookup for the last played Turkish word. */
internal object DictionaryDefinitionService {
    private const val SourceLabel = "TDK Güncel Türkçe Sözlük"
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = ConcurrentHashMap<String, DictionaryDefinition>()

    suspend fun lookup(word: String, language: String): DictionaryDefinition? = withContext(Dispatchers.IO) {
        val lang = SharedDictionaryService.canonicalLanguage(language)
        if (lang != "tr") return@withContext null

        val normalized = SharedDictionaryService.normalize(word, lang)
        if (normalized.isBlank()) return@withContext null
        cache[normalized]?.let { return@withContext it }

        val encoded = URLEncoder.encode(normalized, Charsets.UTF_8.name())
        val connection = (URL("https://sozluk.gov.tr/gts?ara=$encoded").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 6_000
            readTimeout = 6_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "SonHarf/Android")
        }

        try {
            if (connection.responseCode !in 200..299) return@withContext null
            val payload = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = runCatching { json.parseToJsonElement(payload) as? JsonArray }.getOrNull() ?: return@withContext null

            val entries = root.mapNotNull { it as? JsonObject }
            val exact = entries.filter { entry ->
                val headword = entry["madde"]?.jsonPrimitive?.contentOrNull.orEmpty()
                SharedDictionaryService.normalize(headword, lang) == normalized
            }.ifEmpty { entries.take(1) }

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

            if (meanings.isEmpty()) return@withContext null
            val displayHeadword = exact.firstOrNull()
                ?.get("madde")
                ?.jsonPrimitive
                ?.contentOrNull
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?: normalized

            DictionaryDefinition(displayHeadword, meanings, SourceLabel).also { cache[normalized] = it }
        } finally {
            connection.disconnect()
        }
    }
}
