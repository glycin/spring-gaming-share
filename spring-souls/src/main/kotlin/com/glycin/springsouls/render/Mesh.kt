package com.glycin.springsouls.render

import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryUtil

class Mesh(
    positions: FloatArray,
    normals: FloatArray,
    texCoords: FloatArray,
    indices: IntArray,
    boneIds: IntArray? = null,
    boneWeights: FloatArray? = null,
) {
    val vao: Int = glGenVertexArrays()
    val indexCount: Int = indices.size

    private val vboPositions: Int
    private val vboNormals: Int
    private val vboTexCoords: Int
    private val ebo: Int
    private val vboBoneIds: Int
    private val vboBoneWeights: Int

    init {
        glBindVertexArray(vao)

        vboPositions = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, vboPositions)
        val posBuf = MemoryUtil.memAllocFloat(positions.size).put(positions).flip()
        glBufferData(GL_ARRAY_BUFFER, posBuf, GL_STATIC_DRAW)
        MemoryUtil.memFree(posBuf)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0)
        glEnableVertexAttribArray(0)

        vboNormals = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, vboNormals)
        val normBuf = MemoryUtil.memAllocFloat(normals.size).put(normals).flip()
        glBufferData(GL_ARRAY_BUFFER, normBuf, GL_STATIC_DRAW)
        MemoryUtil.memFree(normBuf)
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0)
        glEnableVertexAttribArray(1)

        vboTexCoords = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, vboTexCoords)
        val texBuf = MemoryUtil.memAllocFloat(texCoords.size).put(texCoords).flip()
        glBufferData(GL_ARRAY_BUFFER, texBuf, GL_STATIC_DRAW)
        MemoryUtil.memFree(texBuf)
        glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, 0)
        glEnableVertexAttribArray(2)

        if (boneIds != null && boneWeights != null) {
            vboBoneIds = glGenBuffers()
            glBindBuffer(GL_ARRAY_BUFFER, vboBoneIds)
            val boneBuf = MemoryUtil.memAllocInt(boneIds.size).put(boneIds).flip()
            glBufferData(GL_ARRAY_BUFFER, boneBuf, GL_STATIC_DRAW)
            MemoryUtil.memFree(boneBuf)
            glVertexAttribIPointer(3, 4, GL_INT, 0, 0)
            glEnableVertexAttribArray(3)

            vboBoneWeights = glGenBuffers()
            glBindBuffer(GL_ARRAY_BUFFER, vboBoneWeights)
            val weightBuf = MemoryUtil.memAllocFloat(boneWeights.size).put(boneWeights).flip()
            glBufferData(GL_ARRAY_BUFFER, weightBuf, GL_STATIC_DRAW)
            MemoryUtil.memFree(weightBuf)
            glVertexAttribPointer(4, 4, GL_FLOAT, false, 0, 0)
            glEnableVertexAttribArray(4)
        } else {
            vboBoneIds = 0
            vboBoneWeights = 0
        }

        ebo = glGenBuffers()
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo)
        val idxBuf = MemoryUtil.memAllocInt(indices.size).put(indices).flip()
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, idxBuf, GL_STATIC_DRAW)
        MemoryUtil.memFree(idxBuf)

        glBindVertexArray(0)
    }

    fun draw() {
        glBindVertexArray(vao)
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0)
        glBindVertexArray(0)
    }

    fun cleanup() {
        glDeleteBuffers(vboPositions)
        glDeleteBuffers(vboNormals)
        glDeleteBuffers(vboTexCoords)
        if (vboBoneIds != 0) glDeleteBuffers(vboBoneIds)
        if (vboBoneWeights != 0) glDeleteBuffers(vboBoneWeights)
        glDeleteBuffers(ebo)
        glDeleteVertexArrays(vao)
    }

    companion object {
        fun createCube(): Mesh {
            val positions = floatArrayOf(
                -0.5f, -0.5f,  0.5f,   0.5f, -0.5f,  0.5f,   0.5f,  0.5f,  0.5f,  -0.5f,  0.5f,  0.5f,
                 0.5f, -0.5f, -0.5f,  -0.5f, -0.5f, -0.5f,  -0.5f,  0.5f, -0.5f,   0.5f,  0.5f, -0.5f,
                -0.5f,  0.5f,  0.5f,   0.5f,  0.5f,  0.5f,   0.5f,  0.5f, -0.5f,  -0.5f,  0.5f, -0.5f,
                -0.5f, -0.5f, -0.5f,   0.5f, -0.5f, -0.5f,   0.5f, -0.5f,  0.5f,  -0.5f, -0.5f,  0.5f,
                 0.5f, -0.5f,  0.5f,   0.5f, -0.5f, -0.5f,   0.5f,  0.5f, -0.5f,   0.5f,  0.5f,  0.5f,
                -0.5f, -0.5f, -0.5f,  -0.5f, -0.5f,  0.5f,  -0.5f,  0.5f,  0.5f,  -0.5f,  0.5f, -0.5f,
            )

            val normals = floatArrayOf(
                0f, 0f, 1f,  0f, 0f, 1f,  0f, 0f, 1f,  0f, 0f, 1f,
                0f, 0f, -1f,  0f, 0f, -1f,  0f, 0f, -1f,  0f, 0f, -1f,
                0f, 1f, 0f,  0f, 1f, 0f,  0f, 1f, 0f,  0f, 1f, 0f,
                0f, -1f, 0f,  0f, -1f, 0f,  0f, -1f, 0f,  0f, -1f, 0f,
                1f, 0f, 0f,  1f, 0f, 0f,  1f, 0f, 0f,  1f, 0f, 0f,
                -1f, 0f, 0f,  -1f, 0f, 0f,  -1f, 0f, 0f,  -1f, 0f, 0f,
            )

            val texCoords = floatArrayOf(
                0f, 0f,  1f, 0f,  1f, 1f,  0f, 1f,
                0f, 0f,  1f, 0f,  1f, 1f,  0f, 1f,
                0f, 0f,  1f, 0f,  1f, 1f,  0f, 1f,
                0f, 0f,  1f, 0f,  1f, 1f,  0f, 1f,
                0f, 0f,  1f, 0f,  1f, 1f,  0f, 1f,
                0f, 0f,  1f, 0f,  1f, 1f,  0f, 1f,
            )

            val indices = intArrayOf(
                0, 1, 2,  2, 3, 0,
                4, 5, 6,  6, 7, 4,
                8, 9, 10,  10, 11, 8,
                12, 13, 14,  14, 15, 12,
                16, 17, 18,  18, 19, 16,
                20, 21, 22,  22, 23, 20,
            )

            return Mesh(positions, normals, texCoords, indices)
        }

        fun createCube(sx: Float, sy: Float, sz: Float): Mesh {
            val hx = sx / 2f; val hy = sy / 2f; val hz = sz / 2f
            val positions = floatArrayOf(
                // Front (Z+)
                -hx, -hy,  hz,   hx, -hy,  hz,   hx,  hy,  hz,  -hx,  hy,  hz,
                // Back (Z-)
                 hx, -hy, -hz,  -hx, -hy, -hz,  -hx,  hy, -hz,   hx,  hy, -hz,
                // Top (Y+)
                -hx,  hy,  hz,   hx,  hy,  hz,   hx,  hy, -hz,  -hx,  hy, -hz,
                // Bottom (Y-)
                -hx, -hy, -hz,   hx, -hy, -hz,   hx, -hy,  hz,  -hx, -hy,  hz,
                // Right (X+)
                 hx, -hy,  hz,   hx, -hy, -hz,   hx,  hy, -hz,   hx,  hy,  hz,
                // Left (X-)
                -hx, -hy, -hz,  -hx, -hy,  hz,  -hx,  hy,  hz,  -hx,  hy, -hz,
            )

            val normals = floatArrayOf(
                0f, 0f, 1f,  0f, 0f, 1f,  0f, 0f, 1f,  0f, 0f, 1f,
                0f, 0f, -1f,  0f, 0f, -1f,  0f, 0f, -1f,  0f, 0f, -1f,
                0f, 1f, 0f,  0f, 1f, 0f,  0f, 1f, 0f,  0f, 1f, 0f,
                0f, -1f, 0f,  0f, -1f, 0f,  0f, -1f, 0f,  0f, -1f, 0f,
                1f, 0f, 0f,  1f, 0f, 0f,  1f, 0f, 0f,  1f, 0f, 0f,
                -1f, 0f, 0f,  -1f, 0f, 0f,  -1f, 0f, 0f,  -1f, 0f, 0f,
            )

            // UV tiling: each face tiles based on its world-space dimensions
            val texCoords = floatArrayOf(
                // Front/Back: width=sx, height=sy
                0f, 0f,  sx, 0f,  sx, sy,  0f, sy,
                0f, 0f,  sx, 0f,  sx, sy,  0f, sy,
                // Top/Bottom: width=sx, depth=sz
                0f, 0f,  sx, 0f,  sx, sz,  0f, sz,
                0f, 0f,  sx, 0f,  sx, sz,  0f, sz,
                // Right/Left: depth=sz, height=sy
                0f, 0f,  sz, 0f,  sz, sy,  0f, sy,
                0f, 0f,  sz, 0f,  sz, sy,  0f, sy,
            )

            val indices = intArrayOf(
                0, 1, 2,  2, 3, 0,
                4, 5, 6,  6, 7, 4,
                8, 9, 10,  10, 11, 8,
                12, 13, 14,  14, 15, 12,
                16, 17, 18,  18, 19, 16,
                20, 21, 22,  22, 23, 20,
            )

            return Mesh(positions, normals, texCoords, indices)
        }

        fun createPlane(size: Float = 50f): Mesh {
            val half = size / 2f
            val positions = floatArrayOf(
                -half, 0f, -half,
                 half, 0f, -half,
                 half, 0f,  half,
                -half, 0f,  half,
            )
            val normals = floatArrayOf(
                0f, 1f, 0f,
                0f, 1f, 0f,
                0f, 1f, 0f,
                0f, 1f, 0f,
            )
            val uvScale = size / 2f
            val texCoords = floatArrayOf(
                0f, 0f,
                uvScale, 0f,
                uvScale, uvScale,
                0f, uvScale,
            )
            val indices = intArrayOf(0, 1, 2, 2, 3, 0)
            return Mesh(positions, normals, texCoords, indices)
        }

        fun createSphere(radius: Float = 0.5f, stacks: Int = 16, slices: Int = 16): Mesh {
            val positions = mutableListOf<Float>()
            val normals = mutableListOf<Float>()
            val texCoords = mutableListOf<Float>()
            val indices = mutableListOf<Int>()

            for (i in 0..stacks) {
                val phi = Math.PI * i / stacks
                val sinPhi = kotlin.math.sin(phi).toFloat()
                val cosPhi = kotlin.math.cos(phi).toFloat()
                for (j in 0..slices) {
                    val theta = 2.0 * Math.PI * j / slices
                    val sinTheta = kotlin.math.sin(theta).toFloat()
                    val cosTheta = kotlin.math.cos(theta).toFloat()

                    val nx = cosTheta * sinPhi
                    val ny = cosPhi
                    val nz = sinTheta * sinPhi
                    positions.addAll(listOf(nx * radius, ny * radius, nz * radius))
                    normals.addAll(listOf(nx, ny, nz))
                    texCoords.addAll(listOf(j.toFloat() / slices, i.toFloat() / stacks))
                }
            }

            for (i in 0..<stacks) {
                for (j in 0..<slices) {
                    val a = i * (slices + 1) + j
                    val b = a + slices + 1
                    indices.addAll(listOf(a, b, a + 1, b, b + 1, a + 1))
                }
            }

            return Mesh(
                positions.toFloatArray(), normals.toFloatArray(),
                texCoords.toFloatArray(), indices.toIntArray(),
            )
        }
    }
}
