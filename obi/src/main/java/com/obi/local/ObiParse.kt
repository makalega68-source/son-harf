package com.obi.local

import java.util.Calendar
import java.util.Locale

/** Pure text parsing for local commands, kept free of Android types so it can be unit tested. */
internal object ObiParse {
    private val tr = Locale("tr", "TR")
    private val notePrefixes = listOf("not al", "bunu not et", "şunu not et", "not et")
    private val relative = Regex("(\\d{1,3})\\s*(dakika|dk|saat)\\s+sonra")
    // A suffix such as 9'a / 9'da counts only when no letter follows, so "5 dakika" is not read as 05:00.
    private val clock = Regex("(?:saat\\s*)?(?<!\\d)(\\d{1,2})(?:[:.](\\d{2}))?(?:\\s*['’]?(?:da|de|ta|te|a|e)(?!\\p{L}))?(?!\\d)")
    private val weekdayWords = setOf("bugün", "bugun", "yarın", "yarin", "şimdi", "simdi", "acaba", "orada", "burada")

    fun lower(text: String): String = text.lowercase(tr)

    /** Minutes for "20 dakika sonra" / "2 saat sonra", or null. */
    fun relativeMinutes(t: String): Int? {
        val m = relative.find(t) ?: return null
        val n = m.groupValues[1].toIntOrNull() ?: return null
        return (if (m.groupValues[2] == "saat") n * 60 else n).takeIf { it in 1..1440 }
    }

    /** Hour and minute named in the text, e.g. "sabah 9'a", "saat 14.30", "akşam 7'de". */
    fun clockTime(t: String): Pair<Int, Int>? {
        val match = clock.findAll(t).firstOrNull { candidate ->
            val prefix = t.substring(0, candidate.range.first).trimEnd()
            val suffix = t.substring(candidate.range.last + 1).trimStart()
            val digits = candidate.groupValues[1]
            val tail = candidate.value.substringAfter(digits)
            candidate.value.startsWith("saat") || candidate.value.contains(':') || candidate.value.contains('.') ||
                listOf("saat", "sabah", "akşam", "aksam", "gece", "öğlen", "oglen").any(prefix::endsWith) ||
                tail.any { it == 'a' || it == 'e' } || suffix.startsWith("alarm")
        } ?: return null
        var hour = match.groupValues[1].toIntOrNull()?.takeIf { it in 0..23 } ?: return null
        val minute = match.groupValues[2].toIntOrNull()?.takeIf { it in 0..59 } ?: 0
        if (listOf("akşam", "aksam", "öğleden sonra", "ogleden sonra").any(t::contains) && hour in 1..11) hour += 12
        if (listOf("sabah", "gece").any(t::contains) && hour == 12) hour = 0
        return hour to minute
    }

    /** Absolute trigger time for an alarm or reminder text, or null when no time is given. */
    fun triggerTime(t: String, now: Long = System.currentTimeMillis()): Long? {
        relativeMinutes(t)?.let { return now + it * 60_000L }
        val (hour, minute) = clockTime(t) ?: return null
        val tomorrow = t.contains("yarın") || t.contains("yarin")
        return Calendar.getInstance().apply {
            timeInMillis = now
            if (tomorrow) add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (!tomorrow && timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
    }

    /** The reminder's own words once the time phrase and the command verb are removed. */
    fun reminderNote(text: String): String = text
        .replace(Regex("(?i)\\d{1,3}\\s*(?:dakika|dk|saat)\\s+sonra"), "")
        .replace(Regex("(?i)(?:yarın|yarin|bugün|bugun)?\\s*(?:sabah|akşam|aksam|gece|öğlen|oglen)?\\s*(?:saat\\s*)?\\d{1,2}(?:[:.]\\d{2})?(?:\\s*[’']?(?:da|de|ta|te|a|e)(?!\\p{L}))?"), "")
        .replace(Regex("(?i)(?:diye\\s+)?hatırlat(?:ır mısın|ir misin)?"), "")
        .replace(Regex("\\s{2,}"), " ")
        .trim(' ', ',', ':', '-', '’', '\'').ifBlank { "Hatırlatıcı" }

    fun noteText(normalized: String, original: String): String? {
        val prefix = notePrefixes.firstOrNull { normalized.startsWith(it) } ?: return null
        return original.drop(prefix.length).trim(' ', ':', '-', '“', '”').takeIf { it.isNotBlank() }
    }

    fun timerMinutes(t: String): Int? {
        if (!t.contains("zamanlayıcı") && !t.contains("zamanlayici") && !t.contains("timer")) return null
        val m = Regex("(\\d{1,3})\\s*(dakika|dk|saat)").find(t) ?: return null
        val n = m.groupValues[1].toIntOrNull() ?: return null
        return (if (m.groupValues[2] == "saat") n * 60 else n).takeIf { it in 1..1440 }
    }

    /**
     * True only for clear local commands. Everyday sentences that merely contain "adım", "alarm"
     * or "not" go to Gemini (or the local small talk) instead of being swallowed here.
     */
    fun isLocalCommand(raw: String): Boolean {
        val t = lower(raw.trim())
        if (noteText(t, raw.trim()) != null) return true
        if (t.contains("notlarım") || t.contains("notlarim")) return true
        if (timerMinutes(t) != null) return true
        if (t.contains("hatırlat") || t.contains("hatirlat")) return true
        if (t.contains("uyandır") || t.contains("uyandir") || t.contains("alarm kur")) return true
        if (t.contains("alarm") && (relativeMinutes(t) != null || clockTime(t) != null)) return true
        if (listOf("doğum günüm", "dogum gunum", "sabah motive et", "günaydın mesajı", "sabah mesajını kapat").any(t::contains)) return true
        return t.startsWith("adım ") || t.contains("benim adım ")
    }

    fun isWeatherQuestion(text: String): Boolean {
        val t = lower(text)
        return t.contains("hava durumu") || t.contains("hava nasıl") || t.contains("hava nasil") ||
            Regex("ka[çc] derece(?!\\p{L})").containsMatchIn(t)
    }

    /** City names to try in order; "istanbulda" also yields "istanbul". */
    fun cityCandidates(text: String): List<String> {
        val t = lower(text)
        val before = Regex("([\\p{L}]{2,40})(['’](?:da|de|ta|te))?\\s+(?:hava|ka[çc] derece)").find(t)
            ?.groupValues?.get(1)?.takeUnless { it in weekdayWords }
        val after = Regex("(?:hava durumu|hava nasıl|hava nasil|ka[çc] derece)\\s+([\\p{L}]{2,40})").find(t)
            ?.groupValues?.get(1)?.takeUnless { it in weekdayWords }
        val city = before ?: after ?: return emptyList()
        val stripped = Regex("^(.{3,})(?:da|de|ta|te)$").find(city)?.groupValues?.get(1)
        return listOfNotNull(city, stripped).distinct()
    }
}
