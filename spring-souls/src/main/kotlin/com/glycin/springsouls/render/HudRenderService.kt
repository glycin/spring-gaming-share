package com.glycin.springsouls.render

import com.glycin.springsouls.GameSettings
import org.lwjgl.opengl.GL33.*
import org.lwjgl.stb.STBTTAlignedQuad
import org.lwjgl.stb.STBTTBakedChar
import org.lwjgl.stb.STBTruetype.stbtt_BakeFontBitmap
import org.lwjgl.stb.STBTruetype.stbtt_GetBakedQuad
import org.lwjgl.system.MemoryStack
import org.springframework.stereotype.Service
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val FONT_ATLAS_SIZE = 512
private const val FONT_HEIGHT = 32f
private const val FIRST_CHAR = 32
private const val NUM_CHARS = 96

@Service
class HudRenderService(
    private val gameSettings: GameSettings,
) {

    private var shader: ShaderProgram? = null
    private var quadVao: Int = 0
    private var quadVbo: Int = 0
    private var quadEbo: Int = 0
    private val orthoMatrix = FloatArray(16)
    var menuOpen = false

    private var fontTextureId: Int = 0
    private var charData: STBTTBakedChar.Buffer? = null
    private val deathScreenException = listOf("NoSuchBeanDefinitionException", "ApplicationContextException", "BeanCreationException", "NonTransientDataAccessException", "NullPointerException").random()
    @Volatile var notificationText: String? = null
    @Volatile var notificationTimeLeft = 0f
    @Volatile var notificationR = 1f
    @Volatile var notificationG = 1f
    @Volatile var notificationB = 1f

    fun showNotification(text: String, durationSeconds: Float = 3f, r: Float = 1f, g: Float = 1f, b: Float = 1f) {
        notificationText = text
        notificationTimeLeft = durationSeconds
        notificationR = r
        notificationG = g
        notificationB = b
    }

    fun updateNotification(deltaSeconds: Float) {
        if (notificationTimeLeft > 0f) {
            notificationTimeLeft -= deltaSeconds
            if (notificationTimeLeft <= 0f) {
                notificationText = null
            }
        }
        if (playerDead) {
            deathScreenAlpha = (deathScreenAlpha + deltaSeconds * 0.4f).coerceAtMost(1f)
        }
    }

    @Volatile var hpPercent = 1f
    @Volatile var hpStatus = "UP"
    @Volatile var staminaPercent = 1f
    @Volatile var staminaStatus = "UP"
    @Volatile var potionCount = 0
    @Volatile var bossHpPercent = 1f
    @Volatile var showBossHp = false
    @Volatile var playerDead = false
    private var deathScreenAlpha = 0f

    fun init() {
        shader = ShaderProgram.fromResources("shaders/ui_vertex.glsl", "shaders/ui_fragment.glsl")

        val vertices = floatArrayOf(
            0f, 0f,
            1f, 0f,
            1f, 1f,
            0f, 1f,
        )
        val indices = intArrayOf(0, 1, 2, 2, 3, 0)

        quadVao = glGenVertexArrays()
        glBindVertexArray(quadVao)

        quadVbo = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, quadVbo)
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW)
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0)
        glEnableVertexAttribArray(0)

        quadEbo = glGenBuffers()
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, quadEbo)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW)

        glBindVertexArray(0)

        val w = gameSettings.windowWidth.toFloat()
        val h = gameSettings.windowHeight.toFloat()
        computeOrtho(0f, w, h, 0f)

        initFont()
    }

    private fun initFont() {
        val fontBytes = javaClass.classLoader.getResourceAsStream("font/Pixeled.ttf")!!.use { it.readBytes() }
        val fontBuffer = ByteBuffer.allocateDirect(fontBytes.size).order(ByteOrder.nativeOrder())
        fontBuffer.put(fontBytes).flip()

        charData = STBTTBakedChar.malloc(NUM_CHARS)
        val bitmap = ByteBuffer.allocateDirect(FONT_ATLAS_SIZE * FONT_ATLAS_SIZE).order(ByteOrder.nativeOrder())

        stbtt_BakeFontBitmap(fontBuffer, FONT_HEIGHT, bitmap, FONT_ATLAS_SIZE, FONT_ATLAS_SIZE, FIRST_CHAR, charData!!)

        fontTextureId = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, fontTextureId)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, FONT_ATLAS_SIZE, FONT_ATLAS_SIZE, 0, GL_RED, GL_UNSIGNED_BYTE, bitmap)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
    }

    fun renderMainMenuText() {
        val s = shader ?: return
        glDisable(GL_DEPTH_TEST)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        s.use()

        val w = gameSettings.windowWidth.toFloat()
        val h = gameSettings.windowHeight.toFloat()
        s.setMatrix4("projection", orthoMatrix)
        s.setBool("useTexture", false)

        glBindVertexArray(quadVao)

        val titleScale = 3.5f
        val subtitleScale = 2.0f
        val promptScale = 1.8f

        val titleX = w * 0.35f
        val titleY = h * 0.25f

        drawText(s, "SPRING SOULS", titleX, titleY, titleScale,
            0.9f, 0.85f, 0.7f, 1f)
        drawText(s, "PREPARE TO ENTERPRISE EDITION", titleX, titleY + FONT_HEIGHT * titleScale + 12f, subtitleScale,
            0.6f, 0.55f, 0.45f, 1f)

        val promptText = "Press Enter to Start"
        val promptW = measureText(promptText, promptScale)
        drawText(s, promptText, w - promptW - 30f, h - 80f, promptScale,
            0.7f, 0.7f, 0.7f, 1f)

        glBindVertexArray(0)
        glDisable(GL_BLEND)
        glEnable(GL_DEPTH_TEST)
    }

    fun render() {
        val s = shader ?: return
        glDisable(GL_DEPTH_TEST)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        s.use()

        val w = gameSettings.windowWidth.toFloat()
        val h = gameSettings.windowHeight.toFloat()
        s.setMatrix4("projection", orthoMatrix)
        s.setBool("useTexture", false)

        glBindVertexArray(quadVao)

        if (deathScreenAlpha > 0f) {
            renderDeathScreen(s, w, h)
        } else {
            renderHealthBar(s, w, h)
            renderStaminaBar(s, w, h)
            renderPotions(s, w, h)

            if (showBossHp) {
                renderBossHealthBar(s, w, h)
            }

            if (notificationText != null) {
                renderNotification(s, w, h)
            }

            if (menuOpen) {
                renderMenu(s, w, h)
            }
        }

        glBindVertexArray(0)
        glDisable(GL_BLEND)
        glEnable(GL_DEPTH_TEST)
    }

    private fun renderHealthBar(s: ShaderProgram, w: Float, h: Float) {
        val barX = 20f
        val barY = 20f
        val barW = 250f
        val barH = 16f

        val (r, g, b) = when (hpStatus) {
            "DOWN" -> Triple(0.4f, 0.0f, 0.0f)
            "OUT_OF_SERVICE" -> Triple(0.8f, 0.3f, 0.0f)
            else -> Triple(0.7f, 0.1f, 0.1f)
        }

        drawRect(s, barX, barY, barW, barH, 0.15f, 0.15f, 0.15f, 0.8f)
        val fillW = barW * hpPercent
        drawRect(s, barX, barY, fillW, barH, r, g, b, 1f)
        drawRectOutline(s, barX, barY, barW, barH, 0.6f, 0.6f, 0.6f, 1f)
    }

    private fun renderStaminaBar(s: ShaderProgram, w: Float, h: Float) {
        val barX = 20f
        val barY = 42f
        val barW = 200f
        val barH = 10f

        val (r, g, b) = when (staminaStatus) {
            "OUT_OF_SERVICE" -> Triple(0.5f, 0.5f, 0.0f)
            else -> Triple(0.2f, 0.6f, 0.2f)
        }

        drawRect(s, barX, barY, barW, barH, 0.15f, 0.15f, 0.15f, 0.8f)
        val fillW = barW * staminaPercent
        drawRect(s, barX, barY, fillW, barH, r, g, b, 1f)
        drawRectOutline(s, barX, barY, barW, barH, 0.6f, 0.6f, 0.6f, 1f)
    }

    private fun renderPotions(s: ShaderProgram, w: Float, h: Float) {
        val potionCount = this.potionCount
        val iconSize = 28f
        val gap = 6f
        val marginRight = 20f
        val marginBottom = 20f

        for (i in 0..<potionCount) {
            val px = w - marginRight - (potionCount - i) * (iconSize + gap)
            val py = h - marginBottom - iconSize

            drawRect(s, px, py, iconSize, iconSize, 0.1f, 0.1f, 0.1f, 0.7f)
            val innerPad = 4f
            drawRect(s, px + innerPad, py + innerPad, iconSize - innerPad * 2, iconSize - innerPad * 2,
                0.9f, 0.5f, 0.1f, 1f)
            drawRectOutline(s, px, py, iconSize, iconSize, 0.6f, 0.6f, 0.6f, 1f)
        }
    }

    private fun renderBossHealthBar(s: ShaderProgram, w: Float, h: Float) {
        val barW = w * 2f / 3f
        val barH = 20f
        val barX = (w - barW) / 2f
        val barY = h - 60f

        drawText(s, "LORD OF BEANS", w / 2f, barY - FONT_HEIGHT * 1.6f - 4f, 1.6f,
            0.9f, 0.8f, 0.7f, 1f, centered = true)

        drawRect(s, barX, barY, barW, barH, 0.15f, 0.15f, 0.15f, 0.8f)
        val fillW = barW * bossHpPercent
        drawRect(s, barX, barY, fillW, barH, 0.8f, 0.1f, 0.1f, 1f)
        drawRectOutline(s, barX, barY, barW, barH, 0.7f, 0.7f, 0.7f, 1f)
    }

    private fun renderDeathScreen(s: ShaderProgram, w: Float, h: Float) {
        drawRect(s, 0f, 0f, w, h, 0f, 0f, 0f, deathScreenAlpha)

        val textAlpha = ((deathScreenAlpha - 0.3f) / 0.7f).coerceIn(0f, 1f)
        if (textAlpha > 0f) {
            val text = deathScreenException
            val scale = 2.8f
            drawText(s, text, w / 2f, h / 2f - FONT_HEIGHT * scale / 2f, scale,
                0.8f, 0.1f, 0.05f, textAlpha, centered = true)
        }
    }

    private fun renderMenu(s: ShaderProgram, w: Float, h: Float) {
        drawRect(s, 0f, 0f, w, h, 0f, 0f, 0f, 0.6f)

        val panelW = 300f
        val panelH = 200f
        val px = (w - panelW) / 2f
        val py = (h - panelH) / 2f

        drawRect(s, px, py, panelW, panelH, 0.1f, 0.1f, 0.12f, 0.9f)
        drawRectOutline(s, px, py, panelW, panelH, 0.5f, 0.5f, 0.5f, 1f)

        drawText(s, "MENU", px + panelW / 2f, py + 30f, 0.8f, 0.8f, 0.8f, 0.8f, 1f, centered = true)

        drawRect(s, px + 20f, py + 50f, panelW - 40f, 1f, 0.4f, 0.4f, 0.4f, 1f)

        drawText(s, "RESUME", px + panelW / 2f, py + 80f, 0.7f, 0.6f, 0.6f, 0.6f, 0.8f, centered = true)
        drawText(s, "QUIT", px + panelW / 2f, py + 110f, 0.7f, 0.6f, 0.6f, 0.6f, 0.8f, centered = true)
    }

    private fun renderNotification(s: ShaderProgram, w: Float, h: Float) {
        val text = notificationText ?: return
        val alpha = (notificationTimeLeft / 0.5f).coerceIn(0f, 1f)
        if (alpha <= 0f) return

        val scale = 1.6f
        val textW = measureText(text, scale)
        val textH = FONT_HEIGHT * scale
        val startX = (w - textW) / 2f
        val startY = h * 0.35f

        val padX = 16f
        val padY = 10f
        drawRect(s, startX - padX, startY - padY, textW + padX * 2, textH + padY * 2,
            0.05f, 0.05f, 0.08f, 0.85f * alpha)
        drawRectOutline(s, startX - padX, startY - padY, textW + padX * 2, textH + padY * 2,
            notificationR * 0.6f, notificationG * 0.6f, notificationB * 0.6f, alpha)

        drawText(s, text, startX, startY, scale, notificationR, notificationG, notificationB, alpha)
    }

    private fun measureText(text: String, scale: Float): Float {
        val cd = charData ?: return 0f
        MemoryStack.stackPush().use { stack ->
            val xBuf = stack.floats(0f)
            val yBuf = stack.floats(0f)
            val quad = STBTTAlignedQuad.malloc(stack)
            for (ch in text) {
                val cp = ch.code
                if (cp < FIRST_CHAR || cp >= FIRST_CHAR + NUM_CHARS) continue
                stbtt_GetBakedQuad(cd, FONT_ATLAS_SIZE, FONT_ATLAS_SIZE, cp - FIRST_CHAR, xBuf, yBuf, quad, true)
            }
            return xBuf[0] * scale
        }
    }

    private fun drawText(
        s: ShaderProgram, text: String, x: Float, y: Float, scale: Float,
        r: Float, g: Float, b: Float, a: Float, centered: Boolean = false,
    ) {
        val cd = charData ?: return
        val startX = if (centered) x - measureText(text, scale) / 2f else x

        s.setBool("useTexture", true)
        s.setInt("fontTexture", 0)
        s.setVec4("color", r, g, b, a)
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, fontTextureId)

        MemoryStack.stackPush().use { stack ->
            val xBuf = stack.floats(0f)
            val yBuf = stack.floats(0f)
            val quad = STBTTAlignedQuad.malloc(stack)

            for (ch in text) {
                val cp = ch.code
                if (cp < FIRST_CHAR || cp >= FIRST_CHAR + NUM_CHARS) continue

                stbtt_GetBakedQuad(cd, FONT_ATLAS_SIZE, FONT_ATLAS_SIZE, cp - FIRST_CHAR, xBuf, yBuf, quad, true)

                val qx = startX + quad.x0() * scale
                val qy = y + quad.y0() * scale + FONT_HEIGHT * scale
                val qw = (quad.x1() - quad.x0()) * scale
                val qh = (quad.y1() - quad.y0()) * scale

                s.setVec2("uvOffset", quad.s0(), quad.t0())
                s.setVec2("uvSize", quad.s1() - quad.s0(), quad.t1() - quad.t0())
                s.setVec2("offset", qx, qy)
                s.setVec2("size", qw, qh)

                glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0)
            }
        }

        s.setBool("useTexture", false)
    }

    private fun drawRect(s: ShaderProgram, x: Float, y: Float, w: Float, h: Float,
                          r: Float, g: Float, b: Float, a: Float) {
        s.setVec2("offset", x, y)
        s.setVec2("size", w, h)
        s.setVec4("color", r, g, b, a)
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0)
    }

    private fun drawRectOutline(s: ShaderProgram, x: Float, y: Float, w: Float, h: Float,
                                 r: Float, g: Float, b: Float, a: Float) {
        val t = 1f
        drawRect(s, x, y, w, t, r, g, b, a)
        drawRect(s, x, y + h - t, w, t, r, g, b, a)
        drawRect(s, x, y, t, h, r, g, b, a)
        drawRect(s, x + w - t, y, t, h, r, g, b, a)
    }

    private fun computeOrtho(left: Float, right: Float, bottom: Float, top: Float) {
        orthoMatrix.fill(0f)
        orthoMatrix[0] = 2f / (right - left)
        orthoMatrix[5] = 2f / (top - bottom)
        orthoMatrix[10] = -1f
        orthoMatrix[12] = -(right + left) / (right - left)
        orthoMatrix[13] = -(top + bottom) / (top - bottom)
        orthoMatrix[15] = 1f
    }

    fun cleanup() {
        glDeleteBuffers(quadVbo)
        glDeleteBuffers(quadEbo)
        glDeleteVertexArrays(quadVao)
        if (fontTextureId != 0) glDeleteTextures(fontTextureId)
        charData?.free()
        shader?.cleanup()
    }
}
