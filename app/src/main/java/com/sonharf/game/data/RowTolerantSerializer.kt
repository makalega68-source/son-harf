package com.sonharf.game.data

import io.github.jan.supabase.SupabaseSerializer
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json
import kotlin.reflect.KType

/**
 * PostgREST answers an RPC that returns one row (e.g. `returns word_siege_games`)
 * with a single JSON object, while `decodeSingle()` / `decodeList()` always expect
 * a JSON array. Every such call threw after the server had already applied the
 * action, which surfaced as "İşlem tamamlanamadı" and left screens stale.
 *
 * This serializer wraps a lone object (or `null`) into a list whenever a list is
 * requested, so both response shapes decode correctly everywhere in the app.
 */
class RowTolerantSerializer(
    private val delegate: SupabaseSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true }),
) : SupabaseSerializer {

    override fun <T> encode(type: KType, value: T): String = delegate.encode(type, value)

    override fun <T> decode(type: KType, value: String): T =
        delegate.decode(type, normalizeForType(type, value))

    companion object {
        private fun isListType(type: KType): Boolean {
            val classifier = type.classifier
            return classifier == List::class || classifier == Collection::class ||
                classifier == Iterable::class || classifier == ArrayList::class
        }

        fun normalizeForType(type: KType, value: String): String {
            if (!isListType(type)) return value
            val trimmed = value.trim()
            return when {
                trimmed.isEmpty() || trimmed == "null" -> "[]"
                trimmed.startsWith("{") -> "[$trimmed]"
                else -> value
            }
        }
    }
}
