package com.glycin.springshooter

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "game.level")
data class LevelProperties(
    val spawnX: Double = 2.5,
    val spawnY: Double = 2.5,
    val rows: List<String> = emptyList(),
)
