package com.glycin.springsurvivors.effects

import com.glycin.annotations.GameManager
import com.glycin.annotations.Renderer
import com.glycin.annotations.Update
import com.glycin.image.SpriteSheet
import com.glycin.springsurvivors.grid.GameGrid
import com.glycin.util.GridPos
import org.springframework.context.ApplicationEventPublisher
import java.awt.Graphics2D
import java.awt.image.BufferedImage

private const val FRAME_WIDTH = 71
private const val FRAME_HEIGHT = 99
private const val TOTAL_FRAMES = 9
private const val FRAMES_PER_TICK = 3
private const val SCALE = 2

@GameManager
class LightningEffect(
    private val gameGrid: GameGrid,
    private val eventPublisher: ApplicationEventPublisher,
) {

    private val frames: Array<BufferedImage> = run {
        val scaledWidth = gameGrid.tileSize * SCALE
        val scaledHeight = (scaledWidth.toFloat() / FRAME_WIDTH * FRAME_HEIGHT).toInt()
        SpriteSheet("sprites/lightning.png").getRow(FRAME_WIDTH, FRAME_HEIGHT).map { frame ->
            BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB).also { scaled ->
                val g = scaled.createGraphics()
                g.drawImage(frame, 0, 0, scaledWidth, scaledHeight, null)
                g.dispose()
            }
        }.toTypedArray()
    }

    private val strikes = mutableListOf<Strike>()

    fun strike(gridPos: GridPos) {
        strikes.add(Strike(gridPos))
    }

    @Update(order = 45)
    fun update() {
        val iter = strikes.iterator()
        while (iter.hasNext()) {
            val strike = iter.next()
            strike.tickCounter++
            if (strike.tickCounter >= FRAMES_PER_TICK) {
                strike.tickCounter = 0
                strike.frameIndex++
                if (strike.frameIndex >= TOTAL_FRAMES) {
                    eventPublisher.publishEvent(LightningFinishedEvent(strike.gridPos))
                    iter.remove()
                }
            }
        }
    }

    @Renderer
    fun render(g: Graphics2D) {
        val tileSize = gameGrid.tileSize
        for (strike in strikes) {
            val frame = frames[strike.frameIndex]
            val centerX = gameGrid.toPixelX(strike.gridPos.col) + tileSize / 2
            val centerY = gameGrid.toPixelY(strike.gridPos.row) + tileSize / 2
            g.drawImage(frame, centerX - frame.width / 2, centerY - frame.height / 2, null)
        }
    }

    private class Strike(
        val gridPos: GridPos,
        var frameIndex: Int = 0,
        var tickCounter: Int = 0,
    )
}
