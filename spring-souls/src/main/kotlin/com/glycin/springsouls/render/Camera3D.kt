package com.glycin.springsouls.render

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class Camera3D(
    var x: Float = 0f,
    var y: Float = 2f,
    var z: Float = 5f,
    var yaw: Float = -90f,
    var pitch: Float = -20f,
) {
    var fov: Float = 70f
    var nearPlane: Float = 0.1f
    var farPlane: Float = 1000f

    private val viewBuffer = FloatArray(16)
    private val projBuffer = FloatArray(16)
    private val modelBuffer = FloatArray(16)

    fun viewMatrix(): FloatArray {
        val yawRad = Math.toRadians(yaw.toDouble())
        val pitchRad = Math.toRadians(pitch.toDouble())
        val cosP = cos(pitchRad).toFloat()
        val sinP = sin(pitchRad).toFloat()
        val cosY = cos(yawRad).toFloat()
        val sinY = sin(yawRad).toFloat()

        val fx = cosY * cosP
        val fy = sinP
        val fz = sinY * cosP

        val rx = -fz
        val ry = 0f
        val rz = fx
        val rLen = sqrt(rx * rx + rz * rz)
        val rdx = rx / rLen; val rdz = rz / rLen

        val ux = -rdz * fy
        val uy = rdz * fx - rdx * fz
        val uz = rdx * fy

        viewBuffer[0] = rdx;  viewBuffer[1] = ux;   viewBuffer[2] = -fx;  viewBuffer[3] = 0f
        viewBuffer[4] = ry;   viewBuffer[5] = uy;   viewBuffer[6] = -fy;  viewBuffer[7] = 0f
        viewBuffer[8] = rdz;  viewBuffer[9] = uz;   viewBuffer[10] = -fz; viewBuffer[11] = 0f
        viewBuffer[12] = -(rdx * x + ry * y + rdz * z)
        viewBuffer[13] = -(ux * x + uy * y + uz * z)
        viewBuffer[14] = -(-fx * x + -fy * y + -fz * z)
        viewBuffer[15] = 1f

        return viewBuffer
    }

    fun projectionMatrix(aspectRatio: Float): FloatArray {
        val tanHalfFov = tan(Math.toRadians(fov.toDouble() / 2.0)).toFloat()
        val range = nearPlane - farPlane

        projBuffer[0] = 1f / (aspectRatio * tanHalfFov)
        projBuffer[1] = 0f; projBuffer[2] = 0f; projBuffer[3] = 0f
        projBuffer[4] = 0f
        projBuffer[5] = 1f / tanHalfFov
        projBuffer[6] = 0f; projBuffer[7] = 0f
        projBuffer[8] = 0f; projBuffer[9] = 0f
        projBuffer[10] = (farPlane + nearPlane) / range
        projBuffer[11] = -1f
        projBuffer[12] = 0f; projBuffer[13] = 0f
        projBuffer[14] = (2f * farPlane * nearPlane) / range
        projBuffer[15] = 0f

        return projBuffer
    }

    fun modelMatrix(tx: Float, ty: Float, tz: Float, scale: Float = 1f, rotationY: Float = 0f): FloatArray {
        val rad = Math.toRadians(rotationY.toDouble())
        val c = cos(rad).toFloat()
        val s = sin(rad).toFloat()
        modelBuffer[0] = c * scale;  modelBuffer[1] = 0f; modelBuffer[2] = s * scale;  modelBuffer[3] = 0f
        modelBuffer[4] = 0f;         modelBuffer[5] = scale; modelBuffer[6] = 0f;       modelBuffer[7] = 0f
        modelBuffer[8] = -s * scale; modelBuffer[9] = 0f; modelBuffer[10] = c * scale;  modelBuffer[11] = 0f
        modelBuffer[12] = tx;        modelBuffer[13] = ty; modelBuffer[14] = tz;         modelBuffer[15] = 1f
        return modelBuffer
    }

    fun modelMatrix(tx: Float, ty: Float, tz: Float, scale: Float, rotationY: Float, rotationX: Float): FloatArray {
        val ry = Math.toRadians(rotationY.toDouble())
        val rx = Math.toRadians(rotationX.toDouble())
        val cy = cos(ry).toFloat(); val sy = sin(ry).toFloat()
        val cx = cos(rx).toFloat(); val sx = sin(rx).toFloat()

        // Ry * Rx * scale
        modelBuffer[0]  = cy * scale;       modelBuffer[1]  = sy * sx * scale;  modelBuffer[2]  = sy * cx * scale;  modelBuffer[3]  = 0f
        modelBuffer[4]  = 0f;               modelBuffer[5]  = cx * scale;       modelBuffer[6]  = -sx * scale;      modelBuffer[7]  = 0f
        modelBuffer[8]  = -sy * scale;      modelBuffer[9]  = cy * sx * scale;  modelBuffer[10] = cy * cx * scale;  modelBuffer[11] = 0f
        modelBuffer[12] = tx;               modelBuffer[13] = ty;               modelBuffer[14] = tz;               modelBuffer[15] = 1f
        return modelBuffer
    }
}
