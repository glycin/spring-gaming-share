package com.glycin.springsurvivors.attacks

import com.glycin.springsurvivors.GameSettings
import com.glycin.springsurvivors.GameState
import com.glycin.springsurvivors.enemies.Enemy
import com.glycin.springsurvivors.grid.GameGrid
import com.glycin.util.GridPos
import com.glycin.util.Vec2
import java.awt.Color
import java.awt.Graphics2D

enum class DirectionMode {
    TWO,
    FOUR,
    EIGHT,
}

private val DIAGONAL_UP_RIGHT = Vec2(1f, -1f).normalized()
private val DIAGONAL_UP_LEFT = Vec2(-1f, -1f).normalized()
private val DIAGONAL_DOWN_RIGHT = Vec2(1f, 1f).normalized()
private val DIAGONAL_DOWN_LEFT = Vec2(-1f, 1f).normalized()

class BaseAttack(
    private val gameSettings: GameSettings,
    private val gameState: GameState,
    private val gameGrid: GameGrid,
) : Attack {

    var beatsPerFire: Int = 3
    var directionMode: DirectionMode = DirectionMode.TWO
    var splitOnHit: Boolean = false

    val projectiles = mutableListOf<Projectile>()
    private var beatCounter = 0

    override fun onBeat(playerGridPos: GridPos) {
        beatCounter++
        if (beatCounter < beatsPerFire) return
        beatCounter = 0

        val halfTile = gameGrid.tileSize / 2f
        val cx = gameGrid.toPixelX(playerGridPos.col) + halfTile
        val cy = gameGrid.toPixelY(playerGridPos.row) + halfTile
        val pos = Vec2(cx, cy)

        shoot(pos, Vec2.right)
        shoot(pos, Vec2.left)
        if (directionMode >= DirectionMode.FOUR) {
            shoot(pos, Vec2.up)
            shoot(pos, Vec2.down)
        }
        if (directionMode >= DirectionMode.EIGHT) {
            shoot(pos, DIAGONAL_UP_RIGHT)
            shoot(pos, DIAGONAL_UP_LEFT)
            shoot(pos, DIAGONAL_DOWN_RIGHT)
            shoot(pos, DIAGONAL_DOWN_LEFT)
        }
    }

    override fun update() {
        projectiles.forEach {
            it.position.x += it.direction.x * it.speed
            it.position.y += it.direction.y * it.speed
        }
        projectiles.removeAll { isOutOfBounds(it) }
    }

    override fun render(g: Graphics2D) {
        g.color = Color.YELLOW
        projectiles.forEach { g.fillOval(it.position.x.toInt(), it.position.y.toInt(), it.size, it.size) }
    }

    override fun handleCollisions(enemiesByCell: Map<GridPos, MutableList<Enemy>>, damageMultiplier: Float): Set<Enemy> {
        val projectilesToRemove = mutableSetOf<Projectile>()
        val killed = mutableSetOf<Enemy>()
        val splitProjectiles = mutableListOf<Projectile>()

        for (projectile in projectiles) {
            if (projectile in projectilesToRemove) continue
            val cell = gameGrid[projectile.position.x, projectile.position.y]
            val cellEnemies = enemiesByCell[cell] ?: continue
            for (enemy in cellEnemies) {
                if (enemy in killed) continue
                enemy.hp -= damageMultiplier
                if (!gameState.piercingShots) {
                    projectilesToRemove.add(projectile)
                    if (splitOnHit && !projectile.isSplit) {
                        splitProjectiles.addAll(spawnSplitProjectiles(projectile))
                    }
                }
                if (enemy.hp <= 0f) {
                    killed.add(enemy)
                }
                if (!gameState.piercingShots) break
            }
        }

        projectiles.removeAll(projectilesToRemove)
        projectiles.addAll(splitProjectiles)
        return killed
    }

    override fun snapshot(): Any = projectiles.map { it.copy(position = it.position.copy()) }

    @Suppress("UNCHECKED_CAST")
    override fun restore(snapshot: Any?) {
        projectiles.clear()
        val saved = snapshot as? List<Projectile> ?: return
        projectiles.addAll(saved.map { it.copy(position = it.position.copy()) })
    }

    private fun shoot(position: Vec2, direction: Vec2) {
        val speed = gameSettings.projectile.speed * gameState.projectileSpeedMultiplier
        val size = (gameSettings.projectile.size * gameState.projectileSizeMultiplier).toInt()
        projectiles.add(Projectile(position = position.copy(), direction = direction, speed = speed, size = size))
    }

    private fun spawnSplitProjectiles(parent: Projectile): List<Projectile> {
        val dir = parent.direction
        val perpendicular = Vec2(-dir.y, dir.x)
        val spreadAngle = 0.4f
        return listOf(
            Projectile(parent.position.copy(), dir, parent.speed, parent.size, isSplit = true),
            Projectile(parent.position.copy(), Vec2(dir.x + perpendicular.x * spreadAngle, dir.y + perpendicular.y * spreadAngle).normalized(), parent.speed, parent.size, isSplit = true),
            Projectile(parent.position.copy(), Vec2(dir.x - perpendicular.x * spreadAngle, dir.y - perpendicular.y * spreadAngle).normalized(), parent.speed, parent.size, isSplit = true),
        )
    }

    private fun isOutOfBounds(p: Projectile) =
        p.position.x < -50 || p.position.x > gameSettings.windowWidth + 50 ||
            p.position.y < -50 || p.position.y > gameSettings.windowHeight + 50
}
