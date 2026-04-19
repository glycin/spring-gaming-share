package com.glycin.util

import kotlin.math.sqrt

data class Vec3(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
) {
    companion object {
        val zero get() = Vec3(0f, 0f, 0f)
        val one get() = Vec3(1f, 1f, 1f)

        fun distance(a: Vec3, b: Vec3): Float {
            val dx = b.x - a.x
            val dy = b.y - a.y
            val dz = b.z - a.z
            return sqrt(dx * dx + dy * dy + dz * dz)
        }
    }

    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Int) = Vec3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Int) = Vec3(x / scalar, y / scalar, z / scalar)

    operator fun plus(other: Float) = Vec3(x + other, y + other, z + other)
    operator fun minus(other: Float) = Vec3(x - other, y - other, z - other)
    operator fun times(scalar: Float) = Vec3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float) = Vec3(x / scalar, y / scalar, z / scalar)

    fun dot(other: Vec3) = x * other.x + y * other.y + z * other.z

    fun cross(other: Vec3) = Vec3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x,
    )

    fun magnitude() = sqrt(x.toDouble() * x + y * y + z * z)

    fun normalized(): Vec3 {
        val mag = magnitude().toFloat()
        return if (mag != 0f) this / mag else zero
    }

    fun lerp(other: Vec3, t: Float) = Vec3(
        x + (other.x - x) * t,
        y + (other.y - y) * t,
        z + (other.z - z) * t,
    )
}
