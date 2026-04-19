package com.glycin.springaria.util

import com.glycin.extensions.lerp
import kotlin.math.floor

class SimplexNoise(private val seed: Long) {

    fun noise(x: Float, y: Float): Float {
        val xi = floor(x).toInt()
        val yi = floor(y).toInt()

        val xf = x - floor(x)
        val yf = y - floor(y)

        val u = xf * xf * (3f - 2f * xf)
        val v = yf * yf * (3f - 2f * yf)

        val a = hash(xi, yi)
        val b = hash(xi + 1, yi)
        val c = hash(xi, yi + 1)
        val d = hash(xi + 1, yi + 1)

        val x1 = a.lerp(b, u)
        val x2 = c.lerp(d, u)
        return x1.lerp(x2, v)
    }

    private fun hash(x: Int, y: Int): Float {
        val h = (x * 374761393 + y * 668265263 + seed).toInt()
        return ((h xor (h ushr 13)) and 0xFF) / 255f
    }
}
