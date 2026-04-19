package com.glycin.springsurvivors.xp

import com.glycin.annotations.GameManager
import com.glycin.annotations.Renderer
import com.glycin.annotations.Update
import com.glycin.extensions.lerp
import com.glycin.image.SpringGameImage
import com.glycin.springsurvivors.grid.GameGrid
import com.glycin.springsurvivors.player.Player
import com.glycin.springsurvivors.GameState
import com.glycin.util.Vec2
import org.springframework.context.event.EventListener
import java.awt.Graphics2D
import java.awt.image.BufferedImage

private const val CHASE_FACTOR = 0.12f
private const val COLLECT_DISTANCE_SQ = 16f * 16f

@GameManager
class XpManager(
    private val gameGrid: GameGrid,
    private val player: Player,
    private val gameState: GameState,
) {
    private val diamonds = mutableListOf<XpDiamond>()
    private val diamondSprite: BufferedImage

    init {
        val img = SpringGameImage("sprites/xp.png").image
        val size = gameGrid.tileSize / 3
        diamondSprite = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB).also {
            val g = it.createGraphics()
            g.drawImage(img, 0, 0, size, size, null)
            g.dispose()
        }
    }

    @EventListener
    fun onEnemyDied(event: EnemyDiedEvent) {
        val px = gameGrid.toPixelX(event.gridPos.col).toFloat() + gameGrid.tileSize / 2f
        val py = gameGrid.toPixelY(event.gridPos.row).toFloat() + gameGrid.tileSize / 2f
        diamonds.add(XpDiamond(position = Vec2(px, py), xpValue = event.xpValue))
    }

    @Update(order = 35)
    fun update() {
        val halfTile = gameGrid.tileSize / 2f
        val playerPixelX = gameGrid.toPixelX(player.gridPos.col).toFloat() + halfTile
        val playerPixelY = gameGrid.toPixelY(player.gridPos.row).toFloat() + halfTile

        val playerPos = Vec2(playerPixelX, playerPixelY)
        val iter = diamonds.iterator()
        while (iter.hasNext()) {
            val diamond = iter.next()
            val pos = diamond.position

            pos.x = pos.x.lerp(playerPixelX, CHASE_FACTOR)
            pos.y = pos.y.lerp(playerPixelY, CHASE_FACTOR)

            if (Vec2.distanceSquared(pos, playerPos) < COLLECT_DISTANCE_SQ) {
                player.gainXp((diamond.xpValue * gameState.xpGainMultiplier).toInt())
                iter.remove()
            }
        }
    }

    @Renderer
    fun render(g: Graphics2D) {
        val halfSize = diamondSprite.width / 2
        for (diamond in diamonds) {
            g.drawImage(diamondSprite, (diamond.position.x - halfSize).toInt(), (diamond.position.y - halfSize).toInt(), null)
        }
    }
}
