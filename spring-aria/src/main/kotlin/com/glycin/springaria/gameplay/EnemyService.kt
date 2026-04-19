package com.glycin.springaria.gameplay

import com.glycin.springaria.world.World
import com.glycin.springaria.world.WorldConstants.TILE_SIZE
import com.glycin.springaria.world.WorldConstants.WORLD_HEIGHT_TILES
import com.glycin.springaria.world.WorldConstants.WORLD_WIDTH_TILES
import com.glycin.springaria.world.repositories.Tile
import org.springframework.stereotype.Service
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val SPAWN_CHANCE = 0.3
private const val SPAWN_MIN_DIST = 15
private const val SPAWN_MAX_DIST = 30
private const val SHOOT_INTERVAL = 120
private const val MAX_ENEMIES = 8
private const val ENEMY_WIDTH_TILES = 2
private const val ENEMY_HEIGHT_TILES = 4
private val DESPAWN_DIST_SQ = (SPAWN_MAX_DIST * TILE_SIZE * 2.0).let { it * it }

data class Enemy(
    var x: Double,
    var y: Double,
    var shootCooldown: Int = SHOOT_INTERVAL,
)

@Service
class EnemyService(
    private val world: World,
    private val enemyMissileService: EnemyMissileService,
) {

    val enemies = mutableListOf<Enemy>()

    val widthInPixels = ENEMY_WIDTH_TILES * TILE_SIZE
    val heightInPixels = ENEMY_HEIGHT_TILES * TILE_SIZE

    fun trySpawn(playerX: Double, playerY: Double) {
        if (enemies.size >= MAX_ENEMIES) return
        if (Random.nextDouble() > SPAWN_CHANCE) return

        for (i in 0..<10) {
            val dist = Random.nextInt(SPAWN_MIN_DIST, SPAWN_MAX_DIST + 1)
            val angle = Random.nextDouble() * Math.PI * 2
            val tileX = ((playerX / TILE_SIZE) + cos(angle) * dist).toInt()
            val tileY = ((playerY / TILE_SIZE) + sin(angle) * dist).toInt()

            if (tileX !in 0..<WORLD_WIDTH_TILES || tileY < 1 || tileY >= WORLD_HEIGHT_TILES) continue

            if (world[tileX, tileY] == Tile.AIR && world[tileX, tileY - 1] == Tile.AIR) {
                enemies.add(Enemy(
                    x = tileX * TILE_SIZE.toDouble(),
                    y = (tileY - 1) * TILE_SIZE.toDouble(),
                ))
                return
            }
        }
    }

    fun update(playerX: Double, playerY: Double) {
        val iterator = enemies.iterator()
        while (iterator.hasNext()) {
            val enemy = iterator.next()

            val dx = enemy.x - playerX
            val dy = enemy.y - playerY
            if (dx * dx + dy * dy > DESPAWN_DIST_SQ) {
                iterator.remove()
                continue
            }

            enemy.shootCooldown--
            if (enemy.shootCooldown <= 0) {
                enemy.shootCooldown = SHOOT_INTERVAL + Random.nextInt(-20, 20)
                val cx = enemy.x + widthInPixels / 2.0
                val cy = enemy.y + heightInPixels / 2.0
                val angle = atan2(playerY - cy, playerX - cx)
                enemyMissileService.spawn(cx, cy, angle)
            }
        }
    }

    fun removeAt(x: Double, y: Double, radius: Double): Boolean {
        val rSq = radius * radius
        val iterator = enemies.iterator()
        while (iterator.hasNext()) {
            val enemy = iterator.next()
            val dx = enemy.x + widthInPixels / 2.0 - x
            val dy = enemy.y + heightInPixels / 2.0 - y
            if (dx * dx + dy * dy <= rSq) {
                iterator.remove()
                return true
            }
        }
        return false
    }

    fun clear() {
        enemies.clear()
    }
}
