package com.glycin.springsouls

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "game.level")
data class LevelProperties(
    val spawnX: Float = 10.5f,
    val spawnZ: Float = 10.5f,
    val rows: List<String> = emptyList(),
)
