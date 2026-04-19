package com.glycin.springsurvivors.metrics

import com.glycin.springsurvivors.enemies.DifficultyManager
import com.glycin.springsurvivors.xp.EnemyDiedEvent
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicInteger

@Service
class GameMetricsService(
    private val registry: MeterRegistry,
    difficultyManager: DifficultyManager,
) {

    private val highestCombo = AtomicInteger(0)

    init {
        registry.gauge("combo.highest", highestCombo) { it.toDouble() }
        registry.gauge("beats.total", difficultyManager) { it.totalBeats.toDouble() }
    }

    fun recordBeatHit() {
        registry.counter("beats.hit").increment()
    }

    fun recordBeatMissed() {
        registry.counter("beats.missed").increment()
    }

    fun updateHighestCombo(combo: Int) {
        highestCombo.updateAndGet { maxOf(it, combo) }
    }

    @EventListener
    fun onEnemyDied(event: EnemyDiedEvent) {
        registry.counter("enemies.defeated", "enemy_type", event.enemyType.name).increment()
        registry.counter("enemies.killed").increment()
    }

    fun recordUpgradeChosen(upgradeName: String) {
        registry.counter("upgrades.chosen", "upgrade_name", upgradeName).increment()
    }

    fun recordDeath() {
        registry.counter("deaths.total").increment()
    }
}
