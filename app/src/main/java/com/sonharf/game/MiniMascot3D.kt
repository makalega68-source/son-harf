package com.sonharf.game

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal enum class MiniMood { IDLE, HAPPY, SAD, CUTE, STREAK, COLLECT }

@Composable
internal fun MiniMascot3D(
    mood: MiniMood,
    modifier: Modifier = Modifier,
) {
    var renderer by remember { mutableStateOf<MiniRenderer?>(null) }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            MascotSurface(context).also { view ->
                renderer = view.renderer
                view.renderer.mood = mood
            }
        },
        update = { it.renderer.mood = mood },
    )
    LaunchedEffect(mood) {
        renderer?.mood = mood
        if (mood != MiniMood.IDLE) {
            delay(if (mood == MiniMood.STREAK) 2200 else 1700)
            renderer?.mood = MiniMood.IDLE
        }
    }
}

private class MascotSurface(context: Context) : GLSurfaceView(context) {
    val renderer = MiniRenderer()
    init {
        setEGLContextClientVersion(2)
        setZOrderOnTop(true)
        holder.setFormat(android.graphics.PixelFormat.TRANSLUCENT)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        preserveEGLContextOnPause = true
    }
}

private class MiniRenderer : GLSurfaceView.Renderer {
    @Volatile var mood: MiniMood = MiniMood.IDLE
    private lateinit var sphere: SphereMesh
    private lateinit var cone: ConeMesh
    private var program = 0
    private var aPosition = 0
    private var aNormal = 0
    private var uMvp = 0
    private var uModel = 0
    private var uColor = 0
    private var uLight = 0
    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)
    private val tmp = FloatArray(16)
    private var startNs = System.nanoTime()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        sphere = SphereMesh(18, 14)
        cone = ConeMesh(16)
        program = link(VERTEX, FRAGMENT)
        aPosition = GLES20.glGetAttribLocation(program, "aPosition")
        aNormal = GLES20.glGetAttribLocation(program, "aNormal")
        uMvp = GLES20.glGetUniformLocation(program, "uMvp")
        uModel = GLES20.glGetUniformLocation(program, "uModel")
        uColor = GLES20.glGetUniformLocation(program, "uColor")
        uLight = GLES20.glGetUniformLocation(program, "uLight")
        Matrix.setLookAtM(view, 0, 0f, 0.15f, 6.2f, 0f, 0.15f, 0f, 0f, 1f, 0f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height.coerceAtLeast(1)
        Matrix.perspectiveM(projection, 0, 34f, ratio, 1f, 20f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)
        GLES20.glUniform3f(uLight, -2.5f, 4.5f, 5.5f)
        val t = (System.nanoTime() - startNs) / 1_000_000_000f
        drawKitten(t)
    }

    private fun drawKitten(t: Float) {
        val bob = when (mood) {
            MiniMood.HAPPY -> kotlin.math.abs(sin(t * 7f)) * 0.16f
            MiniMood.STREAK -> kotlin.math.abs(sin(t * 9f)) * 0.23f
            MiniMood.SAD -> -0.08f
            else -> sin(t * 2.2f) * 0.025f
        }
        val sway = when (mood) {
            MiniMood.CUTE -> sin(t * 3.5f) * 12f
            MiniMood.STREAK -> sin(t * 8f) * 10f
            else -> sin(t * 1.8f) * 2.5f
        }
        val eyeSquint = mood == MiniMood.HAPPY || mood == MiniMood.STREAK
        val sad = mood == MiniMood.SAD
        val collect = mood == MiniMood.COLLECT

        // body and head
        drawSphere(0f, -0.55f + bob, 0f, 0.78f, 0.92f, 0.62f, WHITE)
        drawSphere(0f, 0.52f + bob, 0.03f, 1.02f, 0.88f, 0.86f, WHITE, rz = sway)

        // ears
        drawCone(-0.58f, 1.23f + bob, 0f, 0.38f, 0.58f, 0.28f, WHITE, rz = -10f + sway)
        drawCone(0.58f, 1.23f + bob, 0f, 0.38f, 0.58f, 0.28f, WHITE, rz = 10f + sway)
        drawCone(-0.58f, 1.24f + bob, 0.10f, 0.21f, 0.38f, 0.15f, PINK, rz = -10f + sway)
        drawCone(0.58f, 1.24f + bob, 0.10f, 0.21f, 0.38f, 0.15f, PINK, rz = 10f + sway)

        // muzzle
        drawSphere(-0.18f, 0.28f + bob, 0.76f, 0.34f, 0.24f, 0.18f, MUZZLE)
        drawSphere(0.18f, 0.28f + bob, 0.76f, 0.34f, 0.24f, 0.18f, MUZZLE)
        drawSphere(0f, 0.38f + bob, 0.91f, 0.12f, 0.09f, 0.08f, NOSE)

        // eyes: oversized blue irises with dark pupils
        val eyeY = if (sad) 0.64f else 0.72f
        val eyeScaleY = if (eyeSquint) 0.10f else 0.33f
        for (x in floatArrayOf(-0.37f, 0.37f)) {
            drawSphere(x, eyeY + bob, 0.76f, 0.30f, eyeScaleY, 0.12f, EYE_BLUE)
            drawSphere(x, eyeY + bob, 0.86f, 0.15f, eyeScaleY * 0.62f, 0.07f, PUPIL)
            if (!eyeSquint) drawSphere(x - 0.05f, eyeY + 0.08f + bob, 0.93f, 0.055f, 0.055f, 0.03f, HIGHLIGHT)
        }

        // paws/arms
        val leftArmY = when (mood) { MiniMood.HAPPY, MiniMood.STREAK -> 0.03f; MiniMood.SAD -> -0.53f; else -> -0.18f }
        val rightArmY = if (collect) -0.65f else leftArmY
        drawSphere(-0.78f, leftArmY + bob, 0.22f, 0.27f, 0.55f, 0.24f, WHITE, rz = if (mood == MiniMood.HAPPY) -38f else 14f)
        drawSphere(0.78f, rightArmY + bob, 0.22f, 0.27f, 0.55f, 0.24f, WHITE, rz = if (mood == MiniMood.HAPPY) 38f else -14f)
        drawSphere(-0.34f, -1.30f + bob, 0.2f, 0.39f, 0.28f, 0.47f, WHITE)
        drawSphere(0.34f, -1.30f + bob, 0.2f, 0.39f, 0.28f, 0.47f, WHITE)

        // tail made from curved overlapping 3D segments
        for (i in 0..5) {
            val a = i / 5f
            val tx = 0.72f + a * 0.68f
            val ty = -0.70f + sin(a * PI.toFloat()) * 0.55f + bob
            drawSphere(tx, ty, -0.18f, 0.26f, 0.30f, 0.25f, WHITE)
        }

        // blue Son Harf scarf
        drawSphere(0f, -0.02f + bob, 0.62f, 0.64f, 0.16f, 0.12f, SCARF)
        drawCone(0f, -0.42f + bob, 0.61f, 0.42f, 0.62f, 0.10f, SCARF, rz = 180f)

        // tiny gold bell
        drawSphere(0f, -0.12f + bob, 0.80f, 0.11f, 0.12f, 0.09f, GOLD)
    }

    private fun drawSphere(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, color: FloatArray, rz: Float = 0f) {
        drawMesh(sphere, x, y, z, sx, sy, sz, color, rz)
    }

    private fun drawCone(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, color: FloatArray, rz: Float = 0f) {
        drawMesh(cone, x, y, z, sx, sy, sz, color, rz)
    }

    private fun drawMesh(mesh: Mesh, x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, color: FloatArray, rz: Float) {
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, x, y, z)
        Matrix.rotateM(model, 0, rz, 0f, 0f, 1f)
        Matrix.scaleM(model, 0, sx, sy, sz)
        Matrix.multiplyMM(tmp, 0, view, 0, model, 0)
        Matrix.multiplyMM(mvp, 0, projection, 0, tmp, 0)
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(uModel, 1, false, model, 0)
        GLES20.glUniform4fv(uColor, 1, color, 0)
        mesh.draw(aPosition, aNormal)
    }

    private fun link(vs: String, fs: String): Int {
        val v = shader(GLES20.GL_VERTEX_SHADER, vs)
        val f = shader(GLES20.GL_FRAGMENT_SHADER, fs)
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, v)
        GLES20.glAttachShader(p, f)
        GLES20.glLinkProgram(p)
        return p
    }

    private fun shader(type: Int, source: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, source)
        GLES20.glCompileShader(s)
        return s
    }

    companion object {
        private val WHITE = floatArrayOf(0.98f, 0.98f, 1f, 1f)
        private val MUZZLE = floatArrayOf(1f, 0.96f, 0.94f, 1f)
        private val PINK = floatArrayOf(1f, 0.58f, 0.67f, 1f)
        private val NOSE = floatArrayOf(0.95f, 0.42f, 0.50f, 1f)
        private val EYE_BLUE = floatArrayOf(0.12f, 0.62f, 1f, 1f)
        private val PUPIL = floatArrayOf(0.025f, 0.08f, 0.13f, 1f)
        private val HIGHLIGHT = floatArrayOf(1f, 1f, 1f, 1f)
        private val SCARF = floatArrayOf(0.06f, 0.42f, 0.78f, 1f)
        private val GOLD = floatArrayOf(1f, 0.68f, 0.08f, 1f)
        private const val VERTEX = """
            uniform mat4 uMvp;
            uniform mat4 uModel;
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            varying vec3 vNormal;
            varying vec3 vWorld;
            void main() {
                gl_Position = uMvp * vec4(aPosition, 1.0);
                vNormal = normalize(mat3(uModel) * aNormal);
                vWorld = (uModel * vec4(aPosition, 1.0)).xyz;
            }
        """
        private const val FRAGMENT = """
            precision mediump float;
            uniform vec4 uColor;
            uniform vec3 uLight;
            varying vec3 vNormal;
            varying vec3 vWorld;
            void main() {
                vec3 l = normalize(uLight - vWorld);
                float diffuse = max(dot(normalize(vNormal), l), 0.0);
                float rim = pow(1.0 - max(vNormal.z, 0.0), 2.0);
                vec3 rgb = uColor.rgb * (0.54 + diffuse * 0.52) + rim * 0.08;
                gl_FragColor = vec4(rgb, uColor.a);
            }
        """
    }
}

