package com.glycin.springshooter

import com.glycin.image.SpringGameImage
import com.glycin.extensions.flipHorizontal
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

private const val ENEMY_SPEED = 0.8
private const val SHOOT_DAMAGE = 35
private const val HIT_ANGLE_THRESHOLD = 0.05
private const val SPAWN_INTERVAL_TICKS = 600 // 10 seconds at 60 FPS
private const val MIN_SPAWN_DISTANCE = 5.0
private const val GRUNT_SHOOT_COOLDOWN = 120 // 2 seconds
private const val GRUNT_SHOOT_RANGE = 15.0
private const val GRUNT_BULLET_DAMAGE = 8
private const val BULLET_SPEED = 5.0
private const val BULLET_HIT_RADIUS = 0.3
private const val ELITE_MELEE_RANGE = 1.0
private const val ELITE_MELEE_COOLDOWN = 60 // 1 second
private const val ELITE_MELEE_DAMAGE = 15

@Service
class EnemyService(
    private val springApplicationContextService: SpringApplicationContextService,
    private val levelService: LevelService,
) {

    private val logger = LoggerFactory.getLogger(EnemyService::class.java)

    private val gruntRight: BufferedImage = SpringGameImage("sprites/grunt.png").image
    private val gruntLeft: BufferedImage = gruntRight.flipHorizontal()
    private val eliteRight: BufferedImage = SpringGameImage("sprites/elite.png").image
    private val eliteLeft: BufferedImage = eliteRight.flipHorizontal()
    val bulletSprite: BufferedImage = SpringGameImage("sprites/enemy-bullet.png").image

    var enemies: List<Enemy> = emptyList()
    var bullets: MutableList<Bullet> = mutableListOf()
    var lastKillMessage: String? = null
    var killMessageTimer: Int = 0
    var lastHitDistance: Double = 1.0

    private var spawnQueue: MutableList<Enemy> = mutableListOf()
    private var spawnTimer: Int = 0
    private var wave: Int = 0

    fun loadEnemies() {
        enemies = springApplicationContextService.getEnemies()
        bullets.clear()
        spawnQueue = enemies.toMutableList()
        spawnTimer = 0
        wave = 0
    }

    fun update(playerX: Double, playerY: Double, deltaTime: Double, onPlayerHit: (Int) -> Unit) {
        if (killMessageTimer > 0) killMessageTimer--

        updateSpawning(playerX, playerY)

        for (enemy in enemies) {
            if (!enemy.alive) continue
            if (enemy.attackCooldown > 0) enemy.attackCooldown--

            val dx = playerX - enemy.x
            val dy = playerY - enemy.y
            val dist = sqrt(dx * dx + dy * dy)
            enemy.distance = dist

            when (enemy.type) {
                EnemyType.GRUNT -> {
                    if (dist <= GRUNT_SHOOT_RANGE && enemy.attackCooldown <= 0) {
                        val dirX = dx / dist
                        val dirY = dy / dist
                        // Offset bullet spawn to the side facing the player
                        val sideOffset = 0.3
                        val spawnX = enemy.x + if (dx > 0) sideOffset else -sideOffset
                        bullets.add(Bullet(spawnX, enemy.y, dirX, dirY))
                        enemy.attackCooldown = GRUNT_SHOOT_COOLDOWN
                    }
                }
                EnemyType.ELITE -> {
                    if (dist <= ELITE_MELEE_RANGE && enemy.attackCooldown <= 0) {
                        onPlayerHit(ELITE_MELEE_DAMAGE)
                        enemy.attackCooldown = ELITE_MELEE_COOLDOWN
                    }
                }
            }

            if (dist > 0.5) {
                val dirX = dx / dist
                val dirY = dy / dist
                val newX = enemy.x + dirX * ENEMY_SPEED * deltaTime
                val newY = enemy.y + dirY * ENEMY_SPEED * deltaTime
                if (levelService.isPassable(newX, enemy.y)) {
                    enemy.x = newX
                }
                if (levelService.isPassable(enemy.x, newY)) {
                    enemy.y = newY
                }
            }
        }

        updateBullets(playerX, playerY, deltaTime, onPlayerHit)
    }

    private fun updateBullets(playerX: Double, playerY: Double, deltaTime: Double, onPlayerHit: (Int) -> Unit) {
        val iter = bullets.iterator()
        while (iter.hasNext()) {
            val bullet = iter.next()
            bullet.x += bullet.dirX * BULLET_SPEED * deltaTime
            bullet.y += bullet.dirY * BULLET_SPEED * deltaTime

            // Hit player
            val dx = bullet.x - playerX
            val dy = bullet.y - playerY
            if (sqrt(dx * dx + dy * dy) < BULLET_HIT_RADIUS) {
                onPlayerHit(GRUNT_BULLET_DAMAGE)
                iter.remove()
                continue
            }

            // Hit wall
            if (levelService.isWall(bullet.x.toInt(), bullet.y.toInt())) {
                iter.remove()
            }
        }
    }

    private fun updateSpawning(playerX: Double, playerY: Double) {
        if (spawnQueue.isEmpty()) return

        spawnTimer++
        if (spawnTimer < SPAWN_INTERVAL_TICKS) return
        spawnTimer = 0
        wave++

        val openTiles = levelService.getOpenTiles()
            .map { (tx, ty) -> tx + 0.5 to ty + 0.5 }
            .filter { (tx, ty) ->
                val dx = tx - playerX
                val dy = ty - playerY
                sqrt(dx * dx + dy * dy) >= MIN_SPAWN_DISTANCE
            }
            .sortedBy { (tx, ty) ->
                val dx = tx - playerX
                val dy = ty - playerY
                sqrt(dx * dx + dy * dy)
            }

        if (openTiles.isEmpty()) return

        val count = wave.coerceAtMost(spawnQueue.size)
        val toSpawn = spawnQueue.take(count)
        spawnQueue = spawnQueue.drop(count).toMutableList()

        toSpawn.forEachIndexed { index, enemy ->
            val tile = openTiles[index % openTiles.size]
            enemy.x = tile.first + (Math.random() * 0.6 - 0.3)
            enemy.y = tile.second + (Math.random() * 0.6 - 0.3)
            enemy.alive = true
        }

        logger.info("Wave {} — spawned {} enemies ({} remaining in queue)", wave, count, spawnQueue.size)
    }

    fun shoot(playerX: Double, playerY: Double, playerAngle: Double): Boolean {
        val target = enemies
            .filter { it.alive }
            .mapNotNull { enemy ->
                val dx = enemy.x - playerX
                val dy = enemy.y - playerY
                val dist = sqrt(dx * dx + dy * dy)
                enemy.distance = dist

                var relativeAngle = atan2(dy, dx) - playerAngle
                while (relativeAngle > Math.PI) relativeAngle -= 2 * Math.PI
                while (relativeAngle < -Math.PI) relativeAngle += 2 * Math.PI

                val hitThreshold = HIT_ANGLE_THRESHOLD + enemy.size * 0.03 / dist.coerceAtLeast(0.5)
                if (abs(relativeAngle) < hitThreshold) enemy else null
            }
            .minByOrNull { it.distance }
            ?: return false

        lastHitDistance = target.distance
        target.health -= SHOOT_DAMAGE
        logger.info("Hit '{}' for {} damage (HP: {})", target.beanName, SHOOT_DAMAGE, target.health)

        if (target.health <= 0) {
            killEnemy(target)
        }

        return true
    }

    fun getVisibleEnemies(playerX: Double, playerY: Double, playerAngle: Double, fov: Double): List<Enemy> {
        return enemies
            .filter { it.alive }
            .onEach { enemy ->
                val dx = enemy.x - playerX
                val dy = enemy.y - playerY
                enemy.distance = sqrt(dx * dx + dy * dy)
            }
            .filter { it.distance > 0.1 }
            .sortedByDescending { it.distance }
    }

    private fun killEnemy(enemy: Enemy) {
        enemy.alive = false
        lastKillMessage = "Destroyed bean '${enemy.beanName}' (${enemy.beanClassName})"
        killMessageTimer = 180
        logger.info("Killed '{}' — removing from ApplicationContext!", enemy.beanName)
        springApplicationContextService.removeBean(enemy)
    }

    fun getSprite(type: EnemyType, facingRight: Boolean): BufferedImage {
        return when (type) {
            EnemyType.GRUNT -> if (facingRight) gruntRight else gruntLeft
            EnemyType.ELITE -> if (facingRight) eliteRight else eliteLeft
        }
    }

    fun aliveCount(): Int = enemies.count { it.alive }

    fun spawnedCount(): Int = enemies.size - spawnQueue.size

    fun currentWave(): Int = wave
}
