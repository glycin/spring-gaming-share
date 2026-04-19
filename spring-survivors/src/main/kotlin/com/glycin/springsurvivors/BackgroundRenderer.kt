package com.glycin.springsurvivors

import com.glycin.image.SpringGameImage
import com.glycin.springsurvivors.grid.GameGrid
import com.glycin.springsurvivors.rhythm.BeatCosmeticEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage

private const val TINT_ALPHA = 0.24f
private const val FREEZE_TINT_ALPHA = 0.5f
private val TINT_RED = Color(200, 80, 80)
private val TINT_BLUE = Color(80, 80, 200)
private val TINT_GREY = Color(120, 120, 120)

@Component
class BackgroundRenderer(
    private val gameGrid: GameGrid,
    private val gameState: GameState,
) {

    private val backgrounds: Array<BufferedImage> = run {
        val tile1 = SpringGameImage("sprites/tile_1.png").image
        val tile2 = SpringGameImage("sprites/tile_2.png").image
        val tileSize = gameGrid.tileSize
        val width = gameGrid.cols * tileSize
        val height = gameGrid.rows * tileSize

        Array(2) { phase ->
            val img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val g = img.createGraphics()
            for (row in 0..<gameGrid.rows) {
                for (col in 0..<gameGrid.cols) {
                    val isTile1 = (row + col) % 2 == 0
                    val tile = if (isTile1) tile1 else tile2
                    g.drawImage(tile, col * tileSize, row * tileSize, tileSize, tileSize, null)

                    val redTile = if (phase == 0) isTile1 else !isTile1
                    val tint = if (redTile) TINT_RED else TINT_BLUE
                    g.composite = AlphaComposite.SrcOver.derive(TINT_ALPHA)
                    g.color = tint
                    g.fillRect(col * tileSize, row * tileSize, tileSize, tileSize)
                    g.composite = AlphaComposite.SrcOver
                }
            }
            g.dispose()
            img
        }
    }

    private val frozenBackground: BufferedImage = run {
        val tile1 = SpringGameImage("sprites/tile_1.png").image
        val tile2 = SpringGameImage("sprites/tile_2.png").image
        val tileSize = gameGrid.tileSize
        val width = gameGrid.cols * tileSize
        val height = gameGrid.rows * tileSize

        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        for (row in 0..<gameGrid.rows) {
            for (col in 0..<gameGrid.cols) {
                val tile = if ((row + col) % 2 == 0) tile1 else tile2
                g.drawImage(tile, col * tileSize, row * tileSize, tileSize, tileSize, null)

                g.composite = AlphaComposite.SrcOver.derive(FREEZE_TINT_ALPHA)
                g.color = TINT_GREY
                g.fillRect(col * tileSize, row * tileSize, tileSize, tileSize)
                g.composite = AlphaComposite.SrcOver
            }
        }
        g.dispose()
        img
    }

    private var bgIndex = 0

    @EventListener
    fun onBeat(event: BeatCosmeticEvent) {
        bgIndex = (bgIndex + 1) % backgrounds.size
    }

    fun render(g: Graphics2D) {
        val bg = if (gameState.frozen) frozenBackground else backgrounds[bgIndex]
        g.drawImage(bg, 0, 0, null)
    }
}
