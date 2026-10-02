package com.sonharf.game

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** One opaque facial surface, curved around the head; the hidden side collapses at the limb.
 * No independent eye alpha or translation: eyebrows, lids and mouth share this projection.
 * The caller owns the vertex buffer so animation never allocates a mesh per frame.
 */
internal fun mascotFaceMesh(yaw: Float, pitch: Float, columns: Int, rows: Int, out: FloatArray) {
    val turn = yaw.coerceIn(-1f, 1f) * .80f
    val nod = pitch.coerceIn(-1f, 1f) * .18f
    val halfPi = (Math.PI / 2).toFloat()
    var index = 0
    for (row in 0..rows) {
        val y = 1254f * row / rows
        val v = ((y - 641f) / 411f).coerceIn(-1f, 1f)
        val latitude = asin(v)
        val radius = sqrt((1f - v * v).coerceAtLeast(0f))
        val turnedLatitude = (latitude + nod).coerceIn(-halfPi, halfPi)
        for (column in 0..columns) {
            val x = 1254f * column / columns
            val u = (x - 660f) / 400f
            val longitude = asin((u / radius.coerceAtLeast(.0001f)).coerceIn(-1f, 1f))
            val turnedLongitude = (longitude + turn).coerceIn(-halfPi, halfPi)
            out[index++] = 660f + 400f * cos(turnedLatitude) * sin(turnedLongitude)
            out[index++] = 641f + 411f * sin(turnedLatitude)
        }
    }
}
