package com.sonharf.game

/**
 * Mascot speech: every line leaves the mascot in its own voice instead of a human's. Each
 * character has a signature cry and closing sound, and a kaomoji that matches the line's mood,
 * like a chibi anime sidekick. Grammar of the line itself is never touched.
 */
internal object MascotVoice {
    private enum class Tone { HAPPY, SAD, CURIOUS, CALM }

    private class Voice(val cries: List<String>, val tails: List<String>)

    private val trVoices = mapOf(
        WordSiegeMascotSkin.ORB to Voice(listOf("Obi-obi!", "Pıt pıt!", "Vuuu~"), listOf("~obi!", "~pıt!", "✧")),
        WordSiegeMascotSkin.PINK to Voice(listOf("Pii~!", "Kyaa~!", "Pinki-pinki!"), listOf("~pii ♡", "♡", "~kya!")),
        WordSiegeMascotSkin.DEVIL_BLUE to Voice(listOf("Brrr!", "Buzi-buz!", "Çıt çıt!"), listOf("~brrr ❄", "~buz!", "❄")),
        WordSiegeMascotSkin.DEVIL_RED to Voice(listOf("Hehe~!", "Zıp zıp!", "Fışşş!"), listOf("~zıp!", "🔥", "~hehe")),
        WordSiegeMascotSkin.CAT to Voice(listOf("Miyav~!", "Mırrr~", "Nyaa!"), listOf("~miyav", "~nya", "🐾")),
        WordSiegeMascotSkin.ROBOT to Voice(listOf("Bip-bop!", "Bzzt!", "Dıt-dıt!"), listOf("~bip!", "[bop]", "~bzzt")),
        WordSiegeMascotSkin.ASTRONAUT to Voice(listOf("Vuuşş~!", "Nova-nova!", "Bip bip, dünya!"), listOf("✦", "~vuuş", "☄")),
    )
    private val enVoices = mapOf(
        WordSiegeMascotSkin.ORB to Voice(listOf("Obi-obi!", "Pip pip!", "Vooo~"), listOf("~obi!", "~pip!", "✧")),
        WordSiegeMascotSkin.PINK to Voice(listOf("Pii~!", "Kyaa~!", "Pinky-pinky!"), listOf("~pii ♡", "♡", "~kya!")),
        WordSiegeMascotSkin.DEVIL_BLUE to Voice(listOf("Brrr!", "Frost-frost!", "Crick!"), listOf("~brrr ❄", "~frost!", "❄")),
        WordSiegeMascotSkin.DEVIL_RED to Voice(listOf("Hehe~!", "Zip zap!", "Fsssh!"), listOf("~zap!", "🔥", "~hehe")),
        WordSiegeMascotSkin.CAT to Voice(listOf("Meow~!", "Purrr~", "Nyaa!"), listOf("~meow", "~nya", "🐾")),
        WordSiegeMascotSkin.ROBOT to Voice(listOf("Beep-boop!", "Bzzt!", "Dit-dit!"), listOf("~beep!", "[boop]", "~bzzt")),
        WordSiegeMascotSkin.ASTRONAUT to Voice(listOf("Whoosh~!", "Nova-nova!", "Beep beep, Earth!"), listOf("✦", "~whoosh", "☄")),
    )

    private val kaomoji = mapOf(
        Tone.HAPPY to listOf("(≧▽≦)", "٩(◕‿◕)۶", "(ﾉ◕ヮ◕)ﾉ", "(*^▽^*)", "ヽ(>∀<☆)ノ"),
        Tone.SAD to listOf("(｡•́︿•̀｡)", "(ᗒᗣᗕ)", "(╥﹏╥)", "(っ˘̩╭╮˘̩)っ"),
        Tone.CURIOUS to listOf("(・・?)", "(｀・ω・´)", "(°ロ°)", "(¬‿¬)"),
        Tone.CALM to listOf("(◕‿◕)", "(´｡• ᵕ •｡`)", "(˘▾˘)", "(＾▽＾)"),
    )

    private val sadWords = listOf("üzül", "olsun", "kaybet", "maalesef", "yazık", "sorry", "lost", "next time", "sıkıl", "zor")
    private val happyWords = listOf("harika", "süper", "bravo", "kazan", "müthiş", "yaşasın", "great", "awesome", "win", "yes")

    private fun tone(text: String): Tone {
        val lower = text.lowercase()
        return when {
            sadWords.any { it in lower } -> Tone.SAD
            happyWords.any { it in lower } || text.count { it == '!' } >= 1 -> Tone.HAPPY
            '?' in text || '_' in text -> Tone.CURIOUS
            else -> Tone.CALM
        }
    }

    /** Dresses [text] in the mascot's voice. [seed] varies the choice between lines. */
    fun style(text: String, skin: WordSiegeMascotSkin, seed: Int): String {
        val clean = text.trim()
        if (clean.isEmpty()) return clean
        val voice = (if (SonHarfUiState.isEnglish) enVoices else trVoices)[skin] ?: return clean
        val t = tone(clean)
        val face = kaomoji.getValue(t).let { it[Math.floorMod(seed * 7 + clean.length, it.size)] }
        // Hints and patterns ("KA _ _ _") stay readable: only a closing sound, no leading cry.
        val hint = '_' in clean
        return when {
            hint -> "$clean ${voice.tails[Math.floorMod(seed, voice.tails.size)]}"
            t == Tone.SAD -> "$clean $face"
            Math.floorMod(seed, 3) == 0 -> "${voice.cries[Math.floorMod(seed / 3, voice.cries.size)]} $clean $face"
            else -> "$clean ${voice.tails[Math.floorMod(seed, voice.tails.size)]} $face"
        }
    }
}
