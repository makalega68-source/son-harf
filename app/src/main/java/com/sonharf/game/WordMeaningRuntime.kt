package com.sonharf.game

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder

internal object WordMeaningRuntime {
    private val http = HttpClient(OkHttp)
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = mutableMapOf<String, String>()

    suspend fun meaning(word: String, language: String): String {
        val normalized = word.trim().lowercase()
        val key = "$language:$normalized"
        cache[key]?.let { return it }
        val encoded = URLEncoder.encode(normalized, Charsets.UTF_8.name()).replace("+", "%20")

        val dictionaryApi = if (language == "en") runCatching {
            val body = http.get("https://api.dictionaryapi.dev/api/v2/entries/en/$encoded").bodyAsText()
            val root = json.parseToJsonElement(body).jsonArray.firstOrNull()?.jsonObject
            val meanings = root?.get("meanings")?.jsonArray.orEmpty()
            meanings.asSequence().mapNotNull { meaning ->
                meaning.jsonObject["definitions"]?.jsonArray?.firstOrNull()?.jsonObject?.get("definition")?.jsonPrimitive?.content?.trim()
            }.firstOrNull { !it.isNullOrBlank() }.orEmpty()
        }.getOrDefault("") else ""

        val wiktionary = if (dictionaryApi.isBlank()) runCatching {
            val host = if (language == "en") "en.wiktionary.org" else "tr.wiktionary.org"
            val url = "https://$host/w/api.php?action=query&format=json&prop=extracts&exintro=1&explaintext=1&redirects=1&titles=$encoded"
            val root = json.parseToJsonElement(http.get(url).bodyAsText()).jsonObject
            val pages = root["query"]?.jsonObject?.get("pages")?.jsonObject
            pages?.values?.asSequence()?.mapNotNull { it.jsonObject["extract"]?.jsonPrimitive?.content?.trim() }?.firstOrNull { it.isNotBlank() }.orEmpty()
        }.getOrDefault("") else ""

        val raw = dictionaryApi.ifBlank { wiktionary }
        val concise = raw.replace(Regex("\\s+"), " ").trim().let { if (it.length > 420) it.take(417).trimEnd() + "…" else it }
        val value = concise.ifBlank {
            if (language == "en") "Bu İngilizce kelimenin kısa sözlük anlamı şu anda alınamadı. Daha sonra tekrar deneyebilirsin."
            else "Bu kelimenin kısa sözlük anlamı şu anda alınamadı. Daha sonra tekrar deneyebilirsin."
        }
        cache[key] = value
        return value
    }
}
