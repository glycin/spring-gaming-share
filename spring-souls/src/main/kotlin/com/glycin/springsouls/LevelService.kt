package com.glycin.springsouls

import org.springframework.stereotype.Service

const val TILE_EMPTY = 0
const val TILE_WALL = 1
const val TILE_POTION = 2
const val TILE_ENEMY = 3
const val TILE_DOOR = 4
const val TILE_KEY = 5
const val TILE_BOSS_WALL = 6
const val TILE_BOSS = 7

@Service
class LevelService(
    levelProperties: LevelProperties,
) {

    val map: Array<IntArray> = levelProperties.rows.map { row ->
        row.split(",").map { it.trim().toIntOrNull() ?: TILE_EMPTY }.toIntArray()
    }.toTypedArray()

    val depth: Int get() = map.size
    val width: Int get() = if (map.isNotEmpty()) map[0].size else 0

    val spawnX: Float = levelProperties.spawnX
    val spawnZ: Float = levelProperties.spawnZ

    fun getTile(tileX: Int, tileZ: Int): Int {
        if (tileX !in 0..<width || tileZ !in 0..<depth) return TILE_WALL
        return map[tileZ][tileX]
    }

    fun setTile(tileX: Int, tileZ: Int, type: Int) {
        if (tileX in 0..<width && tileZ in 0..<depth) {
            map[tileZ][tileX] = type
        }
    }

    fun isWall(tileX: Int, tileZ: Int): Boolean {
        val tile = getTile(tileX, tileZ)
        return tile == TILE_WALL || tile == TILE_DOOR || tile == TILE_BOSS_WALL
    }

    fun isPassable(worldX: Float, worldZ: Float, margin: Float = 0.2f): Boolean {
        return !isWall((worldX - margin).toInt(), (worldZ - margin).toInt())
            && !isWall((worldX + margin).toInt(), (worldZ - margin).toInt())
            && !isWall((worldX - margin).toInt(), (worldZ + margin).toInt())
            && !isWall((worldX + margin).toInt(), (worldZ + margin).toInt())
    }

    fun getWallTiles(): List<Pair<Int, Int>> = getTilesOfType(TILE_WALL)

    fun getOpenTiles(): List<Pair<Int, Int>> = getTilesOfType(TILE_EMPTY)

    fun getPotionTiles(): List<Pair<Int, Int>> = getTilesOfType(TILE_POTION)

    fun getEnemyTiles(): List<Pair<Int, Int>> = getTilesOfType(TILE_ENEMY)

    fun getDoorTiles(): List<Pair<Int, Int>> = getTilesOfType(TILE_DOOR)

    fun getKeyTiles(): List<Pair<Int, Int>> = getTilesOfType(TILE_KEY)

    fun getBossWallTiles(): List<Pair<Int, Int>> = getTilesOfType(TILE_BOSS_WALL)

    fun getBossTiles(): List<Pair<Int, Int>> = getTilesOfType(TILE_BOSS)

    private fun getTilesOfType(type: Int): List<Pair<Int, Int>> = buildList {
        for (z in 0..<depth) {
            for (x in 0..<width) {
                if (map[z][x] == type) {
                    add(x to z)
                }
            }
        }
    }
}
