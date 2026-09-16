package com.sonharf.game.ui.vfx

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex
import com.sonharf.game.data.WordSiegeVfxMoveRegistry
import com.sonharf.game.ui.premium.rememberReducedMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/**
 * Controller shared between the game code and the visual VFX layer.
 * Game code calls [play] with a VfxEvent; the layer collects events
 * on-screen. The game never waits for the effect to finish.
 */
class VfxController {
    // Bounded: back-pressure by dropping the oldest silent events keeps VFX
    // from stacking during rapid-fire chains. Server / game state is unaffected.
    private val _events = MutableSharedFlow<VfxEvent>(
        extraBufferCapacity = 16,
        replay = 0,
    )
    val events = _events

    fun play(event: VfxEvent) {
        _events.tryEmit(event)
    }
}

/**
 * Composition local for the VFX controller.
 *
 * Defaults to a stand-alone controller that no VfxLayer collects from, so
 * calls to `play(...)` are safe (drop into the void) when a screen renders
 * outside a VfxLayerHost — for example in previews or unit-test hosts.
 * MainActivity wraps the real host below the theme, so live app code always
 * lands on the collected controller.
 */
val LocalVfx = compositionLocalOf<VfxController> { VfxController() }

/**
 * Host that provides a VfxController and stacks the VfxLayer above content.
 * Wrap the app root (below the theme) with this once.
 */
@Composable
fun VfxLayerHost(content: @Composable () -> Unit) {
    val controller = remember { VfxController() }
    val reducedMotion = rememberReducedMotion()
    Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalVfx provides controller) {
            content()
            WordSiegeComboVfxBridge()
        }
        VfxLayer(
            controller = controller,
            reducedMotion = reducedMotion,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10000f),
        )
    }
}

/**
 * Converts fresh authoritative multi-word Word Siege moves into a cosmetic Combo event.
 * The registry suppresses historical moves when a match is first opened, so this is one-shot.
 */
@Composable
private fun WordSiegeComboVfxBridge() {
    val vfx = LocalVfx.current
    var hostSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(vfx) {
        WordSiegeVfxMoveRegistry.comboEvents.collect {
            if (hostSize.width > 0 && hostSize.height > 0) {
                val center = Offset(hostSize.width / 2f, hostSize.height / 2f)
                vfx.play(VfxEvent.Combo(listOf(center)))
            }
        }
    }

    Box(Modifier.fillMaxSize().onSizeChanged { hostSize = it })
}

/**
 * The visual layer itself. Renders active effects and consumes events
 * from the controller. Enforces a soft cap of 3 concurrent large effects
 * per G3.0 rules.
 */
@Composable
fun VfxLayer(
    controller: VfxController,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
) {
    val active = remember { mutableStateListOf<ActiveVfx>() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(controller) {
        controller.events.collect { event ->
            // Soft cap: never queue more than 3 concurrent effects.
            if (active.size >= 3) {
                // Drop the oldest big effect to make room.
                active.removeAt(0)
            }
            val effect = ActiveVfx(event = event, startedAt = System.nanoTime())
            active += effect
            scope.launch {
                delay(effect.durationMs.toLong())
                active.remove(effect)
            }
            // Trigger haptics + reduced-motion short-flash policy.
            playHaptics(context, event)
        }
    }

    Box(modifier = modifier) {
        active.forEach { effect ->
            if (reducedMotion) {
                ReducedMotionFlash(effect)
            } else {
                VfxRenderer(effect)
            }
        }
    }
}

/**
 * Metadata for one live effect on screen.
 */
internal data class ActiveVfx(
    val event: VfxEvent,
    val startedAt: Long,
) {
    /**
     * Total on-screen duration; kept short (G3.0: en fazla ~1,5 sn büyük efektler).
     */
    val durationMs: Int
        get() = when (event) {
            is VfxEvent.LetterDrop -> 250
            is VfxEvent.WordAccepted -> 500
            is VfxEvent.CellCaptured -> 500 + (event.anchors.size * 60)
            is VfxEvent.BigSiege -> 1500
            is VfxEvent.CriticalZone -> 700
            is VfxEvent.PerfectMove -> 900
            is VfxEvent.Victory -> 2200
            is VfxEvent.Defeat -> 900
            is VfxEvent.Streak -> if (event.n >= 10) 1200 else 700
            VfxEvent.LastSeconds -> 300
            is VfxEvent.OpponentError -> 500
            is VfxEvent.RoundWin -> 1200
            is VfxEvent.PathStep -> 350
            is VfxEvent.SpecialNode -> 900
            is VfxEvent.Reward -> 1200
            is VfxEvent.PathComplete -> 1600
            is VfxEvent.LevelUp -> 1200
            is VfxEvent.DiamondGain -> 800
            is VfxEvent.Combo -> 400 + (event.anchors.size * 60)
            is VfxEvent.CastleFall -> 1000
            is VfxEvent.ShieldBlock -> 500
            is VfxEvent.MapWave -> 900
            is VfxEvent.LetterBridge -> 350
            is VfxEvent.LongWord -> 200 + (event.letters.size * 60)
            is VfxEvent.LastSecondSave -> 700
            is VfxEvent.UnlockLevel -> 600
            is VfxEvent.HintReveal -> 500
        }
}

@Composable
internal fun VfxRenderer(effect: ActiveVfx) {
    // Concrete draw code intentionally minimal in this MVP: each event type
    // renders a short-lived, non-blocking flash using the pre-selected VFX
    // sprite. Detailed choreographies (ring expansion, sheet playback) can
    // be added incrementally by expanding this when-block; the VfxEvent
    // contract stays stable.
    val painter = painterResource(id = pickSpriteFor(effect.event))
    val tint = pickTintFor(effect.event)
    val (center, sizeDp) = pickAnchorAndSize(effect.event)
    VfxSprite(
        painter = painter,
        center = center,
        sizeDp = sizeDp,
        tint = tint,
        alpha = 0.9f,
    )
}

@Composable
internal fun ReducedMotionFlash(effect: ActiveVfx) {
    // With reduced motion, we still show a short flash so gameplay feedback is
    // preserved without full particle systems (G3.0: sadece kısa flaş + yazı).
    val painter = painterResource(id = VfxAssets.Flash)
    val (center, sizeDp) = pickAnchorAndSize(effect.event)
    VfxSprite(
        painter = painter,
        center = center,
        sizeDp = sizeDp * 0.6f,
        alpha = 0.7f,
    )
}
