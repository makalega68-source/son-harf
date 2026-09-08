package com.sonharf.game.mascot

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.max
import kotlin.random.Random

enum class Mood { IDLE, HAPPY, EXCITED, ANGRY, PANIC, SAD, CRYING, WINK, TIRED }

/**
 * MASKOT YAPAY ZEKÂSI
 * Sunucuya veya harici bir modele ihtiyaç duymaz, çevrimdışı çalışır, ücretsizdir.
 * Üç katman:
 *   1) Durum motoru  : oyun olaylarını ruh haline çevirir
 *   2) Karar motoru  : konuşmaya değer mi? (sessizlik penceresi, soğuma, önem puanı)
 *   3) Cümle motoru  : ağırlıklı seçim + son 6 cümleyi tekrar etmeme
 */
object MascotBrain {

    var mood by mutableStateOf(Mood.IDLE); private set
    var bubble by mutableStateOf<String?>(null); private set
    var eyesVariant by mutableStateOf("blue")
    var language: String = "tr"

    private var lastSpeakAt = 0L
    private var lastMoodAt = 0L
    private var typingUntil = 0L
    private val recent = ArrayDeque<String>()

    private const val SPEAK_COOLDOWN = 4200L
    private const val MOOD_HOLD = 900L

    private fun now() = System.currentTimeMillis()

    // ---------- 3. CÜMLE MOTORU ----------
    private val lines: Map<String, Pair<List<String>, List<String>>> = mapOf(
        "greet" to Pair(
            listOf("Hazır mısın?", "Bugün formdasın gibi.", "Hadi bir tur atalım.", "Seni bekliyordum."),
            listOf("Ready?", "You look sharp today.", "Let's play a round.", "I was waiting for you.")
        ),
        "good" to Pair(
            listOf("Güzel kelime!", "İşte bu.", "Temiz iş.", "Aferin!", "Tam isabet."),
            listOf("Nice word!", "That's it.", "Clean.", "Well done!", "Spot on.")
        ),
        "streak" to Pair(
            listOf("Üst üste üç! Durma.", "Serin uzuyor!", "Alev aldın!", "Rakip zorlanıyor."),
            listOf("Three in a row!", "Streak is growing!", "You're on fire!", "Rival is struggling.")
        ),
        "long" to Pair(
            listOf("Ne uzun kelime ama!", "Bu kaç puan öyle?", "Kütüphane misin sen?"),
            listOf("What a long word!", "How many points was that?", "Are you a dictionary?")
        ),
        "wrong" to Pair(
            listOf("Sözlükte yok galiba.", "Başka bir tane dene.", "Olsun, devam."),
            listOf("Not in the dictionary.", "Try another one.", "It happens, keep going.")
        ),
        "hurry" to Pair(
            listOf("Süre bitiyor!", "Çabuk, çabuk!", "Bir şey yaz!"),
            listOf("Time's running out!", "Quick, quick!", "Type something!")
        ),
        "behind" to Pair(
            listOf("Geriden geliyoruz, sorun değil.", "Uzun kelime lazım bize.", "Toparlarız."),
            listOf("We're behind, no problem.", "We need a long word.", "We'll catch up.")
        ),
        "ahead" to Pair(
            listOf("Öndeyiz, sakin kal.", "Böyle devam.", "Farkı açalım."),
            listOf("We're ahead, stay calm.", "Keep it up.", "Let's widen the gap.")
        ),
        "win" to Pair(
            listOf("KAZANDIK!", "Efsanesin!", "Bunu kutlamalıyız!"),
            listOf("WE WON!", "You're a legend!", "Time to celebrate!")
        ),
        "lose" to Pair(
            listOf("Bu sefer olmadı...", "Bir dahakine.", "Yine deneriz."),
            listOf("Not this time...", "Next round.", "We'll try again.")
        ),
        "idlepoke" to Pair(
            listOf("Beni mi dürttün?", "Buradayım.", "Mırr.", "Kaşımayı bırak, oyna."),
            listOf("Did you poke me?", "I'm here.", "Purr.", "Stop petting, play.")
        )
    )