private interface Mesh { fun draw(positionLoc: Int, normalLoc: Int) }

private class SphereMesh(lon: Int, lat: Int) : Mesh {
    private val vertices: FloatBuffer
    private val normals: FloatBuffer
    private val indices: ShortBuffer
    private val count: Int
    init {
        val v = ArrayList<Float>()
        val n = ArrayList<Float>()
        val idx = ArrayList<Short>()
        for (iy in 0..lat) {
            val theta = PI * iy / lat
            val st = sin(theta).toFloat(); val ct = cos(theta).toFloat()
            for (ix in 0..lon) {
                val phi = 2.0 * PI * ix / lon
                val sp = sin(phi).toFloat(); val cp = cos(phi).toFloat()
                val x = st * cp; val y = ct; val z = st * sp
                v.add(x); v.add(y); v.add(z); n.add(x); n.add(y); n.add(z)
            }
        }
        for (iy in 0 until lat) for (ix in 0 until lon) {
            val a = (iy * (lon + 1) + ix).toShort()
            val b = ((iy + 1) * (lon + 1) + ix).toShort()
            val c = (a + 1).toShort(); val d = (b + 1).toShort()
            idx.add(a); idx.add(b); idx.add(c); idx.add(c); idx.add(b); idx.add(d)
        }
        vertices = floatBuffer(v); normals = floatBuffer(n); indices = shortBuffer(idx); count = idx.size
    }
    override fun draw(positionLoc: Int, normalLoc: Int) {
        GLES20.glEnableVertexAttribArray(positionLoc); GLES20.glEnableVertexAttribArray(normalLoc)
        GLES20.glVertexAttribPointer(positionLoc, 3, GLES20.GL_FLOAT, false, 0, vertices)
        GLES20.glVertexAttribPointer(normalLoc, 3, GLES20.GL_FLOAT, false, 0, normals)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, count, GLES20.GL_UNSIGNED_SHORT, indices)
    }
}

