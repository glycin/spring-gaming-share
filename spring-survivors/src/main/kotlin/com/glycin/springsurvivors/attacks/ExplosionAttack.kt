package com.glycin.springsurvivors.attacks

import com.glycin.springsurvivors.GameState
import com.glycin.springsurvivors.enemies.Enemy
import com.glycin.springsurvivors.enemies.EnemyManager
import com.glycin.springsurvivors.grid.GameGrid
import com.glycin.springsurvivors.xp.EnemyDiedEvent
import com.glycin.util.GridPos
import org.springframework.context.ApplicationEventPublisher
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D

private const val EXPLOSION_LIFETIME = 15

private val ADJACENT_OFFSETS = listOf(
    GridPos(-1, -1), GridPos(0, -1), GridPos(1, -1),
    GridPos(-1, 0),                  GridPos(1, 0),
    GridPos(-1, 1),  GridPos(0, 1),  GridPos(1, 1),
)
//TODO: Show explosion
class ExplosionAttack(
    private val gameState: GameState,
    private val gameGrid: GameGrid,
    private val enemyManager: EnemyManager,
    private val eventPublisher: ApplicationEventPublisher,
) : Attack {

    var explosionDamage: Float = 0.5f
    var maxRingRadiusTiles: Float = 1.5f

    private val activeExplosions = mutableListOf<Explosion>()
    private val explodedThisFrame = mutableSetOf<GridPos>()

    override fun onBeat(playerGridPos: GridPos) {}

    override fun update() {
        explodedThisFrame.clear()
        activeExplosions.forEach { it.lifetime-- }
        activeExplosions.removeAll { it.lifetime <= 0 }
    }

    override fun render(g: Graphics2D) {
        if (activeExplosions.isEmpty()) return

        val tileSize = gameGrid.tileSize
        val halfTile = tileSize / 2
        val oldComposite = g.composite
        val oldStroke = g.stroke

        for (explosion in activeExplosions) {
            val t = 1f - (explosion.lifetime.toFloat() / EXPLOSION_LIFETIME)
            val alpha = (1f - t).coerceIn(0f, 1f)
            val radius = (t * maxRingRadiusTiles * tileSize).toInt()
            val cx = gameGrid.toPixelX(explosion.gridPos.col) + halfTile
            val cy = gameGrid.toPixelY(explosion.gridPos.row) + halfTile

            if (t < 0.3f) {
                val flashAlpha = (1f - t / 0.3f) * 0.6f
                g.composite = AlphaComposite.SrcOver.derive(flashAlpha)
                g.color = Color(255, 150, 50)
                g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2)
            }

            g.composite = AlphaComposite.SrcOver.derive(alpha)
            g.color = Color(255, 100, 30)
            g.stroke = BasicStroke(3f * (1f - t) + 1f)
            g.drawOval(cx - radius, cy - radius, radius * 2, radius * 2)
        }

        g.composite = oldComposite
        g.stroke = oldStroke
    }

    override fun handleCollisions(enemiesByCell: Map<GridPos, MutableList<Enemy>>, damageMultiplier: Float): Set<Enemy> {
        return emptySet()
    }

    override fun onEnemiesKilled(killed: Set<Enemy>) {
        val chainKilled = mutableSetOf<Enemy>()

        for (enemy in killed) {
            val pos = enemy.gridPos
            if (pos in explodedThisFrame) continue
            explodedThisFrame.add(pos)
            activeExplosions.add(Explosion(pos, EXPLOSION_LIFETIME))

            for (offset in ADJACENT_OFFSETS) {
                val neighbor = pos + offset
                if (!gameGrid.isInBounds(neighbor)) continue
                val neighbors = enemyManager.enemies.filter { it.gridPos == neighbor && it !in killed && it !in chainKilled }
                for (target in neighbors) {
                    target.hp -= explosionDamage * gameState.damageMultiplier
                    if (target.hp <= 0f) {
                        chainKilled.add(target)
                    }
                }
            }
        }

        if (chainKilled.isNotEmpty()) {
            enemyManager.enemies.removeAll(chainKilled)
            for (enemy in chainKilled) {
                eventPublisher.publishEvent(EnemyDiedEvent(enemy.gridPos, enemy.xpValue, enemy.type))
            }
        }
    }

    private class Explosion(
        val gridPos: GridPos,
        var lifetime: Int,
    )
}
