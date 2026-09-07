package com.sonharf.game

internal data class MageCatRuntimeState(
    val cue: MageCatCue? = null,
    val placement: MageCatPlacement = MageCatPlacementPolicy.forScreen(MageCatScreen.HOME),
)

/**
 * Ekranlardan gelen oyun olaylarını tek noktada maskot davranışına dönüştürür.
 * Renderer katmanından bağımsızdır; bu sayede gerçek Mage Cat görseli eklenene
 * kadar davranış ve yerleşim mantığı ayrı test edilebilir.
 */
internal class MageCatRuntime(
    private val director: MageCatDirector = MageCatDirector(),
) {
    private var screen: MageCatScreen = MageCatScreen.HOME

    fun onScreenChanged(newScreen: MageCatScreen): MageCatRuntimeState {
        screen = newScreen
        return MageCatRuntimeState(
            cue = null,
            placement = MageCatPlacementPolicy.forScreen(newScreen),
        )
    }

    fun onEvent(
        event: MageCatEvent,
        nowMs: Long,
        blockingUiVisible: Boolean = false,
        playerInputActive: Boolean = false,
        matchFinished: Boolean = false,
    ): MageCatRuntimeState {
        val context = MageCatContext(
            nowMs = nowMs,
            screen = screen,
            blockingUiVisible = blockingUiVisible,
            playerInputActive = playerInputActive,
            matchFinished = matchFinished,
        )
        return MageCatRuntimeState(
            cue = director.cue(event, context),
            placement = MageCatPlacementPolicy.forScreen(screen),
        )
    }
}
