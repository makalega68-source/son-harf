package com.sonharf.game.data

import java.time.Instant
import java.time.OffsetDateTime

/**
 * Parses a Postgres/Supabase timestamp such as `2026-09-29T20:26:23.533775+00:00`.
 *
 * `Instant.parse` only accepts a trailing `Z` on Android 13 and older, so every
 * server deadline silently became null there: timers froze, round breaks were
 * skipped and timeouts were never claimed on those phones.
 */
fun parseServerInstant(value: String?): Instant? {
    val text = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return runCatching { OffsetDateTime.parse(text).toInstant() }.getOrNull()
        ?: runCatching { Instant.parse(text) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(text.replace(' ', 'T')).toInstant() }.getOrNull()
}
