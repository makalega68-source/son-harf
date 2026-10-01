package com.sonharf.game

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.abs
import kotlin.math.min

/** Lets a screen follow and steer a [ChromaKeyVideo]: time, end, pause and resume. */
internal class ChromaKeyVideoController {
    @Volatile internal var player: MediaPlayer? = null
    var completed by mutableStateOf(false)
        internal set
    val positionMs: Long get() = runCatching { player?.currentPosition?.toLong() ?: 0L }.getOrDefault(0L)
    val durationMs: Long get() = runCatching { player?.duration?.toLong() ?: 0L }.getOrDefault(0L)
    fun pause() { runCatching { if (player?.isPlaying == true) player?.pause() } }
    fun resume() { runCatching { if (!completed && player?.isPlaying == false) player?.start() } }
}

/**
 * Plays a green-screen (or any plain-backdrop) clip with the backdrop removed, so it floats over
 * the screen. The backdrop colour is read from the first frame's border and keyed out on the GPU
 * by hue (chroma), the edge is smoothed over neighbouring pixels and the green spill is removed,
 * so there is no jagged or green outline. The clip keeps its aspect ratio, fitted and shrunk by
 * [scale] inside the view. If the border is not one plain colour, the clip plays as it is.
 */
@Composable
internal fun ChromaKeyVideo(
    @RawRes raw: Int,
    modifier: Modifier = Modifier,
    loop: Boolean = false,
    muted: Boolean = true,
    speed: Float = 1f,
    scale: Float = 1f,
    /** Fill the whole view (centre-crop) instead of fitting the whole clip inside it. */
    crop: Boolean = false,
    /** Remove a plain backdrop; off plays the clip exactly as it is. */
    keying: Boolean = true,
    /** Fill the space around a shrunken clip with the colour of the clip's own edge, so it blends in. */
    matte: Boolean = false,
    controller: ChromaKeyVideoController? = null,
    onFinished: () -> Unit = {},
) {
    val finished by rememberUpdatedState(onFinished)
    AndroidView(
        factory = { ChromaKeyVideoView(it, KeyVideoSpec(raw, loop, muted, speed, scale, crop, keying, matte), controller) { finished() } },
        modifier = modifier,
        onRelease = { it.dispose() },
    )
}

internal data class KeyVideoSpec(@RawRes val raw: Int, val loop: Boolean, val muted: Boolean, val speed: Float, val scale: Float, val crop: Boolean = false, val keying: Boolean = true, val matte: Boolean = false)

