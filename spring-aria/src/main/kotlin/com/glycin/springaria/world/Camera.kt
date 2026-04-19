package com.glycin.springaria.world

import com.glycin.springaria.GameSettings
import com.glycin.springaria.world.WorldConstants.TILE_SIZE
import org.springframework.stereotype.Component

@Component
class Camera(
    private val gameSettings: GameSettings,
) {

    var x: Double = 0.0
    var y: Double = 0.0
    var zoom: Double = 2.0

    val viewportWidthInPixels get() = (gameSettings.windowWidth / zoom).toInt()
    val viewportHeightInPixels get() = (gameSettings.windowHeight / zoom).toInt()

    val left get() = x - viewportWidthInPixels / 2.0
    val right get() = x + viewportWidthInPixels / 2.0
    val top get() = y - viewportHeightInPixels / 2.0
    val bottom get() = y + viewportHeightInPixels / 2.0

    val tileLeft get() = (left / TILE_SIZE).toInt() - 1
    val tileRight get() = (right / TILE_SIZE).toInt() + 1
    val tileTop get() = (top / TILE_SIZE).toInt() - 1
    val tileBottom get() = (bottom / TILE_SIZE).toInt() + 1

    fun centerOn(targetX: Double, targetY: Double) {
        x = targetX
        y = targetY
    }

    fun isVisible(worldX: Double, worldY: Double, width: Double, height: Double): Boolean {
        return worldX + width > left && worldX < right &&
               worldY + height > top && worldY < bottom
    }

    fun isTileVisible(tileX: Int, tileY: Int): Boolean {
        return tileX in tileLeft..tileRight && tileY in tileTop..tileBottom
    }

    fun worldToScreenX(worldX: Double): Double = (worldX - left) * zoom

    fun worldToScreenY(worldY: Double): Double = (worldY - top) * zoom

    fun screenToWorldX(screenX: Double): Double = screenX / zoom + left

    fun screenToWorldY(screenY: Double): Double = screenY / zoom + top
}
