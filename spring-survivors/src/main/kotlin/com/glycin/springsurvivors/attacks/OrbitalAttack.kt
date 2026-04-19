package com.glycin.springsurvivors.attacks

import com.glycin.springsurvivors.enemies.Enemy
import com.glycin.springsurvivors.grid.GameGrid
import com.glycin.springsurvivors.player.Player
import com.glycin.util.GridPos
import java.awt.Color
import java.awt.Graphics2D
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val ORBITAL_SIZE = 8
private val TWO_PI = 2 * PI.toFloat()

private val ORBITAL_GLOW = Color(100, 200, 255, 80)
private val ORBITAL_CORE = Color(150, 230, 255)

class OrbitalAttack(
    private val gameGrid: GameGrid,
    private val player: Player,
) : Attack {

    var orbitalCount: Int = 3
    var orbitalDamage: Float = 0.5f
    var orbitalRadiusTiles: Float = 2.0f
    var rotationSpeed: Float = 0.04f

    private var angle = 0f
    private val damagedThisCycle = mutableSetOf<Enemy>()
    private var lastResetAngle = 0f

    override fun onBeat(playerGridPos: GridPos) {}

    override fun update() {
        angle += rotationSpeed
        if (angle > TWO_PI) {
            angle -= TWO_PI
            lastResetAngle -= TWO_PI
        }
        if (angle - lastResetAngle > PI.toFloat()) {
            damagedThisCycle.clear()
            lastResetAngle = angle
        }
    }

    override fun render(g: Graphics2D) {
        val tileSize = gameGrid.tileSize
        val halfTile = tileSize / 2
        val cx = gameGrid.toPixelX(player.gridPos.col) + halfTile
        val cy = gameGrid.toPixelY(player.gridPos.row) + halfTile
        val radius = orbitalRadiusTiles * tileSize

        for (i in 0..<orbitalCount) {
            val offset = i * TWO_PI / orbitalCount
            val px = (cx + cos((angle + offset).toDouble()) * radius).toInt()
            val py = (cy + sin((angle + offset).toDouble()) * radius).toInt()

            g.color = ORBITAL_GLOW
            g.fillOval(px - ORBITAL_SIZE, py - ORBITAL_SIZE, ORBITAL_SIZE * 2, ORBITAL_SIZE * 2)
            g.color = ORBITAL_CORE
            g.fillOval(px - ORBITAL_SIZE / 2, py - ORBITAL_SIZE / 2, ORBITAL_SIZE, ORBITAL_SIZE)
            g.color = Color.WHITE
            g.fillOval(px - 2, py - 2, 4, 4)
        }
    }

    override fun handleCollisions(enemiesByCell: Map<GridPos, MutableList<Enemy>>, damageMultiplier: Float): Set<Enemy> {
        val killed = mutableSetOf<Enemy>()
        val tileSize = gameGrid.tileSize
        val halfTile = tileSize / 2
        val cx = gameGrid.toPixelX(player.gridPos.col) + halfTile
        val cy = gameGrid.toPixelY(player.gridPos.row) + halfTile
        val radius = orbitalRadiusTiles * tileSize

        for (i in 0..<orbitalCount) {
            val offset = i * TWO_PI / orbitalCount
            val px = (cx + cos((angle + offset).toDouble()) * radius).toFloat()
            val py = (cy + sin((angle + offset).toDouble()) * radius).toFloat()
            val cell = gameGrid[px, py]

            val cellEnemies = enemiesByCell[cell] ?: continue
            for (enemy in cellEnemies) {
                if (enemy in damagedThisCycle || enemy in killed) continue
                enemy.hp -= orbitalDamage * damageMultiplier
                damagedThisCycle.add(enemy)
                if (enemy.hp <= 0f) {
                    killed.add(enemy)
                }
            }
        }

        return killed
    }
}
