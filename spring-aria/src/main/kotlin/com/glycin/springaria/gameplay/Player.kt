package com.glycin.springaria.gameplay

import com.glycin.springaria.GameSettings
import com.glycin.springaria.world.Camera
import com.glycin.springaria.world.World
import com.glycin.springaria.world.WorldConstants.TILE_SIZE
import com.glycin.springaria.world.repositories.Tile
import com.glycin.springaria.world.repositories.TileRepository
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val GRAVITY = 0.25
private const val MAX_FALL_SPEED = 8.0
private const val LAVA_DAMAGE_INTERVAL = 60
private const val MINING_REACH_TILES = 3
private const val BUILDING_REACH_TILES = 3
private const val SPRAY_REACH_TILES = 25

@Component
class Player(
    private val collisionService: CollisionService,
    private val playerActionService: PlayerActionService,
    private val gameSettings: GameSettings,
    private val world: World,
    private val camera: Camera,
    private val fluidSimulationService: FluidSimulationService,
    private val tileRepository: TileRepository,
    private val playerMissileService: PlayerMissileService,
    private val bulletService: BulletService,
) {

    var x: Double = 0.0
    var y: Double = 0.0
    var velocityX: Double = 0.0
    var velocityY: Double = 0.0
    var onGround: Boolean = false
    var mining: Boolean = false
    var hp: Int = 10
    var lastDamageTime: Long = 0

    private val animator = PlayerAnimator()
    private var lavaDamageTick = 0

    val widthInTiles = 1
    val heightInTiles = 2
    val widthInPixels = widthInTiles * TILE_SIZE
    val heightInPixels = heightInTiles * TILE_SIZE

    fun spawnAt(tileX: Int, tileY: Int) {
        x = tileX * TILE_SIZE.toDouble()
        y = tileY * TILE_SIZE.toDouble()
        velocityX = 0.0
        velocityY = 0.0
    }

    fun update(movingLeft: Boolean, movingRight: Boolean, jump: Boolean) {
        velocityX = when {
            movingLeft && !movingRight -> -gameSettings.playerSpeed
            movingRight && !movingLeft -> gameSettings.playerSpeed
            else -> 0.0
        }

        if (jump && onGround) {
            playerActionService.jump {
                velocityY = gameSettings.jumpForce
                onGround = false
            }
        }

        velocityY = (velocityY + GRAVITY).coerceAtMost(MAX_FALL_SPEED)

        moveX(velocityX)
        moveY(velocityY)

        if (collisionService.isInLava(x, y, widthInPixels, heightInPixels)) {
            lavaDamageTick++
            if (lavaDamageTick >= LAVA_DAMAGE_INTERVAL) {
                lavaDamageTick = 0
                if (hp > 0) {
                    hp--
                    lastDamageTime = System.currentTimeMillis()
                }
            }
        } else {
            lavaDamageTick = 0
        }

        animator.update(movingX = velocityX != 0.0, onGround = onGround, mining = mining, movingLeft = movingLeft, movingRight = movingRight)
        mining = false
    }

    fun tryMine(screenX: Int, screenY: Int) {
        val angle = aimAngle(screenX, screenY)
        val (tileX, tileY) = raycastTile(angle, MINING_REACH_TILES) { it != Tile.AIR && !it.isLiquid } ?: return
        tileRepository.deleteByXAndY(tileX, tileY)
        fluidSimulationService.registerAdjacentFluids(tileX, tileY)
        mining = true
    }

    fun shoot(screenX: Int, screenY: Int) {
        val (cx, cy, angle) = aimFrom(screenX, screenY)
        playerActionService.shoot {
            bulletService.spawn(cx, cy, angle)
        }
    }

    fun shootMissile(screenX: Int, screenY: Int) {
        val (cx, cy, angle) = aimFrom(screenX, screenY)
        playerActionService.shootMissile {
            playerMissileService.spawn(cx, cy, angle)
        }
    }

    fun sprayLava(screenX: Int, screenY: Int) {
        val (cx, cy, angle) = aimFrom(screenX, screenY)
        playerActionService.sprayLava {
            for (dist in 2..SPRAY_REACH_TILES) {
                val tileX = ((cx + cos(angle) * dist * TILE_SIZE) / TILE_SIZE).roundToInt()
                val tileY = ((cy + sin(angle) * dist * TILE_SIZE) / TILE_SIZE).roundToInt()
                val tile = world[tileX, tileY]
                if (tile == Tile.AIR) {
                    world[tileX, tileY] = Tile.LAVA
                    fluidSimulationService.registerFluid(tileX, tileY)
                } else if (tile.solid) {
                    break
                }
            }
        }
    }

    private fun aimFrom(screenX: Int, screenY: Int): Triple<Double, Double, Double> {
        return Triple(x + widthInPixels / 2.0, y + heightInPixels / 2.0, aimAngle(screenX, screenY))
    }

    fun tryBuild(screenX: Int, screenY: Int) {
        val angle = aimAngle(screenX, screenY)
        for (dist in 1..BUILDING_REACH_TILES) {
            val tileX = ((x + widthInPixels / 2.0 + cos(angle) * dist * TILE_SIZE) / TILE_SIZE).roundToInt()
            val tileY = ((y + heightInPixels / 2.0 + sin(angle) * dist * TILE_SIZE) / TILE_SIZE).roundToInt()
            if (world[tileX, tileY] == Tile.AIR) {
                if (overlapsPlayer(tileX, tileY)) continue
                tileRepository.saveByXAndYAndType(tileX, tileY, Tile.STONE)
                return
            }
        }
    }

    private fun aimAngle(screenX: Int, screenY: Int): Double {
        val worldMouseX = camera.screenToWorldX(screenX.toDouble())
        val worldMouseY = camera.screenToWorldY(screenY.toDouble())
        return atan2(worldMouseY - (y + heightInPixels / 2.0), worldMouseX - (x + widthInPixels / 2.0))
    }

    private inline fun raycastTile(angle: Double, reach: Int, predicate: (Tile) -> Boolean): Pair<Int, Int>? {
        val centerX = x + widthInPixels / 2.0
        val centerY = y + heightInPixels / 2.0
        for (dist in 1..reach) {
            val tileX = ((centerX + cos(angle) * dist * TILE_SIZE) / TILE_SIZE).roundToInt()
            val tileY = ((centerY + sin(angle) * dist * TILE_SIZE) / TILE_SIZE).roundToInt()
            if (predicate(world[tileX, tileY])) return tileX to tileY
        }
        return null
    }

    private fun overlapsPlayer(tileX: Int, tileY: Int): Boolean {
        val playerLeftTile = (x / TILE_SIZE).toInt()
        val playerRightTile = ((x + widthInPixels - 1) / TILE_SIZE).toInt()
        val playerTopTile = (y / TILE_SIZE).toInt()
        val playerBottomTile = ((y + heightInPixels - 1) / TILE_SIZE).toInt()
        return tileX in playerLeftTile..playerRightTile && tileY in playerTopTile..playerBottomTile
    }

    fun getCurrentFrame(): BufferedImage = animator.getCurrentFrame()

    private fun moveX(dx: Double) {
        val newX = x + dx
        if (!collisionService.collidesAt(newX, y, widthInPixels, heightInPixels)) {
            x = newX
        } else {
            velocityX = 0.0
        }
    }

    private fun moveY(dy: Double) {
        val newY = y + dy
        if (!collisionService.collidesAt(x, newY, widthInPixels, heightInPixels)) {
            y = newY
            onGround = false
        } else {
            if (velocityY > 0) onGround = true
            velocityY = 0.0
        }
    }
}
