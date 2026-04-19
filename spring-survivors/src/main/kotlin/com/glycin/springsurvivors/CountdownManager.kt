package com.glycin.springsurvivors

import com.glycin.annotations.GameManager
import com.glycin.annotations.Renderer
import org.springframework.beans.factory.annotation.Value
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D

private val COUNTDOWN_FONT = Font("Monospaced", Font.BOLD, 120)
private val PUN_FONT = Font("Monospaced", Font.BOLD, 28)

@GameManager
class CountdownManager(
    private val gameSettings: GameSettings,
    @Value("#{1000 / \${game.settings.fps}}") private val deltaTime: Long,
) {

    private var remainingMs = 5000L

    val isCountingDown: Boolean get() = remainingMs > 0

    fun tick() {
        if (remainingMs > 0) {
            remainingMs -= deltaTime
        }
    }

    @Renderer
    fun render(g: Graphics2D) {
        if (!isCountingDown) return

        val seconds = (remainingMs / 1000).toInt() + 1
        val text = seconds.toString()

        val centerX = gameSettings.windowWidth / 2
        val topY = gameSettings.windowHeight / 4

        g.font = COUNTDOWN_FONT
        val fm = g.fontMetrics
        val textWidth = fm.stringWidth(text)

        g.color = Color.BLACK
        g.drawString(text, centerX - textWidth / 2 + 3, topY + fm.ascent / 2 + 3)
        g.color = Color.WHITE
        g.drawString(text, centerX - textWidth / 2, topY + fm.ascent / 2)

        g.font = PUN_FONT
        val pfm = g.fontMetrics
        val pun = gameSettings.countdownPun
        val punWidth = pfm.stringWidth(pun)
        val punY = gameSettings.windowHeight / 2 + 80
        g.color = Color.BLACK
        g.drawString(pun, centerX - punWidth / 2 + 2, punY + 2)
        g.color = Color.WHITE
        g.drawString(pun, centerX - punWidth / 2, punY)
    }
}
