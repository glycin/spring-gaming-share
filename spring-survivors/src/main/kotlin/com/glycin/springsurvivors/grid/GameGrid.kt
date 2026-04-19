package com.glycin.springsurvivors.grid

import com.glycin.extensions.lerp
import com.glycin.springsurvivors.GameSettings
import com.glycin.util.GridPos
import org.springframework.stereotype.Component
import kotlin.math.PI
import kotlin.math.sin

@Component
class GameGrid(
    gameSettings: GameSettings,
) {

    val rows = gameSettings.gridSize
    val tileSize = gameSettings.windowHeight / rows
    val cols = (gameSettings.windowWidth + tileSize - 1) / tileSize

    fun toPixelX(col: Int): Int = col * tileSize

    fun toPixelY(row: Int): Int = row * tileSize

    fun toGridCol(pixelX: Float): Int = (pixelX / tileSize).toInt()

    fun toGridRow(pixelY: Float): Int = (pixelY / tileSize).toInt()

    operator fun get(pixelX: Float, pixelY: Float) = GridPos(toGridCol(pixelX), toGridRow(pixelY))
    operator fun get(col: Int, row: Int): Pair<Int, Int> {
        return toPixelX(col) to toPixelY(row)
    }

    fun isInBounds(pos: GridPos): Boolean =
        pos.col in 0..<cols && pos.row in 0..<rows

    fun lerpPixelPosition(from: GridPos, to: GridPos, t: Float, hopHeight: Float): Pair<Int, Int> {
        val fromX = (from.col * tileSize).toFloat()
        val fromY = (from.row * tileSize).toFloat()
        val toX = (to.col * tileSize).toFloat()
        val toY = (to.row * tileSize).toFloat()

        val x = fromX.lerp(toX, t).toInt()
        val baseY = fromY.lerp(toY, t).toInt()
        val hop = if (from.col != to.col && t < 1f) (sin(t * PI) * hopHeight).toInt() else 0

        return x to baseY - hop
    }
}
