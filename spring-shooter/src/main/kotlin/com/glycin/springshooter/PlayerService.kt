package com.glycin.springshooter

import com.glycin.image.SpringGameImage
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
import kotlin.math.cos
import kotlin.math.sin

private const val MOVE_SPEED = 3.0
private const val TURN_SPEED = 2.5
private const val MOUSE_SENSITIVITY_X = 0.003
private const val MOUSE_SENSITIVITY_Y = 0.003
private const val MAX_PITCH = 200.0
private const val FOV = Math.PI / 3.0
private const val SHOOT_COOLDOWN_TICKS = 12
private const val MUZZLE_FLASH_TICKS = 4
const val MAG_SIZE = 30
private const val RELOAD_TICKS = 120 // 2 seconds at 60 FPS

@Service
class PlayerService(
    private val inputService: InputService,
    private val enemyService: EnemyService,
    private val levelService: LevelService,
) {

    val sprite: BufferedImage = SpringGameImage("sprites/master-chief.png").image
    val muzzleFlashSprite: BufferedImage = SpringGameImage("sprites/muzzle-flash.png").image
    val enemyHitSprite: BufferedImage = SpringGameImage("sprites/enemy-hit.png").image

    var x: Double = 0.0
    var y: Double = 0.0
    var angle: Double = 0.0
    var pitch: Double = 0.0
    var muzzleFlashTimer: Int = 0
    var lastShotHit: Boolean = false
    var ammo: Int = MAG_SIZE
    var reloading: Boolean = false
    var health: Int = 100
    var damageFlashTimer: Int = 0

    val fov: Double get() = FOV

    private var cooldownTimer: Int = 0
    private var reloadTimer: Int = 0

    fun spawn() {
        x = levelService.spawnX
        y = levelService.spawnY
        angle = 0.0
        pitch = 0.0
        ammo = MAG_SIZE
        reloading = false
        reloadTimer = 0
        health = 100
        damageFlashTimer = 0
    }

    fun update(deltaTime: Double) {
        val mouseDx = inputService.consumeMouseDeltaX()
        val mouseDy = inputService.consumeMouseDeltaY()
        angle += mouseDx * MOUSE_SENSITIVITY_X
        pitch = (pitch - mouseDy * MOUSE_SENSITIVITY_Y * WINDOW_HEIGHT).coerceIn(-MAX_PITCH, MAX_PITCH)

        if (inputService.turningLeft) {
            angle -= TURN_SPEED * deltaTime
        }
        if (inputService.turningRight) {
            angle += TURN_SPEED * deltaTime
        }

        var dx = 0.0
        var dy = 0.0

        val forwardX = cos(angle)
        val forwardY = sin(angle)
        val strafeX = cos(angle + Math.PI / 2.0)
        val strafeY = sin(angle + Math.PI / 2.0)

        if (inputService.movingForward) {
            dx += forwardX * MOVE_SPEED * deltaTime
            dy += forwardY * MOVE_SPEED * deltaTime
        }
        if (inputService.movingBackward) {
            dx -= forwardX * MOVE_SPEED * deltaTime
            dy -= forwardY * MOVE_SPEED * deltaTime
        }
        if (inputService.strafingLeft) {
            dx -= strafeX * MOVE_SPEED * deltaTime
            dy -= strafeY * MOVE_SPEED * deltaTime
        }
        if (inputService.strafingRight) {
            dx += strafeX * MOVE_SPEED * deltaTime
            dy += strafeY * MOVE_SPEED * deltaTime
        }

        // Slide along walls: try each axis independently
        if (levelService.isPassable(x + dx, y)) {
            x += dx
        }
        if (levelService.isPassable(x, y + dy)) {
            y += dy
        }

        if (damageFlashTimer > 0) damageFlashTimer--

        updateShooting()
    }

    fun takeDamage(amount: Int) {
        health = (health - amount).coerceAtLeast(0)
        damageFlashTimer = 15
    }

    fun isMoving(): Boolean = inputService.movingForward || inputService.movingBackward
        || inputService.strafingLeft || inputService.strafingRight

    private fun updateShooting() {
        if (muzzleFlashTimer > 0) muzzleFlashTimer--
        if (cooldownTimer > 0) cooldownTimer--

        if (reloading) {
            reloadTimer--
            if (reloadTimer <= 0) {
                ammo = MAG_SIZE
                reloading = false
            }
            return
        }

        if (inputService.reloading && ammo < MAG_SIZE) {
            reloading = true
            reloadTimer = RELOAD_TICKS
            return
        }

        val wantsToShoot = inputService.consumeShoot() || inputService.shooting
        if (wantsToShoot && cooldownTimer <= 0 && ammo > 0) {
            ammo--
            lastShotHit = enemyService.shoot(x, y, angle)
            muzzleFlashTimer = MUZZLE_FLASH_TICKS
            cooldownTimer = SHOOT_COOLDOWN_TICKS

            if (ammo <= 0) {
                reloading = true
                reloadTimer = RELOAD_TICKS
            }
        }
    }
}
