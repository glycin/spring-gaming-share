package com.glycin.springsurvivors.player

import com.glycin.annotations.GameManager
import com.glycin.annotations.Renderer
import com.glycin.image.SpriteSheet
import com.glycin.springsurvivors.GameSettings
import com.glycin.springsurvivors.autosave.AutoSaveGameState
import com.glycin.springsurvivors.grid.GameGrid
import com.glycin.springsurvivors.rhythm.BeatCosmeticEvent
import org.springframework.context.event.EventListener
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage

private const val SPRITE_WIDTH = 256
private const val SPRITE_HEIGHT = 256
private const val HOP_HEIGHT = 14f
private const val GHOST_ALPHA = 0.35f

@GameManager
class PlayerRenderer(
    private val player: Player,
    private val gameGrid: GameGrid,
    private val autoSaveGameState: AutoSaveGameState,
    private val gameSettings: GameSettings,
) {

    private val frames: Array<BufferedImage> = run {
        val tileSize = gameGrid.tileSize
        SpriteSheet("sprites/player_sheet.png").getRow(SPRITE_WIDTH, SPRITE_HEIGHT).map { frame ->
            BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB).also { scaled ->
                val g = scaled.createGraphics()
                g.drawImage(frame, 0, 0, tileSize, tileSize, null)
                g.dispose()
            }
        }.toTypedArray()
    }

    private var frameIndex = 0

    @EventListener
    fun onBeat(event: BeatCosmeticEvent) {
        frameIndex = (frameIndex + 1) % frames.size
    }

    @Renderer
    fun render(g: Graphics2D) {
        val halfTile = gameGrid.tileSize / 2
        val snapshotPos = autoSaveGameState.playerGridPos

        // Ghost at snapshot position
        if (snapshotPos != player.gridPos) {
            val gx = gameGrid.toPixelX(snapshotPos.col)
            val gy = gameGrid.toPixelY(snapshotPos.row)
            val oldComposite = g.composite
            g.composite = AlphaComposite.SrcOver.derive(GHOST_ALPHA)
            val ghostFrame = frames[frameIndex]
            if (player.facingRight) {
                g.drawImage(ghostFrame, gx, gy - halfTile, null)
            } else {
                g.drawImage(ghostFrame, gx + ghostFrame.width, gy - halfTile, -ghostFrame.width, ghostFrame.height, null)
            }
            g.composite = oldComposite
        }

        // Player
        val (x, y) = gameGrid.lerpPixelPosition(
            player.previousGridPos, player.gridPos, player.lerpProgress, HOP_HEIGHT,
        )
        val frame = frames[frameIndex]
        if (player.facingRight) {
            g.drawImage(frame, x, y - halfTile, null)
        } else {
            g.drawImage(frame, x + frame.width, y - halfTile, -frame.width, frame.height, null)
        }

        // Screen-edge hit flash
        if (player.hitFlash > 0f) {
            val w = gameSettings.windowWidth
            val h = gameSettings.windowHeight
            val border = 40
            val alpha = player.hitFlash * 0.6f
            val oldComposite = g.composite
            g.composite = AlphaComposite.SrcOver.derive(alpha)
            g.color = Color.RED
            g.fillRect(0, 0, w, border)           // top
            g.fillRect(0, h - border, w, border)   // bottom
            g.fillRect(0, 0, border, h)            // left
            g.fillRect(w - border, 0, border, h)   // right
            g.composite = oldComposite
        }
    }
}
