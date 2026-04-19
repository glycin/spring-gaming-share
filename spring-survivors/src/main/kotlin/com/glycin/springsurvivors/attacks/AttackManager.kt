package com.glycin.springsurvivors.attacks

import com.glycin.annotations.GameManager
import com.glycin.annotations.Renderer
import com.glycin.annotations.Update
import com.glycin.springsurvivors.GameSettings
import com.glycin.springsurvivors.GameState
import com.glycin.springsurvivors.collisions.CollisionManager
import com.glycin.springsurvivors.enemies.Enemy
import com.glycin.springsurvivors.effects.LightningFinishedEvent
import com.glycin.springsurvivors.grid.GameGrid
import com.glycin.springsurvivors.player.Player
import com.glycin.springsurvivors.rhythm.BeatEvent
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import java.awt.Graphics2D

@GameManager
class AttackManager(
    gameSettings: GameSettings,
    private val gameState: GameState,
    gameGrid: GameGrid,
    private val player: Player,
    private val collisionManager: CollisionManager,
) {

    private val attacks = mutableListOf<Attack>()

    init {
        attacks.add(BaseAttack(gameSettings, gameState, gameGrid))
    }

    fun unlock(attack: Attack) {
        attacks.add(attack)
    }

    fun <T : Attack> getAttack(type: Class<T>): T? = attacks.filterIsInstance(type).firstOrNull()

    @EventListener
    @Order(2)
    fun onBeat(event: BeatEvent) {
        if (gameState.frozen || player.dead) return
        attacks.forEach { it.onBeat(player.gridPos) }
    }

    @EventListener
    fun onLightningFinished(event: LightningFinishedEvent) {
        attacks.forEach { it.onLightningFinished(event.gridPos) }
    }

    @Update(order = 30)
    fun update() {
        attacks.forEach { it.update() }

        val enemiesByCell = collisionManager.buildSpatialIndex()

        val allKilled = mutableSetOf<Enemy>()
        for (attack in attacks) {
            allKilled.addAll(attack.handleCollisions(enemiesByCell, gameState.damageMultiplier))
        }

        if (allKilled.isNotEmpty()) {
            attacks.forEach { it.onEnemiesKilled(allKilled) }
        }

        collisionManager.processKills(allKilled)
        collisionManager.checkPlayerCollision(enemiesByCell, allKilled)
    }

    @Renderer
    fun render(g: Graphics2D) {
        attacks.forEach { it.render(g) }
    }

    fun snapshotAll(): List<Any?> = attacks.map { it.snapshot() }

    fun restoreAll(snapshots: List<Any?>) {
        attacks.forEachIndexed { i, attack -> attack.restore(snapshots.getOrNull(i)) }
    }
}
