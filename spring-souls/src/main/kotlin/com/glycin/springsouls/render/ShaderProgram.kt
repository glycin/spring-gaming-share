package com.glycin.springsouls.render

import org.lwjgl.opengl.GL33.*

class ShaderProgram(vertexSource: String, fragmentSource: String) {

    val id: Int
    private val uniformCache = HashMap<String, Int>()

    init {
        val vertexShader = compileShader(vertexSource, GL_VERTEX_SHADER)
        val fragmentShader = compileShader(fragmentSource, GL_FRAGMENT_SHADER)

        id = glCreateProgram()
        glAttachShader(id, vertexShader)
        glAttachShader(id, fragmentShader)
        glLinkProgram(id)

        if (glGetProgrami(id, GL_LINK_STATUS) == GL_FALSE) {
            val log = glGetProgramInfoLog(id)
            glDeleteProgram(id)
            error("Shader program linking failed:\n$log")
        }

        glDeleteShader(vertexShader)
        glDeleteShader(fragmentShader)
    }

    fun use() {
        glUseProgram(id)
    }

    fun setMatrix4(name: String, matrix: FloatArray) {
        glUniformMatrix4fv(getUniform(name), false, matrix)
    }

    fun setVec2(name: String, x: Float, y: Float) {
        glUniform2f(getUniform(name), x, y)
    }

    fun setVec3(name: String, x: Float, y: Float, z: Float) {
        glUniform3f(getUniform(name), x, y, z)
    }

    fun setVec4(name: String, x: Float, y: Float, z: Float, w: Float) {
        glUniform4f(getUniform(name), x, y, z, w)
    }

    fun setFloat(name: String, value: Float) {
        glUniform1f(getUniform(name), value)
    }

    fun setInt(name: String, value: Int) {
        glUniform1i(getUniform(name), value)
    }

    fun setBool(name: String, value: Boolean) {
        glUniform1i(getUniform(name), if (value) 1 else 0)
    }

    fun setMatrix4Array(name: String, flatMatrices: FloatArray) {
        glUniformMatrix4fv(getUniform("${name}[0]"), false, flatMatrices)
    }

    fun cleanup() {
        glDeleteProgram(id)
    }

    private fun getUniform(name: String): Int =
        uniformCache.getOrPut(name) { glGetUniformLocation(id, name) }

    companion object {
        fun fromResources(vertexPath: String, fragmentPath: String): ShaderProgram {
            val vertexSource = readResource(vertexPath)
            val fragmentSource = readResource(fragmentPath)
            return ShaderProgram(vertexSource, fragmentSource)
        }

        private fun readResource(path: String): String {
            val stream = ShaderProgram::class.java.classLoader.getResourceAsStream(path)
                ?: error("Shader resource not found: $path")
            return stream.bufferedReader().use { it.readText() }
        }

        private fun compileShader(source: String, type: Int): Int {
            val shader = glCreateShader(type)
            glShaderSource(shader, source)
            glCompileShader(shader)

            if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
                val log = glGetShaderInfoLog(shader)
                glDeleteShader(shader)
                val typeName = if (type == GL_VERTEX_SHADER) "vertex" else "fragment"
                error("$typeName shader compilation failed:\n$log")
            }

            return shader
        }
    }
}
