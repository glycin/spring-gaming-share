package com.glycin.springsurvivors.rhythm

import com.glycin.annotations.GameManager
import com.glycin.annotations.Renderer
import com.glycin.springsurvivors.GameSettings
import org.springframework.context.event.EventListener
import java.awt.Color
import java.awt.Graphics2D

private const val BAR_HALF_WIDTH = 150
private const val RECT_WIDTH = 4
private const val RECT_HEIGHT = 20
private const val BEATS_TO_CENTER = 5
private const val BAR_MARGIN = 20
private val RECT_COLOR = Color(0, 200, 255)

@GameManager
class BeatBarRenderer(
    private val beatScheduler: BeatScheduler,
    private val gameSettings: GameSettings,
) {

    private val spawnTimes = mutableListOf<Long>()

    @EventListener
    fun onBeat(event: BeatCosmeticEvent) {
        spawnTimes.add(event.timestamp)
    }

    @Renderer
    fun render(g: Graphics2D) {
        val centerX = gameSettings.windowWidth / 2
        val centerY = gameSettings.windowHeight - BAR_MARGIN - 32
        val now = System.currentTimeMillis()
        val travelDurationMs = 60_000L * BEATS_TO_CENTER / beatScheduler.currentBpmValue

        spawnTimes.removeAll { now - it > travelDurationMs }

        g.color = RECT_COLOR
        for (spawnTime in spawnTimes) {
            val progress = ((now - spawnTime).toFloat() / travelDurationMs).coerceIn(0f, 1f)
            val offset = (BAR_HALF_WIDTH * progress).toInt()

            // From left toward center
            g.fillRect(centerX - BAR_HALF_WIDTH + offset - RECT_WIDTH / 2, centerY - RECT_HEIGHT / 2, RECT_WIDTH, RECT_HEIGHT)
            // From right toward center
            g.fillRect(centerX + BAR_HALF_WIDTH - offset - RECT_WIDTH / 2, centerY - RECT_HEIGHT / 2, RECT_WIDTH, RECT_HEIGHT)
        }
    }
}
