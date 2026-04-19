package com.glycin.springsouls.gameplay

import com.glycin.springsouls.LevelService
import com.glycin.springsouls.input.InputService
import com.glycin.springsouls.metrics.GameMetricsService
import com.glycin.springsouls.render.Camera3D
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val MOVE_SPEED = 5f
private const val STAMINA_REGEN_RATE = 8f
private const val DODGE_STAMINA_COST = 25f
private const val PUNCH_STAMINA_COST = 15f
private const val KICK_STAMINA_COST = 20f
private const val MOUSE_SENSITIVITY = 0.15f
private const val ATTACK_RANGE = 1.5f
private const val ATTACK_HIT_RADIUS = 1.2f
private const val PUNCH_DAMAGE = 15
private const val KICK_DAMAGE = 25
private const val CAMERA_DISTANCE = 1.9f
private const val CAMERA_HEIGHT_OFFSET = 2.1f

@Component
class Player(
    private val levelService: LevelService,
    @Lazy private val potionRepository: PotionRepository,
    @Lazy private val metricsService: GameMetricsService,
    registry: MeterRegistry,
) {
    init {
        registry.gauge("player.hp", this) { it.hp.toDouble() }
        registry.gauge("player.stamina", this) { it.stamina.toDouble() }
        registry.gauge("player.potion.charges", this) { it.potionCharges.toDouble() }
    }

    var x = 0f
    var y = 0f
    var z = 0f
    val eyeHeight = 1.7f

    var hp = 100
    var maxHp = 100
    var stamina = 100f
    var maxStamina = 100f
    var potionCharges = 2
    var maxPotionCharges = 5

    var state: PlayerState = PlayerState.IDLE

    private var attackHitApplied = false
    private var cosYaw = 1f
    private var sinYaw = 0f

    val isAlive get() = hp > 0
    val staminaPercent get() = stamina / maxStamina
    val hpPercent get() = hp.toFloat() / maxHp.toFloat()

    fun update(input: InputService, camera: Camera3D, dt: Double) {
        // Mouse look (always consume deltas to prevent accumulation)
        if (state.isDodge) {
            input.consumeMouseDeltaX()
        } else {
            camera.yaw += input.consumeMouseDeltaX() * MOUSE_SENSITIVITY
        }
        input.consumeMouseDeltaY()

        // Cache yaw decomposition for use by processAttack() and updateCamera()
        val yawRad = Math.toRadians(camera.yaw.toDouble())
        cosYaw = cos(yawRad).toFloat()
        sinYaw = sin(yawRad).toFloat()

        if (!isAlive) return

        val dtf = dt.toFloat()
        stamina = (stamina + STAMINA_REGEN_RATE * dtf).coerceAtMost(maxStamina)

        // Potion use
        if (input.usePotion && !state.isAction) {
            state = PlayerState.DRINK
            if (potionRepository.drink(this)) {
                metricsService.recordPotionUsed()
            }
            return
        }

        // Action states (dodge/punch/kick) lock input — movement handled by root motion or nothing
        if (state.isAction) return

        val frontX = cosYaw
        val frontZ = sinYaw
        val rightX = -frontZ
        val rightZ = frontX

        // Process new input
        val shifting = input.isKeyDown(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)

        // Dodge: shift + direction
        if (shifting && stamina >= DODGE_STAMINA_COST) {
            val dodgeState = when {
                input.movingForward -> PlayerState.DODGE_FORWARD
                input.movingBackward -> PlayerState.DODGE_BACK
                input.strafingLeft -> PlayerState.DODGE_LEFT
                input.strafingRight -> PlayerState.DODGE_RIGHT
                else -> null
            }
            if (dodgeState != null) {
                state = dodgeState
                stamina -= DODGE_STAMINA_COST
                metricsService.recordDodgeRoll()
                return
            }
        }

        // Punch: left click (check stamina first to avoid consuming the input when we can't act)
        if (stamina >= PUNCH_STAMINA_COST && input.punching) {
            state = PlayerState.PUNCH
            stamina -= PUNCH_STAMINA_COST
            attackHitApplied = false
            return
        }

        // Kick: right click
        if (stamina >= KICK_STAMINA_COST && input.kicking) {
            state = PlayerState.KICK
            stamina -= KICK_STAMINA_COST
            attackHitApplied = false
            return
        }

        // Movement
        var moveX = 0f
        var moveZ = 0f

        if (input.movingForward) { moveX += frontX; moveZ += frontZ }
        if (input.movingBackward) { moveX -= frontX; moveZ -= frontZ }
        if (input.strafingLeft) { moveX -= rightX; moveZ -= rightZ }
        if (input.strafingRight) { moveX += rightX; moveZ += rightZ }

        val len = sqrt((moveX * moveX + moveZ * moveZ).toDouble()).toFloat()
        if (len > 0f) {
            moveX /= len
            moveZ /= len
            tryMoveTo(x + moveX * MOVE_SPEED * dtf, z + moveZ * MOVE_SPEED * dtf)

            // Determine run direction (prioritize forward/back over strafe)
            state = when {
                input.movingForward -> PlayerState.RUN_FORWARD
                input.movingBackward -> PlayerState.RUN_BACK
                input.strafingLeft -> PlayerState.RUN_LEFT
                input.strafingRight -> PlayerState.RUN_RIGHT
                else -> PlayerState.IDLE
            }
        } else {
            state = PlayerState.IDLE
        }
    }

    fun processAttack(damageAt: (Float, Float, Float, Int) -> Boolean) {
        val isAttacking = state == PlayerState.PUNCH || state == PlayerState.KICK
        if (isAttacking && !attackHitApplied) {
            val hitX = x + cosYaw * ATTACK_RANGE
            val hitZ = z + sinYaw * ATTACK_RANGE
            val damage = if (state == PlayerState.KICK) KICK_DAMAGE else PUNCH_DAMAGE
            if (damageAt(hitX, hitZ, ATTACK_HIT_RADIUS, damage)) {
                attackHitApplied = true
            }
        }
    }

    fun updateCamera(camera: Camera3D) {
        camera.x = x - cosYaw * CAMERA_DISTANCE
        camera.y = y + CAMERA_HEIGHT_OFFSET
        camera.z = z - sinYaw * CAMERA_DISTANCE
    }

    fun tryMoveTo(newX: Float, newZ: Float) {
        if (levelService.isPassable(newX, z)) x = newX
        if (levelService.isPassable(x, newZ)) z = newZ
    }

    fun takeDamage(amount: Int): Int {
        val actual = amount.coerceAtMost(hp)
        hp -= actual
        return actual
    }

    fun heal(amount: Int) {
        hp = (hp + amount).coerceAtMost(maxHp)
    }
}
