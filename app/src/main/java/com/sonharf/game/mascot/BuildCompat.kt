package com.sonharf.game.mascot

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import app.rive.runtime.kotlin.core.Fit

/** Build-only compatibility layer. Original supplied source files remain unchanged. */
@Composable
fun BoxScope.AnimatedVisibility(
    visible: Boolean,
    enter: EnterTransition,
    exit: ExitTransition,
    content: @Composable () -> Unit
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = enter,
        exit = exit
    ) { content() }
}

/** Adapts the supplied Rive calls to the Rive 9.x Android API without editing RiveMascotView.kt. */
class RiveAnimationView(context: Context) : FrameLayout(context) {
    private val delegate = app.rive.runtime.kotlin.RiveAnimationView(context).also {
        addView(it, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun setRiveResourceFromAsset(
        context: Context,
        asset: String,
        fit: Fit = Fit.CONTAIN,
        stateMachineName: String? = null
    ) {
        val bytes = context.assets.open(asset).use { it.readBytes() }
        delegate.setRiveBytes(bytes, fit = fit, stateMachineName = stateMachineName)
    }

    fun setNumberState(stateMachineName: String, inputName: String, value: Float) {
        delegate.setNumberState(stateMachineName, inputName, value)
    }

    class Builder {
        fun setAutoplay(value: Boolean): Builder = this
        fun build(): Any = Any()
    }
}
