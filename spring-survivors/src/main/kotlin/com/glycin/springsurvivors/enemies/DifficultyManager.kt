package com.glycin.springsurvivors.enemies

import org.springframework.stereotype.Component

private data class DifficultyTier(
    val beatThreshold: Int,
    val spawnTable: List<Pair<EnemyType, Int>>,
    val spawnCount: IntRange,
    val spawnInterval: Int,
    val hpMultiplier: Float = 1f,
)

private val TIERS = listOf(
    DifficultyTier(0, listOf(EnemyType.SKULL to 1), 1..3, 5),
    DifficultyTier(50, listOf(EnemyType.SKULL to 3, EnemyType.SPEEDER to 2), 1..4, 4),
    DifficultyTier(120, listOf(EnemyType.SKULL to 3, EnemyType.SPEEDER to 2, EnemyType.BRUTE to 1), 2..4, 4, hpMultiplier = 1.5f),
    DifficultyTier(200, listOf(EnemyType.SKULL to 2, EnemyType.SPEEDER to 2, EnemyType.BRUTE to 2, EnemyType.WRAITH to 1), 2..5, 3, hpMultiplier = 2.5f),
    DifficultyTier(350, listOf(EnemyType.SKULL to 1, EnemyType.SPEEDER to 2, EnemyType.BRUTE to 2, EnemyType.WRAITH to 2, EnemyType.TITAN to 1), 3..6, 3, hpMultiplier = 4f),
)

@Component
class DifficultyManager {

    var totalBeats: Int = 0

    fun onBeat() {
        totalBeats++
    }

    private fun currentTier(): DifficultyTier = TIERS.last { totalBeats >= it.beatThreshold }

    fun getSpawnTable(): List<Pair<EnemyType, Int>> = currentTier().spawnTable

    fun getSpawnCountRange(): IntRange = currentTier().spawnCount

    fun getSpawnInterval(): Int = currentTier().spawnInterval

    fun getHpMultiplier(): Float = currentTier().hpMultiplier + (totalBeats / 200f) * 1.5f
}
