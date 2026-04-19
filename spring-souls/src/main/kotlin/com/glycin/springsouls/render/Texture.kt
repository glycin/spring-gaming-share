package com.glycin.springsouls.render

import org.lwjgl.assimp.AITexture
import org.lwjgl.opengl.GL33.*
import org.lwjgl.stb.STBImage.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

class Texture(val id: Int) {

    fun bind(unit: Int = 0) {
        glActiveTexture(GL_TEXTURE0 + unit)
        glBindTexture(GL_TEXTURE_2D, id)
    }

    fun cleanup() {
        glDeleteTextures(id)
    }

    companion object {

        fun fromResource(path: String): Texture {
            val stream = Texture::class.java.classLoader.getResourceAsStream(path)
                ?: error("Texture resource not found: $path")
            val bytes = stream.use { it.readBytes() }
            val buf = MemoryUtil.memAlloc(bytes.size).put(bytes).flip()
            val texture = fromCompressed(buf)
            MemoryUtil.memFree(buf)
            return texture
        }

        fun fromEmbedded(aiTexture: AITexture): Texture {
            val width = aiTexture.mWidth()
            val height = aiTexture.mHeight()

            return if (height == 0) {
                val texelBuffer = aiTexture.pcData()
                val compressedData = MemoryUtil.memByteBuffer(texelBuffer.address(), width)
                fromCompressed(compressedData)
            } else {
                val texelCount = width * height
                val texelBuffer = aiTexture.pcData()
                val pixels = MemoryUtil.memAlloc(texelCount * 4)
                for (i in 0..<texelCount) {
                    val texel = texelBuffer.get(i)
                    pixels.put(texel.r())
                    pixels.put(texel.g())
                    pixels.put(texel.b())
                    pixels.put(texel.a())
                }
                pixels.flip()
                val texture = fromRGBA(pixels, width, height)
                MemoryUtil.memFree(pixels)
                texture
            }
        }

        private fun fromCompressed(compressedData: ByteBuffer): Texture {
            MemoryStack.stackPush().use { stack ->
                val w = stack.mallocInt(1)
                val h = stack.mallocInt(1)
                val ch = stack.mallocInt(1)
                val pixels = stbi_load_from_memory(compressedData, w, h, ch, 4)
                    ?: error("Failed to decode embedded texture: ${stbi_failure_reason()}")
                val texture = fromRGBA(pixels, w.get(0), h.get(0))
                stbi_image_free(pixels)
                return texture
            }
        }

        private fun fromRGBA(pixels: ByteBuffer, width: Int, height: Int): Texture {
            val texId = glGenTextures()
            glBindTexture(GL_TEXTURE_2D, texId)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels)
            glGenerateMipmap(GL_TEXTURE_2D)
            return Texture(texId)
        }
    }
}
