package com.glycin.springsurvivors.attacks

import com.glycin.springsurvivors.GameState
import com.glycin.springsurvivors.effects.LightningEffect
import com.glycin.springsurvivors.enemies.Enemy
import com.glycin.springsurvivors.enemies.EnemyManager
import com.glycin.springsurvivors.grid.GameGrid
import com.glycin.springsurvivors.xp.EnemyDiedEvent
import com.glycin.util.GridPos
import org.springframework.context.ApplicationEventPublisher
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D

class AirstrikeAttack(
    private val gameState: GameState,
    private val gameGrid: GameGrid,
    private val enemyManager: EnemyManager,
    private val lightningEffect: LightningEffect,
    private val eventPublisher: ApplicationEventPublisher,
) : Attack {

    var beatsBetweenStrikes: Int = 8
    var strikesPerWave: Int = 3
    var strikeDamage: Float = 3f

    private var beatCounter = 0
    private val pendingStrikes = mutableSetOf<GridPos>()

    override fun onBeat(playerGridPos: GridPos) {
        beatCounter++
        if (beatCounter < beatsBetweenStrikes) return
        beatCounter = 0

        val targets = enemyManager.enemies
            .map { it.gridPos }
            .distinct()
            .shuffled()
            .take(strikesPerWave)

        for (target in targets) {
            pendingStrikes.add(target)
            lightningEffect.strike(target)
        }
    }

    override fun update() {}

    override fun render(g: Graphics2D) {
        if (pendingStrikes.isEmpty()) return

        val tileSize = gameGrid.tileSize
        val halfTile = tileSize / 2
        val oldStroke = g.stroke

        g.color = Color(255, 50, 50, 150)
        g.stroke = BasicStroke(1.5f)

        for (pos in pendingStrikes) {
            val cx = gameGrid.toPixelX(pos.col) + halfTile
            val cy = gameGrid.toPixelY(pos.row) + halfTile
            val r = halfTile / 2
            g.drawOval(cx - r, cy - r, r * 2, r * 2)
            g.drawLine(cx - r, cy, cx + r, cy)
            g.drawLine(cx, cy - r, cx, cy + r)
        }

        g.stroke = oldStroke
    }

    override fun handleCollisions(enemiesByCell: Map<GridPos, MutableList<Enemy>>, damageMultiplier: Float): Set<Enemy> {
        return emptySet()
    }

    override fun onLightningFinished(gridPos: GridPos) {
        if (!pendingStrikes.remove(gridPos)) return

        val killed = mutableSetOf<Enemy>()
        val targets = enemyManager.enemies.filter { it.gridPos == gridPos }
        for (enemy in targets) {
            enemy.hp -= strikeDamage * gameState.damageMultiplier
            if (enemy.hp <= 0f) {
                killed.add(enemy)
            }
        }

        if (killed.isNotEmpty()) {
            enemyManager.enemies.removeAll(killed)
            for (enemy in killed) {
                eventPublisher.publishEvent(EnemyDiedEvent(enemy.gridPos, enemy.xpValue, enemy.type))
            }
        }
    }
}
