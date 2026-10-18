package com.obi.local

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.PlaybackParams
import android.util.Base64
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

/**
 * Obi's voice from Gemini text-to-speech: native Turkish, then pitched up (without speeding up)
 * so it sounds small, like a kitten. Clips are made ahead of time and kept on the phone, so an
 * alarm still speaks without internet.
 */
internal class ObiGeminiVoice(private val context: Context) {
    companion object {
        private val MODELS = listOf("gemini-2.5-flash-preview-tts", "gemini-2.5-flash-tts", "gemini-2.5-pro-preview-tts")
        /** Gemini voices to try with "sesini değiştir"; the user's choice is remembered. */
        val VOICES = listOf("Leda", "Zephyr", "Aoede", "Kore", "Puck", "Laomedeia", "Achernar")
        private const val DAILY_LIMIT = 40

        /** Fixed lines, made once and reused. */
        val FIXED = mapOf(
            // Most needed first, so a small daily allowance goes to what is actually heard.
            "reminder" to "Hatırlatma zamanı geldi!",
            "done" to "Tamam, hallettim!",
            "preview" to "Merhaba! Ben Obi. Sesim böyle, beğendin mi?",
            "listening" to "Efendim?",
            "alarm" to "Alarm zamanı geldi! Hadi bakalım, kalkıyoruz!",
            "alarm_morning" to "Günaydın! Sabah oldu, hadi uyan bakalım. Yeni bir gün seni bekliyor!",
        )

        fun reminderLine(text: String) = "Hatırlatma var: ${text.trimEnd('.', '!')}."
    }

    private val prefs = context.getSharedPreferences("obi_gemini", Context.MODE_PRIVATE)
    private val dir = File(context.filesDir, "voice").apply { mkdirs() }

    fun voiceName(): String = prefs.getString("voice_name", VOICES.first()) ?: VOICES.first()

    /** Switches to the next voice and drops the clips made with the old one. */
    fun nextVoice(): String {
        val next = VOICES[(VOICES.indexOf(voiceName()) + 1).mod(VOICES.size)]
        prefs.edit().putString("voice_name", next).apply()
        dir.listFiles()?.forEach { it.delete() }
        return next
    }

    /** What happened the last time a clip was requested, for "ses durumu". */
    fun lastStatus(): String = prefs.getString("voice_status", "Henüz ses üretilmedi.") ?: ""

    private fun status(text: String) = prefs.edit().putString("voice_status", text).apply()

    fun file(key: String) = File(dir, "$key.pcm")
    fun has(key: String) = file(key).length() > 1000
    fun forget(key: String) { file(key).delete() }

    /** Makes the clip if it is missing. Blocking; call from a background thread. */
    fun prepareBlocking(key: String, text: String): Boolean {
        if (has(key)) return true
        val apiKey = ObiGemini(context).apiKey() ?: return false
        val today = LocalDate.now().toString()
        val used = if (prefs.getString("voice_day", "") == today) prefs.getInt("voice_used", 0) else 0
        if (used >= DAILY_LIMIT) { status("Bugünlük ses hakkı doldu ($DAILY_LIMIT)."); return false }
        val blockedUntil = prefs.getLong("voice_blocked_until", 0L)
        if (System.currentTimeMillis() < blockedUntil) {
            status("Ücretsiz Gemini ses hakkı bugünlük bitti; ${clock(blockedUntil)} civarında yenilenecek. Hazır sesler kullanılıyor.")
            return false
        }
        val preferred = prefs.getString("voice_model", null)
        val candidates = (listOfNotNull(preferred) + MODELS).distinct().toMutableList()
        var lastError = "bağlantı kurulamadı"
        var discovered = false
        var index = 0
        while (index < candidates.size) {
            val model = candidates[index++]
            val result = runCatching { request(apiKey, model, text) }.getOrElse { lastError = it.javaClass.simpleName; null } ?: continue
            val audio = result.second
            if (audio == null) {
                lastError = "kod ${result.first}${result.third?.let { ": $it" } ?: ""}"
                // Unknown model: ask Google which text-to-speech models this key can use, once.
                if ((result.first == 404 || result.first == 400) && !discovered) {
                    discovered = true
                    candidates += runCatching { ttsModels(apiKey) }.getOrDefault(emptyList()).filter { it !in candidates }
                }
                if (result.first == 429 && (result.third ?: "").contains("quota", ignoreCase = true)) {
                    // The free daily allowance is used up: stop asking until Google resets it
                    // (midnight Pacific time), so no requests are wasted.
                    prefs.edit().putLong("voice_blocked_until", nextQuotaReset()).apply()
                    status("Ücretsiz Gemini ses hakkı bugünlük bitti; ${clock(nextQuotaReset())} civarında yenilenecek. Hazır sesler kullanılıyor.")
                    return false
                }
                if (result.first == 429 || result.first == 401 || result.first == 403) break
                continue
            }
            prefs.edit().putString("voice_model", model).putString("voice_day", today).putInt("voice_used", used + 1).apply()
            DataOutputStream(file(key).outputStream().buffered()).use { out ->
                out.writeInt(audio.second)
                out.write(audio.first)
            }
            status("Gemini sesi çalışıyor ✓ (ses: ${voiceName()}, model: $model)")
            return true
        }
        status("Gemini sesi üretilemedi — $lastError")
        return false
    }

