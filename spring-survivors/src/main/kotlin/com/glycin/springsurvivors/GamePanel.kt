package com.glycin.springsurvivors

import com.glycin.springsurvivors.player.Player
import com.glycin.springsurvivors.rhythm.BeatScheduler
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JPanel

private val COMBO_FONT = Font("Monospaced", Font.BOLD, 24)
private val BPM_FONT = Font("Monospaced", Font.PLAIN, 16)

class GamePanel(
    private val player: Player,
    private val beatScheduler: BeatScheduler,
    private val backgroundRenderer: BackgroundRenderer,
    private val gameRenderExecutor: GameRenderExecutor,
) : JPanel() {

    init {
        background = Color.BLACK
        isDoubleBuffered = true
        isFocusable = true
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        backgroundRenderer.render(g2d)
        gameRenderExecutor.executeAll(g2d)
        renderHud(g2d)
    }

    private fun renderHud(g: Graphics2D) {
        val combo = player.combo
        if (combo > 0) {
            g.font = COMBO_FONT
            g.color = Color.GREEN
            g.drawString("COMBO x$combo", 20, 40)
        }

        g.font = BPM_FONT
        g.color = Color.WHITE
        g.drawString("BPM: ${beatScheduler.currentBpmValue}", width - 120, 30)
    }
}