private class ConeMesh(segments: Int) : Mesh {
    private val vertices: FloatBuffer
    private val normals: FloatBuffer
    private val indices: ShortBuffer
    private val count: Int
    init {
        val v = ArrayList<Float>(); val n = ArrayList<Float>(); val idx = ArrayList<Short>()
        v.add(0f); v.add(1f); v.add(0f); n.add(0f); n.add(0.6f); n.add(0.8f)
        for (i in 0..segments) {
            val a = 2.0 * PI * i / segments
            val x = cos(a).toFloat(); val z = sin(a).toFloat()
            v.add(x); v.add(-1f); v.add(z); n.add(x); n.add(0.45f); n.add(z)
        }
        for (i in 1..segments) { idx.add(0); idx.add(i.toShort()); idx.add((i + 1).toShort()) }
        vertices = floatBuffer(v); normals = floatBuffer(n); indices = shortBuffer(idx); count = idx.size
    }
    override fun draw(positionLoc: Int, normalLoc: Int) {
        GLES20.glEnableVertexAttribArray(positionLoc); GLES20.glEnableVertexAttribArray(normalLoc)
        GLES20.glVertexAttribPointer(positionLoc, 3, GLES20.GL_FLOAT, false, 0, vertices)
        GLES20.glVertexAttribPointer(normalLoc, 3, GLES20.GL_FLOAT, false, 0, normals)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, count, GLES20.GL_UNSIGNED_SHORT, indices)
    }
}

private fun floatBuffer(values: List<Float>): FloatBuffer = ByteBuffer.allocateDirect(values.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
    values.forEach { put(it) }; position(0)
}
private fun shortBuffer(values: List<Short>): ShortBuffer = ByteBuffer.allocateDirect(values.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer().apply {
    values.forEach { put(it) }; position(0)
}
