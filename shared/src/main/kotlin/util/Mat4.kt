package com.glycin.util

class Mat4(val data: FloatArray = FloatArray(16)) {

    operator fun times(other: Mat4): Mat4 {
        val result = FloatArray(16)
        for (col in 0..3) {
            for (row in 0..3) {
                var sum = 0f
                for (k in 0..3) {
                    sum += data[k * 4 + row] * other.data[col * 4 + k]
                }
                result[col * 4 + row] = sum
            }
        }
        return Mat4(result)
    }

    fun toFloatArray(): FloatArray = data

    fun inverse(): Mat4 {
        val d = data
        val inv = FloatArray(16)

        inv[0]  =  d[5]*d[10]*d[15] - d[5]*d[11]*d[14] - d[9]*d[6]*d[15] + d[9]*d[7]*d[14] + d[13]*d[6]*d[11] - d[13]*d[7]*d[10]
        inv[4]  = -d[4]*d[10]*d[15] + d[4]*d[11]*d[14] + d[8]*d[6]*d[15] - d[8]*d[7]*d[14] - d[12]*d[6]*d[11] + d[12]*d[7]*d[10]
        inv[8]  =  d[4]*d[9]*d[15]  - d[4]*d[11]*d[13] - d[8]*d[5]*d[15] + d[8]*d[7]*d[13] + d[12]*d[5]*d[11] - d[12]*d[7]*d[9]
        inv[12] = -d[4]*d[9]*d[14]  + d[4]*d[10]*d[13] + d[8]*d[5]*d[14] - d[8]*d[6]*d[13] - d[12]*d[5]*d[10] + d[12]*d[6]*d[9]
        inv[1]  = -d[1]*d[10]*d[15] + d[1]*d[11]*d[14] + d[9]*d[2]*d[15] - d[9]*d[3]*d[14] - d[13]*d[2]*d[11] + d[13]*d[3]*d[10]
        inv[5]  =  d[0]*d[10]*d[15] - d[0]*d[11]*d[14] - d[8]*d[2]*d[15] + d[8]*d[3]*d[14] + d[12]*d[2]*d[11] - d[12]*d[3]*d[10]
        inv[9]  = -d[0]*d[9]*d[15]  + d[0]*d[11]*d[13] + d[8]*d[1]*d[15] - d[8]*d[3]*d[13] - d[12]*d[1]*d[11] + d[12]*d[3]*d[9]
        inv[13] =  d[0]*d[9]*d[14]  - d[0]*d[10]*d[13] - d[8]*d[1]*d[14] + d[8]*d[2]*d[13] + d[12]*d[1]*d[10] - d[12]*d[2]*d[9]
        inv[2]  =  d[1]*d[6]*d[15]  - d[1]*d[7]*d[14]  - d[5]*d[2]*d[15] + d[5]*d[3]*d[14] + d[13]*d[2]*d[7]  - d[13]*d[3]*d[6]
        inv[6]  = -d[0]*d[6]*d[15]  + d[0]*d[7]*d[14]  + d[4]*d[2]*d[15] - d[4]*d[3]*d[14] - d[12]*d[2]*d[7]  + d[12]*d[3]*d[6]
        inv[10] =  d[0]*d[5]*d[15]  - d[0]*d[7]*d[13]  - d[4]*d[1]*d[15] + d[4]*d[3]*d[13] + d[12]*d[1]*d[7]  - d[12]*d[3]*d[5]
        inv[14] = -d[0]*d[5]*d[14]  + d[0]*d[6]*d[13]  + d[4]*d[1]*d[14] - d[4]*d[2]*d[13] - d[12]*d[1]*d[6]  + d[12]*d[2]*d[5]
        inv[3]  = -d[1]*d[6]*d[11]  + d[1]*d[7]*d[10]  + d[5]*d[2]*d[11] - d[5]*d[3]*d[10] - d[9]*d[2]*d[7]   + d[9]*d[3]*d[6]
        inv[7]  =  d[0]*d[6]*d[11]  - d[0]*d[7]*d[10]  - d[4]*d[2]*d[11] + d[4]*d[3]*d[10] + d[8]*d[2]*d[7]   - d[8]*d[3]*d[6]
        inv[11] = -d[0]*d[5]*d[11]  + d[0]*d[7]*d[9]   + d[4]*d[1]*d[11] - d[4]*d[3]*d[9]  - d[8]*d[1]*d[7]   + d[8]*d[3]*d[5]
        inv[15] =  d[0]*d[5]*d[10]  - d[0]*d[6]*d[9]   - d[4]*d[1]*d[10] + d[4]*d[2]*d[9]  + d[8]*d[1]*d[6]   - d[8]*d[2]*d[5]

        val det = d[0] * inv[0] + d[1] * inv[4] + d[2] * inv[8] + d[3] * inv[12]
        if (det == 0f) return identity()

        val invDet = 1f / det
        for (i in 0..15) inv[i] *= invDet
        return Mat4(inv)
    }

    companion object {
        fun identity(): Mat4 = Mat4(floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f,
        ))

        fun fromTranslation(v: Vec3): Mat4 = Mat4(floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            v.x, v.y, v.z, 1f,
        ))

        fun fromScale(v: Vec3): Mat4 = Mat4(floatArrayOf(
            v.x, 0f, 0f, 0f,
            0f, v.y, 0f, 0f,
            0f, 0f, v.z, 0f,
            0f, 0f, 0f, 1f,
        ))
    }
}
