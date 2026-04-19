package com.glycin.springsurvivors.xp

import com.glycin.annotations.GameManager
import com.glycin.annotations.Renderer
import com.glycin.springsurvivors.GameSettings
import com.glycin.springsurvivors.player.Player
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D

private val XP_FONT = Font("Monospaced", Font.BOLD, 12)
private val XP_BAR_BG = Color(20, 20, 40, 200)
private val XP_BAR_FILL = Color(50, 120, 255)
private val XP_BAR_BORDER = Color(100, 160, 255)

private const val BAR_HEIGHT = 12
private const val PADDING = 4

@GameManager
class XpBarRenderer(
    private val player: Player,
    gameSettings: GameSettings,
) {

    private val screenWidth = gameSettings.windowWidth

    @Renderer
    fun render(g: Graphics2D) {
        g.color = XP_BAR_BG
        g.fillRect(0, 0, screenWidth, BAR_HEIGHT + PADDING * 2)

        g.color = XP_BAR_FILL
        g.fillRect(PADDING, PADDING, ((screenWidth - PADDING * 2) * player.levelProgress).toInt(), BAR_HEIGHT)

        g.color = XP_BAR_BORDER
        g.drawRect(PADDING, PADDING, screenWidth - PADDING * 2 - 1, BAR_HEIGHT)

        if (player.level > 0) {
            g.font = XP_FONT
            g.color = Color.WHITE
            g.drawString("Lv ${player.level}", screenWidth / 2 - 15, BAR_HEIGHT + PADDING - 2)
        }
    }
}