    /**
     * Makes a clip in the background. The free Gemini voice allows only a few requests a minute,
     * so a refused request ("429") is retried a few times with a pause instead of giving up.
     */
    fun prepare(key: String, text: String, done: (Boolean) -> Unit = {}) {
        Thread { done(prepareWithRetry(key, text)) }.start()
    }

    private fun prepareWithRetry(key: String, text: String): Boolean {
        repeat(5) { attempt ->
            if (runCatching { prepareBlocking(key, text) }.getOrDefault(false)) return true
            if (lastStatus().contains("bitti")) return false
            if (!lastStatus().contains("429") && !lastStatus().contains("bağlantı")) return false
            Thread.sleep(25_000L * (attempt + 1))
        }
        return false
    }

    /** Makes the fixed lines one by one, slowly, so they do not use up the per-minute limit. */
    fun prepareFixed() {
        Thread {
            synchronized(ObiGeminiVoice::class.java) {
                FIXED.forEach { (key, text) ->
                    if (!has(key)) { prepareWithRetry(key, text); Thread.sleep(8_000L) }
                }
            }
        }.start()
    }

    private fun nextQuotaReset(): Long {
        val pacific = java.time.ZoneId.of("America/Los_Angeles")
        return java.time.ZonedDateTime.now(pacific).toLocalDate().plusDays(1).atStartOfDay(pacific)
            .plusMinutes(5).toInstant().toEpochMilli()
    }

    private fun clock(time: Long): String = java.time.Instant.ofEpochMilli(time).atZone(java.time.ZoneId.systemDefault())
        .let { "${if (it.toLocalDate() == LocalDate.now()) "bugün" else "yarın"} ${String.format(java.util.Locale.ROOT, "%02d:%02d", it.hour, it.minute)}" }

    /** Model names containing "tts" that this key may call. */
    private fun ttsModels(apiKey: String): List<String> {
        val connection = URL("https://generativelanguage.googleapis.com/v1beta/models?pageSize=200").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8000; connection.readTimeout = 15000
            connection.setRequestProperty("x-goog-api-key", apiKey)
            if (connection.responseCode !in 200..299) return emptyList()
            val models = JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).optJSONArray("models") ?: return emptyList()
            return (0 until models.length()).mapNotNull { models.optJSONObject(it)?.optString("name")?.removePrefix("models/") }
                .filter { it.contains("tts") }.sortedBy { if (it.contains("flash")) 0 else 1 }
        } finally { connection.disconnect() }
    }

    /** Returns (status, (pcm bytes, sample rate), error message). */
    private fun request(apiKey: String, model: String, text: String): Triple<Int, Pair<ByteArray, Int>?, String?> {
        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text",
                "Say in a cute, cheerful, bright little voice, with natural native Turkish pronunciation: $text")))))
            .put("generationConfig", JSONObject()
                .put("responseModalities", JSONArray().put("AUDIO"))
                .put("speechConfig", JSONObject().put("voiceConfig", JSONObject()
                    .put("prebuiltVoiceConfig", JSONObject().put("voiceName", voiceName())))))
        val connection = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 8000
            connection.readTimeout = 30000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("x-goog-api-key", apiKey)
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            if (status !in 200..299) {
                val message = runCatching {
                    JSONObject(connection.errorStream.bufferedReader().use { it.readText() }).optJSONObject("error")?.optString("message")
                }.getOrNull()?.take(120)
                return Triple(status, null, message)
            }
            val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val parts = root.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts")
            for (i in 0 until parts.length()) {
                val inline = parts.getJSONObject(i).optJSONObject("inlineData") ?: continue
                val rate = Regex("rate=(\\d+)").find(inline.optString("mimeType"))?.groupValues?.get(1)?.toIntOrNull() ?: 24000
                return Triple(status, Base64.decode(inline.getString("data"), Base64.DEFAULT) to rate, null)
            }
            return Triple(status, null, "yanıtta ses yok")
        } finally { connection.disconnect() }
    }

    /**
     * Plays a stored clip, pitched up by [pitch] at normal speed. [done] runs on the playing
     * thread when the clip ends or fails; returns false when there is nothing to play.
     */
    fun play(key: String, pitch: Float, repeat: Int = 1, done: () -> Unit): AudioTrack? {
        val file = file(key)
        if (!has(key)) return null
        val (rate, pcm) = runCatching {
            DataInputStream(file.inputStream().buffered()).use { input -> input.readInt() to input.readBytes() }
        }.getOrNull() ?: return null
        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT) * 4)
                .build()
        }.getOrNull() ?: return null
        runCatching { track.playbackParams = PlaybackParams().setPitch(pitch.coerceIn(0.8f, 2.2f)).setSpeed(1.0f) }
        Thread {
            runCatching {
                track.play()
                repeat(repeat.coerceAtLeast(1)) { round ->
                    track.write(pcm, 0, pcm.size)
                    if (round < repeat - 1) track.write(ByteArray(rate), 0, rate) // half a second of quiet
                }
                // Wait for the queued audio to drain before reporting the end.
                val frames = pcm.size / 2 * repeat + (repeat - 1) * rate / 2
                val deadline = System.currentTimeMillis() + frames * 1000L / rate + 3_000L
                while (track.playbackHeadPosition < frames - rate / 10 && System.currentTimeMillis() < deadline &&
                    track.playState == AudioTrack.PLAYSTATE_PLAYING) Thread.sleep(100)
            }
            runCatching { track.stop(); track.release() }
            done()
        }.start()
        return track
    }
}
