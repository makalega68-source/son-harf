package com.sonharf.game

/**
 * Runtime contract for the Kelime Kuşatması mascot.
 *
 * This is intentionally renderer-agnostic: game events go in, realtime mascot directives come out.
 * A future AI implementation can replace [RuleBasedMascotBrain] without changing the renderer.
 */
internal interface MascotBrain {
    fun react(event: MascotEvent): MascotDirective
}

internal sealed interface MascotEvent {
    data class LetterFocused(val column: Int, val row: Int, val letter: Char) : MascotEvent
    data class WordChanged(val word: String) : MascotEvent
    data class MoveAccepted(val word: String, val score: Int) : MascotEvent
    data class MoveRejected(val word: String) : MascotEvent
    data object OpponentThreat : MascotEvent
    data object Listening : MascotEvent
}

internal enum class MascotEmotion {
    CALM,
    FOCUSED,
    THINKING,
    HAPPY,
    SURPRISED,
    CONCERNED,
    PROUD,
}

internal data class MascotDirective(
    val emotion: MascotEmotion,
    val gazeX: Float = 0f,
    val gazeY: Float = 0.65f,
    val shouldSpeak: Boolean = false,
    val utterance: String? = null,
)

internal class RuleBasedMascotBrain : MascotBrain {
    override fun react(event: MascotEvent): MascotDirective = when (event) {
        is MascotEvent.LetterFocused -> MascotDirective(
            emotion = MascotEmotion.FOCUSED,
            gazeX = ((event.column - 2) / 2f).coerceIn(-1f, 1f),
            gazeY = (0.35f + event.row * 0.14f).coerceIn(0.35f, 0.95f),
        )
        is MascotEvent.WordChanged -> MascotDirective(
            emotion = if (event.word.length >= 6) MascotEmotion.THINKING else MascotEmotion.FOCUSED,
            gazeY = 0.88f,
        )
        is MascotEvent.MoveAccepted -> MascotDirective(
            emotion = if (event.score >= 20) MascotEmotion.PROUD else MascotEmotion.HAPPY,
            gazeY = 0.82f,
        )
        is MascotEvent.MoveRejected -> MascotDirective(
            emotion = MascotEmotion.CONCERNED,
            gazeY = 0.86f,
        )
        MascotEvent.OpponentThreat -> MascotDirective(
            emotion = MascotEmotion.SURPRISED,
            gazeX = 0.45f,
            gazeY = 0.72f,
        )
        MascotEvent.Listening -> MascotDirective(
            emotion = MascotEmotion.CALM,
            gazeY = 0.72f,
        )
    }
}
