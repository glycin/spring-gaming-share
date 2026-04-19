package com.glycin.springsurvivors

import com.glycin.annotations.GameManager
import com.glycin.annotations.Renderer
import com.glycin.annotations.WithSound
import com.glycin.springsurvivors.metrics.GameMetricsService
import com.glycin.springsurvivors.player.PlayerDiedEvent
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.EventListener
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.util.Timer
import kotlin.concurrent.schedule

private val GAME_OVER_FONT = Font("Monospaced", Font.BOLD, 72)
private val SUBTITLE_FONT = Font("Monospaced", Font.PLAIN, 20)
private const val SHUTDOWN_DELAY_MS = 5000L

@GameManager
class GameOverManager(
    private val gameState: GameState,
    private val gameSettings: GameSettings,
    private val context: ConfigurableApplicationContext,
    private val metricsService: GameMetricsService,
) {

    @EventListener
    @WithSound("sounds/death_sound.wav")
    fun onPlayerDied(event: PlayerDiedEvent) {
        if (gameState.gameOver) return
        gameState.gameOver = true
        metricsService.recordDeath()
        Timer(true).schedule(SHUTDOWN_DELAY_MS) {
            context.close()
        }
    }

    @Renderer
    fun render(g: Graphics2D) {
        if (!gameState.gameOver) return

        val centerX = gameSettings.windowWidth / 2
        val centerY = gameSettings.windowHeight / 2

        g.font = GAME_OVER_FONT
        val fm = g.fontMetrics
        val text = "GAME OVER"
        val textWidth = fm.stringWidth(text)
        g.color = Color.BLACK
        g.drawString(text, centerX - textWidth / 2 + 3, centerY + 3)
        g.color = Color.RED
        g.drawString(text, centerX - textWidth / 2, centerY)

        g.font = SUBTITLE_FONT
        val sfm = g.fontMetrics
        val sub = "Closing in 5 seconds..."
        val subWidth = sfm.stringWidth(sub)
        g.color = Color.WHITE
        g.drawString(sub, centerX - subWidth / 2, centerY + 50)
    }
}
