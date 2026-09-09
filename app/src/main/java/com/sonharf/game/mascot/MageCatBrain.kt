package com.sonharf.game.mascot

/** Offline contextual behaviour AI. Read-only; no game, network or economy dependencies. */
data class CompanionSnapshot(
    val matchId: String,
    val myScore: Int,
    val rivalScore: Int,
    val myTurn: Boolean,
    val secondsLeft: Int? = null,
    val streak: Int = 0,
    val finished: Boolean = false,
    val won: Boolean = false,
    val draw: Boolean = false,
    val territory: Int? = null,
    val rejectedWords: Int = 0,
    val ownFailureKey: String? = null,
)

data class CompanionReaction(val mood: MageCatMood, val tr: String, val en: String, val sequence: Long = 0)

class MageCatBrain {
    private var previous: CompanionSnapshot? = null
    private var active = CompanionReaction(MageCatMood.IDLE, "Yanındayım.", "I'm with you.")
    private var until = 0L
    private var sequence = 0L
    private var lastUrgentTurn = false
    private var praiseIndex = 0

    fun observe(next: CompanionSnapshot, now: Long): CompanionReaction {
        val old = previous?.takeIf { it.matchId == next.matchId }
        if (old == null) { until = 0; lastUrgentTurn = false }
        previous = next
        fun react(mood: MageCatMood, tr: String, en: String, duration: Long = 2600): CompanionReaction {
            until = now + duration
            active = CompanionReaction(mood, tr, en, ++sequence)
            return active
        }
        // Terminal state always wins over a score increase or an expired timer.
        if (next.finished) {
            if (old?.finished != true) return when {
                next.draw -> react(MageCatMood.HAPPY, "Denk rakipler! Rövanş?", "Well matched! Rematch?", 5000)
                next.won -> react(MageCatMood.EXCITED, "Zafer senin! Harika oynadın.", "Victory! Beautifully played.", 5000)
                else -> react(MageCatMood.CRYING, "Güzel mücadele. Yeniden deneyelim!", "Good effort. Let's try again!", 5000)
            }
            return active
        }
        if (!next.myTurn || (next.secondsLeft ?: 99) > 8) lastUrgentTurn = false
        if (old != null && next.myScore > old.myScore) {
            val comeback = old.myScore <= old.rivalScore && next.myScore > next.rivalScore
            return when {
                comeback -> react(MageCatMood.EXCITED, "Öne geçtin!", "You took the lead!")
                next.streak >= 3 -> react(MageCatMood.EXCITED, "${next.streak} kelimelik seri!", "${next.streak}-word streak!")
                else -> {
                    val tr = listOf("Güzel kelime!", "İşte bu!", "Çok iyi gidiyorsun!", "Bir adım daha!")
                    val en = listOf("Nice word!", "That's it!", "You're doing great!", "One step closer!")
                    val index = praiseIndex++ % tr.size
                    react(MageCatMood.HAPPY, tr[index], en[index])
                }
            }
        }
        if (old != null && (next.rejectedWords > old.rejectedWords ||
            (next.ownFailureKey != null && next.ownFailureKey != old.ownFailureKey))) {
            return react(MageCatMood.SAD, "Başka bir kelime deneyelim.", "Let's try another word.")
        }
        if (old?.territory != null && next.territory != null && next.territory < old.territory) {
            return react(MageCatMood.SAD, "Alanı geri alabilirsin.", "You can reclaim that territory.")
        }
        if (next.myTurn && next.secondsLeft != null && next.secondsLeft in 1..4 && !lastUrgentTurn) {
            lastUrgentTurn = true
            return react(MageCatMood.PANIC, "Son saniyeler, odaklan!", "Last seconds, focus!", 1600)
        }
        if (now < until && old != null) return active
        val mood = if (next.myTurn) MageCatMood.ANGRY else MageCatMood.IDLE
        val tr = if (next.myTurn) "Sıra sende. Hazırsın!" else "Rakibini izleyelim."
        val en = if (next.myTurn) "Your turn. You've got this!" else "Let's watch your rival."
        if (active.mood != mood || active.tr != tr) active = CompanionReaction(mood, tr, en, ++sequence)
        return active
    }
}
