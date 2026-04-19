package com.glycin.springsurvivors.player

import com.glycin.annotations.GameManager
import com.glycin.annotations.Renderer
import com.glycin.springsurvivors.GameSettings
import com.glycin.springsurvivors.GameState
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D

private val HP_FONT = Font("Monospaced", Font.BOLD, 12)
private val HP_BAR_BG = Color(20, 20, 40, 200)
private val HP_BAR_FILL = Color(200, 40, 40)
private val HP_BAR_BORDER = Color(255, 80, 80)

private const val BAR_WIDTH = 120
private const val BAR_HEIGHT = 12
private const val PADDING = 4

@GameManager
class HpBarRenderer(
    private val player: Player,
    private val gameState: GameState,
    gameSettings: GameSettings,
) {

    private val screenHeight = gameSettings.windowHeight

    @Renderer
    fun render(g: Graphics2D) {
        val x = PADDING
        val y = screenHeight - BAR_HEIGHT - PADDING * 3

        g.color = HP_BAR_BG
        g.fillRect(x, y, BAR_WIDTH + PADDING * 2, BAR_HEIGHT + PADDING * 2)

        val fillWidth = (BAR_WIDTH * player.hp / gameState.maxHp).toInt()
        g.color = HP_BAR_FILL
        g.fillRect(x + PADDING, y + PADDING, fillWidth, BAR_HEIGHT)

        g.color = HP_BAR_BORDER
        g.drawRect(x + PADDING, y + PADDING, BAR_WIDTH, BAR_HEIGHT)

        g.font = HP_FONT
        g.color = Color.WHITE
        g.drawString("${"%.0f".format(player.hp)} / ${"%.0f".format(gameState.maxHp)}", x + PADDING + 4, y + PADDING + BAR_HEIGHT - 2)
    }
}
