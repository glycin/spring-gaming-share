package com.glycin.springsurvivors

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "game.settings")
data class GameSettings(
    val windowWidth: Int = 1200,
    val windowHeight: Int = 800,
    val gridSize: Int = 16,
    val enemy: EnemySettings = EnemySettings(),
    val projectile: ProjectileSettings = ProjectileSettings(),
    val spawning: SpawningSettings = SpawningSettings(),
    val beat: BeatSettings = BeatSettings(),
    val xp: XpSettings = XpSettings(),
    val player: PlayerSettings = PlayerSettings(),
    val countdownPun: String = "Are you ready to Spring & Roll?!",
    val musicPath: String = "music/demo_song.wav",
)

data class EnemySettings(
    val hp: Float = 3f,
)

data class ProjectileSettings(
    val speed: Float = 5f,
    val size: Int = 8,
)

data class SpawningSettings(
    val maxEnemies: Int = 200,
    val interval: Int = 30,
    val countMin: Int = 1,
    val countMax: Int = 3,
)

data class BeatSettings(
    val initialBpm: Int = 120,
    val beatToleranceRatio: Float = 0.6f,
    val bpmEscalation: List<BpmThreshold> = listOf(
        BpmThreshold(5, 200),
        BpmThreshold(10, 250),
    ),
) {
    fun toleranceMsForBpm(bpm: Int): Int = ((60_000f / bpm) * beatToleranceRatio).toInt()
}

data class XpSettings(
    val xpPerLevel: Int = 20,
    val xpGained: Int = 10,
    val xpGrowthFactor: Float = 0.4f,
)

data class PlayerSettings(
    val maxHp: Float = 5f,
)

data class BpmThreshold(
    val waveNumber: Int,
    val bpm: Int,
)
