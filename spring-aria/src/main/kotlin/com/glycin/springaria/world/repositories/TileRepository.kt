package com.glycin.springaria.world.repositories

import com.glycin.springaria.world.World
import com.glycin.springaria.world.WorldConstants.CHUNK_SIZE
import org.springframework.data.repository.Repository

interface TileRepository : Repository<TileRecord, Long>, TileRepositoryCustom

interface TileRepositoryCustom {
    fun findByXBetweenAndYBetween(x1: Int, x2: Int, y1: Int, y2: Int): List<TileRecord>
    fun findByType(type: Tile): List<TileRecord>
    fun findByYGreaterThanAndType(y: Int, type: Tile): List<TileRecord>
    fun deleteByXAndY(x: Int, y: Int)
    fun saveByXAndYAndType(x: Int, y: Int, type: Tile)
    fun findTileAt(x: Int, y: Int): Tile
    fun findByRadius(centerX: Int, centerY: Int, radius: Int): List<TileRecord>
    fun existsByXAndYAndType(x: Int, y: Int, type: Tile): Boolean
}

class TileRepositoryCustomImpl(
    private val world: World,
) : TileRepositoryCustom {

    override fun findByXBetweenAndYBetween(x1: Int, x2: Int, y1: Int, y2: Int): List<TileRecord> {
        val results = mutableListOf<TileRecord>()
        for (y in y1..y2) {
            for (x in x1..x2) {
                val tile = world[x, y]
                if (tile != Tile.AIR) {
                    results.add(TileRecord(TileRecord.packId(x, y), x, y, tile))
                }
            }
        }
        return results
    }

    override fun findByType(type: Tile): List<TileRecord> {
        return filterTiles { _, _, tile -> tile == type }
    }

    override fun findByYGreaterThanAndType(y: Int, type: Tile): List<TileRecord> {
        return filterTiles(startChunkY = (y / CHUNK_SIZE).coerceIn(0, world.chunksHigh - 1)) { _, worldY, tile ->
            worldY > y && tile == type
        }
    }

    private inline fun filterTiles(
        startChunkY: Int = 0,
        predicate: (worldX: Int, worldY: Int, tile: Tile) -> Boolean,
    ): List<TileRecord> {
        val results = mutableListOf<TileRecord>()
        for (cy in startChunkY..<world.chunksHigh) {
            for (cx in 0..<world.chunksWide) {
                val chunk = world.getChunk(cx, cy) ?: continue
                chunk.forEach { localX, localY, tile ->
                    val worldX = cx * CHUNK_SIZE + localX
                    val worldY = cy * CHUNK_SIZE + localY
                    if (predicate(worldX, worldY, tile)) {
                        results.add(TileRecord(TileRecord.packId(worldX, worldY), worldX, worldY, tile))
                    }
                }
            }
        }
        return results
    }

    override fun deleteByXAndY(x: Int, y: Int) {
        world[x, y] = Tile.AIR
    }

    override fun saveByXAndYAndType(x: Int, y: Int, type: Tile) {
        world[x, y] = type
    }

    override fun findTileAt(x: Int, y: Int): Tile {
        return world[x, y]
    }

    override fun findByRadius(centerX: Int, centerY: Int, radius: Int): List<TileRecord> {
        val rSq = radius * radius
        val results = mutableListOf<TileRecord>()
        for (y in (centerY - radius)..(centerY + radius)) {
            for (x in (centerX - radius)..(centerX + radius)) {
                val dx = x - centerX
                val dy = y - centerY
                if (dx * dx + dy * dy <= rSq) {
                    val tile = world[x, y]
                    if (tile != Tile.AIR) {
                        results.add(TileRecord(TileRecord.packId(x, y), x, y, tile))
                    }
                }
            }
        }
        return results
    }

    override fun existsByXAndYAndType(x: Int, y: Int, type: Tile): Boolean {
        return world[x, y] == type
    }
}
