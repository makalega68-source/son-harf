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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.abs

/**
 * Plays a video with its plain background removed, so it floats over the screen.
 *
 * The clip has no alpha channel, so the background colour is read from the first frame's border
 * and keyed out on the GPU (soft edge + colour spill removal). If the border is not one plain
 * colour the video simply plays as it is.
 */
@Composable
internal fun ChromaKeyVideo(@RawRes raw: Int, modifier: Modifier = Modifier, loop: Boolean = false, onFinished: () -> Unit = {}) {
    val finished by rememberUpdatedState(onFinished)
    AndroidView(
        factory = { ChromaKeyVideoView(it, raw, loop) { finished() } },
        modifier = modifier,
    )
}

internal class ChromaKeyVideoView(
    context: Context,
    @RawRes private val raw: Int,
    private val loop: Boolean,
    private val onFinished: () -> Unit,
) : TextureView(context), TextureView.SurfaceTextureListener {
    private var renderer: KeyRenderer? = null

    init {
        isOpaque = false
        surfaceTextureListener = this
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        renderer = KeyRenderer(context.applicationContext, raw, loop, surface, width, height, onFinished).also { it.start() }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        renderer?.resize(width, height)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        renderer?.quit()
        renderer = null
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
}

private class KeyRenderer(
    private val context: Context,
    @RawRes private val raw: Int,
    private val loop: Boolean,
    private val output: SurfaceTexture,
    @Volatile private var width: Int,
    @Volatile private var height: Int,
    private val onFinished: () -> Unit,
) {
    private val thread = HandlerThread("chroma-key-video")
    private lateinit var handler: Handler
    private val main = Handler(Looper.getMainLooper())

    private var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var program = 0
    private var videoTex = 0
    private var videoTexture: SurfaceTexture? = null
    private var videoSurface: Surface? = null
    private var player: MediaPlayer? = null
    private val texMatrix = FloatArray(16)
    private val quad: FloatBuffer = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
        // x, y, u, v
        put(floatArrayOf(-1f, -1f, 0f, 0f, 1f, -1f, 1f, 0f, -1f, 1f, 0f, 1f, 1f, 1f, 1f, 1f)).position(0)
    }

    // Key colour, decided from the first frame; keying stays off when the border is not plain.
    private var keyKnown = false
    private var keyOn = false
    private val key = floatArrayOf(0f, 0f, 0f)

    fun start() {
        thread.start()
        handler = Handler(thread.looper)
        handler.post { runCatching { setUp() }.onFailure { tearDown(); main.post(onFinished) } }
    }

    fun resize(w: Int, h: Int) {
        width = w
        height = h
    }

    fun quit() {
        handler.post {
            tearDown()
            thread.quitSafely()
        }
    }

    private fun setUp() {
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

        // Nothing is shown until the first keyed frame is ready.
        clear()
        EGL14.eglSwapBuffers(display, eglSurface)

        val texture = SurfaceTexture(videoTex)
        texture.setOnFrameAvailableListener({ drawFrame() }, handler)
        videoTexture = texture
        val surface = Surface(texture)
        videoSurface = surface

        val afd = context.resources.openRawResourceFd(raw)
        player = MediaPlayer().apply {
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            setSurface(surface)
            setVolume(0f, 0f)
            isLooping = loop
            setOnCompletionListener { main.post(onFinished) }
            setOnErrorListener { _, _, _ -> main.post(onFinished); true }
            setOnPreparedListener { it.start() }
            prepareAsync()
        }
    }

    private fun drawFrame() {
        val texture = videoTexture ?: return
        if (eglSurface == EGL14.EGL_NO_SURFACE) return
        texture.updateTexImage()
        texture.getTransformMatrix(texMatrix)
        GLES20.glViewport(0, 0, width, height)
        if (!keyKnown) {
            // Draw the frame as it is and read its border to learn the background colour.
            clear()
            draw(keying = false)
            decideKey()
        }
        clear()
        draw(keying = keyOn)
        EGL14.eglSwapBuffers(display, eglSurface)
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
        // Plain background: most border samples sit close to the median colour.
        val close = samples.count { s -> (0..2).sumOf { abs(s[it] - key[it]).toDouble() } < .18 }
        keyOn = close >= 6
        keyKnown = true
    }

    private fun clear() {
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    }

    private fun draw(keying: Boolean) {
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
        GLES20.glUniform3f(GLES20.glGetUniformLocation(program, "uKey"), key[0], key[1], key[2])
        GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "uKeying"), if (keying) 1f else 0f)
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
            varying vec2 vUv;
            void main() {
                gl_Position = vec4(aPos, 0.0, 1.0);
                vUv = (uTex * vec4(aUv, 0.0, 1.0)).xy;
            }
        """.trimIndent()
        val fragment = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            uniform samplerExternalOES sTex;
            uniform vec3 uKey;
            uniform float uKeying;
            varying vec2 vUv;
            void main() {
                vec3 c = texture2D(sTex, vUv).rgb;
                float a = 1.0;
                if (uKeying > 0.5) {
                    // Soft key: fully clear near the background colour, solid further away.
                    a = smoothstep(0.10, 0.30, distance(c, uKey));
                    // Remove the background's tint from half-transparent edge pixels.
                    c = clamp((c - uKey * (1.0 - a)) / max(a, 0.02), 0.0, 1.0);
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
