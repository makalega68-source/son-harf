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
        private const val VOICE = "Leda"
        private const val DAILY_LIMIT = 40

        /** Fixed lines, made once and reused. */
        val FIXED = mapOf(
            "preview" to "Merhaba! Ben Obi. Sesim böyle, beğendin mi?",
            "alarm_morning" to "Günaydın! Sabah oldu, hadi uyan bakalım. Yeni bir gün seni bekliyor!",
            "alarm" to "Alarm zamanı geldi! Hadi bakalım, kalkıyoruz!",
            "reminder" to "Hatırlatma zamanı geldi!",
        )

        fun reminderLine(text: String) = "Hatırlatma var: ${text.trimEnd('.', '!')}."
    }

    private val prefs = context.getSharedPreferences("obi_gemini", Context.MODE_PRIVATE)
    private val dir = File(context.filesDir, "voice").apply { mkdirs() }

    fun file(key: String) = File(dir, "$key.pcm")
    fun has(key: String) = file(key).length() > 1000
    fun forget(key: String) { file(key).delete() }

    /** Makes the clip if it is missing. Blocking; call from a background thread. */
    fun prepareBlocking(key: String, text: String): Boolean {
        if (has(key)) return true
        val apiKey = ObiGemini(context).apiKey() ?: return false
        val today = LocalDate.now().toString()
        val used = if (prefs.getString("voice_day", "") == today) prefs.getInt("voice_used", 0) else 0
        if (used >= DAILY_LIMIT) return false
        val preferred = prefs.getString("voice_model", null)
        for (model in listOfNotNull(preferred) + MODELS.filter { it != preferred }) {
            val result = runCatching { request(apiKey, model, text) }.getOrNull() ?: continue
            if (result.first == 404 || result.first == 400) continue
            val audio = result.second ?: return false
            prefs.edit().putString("voice_model", model).putString("voice_day", today).putInt("voice_used", used + 1).apply()
            DataOutputStream(file(key).outputStream().buffered()).use { out ->
                out.writeInt(audio.second)
                out.write(audio.first)
            }
            return true
        }
        return false
    }

    fun prepare(key: String, text: String, done: (Boolean) -> Unit = {}) {
        Thread { val ok = runCatching { prepareBlocking(key, text) }.getOrDefault(false); done(ok) }.start()
    }

    /** Makes the fixed lines in the background, once a key is connected. */
    fun prepareFixed() {
        Thread { FIXED.forEach { (key, text) -> runCatching { prepareBlocking(key, text) } } }.start()
    }

    /** Returns (status, (pcm bytes, sample rate)). */
    private fun request(apiKey: String, model: String, text: String): Pair<Int, Pair<ByteArray, Int>?> {
        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text",
                "Minik, tatlı, neşeli ve sevimli bir sesle, akıcı ve doğal Türkçe telaffuzla söyle: $text")))))
            .put("generationConfig", JSONObject()
                .put("responseModalities", JSONArray().put("AUDIO"))
                .put("speechConfig", JSONObject().put("voiceConfig", JSONObject()
                    .put("prebuiltVoiceConfig", JSONObject().put("voiceName", VOICE)))))
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
            if (status !in 200..299) return status to null
            val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val parts = root.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts")
            for (i in 0 until parts.length()) {
                val inline = parts.getJSONObject(i).optJSONObject("inlineData") ?: continue
                val rate = Regex("rate=(\\d+)").find(inline.optString("mimeType"))?.groupValues?.get(1)?.toIntOrNull() ?: 24000
                return status to (Base64.decode(inline.getString("data"), Base64.DEFAULT) to rate)
            }
            return status to null
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
