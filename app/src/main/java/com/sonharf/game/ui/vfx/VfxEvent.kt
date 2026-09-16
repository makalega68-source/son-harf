package com.sonharf.game.ui.vfx

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.sonharf.game.SonHarfTheme

/**
 * All in-game "moment" events the VFX layer can play (G3.0 spec).
 *
 * Payload rule: events include only what the layer needs to draw
 * (screen anchor, count, tint, magnitude label). They must NEVER
 * carry gameplay-critical state. The game logic and server calls
 * proceed independently — VFX must NEVER block.
 */
sealed class VfxEvent {
    /** Small twinkle when a letter is placed. */
    data class LetterDrop(val anchor: Offset) : VfxEvent()

    /** Correct word accepted: sweep across the tiles + score bubble. */
    data class WordAccepted(val score: Int, val anchor: Offset, val tint: Color) : VfxEvent()

    /** Cells captured (Kuşatma): sequential ring + rim glow on each anchor. */
    data class CellCaptured(val anchors: List<Offset>, val tint: Color = SonHarfTheme.KusatmaPurple) : VfxEvent()

    /** Big siege combo: multi-cell + shockwave + score. */
    data class BigSiege(val score: Int, val anchors: List<Offset>) : VfxEvent()

    /** Critical / castle zone flare. */
    data class CriticalZone(val anchor: Offset) : VfxEvent()

    /** Perfect move: sparkles + label. */
    data class PerfectMove(val score: Int, val anchor: Offset) : VfxEvent()

    /** Match victory: gold burst + confetti. */
    data class Victory(val anchor: Offset) : VfxEvent()

    /** Match defeat: soft gray-purple radial. */
    data class Defeat(val anchor: Offset) : VfxEvent()

    /** Streak N (Son Harf 3/5/10). */
    data class Streak(val n: Int, val anchor: Offset) : VfxEvent()

    /** Time pressure: pulse the timer ring. */
    data object LastSeconds : VfxEvent()

    /** Opponent made a mistake — flare on their card. */
    data class OpponentError(val anchor: Offset) : VfxEvent()

    /** Won a round in a series. */
    data class RoundWin(val anchor: Offset) : VfxEvent()

    /** LetterLadder: step advanced. */
    data class PathStep(val anchor: Offset) : VfxEvent()

    /** LetterLadder: reached a special node. */
    data class SpecialNode(val anchor: Offset) : VfxEvent()

    /** Reward chest reveal. */
    data class Reward(val anchor: Offset) : VfxEvent()

    /** Path completed. */
    data class PathComplete(val anchor: Offset) : VfxEvent()

    /** Level up. */
    data class LevelUp(val level: Int, val anchor: Offset) : VfxEvent()

    /** Diamonds gained (fly toward counter). */
    data class DiamondGain(val amount: Int, val from: Offset, val to: Offset) : VfxEvent()

    /** Combo chain across cells. */
    data class Combo(val anchors: List<Offset>) : VfxEvent()

    /** Castle fell (Kuşatma). */
    data class CastleFall(val anchor: Offset) : VfxEvent()

    /** Shield block. */
    data class ShieldBlock(val anchor: Offset) : VfxEvent()

    /** Map-percent wave. */
    data class MapWave(val anchor: Offset) : VfxEvent()

    /** Last-letter bridges across to opponent card. */
    data class LetterBridge(val from: Offset, val to: Offset, val glyph: Char) : VfxEvent()

    /** Long word (8+). */
    data class LongWord(val letters: List<Offset>) : VfxEvent()

    /** Correct answer <= 2s before timeout (Son Harf). */
    data class LastSecondSave(val anchor: Offset) : VfxEvent()

    /** LetterLadder: unlock icebreak on a locked stage. */
    data class UnlockLevel(val anchor: Offset) : VfxEvent()

    /** Hint reveal cell parlar (Kelime Yolu). */
    data class HintReveal(val anchor: Offset) : VfxEvent()
}