    private fun pick(key: String): String? {
        val pair = lines[key] ?: return null
        val pool = if (language == "tr") pair.first else pair.second
        val fresh = pool.filterNot { recent.contains(it) }
        val chosen = (if (fresh.isNotEmpty()) fresh else pool).random()
        recent.addLast(chosen)
        while (recent.size > 6) recent.removeFirst()
        return chosen
    }

    // ---------- 2. KARAR MOTORU ----------
    /** importance: 0 = önemsiz, 3 = kritik. Kritik olanlar soğumayı deler. */
    private fun say(key: String, importance: Int = 1) {
        val t = now()
        if (importance < 3 && t < typingUntil) return
        if (importance < 2 && t - lastSpeakAt < SPEAK_COOLDOWN) return
        bubble = pick(key) ?: return
        lastSpeakAt = t
    }

    private fun setMood(m: Mood, force: Boolean = false) {
        val t = now()
        if (!force && t - lastMoodAt < MOOD_HOLD && mood != Mood.IDLE) return
        mood = m; lastMoodAt = t
    }

    /** Oyuncu tuşa bastığında maskot 1.4 sn susar; dikkat dağıtmaz. */
    fun onPlayerTyping() { typingUntil = now() + 1400 }

    fun clearBubble() { bubble = null }

    // ---------- 1. DURUM MOTORU ----------
    fun onLobby() { setMood(Mood.WINK, true); say("greet", 2) }

    fun onMatchStart() { setMood(Mood.IDLE, true); bubble = null; recent.clear() }

    fun onCorrectWord(length: Int, streak: Int, points: Int) {
        when {
            streak >= 3 -> { setMood(Mood.EXCITED, true); say("streak", 2) }
            length >= 7 -> { setMood(Mood.EXCITED, true); say("long", 2) }
            else -> { setMood(Mood.HAPPY, true); say("good", 1) }
        }
    }

    fun onWrongWord() { setMood(Mood.SAD, true); say("wrong", 1) }

    fun onTick(secondsLeft: Int, myScore: Int, rivalScore: Int) {
        when {
            secondsLeft in 1..4 -> { setMood(Mood.PANIC, true); say("hurry", 3) }
            secondsLeft in 5..8 -> setMood(Mood.ANGRY)
            secondsLeft == 12 && myScore + 30 < rivalScore -> say("behind", 1)
            secondsLeft == 12 && myScore > rivalScore + 30 -> say("ahead", 1)
            secondsLeft > 12 && mood != Mood.IDLE && now() - lastMoodAt > 2600 -> setMood(Mood.IDLE)
        }
    }

    fun onTurnLost() { setMood(Mood.TIRED, true) }

    fun onVictory() { setMood(Mood.EXCITED, true); bubble = pick("win") }

    fun onDefeat() { setMood(Mood.CRYING, true); bubble = pick("lose") }

    fun onPoke() {
        setMood(if (Random.nextBoolean()) Mood.WINK else Mood.HAPPY, true)
        bubble = pick("idlepoke"); lastSpeakAt = now()
    }

    /** Zafer uçuşu için rastgele ama tekrarsız neşeli ifade dizisi. */
    fun celebrationSequence(): List<Mood> =
        listOf(Mood.EXCITED, Mood.HAPPY, Mood.WINK, Mood.EXCITED, Mood.HAPPY)

    fun idleBlinkAllowed(): Boolean = mood == Mood.IDLE && now() - lastMoodAt > 1500

    fun heat(): Float = when (mood) {
        Mood.EXCITED -> 1f; Mood.PANIC -> .9f; Mood.HAPPY -> .6f
        Mood.ANGRY -> .5f; Mood.CRYING -> .2f; else -> 0f
    }

    fun pressure(secondsLeft: Int): Float = max(0f, (10 - secondsLeft) / 10f)
}
