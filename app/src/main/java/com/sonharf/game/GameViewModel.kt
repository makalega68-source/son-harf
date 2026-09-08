package com.sonharf.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sonharf.game.mascot.MascotBrain
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Screen { LOBBY, GAME, RESULT, STORE, CREDITS }

class GameViewModel : ViewModel() {

    var screen by mutableStateOf(Screen.LOBBY)
    var profile by mutableStateOf(Profile())
    var catalog by mutableStateOf<List<StoreItem>>(emptyList())
    var owned by mutableStateOf<Set<String>>(emptySet())
    var loading by mutableStateOf(true)
    var toast by mutableStateOf<String?>(null)

    var requiredLetter by mutableStateOf('K')
    var input by mutableStateOf("")
    var myScore by mutableIntStateOf(0)
    var rivalScore by mutableIntStateOf(0)
    var streak by mutableIntStateOf(0)
    var bestStreak by mutableIntStateOf(0)
    var wordsPlayed by mutableIntStateOf(0)
    var secondsLeft by mutableIntStateOf(Config.TURN_SECONDS)
    var turnsLeft by mutableIntStateOf(Config.MATCH_TURNS)
    var won by mutableStateOf(false)
    var lastGain by mutableStateOf(Pair(0, 0))
    var doubleClaimed by mutableStateOf(false)

    private var timer: Job? = null
    private val used = mutableSetOf<String>()

    val lang: String get() = profile.language

    init {
        viewModelScope.launch {
            if (Supa.signInAnonymously()) {
                Supa.ensureProfile()?.let { profile = it }
                MascotBrain.eyesVariant = profile.eyes
                MascotBrain.language = profile.language
                catalog = Supa.catalog()
                owned = Supa.ownedIds()
                listOf("tr", "en").forEach { l ->
                    val w = Supa.words(l)
                    if (w.isNotEmpty()) DictionaryEngine.load(l, w)
                }
            }
            loading = false
            MascotBrain.onLobby()
        }
    }

    fun setLanguage(l: String) {
        profile = profile.copy(language = l); MascotBrain.language = l
    }

    fun startMatch() {
        myScore = 0; rivalScore = 0; streak = 0; bestStreak = 0; wordsPlayed = 0
        turnsLeft = Config.MATCH_TURNS; input = ""; used.clear(); doubleClaimed = false
        requiredLetter = randomStartLetter()
        MascotBrain.onMatchStart()
        screen = Screen.GAME
        runTimer()
    }

    private fun randomStartLetter(): Char {
        val pool = if (lang == "tr") "KMAELTNRS" else "AETRSLNOK"
        return pool.random()
    }

    private fun runTimer() {
        timer?.cancel()
        secondsLeft = Config.TURN_SECONDS
        timer = viewModelScope.launch {
            while (secondsLeft > 0) {
                delay(1000)
                secondsLeft--
                MascotBrain.onTick(secondsLeft, myScore, rivalScore)
            }
            onTurnTimeout()
        }
    }

    fun onKey(c: Char) {
        MascotBrain.onPlayerTyping()
        if (input.isEmpty() && !c.equals(requiredLetter, true)) return
        if (input.length >= 12) return
        input += c.uppercaseChar()
    }

    fun onBackspace() { MascotBrain.onPlayerTyping(); if (input.isNotEmpty()) input = input.dropLast(1) }

    fun onSubmit() {
        val w = DictionaryEngine.normalize(input, lang)
        if (w.length < 2 || !w.first().equals(requiredLetter, true)) { MascotBrain.onWrongWord(); return }
        if (used.contains(w)) { MascotBrain.onWrongWord(); toast = if (lang == "tr") "Bu kelime kullanıldı" else "Already used"; return }
        if (!DictionaryEngine.isValidWord(w, lang)) { MascotBrain.onWrongWord(); return }

        used.add(w)
        val pts = DictionaryEngine.calculatePoints(w, lang) + (secondsLeft / 4)
        myScore += pts
        streak += 1
        if (streak > bestStreak) bestStreak = streak
        wordsPlayed += 1
        MascotBrain.onCorrectWord(w.length, streak, pts)
        requiredLetter = w.last()
        input = ""
        nextTurn(rivalPlays = true)
    }

    private fun onTurnTimeout() {
        streak = 0
        MascotBrain.onTurnLost()
        input = ""
        nextTurn(rivalPlays = true)
    }

    /** Rakip bot: sözlükten geçerli bir kelime bulur, seviyesine göre bazen ıskalar. */
    private fun nextTurn(rivalPlays: Boolean) {
        turnsLeft -= 1
        if (turnsLeft <= 0) { finishMatch(); return }

        if (rivalPlays) {
            val botWord = DictionaryEngine.anyWordStartingWith(requiredLetter, lang)
            val botSkill = 0.55 + (profile.level.coerceAtMost(10) * 0.02)
            if (botWord != null && Math.random() < botSkill) {
                rivalScore += DictionaryEngine.calculatePoints(botWord, lang)
                requiredLetter = botWord.last()
            }
        }
        runTimer()
    }

    private fun finishMatch() {
        timer?.cancel()
        won = myScore > rivalScore
        if (won) MascotBrain.onVictory() else MascotBrain.onDefeat()
        screen = Screen.RESULT
        pushResult(false)
    }

    private fun pushResult(dbl: Boolean) {
        viewModelScope.launch {
            val before = profile
            Supa.submitMatch(myScore, rivalScore, wordsPlayed, bestStreak, lang, dbl)?.let {
                lastGain = Pair(it.xp - before.xp, it.coins - before.coins)
                profile = it
            }
        }
    }

    fun claimDoubleReward() {
        if (doubleClaimed) return
        doubleClaimed = true
        viewModelScope.launch {
            Supa.claimRewarded("coins")?.let { profile = it }
            toast = if (lang == "tr") "Ödül iki katına çıktı!" else "Reward doubled!"
        }
    }

    fun buy(item: StoreItem) {
        viewModelScope.launch {
            val p = Supa.buy(item.id)
            if (p == null) {
                toast = if (lang == "tr") "Satın alınamadı" else "Purchase failed"
            } else {
                profile = p; owned = Supa.ownedIds()
                toast = if (lang == "tr") "Alındı!" else "Purchased!"
            }
        }
    }

    fun equip(item: StoreItem) {
        viewModelScope.launch {
            Supa.equip(item.id)?.let {
                profile = it
                MascotBrain.eyesVariant = it.eyes
                toast = if (lang == "tr") "Kuşanıldı" else "Equipped"
            }
        }
    }

    fun grantPro(days: Int) {
        viewModelScope.launch { Supa.activatePro(days)?.let { profile = it } }
    }

    fun backToLobby() { timer?.cancel(); screen = Screen.LOBBY; MascotBrain.onLobby() }

    override fun onCleared() { timer?.cancel(); super.onCleared() }
}
