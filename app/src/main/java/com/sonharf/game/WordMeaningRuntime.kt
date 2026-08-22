package com.sonharf.game

import android.content.Context
import android.content.SharedPreferences
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder
import java.util.Locale

/**
 * Dictionary runtime with two-level cache. Successfully resolved meanings are kept in
 * memory and persisted on-device. Turkish definitions prefer TDK Guncel Turkce Sozluk;
 * English definitions prefer DictionaryAPI. Wiktionary is a secondary fallback.
 */
internal object WordMeaningRuntime {
    private val http = HttpClient(OkHttp)
    private val json = Json { ignoreUnknownKeys = true }
    private val memory = LinkedHashMap<String, String>()
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("word_meaning_cache_v3", Context.MODE_PRIVATE)
    }

    private fun normalize(word: String, language: String): String {
        val locale = if (language == "tr") Locale("tr", "TR") else Locale.ENGLISH
        return word.trim().lowercase(locale)
    }

    private fun store(key: String, value: String) {
        if (value.isBlank()) return
        synchronized(memory) {
            memory[key] = value
            while (memory.size > 800) memory.remove(memory.keys.first())
        }
        prefs?.edit()?.putString(key, value)?.apply()
    }

    suspend fun meaning(word: String, language: String): String {
        val normalized = normalize(word, language)
        val key = "$language:$normalized"
        synchronized(memory) { memory[key] }?.let { return it }
        prefs?.getString(key, null)?.takeIf { it.isNotBlank() }?.let {
            synchronized(memory) { memory[key] = it }
            return it
        }

        val encoded = URLEncoder.encode(normalized, Charsets.UTF_8.name()).replace("+", "%20")
        val host = if (language == "en") "en.wiktionary.org" else "tr.wiktionary.org"

        val tdkApi = if (language == "tr") runCatching {
            val body = http.get("https://sozluk.gov.tr/gts?ara=$encoded").bodyAsText()
            val firstEntry = json.parseToJsonElement(body).jsonArray.firstOrNull()?.jsonObject
            val meanings = firstEntry?.get("anlamlarListe")?.jsonArray
            meanings.orEmpty().asSequence().mapNotNull { item ->
                item.jsonObject["anlam"]?.jsonPrimitive?.content?.trim()
            }.firstOrNull { !it.isNullOrBlank() }.orEmpty()
        }.getOrDefault("") else ""

        val dictionaryApi = if (language == "en") runCatching {
            val body = http.get("https://api.dictionaryapi.dev/api/v2/entries/en/$encoded").bodyAsText()
            val root = json.parseToJsonElement(body).jsonArray.firstOrNull()?.jsonObject
            root?.get("meanings")?.jsonArray.orEmpty().asSequence().mapNotNull { meaning ->
                meaning.jsonObject["definitions"]?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("definition")?.jsonPrimitive?.content?.trim()
            }.firstOrNull { !it.isNullOrBlank() }.orEmpty()
        }.getOrDefault("") else ""

        val primary = if (language == "tr") tdkApi else dictionaryApi
        val restSummary = if (primary.isBlank()) runCatching {
            val body = http.get("https://$host/api/rest_v1/page/summary/$encoded").bodyAsText()
            val root = json.parseToJsonElement(body).jsonObject
            root["extract"]?.jsonPrimitive?.content?.trim().orEmpty()
        }.getOrDefault("") else ""

        val queryExtract = if (primary.isBlank() && restSummary.isBlank()) runCatching {
            val url = "https://$host/w/api.php?action=query&format=json&prop=extracts&explaintext=1&redirects=1&titles=$encoded"
            val root = json.parseToJsonElement(http.get(url).bodyAsText()).jsonObject
            val pages = root["query"]?.jsonObject?.get("pages")?.jsonObject
            pages?.values?.asSequence()?.mapNotNull { it.jsonObject["extract"]?.jsonPrimitive?.content?.trim() }
                ?.firstOrNull { it.isNotBlank() }.orEmpty()
        }.getOrDefault("") else ""

        val raw = primary.ifBlank { restSummary }.ifBlank { queryExtract }
        val concise = raw
            .replace(Regex("\\s+"), " ")
            .replace(Regex("^${Regex.escape(normalized)}\\s*", RegexOption.IGNORE_CASE), "")
            .trim(' ', '-', ':', ';')
            .let { if (it.length > 520) it.take(517).trimEnd() + "…" else it }

        if (concise.isNotBlank()) {
            store(key, concise)
            return concise
        }

        return if (language == "en")
            "Bu kelime oyun sözlüğünde geçerli; ancak kısa İngilizce tanımı kaynaklardan alınamadı."
        else
            "Bu kelime oyun sözlüğünde geçerli; ancak TDK kaynağında kısa tanımı bulunamadı."
    }
}