internal class ChromaKeyVideoView(
    context: Context,
    private val spec: KeyVideoSpec,
    private val controller: ChromaKeyVideoController?,
    private val onFinished: () -> Unit,
) : TextureView(context), TextureView.SurfaceTextureListener {
    private var renderer: KeyRenderer? = null

    init {
        isOpaque = false
        surfaceTextureListener = this
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        renderer = KeyRenderer(context.applicationContext, spec, controller, surface, width, height, onFinished).also { it.start() }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        renderer?.resize(width, height)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        // The render thread may still be drawing into this surface: it is released there, after
        // the player and GL are torn down (releasing it here at once could freeze or crash).
        val active = renderer
        renderer = null
        if (active == null) return true
        active.quit { runCatching { surface.release() } }
        return false
    }

    fun dispose() {
        renderer?.quit()
        renderer = null
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
}

private class KeyRenderer(
    private val context: Context,
    private val spec: KeyVideoSpec,
    private val controller: ChromaKeyVideoController?,
    private val output: SurfaceTexture,
    @Volatile private var width: Int,
    @Volatile private var height: Int,
    private val onFinished: () -> Unit,
) {
    private val thread = HandlerThread("chroma-key-video")
    private lateinit var handler: Handler
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var stopping = false
    private var finished = false
    private var lastFrameNanos = 0L

    private var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var program = 0
    private var videoTex = 0
    private var videoTexture: SurfaceTexture? = null
    private var videoSurface: Surface? = null
    private var player: MediaPlayer? = null
    @Volatile private var videoW = 0
    @Volatile private var videoH = 0
    private val texMatrix = FloatArray(16)
    private val quad: FloatBuffer = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
        // x, y, u, v
        put(floatArrayOf(-1f, -1f, 0f, 0f, 1f, -1f, 1f, 0f, -1f, 1f, 0f, 1f, 1f, 1f, 1f, 1f)).position(0)
    }

    // Key colour, decided from the first frame; keying stays off when the border is not plain.
    private var keyKnown = false
    private var keyOn = false
    private var keyTries = 0
    private val key = floatArrayOf(0f, 0f, 0f)
    // Edge colour of the playing clip, used to fill around it when [KeyVideoSpec.matte] is on.
    private val matteColor = floatArrayOf(0f, 0f, 0f)
    private var matteKnown = false
    private var matteFrame = 0

    fun start() {
        thread.start()
        handler = Handler(thread.looper)
        handler.post { runCatching { setUp() }.onFailure { tearDown(); finish() } }
    }

    fun resize(w: Int, h: Int) {
        width = w
        height = h
    }

    fun quit(afterTearDown: () -> Unit = {}) {
        if (stopping) return
        stopping = true
        handler.postAtFrontOfQueue {
            runCatching { tearDown() }
            afterTearDown()
            thread.quitSafely()
        }
    }

    private fun finish() {
        if (finished || stopping) return
        finished = true
        main.post {
            controller?.completed = true
            onFinished()
        }
    }

    private fun setUp() {
        if (stopping) return
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        check(EGL14.eglInitialize(display, version, 0, version, 1))
        val attribs = intArrayOf(
            EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, EGL14.EGL_NONE,
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val count = IntArray(1)
        check(EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, count, 0) && count[0] > 0)
        eglContext = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT, intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0)
        eglSurface = EGL14.eglCreateWindowSurface(display, configs[0], output, intArrayOf(EGL14.EGL_NONE), 0)
        check(EGL14.eglMakeCurrent(display, eglSurface, eglSurface, eglContext))

        program = buildProgram()
        val tex = IntArray(1)
        GLES20.glGenTextures(1, tex, 0)
        videoTex = tex[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, videoTex)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // Nothing is shown until the first keyed frame is ready.
        clear()
        EGL14.eglSwapBuffers(display, eglSurface)

        val texture = SurfaceTexture(videoTex)
        // A frame that fails to draw (e.g. the surface going away) is skipped, never fatal.
        texture.setOnFrameAvailableListener({
            if (!stopping && !finished) runCatching { drawFrame() }.onFailure { finish() }
        }, handler)
        videoTexture = texture
        val surface = Surface(texture)
        videoSurface = surface

        val afd = context.resources.openRawResourceFd(spec.raw)
        player = MediaPlayer().apply {
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            setSurface(surface)
            if (spec.muted) setVolume(0f, 0f)
            isLooping = spec.loop
            setOnVideoSizeChangedListener { _, w, h ->
                videoW = w
                videoH = h
            }
            setOnCompletionListener { finish() }
            setOnErrorListener { _, _, _ -> finish(); true }
            setOnPreparedListener {
                if (stopping) return@setOnPreparedListener
                it.start()
                if (spec.speed != 1f) runCatching { it.playbackParams = it.playbackParams.setSpeed(spec.speed) }
            }
            prepareAsync()
        }
        controller?.player = player
    }

    /** Fit the clip inside the view keeping its aspect ratio, then shrink it by [KeyVideoSpec.scale]. */
    private fun fitScale(): Pair<Float, Float> {
        val vw = videoW.toFloat()
        val vh = videoH.toFloat()
        if (vw <= 0f || vh <= 0f || width <= 0 || height <= 0) return spec.scale to spec.scale
        val viewAspect = width.toFloat() / height
        val videoAspect = vw / vh
        // Fit: the wider side touches the view edge. Crop: the narrower side does, and the rest spills over.
        val wider = videoAspect > viewAspect
        val (sx, sy) = if (wider != spec.crop) 1f to viewAspect / videoAspect else videoAspect / viewAspect to 1f
        if (spec.crop) {
            val grow = 1f / minOf(sx, sy)
            return sx * grow * spec.scale to sy * grow * spec.scale
        }
        return sx * spec.scale to sy * spec.scale
    }

    private fun drawFrame() {
        if (stopping || finished) return
        val texture = videoTexture ?: return
        if (eglSurface == EGL14.EGL_NO_SURFACE) return
        texture.updateTexImage()
        val now = System.nanoTime()
        if (now - lastFrameNanos < 33_000_000L) return
        lastFrameNanos = now
        texture.getTransformMatrix(texMatrix)
        GLES20.glViewport(0, 0, width, height)
        if (!spec.keying) {
            keyKnown = true
            keyOn = false
        }
        if (!keyKnown) {
            // Draw the frame full-size and unkeyed, and read its border to learn the backdrop colour.
            clear()
            draw(keying = false, sx = 1f, sy = 1f)
            decideKey()
        }
        val (sx, sy) = fitScale()
        if (spec.matte && !keyOn) {
            // Every few frames draw the clip once to read its edge, then fill the view with that colour.
            if (matteFrame++ % 4 == 0) {
                clear()
                draw(keying = false, sx = sx, sy = sy)
                sampleMatte(sx, sy)
            }
            GLES20.glClearColor(matteColor[0], matteColor[1], matteColor[2], 1f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        } else {
            clear()
        }
        draw(keying = keyOn, sx = sx, sy = sy)
        EGL14.eglSwapBuffers(display, eglSurface)
    }

    /** Median colour of points just inside the drawn clip's border, eased so the fill never flickers. */
    private fun sampleMatte(sx: Float, sy: Float) {
        if (width <= 0 || height <= 0) return
        val x0 = ((1f - sx) / 2f * width).toInt().coerceIn(0, width - 1)
        val x1 = ((1f + sx) / 2f * width).toInt().coerceIn(0, width - 1)
        val y0 = ((1f - sy) / 2f * height).toInt().coerceIn(0, height - 1)
        val y1 = ((1f + sy) / 2f * height).toInt().coerceIn(0, height - 1)
        val inset = 4
        val left = (x0 + inset).coerceAtMost(width - 1)
        val right = (x1 - inset).coerceAtLeast(0)
        val bottom = (y0 + inset).coerceAtMost(height - 1)
        val top = (y1 - inset).coerceAtLeast(0)
        val midX = (x0 + x1) / 2
        val midY = (y0 + y1) / 2
        val points = listOf(
            left to bottom, midX to bottom, right to bottom,
            left to midY, right to midY,
            left to top, midX to top, right to top,
            left to (y0 + y1 * 3) / 4, right to (y0 + y1 * 3) / 4,
            left to (y0 * 3 + y1) / 4, right to (y0 * 3 + y1) / 4,
        )
        val pixel = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
        val samples = points.map { (px, py) ->
            pixel.clear()
            GLES20.glReadPixels(px, py, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixel)
            floatArrayOf((pixel.get(0).toInt() and 0xFF) / 255f, (pixel.get(1).toInt() and 0xFF) / 255f, (pixel.get(2).toInt() and 0xFF) / 255f)
        }
        for (c in 0..2) {
            val median = samples.map { it[c] }.sorted()[samples.size / 2]
            matteColor[c] = if (matteKnown) matteColor[c] * .75f + median * .25f else median
        }
        matteKnown = true
    }

    private fun decideKey() {
        val points = listOf(
            .02f to .02f, .5f to .02f, .98f to .02f,
            .02f to .5f, .98f to .5f,
            .02f to .98f, .5f to .98f, .98f to .98f,
        )
        val pixel = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
        val samples = points.map { (fx, fy) ->
            pixel.clear()
            GLES20.glReadPixels((fx * (width - 1)).toInt(), (fy * (height - 1)).toInt(), 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixel)
            floatArrayOf((pixel.get(0).toInt() and 0xFF) / 255f, (pixel.get(1).toInt() and 0xFF) / 255f, (pixel.get(2).toInt() and 0xFF) / 255f)
        }
        for (c in 0..2) key[c] = samples.map { it[c] }.sorted()[samples.size / 2]
        // Plain backdrop: most border samples sit close to the median colour.
        val close = samples.count { s -> (0..2).sumOf { abs(s[it] - key[it]).toDouble() } < .22 }
        // Only a coloured backdrop is keyed: a black (fade-in) or grey border would wipe out the
        // dark parts of the mascot, so such frames are skipped and a later frame decides.
        val saturation = maxOf(key[0], key[1], key[2]) - minOf(key[0], key[1], key[2])
        keyOn = close >= 6 && saturation > .18f
        keyTries++
        keyKnown = keyOn || keyTries >= 45
    }

    private fun clear() {
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    }

    private fun draw(keying: Boolean, sx: Float, sy: Float) {
        GLES20.glUseProgram(program)
        val pos = GLES20.glGetAttribLocation(program, "aPos")
        val uv = GLES20.glGetAttribLocation(program, "aUv")
        quad.position(0)
        GLES20.glVertexAttribPointer(pos, 2, GLES20.GL_FLOAT, false, 16, quad)
        GLES20.glEnableVertexAttribArray(pos)
        quad.position(2)
        GLES20.glVertexAttribPointer(uv, 2, GLES20.GL_FLOAT, false, 16, quad)
        GLES20.glEnableVertexAttribArray(uv)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program, "uTex"), 1, false, texMatrix, 0)
        GLES20.glUniform2f(GLES20.glGetUniformLocation(program, "uScale"), sx, sy)
        GLES20.glUniform3f(GLES20.glGetUniformLocation(program, "uKey"), key[0], key[1], key[2])
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uKeying"), if (keying) 1f else 0f)
        val greenKey = key[1] > key[0] + .08f && key[1] > key[2] + .08f
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uGreen"), if (greenKey) 1f else 0f)
        // One source texel in texture space, for smoothing the edge over neighbours.
        val texel = 1f / maxOf(1, min(videoW, videoH).takeIf { it > 0 } ?: 1080)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uTexel"), texel * 1.5f)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, videoTex)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "sTex"), 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    private fun buildProgram(): Int {
        val vertex = """
            attribute vec2 aPos;
            attribute vec2 aUv;
            uniform mat4 uTex;
            uniform vec2 uScale;
            varying vec2 vUv;
            void main() {
                gl_Position = vec4(aPos * uScale, 0.0, 1.0);
                vUv = (uTex * vec4(aUv, 0.0, 1.0)).xy;
            }
        """.trimIndent()
        val fragment = """
            #extension GL_OES_EGL_image_external : require
            precision highp float;
            uniform samplerExternalOES sTex;
            uniform vec3 uKey;
            uniform float uKeying;
            uniform float uGreen;
            uniform float uTexel;
            varying vec2 vUv;

            // Hue plane (Cb, Cr): lighting changes on the backdrop barely move it.
            vec2 chroma(vec3 c) {
                return vec2(-0.1687 * c.r - 0.3313 * c.g + 0.5 * c.b, 0.5 * c.r - 0.4187 * c.g - 0.0813 * c.b);
            }
            float keyAlpha(vec2 uv) {
                vec3 c = texture2D(sTex, uv).rgb;
                return smoothstep(0.075, 0.18, distance(chroma(c), chroma(uKey)));
            }
            void main() {
                vec3 c = texture2D(sTex, vUv).rgb;
                float a = 1.0;
                if (uKeying > 0.5) {
                    // Smooth the matte over the neighbours so the outline is not jagged,
                    // then pull it in slightly so no backdrop fringe is left around the edge.
                    float m = keyAlpha(vUv) * 0.4
                        + (keyAlpha(vUv + vec2(uTexel, 0.0)) + keyAlpha(vUv - vec2(uTexel, 0.0))
                        + keyAlpha(vUv + vec2(0.0, uTexel)) + keyAlpha(vUv - vec2(0.0, uTexel))) * 0.15;
                    a = smoothstep(0.2, 0.9, m);
                    if (uGreen > 0.5) {
                        // Green spill: green never exceeds the stronger of red and blue.
                        c.g = min(c.g, max(c.r, c.b));
                    } else {
                        c = clamp((c - uKey * (1.0 - a)) / max(a, 0.02), 0.0, 1.0);
                    }
                }
                gl_FragColor = vec4(c * a, a);
            }
        """.trimIndent()
        fun shader(type: Int, source: String): Int = GLES20.glCreateShader(type).also {
            GLES20.glShaderSource(it, source)
            GLES20.glCompileShader(it)
            val ok = IntArray(1)
            GLES20.glGetShaderiv(it, GLES20.GL_COMPILE_STATUS, ok, 0)
            check(ok[0] != 0) { GLES20.glGetShaderInfoLog(it) }
        }
        return GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, shader(GLES20.GL_VERTEX_SHADER, vertex))
            GLES20.glAttachShader(it, shader(GLES20.GL_FRAGMENT_SHADER, fragment))
            GLES20.glLinkProgram(it)
        }
    }

    private fun tearDown() {
        controller?.player = null
        runCatching { player?.release() }
        player = null
        videoSurface?.release()
        videoSurface = null
        videoTexture?.release()
        videoTexture = null
        if (display != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, eglSurface)
            if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, eglContext)
            // Never eglTerminate the shared default display: the UI renderer uses it too.
            EGL14.eglReleaseThread()
        }
        display = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
        eglSurface = EGL14.EGL_NO_SURFACE
    }
}
