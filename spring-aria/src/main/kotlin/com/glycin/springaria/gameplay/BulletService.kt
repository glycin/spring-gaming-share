package com.glycin.springaria.gameplay

import com.glycin.springaria.world.World
import com.glycin.springaria.world.WorldConstants.TILE_SIZE
import com.glycin.springaria.world.repositories.Tile
import org.springframework.stereotype.Service
import kotlin.math.cos
import kotlin.math.sin

private const val BULLET_SPEED = 6.0
private const val MAX_LIFETIME = 120

data class Bullet(
    var x: Double,
    var y: Double,
    val vx: Double,
    val vy: Double,
    var lifetime: Int = 0,
)

@Service
class BulletService(
    private val world: World,
    private val fluidSimulationService: FluidSimulationService,
) {

    val bullets = mutableListOf<Bullet>()

    fun spawn(worldX: Double, worldY: Double, angle: Double) {
        bullets.add(Bullet(
            x = worldX,
            y = worldY,
            vx = cos(angle) * BULLET_SPEED,
            vy = sin(angle) * BULLET_SPEED,
        ))
    }

    fun update(enemyService: EnemyService) {
        val iterator = bullets.iterator()
        while (iterator.hasNext()) {
            val bullet = iterator.next()
            bullet.x += bullet.vx
            bullet.y += bullet.vy
            bullet.lifetime++

            if (enemyService.removeAt(bullet.x, bullet.y, enemyService.widthInPixels.toDouble())) {
                iterator.remove()
                continue
            }

            val tileX = (bullet.x / TILE_SIZE).toInt()
            val tileY = (bullet.y / TILE_SIZE).toInt()
            val tile = world[tileX, tileY]

            if (tile != Tile.AIR && tile.solid) {
                world[tileX, tileY] = Tile.AIR
                fluidSimulationService.registerAdjacentFluids(tileX, tileY)
                iterator.remove()
            } else if (bullet.lifetime >= MAX_LIFETIME) {
                iterator.remove()
            }
        }
    }
}
