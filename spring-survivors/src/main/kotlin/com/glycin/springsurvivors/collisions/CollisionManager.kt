package com.glycin.springsurvivors.collisions

import com.glycin.annotations.GameManager
import com.glycin.springsurvivors.enemies.Enemy
import com.glycin.springsurvivors.enemies.EnemyManager
import com.glycin.springsurvivors.grid.GameGrid
import com.glycin.springsurvivors.player.Player
import com.glycin.springsurvivors.xp.EnemyDiedEvent
import com.glycin.util.GridPos
import org.springframework.context.ApplicationEventPublisher

@GameManager
class CollisionManager(
    private val gameGrid: GameGrid,
    private val enemyManager: EnemyManager,
    private val player: Player,
    private val eventPublisher: ApplicationEventPublisher,
) {

    private val enemiesByCell = HashMap<GridPos, MutableList<Enemy>>()

    fun buildSpatialIndex(): Map<GridPos, MutableList<Enemy>> {
        for (list in enemiesByCell.values) list.clear()
        for (enemy in enemyManager.enemies) {
            enemiesByCell.getOrPut(enemy.gridPos) { mutableListOf() }.add(enemy)
        }
        return enemiesByCell
    }

    fun processKills(killed: Set<Enemy>) {
        if (killed.isEmpty()) return
        enemyManager.enemies.removeAll(killed)
        for (enemy in killed) {
            eventPublisher.publishEvent(EnemyDiedEvent(enemy.gridPos, enemy.xpValue, enemy.type))
        }
    }

    fun checkPlayerCollision(enemiesByCell: Map<GridPos, MutableList<Enemy>>, alreadyKilled: Set<Enemy>) {
        enemiesByCell[player.gridPos]
            ?.filter { it !in alreadyKilled }
            ?.forEach { player.takeDamage(it.contactDamage) }
    }
}
