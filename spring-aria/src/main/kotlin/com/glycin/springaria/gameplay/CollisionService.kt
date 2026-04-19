package com.glycin.springaria.gameplay

import com.glycin.springaria.world.World
import com.glycin.springaria.world.WorldConstants.TILE_SIZE
import com.glycin.springaria.world.repositories.Tile
import org.springframework.stereotype.Service

@Service
class CollisionService(
    private val world: World,
) {

    fun collidesAt(px: Double, py: Double, widthInPixels: Int, heightInPixels: Int): Boolean =
        anyTileMatches(px, py, widthInPixels, heightInPixels) { it.solid }

    fun isInLava(px: Double, py: Double, widthInPixels: Int, heightInPixels: Int): Boolean =
        anyTileMatches(px, py, widthInPixels, heightInPixels) { it == Tile.LAVA }

    private inline fun anyTileMatches(
        px: Double, py: Double, widthInPixels: Int, heightInPixels: Int,
        predicate: (Tile) -> Boolean,
    ): Boolean {
        val leftTile = (px / TILE_SIZE).toInt()
        val rightTile = ((px + widthInPixels - 1) / TILE_SIZE).toInt()
        val topTile = (py / TILE_SIZE).toInt()
        val bottomTile = ((py + heightInPixels - 1) / TILE_SIZE).toInt()

        for (ty in topTile..bottomTile) {
            for (tx in leftTile..rightTile) {
                if (predicate(world[tx, ty])) return true
            }
        }
        return false
    }
}
