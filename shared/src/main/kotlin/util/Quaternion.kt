package com.glycin.util

import kotlin.math.acos
import kotlin.math.sin
import kotlin.math.sqrt

data class Quaternion(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val w: Float = 1f,
) {
    companion object {
        val identity = Quaternion(0f, 0f, 0f, 1f)
    }

    fun normalize(): Quaternion {
        val len = sqrt(x * x + y * y + z * z + w * w)
        return if (len > 0f) Quaternion(x / len, y / len, z / len, w / len) else this
    }

    fun slerp(other: Quaternion, t: Float): Quaternion {
        var bx = other.x; var by = other.y; var bz = other.z; var bw = other.w
        var dot = x * bx + y * by + z * bz + w * bw

        if (dot < 0f) {
            bx = -bx; by = -by; bz = -bz; bw = -bw
            dot = -dot
        }

        if (dot > 0.9995f) {
            return Quaternion(
                x + (bx - x) * t,
                y + (by - y) * t,
                z + (bz - z) * t,
                w + (bw - w) * t,
            ).normalize()
        }

        val theta = acos(dot.coerceIn(-1f, 1f))
        val sinTheta = sin(theta)
        val wa = sin((1f - t) * theta) / sinTheta
        val wb = sin(t * theta) / sinTheta

        return Quaternion(
            wa * x + wb * bx,
            wa * y + wb * by,
            wa * z + wb * bz,
            wa * w + wb * bw,
        )
    }

    fun toMat4(): Mat4 {
        val xx = x * x; val xy = x * y; val xz = x * z; val xw = x * w
        val yy = y * y; val yz = y * z; val yw = y * w
        val zz = z * z; val zw = z * w

        return Mat4(floatArrayOf(
            1f - 2f * (yy + zz), 2f * (xy + zw),       2f * (xz - yw),       0f,
            2f * (xy - zw),       1f - 2f * (xx + zz), 2f * (yz + xw),       0f,
            2f * (xz + yw),       2f * (yz - xw),       1f - 2f * (xx + yy), 0f,
            0f,                   0f,                   0f,                   1f,
        ))
    }
}
