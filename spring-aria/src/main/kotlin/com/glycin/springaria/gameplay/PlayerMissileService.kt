package com.glycin.springaria.gameplay

import com.glycin.springaria.world.World
import com.glycin.springaria.world.WorldConstants.TILE_SIZE
import com.glycin.springaria.world.repositories.Tile
import com.glycin.springaria.world.repositories.TileRepository
import org.springframework.stereotype.Service
import kotlin.math.cos
import kotlin.math.sin

private const val MISSILE_SPEED = 1.0
private const val MAX_LIFETIME = 500
private const val EXPLOSION_RADIUS = 16

data class PlayerMissile(
    var x: Double,
    var y: Double,
    val vx: Double,
    val vy: Double,
    var lifetime: Int = 0,
)

data class PlayerExplosion(
    val worldX: Double,
    val worldY: Double,
    val startTime: Long = System.currentTimeMillis(),
)

@Service
class PlayerMissileService(
    private val world: World,
    private val tileRepository: TileRepository,
    private val fluidSimulationService: FluidSimulationService,
) {

    val missiles = mutableListOf<PlayerMissile>()
    val explosions = mutableListOf<PlayerExplosion>()
    var pendingExplosion = false

    fun spawn(worldX: Double, worldY: Double, angle: Double) {
        missiles.add(PlayerMissile(
            x = worldX,
            y = worldY,
            vx = cos(angle) * MISSILE_SPEED,
            vy = sin(angle) * MISSILE_SPEED,
        ))
    }

    fun update(enemyService: EnemyService) {
        val iterator = missiles.iterator()
        while (iterator.hasNext()) {
            val missile = iterator.next()
            missile.x += missile.vx
            missile.y += missile.vy
            missile.lifetime++

            if (enemyService.removeAt(missile.x, missile.y, enemyService.widthInPixels.toDouble())) {
                explode(missile.x, missile.y)
                iterator.remove()
                continue
            }

            val tileX = (missile.x / TILE_SIZE).toInt()
            val tileY = (missile.y / TILE_SIZE).toInt()
            val tile = world[tileX, tileY]

            if ((tile != Tile.AIR && tile.solid) || missile.lifetime >= MAX_LIFETIME) {
                explode(missile.x, missile.y)
                iterator.remove()
            }
        }

        explosions.removeAll { System.currentTimeMillis() - it.startTime > 1000L }
    }

    private fun explode(worldX: Double, worldY: Double) {
        val centerTileX = (worldX / TILE_SIZE).toInt()
        val centerTileY = (worldY / TILE_SIZE).toInt()

        val tiles = tileRepository.findByRadius(centerTileX, centerTileY, EXPLOSION_RADIUS)
        for (tile in tiles) {
            tileRepository.deleteByXAndY(tile.x, tile.y)
        }

        fluidSimulationService.registerAdjacentFluids(centerTileX, centerTileY, EXPLOSION_RADIUS)

        explosions.add(PlayerExplosion(worldX, worldY))
        pendingExplosion = true
    }
}
