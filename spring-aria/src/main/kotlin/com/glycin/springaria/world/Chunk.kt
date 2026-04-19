package com.glycin.springaria.world

import com.glycin.springaria.world.WorldConstants.CHUNK_PIXEL_HEIGHT
import com.glycin.springaria.world.WorldConstants.CHUNK_PIXEL_WIDTH
import com.glycin.springaria.world.WorldConstants.CHUNK_SIZE
import com.glycin.springaria.world.WorldConstants.CHUNK_TILE_COUNT
import com.glycin.springaria.world.WorldConstants.TILE_SIZE
import com.glycin.springaria.world.repositories.Tile
import com.glycin.springaria.world.repositories.toTile
import java.awt.Color
import java.awt.image.BufferedImage

class Chunk(
    val chunkX: Int,
    val chunkY: Int
) {

    @PublishedApi
    internal val tiles = ByteArray(CHUNK_TILE_COUNT) { Tile.AIR.ordinal.toByte() }

    var liquidTileCount: Int = 0
        private set

    val hasLiquid: Boolean get() = liquidTileCount > 0

    private var cachedImage: BufferedImage? = null

    var isDirty: Boolean = true

    fun getTileAt(localX: Int, localY: Int): Tile {
        return tiles[index(localX, localY)].toTile()
    }

    fun getTileOrNull(localX: Int, localY: Int): Tile? {
        if (localX !in 0..<CHUNK_SIZE || localY !in 0..<CHUNK_SIZE) return null
        return tiles[index(localX, localY)].toTile()
    }

    fun setTileAt(localX: Int, localY: Int, tile: Tile) {
        val idx = index(localX, localY)
        val newValue = tile.ordinal.toByte()
        if (tiles[idx] != newValue) {
            if (tiles[idx].toTile().isLiquid) liquidTileCount--
            if (tile.isLiquid) liquidTileCount++
            tiles[idx] = newValue
            isDirty = true
        }
    }

    fun fillWith(tile: Tile) {
        tiles.fill(tile.ordinal.toByte())
        liquidTileCount = if (tile.isLiquid) CHUNK_TILE_COUNT else 0
        isDirty = true
    }

    fun markClean() {
        isDirty = false
    }

    inline fun forEach(action: (localX: Int, localY: Int, tile: Tile) -> Unit) {
        for (y in 0..<CHUNK_SIZE) {
            for (x in 0..<CHUNK_SIZE) {
                action(x, y, tiles[index(x, y)].toTile())
            }
        }
    }

    fun getImage(): BufferedImage {
        cachedImage?.takeIf { !isDirty }?.let { return it }

        val image = BufferedImage(CHUNK_PIXEL_WIDTH, CHUNK_PIXEL_HEIGHT, BufferedImage.TYPE_INT_ARGB)
        val g2 = image.createGraphics()

        g2.background = Color(0, 0, 0, 0)
        g2.clearRect(0, 0, CHUNK_PIXEL_WIDTH, CHUNK_PIXEL_HEIGHT)

        forEach { localX, localY, tile ->
            if (tile == Tile.AIR) return@forEach
            g2.color = tile.color
            g2.fillRect(
                localX * TILE_SIZE,
                localY * TILE_SIZE,
                TILE_SIZE,
                TILE_SIZE,
            )
        }

        g2.dispose()
        cachedImage = image
        isDirty = false
        return image
    }

    @PublishedApi
    internal fun index(localX: Int, localY: Int): Int = localY * CHUNK_SIZE + localX
}