package com.glycin.springshooter

import org.springframework.stereotype.Service

const val TILE_EMPTY = 0
const val TILE_WALL = 1

@Service
class LevelService(
    levelProperties: LevelProperties,
) {

    val map: Array<IntArray> = levelProperties.rows.map { row ->
        row.split(",").map { it.trim().toInt() }.toIntArray()
    }.toTypedArray()

    val height: Int get() = map.size
    val width: Int get() = map[0].size

    val spawnX: Double = levelProperties.spawnX
    val spawnY: Double = levelProperties.spawnY

    fun isWall(tileX: Int, tileY: Int): Boolean {
        if (tileX !in 0..<width || tileY !in 0..<height) return true
        return map[tileY][tileX] != TILE_EMPTY
    }

    fun getTile(tileX: Int, tileY: Int): Int {
        if (tileX !in 0..<width || tileY !in 0..<height) return TILE_WALL
        return map[tileY][tileX]
    }

    fun isPassable(worldX: Double, worldY: Double, margin: Double = 0.2): Boolean {
        return !isWall((worldX - margin).toInt(), (worldY - margin).toInt())
            && !isWall((worldX + margin).toInt(), (worldY - margin).toInt())
            && !isWall((worldX - margin).toInt(), (worldY + margin).toInt())
            && !isWall((worldX + margin).toInt(), (worldY + margin).toInt())
    }

    fun getOpenTiles(): List<Pair<Int, Int>> =
        (0..<height).flatMap { y ->
            (0..<width).filter { x -> map[y][x] == TILE_EMPTY }.map { x -> x to y }
        }
}
