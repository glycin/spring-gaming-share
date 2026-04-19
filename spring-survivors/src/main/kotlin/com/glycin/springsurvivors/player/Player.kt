package com.glycin.springsurvivors.player

import com.glycin.annotations.Update
import com.glycin.springsurvivors.GameSettings
import com.glycin.springsurvivors.autosave.AutoSaveGameState
import com.glycin.springsurvivors.autosave.BeatMissedError
import com.glycin.springsurvivors.autosave.BeatTransactionService
import com.glycin.springsurvivors.grid.GameGrid
import com.glycin.springsurvivors.rhythm.BeatEvent
import com.glycin.springsurvivors.GameState
import com.glycin.springsurvivors.metrics.GameMetricsService
import com.glycin.springsurvivors.upgrades.LevelUpEvent
import com.glycin.util.GridPos
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

private const val SNAPSHOT_EVERY_N_BEATS = 5
private const val LERP_SPEED = 0.15f

@Component
class Player(
    private val inputService: InputService,
    private val gameGrid: GameGrid,
    private val autoSaveGameState: AutoSaveGameState,
    private val beatTransactionService: BeatTransactionService,
    private val eventPublisher: ApplicationEventPublisher,
    private val gameState: GameState,
    private val metricsService: GameMetricsService,
    gameSettings: GameSettings,
) {

    private val baseXpPerLevel = gameSettings.xp.xpPerLevel
    private val xpGrowthFactor = gameSettings.xp.xpGrowthFactor

    var gridPos = GridPos(gameGrid.cols / 2, gameGrid.rows / 2)
    var previousGridPos: GridPos = gridPos
    var lerpProgress: Float = 1f

    var facingRight = true

    var hp: Float = gameState.maxHp
    var dead = false

    var hitFlash = 0f

    var combo = 0
    var xp = 0

    var level = 0
    var levelProgress = 0f

    fun gainXp(amount: Int) {
        xp += amount
        while (xp >= xpForNextLevel()) {
            xp -= xpForNextLevel()
            level++
            eventPublisher.publishEvent(LevelUpEvent(level))
        }
        levelProgress = xp.toFloat() / xpForNextLevel()
    }

    fun xpForNextLevel(): Int = (baseXpPerLevel * (1 + level * xpGrowthFactor)).toInt()

    fun takeDamage(amount: Float) {
        if (dead) return
        hp = (hp - amount).coerceAtLeast(0f)
        hitFlash = 1f
        if (hp <= 0f) {
            dead = true
            eventPublisher.publishEvent(PlayerDiedEvent())
        }
    }

    @Update(order = 10)
    fun update() {
        if (lerpProgress < 1f) {
            lerpProgress = (lerpProgress + LERP_SPEED).coerceAtMost(1f)
        }
        if (hitFlash > 0f) {
            hitFlash = (hitFlash - 0.05f).coerceAtLeast(0f)
        }
    }

    @EventListener
    @Order(1)
    fun onBeat(event: BeatEvent) {
        if (dead) return
        if (gameState.frozen) {
            onBeatFrozen()
            return
        }

        beatTransactionService.executeOnBeat {
            val direction = inputService.currentGridDirection()

            if (direction == null) {
                if (combo > 0) missedBeat()
                return@executeOnBeat
            }

            val newPos = gridPos + direction
            if (!gameGrid.isInBounds(newPos)) return@executeOnBeat

            moveTo(newPos)
            inputService.consumeDirection()

            combo++
            metricsService.recordBeatHit()
            metricsService.updateHighestCombo(combo)
            if (combo % SNAPSHOT_EVERY_N_BEATS == 0) {
                autoSaveGameState.snapshotRequested = true
            }
        }
    }

    private fun onBeatFrozen() {
        val direction = inputService.currentGridDirection() ?: return
        val newPos = gridPos + direction
        if (gameGrid.isInBounds(newPos)) {
            moveTo(newPos)
        }
    }

    fun moveTo(target: GridPos) {
        val delta = target.col - gridPos.col
        if (delta != 0) {
            facingRight = delta > 0
        }
        previousGridPos = gridPos
        gridPos = target
        lerpProgress = 0f
    }

    private fun missedBeat(): Nothing {
        combo = 0
        metricsService.recordBeatMissed()
        throw BeatMissedError()
    }
}
