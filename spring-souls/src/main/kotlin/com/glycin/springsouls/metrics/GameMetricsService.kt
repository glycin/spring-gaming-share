package com.glycin.springsouls.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Service

@Service
class GameMetricsService(
    private val registry: MeterRegistry,
) {
    private val tickTimer = registry.timer("game.tick.duration")

    fun recordDeath(cause: String) {
        registry.counter("deaths.total", "cause", cause).increment()
    }

    fun recordDamageDealt(amount: Double, weapon: String, enemyType: String) {
        registry.summary("damage.dealt", "weapon", weapon, "enemy_type", enemyType).record(amount)
    }

    fun recordDamageReceived(amount: Double, source: String) {
        registry.summary("damage.received", "source", source).record(amount)
    }

    fun recordEnemyKilled(enemyType: String) {
        registry.counter("enemies.killed", "enemy_type", enemyType).increment()
    }

    fun recordPotionUsed() {
        registry.counter("potions.used").increment()
    }

    fun recordDodgeRoll() {
        registry.counter("dodge.rolls").increment()
    }

    fun recordDoorOpened(doorName: String) {
        registry.counter("doors.opened", "door_name", doorName).increment()
    }

    fun recordDoorDenied(doorName: String) {
        registry.counter("doors.denied", "door_name", doorName).increment()
    }

    fun startBossFight(bossName: String): Timer.Sample = Timer.start(registry)

    fun endBossFight(sample: Timer.Sample, bossName: String, outcome: String) {
        sample.stop(registry.timer("boss.fight.duration", "boss_name", bossName, "outcome", outcome))
    }

    fun timeGameTick(block: () -> Unit) {
        tickTimer.record(block)
    }
}
