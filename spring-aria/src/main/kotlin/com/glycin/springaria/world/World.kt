package com.glycin.springaria.world

import com.glycin.springaria.world.WorldConstants.CHUNK_PIXEL_HEIGHT
import com.glycin.springaria.world.WorldConstants.CHUNK_PIXEL_WIDTH
import com.glycin.springaria.world.WorldConstants.CHUNK_SIZE
import com.glycin.springaria.world.WorldConstants.WORLD_HEIGHT_TILES
import com.glycin.springaria.world.WorldConstants.WORLD_WIDTH_TILES
import com.glycin.springaria.world.repositories.Tile
import org.springframework.stereotype.Component

@Component
class World {

    private val chunks = HashMap<Long, Chunk>()
    val chunksWide = WORLD_WIDTH_TILES / CHUNK_SIZE
    val chunksHigh = WORLD_HEIGHT_TILES / CHUNK_SIZE

    init {
        for (chunkY in 0..<chunksHigh) {
            for (chunkX in 0..<chunksWide) {
                chunks[packChunkCoords(chunkX, chunkY)] = Chunk(chunkX, chunkY)
            }
        }
    }

    fun getVisibleChunks(camera: Camera): List<Chunk> = getChunksInRange(camera)

    fun getActiveChunks(camera: Camera, radiusChunks: Int): List<Chunk> =
        getChunksInRange(camera, radiusChunks) { it.hasLiquid }

    operator fun get(worldX: Int, worldY: Int): Tile {
        val chunkX = worldX / CHUNK_SIZE
        val chunkY = worldY / CHUNK_SIZE
        val localX = worldX % CHUNK_SIZE
        val localY = worldY % CHUNK_SIZE
        return getChunk(chunkX, chunkY)?.getTileAt(localX, localY) ?: Tile.AIR
    }

    operator fun set(worldX: Int, worldY: Int, tile: Tile) {
        val chunkX = worldX / CHUNK_SIZE
        val chunkY = worldY / CHUNK_SIZE
        val localX = worldX % CHUNK_SIZE
        val localY = worldY % CHUNK_SIZE
        getChunk(chunkX, chunkY)?.setTileAt(localX, localY, tile)
    }

    fun getChunk(chunkX: Int, chunkY: Int): Chunk? = chunks[packChunkCoords(chunkX, chunkY)]

    private fun getChunksInRange(camera: Camera, padding: Int = 0, filter: ((Chunk) -> Boolean)? = null): List<Chunk> {
        val startCX = ((camera.left / CHUNK_PIXEL_WIDTH).toInt() - padding).coerceIn(0, chunksWide - 1)
        val endCX = ((camera.right / CHUNK_PIXEL_WIDTH).toInt() + padding).coerceIn(0, chunksWide - 1)
        val startCY = ((camera.top / CHUNK_PIXEL_HEIGHT).toInt() - padding).coerceIn(0, chunksHigh - 1)
        val endCY = ((camera.bottom / CHUNK_PIXEL_HEIGHT).toInt() + padding).coerceIn(0, chunksHigh - 1)

        val result = mutableListOf<Chunk>()
        for (cy in startCY..endCY) {
            for (cx in startCX..endCX) {
                val chunk = getChunk(cx, cy) ?: continue
                if (filter == null || filter(chunk)) {
                    result.add(chunk)
                }
            }
        }
        return result
    }

    private fun packChunkCoords(chunkX: Int, chunkY: Int): Long =
        WorldConstants.packCoords(chunkX, chunkY)
}
